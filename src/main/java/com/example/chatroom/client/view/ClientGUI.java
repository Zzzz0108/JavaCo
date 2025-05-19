package com.example.chatroom.client.view;

import com.example.chatroom.client.controller.ClientController;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.SocketException;
import java.util.HashSet;
import java.util.Set;

public class ClientGUI extends JFrame implements KeyListener {
    private final JTextArea jta;
    private final JTextField jtf;
    private final ClientController clientController;
    private final Set<String> joinedGroups;
    private final JComboBox<String> groupSelector;
    private final JComboBox<String> privateChatSelector;

    public ClientGUI(String host, int port) {
        this.clientController = new ClientController(host, port);
        this.joinedGroups = new HashSet<>();

        setTitle("聊天室客户端");
        setBounds(700, 300, 1100, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // 创建主面板
        JPanel mainPanel = new JPanel(new BorderLayout(5, 5));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 顶部：群组选择和控制面板
        JPanel topPanel = new JPanel(new BorderLayout(5, 0));
        
        // 左侧：群组选择
        JPanel groupSelectPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        groupSelector = new JComboBox<>(new String[]{"公共聊天"});
        groupSelector.setPreferredSize(new Dimension(150, 30));
        groupSelectPanel.add(new JLabel("当前群组："));
        groupSelectPanel.add(groupSelector);
        topPanel.add(groupSelectPanel, BorderLayout.WEST);

        // 中间：私聊对象选择
        JPanel privateChatPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 2, 0));
        privateChatSelector = new JComboBox<>(new String[]{"选择私聊对象"});
        privateChatSelector.setPreferredSize(new Dimension(150, 30));
        JButton refreshUsersBtn = new JButton("刷新用户列表");
        refreshUsersBtn.setPreferredSize(new Dimension(100, 30));
        privateChatPanel.add(new JLabel("私聊对象："));
        privateChatPanel.add(privateChatSelector);
        privateChatPanel.add(refreshUsersBtn);
        topPanel.add(privateChatPanel, BorderLayout.CENTER);

