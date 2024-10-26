package server;
/**
 * Name: 9bitbin
 * Project: Messenger App
 */


import java.io.*;
import java.net.*;
import java.util.*;

public class ChatServer {
    private static final int PORT = 12345; // Port number for the server to listen on
    private static final int HISTORY_LIMIT = 50; // Limit for message history
    private static Map<String, ClientHandler> clientHandlers = new HashMap<>(); // Track clients by username
    private static List<String> messageHistory = new ArrayList<>(); // List to store chat message history
    private static final String USERS_FILE = "src/server/users.txt"; // File path for storing user credentials

    public static void main(String[] args) {
        System.out.println("Chat server started...");
        try (ServerSocket serverSocket = new ServerSocket(PORT)) { // Create server socket to listen for connections
            while (true) {
                Socket clientSocket = serverSocket.accept(); // Accept a new client connection
                ClientHandler clientHandler = new ClientHandler(clientSocket); // Create handler for new client
                clientHandler.start(); // Start a new thread for handling client
            }
        } catch (IOException e) {
            e.printStackTrace(); // Print error if server fails
        }
    }

    // Broadcast a message to all clients
    public static void broadcastMessage(String message, ClientHandler sender) {
        synchronized (messageHistory) { // Synchronize access to message history
            if (messageHistory.size() >= HISTORY_LIMIT) {
                messageHistory.remove(0); // Remove oldest message if limit is reached
            }
            messageHistory.add(message); // Add new message to history
        }
        synchronized (clientHandlers) { // Synchronize access to client handlers
            for (ClientHandler clientHandler : clientHandlers.values()) {
                if (clientHandler != sender) { // Do not send the message to the sender
                    clientHandler.sendMessage(message); // Send message to other clients
                }
            }
        }
    }

    // Send a private message to a specific client
    public static void sendPrivateMessage(String recipient, String message, ClientHandler sender) {
        ClientHandler recipientHandler = clientHandlers.get(recipient); // Get the recipient's client handler
        if (recipientHandler != null) { // Check if recipient is online
            recipientHandler.sendMessage("Private from " + sender.username + ": " + message); // Send private message to recipient
            sender.sendMessage("Private to " + recipient + ": " + message); // Send confirmation to the sender
        } else {
            sender.sendMessage("User " + recipient + " is not online."); // Inform sender that recipient is not online
        }
    }

    // Broadcast the list of online users to all clients
    public static void broadcastUserList() {
        StringBuilder userList = new StringBuilder("/users "); // Command to indicate user list update
        synchronized (clientHandlers) { // Synchronize access to client handlers
            for (String username : clientHandlers.keySet()) {
                userList.append(username).append(" "); // Append each username to the list
            }
        }
        for (ClientHandler client : clientHandlers.values()) { // Send updated user list to all clients
            client.sendMessage(userList.toString().trim());
        }
    }

