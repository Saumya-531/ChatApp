package client;

import java.io.*;
import java.net.Socket;
import java.util.UUID;

public class ChatClient {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private final String username;

    public ChatClient(String host, int port, String username) throws IOException {
        this.username = username;
        socket = new Socket(host, port);

        in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
        out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);


        out.println("JOIN:" + username);
        System.out.println("[CLIENT] Connected to server as " + username);
    }

    public void sendMessage(String text) {
        String msgId = UUID.randomUUID().toString();
        out.println("MSG:" + msgId + ":" + text);
        System.out.println("[CLIENT] Sent -> MSG:" + msgId + ":" + text);
    }

    public BufferedReader getInput() {
        return in;
    }

    public PrintWriter getOutput() {
        return out;
    }

    public String getUsername() {
        return username;
    }

    public void close() {
        try { if (in != null) in.close(); } catch (IOException ignored) {}
        if (out != null) out.close();
        try { if (socket != null && !socket.isClosed()) socket.close(); } catch (IOException ignored) {}
    }
}
