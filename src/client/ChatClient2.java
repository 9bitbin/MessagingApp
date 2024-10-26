// Version 3 of the Chat Client
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

public class ChatClient {
    private static final String SERVER_ADDRESS = "localhost"; // Server address
    private static final int SERVER_PORT = 12345; // Server port

    private Socket socket; // Socket for connecting to the server
    private PrintWriter out; // Output stream for sending messages to the server
    private BufferedReader in; // Input stream for receiving messages from the server

    private JFrame loginFrame; // Login frame
    private JFrame chatFrame; // Main chat frame
    private JTextArea messageArea; // Area to display chat messages
    private JTextField messageField; // Field for typing messages
    private JList<String> userList; // List of online users
    private DefaultListModel<String> userModel; // Data model for the user list
    private String username; // Username of the client

    private Map<String, PrivateChatWindow> privateChats; // Map for managing private chat windows

    public ChatClient() {
        privateChats = new HashMap<>(); // Initialize the map for private chats
        showLoginScreen(); // Display the login screen when the client starts
    }

    // Show the login/sign-up screen
    private void showLoginScreen() {
        loginFrame = new JFrame("Login/Sign Up"); // Create the login frame
        loginFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // Close operation for the frame
        loginFrame.setSize(300, 150); // Set frame size
        loginFrame.setLayout(new GridLayout(4, 2)); // Set layout for the login frame

        // Create UI components for login
        JLabel userLabel = new JLabel("Username:");
        JLabel passLabel = new JLabel("Password:");
        JTextField userField = new JTextField();
        JPasswordField passField = new JPasswordField();
        JButton loginButton = new JButton("Login");
        JButton signupButton = new JButton("Sign Up");

        // Add components to the frame
        loginFrame.add(userLabel);
        loginFrame.add(userField);
        loginFrame.add(passLabel);
        loginFrame.add(passField);
        loginFrame.add(loginButton);
        loginFrame.add(signupButton);

        loginFrame.setVisible(true); // Display the frame

        // Login button listener
        loginButton.addActionListener(e -> {
            username = userField.getText(); // Get the username from the input field
            String password = new String(passField.getPassword()); // Get the password
            handleAuthentication("LOGIN", username, password); // Handle login authentication
        });

        // Sign-up button listener
        signupButton.addActionListener(e -> {
            username = userField.getText(); // Get the username from the input field
            String password = new String(passField.getPassword()); // Get the password
            handleAuthentication("SIGNUP", username, password); // Handle sign-up authentication
        });
    }

