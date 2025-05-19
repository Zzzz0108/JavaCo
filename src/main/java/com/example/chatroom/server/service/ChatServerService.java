package com.example.chatroom.server.service;

import com.example.chatroom.server.model.ClientConnection;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class ChatServerService {
    private int port;
    private ServerSocket serverSocket;
    private boolean isRunning = false;
    private final List<ClientConnection> clientConnections = new CopyOnWriteArrayList<>();
    private final Map<String, String> users = new HashMap<>();
    private final String usersFile = "users.txt";
    private final String logFile = "log.txt";

    public ChatServerService(int port) {
        this.port = port;
        loadUsers();
    }

    public void startServer() {
        if (isRunning) return;

        isRunning = true;
        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(port);
                while (isRunning) {
                    Socket socket = serverSocket.accept();
                    DataInputStream dis = new DataInputStream(socket.getInputStream());
                    DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                    handleNewConnection(new ClientConnection(socket, dis, dos));
                }
            } catch (IOException e) {
                if (isRunning) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void handleNewConnection(ClientConnection connection) {
        new Thread(() -> {
            try {
                String ipAddress = connection.getSocket().getInetAddress().getHostAddress();
                String credentials = connection.getDis().readUTF();
                String[] parts = credentials.split(",");

                if (parts.length == 2 && authenticate(parts[0], parts[1])) {
                    connection.getDos().writeUTF("success");
                    connection.setName(parts[0]);

                    synchronized (this) {
                        clientConnections.add(connection);
                        broadcast(connection.getName() + " 加入了聊天室");
                        logToFile("登录成功: 用户[" + connection.getName() + "], IP地址: " + ipAddress +
                                ", 时间: " + getCurrentTime());
                    }

                    handleClientCommunication(connection);
                } else {
                    connection.getDos().writeUTF("fail");
                    logToFile("登录失败: 用户[" + parts[0] + "], IP地址: " + ipAddress +
                            ", 时间: " + getCurrentTime());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void listOnlineUsersForClient(ClientConnection connection) {
        StringBuilder onlineUsers = new StringBuilder("在线用户：");
        for (ClientConnection cc : clientConnections) {
            onlineUsers.append(cc.getName()).append(" ");
        }
        try {
            connection.getDos().writeUTF(onlineUsers.toString());
            connection.getDos().flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handlePrivateMessage(String message, ClientConnection sender) {
        String[] parts = message.substring(1).split("：", 2);
        if (parts.length == 2) {
            String receiverName = parts[0];
            String content = parts[1];

            boolean userFound = false;
            for (ClientConnection cc : clientConnections) {
                if (cc.getName().equals(receiverName)) {
                    try {
                        cc.getDos().writeUTF("[" + sender.getName() + "] 私聊说: " + content);
                        cc.getDos().flush();
                        userFound = true;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                }
            }

            if (!userFound) {
                try {
                    sender.getDos().writeUTF("用户 [" + receiverName + "] 不在线/不存在/为匿名用户");
                    sender.getDos().flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void handleClientCommunication(ClientConnection connection) {
        try {
            while (isRunning) {
                String message = connection.getDis().readUTF();

                if (message.equals("##exit")) {
                    handleClientExit(connection);
                    break;
                } else if (message.equals("@@list")) {
                    listOnlineUsersForClient(connection);
                } else if (message.equals("@@quit")) {
                    handleClientQuit(connection);
                    break;
                } else if (message.equals("@@anonymous")) {
                    connection.setAnonymous(!connection.isAnonymous());
                    connection.getDos().writeUTF("已切换到 " + (connection.isAnonymous() ? "匿名" : "实名") + " 聊天模式");
                    connection.getDos().flush();
                } else if (message.startsWith("@")) {
                    handlePrivateMessage(message, connection);
                } else {
                    String displayName = connection.isAnonymous() ? connection.getAnonymousName() : connection.getName();
                    broadcast("[" + displayName + "]：" + message);
                }
            }
        } catch (IOException e) {
            if (isRunning) {
                handleClientExit(connection);
            }
        }
    }

    private void handleClientExit(ClientConnection connection) {
        clientConnections.remove(connection);
        broadcast(connection.getName() + " 退出了聊天室");
        logToFile("用户退出: 用户[" + connection.getName() + "], 时间: " + getCurrentTime());
    }

    private void handleClientQuit(ClientConnection connection) throws IOException {
        clientConnections.remove(connection);
        broadcast(connection.getName() + " 请求退出聊天室");
        connection.getDos().writeUTF("##exit");
        logToFile("用户退出: 用户[" + connection.getName() + "], 时间: " + getCurrentTime());
    }

    public void broadcast(String message) {
        for (ClientConnection cc : clientConnections) {
            try {
                cc.getDos().writeUTF(message);
                cc.getDos().flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public String listOnlineUsers() {
        StringBuilder onlineUsers = new StringBuilder();
        for (ClientConnection cc : clientConnections) {
            onlineUsers.append(cc.getName()).append(" ");
        }
        return onlineUsers.toString().trim();
    }

    public String listAllUsers() {
        return "全部用户：" + String.join(" ", users.keySet());
    }

    public void stopServer() {
        isRunning = false;
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean authenticate(String username, String password) {
        return users.containsKey(username) && users.get(username).equals(password);
    }

    private void loadUsers() {
        try {
            InputStream is = getClass().getClassLoader().getResourceAsStream(usersFile);
            if (is == null) {
                System.err.println("无法找到用户文件: " + usersFile);
                return;
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split(",");
                    if (parts.length == 2) {
                        users.put(parts[0], parts[1]);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("读取用户文件时发生错误: " + e.getMessage());
        }
    }

    private void logToFile(String message) {
        try (FileWriter fw = new FileWriter(logFile, true);
             BufferedWriter bw = new BufferedWriter(fw)) {
            bw.write(message + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getCurrentTime() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
    }
}