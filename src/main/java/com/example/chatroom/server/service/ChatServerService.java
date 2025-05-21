package com.example.chatroom.server.service;

import com.example.chatroom.server.model.ChatGroup;
import com.example.chatroom.server.model.ClientConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ConcurrentHashMap;

public class ChatServerService {
    private static final Logger logger = LoggerFactory.getLogger(ChatServerService.class);
    private int port;
    private ServerSocket serverSocket;
    private boolean isRunning = false;
    private final List<ClientConnection> clientConnections = new CopyOnWriteArrayList<>();
    private final Map<String, String> users = new HashMap<>();
    private final Map<String, ChatGroup> groups = new ConcurrentHashMap<>();
    private final Map<String, List<String>> unreadMessages = new ConcurrentHashMap<>();  // 存储未读消息
    private final String usersFile = "users.txt";
    private final String groupsFile = "groups.txt";
    private final String logFile = "log.txt";
    private final String chatLogsDir = "chat_logs";  // 聊天记录目录
    private final String filesDir = "shared_files";  // 文件存储目录

    public ChatServerService(int port) {
        this.port = port;
        loadUsers();
        loadGroups();
        createChatLogsDirectory();
        createFilesDirectory();
    }

    private void createChatLogsDirectory() {
        File dir = new File(chatLogsDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    private void createFilesDirectory() {
        File dir = new File(filesDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    private void saveChatLog(String username, String message) {
        try {
            File logFile = new File(chatLogsDir, username + "_chat.txt");
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFile, true))) {
                writer.write(getCurrentTime() + " " + message + "\n");
            }
        } catch (IOException e) {
            System.err.println("保存聊天记录失败: " + e.getMessage());
        }
    }

    private void sendChatHistory(ClientConnection connection) {
        try {
            File logFile = new File(chatLogsDir, connection.getName() + "_chat.txt");
            if (logFile.exists()) {
                try (BufferedReader reader = new BufferedReader(new FileReader(logFile))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        connection.getDos().writeUTF("[历史记录] " + line);
                        connection.getDos().flush();
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("发送聊天记录失败: " + e.getMessage());
        }
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
                        ChatGroup group = ChatGroup.fromFileString(line);
                        if (group != null) {
                            System.out.println("加载群组: " + group.getGroupId() + " - " + group.getGroupName() + 
                                             " (成员数: " + group.getMemberCount() + ")");
                            groups.put(group.getGroupId(), group);
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
                    String line = group.toFileString();
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
            System.out.println("找到群组，当前成员数: " + group.getMemberCount());
            System.out.println("当前在线成员数: " + group.getOnlineMembers().size());
            System.out.println("当前在线成员列表: " + group.getOnlineMembers().stream()
                    .map(ClientConnection::getName)
                    .collect(java.util.stream.Collectors.joining(", ")));
            
            // 检查用户是否已经在群组中
            if (group.hasMember(client)) {
                System.out.println("用户已经在群组中，更新在线状态");
                group.addMember(client); // 重新添加以更新在线状态
            } else {
                System.out.println("用户不在群组中，添加新成员");
                group.addMember(client);
            }
            
            System.out.println("用户 " + client.getName() + " 成功加入群组 " + groupId);
            System.out.println("加入后成员数: " + group.getMemberCount());
            System.out.println("加入后在线成员数: " + group.getOnlineMembers().size());
            System.out.println("加入后在线成员列表: " + group.getOnlineMembers().stream()
                    .map(ClientConnection::getName)
                    .collect(java.util.stream.Collectors.joining(", ")));
            
            broadcastToGroup(groupId, client.getName() + " 加入了群组");
            saveGroups(); // 保存群组信息
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
            saveGroups(); // 保存群组信息
            return true;
        }
        return false;
    }

    public void broadcastToGroup(String groupId, String message) {
        ChatGroup group = groups.get(groupId);
        if (group != null) {
            System.out.println("开始广播群组消息 - 群组ID: " + groupId + ", 消息: " + message);  // 调试信息1
            System.out.println("群组成员数量: " + group.getOnlineMembers().size());  // 调试信息2
            
            // 广播给所有在线群组成员
            for (ClientConnection member : group.getOnlineMembers()) {
                try {
                    System.out.println("发送消息给群组成员: " + member.getName());  // 调试信息3
                    member.getDos().writeUTF(message);
                    member.getDos().flush();
                    // 保存聊天记录
                    saveChatLog(member.getName(), message);
                } catch (IOException e) {
                    System.err.println("发送消息给群组成员失败: " + member.getName() + ", 错误: " + e.getMessage());  // 调试信息4
                    e.printStackTrace();
                }
            }
            
            // 为群组中离线的成员保存消息
            for (String username : group.getAllMembers()) {
                if (!isUserOnline(username)) {
                    System.out.println("为离线成员保存消息: " + username);  // 调试信息5
                    addUnreadMessage(username, message);
                }
            }
        } else {
            System.err.println("群组不存在: " + groupId);  // 调试信息6
        }
    }

    public void startServer() {
        if (isRunning) return;

        isRunning = true;
        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(port, 0, InetAddress.getByName("0.0.0.0"));;
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

                    // 发送历史聊天记录
                    sendChatHistory(connection);
                    
                    // 发送未读消息
                    sendUnreadMessages(connection);

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

    private void sendUnreadMessages(ClientConnection connection) {
        String username = connection.getName();
        List<String> messages = unreadMessages.get(username);
        if (messages != null && !messages.isEmpty()) {
            try {
                connection.getDos().writeUTF("===未读消息===");
                connection.getDos().flush();
                
                for (String message : messages) {
                    connection.getDos().writeUTF(message);
                    connection.getDos().flush();
                    // 将未读消息也保存到历史记录中
                    saveChatLog(username, message);
                }
                
                // 清空未读消息
                unreadMessages.remove(username);
            } catch (IOException e) {
                System.err.println("发送未读消息失败: " + e.getMessage());
            }
        }
    }

    private void addUnreadMessage(String username, String message) {
        if (!isUserOnline(username)) {
            unreadMessages.computeIfAbsent(username, k -> new ArrayList<>()).add(message);
            // 同时保存到历史记录
            saveChatLog(username, message);
        }
    }

    private boolean isUserOnline(String username) {
        for (ClientConnection cc : clientConnections) {
            if (cc.getName().equals(username)) {
                return true;
            }
        }
        return false;
    }

    private void listOnlineUsersForClient(ClientConnection connection) {
        StringBuilder onlineUsers = new StringBuilder("在线用户：");
        boolean isFirst = true;
        for (ClientConnection cc : clientConnections) {
            if (!isFirst) {
                onlineUsers.append(" ");
            }
            onlineUsers.append(cc.getName());
            isFirst = false;
        }
        try {
            System.out.println("发送在线用户列表: " + onlineUsers.toString());
            connection.getDos().writeUTF(onlineUsers.toString());
            connection.getDos().flush();
        } catch (IOException e) {
            System.err.println("发送在线用户列表失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handlePrivateMessage(String message, ClientConnection sender) {
        String[] parts = message.substring(1).split("：", 2);
        if (parts.length == 2) {
            String receiverName = parts[0];
            String content = parts[1];
            
            // 发送者看到的格式
            String senderMessage = "[我与" + receiverName + "]：" + content;
            // 接收者看到的格式
            String receiverMessage = "[" + sender.getName() + "对我说]：" + content;

            // 检查接收者是否是已注册用户
            if (!users.containsKey(receiverName)) {
                try {
                    String errorMessage = "用户 [" + receiverName + "] 不存在";
                    sender.getDos().writeUTF("[系统消息]：" + errorMessage);
                    sender.getDos().flush();
                    saveChatLog(sender.getName(), "[系统消息]：" + errorMessage);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return;
            }

            boolean userOnline = false;
            // 尝试发送给在线用户
            for (ClientConnection cc : clientConnections) {
                if (cc.getName().equals(receiverName)) {
                    try {
                        // 发送者看到的消息
                        sender.getDos().writeUTF(senderMessage);
                        sender.getDos().flush();
                        // 接收者看到的消息
                        cc.getDos().writeUTF(receiverMessage);
                        cc.getDos().flush();
                        // 保存聊天记录
                        saveChatLog(receiverName, receiverMessage);
                        saveChatLog(sender.getName(), senderMessage);
                        userOnline = true;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                }
            }

            // 如果用户不在线，保存为未读消息
            if (!userOnline) {
                addUnreadMessage(receiverName, receiverMessage);
                saveChatLog(sender.getName(), senderMessage);
                
                try {
                    String noticeMessage = "用户 [" + receiverName + "] 当前不在线，消息将在其登录后发送";
                    sender.getDos().writeUTF("[系统消息]：" + noticeMessage);
                    sender.getDos().flush();
                    saveChatLog(sender.getName(), "[系统消息]：" + noticeMessage);
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
            ChatGroup group = groups.get(groupId);
            
            if (group != null) {
                // 检查发送者是否是群组成员
                if (group.hasMember(sender.getName())) {
                    String displayName = sender.isAnonymous() ? sender.getAnonymousName() : sender.getName();
                    // 使用简单的消息格式
                    String fullMessage = "[" + displayName + "：" + content + "]";
                    System.out.println("准备广播群组消息: " + fullMessage);
                    System.out.println("群组 " + groupId + " 的在线成员数: " + group.getOnlineMembers().size());
                    System.out.println("群组 " + groupId + " 的所有成员: " + group.getAllMembers());
                    System.out.println("群组 " + groupId + " 的在线成员: " + group.getOnlineMembers().stream()
                            .map(ClientConnection::getName)
                            .collect(java.util.stream.Collectors.joining(", ")));
                    
                    // 获取所有群组成员
                    List<String> allMembers = group.getAllMembers();
                    System.out.println("群组 " + groupId + " 的所有成员: " + allMembers);
                    
                    // 广播给所有在线成员
                    for (ClientConnection cc : clientConnections) {
                        if (allMembers.contains(cc.getName())) {
                            try {
                                System.out.println("发送消息给群组成员: " + cc.getName());
                                cc.getDos().writeUTF(fullMessage);
                                cc.getDos().flush();
                                // 保存聊天记录
                                saveChatLog(cc.getName(), fullMessage);
                            } catch (IOException e) {
                                System.err.println("发送消息给群组成员失败: " + cc.getName() + ", 错误: " + e.getMessage());
                                e.printStackTrace();
                            }
                        }
                    }
                    
                    // 为离线成员保存消息
                    for (String username : allMembers) {
                        if (!isUserOnline(username)) {
                            System.out.println("为离线成员保存消息: " + username);
                            addUnreadMessage(username, fullMessage);
                        }
                    }
                } else {
                    try {
                        String errorMessage = "[系统消息]：您不是该群组成员";
                        sender.getDos().writeUTF(errorMessage);
                        sender.getDos().flush();
                        System.out.println("发送错误消息给用户: " + sender.getName() + ", 消息: " + errorMessage);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            } else {
                try {
                    String errorMessage = "[系统消息]：群组不存在";
                    sender.getDos().writeUTF(errorMessage);
                    sender.getDos().flush();
                    System.out.println("发送错误消息给用户: " + sender.getName() + ", 消息: " + errorMessage);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
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
                    .append(" (成员: ");
            
            // 获取所有成员名称
            List<String> memberNames = new ArrayList<>();
            
            // 获取所有成员
            List<String> allMembers = group.getAllMembers();
            
            // 遍历所有成员，检查在线状态
            for (String member : allMembers) {
                boolean isOnline = false;
                // 检查是否在线
                for (ClientConnection cc : clientConnections) {
                    if (cc.getName().equals(member)) {
                        isOnline = true;
                        break;
                    }
                }
                // 添加成员名称和状态
                memberNames.add(member + (isOnline ? "(在线)" : "(离线)"));
            }
            
            // 添加成员列表
            groupList.append(String.join(", ", memberNames))
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
                } else if (message.startsWith("@@voice|")) {
                    System.out.println("收到语音聊天请求");
                    handleVoiceChat(message, connection);
                } else if (message.startsWith("@@voiceend|")) {
                    System.out.println("收到语音聊天结束请求");
                    handleVoiceChatEnd(message, connection);
                } else if (message.startsWith("@@groupvoice|")) {
                    System.out.println("收到群组语音聊天请求");
                    handleGroupVoiceChat(message, connection);
                } else if (message.startsWith("@@groupvoiceend|")) {
                    System.out.println("收到群组语音聊天结束请求");
                    handleGroupVoiceChatEnd(message, connection);
                } else if (message.startsWith("@@file|")) {
                    System.out.println("收到文件传输请求");
                    handleFileTransfer(message, connection);
                } else if (message.startsWith("@@download|")) {
                    System.out.println("收到文件下载请求");
                    handleFileDownload(message, connection);
                } else if (message.equals("@@list")) {
                    System.out.println("收到在线用户列表请求");
                    listOnlineUsersForClient(connection);
                } else if (message.equals("@@allusers")) {
                    System.out.println("收到全部用户列表请求");
                    listAllUsersForClient(connection);
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
        // 从所有群组中移除该用户的在线状态
        for (ChatGroup group : groups.values()) {
            if (group.hasMember(connection)) {
                group.removeMember(connection);
            }
        }
        
        clientConnections.remove(connection);
        broadcast(connection.getName() + " 退出了聊天室");
        logToFile("用户退出: 用户[" + connection.getName() + "], 时间: " + getCurrentTime());
    }

    private void handleClientQuit(ClientConnection connection) throws IOException {
        // 从所有群组中移除该用户的在线状态
        for (ChatGroup group : groups.values()) {
            if (group.hasMember(connection)) {
                group.removeMember(connection);
            }
        }
        
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
                // 保存聊天记录
                saveChatLog(cc.getName(), message);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        
        // 为离线用户保存消息
        for (String username : users.keySet()) {
            if (!isUserOnline(username)) {
                addUnreadMessage(username, message);
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

    private void listAllUsersForClient(ClientConnection connection) {
        StringBuilder allUsers = new StringBuilder("全部用户：");
        boolean isFirst = true;
        System.out.println("当前用户映射: " + users); // 添加调试信息
        for (String username : users.keySet()) {
            if (!isFirst) {
                allUsers.append(" ");
            }
            allUsers.append(username);
            isFirst = false;
        }
        try {
            System.out.println("发送全部用户列表: " + allUsers.toString());
            connection.getDos().writeUTF(allUsers.toString());
            connection.getDos().flush();
        } catch (IOException e) {
            System.err.println("发送全部用户列表失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleFileTransfer(String message, ClientConnection sender) {
        try {
            String[] parts = message.split("\\|");
            if (parts.length != 4) {
                System.err.println("文件传输命令格式错误: " + message);
                return;
            }

            String type = parts[1];  // "group" 或 "private"
            String target = parts[2];  // 群组ID或接收者用户名
            String fileName = parts[3];
            System.out.println("开始处理文件传输 - 类型: " + type + ", 目标: " + target + ", 文件名: " + fileName);

            // 接收文件数据
            long fileSize = sender.getDis().readLong();
            System.out.println("接收到的文件大小: " + formatFileSize(fileSize));
            
            if (fileSize <= 0) {
                System.err.println("文件大小为0或无效");
                sender.getDos().writeUTF("文件传输失败：文件大小为0");
                sender.getDos().flush();
                return;
            }

            // 创建文件存储目录（如果不存在）
            File dir = new File(filesDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            // 保存文件
            String filePath = filesDir + File.separator + fileName;
            System.out.println("保存文件到: " + filePath);
            
            try (FileOutputStream fos = new FileOutputStream(filePath)) {
                byte[] buffer = new byte[8192];
                long remaining = fileSize;
                long totalBytesRead = 0;
                
                while (remaining > 0) {
                    int bytesRead = sender.getDis().read(buffer, 0, (int) Math.min(buffer.length, remaining));
                    if (bytesRead == -1) break;
                    fos.write(buffer, 0, bytesRead);
                    remaining -= bytesRead;
                    totalBytesRead += bytesRead;
                    System.out.println("已接收: " + formatFileSize(totalBytesRead) + " / " + formatFileSize(fileSize));
                }
                System.out.println("文件保存成功");
            }

            if (type.equals("group")) {
                // 群文件传输
                ChatGroup group = groups.get(target);
                if (group != null) {
                    String fileMessage = "[" + sender.getName() + "] 上传了群文件: " + fileName + " (大小: " + formatFileSize(fileSize) + ")";
                    System.out.println("广播群文件消息: " + fileMessage);
                    broadcastToGroup(target, fileMessage);
                    
                    // 为离线群成员保存文件通知
                    for (String username : group.getAllMembers()) {
                        if (!isUserOnline(username)) {
                            System.out.println("为离线用户 " + username + " 保存文件通知");
                            addUnreadMessage(username, "@@file|" + sender.getName() + "|" + fileName + "|" + fileSize);
                        }
                    }
                } else {
                    System.err.println("群组不存在: " + target);
                    sender.getDos().writeUTF("文件传输失败：群组不存在");
                    sender.getDos().flush();
                }
            } else {
                // 私聊文件传输
                String fileMessage = "[" + sender.getName() + "] 发送了私聊文件: " + fileName + " (大小: " + formatFileSize(fileSize) + ")";
                System.out.println("处理私聊文件: " + fileMessage);
                
                // 发送给在线用户
                boolean userOnline = false;
                for (ClientConnection cc : clientConnections) {
                    if (cc.getName().equals(target)) {
                        System.out.println("发送文件通知给在线用户: " + target);
                        cc.getDos().writeUTF("@@file|" + sender.getName() + "|" + fileName + "|" + fileSize);
                        cc.getDos().flush();
                        userOnline = true;
                        break;
                    }
                }

                // 如果用户不在线，保存为未读消息
                if (!userOnline) {
                    System.out.println("用户 " + target + " 不在线，保存为未读消息");
                    addUnreadMessage(target, "@@file|" + sender.getName() + "|" + fileName + "|" + fileSize);
                }

                // 通知发送者
                System.out.println("通知发送者文件已发送");
                sender.getDos().writeUTF("文件已发送给 " + target);
                sender.getDos().flush();
            }
        } catch (IOException e) {
            System.err.println("文件传输失败: " + e.getMessage());
            e.printStackTrace();
            try {
                sender.getDos().writeUTF("文件传输失败: " + e.getMessage());
                sender.getDos().flush();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    private void handleFileDownload(String message, ClientConnection client) {
        try {
            String fileName = message.split("\\|")[1];
            File file = new File(filesDir, fileName);
            
            if (file.exists() && file.length() > 0) {
                System.out.println("开始发送文件: " + fileName + " (大小: " + formatFileSize(file.length()) + ")");
                // 发送文件大小
                client.getDos().writeLong(file.length());
                
                // 发送文件数据
                try (FileInputStream fis = new FileInputStream(file)) {
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    long totalBytesRead = 0;
                    while ((bytesRead = fis.read(buffer)) != -1) {
                        client.getDos().write(buffer, 0, bytesRead);
                        totalBytesRead += bytesRead;
                        System.out.println("已发送: " + formatFileSize(totalBytesRead) + " / " + formatFileSize(file.length()));
                    }
                }
                client.getDos().flush();
                System.out.println("文件发送完成");
            } else {
                System.err.println("文件不存在或为空: " + fileName);
                client.getDos().writeLong(-1);  // 文件不存在
                client.getDos().flush();
            }
        } catch (IOException e) {
            System.err.println("文件下载失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String formatFileSize(long size) {
        if (size < 1024) return size + " B";
        if (size < 1024 * 1024) return String.format("%.1f KB", size / 1024.0);
        if (size < 1024 * 1024 * 1024) return String.format("%.1f MB", size / (1024.0 * 1024));
        return String.format("%.1f GB", size / (1024.0 * 1024 * 1024));
    }

    private void handleVoiceChat(String message, ClientConnection sender) {
        try {
            String[] parts = message.split("\\|");
            if (parts.length == 2) {
                String targetUsername = parts[1];
                logger.info("处理语音聊天请求 - 发送者: {}, 目标用户: {}", sender.getUsername(), targetUsername);
                
                ClientConnection target = getClientByUsername(targetUsername);
                
                if (target != null) {
                    // 获取发送者的IP和端口
                    String senderAddress = sender.getSocket().getInetAddress().getHostAddress();
                    int senderPort = sender.getVoicePort();
                    logger.info("找到目标用户 - IP: {}, 端口: {}", senderAddress, senderPort);
                    
                    // 发送语音聊天请求给目标用户
                    String voiceRequest = "@@voice|" + sender.getUsername() + "|" + senderAddress + "|" + senderPort;
                    logger.info("发送语音聊天请求: {}", voiceRequest);
                    target.getDos().writeUTF(voiceRequest);
                    target.getDos().flush();
                    
                    // 在私聊对话框中显示语音聊天请求
                    String senderVoiceMessage = "[我与" + targetUsername + "]：发起了语音聊天";
                    String targetVoiceMessage = "[" + sender.getUsername() + "对我说]：发起了语音聊天";
                    
                    // 发送给目标用户
                    target.getDos().writeUTF(targetVoiceMessage);
                    target.getDos().flush();
                    // 发送给发起者
                    sender.getDos().writeUTF(senderVoiceMessage);
                    sender.getDos().flush();
                    
                    // 保存聊天记录
                    saveChatLog(target.getUsername(), targetVoiceMessage);
                    saveChatLog(sender.getUsername(), senderVoiceMessage);
                } else {
                    logger.warn("目标用户不在线: {}", targetUsername);
                    sender.getDos().writeUTF("[系统消息]：用户 [" + targetUsername + "] 不在线");
                    sender.getDos().flush();
                }
            } else {
                logger.error("语音聊天请求格式错误: {}", message);
                sender.getDos().writeUTF("[系统消息]：语音聊天请求格式错误");
                sender.getDos().flush();
            }
        } catch (IOException e) {
            logger.error("处理语音聊天请求失败", e);
            try {
                sender.getDos().writeUTF("[系统消息]：处理语音聊天请求失败 - " + e.getMessage());
                sender.getDos().flush();
            } catch (IOException ex) {
                logger.error("发送错误消息失败", ex);
            }
        }
    }

    private void handleVoiceChatEnd(String message, ClientConnection sender) {
        try {
            String[] parts = message.split("\\|");
            if (parts.length == 2) {
                String targetUsername = parts[1];
                ClientConnection target = getClientByUsername(targetUsername);
                
                if (target != null) {
                    // 发送语音结束消息给双方
                    String endMessage = "@@voiceend|" + sender.getUsername();
                    target.getDos().writeUTF(endMessage);
                    target.getDos().flush();
                    
                    // 在私聊对话框中显示语音结束消息
                    String senderMessage = "[我与" + targetUsername + "]：语音通话已结束";
                    String targetMessage = "[" + sender.getUsername() + "对我说]：语音通话已结束";
                    
                    // 发送给双方
                    sender.getDos().writeUTF(senderMessage);
                    sender.getDos().flush();
                    target.getDos().writeUTF(targetMessage);
                    target.getDos().flush();
                    
                    // 保存聊天记录
                    saveChatLog(sender.getUsername(), senderMessage);
                    saveChatLog(target.getUsername(), targetMessage);
                }
            }
        } catch (IOException e) {
            logger.error("处理语音聊天结束请求失败", e);
        }
    }

    private void handleGroupVoiceChat(String message, ClientConnection sender) {
        try {
            String[] parts = message.split("\\|");
            if (parts.length == 2) {
                String groupId = parts[1];
                ChatGroup group = groups.get(groupId);
                
                System.out.println("处理群组语音聊天请求 - 群组ID: " + groupId);
                System.out.println("群组是否存在: " + (group != null));
                if (group != null) {
                    System.out.println("发送者是否是群组成员: " + group.hasMember(sender.getUsername()));
                    System.out.println("群组所有成员: " + group.getAllMembers());
                }
                
                if (group != null && group.hasMember(sender.getUsername())) {
                    // 获取发送者的IP和端口
                    String senderAddress = sender.getSocket().getInetAddress().getHostAddress();
                    int senderPort = sender.getVoicePort();
                    
                    // 构建群组成员列表
                    StringBuilder memberList = new StringBuilder();
                    List<String> onlineMembers = new ArrayList<>();
                    
                    // 检查所有群组成员中谁在线
                    for (String memberName : group.getAllMembers()) {
                        // 跳过发送者自己
                        if (memberName.equals(sender.getUsername())) {
                            continue;
                        }
                        
                        // 检查成员是否在线
                        for (ClientConnection cc : clientConnections) {
                            if (cc.getName().equals(memberName)) {
                                onlineMembers.add(memberName);
                                memberList.append(memberName).append("|");
                                memberList.append(cc.getSocket().getInetAddress().getHostAddress()).append("|");
                                memberList.append(cc.getVoicePort()).append("|");
                                System.out.println("添加在线成员到列表: " + memberName);
                                break;
                            }
                        }
                    }
                    
                    System.out.println("在线成员列表: " + onlineMembers);
                    
                    // 发送群组语音聊天请求给所有在线成员
                    String request = "@@groupvoice|" + groupId + "|" + sender.getUsername() + "|" + senderAddress + "|" + senderPort + "|" + memberList.toString();
                    System.out.println("发送请求: " + request);
                    
                    // 发送给所有在线成员
                    for (String memberName : onlineMembers) {
                        for (ClientConnection cc : clientConnections) {
                            if (cc.getName().equals(memberName)) {
                                System.out.println("发送请求给成员: " + memberName);
                                cc.getDos().writeUTF(request);
                                cc.getDos().flush();
                                
                                // 在群组中显示语音聊天请求
                                String voiceMessage = "[" + sender.getUsername() + "] 发起了群组语音聊天";
                                cc.getDos().writeUTF(voiceMessage);
                                cc.getDos().flush();
                                break;
                            }
                        }
                    }
                    
                    // 发送给发起者
                    String senderMessage = "[我与群组" + groupId + "]：发起了群组语音聊天";
                    sender.getDos().writeUTF(senderMessage);
                    sender.getDos().flush();
                } else {
                    sender.getDos().writeUTF("[系统消息]：群组不存在或您不是群组成员");
                    sender.getDos().flush();
                }
            }
        } catch (IOException e) {
            logger.error("处理群组语音聊天请求失败", e);
        }
    }

    private void handleGroupVoiceChatEnd(String message, ClientConnection sender) {
        try {
            String[] parts = message.split("\\|");
            if (parts.length == 2) {
                String groupId = parts[1];
                ChatGroup group = groups.get(groupId);
                
                if (group != null) {
                    // 获取当前在线的群组成员数量
                    int onlineMembers = group.getOnlineMembers().size();
                    
                    // 发送退出消息给其他成员
                    String endMessage = "@@groupvoiceend|" + groupId + "|" + sender.getUsername();
                    if (onlineMembers <= 1) {
                        // 如果是最后一个成员，添加last标记
                        endMessage += "|last|";
                    }
                    
                    for (ClientConnection member : group.getOnlineMembers()) {
                        if (!member.getUsername().equals(sender.getUsername())) {
                            member.getDos().writeUTF(endMessage);
                            member.getDos().flush();
                        }
                    }
                    
                    // 在群组中显示退出消息
                    String groupMessage = "[" + sender.getUsername() + "] 退出了群组语音通话";
                    broadcastToGroup(groupId, groupMessage);
                    
                    // 如果是最后一个成员，显示通话结束消息
                    if (onlineMembers <= 1) {
                        broadcastToGroup(groupId, "群组语音通话已结束");
                    }
                }
            }
        } catch (IOException e) {
            logger.error("处理群组语音聊天结束请求失败", e);
        }
    }

    private ClientConnection getClientByUsername(String username) {
        for (ClientConnection cc : clientConnections) {
            if (cc.getName().equals(username)) {
                return cc;
            }
        }
        return null;
    }
}