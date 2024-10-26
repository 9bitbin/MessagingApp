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
