package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ChatServer {

    private static final Set<ClientHandler> clients = ConcurrentHashMap.newKeySet();
    private static final Set<String> onlineUsers = ConcurrentHashMap.newKeySet();
    private static final List<String> chatHistory = Collections.synchronizedList(new ArrayList<>());

    private static final Map<String, String> messageSenderMap = new ConcurrentHashMap<>();

    private static final Map<String, ClientHandler> usernameHandlerMap = new ConcurrentHashMap<>();

    public static void addClient(ClientHandler ch) { clients.add(ch); }
    public static void removeClient(ClientHandler ch) { clients.remove(ch); }
    public static Set<ClientHandler> getClients() { return clients; }

    public static void addOnlineUser(String username) { onlineUsers.add(username); }
    public static void removeOnlineUser(String username) { onlineUsers.remove(username); }
    public static Set<String> getOnlineUsers() { return onlineUsers; }

    public static List<String> getChatHistory() { return chatHistory; }

    public static void registerUsernameHandler(String username, ClientHandler handler) {
        if (username != null && handler != null) usernameHandlerMap.put(username, handler);
    }
    public static void unregisterUsernameHandler(String username) {
        if (username != null) usernameHandlerMap.remove(username);
    }
    public static ClientHandler getHandlerByUsername(String username) {
        return usernameHandlerMap.get(username);
    }


    public static void broadcast(String message) {
        if (message == null) return;

        if (message.startsWith("MSG:")) {
            chatHistory.add(message);
            String[] parts = message.split(":", 4);
            if (parts.length >= 3) {
                String msgId = parts[1];
                String sender = parts[2];
                if (msgId != null && sender != null) messageSenderMap.put(msgId, sender);
            }
        }

        for (ClientHandler ch : clients) {
            ch.sendLine(message);
        }
    }


    public static void handleDeliveredAck(String msgId, String recipient) {
        if (msgId == null || recipient == null) return;
        String originalSender = messageSenderMap.get(msgId);
        if (originalSender == null) return;
        ClientHandler senderHandler = getHandlerByUsername(originalSender);
        if (senderHandler != null) {
            senderHandler.sendLine("DELIVERED_UPDATE:" + msgId + ":" + recipient);
        }
    }


    public static void handleReadAck(String msgId, String recipient) {
        if (msgId == null || recipient == null) return;
        String originalSender = messageSenderMap.get(msgId);
        if (originalSender == null) return;
        ClientHandler senderHandler = getHandlerByUsername(originalSender);
        if (senderHandler != null) {
            senderHandler.sendLine("READ_UPDATE:" + msgId + ":" + recipient);
        }
    }


    public static void forwardTyping(String fromUser, boolean start) {
        String cmd = start ? "TYPING:" + fromUser + ":START" : "TYPING:" + fromUser + ":STOP";
        for (ClientHandler ch : clients) {
            if (!fromUser.equals(ch.getUsername())) ch.sendLine(cmd);
        }
    }

    public static void main(String[] args) {
        int port = 12345;
        System.out.println("[SERVER] Starting on port " + port + " ...");

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("[SERVER] Listening on port " + port);

            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("[SERVER] New connection from " + socket.getRemoteSocketAddress());

                ClientHandler handler = new ClientHandler(socket);
                addClient(handler);
                new Thread(handler).start();
            }

        } catch (IOException e) {
            System.err.println("[SERVER] Fatal error: " + e.getMessage());
        }
    }
}
