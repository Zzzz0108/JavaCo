package com.example.chatroom.server.service;

import com.example.chatroom.server.model.ChatGroup;
import com.example.chatroom.server.model.ClientConnection;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ConcurrentHashMap;

public class ChatServerService {
    private int port;
    private ServerSocket serverSocket;
    private boolean isRunning = false;
    private final List<ClientConnection> clientConnections = new CopyOnWriteArrayList<>();
    private final Map<String, String> users = new HashMap<>();
    private final Map<String, ChatGroup> groups = new ConcurrentHashMap<>();
    private final String usersFile = "users.txt";
    private final String groupsFile = "groups.txt";
    private final String logFile = "log.txt";

    public ChatServerService(int port) {
        this.port = port;
        loadUsers();
        loadGroups();
    }

    public ChatGroup createGroup(String groupId, String groupName) {
        System.out.println("开始创建群组: " + groupId + " - " + groupName);
        ChatGroup group = new ChatGroup(groupId, groupName);
        groups.put(groupId, group);
        System.out.println("群组已添加到内存中");
        saveGroups(); // 保存群组配置
        System.out.println("群组配置已保存到文件");
        return group;
    }

    private void loadGroups() {
        try {
            File dir = new File("src/main/resources");
            File file = new File(dir, groupsFile);
            System.out.println("尝试加载群组文件: " + file.getAbsolutePath());
            
            if (!file.exists()) {
                System.out.println("群组文件不存在，创建默认群组");
                // 创建默认群组
                createGroup("default", "默认群组");
                return;
            }
            
            System.out.println("开始读取群组文件");
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (!line.isEmpty()) {
                        String[] parts = line.split(",");
                        if (parts.length == 2) {
                            String groupId = parts[0].trim();
                            String groupName = parts[1].trim();
                            System.out.println("加载群组: " + groupId + " - " + groupName);
                            createGroup(groupId, groupName);
                        }
                    }
                }
                System.out.println("群组文件加载完成");
            }
        } catch (IOException e) {
            System.err.println("读取群组文件时发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void saveGroups() {
        try {
            // 确保目录存在
            File dir = new File("src/main/resources");
            if (!dir.exists()) {
                System.out.println("创建目录: " + dir.getAbsolutePath());
                dir.mkdirs();
            }
            
            File file = new File(dir, groupsFile);
            System.out.println("保存群组到文件: " + file.getAbsolutePath());
            
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                for (ChatGroup group : groups.values()) {
                    String line = group.getGroupId() + "," + group.getGroupName();
                    System.out.println("写入群组: " + line);
                    writer.write(line);
                    writer.newLine();
                }
                writer.flush();
                System.out.println("群组文件保存完成");
            }
        } catch (IOException e) {
            System.err.println("保存群组文件时发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public ChatGroup getGroup(String groupId) {
        return groups.get(groupId);
    }

    public Collection<ChatGroup> getAllGroups() {
        return groups.values();
    }

    public boolean joinGroup(String groupId, ClientConnection client) {
        System.out.println("尝试加入群组: " + groupId + ", 用户: " + client.getName());
        ChatGroup group = groups.get(groupId);
        if (group != null) {
            group.addMember(client);
            System.out.println("用户 " + client.getName() + " 成功加入群组 " + groupId);
            broadcastToGroup(groupId, client.getName() + " 加入了群组");
            return true;
        }
        System.out.println("加入群组失败: 群组不存在 - " + groupId);
        return false;
    }

    public boolean leaveGroup(String groupId, ClientConnection client) {
        ChatGroup group = groups.get(groupId);
        if (group != null && group.hasMember(client)) {
            group.removeMember(client);
            broadcastToGroup(groupId, client.getName() + " 离开了群组");
            return true;
        }
        return false;
    }

    public void broadcastToGroup(String groupId, String message) {
        ChatGroup group = groups.get(groupId);
        if (group != null) {
            for (ClientConnection member : group.getMembers()) {
                try {
                    member.getDos().writeUTF("[群组-" + group.getGroupName() + "] " + message);
                    member.getDos().flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
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

    private void handleGroupMessage(String message, ClientConnection sender) {
        String[] parts = message.substring(1).split("：", 2);
        if (parts.length == 2) {
            String groupId = parts[0];
            String content = parts[1];
            String displayName = sender.isAnonymous() ? sender.getAnonymousName() : sender.getName();
            broadcastToGroup(groupId, "[" + displayName + "]：" + content);
        }
    }

    private void handleJoinGroup(String message, ClientConnection client) {
        String[] parts = message.split(" ");
        if (parts.length == 2) {
            String groupId = parts[1];
            if (joinGroup(groupId, client)) {
                try {
                    client.getDos().writeUTF("成功加入群组: " + groupId);
                    client.getDos().flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                try {
                    client.getDos().writeUTF("加入群组失败: 群组不存在");
                    client.getDos().flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void handleLeaveGroup(String message, ClientConnection client) {
        String[] parts = message.split(" ");
        if (parts.length == 2) {
            String groupId = parts[1];
            if (leaveGroup(groupId, client)) {
                try {
                    client.getDos().writeUTF("成功离开群组: " + groupId);
                    client.getDos().flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                try {
                    client.getDos().writeUTF("离开群组失败: 群组不存在或您不在该群组中");
                    client.getDos().flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void listGroupsForClient(ClientConnection client) {
        StringBuilder groupList = new StringBuilder("可用群组：\n");
        for (ChatGroup group : groups.values()) {
            groupList.append(group.getGroupId())
                    .append(" - ")
                    .append(group.getGroupName())
                    .append(" (成员数: ")
                    .append(group.getMemberCount())
                    .append(")\n");
        }
        try {
            client.getDos().writeUTF(groupList.toString());
            client.getDos().flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleClientCommunication(ClientConnection connection) {
        try {
            while (isRunning) {
                System.out.println("等待接收客户端消息...");
                String message = connection.getDis().readUTF();
                System.out.println("收到客户端消息: " + message);

                if (message.equals("##exit")) {
                    System.out.println("收到退出命令");
                    handleClientExit(connection);
                    break;
                } else if (message.equals("@@list")) {
                    System.out.println("收到在线用户列表请求");
                    listOnlineUsersForClient(connection);
                } else if (message.equals("@@quit")) {
                    System.out.println("收到退出请求");
                    handleClientQuit(connection);
                    break;
                } else if (message.equals("@@anonymous")) {
                    System.out.println("收到匿名模式切换请求");
                    connection.setAnonymous(!connection.isAnonymous());
                    connection.getDos().writeUTF("已切换到 " + (connection.isAnonymous() ? "匿名" : "实名") + " 聊天模式");
                    connection.getDos().flush();
                } else if (message.startsWith("@@create")) {
                    System.out.println("收到创建群组请求");
                    handleCreateGroup(message, connection);
                } else if (message.startsWith("@@join")) {
                    System.out.println("收到加入群组请求");
                    handleJoinGroup(message, connection);
                } else if (message.startsWith("@@leave")) {
                    System.out.println("收到离开群组请求");
                    handleLeaveGroup(message, connection);
                } else if (message.startsWith("@@groups")) {
                    System.out.println("收到群组列表请求");
                    listGroupsForClient(connection);
                } else if (message.startsWith("@")) {
                    System.out.println("收到私聊消息");
                    handlePrivateMessage(message, connection);
                } else if (message.startsWith("#")) {
                    System.out.println("收到群组消息");
                    handleGroupMessage(message, connection);
                } else {
                    System.out.println("收到普通消息");
                    String displayName = connection.isAnonymous() ? connection.getAnonymousName() : connection.getName();
                    broadcast("[" + displayName + "]：" + message);
                }
            }
        } catch (IOException e) {
            if (isRunning) {
                System.err.println("处理客户端通信时发生错误: " + e.getMessage());
                e.printStackTrace();
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

    private void handleCreateGroup(String message, ClientConnection client) {
        System.out.println("收到创建群组请求: " + message);
        String[] parts = message.split(" ", 3);
        if (parts.length == 3) {
            String groupId = parts[1];
            String groupName = parts[2];
            System.out.println("解析群组信息 - ID: " + groupId + ", 名称: " + groupName);
            
            if (groups.containsKey(groupId)) {
                System.out.println("群组ID已存在: " + groupId);
                try {
                    client.getDos().writeUTF("创建群组失败: 群组ID已存在");
                    client.getDos().flush();
                } catch (IOException e) {
                    System.err.println("发送群组已存在消息失败: " + e.getMessage());
                    e.printStackTrace();
                }
                return;
            }

            try {
                ChatGroup group = createGroup(groupId, groupName);
                System.out.println("成功创建群组: " + groupId + " - " + groupName);
                
                client.getDos().writeUTF("成功创建群组: " + groupId + " - " + groupName);
                client.getDos().flush();
                
                // 自动加入新创建的群组
                boolean joined = joinGroup(groupId, client);
                System.out.println("自动加入群组结果: " + (joined ? "成功" : "失败"));
            } catch (IOException e) {
                System.err.println("创建群组过程中发生错误: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.out.println("创建群组命令格式错误: " + message);
            try {
                client.getDos().writeUTF("创建群组失败: 格式错误，请使用 @@create 群组ID 群组名称");
                client.getDos().flush();
            } catch (IOException e) {
                System.err.println("发送格式错误消息失败: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}