    // Handle authentication by sending request to the server
    private void handleAuthentication(String loginType, String username, String password) {
        try {
            // Connect to the server
            socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
            out = new PrintWriter(socket.getOutputStream(), true); // Initialize output stream
            in = new BufferedReader(new InputStreamReader(socket.getInputStream())); // Initialize input stream

            // Send login type, username, and password to server
            out.println(loginType);
            out.println(username);
            out.println(password);

            String response = in.readLine(); // Read server response
            if ("SUCCESS".equals(response)) { // If authentication is successful
                loginFrame.dispose(); // Close the login frame
                setUpChatUI(); // Set up the main chat UI
                new MessageReceiver().start(); // Start receiving messages from the server
            } else {
                // Show error message if authentication fails
                JOptionPane.showMessageDialog(loginFrame, "Authentication failed!", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (IOException e) {
            e.printStackTrace(); // Print error stack trace
        }
    }

    // Set up the main chat UI
    private void setUpChatUI() {
        chatFrame = new JFrame("Chat - " + username); // Create the main chat frame with username
        chatFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // Set close operation
        chatFrame.setSize(600, 400); // Set frame size

        // Create message area for chat
        messageArea = new JTextArea(10, 30);
        messageArea.setEditable(false); // Make message area read-only
        JScrollPane scrollPane = new JScrollPane(messageArea); // Add scroll pane for message area
        chatFrame.add(scrollPane, BorderLayout.CENTER); // Add message area to the center

        // Create message field for typing messages
        messageField = new JTextField(30);
        messageField.addActionListener(e -> {
            String message = messageField.getText(); // Get message from input field
            addMessage("Me: " + message); // Display the message locally
            out.println(message); // Send the message to the server
            messageField.setText(""); // Clear the message field
        });
        chatFrame.add(messageField, BorderLayout.SOUTH); // Add message field to the bottom

        // User list for online users
        userModel = new DefaultListModel<>(); // Create a model for the user list
        userList = new JList<>(userModel); // Create a list for displaying online users
        userList.addMouseListener(new MouseAdapter() { // Add a mouse listener for the user list
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) { // Check for double-click
                    String selectedUser = userList.getSelectedValue(); // Get selected user
                    if (selectedUser != null && !selectedUser.equals(username)) {
                        openPrivateChat(selectedUser); // Open a private chat with the selected user
                    }
                }
            }
        });
        JScrollPane userScrollPane = new JScrollPane(userList); // Add scroll pane for user list
        userScrollPane.setPreferredSize(new Dimension(150, 0)); // Set preferred size for user list
        chatFrame.add(userScrollPane, BorderLayout.EAST); // Add user list to the right

        // Logout button
        JButton logoutButton = new JButton("Logout");
        logoutButton.addActionListener(e -> logout()); // Add logout button listener
        chatFrame.add(logoutButton, BorderLayout.NORTH); // Add logout button to the top

        chatFrame.setVisible(true); // Display the chat frame
    }

    // Handle logout functionality
    private void logout() {
        out.println("/logout"); // Send logout command to the server
        closeConnection(); // Close the connection to the server
        chatFrame.dispose(); // Close the chat frame
        showLoginScreen(); // Show the login screen again
    }

    // Close the connection to the server
    private void closeConnection() {
        try {
            if (socket != null) socket.close(); // Close the socket if it is not null
        } catch (IOException e) {
            e.printStackTrace(); // Print error stack trace
        }
    }

    // Open a private chat window for the selected user
    private void openPrivateChat(String recipient) {
        if (!privateChats.containsKey(recipient)) { // Check if a chat window already exists for the recipient
            PrivateChatWindow chatWindow = new PrivateChatWindow(recipient); // Create a new private chat window
            privateChats.put(recipient, chatWindow); // Add the new chat window to the map
        } else {
            privateChats.get(recipient).toFront(); // Bring existing chat window to the front
        }
    }

    // Update the user list in the UI
    private void updateUserList(String users) {
        userModel.clear(); // Clear the current user list
        String[] userArray = users.split(" "); // Split the list of users
        for (String user : userArray) {
            userModel.addElement(user); // Add each user to the user model
        }
        // Customize the look of the user list
        userList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                label.setForeground(Color.GREEN); // Set the text color to green
                return label;
            }
        });
    }

    // Handle incoming private messages
    private void handlePrivateMessage(String message) {
        int senderEnd = message.indexOf(": "); // Find the end of the sender's name
        String sender = message.substring(13, senderEnd); // Extract the sender's name
        String content = message.substring(senderEnd + 2); // Extract the message content

        openPrivateChat(sender); // Open a private chat window with the sender
        PrivateChatWindow chatWindow = privateChats.get(sender); // Get the chat window for the sender
        chatWindow.addMessage("From " + sender + ": " + content); // Display the received message in the chat window
    }

    // Class representing a private chat window
    private class PrivateChatWindow extends JFrame {
        private JTextArea chatArea; // Area to display chat messages
        private JTextField inputField; // Field for typing messages
        private String recipient; // Recipient of the private chat

        public PrivateChatWindow(String recipient) {
            this.recipient = recipient; // Set the recipient
            setTitle("Private Chat with " + recipient); // Set the title of the window
            setSize(300, 200); // Set the size of the window
            setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE); // Set the close operation

            chatArea = new JTextArea(); // Create the chat area
            chatArea.setEditable(false); // Make the chat area read-only
            add(new JScrollPane(chatArea), BorderLayout.CENTER); // Add chat area with scroll pane

            inputField = new JTextField(); // Create the input field
            inputField.addActionListener(e -> {
                String message = inputField.getText(); // Get the message from the input field
                addMessage("Me: " + message); // Display the message locally
                out.println("/msg " + recipient + " " + message); // Send the message to the server
                inputField.setText(""); // Clear the input field
            });
            add(inputField, BorderLayout.SOUTH); // Add the input field to the bottom

            // Remove the chat window from the map when it is closed
            addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    privateChats.remove(recipient); // Remove from the map
                }
            });

            setVisible(true); // Display the private chat window
        }

        // Add a message to the chat area with a timestamp
        public void addMessage(String message) {
            String timestamp = new SimpleDateFormat("HH:mm:ss").format(new Date()); // Create a timestamp
            chatArea.append("[" + timestamp + "] " + message + "\n"); // Append the message to the chat area
        }

        // Bring the window to the front
        public void toFront() {
            super.toFront();
            setState(JFrame.NORMAL); // Set window state to normal
        }
    }

    // Class for receiving messages from the server
    private class MessageReceiver extends Thread {
        public void run() {
            String message;
            try {
                while ((message = in.readLine()) != null) { // Continuously read messages from the server
                    if (message.startsWith("/users ")) { // If the message contains the user list
                        updateUserList(message.substring(7)); // Update the user list
                    } else if (message.startsWith("Private from ")) { // If the message is a private message
                        handlePrivateMessage(message); // Handle the private message
                    } else {
                        addMessage(message); // Add the message to the main chat
                    }
                }
            } catch (IOException e) {
                e.printStackTrace(); // Print error stack trace
            }
        }
    }

    // Add a message to the main chat area with a timestamp
    private void addMessage(String message) {
        String timestamp = new SimpleDateFormat("HH:mm:ss").format(new Date()); // Create a timestamp
        messageArea.append("[" + timestamp + "] " + message + "\n"); // Append the message to the message area
    }

    // Main method to start the chat client
    public static void main(String[] args) {
        SwingUtilities.invokeLater(ChatClient::new); // Create a new instance of ChatClient in the Swing thread
    }
}