        // 右侧：群组控制按钮
        JPanel groupControlPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 0));
        JButton createGroupBtn = new JButton("创建群组");
        JButton joinGroupBtn = new JButton("加入群组");
        JButton leaveGroupBtn = new JButton("退出群组");
        JButton listGroupsBtn = new JButton("群组列表");
        JButton sendFileBtn = new JButton("发送文件");
        
        // 设置按钮大小一致
        Dimension buttonSize = new Dimension(100, 30);
        createGroupBtn.setPreferredSize(buttonSize);
        joinGroupBtn.setPreferredSize(buttonSize);
        leaveGroupBtn.setPreferredSize(buttonSize);
        listGroupsBtn.setPreferredSize(buttonSize);
        sendFileBtn.setPreferredSize(buttonSize);
        
        groupControlPanel.add(createGroupBtn);
        groupControlPanel.add(joinGroupBtn);
        groupControlPanel.add(leaveGroupBtn);
        groupControlPanel.add(listGroupsBtn);
        groupControlPanel.add(sendFileBtn);
        topPanel.add(groupControlPanel, BorderLayout.EAST);
        
        mainPanel.add(topPanel, BorderLayout.NORTH);

        // 中间：聊天区域
        jta = new JTextArea();
        jta.setEditable(false);
        jta.setFont(new Font("宋体", Font.PLAIN, 14));
        jta.setLineWrap(true);
        jta.setWrapStyleWord(true);
        JScrollPane jsp = new JScrollPane(jta);
        jsp.setPreferredSize(new Dimension(1080, 450));
        mainPanel.add(jsp, BorderLayout.CENTER);

        // 底部：输入区域
        JPanel bottomPanel = new JPanel(new BorderLayout(2, 0));
        jtf = new JTextField();
        jtf.setPreferredSize(new Dimension(1000, 30));
        jtf.addKeyListener(this);
        JButton sendBtn = new JButton("发送");
        sendBtn.setPreferredSize(new Dimension(80, 30));
        sendBtn.addActionListener(this::handleSendAction);
        
        JPanel inputPanel = new JPanel(new BorderLayout(2, 0));
        inputPanel.add(jtf, BorderLayout.CENTER);
        inputPanel.add(sendBtn, BorderLayout.EAST);
        bottomPanel.add(inputPanel, BorderLayout.CENTER);
        
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        add(mainPanel);

        // 添加刷新用户列表按钮的事件监听
        refreshUsersBtn.addActionListener(e -> {
            try {
                // 请求全部用户列表而不是在线用户列表
                clientController.handleSystemCommand("@@allusers");
            } catch (IOException ex) {
                jta.append("获取用户列表失败: " + ex.getMessage() + "\n");
            }
        });

        // 添加群组控制按钮的事件监听
        createGroupBtn.addActionListener(e -> {
            String groupId = JOptionPane.showInputDialog(this, "请输入新群组ID:");
            if (groupId != null && !groupId.isEmpty()) {
                String groupName = JOptionPane.showInputDialog(this, "请输入新群组名称:");
                if (groupName != null && !groupName.isEmpty()) {
                    try {
                        clientController.handleSystemCommand("@@create " + groupId + " " + groupName);
                    } catch (IOException ex) {
                        jta.append("创建群组失败: " + ex.getMessage() + "\n");
                    }
                }
            }
        });

        joinGroupBtn.addActionListener(e -> {
            String groupId = JOptionPane.showInputDialog(this, "请输入要加入的群组ID:");
            if (groupId != null && !groupId.isEmpty()) {
                try {
                    clientController.handleSystemCommand("@@join " + groupId);
                } catch (IOException ex) {
                    jta.append("加入群组失败: " + ex.getMessage() + "\n");
                }
            }
        });

        leaveGroupBtn.addActionListener(e -> {
            String selectedGroup = (String) groupSelector.getSelectedItem();
            if (selectedGroup != null && !selectedGroup.equals("公共聊天")) {
                try {
                    clientController.handleSystemCommand("@@leave " + selectedGroup);
                } catch (IOException ex) {
                    jta.append("退出群组失败: " + ex.getMessage() + "\n");
                }
            }
        });

        listGroupsBtn.addActionListener(e -> {
            try {
                clientController.handleSystemCommand("@@groups");
            } catch (IOException ex) {
                jta.append("获取群组列表失败: " + ex.getMessage() + "\n");
            }
        });

        // 添加发送文件按钮的事件监听
        sendFileBtn.addActionListener(e -> {
            String selectedGroup = (String) groupSelector.getSelectedItem();
            String selectedUser = (String) privateChatSelector.getSelectedItem();
            
            JFileChooser fileChooser = new JFileChooser();
            if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fileChooser.getSelectedFile();
                try {
                    if (selectedUser != null && !selectedUser.equals("选择私聊对象")) {
                        // 发送私聊文件
                        sendPrivateFile(selectedUser, selectedFile);
                    } else if (selectedGroup != null && !selectedGroup.equals("公共聊天")) {
                        // 发送群文件
                        sendGroupFile(selectedGroup, selectedFile);
                    } else {
                        JOptionPane.showMessageDialog(this, "请选择私聊对象或群组");
                    }
                } catch (IOException ex) {
                    jta.append("发送文件失败: " + ex.getMessage() + "\n");
                }
            }
        });

        // 登录
        login();

        // 启动消息接收线程
        new Thread(this::receiveMessages).start();
    }

    private void login() {
        while (true) {
            String name = JOptionPane.showInputDialog(this, "请输入你的用户名:");
            String password = JOptionPane.showInputDialog(this, "请输入你的密码:");

            if (name == null || password == null || name.isEmpty() || password.isEmpty()) {
                JOptionPane.showMessageDialog(this, "用户名和密码不能为空");
                System.exit(0);
            }

            try {
                if (clientController.login(name, password)) {
                    setTitle("聊天室-" + name);
                    break;
                } else {
                    JOptionPane.showMessageDialog(this, "用户名或密码错误，请重新输入。");
                }
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "连接服务器失败: " + e.getMessage());
                System.exit(0);
            }
        }
    }

    private void handleSendAction(ActionEvent e) {
        String text = jtf.getText();
        if (text.isEmpty()) return;

        try {
            String selectedGroup = (String) groupSelector.getSelectedItem();
            String selectedUser = (String) privateChatSelector.getSelectedItem();
            
            if (selectedUser != null && !selectedUser.equals("选择私聊对象")) {
                // 发送私聊消息
                clientController.sendPrivateMessage(selectedUser, text);
            } else if (selectedGroup != null && !selectedGroup.equals("公共聊天")) {
                // 发送群组消息
                clientController.sendMessage("#" + selectedGroup + "：" + text);
            } else if (text.startsWith("@@")) {
                // 处理系统命令
                clientController.handleSystemCommand(text);
                if (text.equals("@@anonymous")) {
                    jta.append("聊天方式已切换为:" + (clientController.isAnonymous() ? "匿名" : "实名") + "\n");
                }
            } else {
                // 发送公共消息
                clientController.sendMessage(text);
            }
            jtf.setText("");
        } catch (IOException ex) {
            jta.append("发送失败: " + ex.getMessage() + "\n");
        }
    }

    private void receiveMessages() {
        try {
            while (true) {
                String message = clientController.receiveMessage();
                if (message.equals("===未读消息===")) {
                    // 处理未读消息分隔符
                    jta.append("\n===未读消息===\n");
                } else if (message.startsWith("[历史记录]")) {
                    // 处理历史记录消息
                    jta.append(message.substring("[历史记录]".length()) + "\n");
                } else if (message.startsWith("@@file|")) {
                    // 处理文件接收请求
                    handleFileReceive(message);
                } else if (message.contains("上传了群文件:") || message.contains("发送了私聊文件:")) {
                    // 处理文件上传消息
                    String fileName;
                    String fileSize;
                    
                    if (message.contains("上传了群文件:")) {
                        String[] parts = message.split("上传了群文件:");
                        fileName = parts[1].substring(0, parts[1].indexOf(" (")).trim();
                        fileSize = parts[1].substring(parts[1].indexOf("(") + 1, parts[1].indexOf(")"));
                    } else {
                        String[] parts = message.split("发送了私聊文件:");
                        fileName = parts[1].substring(0, parts[1].indexOf(" (")).trim();
                        fileSize = parts[1].substring(parts[1].indexOf("(") + 1, parts[1].indexOf(")"));
                    }
                    
                    // 将消息添加到聊天区域
                    jta.append(message + "\n");
                    jta.append("点击这里下载文件: " + fileName + "\n");
                    jta.append("文件大小: " + fileSize + "\n\n");
                    
                    // 添加鼠标点击事件
                    jta.addMouseListener(new java.awt.event.MouseAdapter() {
                        @Override
                        public void mouseClicked(java.awt.event.MouseEvent evt) {
                            int offset = jta.viewToModel2D(evt.getPoint());
                            try {
                                int line = jta.getLineOfOffset(offset);
                                String lineText = jta.getText(jta.getLineStartOffset(line), 
                                                           jta.getLineEndOffset(line) - jta.getLineStartOffset(line));
                                if (lineText.contains("点击这里下载文件:")) {
                                    try {
                                        downloadFile(fileName);
                                    } catch (IOException ex) {
                                        jta.append("下载文件失败: " + ex.getMessage() + "\n");
                                    }
                                }
                            } catch (javax.swing.text.BadLocationException ex) {
                                ex.printStackTrace();
                            }
                        }
                    });
                } else if (message.startsWith("[群组-")) {
                    // 处理群组消息
                    jta.append(message + "\n");
                } else if (message.startsWith("成功创建群组:")) {
                    // 处理创建群组成功消息
                    String[] parts = message.substring("成功创建群组:".length()).trim().split(" - ");
                    if (parts.length == 2) {
                        String groupId = parts[0];
                        joinedGroups.add(groupId);
                        updateGroupSelector();
                        groupSelector.setSelectedItem(groupId);
                        jta.append(message + "\n");
                    }
                } else if (message.startsWith("成功加入群组:")) {
                    // 处理加入群组成功消息
                    String groupId = message.substring("成功加入群组:".length()).trim();
                    joinedGroups.add(groupId);
                    updateGroupSelector();
                    jta.append(message + "\n");
                } else if (message.startsWith("成功离开群组:")) {
                    // 处理离开群组成功消息
                    String groupId = message.substring("成功离开群组:".length()).trim();
                    joinedGroups.remove(groupId);
                    updateGroupSelector();
                    jta.append(message + "\n");
                } else if (message.startsWith("可用群组：")) {
                    // 处理群组列表消息
                    jta.append(message + "\n");
                } else if (message.startsWith("在线用户：")) {
                    // 处理在线用户列表消息
                    updatePrivateChatSelector(message.substring("在线用户：".length()).trim());
                    jta.append(message + "\n");
                } else if (message.startsWith("全部用户：")) {
                    // 处理全部用户列表消息
                    updatePrivateChatSelector(message.substring("全部用户：".length()).trim());
                } else if (message.startsWith("[") && message.contains("私聊说:")) {
                    // 处理私聊消息
                    String sender = message.substring(1, message.indexOf("]"));
                    String content = message.substring(message.indexOf("私聊说:") + 4);
                    clientController.handlePrivateMessage(sender, content);
                    jta.append(message + "\n");
                } else {
                    // 处理普通消息
                    jta.append(message + "\n");
                }
            }
        } catch (SocketException e) {
            jta.append("服务器终止\n");
        } catch (IOException e) {
            jta.append("接收消息错误: " + e.getMessage() + "\n");
        }
    }

    private void updateGroupSelector() {
        String currentSelection = (String) groupSelector.getSelectedItem();
        groupSelector.removeAllItems();
        groupSelector.addItem("公共聊天");
        for (String groupId : joinedGroups) {
            groupSelector.addItem(groupId);
        }
        // 如果之前选择的群组仍然存在，保持选择
        if (currentSelection != null && (currentSelection.equals("公共聊天") || joinedGroups.contains(currentSelection))) {
            groupSelector.setSelectedItem(currentSelection);
        }
    }

    private void updatePrivateChatSelector(String usersList) {
        System.out.println("更新私聊对象列表，收到用户列表: " + usersList);
        String currentSelection = (String) privateChatSelector.getSelectedItem();
        privateChatSelector.removeAllItems();
        privateChatSelector.addItem("选择私聊对象");
        
        String[] users = usersList.split("\\s+");
        System.out.println("解析出的用户数量: " + users.length);
        for (String user : users) {
            if (!user.isEmpty() && !user.equals(clientController.getName())) {
                System.out.println("添加用户到下拉框: " + user);
                privateChatSelector.addItem(user);
            }
        }
        
        // 如果之前选择的用户仍然在线，保持选择
        if (currentSelection != null && !currentSelection.equals("选择私聊对象")) {
            for (int i = 0; i < privateChatSelector.getItemCount(); i++) {
                if (privateChatSelector.getItemAt(i).equals(currentSelection)) {
                    System.out.println("恢复之前选择的用户: " + currentSelection);
                    privateChatSelector.setSelectedItem(currentSelection);
                    break;
                }
            }
        }
    }

    private void sendGroupFile(String groupId, File file) throws IOException {
        String fileName = file.getName();
        System.out.println("开始发送群文件: " + fileName + " 到群组: " + groupId);
        
        // 先发送文件传输命令
        clientController.handleSystemCommand("@@file|group|" + groupId + "|" + fileName);
        
        // 等待一小段时间确保命令被处理
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        // 发送文件数据
        try (FileInputStream fis = new FileInputStream(file)) {
            long fileSize = file.length();
            System.out.println("文件大小: " + formatFileSize(fileSize));
            
            // 发送文件大小
            clientController.getDos().writeLong(fileSize);
            clientController.getDos().flush();
            
            // 发送文件内容
            byte[] buffer = new byte[8192];
            int bytesRead;
            long totalBytesRead = 0;
            while ((bytesRead = fis.read(buffer)) != -1) {
                clientController.getDos().write(buffer, 0, bytesRead);
                totalBytesRead += bytesRead;
                System.out.println("已发送: " + formatFileSize(totalBytesRead) + " / " + formatFileSize(fileSize));
            }
            clientController.getDos().flush();
            System.out.println("文件发送完成");
        }
    }

    private void sendPrivateFile(String receiver, File file) throws IOException {
        String fileName = file.getName();
        System.out.println("开始发送私聊文件: " + fileName + " 给用户: " + receiver);
        
        // 先发送文件传输命令
        clientController.handleSystemCommand("@@file|private|" + receiver + "|" + fileName);
        
        // 等待一小段时间确保命令被处理
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        // 发送文件数据
        try (FileInputStream fis = new FileInputStream(file)) {
            long fileSize = file.length();
            System.out.println("文件大小: " + formatFileSize(fileSize));
            
            // 发送文件大小
            clientController.getDos().writeLong(fileSize);
            clientController.getDos().flush();
            
            // 发送文件内容
            byte[] buffer = new byte[8192];
            int bytesRead;
            long totalBytesRead = 0;
            while ((bytesRead = fis.read(buffer)) != -1) {
                clientController.getDos().write(buffer, 0, bytesRead);
                totalBytesRead += bytesRead;
                System.out.println("已发送: " + formatFileSize(totalBytesRead) + " / " + formatFileSize(fileSize));
            }
            clientController.getDos().flush();
            System.out.println("文件发送完成");
        }
    }

    private void handleFileReceive(String message) {
        try {
            String[] parts = message.split("\\|");
            String sender = parts[1];
            String fileName = parts[2];
            long fileSize = Long.parseLong(parts[3]);

            int choice = JOptionPane.showConfirmDialog(this,
                    "收到来自 " + sender + " 的文件: " + fileName + "\n大小: " + formatFileSize(fileSize) + "\n是否接收？",
                    "文件接收",
                    JOptionPane.YES_NO_OPTION);

            if (choice == JOptionPane.YES_OPTION) {
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setSelectedFile(new File(fileName));
                if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                    File saveFile = fileChooser.getSelectedFile();
                    clientController.handleSystemCommand("@@download|" + fileName);
                    
                    // 接收文件数据
                    long receivedSize = clientController.getDis().readLong();
                    if (receivedSize >= 0) {
                        try (FileOutputStream fos = new FileOutputStream(saveFile)) {
                            byte[] buffer = new byte[8192];
                            long remaining = receivedSize;
                            while (remaining > 0) {
                                int bytesRead = clientController.getDis().read(buffer, 0, (int) Math.min(buffer.length, remaining));
                                if (bytesRead == -1) break;
                                fos.write(buffer, 0, bytesRead);
                                remaining -= bytesRead;
                            }
                        }
                        jta.append("文件已保存到: " + saveFile.getAbsolutePath() + "\n");
                    } else {
                        jta.append("文件下载失败: 文件不存在\n");
                    }
                }
            }
        } catch (IOException e) {
            jta.append("接收文件失败: " + e.getMessage() + "\n");
        }
    }

    private void downloadFile(String fileName) throws IOException {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setSelectedFile(new File(fileName));
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File saveFile = fileChooser.getSelectedFile();
            clientController.handleSystemCommand("@@download|" + fileName);
            
            // 接收文件数据
            long receivedSize = clientController.getDis().readLong();
            if (receivedSize >= 0) {
                try (FileOutputStream fos = new FileOutputStream(saveFile)) {
                    byte[] buffer = new byte[8192];
                    long remaining = receivedSize;
                    while (remaining > 0) {
                        int bytesRead = clientController.getDis().read(buffer, 0, (int) Math.min(buffer.length, remaining));
                        if (bytesRead == -1) break;
                        fos.write(buffer, 0, bytesRead);
                        remaining -= bytesRead;
                    }
                }
                jta.append("文件已保存到: " + saveFile.getAbsolutePath() + "\n");
            } else {
                jta.append("文件下载失败: 文件不存在\n");
            }
        }
    }

    private String formatFileSize(long size) {
        if (size < 1024) return size + " B";
        if (size < 1024 * 1024) return String.format("%.1f KB", size / 1024.0);
        if (size < 1024 * 1024 * 1024) return String.format("%.1f MB", size / (1024.0 * 1024));
        return String.format("%.1f GB", size / (1024.0 * 1024 * 1024));
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ENTER) {
            handleSendAction(null);
        }
    }

    @Override public void keyReleased(KeyEvent e) {}
    @Override public void keyTyped(KeyEvent e) {}
}