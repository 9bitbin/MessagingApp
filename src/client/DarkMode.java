package client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Date;

public class DarkMode {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 12345;

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    private JFrame loginFrame;
    private JFrame chatFrame;
    private JTextArea messageArea;
    private JTextField messageField;
    private JList<String> userList;
    private DefaultListModel<String> userModel;
    private JToggleButton themeToggle; // Toggle for light/dark mode
    private String username;

    private Map<String, PrivateChatWindow> privateChats;
    private static final Map<String, String> emojiMap = new HashMap<>();
    private boolean isDarkMode = false; // Current theme mode

    static {
        emojiMap.put(":smile:", "😊");
        emojiMap.put(":sad:", "😢");
        emojiMap.put(":laugh:", "😂");
        emojiMap.put(":thumbs_up:", "👍");
        emojiMap.put(":heart:", "❤️");
        emojiMap.put(":star:", "⭐");
        emojiMap.put(":fire:", "🔥");
    }

    public DarkMode() {
        privateChats = new HashMap<>();
        showLoginScreen();
    }

    private void showLoginScreen() {
        loginFrame = new JFrame("Login/Sign Up");
        loginFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        loginFrame.setSize(300, 150);
        loginFrame.setLayout(new GridLayout(4, 2));

        JLabel userLabel = new JLabel("Username:");
        JLabel passLabel = new JLabel("Password:");
        JTextField userField = new JTextField();
        JPasswordField passField = new JPasswordField();
        JButton loginButton = new JButton("Login");
        JButton signupButton = new JButton("Sign Up");

        loginFrame.add(userLabel);
        loginFrame.add(userField);
        loginFrame.add(passLabel);
        loginFrame.add(passField);
        loginFrame.add(loginButton);
        loginFrame.add(signupButton);

        loginFrame.setVisible(true);

        loginButton.addActionListener(e -> {
            username = userField.getText();
            String password = new String(passField.getPassword());
            handleAuthentication("LOGIN", username, password);
        });

        signupButton.addActionListener(e -> {
            username = userField.getText();
            String password = new String(passField.getPassword());
            handleAuthentication("SIGNUP", username, password);
        });
    }

    private void handleAuthentication(String loginType, String username, String password) {
        try {
            socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            out.println(loginType);
            out.println(username);
            out.println(password);

            String response = in.readLine();
            if ("SUCCESS".equals(response)) {
                loginFrame.dispose();
                setUpChatUI();
                new MessageReceiver().start();
            } else {
                JOptionPane.showMessageDialog(loginFrame, "Authentication failed!", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setUpChatUI() {
        chatFrame = new JFrame("Chat - " + username);
        chatFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        chatFrame.setSize(600, 400);

        messageArea = new JTextArea(10, 30);
        messageArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(messageArea);
        chatFrame.add(scrollPane, BorderLayout.CENTER);

        messageField = new JTextField(30);
        messageField.addActionListener(e -> sendMessage());
        chatFrame.add(messageField, BorderLayout.SOUTH);

        userModel = new DefaultListModel<>();
        userList = new JList<>(userModel);
        userList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    String selectedUser = userList.getSelectedValue();
                    if (selectedUser != null && !selectedUser.equals(username)) {
                        openPrivateChat(selectedUser);
                    }
                }
            }
        });
        JScrollPane userScrollPane = new JScrollPane(userList);
        userScrollPane.setPreferredSize(new Dimension(150, 0));
        chatFrame.add(userScrollPane, BorderLayout.EAST);

        // Top panel to hold Logout and Theme Toggle buttons
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton logoutButton = new JButton("Logout");
        logoutButton.addActionListener(e -> logout());
        themeToggle = new JToggleButton("Switch to Dark Mode");
        themeToggle.addActionListener(e -> toggleTheme());

        topPanel.add(logoutButton);
        topPanel.add(themeToggle);
        chatFrame.add(topPanel, BorderLayout.NORTH);

        chatFrame.setVisible(true);
    }

    private void logout() {
    }

    private void toggleTheme() {
        isDarkMode = !isDarkMode;
        themeToggle.setText(isDarkMode ? "Switch to Light Mode" : "Switch to Dark Mode");
        updateTheme();
    }

