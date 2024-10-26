# Chat Application Server ⭐

Welcome to the **Chat Application Server** project! This project implements a multi-client chat server where users can join, communicate in public chat rooms, or send private messages—all in real time! ✨ The project is designed to create a seamless and engaging chat experience by managing connections, authenticating users, and handling message flows.

## Key Features 🛠️

1. **Client Connections and Thread Handling 🌐**: The server listens on a specific port for incoming client connections, creating a new `ClientHandler` thread for each client. This ensures parallel message processing and a smooth multi-user experience.

2. **Authentication System 🔒**: The server supports both login and sign-up functionalities by validating user credentials against a file containing user details (`users.txt`). This ensures that users are correctly authenticated before they can participate in the chat.

3. **Message Broadcasting 📢**: The server handles two types of messages:
   - **Public Messages**: Broadcast to all online clients to foster community chat.
   - **Private Messages**: Sent directly to a specific user, enabling private conversations.

4. **Chat History Management 📃**: The server maintains a history of the last 50 messages. When new users join, they can view recent messages to understand the context of ongoing conversations—enhancing the user experience.

5. **User List Broadcasting 📜**: The server keeps track of online users and broadcasts an updated list of usernames to all connected clients. This feature ensures that users are always aware of who is currently active in the chat room.

6. **Graceful Disconnection 🔄**: When a client disconnects, the server removes them from the active user list and updates everyone else. This ensures that the chat room reflects the current state accurately, preventing confusion.

## How It Works 🌟
- The `ClientServer.java` file acts as the backbone of the chat system, managing client connections and handling both public and private messages.
- It uses threading to manage multiple clients simultaneously, ensuring that users can interact without delay.
- The server is continuously listening for incoming connections, maintaining a robust and responsive chat environment.

## Usage 📝
- Clone the repository and navigate to the server directory.
- Compile and run the `ClientServer.java` file to start the server.
- Connect multiple clients to the server to start group or private chats.

## Future Enhancements 💡
- **User Interface Improvements**: Add a graphical user interface to make connecting and chatting more user-friendly.
- **Encryption**: Implement message encryption for better security.
- **Notifications**: Add push notifications for new messages or when users join/leave.

## Technologies Used 🧠
- **Java**: Core language used for both server and client implementation.
- **Sockets**: For network communication between server and clients.
- **Multithreading**: To manage multiple clients concurrently.

Feel free to fork contribute or suggest any improvements! 🚀✨

==
Happy chatting! 💬