    // Validate user login credentials
    private static boolean validateLogin(String username, String password) {
        try (BufferedReader reader = new BufferedReader(new FileReader(USERS_FILE))) { // Read credentials from file
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(":"); // Split username and password
                if (parts.length == 2 && parts[0].equals(username) && parts[1].equals(password)) { // Match credentials
                    return true; // Return true if credentials match
                }
            }
        } catch (IOException e) {
            e.printStackTrace(); // Print error if reading file fails
        }
        return false; // Return false if credentials do not match
    }

    // Register a new user with additional debug output for troubleshooting
    private static boolean registerUser(String username, String password) {
        if (validateLogin(username, password)) { // Check if user already exists
            System.out.println("Sign-up failed: User already exists with username " + username);
            return false; // Return false if user exists
        }
        try {
            File file = new File(USERS_FILE);
            if (!file.exists()) { // Check if users file exists
                if (file.createNewFile()) { // Create the file if it does not exist
                    System.out.println("Created new users.txt file at " + file.getAbsolutePath());
                } else {
                    System.out.println("Failed to create users.txt file.");
                    return false; // Return false if file creation fails
                }
            }
            try (PrintWriter writer = new PrintWriter(new FileWriter(USERS_FILE, true))) { // Append new user to file
                writer.println(username + ":" + password); // Write new user credentials
                System.out.println("User " + username + " successfully registered.");
                return true; // Return true if registration is successful
            }
        } catch (IOException e) {
            e.printStackTrace(); // Print error if writing to file fails
        }
        System.out.println("Sign-up failed due to an unknown error.");
        return false; // Return false if an unknown error occurs
    }

    // Inner class to handle each client's connection
    private static class ClientHandler extends Thread {
        private Socket socket; // Client socket
        private PrintWriter out; // Output stream to client
        private BufferedReader in; // Input stream from client
        private String username; // Username of the connected client

        public ClientHandler(Socket socket) {
            this.socket = socket; // Initialize socket
        }

        @Override
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream())); // Initialize input stream
                out = new PrintWriter(socket.getOutputStream(), true); // Initialize output stream

                if (!authenticateUser()) { // Authenticate user (login/signup)
                    socket.close(); // Close socket if authentication fails
                    return; // Stop execution if authentication fails
                }

                synchronized (clientHandlers) {
                    clientHandlers.put(username, this); // Add the authenticated user to the list of online clients
                    broadcastUserList(); // Broadcast the updated list of online users
                }

                sendHistory(); // Send chat history to the new user
                String message;

                while ((message = in.readLine()) != null) { // Continuously read messages from the client
                    if (message.startsWith("/msg ")) { // Check if the message is a private message
                        String[] parts = message.split(" ", 3);
                        if (parts.length == 3) { // Validate private message format
                            String recipient = parts[1];
                            String privateMessage = parts[2];
                            sendPrivateMessage(recipient, privateMessage, this); // Send private message
                        } else {
                            sendMessage("Invalid private message format. Use: /msg recipient message"); // Inform about invalid format
                        }
                    } else { // Handle public message
                        broadcastMessage(username + ": " + message, this); // Broadcast public message to all clients
                    }
                }
            } catch (IOException e) {
                e.printStackTrace(); // Print error if message handling fails
            } finally {
                try {
                    socket.close(); // Close client socket
                } catch (IOException e) {
                    e.printStackTrace(); // Print error if socket closing fails
                }
                synchronized (clientHandlers) {
                    clientHandlers.remove(username); // Remove client from the list of online users
                    broadcastUserList(); // Update user list when a client disconnects
                }
                System.out.println("Client disconnected: " + username); // Log disconnection
            }
        }

        // Authenticate user with login/signup
        private boolean authenticateUser() throws IOException {
            while (true) { // Loop until authentication succeeds or fails
                String loginType = in.readLine(); // Read login type (LOGIN/SIGNUP)
                String username = in.readLine(); // Read username
                String password = in.readLine(); // Read password

                if (loginType.equals("LOGIN")) { // Handle login request
                    if (validateLogin(username, password)) { // Validate login credentials
                        out.println("SUCCESS"); // Inform client of successful login
                        this.username = username; // Set username
                        System.out.println("User logged in: " + username); // Log successful login
                        return true; // Return true if login is successful
                    } else {
                        out.println("FAIL"); // Inform client of failed login
                    }
                } else if (loginType.equals("SIGNUP")) { // Handle signup request
                    if (registerUser(username, password)) { // Register new user
                        out.println("SUCCESS"); // Inform client of successful signup
                        this.username = username; // Set username
                        System.out.println("User signed up: " + username); // Log successful signup
                        return true; // Return true if signup is successful
                    } else {
                        out.println("FAIL"); // Inform client of failed signup
                    }
                }
            }
        }

        // Send chat history to this client
        private void sendHistory() {
            synchronized (messageHistory) { // Synchronize access to message history
                for (String msg : messageHistory) { // Iterate over each message in history
                    sendMessage(msg); // Send message to client
                }
            }
        }

        // Send a message to this client
        public void sendMessage(String message) {
            out.println(message); // Send message to the client's output stream
        }
    }
}