////Version 4
//package client;
//
//import javax.swing.*;
//import java.awt.*;
//import java.awt.event.*;
//import java.io.*;
//import java.net.*;
//import java.text.SimpleDateFormat;
//import java.util.HashMap;
//import java.util.Map;
//import java.util.Date;
//
//public class ChatClient2 {
//    private static final String SERVER_ADDRESS = "localhost";
//    private static final int SERVER_PORT = 12345;
//
//    private Socket socket;
//    private PrintWriter out;
//    private BufferedReader in;
//
//    private JFrame loginFrame;
//    private JFrame chatFrame;
//    private JTextArea messageArea;
//    private JTextField messageField;
//    private JList<String> userList;
//    private DefaultListModel<String> userModel;
//    private String username;
//
//    private Map<String, PrivateChatWindow> privateChats;
//    private static final Map<String, String> emojiMap = new HashMap<>();
//
//    static {
//        emojiMap.put(":smile:", "😊");
//        emojiMap.put(":sad:", "😢");
//        emojiMap.put(":laugh:", "😂");
//        emojiMap.put(":thumbs_up:", "👍");
//        emojiMap.put(":heart:", "❤️");
//        emojiMap.put(":star:", "⭐");
//        emojiMap.put(":fire:", "🔥");
//    }
//
//    public ChatClient2() {
//        privateChats = new HashMap<>();
//        showLoginScreen();
//    }
//
//    private void showLoginScreen() {
//        loginFrame = new JFrame("Login/Sign Up");
//        loginFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//        loginFrame.setSize(300, 150);
//        loginFrame.setLayout(new GridLayout(4, 2));
//
//        JLabel userLabel = new JLabel("Username:");
//        JLabel passLabel = new JLabel("Password:");
//        JTextField userField = new JTextField();
//        JPasswordField passField = new JPasswordField();
//        JButton loginButton = new JButton("Login");
//        JButton signupButton = new JButton("Sign Up");
//
//        loginFrame.add(userLabel);
//        loginFrame.add(userField);
//        loginFrame.add(passLabel);
//        loginFrame.add(passField);
//        loginFrame.add(loginButton);
//        loginFrame.add(signupButton);
//
//        loginFrame.setVisible(true);
//
//        loginButton.addActionListener(e -> {
//            username = userField.getText();
//            String password = new String(passField.getPassword());
//            handleAuthentication("LOGIN", username, password);
//        });
//
//        signupButton.addActionListener(e -> {
//            username = userField.getText();
//            String password = new String(passField.getPassword());
//            handleAuthentication("SIGNUP", username, password);
//        });
//    }
//
//    private void handleAuthentication(String loginType, String username, String password) {
//        try {
//            socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
//            out = new PrintWriter(socket.getOutputStream(), true);
//            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
//
//            out.println(loginType);
//            out.println(username);
//            out.println(password);
//
//            String response = in.readLine();
//            if ("SUCCESS".equals(response)) {
//                loginFrame.dispose();
//                setUpChatUI();
//                new MessageReceiver().start();
//            } else {
//                JOptionPane.showMessageDialog(loginFrame, "Authentication failed!", "Error", JOptionPane.ERROR_MESSAGE);
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//
//    private void setUpChatUI() {
//        chatFrame = new JFrame("Chat - " + username);
//        chatFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//        chatFrame.setSize(600, 400);
//
//        messageArea = new JTextArea(10, 30);
//        messageArea.setEditable(false);
//        JScrollPane scrollPane = new JScrollPane(messageArea);
//        chatFrame.add(scrollPane, BorderLayout.CENTER);
//
//        messageField = new JTextField(30);
//        messageField.addActionListener(e -> sendMessage());
//        messageField.addKeyListener(new KeyAdapter() {
//            public void keyPressed(KeyEvent e) {
//                out.println("/typing " + username);
//            }
//        });
//        chatFrame.add(messageField, BorderLayout.SOUTH);
//
//        userModel = new DefaultListModel<>();
//        userList = new JList<>(userModel);
//        userList.addMouseListener(new MouseAdapter() {
//            @Override
//            public void mouseClicked(MouseEvent e) {
//                if (e.getClickCount() == 2) {
//                    String selectedUser = userList.getSelectedValue();
//                    if (selectedUser != null && !selectedUser.equals(username)) {
//                        openPrivateChat(selectedUser);
//                    }
//                }
//            }
//        });
//        JScrollPane userScrollPane = new JScrollPane(userList);
//        userScrollPane.setPreferredSize(new Dimension(150, 0));
//        chatFrame.add(userScrollPane, BorderLayout.EAST);
//
//        JButton sendFileButton = new JButton("Send File");
//        sendFileButton.addActionListener(e -> sendFile());
//        chatFrame.add(sendFileButton, BorderLayout.NORTH);
//
//        chatFrame.setVisible(true);
//    }
//
//    private void sendMessage() {
//        String message = messageField.getText();
//        message = replaceEmojis(message);
//        addMessage("Me: " + message);
//        out.println(message);
//        messageField.setText("");
//    }
//
//    private void sendFile() {
//        JFileChooser fileChooser = new JFileChooser();
//        int option = fileChooser.showOpenDialog(chatFrame);
//        if (option == JFileChooser.APPROVE_OPTION) {
//            File file = fileChooser.getSelectedFile();
//            try (FileInputStream fis = new FileInputStream(file)) {
//                long fileSize = file.length();
//                out.println("/file " + file.getName() + " " + fileSize);
//
//                byte[] buffer = new byte[1024];
//                int bytesRead;
//                while ((bytesRead = fis.read(buffer)) != -1) {
//                    socket.getOutputStream().write(buffer, 0, bytesRead);
//                }
//                addMessage("Sent file: " + file.getName());
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//    }
//
//    private void openPrivateChat(String recipient) {
//        if (!privateChats.containsKey(recipient)) {
//            PrivateChatWindow chatWindow = new PrivateChatWindow(recipient);
//            privateChats.put(recipient, chatWindow);
//        } else {
//            privateChats.get(recipient).toFront();
//        }
//    }
//
//    private void updateUserList(String users) {
//        userModel.clear();
//        String[] userArray = users.split(" ");
//        for (String user : userArray) {
//            userModel.addElement(user);
//        }
//    }
//
//    private void handlePrivateMessage(String message) {
//        int senderEnd = message.indexOf(": ");
//        String sender = message.substring(13, senderEnd);
//        String content = message.substring(senderEnd + 2);
//
//        openPrivateChat(sender);
//        PrivateChatWindow chatWindow = privateChats.get(sender);
//        chatWindow.addMessage("From " + sender + ": " + replaceEmojis(content));
//    }
//
//    private class PrivateChatWindow extends JFrame {
//        private JTextArea chatArea;
//        private JTextField inputField;
//        private String recipient;
//
//        public PrivateChatWindow(String recipient) {
//            this.recipient = recipient;
//            setTitle("Private Chat with " + recipient);
//            setSize(300, 200);
//            setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
//
//            chatArea = new JTextArea();
//            chatArea.setEditable(false);
//            add(new JScrollPane(chatArea), BorderLayout.CENTER);
//
//            inputField = new JTextField();
//            inputField.addActionListener(e -> {
//                String message = inputField.getText();
//                message = replaceEmojis(message);
//                addMessage("Me: " + message);
//                out.println("/msg " + recipient + " " + message);
//                inputField.setText("");
//            });
//            add(inputField, BorderLayout.SOUTH);
//
//            addWindowListener(new WindowAdapter() {
//                @Override
//                public void windowClosing(WindowEvent e) {
//                    privateChats.remove(recipient);
//                }
//            });
//
//            setVisible(true);
//        }
//
//        public void addMessage(String message) {
//            String timestamp = new SimpleDateFormat("HH:mm:ss").format(new Date());
//            chatArea.append("[" + timestamp + "] " + message + "\n");
//        }
//
//        public void toFront() {
//            super.toFront();
//            setState(JFrame.NORMAL);
//        }
//    }
//
//    private class MessageReceiver extends Thread {
//        public void run() {
//            String message;
//            try {
//                while ((message = in.readLine()) != null) {
//                    if (message.startsWith("/users ")) {
//                        updateUserList(message.substring(7));
//                    } else if (message.startsWith("Private from ")) {
//                        handlePrivateMessage(message);
//                    } else if (message.startsWith("/typing ")) {
//                        displayTypingIndicator(message.substring(9));
//                    } else if (message.startsWith("/read ")) {
//                        displayReadReceipt(message.substring(6));
//                    } else {
//                        addMessage(replaceEmojis(message));
//                    }
//                }
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//    }
//
//    private void addMessage(String message) {
//        String timestamp = new SimpleDateFormat("HH:mm:ss").format(new Date());
//        messageArea.append("[" + timestamp + "] " + message + "\n");
//    }
//
//    private void displayTypingIndicator(String typingUser) {
//        if (!typingUser.equals(username)) {
//            addMessage(typingUser + " is typing...");
//        }
//    }
//
//    private void displayReadReceipt(String message) {
//        addMessage("Read receipt: " + message);
//    }
//
//    private String replaceEmojis(String message) {
//        for (Map.Entry<String, String> entry : emojiMap.entrySet()) {
//            message = message.replace(entry.getKey(), entry.getValue());
//        }
//        return message;
//    }
//
//    public static void main(String[] args) {
//        SwingUtilities.invokeLater(ChatClient::new);
//    }
//}



