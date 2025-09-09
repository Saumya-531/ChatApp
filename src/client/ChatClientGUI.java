package client;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.BufferedReader;

public class ChatClientGUI extends JFrame {
    private final ChatClient client;
    private final JPanel chatPanel;
    private final JTextField inputField;
    private final DefaultListModel<String> userListModel;
    private final JScrollPane chatScroll;

    private final String username;

    public ChatClientGUI(String host, int port, String username) throws Exception {
        super("Chat - " + username);
        this.username = username;

        client = new ChatClient(host, port, username);

        setSize(1000, 650);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        chatPanel = new JPanel();
        chatPanel.setLayout(new BoxLayout(chatPanel, BoxLayout.Y_AXIS));
        chatPanel.setBackground(Color.WHITE);

        chatScroll = new JScrollPane(chatPanel);
        chatScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        add(chatScroll, BorderLayout.CENTER);

        inputField = new JTextField();
        inputField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        JButton sendButton = new JButton("Send");
        sendButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        JPanel bottom = new JPanel(new BorderLayout(5, 5));
        bottom.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        bottom.add(inputField, BorderLayout.CENTER);
        bottom.add(sendButton, BorderLayout.EAST);
        add(bottom, BorderLayout.SOUTH);

        userListModel = new DefaultListModel<>();
        JList<String> userList = new JList<>(userListModel);
        userList.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        JScrollPane userScroll = new JScrollPane(userList);
        userScroll.setPreferredSize(new Dimension(200, 0));

        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.add(new JLabel(" Online Users", SwingConstants.CENTER), BorderLayout.NORTH);
        rightPanel.add(userScroll, BorderLayout.CENTER);
        add(rightPanel, BorderLayout.EAST);

        sendButton.addActionListener(e -> sendMessage());
        inputField.addActionListener(e -> sendMessage());

        new Thread(() -> {
            try {
                BufferedReader in = client.getInput();
                String line;
                while ((line = in.readLine()) != null) {
                    processServerMessage(line);
                }
            } catch (Exception ex) {
                addSystemMessage("[System] Disconnected from server.");
            }
        }).start();

        setVisible(true);
    }

    private void sendMessage() {
        String text = inputField.getText().trim();
        if (!text.isEmpty()) {
            client.sendMessage(text);
            addMessageBubble(username, text, true); // Your message bubble
            inputField.setText("");
        }
    }

    private void processServerMessage(String line) {
        SwingUtilities.invokeLater(() -> {
            if (line.startsWith("USERLIST:")) {
                userListModel.clear();
                String[] users = line.substring(9).split(",");
                for (String u : users) {
                    if (!u.isBlank()) userListModel.addElement(u);
                }
            } else if (line.startsWith("MSG:")) {
                String[] parts = line.split(":", 3);
                if (parts.length == 3) {
                    String fromUser = parts[1];
                    String msg = parts[2];
                    addMessageBubble(fromUser, msg, fromUser.equals(username));
                }
            } else if (line.startsWith("SYS:")) {
                addSystemMessage(line.substring(4));
            } else {
                addSystemMessage(line);
            }
        });
    }

    private void addMessageBubble(String sender, String message, boolean isSelf) {
        JPanel bubble = new JPanel();
        bubble.setLayout(new BorderLayout());
        bubble.setBorder(new EmptyBorder(5, 10, 5, 10));

        JLabel msgLabel = new JLabel("<html><p style='width:200px;'>" + message + "</p></html>");
        msgLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        msgLabel.setOpaque(true);
        msgLabel.setBorder(new EmptyBorder(8, 10, 8, 10));

        if (isSelf) {
            msgLabel.setBackground(new Color(0, 120, 215));
            msgLabel.setForeground(Color.WHITE);
            bubble.add(msgLabel, BorderLayout.EAST);
        } else {
            msgLabel.setBackground(new Color(220, 220, 220));
            msgLabel.setForeground(Color.BLACK);
            bubble.add(new JLabel(sender + ": "), BorderLayout.WEST);
            bubble.add(msgLabel, BorderLayout.CENTER);
        }

        chatPanel.add(bubble);
        chatPanel.revalidate();
        chatScroll.getVerticalScrollBar().setValue(chatScroll.getVerticalScrollBar().getMaximum());
    }

    private void addSystemMessage(String message) {
        JLabel sysLabel = new JLabel(message, SwingConstants.CENTER);
        sysLabel.setFont(new Font("Segoe UI", Font.ITALIC, 13));
        sysLabel.setForeground(Color.GRAY);

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.add(sysLabel, BorderLayout.CENTER);

        chatPanel.add(panel);
        chatPanel.revalidate();
        chatScroll.getVerticalScrollBar().setValue(chatScroll.getVerticalScrollBar().getMaximum());
    }

    public static void main(String[] args) throws Exception {
        String host = "localhost";
        int port = 12345;
        String username = JOptionPane.showInputDialog("Enter username:");
        if (username != null && !username.trim().isEmpty()) {
            new ChatClientGUI(host, port, username.trim());
        }
    }
}
