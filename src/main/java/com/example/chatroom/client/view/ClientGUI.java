package com.example.chatroom.client.view;

import com.example.chatroom.client.controller.ClientController;
import com.example.chatroom.client.service.VoiceChatService;

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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ClientGUI extends JFrame implements KeyListener {
    private final JTextArea jta;
    private final JTextField jtf;
    private final ClientController clientController;
    private final Set<String> joinedGroups;
    private final JComboBox<String> groupSelector;
    private final JComboBox<String> privateChatSelector;
    private final VoiceChatService voiceChatService;
    private JButton voiceChatBtn;
    private JButton groupVoiceChatBtn;
    private boolean isInVoiceChat = false;
    private String currentVoiceChatPartner = null;
    private JButton createGroupButton;
    private JButton joinGroupButton;
    private JButton leaveGroupButton;
    private JButton listGroupsButton;
    private JButton listUsersButton;
    private JButton anonymousButton;
    private JButton quitButton;
    private Map<String, JDialog> groupDialogs = new HashMap<>();
    private Map<String, JTextArea> groupTextAreas = new HashMap<>();
    private JDialog currentDialog = null;

    public ClientGUI(String host, int port) {
        this.clientController = new ClientController(host, port);
        this.joinedGroups = new HashSet<>();
        this.voiceChatService = new VoiceChatService();

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
        privateChatPanel.add(new JLabel("私聊对象："));
        privateChatPanel.add(privateChatSelector);
        topPanel.add(privateChatPanel, BorderLayout.CENTER);

        // 右侧：群组控制按钮和语音聊天按钮
        JPanel groupControlPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 0));
        createGroupButton = new JButton("创建群组");
        joinGroupButton = new JButton("加入群组");
        leaveGroupButton = new JButton("退出群组");
        listGroupsButton = new JButton("群组列表");
        listUsersButton = new JButton("在线用户列表");
        anonymousButton = new JButton("切换聊天方式");
        voiceChatBtn = new JButton("语音聊天");
        groupVoiceChatBtn = new JButton("群组语音");
        
        // 设置按钮大小一致
        Dimension buttonSize = new Dimension(100, 30);
        createGroupButton.setPreferredSize(buttonSize);
        joinGroupButton.setPreferredSize(buttonSize);
        leaveGroupButton.setPreferredSize(buttonSize);
        listGroupsButton.setPreferredSize(buttonSize);
        listUsersButton.setPreferredSize(buttonSize);
        anonymousButton.setPreferredSize(buttonSize);
        voiceChatBtn.setPreferredSize(buttonSize);
        groupVoiceChatBtn.setPreferredSize(buttonSize);
        
        groupControlPanel.add(createGroupButton);
        groupControlPanel.add(joinGroupButton);
        groupControlPanel.add(leaveGroupButton);
        groupControlPanel.add(listGroupsButton);
        groupControlPanel.add(listUsersButton);
        groupControlPanel.add(anonymousButton);
        groupControlPanel.add(voiceChatBtn);
        groupControlPanel.add(groupVoiceChatBtn);
        
        // 添加按钮事件监听器
        voiceChatBtn.addActionListener(e -> handleVoiceChat());
        groupVoiceChatBtn.addActionListener(e -> handleGroupVoiceChat());
        
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

        // 添加群组控制按钮的事件监听
        createGroupButton.addActionListener(e -> {
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

        joinGroupButton.addActionListener(e -> {
            String groupId = JOptionPane.showInputDialog(this, "请输入要加入的群组ID:");
            if (groupId != null && !groupId.isEmpty()) {
                try {
                    clientController.handleSystemCommand("@@join " + groupId);
                } catch (IOException ex) {
                    jta.append("加入群组失败: " + ex.getMessage() + "\n");
                }
            }
        });

        leaveGroupButton.addActionListener(e -> {
            String selectedGroup = (String) groupSelector.getSelectedItem();
            if (selectedGroup != null && !selectedGroup.equals("公共聊天")) {
                try {
                    clientController.handleSystemCommand("@@leave " + selectedGroup);
                } catch (IOException ex) {
                    jta.append("退出群组失败: " + ex.getMessage() + "\n");
                }
            }
        });

        listGroupsButton.addActionListener(e -> {
            try {
                clientController.handleSystemCommand("@@groups");
            } catch (IOException ex) {
                jta.append("获取群组列表失败: " + ex.getMessage() + "\n");
            }
        });

        listUsersButton.addActionListener(e -> {
            try {
                clientController.handleSystemCommand("@@list");
            } catch (IOException ex) {
                jta.append("获取在线用户列表失败: " + ex.getMessage() + "\n");
            }
        });

        anonymousButton.addActionListener(e -> {
            try {
                clientController.handleSystemCommand("@@anonymous");
                jta.append("聊天方式已切换为:" + (clientController.isAnonymous() ? "匿名" : "实名") + "\n");
            } catch (IOException ex) {
                jta.append("切换聊天方式失败: " + ex.getMessage() + "\n");
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
                    // 登录成功后立即获取群组列表和用户列表
                    clientController.handleSystemCommand("@@groups");
                    clientController.handleSystemCommand("@@allusers");
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

    private void handleVoiceChat() {
        String selectedUser = (String) privateChatSelector.getSelectedItem();
        if (selectedUser != null && !selectedUser.equals("选择私聊对象")) {
            if (!isInVoiceChat) {
                try {
                    // 发送语音聊天请求
                    clientController.handleSystemCommand("@@voice|" + selectedUser);
                    voiceChatBtn.setText("结束语音");
                    isInVoiceChat = true;
                    // 保存当前语音聊天的对象
                    currentVoiceChatPartner = selectedUser;
                } catch (IOException ex) {
                    jta.append("启动语音聊天失败: " + ex.getMessage() + "\n");
                }
            } else {
                // 结束语音聊天
                voiceChatService.stopVoiceChat();
                voiceChatBtn.setText("语音聊天");
                isInVoiceChat = false;
                // 发送语音结束消息
                try {
                    clientController.handleSystemCommand("@@voiceend|" + currentVoiceChatPartner);
                    jta.append("[系统消息]：语音通话已结束\n");
                    currentVoiceChatPartner = null;
                } catch (IOException ex) {
                    jta.append("发送语音结束消息失败: " + ex.getMessage() + "\n");
                }
            }
        } else if (isInVoiceChat && currentVoiceChatPartner != null) {
            // 如果正在语音聊天，但当前选择的不是语音对象，使用保存的语音对象
            voiceChatService.stopVoiceChat();
            voiceChatBtn.setText("语音聊天");
            isInVoiceChat = false;
            // 发送语音结束消息
            try {
                clientController.handleSystemCommand("@@voiceend|" + currentVoiceChatPartner);
                jta.append("[系统消息]：语音通话已结束\n");
                currentVoiceChatPartner = null;
            } catch (IOException ex) {
                jta.append("发送语音结束消息失败: " + ex.getMessage() + "\n");
            }
        } else {
            JOptionPane.showMessageDialog(this, "请选择私聊对象");
        }
    }

    private void handleGroupVoiceChat() {
        String selectedGroup = (String) groupSelector.getSelectedItem();
        if (selectedGroup != null && !selectedGroup.equals("公共聊天")) {
            if (!isInVoiceChat) {
                try {
                    // 发送群组语音聊天请求
                    clientController.handleSystemCommand("@@groupvoice|" + selectedGroup);
                    groupVoiceChatBtn.setText("结束群语音");
                    isInVoiceChat = true;
                } catch (IOException ex) {
                    jta.append("启动群组语音聊天失败: " + ex.getMessage() + "\n");
                }
            } else {
                // 结束群组语音聊天
                voiceChatService.stopVoiceChat();
                groupVoiceChatBtn.setText("群组语音");
                isInVoiceChat = false;
                // 发送群组语音结束消息
                try {
                    clientController.handleSystemCommand("@@groupvoiceend|" + selectedGroup);
                    jta.append("[系统消息]：您已退出群组语音通话\n");
                } catch (IOException ex) {
                    jta.append("发送群组语音结束消息失败: " + ex.getMessage() + "\n");
                }
            }
        } else {
            JOptionPane.showMessageDialog(this, "请选择群组");
        }
    }

    private void handleVoiceChatRequest(String message) {
        String[] parts = message.split("\\|");
        if (parts.length == 4) {
            String sender = parts[1];
            String address = parts[2];
            int port = Integer.parseInt(parts[3]);
            
            // 确保切换到发送者的私聊对话框
            privateChatSelector.setSelectedItem(sender);
            
            int choice = JOptionPane.showConfirmDialog(this,
                "收到来自 " + sender + " 的语音聊天请求，是否接受？",
                "语音聊天请求",
                JOptionPane.YES_NO_OPTION);
                
            if (choice == JOptionPane.YES_OPTION) {
                try {
                    voiceChatService.startVoiceChat(address, port);
                    voiceChatBtn.setText("结束语音");
                    isInVoiceChat = true;
                    // 保存当前语音聊天的对象
                    currentVoiceChatPartner = sender;
                } catch (Exception e) {
                    jta.append("启动语音聊天失败: " + e.getMessage() + "\n");
                }
            }
        }
    }

    private void handleGroupVoiceChatRequest(String message) {
        System.out.println("开始处理群组语音聊天请求: " + message);
        String[] parts = message.split("\\|");
        System.out.println("消息部分数量: " + parts.length);
        if (parts.length >= 5) {
            String groupId = parts[1];
            String sender = parts[2];
            String senderAddress = parts[3];
            int senderPort = Integer.parseInt(parts[4]);
            
            System.out.println("解析请求信息 - 群组ID: " + groupId + ", 发送者: " + sender + 
                             ", 地址: " + senderAddress + ", 端口: " + senderPort);
            
            // 确保切换到正确的群组
            groupSelector.setSelectedItem(groupId);
            
            int choice = JOptionPane.showConfirmDialog(this,
                "收到来自 " + sender + " 的群组语音聊天请求，是否加入？",
                "群组语音聊天请求",
                JOptionPane.YES_NO_OPTION);
                
            if (choice == JOptionPane.YES_OPTION) {
                try {
                    System.out.println("用户接受群组语音聊天请求");
                    // 先连接到发起者
                    voiceChatService.startVoiceChat(senderAddress, senderPort);
                    
                    // 如果有其他成员，也连接到他们
                    if (parts.length > 5) {
                        System.out.println("处理其他成员连接");
                        for (int i = 5; i < parts.length; i += 3) {
                            if (i + 2 < parts.length) {
                                String memberAddress = parts[i + 1];
                                int memberPort = Integer.parseInt(parts[i + 2]);
                                System.out.println("添加成员到群组语音: " + memberAddress + ":" + memberPort);
                                voiceChatService.addMemberToGroup(memberAddress, memberPort);
                            }
                        }
                    }
                    
                    groupVoiceChatBtn.setText("结束群语音");
                    isInVoiceChat = true;
                    jta.append("[系统消息]：您已加入群组语音通话\n");
                } catch (Exception e) {
                    System.err.println("启动群组语音聊天失败: " + e.getMessage());
                    e.printStackTrace();
                    jta.append("启动群组语音聊天失败: " + e.getMessage() + "\n");
                }
            } else {
                System.out.println("用户拒绝群组语音聊天请求");
            }
        } else {
            System.err.println("群组语音聊天请求格式错误: " + message);
        }
    }

    private void receiveMessages() {
        try {
            while (true) {
                String message = clientController.receiveMessage();
                System.out.println("收到消息: " + message);  // 调试信息1：显示收到的原始消息

                if (message.equals("===未读消息===")) {
                    jta.append("\n===未读消息===\n");
                } else if (message.startsWith("[历史记录]")) {
                    jta.append(message.substring("[历史记录]".length()) + "\n");
                } else if (message.startsWith("@@file|")) {
                    handleFileReceive(message);
                } else if (message.startsWith("在线用户：")) {
                    // 处理在线用户列表消息
                    System.out.println("收到在线用户列表: " + message);
                    jta.append(message + "\n");
                } else if (message.startsWith("全部用户：")) {
                    // 处理全部用户列表消息
                    System.out.println("收到全部用户列表: " + message);
                    updatePrivateChatSelector(message.substring("全部用户：".length()));
                } else if (message.startsWith("可用群组：")) {
                    // 处理群组列表消息
                    String[] lines = message.split("\n");
                    joinedGroups.clear(); // 清空当前群组列表
                    
                    // 解析群组列表
                    for (int i = 1; i < lines.length; i++) {
                        String line = lines[i].trim();
                        if (!line.isEmpty()) {
                            // 提取群组ID和名称
                            String[] parts = line.split(" - ");
                            if (parts.length >= 2) {
                                String groupId = parts[0];
                                String groupName = parts[1].split(" \\(")[0];
                                // 检查当前用户是否在群组中
                                if (line.contains("(成员:") && line.contains(clientController.getName() + "(在线)")) {
                                    joinedGroups.add(groupId);
                                }
                            }
                        }
                    }
                    
                    // 更新群组选择器
                    updateGroupSelector();
                    jta.append(message + "\n");
                } else if (message.startsWith("[") && message.endsWith("]")) {
                    // 处理群组消息
                    System.out.println("处理群组消息: " + message);  // 调试信息2
                    String selectedGroup = (String) groupSelector.getSelectedItem();
                    System.out.println("当前选择的群组: " + selectedGroup);  // 调试信息3
                    
                    if (selectedGroup != null && !selectedGroup.equals("公共聊天")) {
                        System.out.println("检查群组对话框是否存在: " + selectedGroup);  // 调试信息4
                        System.out.println("当前群组对话框列表: " + groupDialogs.keySet());  // 调试信息5
                        
                        if (groupDialogs.containsKey(selectedGroup)) {
                            System.out.println("找到群组对话框，准备添加消息");  // 调试信息6
                            JTextArea textArea = groupTextAreas.get(selectedGroup);
                            if (textArea != null) {
                                final String finalMessage = message;  // 创建final变量以便在lambda中使用
                                SwingUtilities.invokeLater(() -> {
                                    textArea.append(finalMessage + "\n");
                                    textArea.setCaretPosition(textArea.getDocument().getLength());
                                    groupDialogs.get(selectedGroup).setVisible(true);
                                    System.out.println("消息已添加到对话框: " + finalMessage);  // 调试信息7
                                });
                            } else {
                                System.out.println("群组文本区域不存在");  // 调试信息8
                                jta.append(message + "\n");
                            }
                        } else {
                            System.out.println("群组对话框不存在，添加到主窗口");  // 调试信息9
                            jta.append(message + "\n");
                        }
                    } else {
                        System.out.println("未选择群组或选择了公共聊天，添加到主窗口");  // 调试信息10
                        jta.append(message + "\n");
                    }
                } else if (message.startsWith("成功创建群组:")) {
                    // 处理创建群组成功消息
                    String[] parts = message.substring("成功创建群组:".length()).trim().split(" - ");
                    if (parts.length == 2) {
                        String groupId = parts[0];
                        String groupName = parts[1];
                        joinedGroups.add(groupId);
                        updateGroupSelector();
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
                    // 关闭并移除群组对话框
                    if (groupDialogs.containsKey(groupId)) {
                        groupDialogs.get(groupId).dispose();
                        groupDialogs.remove(groupId);
                        groupTextAreas.remove(groupId);
                    }
                    jta.append(message + "\n");
                } else if (message.startsWith("@@voice|")) {
                    // 处理语音聊天请求
                    System.out.println("收到语音聊天请求: " + message);
                    handleVoiceChatRequest(message);
                } else if (message.startsWith("@@voiceend|")) {
                    // 处理语音聊天结束请求
                    System.out.println("收到语音聊天结束请求: " + message);
                    voiceChatService.stopVoiceChat();
                    voiceChatBtn.setText("语音聊天");
                    isInVoiceChat = false;
                    currentVoiceChatPartner = null;
                } else if (message.startsWith("@@groupvoice|")) {
                    // 处理群组语音聊天请求
                    System.out.println("收到群组语音聊天请求: " + message);
                    handleGroupVoiceChatRequest(message);
                } else if (message.startsWith("@@groupvoiceend|")) {
                    // 处理群组语音聊天结束请求
                    System.out.println("收到群组语音聊天结束请求: " + message);
                    voiceChatService.stopVoiceChat();
                    groupVoiceChatBtn.setText("群组语音");
                    isInVoiceChat = false;
                } else {
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
        
        // 添加已加入的群组
        for (String groupId : joinedGroups) {
            groupSelector.addItem(groupId);
        }
        
        // 如果之前选择的群组仍然存在，保持选择
        if (currentSelection != null && (currentSelection.equals("公共聊天") || joinedGroups.contains(currentSelection))) {
            groupSelector.setSelectedItem(currentSelection);
        }

        // 添加群组选择监听器
        groupSelector.addActionListener(e -> {
            String selectedGroup = (String) groupSelector.getSelectedItem();
            if (selectedGroup != null && !selectedGroup.equals("公共聊天")) {
                // 如果选择了群组，创建或显示对应的对话框
                if (!groupDialogs.containsKey(selectedGroup)) {
                    // 从群组列表中获取群组名称
                    String groupName = "";
                    for (String group : joinedGroups) {
                        if (group.equals(selectedGroup)) {
                            groupName = group; // 暂时使用群组ID作为名称
                            break;
                        }
                    }
                    createGroupDialog(selectedGroup, groupName);
                } else {
                    // 如果对话框已存在，显示它
                    groupDialogs.get(selectedGroup).setVisible(true);
                }
            }
        });
    }

    private void updatePrivateChatSelector(String usersList) {
        System.out.println("更新私聊对象列表，收到用户列表: " + usersList);
        String currentSelection = (String) privateChatSelector.getSelectedItem();
        privateChatSelector.removeAllItems();
        privateChatSelector.addItem("选择私聊对象");
        
        if (usersList == null || usersList.trim().isEmpty()) {
            System.out.println("警告：收到的用户列表为空");
            return;
        }
        
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
        
        System.out.println("更新后的下拉框项目数: " + privateChatSelector.getItemCount());
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

    private void createGroupDialog(String groupId, String groupName) {
        System.out.println("创建群组对话框: " + groupId + " - " + groupName);  // 调试信息12：显示创建对话框
        JDialog dialog = new JDialog(this, groupName, false);
        dialog.setSize(600, 400);
        dialog.setLocationRelativeTo(this);

        JTextArea groupTextArea = new JTextArea();
        groupTextArea.setEditable(false);
        groupTextArea.setFont(new Font("宋体", Font.PLAIN, 14));  // 设置字体
        groupTextArea.setLineWrap(true);  // 启用自动换行
        groupTextArea.setWrapStyleWord(true);  // 按单词换行
        JScrollPane scrollPane = new JScrollPane(groupTextArea);
        
        JTextField messageField = new JTextField();
        JButton sendButton = new JButton("发送");
        
        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.add(messageField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);
        
        dialog.setLayout(new BorderLayout());
        dialog.add(scrollPane, BorderLayout.CENTER);
        dialog.add(inputPanel, BorderLayout.SOUTH);
        
        // 保存对话框和文本区域的引用
        groupDialogs.put(groupId, dialog);
        groupTextAreas.put(groupId, groupTextArea);
        System.out.println("群组对话框创建完成，当前对话框数量: " + groupDialogs.size());  // 调试信息13：显示对话框数量
        System.out.println("当前群组对话框列表: " + groupDialogs.keySet());  // 调试信息14：显示所有对话框
        
        // 添加发送消息的监听器
        ActionListener sendListener = e -> {
            String message = messageField.getText();
            if (!message.isEmpty()) {
                try {
                    System.out.println("发送群组消息: " + groupId + " - " + message);  // 调试信息15：显示发送的消息
                    clientController.sendMessage("#" + groupId + "：" + message);
                    messageField.setText("");
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(dialog, "发送消息失败: " + ex.getMessage());
                }
            }
        };
        
        sendButton.addActionListener(sendListener);
        messageField.addActionListener(sendListener);
        
        dialog.setVisible(true);
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