//VERSION 2
//package client;
//
//import javax.swing.*;
//import java.awt.*;
//import java.awt.event.*;
//import java.io.*;
//import java.net.*;
//import java.util.HashMap;
//import java.util.Map;
//
//public class ChatClient2  {
//    private static final String SERVER_ADDRESS = "localhost";
//    private static final int SERVER_PORT = 12345;
//
//    private Socket socket;
//    private PrintWriter out;
//    private BufferedReader in;
//
//    private JFrame loginFrame;
//    private JFrame chatFrame;
//    private JTextArea messageArea;
//    private JTextField messageField;
//    private JList<String> userList;
//    private DefaultListModel<String> userModel;
//    private String username;
//
//    private Map<String, PrivateChatWindow> privateChats;
//
//    public ChatClient2 () {
//        privateChats = new HashMap<>();
//        showLoginScreen();
//    }
//
//    // Show the login/sign-up screen
//    private void showLoginScreen() {
//        loginFrame = new JFrame("Login/Sign Up");
//        loginFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//        loginFrame.setSize(300, 150);
//        loginFrame.setLayout(new GridLayout(4, 2));
//
//        JLabel userLabel = new JLabel("Username:");
//        JLabel passLabel = new JLabel("Password:");
//        JTextField userField = new JTextField();
//        JPasswordField passField = new JPasswordField();
//        JButton loginButton = new JButton("Login");
//        JButton signupButton = new JButton("Sign Up");
//
//        loginFrame.add(userLabel);
//        loginFrame.add(userField);
//        loginFrame.add(passLabel);
//        loginFrame.add(passField);
//        loginFrame.add(loginButton);
//        loginFrame.add(signupButton);
//
//        loginFrame.setVisible(true);
//
//        // Login button listener
//        loginButton.addActionListener(e -> {
//            username = userField.getText();
//            String password = new String(passField.getPassword());
//            handleAuthentication("LOGIN", username, password);
//        });
//
//        // Sign-up button listener
//        signupButton.addActionListener(e -> {
//            username = userField.getText();
//            String password = new String(passField.getPassword());
//            handleAuthentication("SIGNUP", username, password);
//        });
//    }
//
//    // Handle authentication by sending request to the server
//    private void handleAuthentication(String loginType, String username, String password) {
//        try {
//            socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
//            out = new PrintWriter(socket.getOutputStream(), true);
//            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
//
//            out.println(loginType);
//            out.println(username);
//            out.println(password);
//
//            String response = in.readLine();
//            if ("SUCCESS".equals(response)) {
//                loginFrame.dispose();
//                setUpChatUI();
//                new MessageReceiver().start();
//            } else {
//                JOptionPane.showMessageDialog(loginFrame, "Authentication failed!", "Error", JOptionPane.ERROR_MESSAGE);
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//
//    // Set up the main chat UI
//    private void setUpChatUI() {
//        chatFrame = new JFrame("Chat - " + username);
//        chatFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//        chatFrame.setSize(600, 400);
//
//        messageArea = new JTextArea(10, 30);
//        messageArea.setEditable(false);
//        JScrollPane scrollPane = new JScrollPane(messageArea);
//        chatFrame.add(scrollPane, BorderLayout.CENTER);
//
//        messageField = new JTextField(30);
//        messageField.addActionListener(e -> {
//            String message = messageField.getText();
//            messageArea.append("Me: " + message + "\n");
//            out.println(message);
//            messageField.setText("");
//        });
//        chatFrame.add(messageField, BorderLayout.SOUTH);
//
//        // User list for online users
//        userModel = new DefaultListModel<>();
//        userList = new JList<>(userModel);
//        userList.addMouseListener(new MouseAdapter() {
//            @Override
//            public void mouseClicked(MouseEvent e) {
//                if (e.getClickCount() == 2) {
//                    String selectedUser = userList.getSelectedValue();
//                    if (selectedUser != null && !selectedUser.equals(username)) {
//                        openPrivateChat(selectedUser);
//                    }
//                }
//            }
//        });
//        JScrollPane userScrollPane = new JScrollPane(userList);
//        userScrollPane.setPreferredSize(new Dimension(150, 0));
//        chatFrame.add(userScrollPane, BorderLayout.EAST);
//
//        chatFrame.setVisible(true);
//    }
//
//    // Open a private chat window for the selected user
//    private void openPrivateChat(String recipient) {
//        if (!privateChats.containsKey(recipient)) {
//            PrivateChatWindow chatWindow = new PrivateChatWindow(recipient);
//            privateChats.put(recipient, chatWindow);
//        } else {
//            privateChats.get(recipient).toFront();
//        }
//    }
//
//    // Thread to receive messages from the server
//    private class MessageReceiver extends Thread {
//        public void run() {
//            String message;
//            try {
//                while ((message = in.readLine()) != null) {
//                    if (message.startsWith("/users ")) {
//                        updateUserList(message.substring(7));
//                    } else if (message.startsWith("Private from ")) {
//                        handlePrivateMessage(message);
//                    } else {
//                        messageArea.append(message + "\n");
//                    }
//                }
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//    }
//
//    // Update the list of online users
//    private void updateUserList(String users) {
//        userModel.clear();
//        String[] userArray = users.split(" ");
//        for (String user : userArray) {
//            userModel.addElement(user);
//        }
//    }
//
//    // Handle incoming private message
//    private void handlePrivateMessage(String message) {
//        int senderEnd = message.indexOf(": ");
//        String sender = message.substring(13, senderEnd);
//        String content = message.substring(senderEnd + 2);
//
//        openPrivateChat(sender);
//        PrivateChatWindow chatWindow = privateChats.get(sender);
//        chatWindow.addMessage("From " + sender + ": " + content);
//    }
//
//    // Inner class for a private chat window
//    private class PrivateChatWindow extends JFrame {
//        private JTextArea chatArea;
//        private JTextField inputField;
//        private String recipient;
//
//        public PrivateChatWindow(String recipient) {
//            this.recipient = recipient;
//            setTitle("Private Chat with " + recipient);
//            setSize(300, 200);
//            setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
//
//            chatArea = new JTextArea();
//            chatArea.setEditable(false);
//            add(new JScrollPane(chatArea), BorderLayout.CENTER);
//
//            inputField = new JTextField();
//            inputField.addActionListener(e -> {
//                String message = inputField.getText();
//                addMessage("Me: " + message);
//                out.println("/msg " + recipient + " " + message);
//                inputField.setText("");
//            });
//            add(inputField, BorderLayout.SOUTH);
//
//            addWindowListener(new WindowAdapter() {
//                @Override
//                public void windowClosing(WindowEvent e) {
//                    privateChats.remove(recipient);
//                }
//            });
//
//            setVisible(true);
//        }
//
//        public void addMessage(String message) {
//            chatArea.append(message + "\n");
//        }
//
//        // Corrected toFront() method to bring the window to the front without recursion
//        public void toFront() {
//            super.toFront();
//            setState(JFrame.NORMAL);
//        }
//    }
//
//    public static void main(String[] args) {
//        SwingUtilities.invokeLater(ChatClient2 ::new);
//    }
//}

