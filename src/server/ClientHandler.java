package server;

import java.io.*;
import java.net.Socket;
public class ClientHandler implements Runnable {

    private final Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private String username = "Unknown";

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    public String getUsername() {
        return username;
    }

    public void sendLine(String line) {
        if (out != null) out.println(line);
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);

            String first = in.readLine();
            if (first == null || !first.startsWith("JOIN:")) {
                out.println("SYS:Invalid handshake. Use JOIN:<username>");
                close();
                return;
            }

            username = first.substring("JOIN:".length()).trim();
            if (username.isEmpty()) username = "User" + socket.getPort();


            ChatServer.addOnlineUser(username);
            ChatServer.registerUsernameHandler(username, this);


            sendLine("SYS:Welcome " + username + "! You are connected.");
            for (String msg : ChatServer.getChatHistory()) sendLine(msg);


            ChatServer.broadcast("SYS:" + username + " joined the chat.");
            broadcastUserList();


            String line;
            while ((line = in.readLine()) != null) {
                line = line.trim();
                if (line.equalsIgnoreCase("QUIT")) break;

                if (line.startsWith("MSG:")) {
                    // Client sends: MSG:msgId:text
                    String[] parts = line.split(":", 3);
                    if (parts.length >= 3) {
                        String msgId = parts[1];
                        String text = parts[2];
                        // Broadcast in format: MSG:msgId:sender:text
                        String outgoing = "MSG:" + msgId + ":" + username + ":" + text;
                        ChatServer.broadcast(outgoing);
                    } else {
                        out.println("SYS:Bad MSG format. Use MSG:<msgId>:<text>");
                    }
                } else if (line.startsWith("DELIVERED:")) {
                    String msgId = line.substring("DELIVERED:".length()).trim();
                    if (!msgId.isEmpty()) ChatServer.handleDeliveredAck(msgId, username);
                } else if (line.startsWith("READ:")) {
                    String msgId = line.substring("READ:".length()).trim();
                    if (!msgId.isEmpty()) ChatServer.handleReadAck(msgId, username);
                } else if (line.startsWith("TYPING:")) {
                    String arg = line.substring("TYPING:".length()).trim();
                    if ("START".equalsIgnoreCase(arg)) ChatServer.forwardTyping(username, true);
                    else if ("STOP".equalsIgnoreCase(arg)) ChatServer.forwardTyping(username, false);
                } else {
                    out.println("SYS:Unknown command");
                }
            }

        } catch (IOException e) {
            System.err.println("[SERVER] Connection error with " + username + ": " + e.getMessage());
        } finally {
            // cleanup
            ChatServer.removeClient(this);
            ChatServer.removeOnlineUser(username);
            ChatServer.unregisterUsernameHandler(username);
            ChatServer.broadcast("SYS:" + username + " left the chat.");
            broadcastUserList();
            close();
            System.out.println("[SERVER] " + username + " disconnected.");
        }
    }

    private void broadcastUserList() {
        String list = String.join(",", ChatServer.getOnlineUsers());
        String msg = "USERLIST:" + list;
        for (ClientHandler ch : ChatServer.getClients()) ch.sendLine(msg);
    }

    private void close() {
        try { if (in != null) in.close(); } catch (IOException ignored) {}
        if (out != null) out.close();
        try { if (socket != null && !socket.isClosed()) socket.close(); } catch (IOException ignored) {}
    }
}