    private void updateTheme() {
        Color bgColor = isDarkMode ? Color.DARK_GRAY : Color.WHITE;
        Color fgColor = isDarkMode ? Color.WHITE : Color.BLACK;

        chatFrame.getContentPane().setBackground(bgColor);
        messageArea.setBackground(bgColor);
        messageArea.setForeground(fgColor);
        messageField.setBackground(bgColor);
        messageField.setForeground(fgColor);
        userList.setBackground(bgColor);
        userList.setForeground(fgColor);

        // Update private chat windows
        privateChats.values().forEach(PrivateChatWindow::updateTheme);
    }

    private void sendMessage() {
        String message = messageField.getText();
        message = replaceEmojis(message);
        addMessage("Me: " + message);
        out.println(message);
        messageField.setText("");
    }

    private void openPrivateChat(String recipient) {
        if (!privateChats.containsKey(recipient)) {
            PrivateChatWindow chatWindow = new PrivateChatWindow(recipient);
            privateChats.put(recipient, chatWindow);
        } else {
            privateChats.get(recipient).toFront();
        }
    }

    private void updateUserList(String users) {
        userModel.clear();
        String[] userArray = users.split(" ");
        for (String user : userArray) {
            userModel.addElement(user);
        }
    }

    private void handlePrivateMessage(String message) {
        int senderEnd = message.indexOf(": ");
        String sender = message.substring(13, senderEnd);
        String content = message.substring(senderEnd + 2);

        openPrivateChat(sender);
        PrivateChatWindow chatWindow = privateChats.get(sender);
        chatWindow.addMessage("From " + sender + ": " + replaceEmojis(content));
    }

    private class PrivateChatWindow extends JFrame {
        private JTextArea chatArea;
        private JTextField inputField;
        private String recipient;

        public PrivateChatWindow(String recipient) {
            this.recipient = recipient;
            setTitle("Private Chat with " + recipient);
            setSize(300, 200);
            setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

            chatArea = new JTextArea();
            chatArea.setEditable(false);
            add(new JScrollPane(chatArea), BorderLayout.CENTER);

            inputField = new JTextField();
            inputField.addActionListener(e -> {
                String message = inputField.getText();
                message = replaceEmojis(message);
                addMessage("Me: " + message);
                out.println("/msg " + recipient + " " + message);
                inputField.setText("");
            });
            add(inputField, BorderLayout.SOUTH);

            addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    privateChats.remove(recipient);
                }
            });

            updateTheme();
            setVisible(true);
        }

        public void addMessage(String message) {
            String timestamp = new SimpleDateFormat("HH:mm:ss").format(new Date());
            chatArea.append("[" + timestamp + "] " + message + "\n");
        }

        public void updateTheme() {
            Color bgColor = isDarkMode ? Color.DARK_GRAY : Color.WHITE;
            Color fgColor = isDarkMode ? Color.WHITE : Color.BLACK;

            chatArea.setBackground(bgColor);
            chatArea.setForeground(fgColor);
            inputField.setBackground(bgColor);
            inputField.setForeground(fgColor);
            getContentPane().setBackground(bgColor);
        }

        public void toFront() {
            super.toFront();
            setState(JFrame.NORMAL);
        }
    }

    private class MessageReceiver extends Thread {
        public void run() {
            String message;
            try {
                while ((message = in.readLine()) != null) {
                    if (message.startsWith("/users ")) {
                        updateUserList(message.substring(7));
                    } else if (message.startsWith("Private from ")) {
                        handlePrivateMessage(message);
                    } else {
                        addMessage(replaceEmojis(message));
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void addMessage(String message) {
        String timestamp = new SimpleDateFormat("HH:mm:ss").format(new Date());
        messageArea.append("[" + timestamp + "] " + message + "\n");
    }

    private String replaceEmojis(String message) {
        for (Map.Entry<String, String> entry : emojiMap.entrySet()) {
            message = message.replace(entry.getKey(), entry.getValue());
        }
        return message;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ChatClient::new);
    }
}