// VERSION 1
//package client;
//
//import javax.swing.*;
//import java.awt.*;
//import java.awt.event.*;
//import java.io.*;
//import java.net.*;
//import java.util.HashMap;
//import java.util.Map;
//
//public class ChatClient2  {
//    private static final String SERVER_ADDRESS = "localhost";
//    private static final int SERVER_PORT = 12345;
//
//    private Socket socket;
//    private PrintWriter out;
//    private BufferedReader in;
//
//    private JFrame loginFrame;
//    private JFrame chatFrame;
//    private JTextArea messageArea;
//    private JTextField messageField;
//    private JList<String> userList; // UI component to display online users
//    private DefaultListModel<String> userModel; // Model for the JList
//    private String username;
//
//    private Map<String, PrivateChatWindow> privateChats; // Track open private chat windows
//
//    public ChatClient2 () {
//        privateChats = new HashMap<>(); // Initialize the map for private chat windows
//        showLoginScreen();
//    }
//
//    // Show the login/sign-up screen
//    private void showLoginScreen() {
//        loginFrame = new JFrame("Login/Sign Up");
//        loginFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//        loginFrame.setSize(300, 150);
//        loginFrame.setLayout(new GridLayout(4, 2));
//
//        JLabel userLabel = new JLabel("Username:");
//        JLabel passLabel = new JLabel("Password:");
//        JTextField userField = new JTextField();
//        JPasswordField passField = new JPasswordField();
//        JButton loginButton = new JButton("Login");
//        JButton signupButton = new JButton("Sign Up");
//
//        loginFrame.add(userLabel);
//        loginFrame.add(userField);
//        loginFrame.add(passLabel);
//        loginFrame.add(passField);
//        loginFrame.add(loginButton);
//        loginFrame.add(signupButton);
//
//        loginFrame.setVisible(true);
//
//        // Login button listener
//        loginButton.addActionListener(e -> {
//            username = userField.getText();
//            String password = new String(passField.getPassword());
//            handleAuthentication("LOGIN", username, password);
//        });
//
//        // Sign-up button listener
//        signupButton.addActionListener(e -> {
//            username = userField.getText();
//            String password = new String(passField.getPassword());
//            handleAuthentication("SIGNUP", username, password);
//        });
//    }
//
//    // Handle authentication by sending request to the server
//    private void handleAuthentication(String loginType, String username, String password) {
//        try {
//            // Connect to server
//            socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
//            out = new PrintWriter(socket.getOutputStream(), true);
//            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
//
//            // Send login/signup type, username, and password to server
//            out.println(loginType);
//            out.println(username);
//            out.println(password);
//
//            // Read response from the server
//            String response = in.readLine();
//            if ("SUCCESS".equals(response)) {
//                loginFrame.dispose(); // Close login screen
//                setUpChatUI();        // Open chat UI
//                new MessageReceiver().start(); // Start receiving messages
//            } else {
//                JOptionPane.showMessageDialog(loginFrame, "Authentication failed!", "Error", JOptionPane.ERROR_MESSAGE);
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//
//    // Set up the main chat UI
//    private void setUpChatUI() {
//        chatFrame = new JFrame("Chat - " + username);
//        chatFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//        chatFrame.setSize(600, 400);
//
//        messageArea = new JTextArea(10, 30);
//        messageArea.setEditable(false);
//        JScrollPane scrollPane = new JScrollPane(messageArea);
//        chatFrame.add(scrollPane, BorderLayout.CENTER);
//
//        messageField = new JTextField(30);
//        messageField.addActionListener(e -> {
//            String message = messageField.getText();
//            messageArea.append("Me: " + message + "\n"); // Display message in the client’s own chat area
//            out.println(message);  // Send the message to the server
//            messageField.setText("");
//        });
//        chatFrame.add(messageField, BorderLayout.SOUTH);
//
//        // User list for online users
//        userModel = new DefaultListModel<>();
//        userList = new JList<>(userModel);
//        userList.addMouseListener(new MouseAdapter() {
//            @Override
//            public void mouseClicked(MouseEvent e) {
//                if (e.getClickCount() == 2) { // Double-click to open private chat
//                    String selectedUser = userList.getSelectedValue();
//                    if (selectedUser != null && !selectedUser.equals(username)) {
//                        openPrivateChat(selectedUser);
//                    }
//                }
//            }
//        });
//        JScrollPane userScrollPane = new JScrollPane(userList);
//        userScrollPane.setPreferredSize(new Dimension(150, 0));
//        chatFrame.add(userScrollPane, BorderLayout.EAST);
//
//        chatFrame.setVisible(true);
//    }
//
//    // Open a private chat window for the selected user
//    private void openPrivateChat(String recipient) {
//        if (!privateChats.containsKey(recipient)) {
//            PrivateChatWindow chatWindow = new PrivateChatWindow(recipient);
//            privateChats.put(recipient, chatWindow);
//        } else {
//            privateChats.get(recipient).toFront();
//        }
//    }
//
//    // Thread to receive messages from the server
//    private class MessageReceiver extends Thread {
//        public void run() {
//            String message;
//            try {
//                while ((message = in.readLine()) != null) {
//                    if (message.startsWith("/users ")) { // Message with the list of online users
//                        updateUserList(message.substring(7)); // Remove "/users " prefix
//                    } else if (message.startsWith("Private from ")) { // Incoming private message
//                        handlePrivateMessage(message);
//                    } else {
//                        messageArea.append(message + "\n"); // Public message
//                    }
//                }
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//    }
//
//    // Update the list of online users
//    private void updateUserList(String users) {
//        userModel.clear();
//        String[] userArray = users.split(" ");
//        for (String user : userArray) {
//            userModel.addElement(user);
//        }
//    }
//
//    // Handle incoming private message
//    private void handlePrivateMessage(String message) {
//        int senderEnd = message.indexOf(": ");
//        String sender = message.substring(13, senderEnd);
//        String content = message.substring(senderEnd + 2);
//
//        // Open the private chat window if not already open
//        openPrivateChat(sender);
//        PrivateChatWindow chatWindow = privateChats.get(sender);
//        chatWindow.addMessage("From " + sender + ": " + content);
//    }
//
//    // Inner class for a private chat window
//    private class PrivateChatWindow extends JFrame {
//        private JTextArea chatArea;
//        private JTextField inputField;
//        private String recipient;
//
//        public PrivateChatWindow(String recipient) {
//            this.recipient = recipient;
//            setTitle("Private Chat with " + recipient);
//            setSize(300, 200);
//            setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
//
//            chatArea = new JTextArea();
//            chatArea.setEditable(false);
//            add(new JScrollPane(chatArea), BorderLayout.CENTER);
//
//            inputField = new JTextField();
//            inputField.addActionListener(e -> {
//                String message = inputField.getText();
//                addMessage("Me: " + message);
//                out.println("/msg " + recipient + " " + message); // Send private message
//                inputField.setText("");
//            });
//            add(inputField, BorderLayout.SOUTH);
//
//            addWindowListener(new WindowAdapter() {
//                @Override
//                public void windowClosing(WindowEvent e) {
//                    privateChats.remove(recipient); // Remove from open chats on close
//                }
//            });
//
//            setVisible(true);
//        }
//
//        public void addMessage(String message) {
//            chatArea.append(message + "\n");
//        }
//
//        public void toFront() {
//            this.setState(JFrame.NORMAL);
//            this.toFront();
//        }
//    }
//
//    public static void main(String[] args) {
//        SwingUtilities.invokeLater(ChatClient2 ::new);
//    }
//}
