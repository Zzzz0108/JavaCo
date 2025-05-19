package com.example.chatroom.client.view;

import com.example.chatroom.client.controller.ClientController;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
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

    public ClientGUI(String host, int port) {
        this.clientController = new ClientController(host, port);
        this.joinedGroups = new HashSet<>();

        setTitle("聊天室客户端");
        setBounds(700, 300, 600, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // 创建主面板
        JPanel mainPanel = new JPanel(new BorderLayout());

        // 顶部：群组选择和控制面板
        JPanel topPanel = new JPanel(new BorderLayout());
        
        // 左侧：群组选择
        JPanel groupSelectPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        groupSelector = new JComboBox<>(new String[]{"公共聊天"});
        groupSelectPanel.add(new JLabel("当前群组："));
        groupSelectPanel.add(groupSelector);
        topPanel.add(groupSelectPanel, BorderLayout.WEST);

        // 右侧：群组控制按钮
        JPanel groupControlPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton createGroupBtn = new JButton("创建群组");
        JButton joinGroupBtn = new JButton("加入群组");
        JButton leaveGroupBtn = new JButton("退出群组");
        JButton listGroupsBtn = new JButton("群组列表");
        
        // 设置按钮大小一致
        Dimension buttonSize = new Dimension(100, 30);
        createGroupBtn.setPreferredSize(buttonSize);
        joinGroupBtn.setPreferredSize(buttonSize);
        leaveGroupBtn.setPreferredSize(buttonSize);
        listGroupsBtn.setPreferredSize(buttonSize);
        
        groupControlPanel.add(createGroupBtn);
        groupControlPanel.add(joinGroupBtn);
        groupControlPanel.add(leaveGroupBtn);
        groupControlPanel.add(listGroupsBtn);
        topPanel.add(groupControlPanel, BorderLayout.EAST);
        
        mainPanel.add(topPanel, BorderLayout.NORTH);

        // 中间：聊天区域
        jta = new JTextArea();
        jta.setEditable(false);
        jta.setFont(new Font("宋体", Font.PLAIN, 14));
        JScrollPane jsp = new JScrollPane(jta);
        mainPanel.add(jsp, BorderLayout.CENTER);

        // 底部：输入区域
        JPanel bottomPanel = new JPanel(new BorderLayout());
        jtf = new JTextField();
        jtf.addKeyListener(this);
        JButton sendBtn = new JButton("发送");
        sendBtn.addActionListener(this::handleSendAction);
        
        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.add(jtf, BorderLayout.CENTER);
        inputPanel.add(sendBtn, BorderLayout.EAST);
        bottomPanel.add(inputPanel, BorderLayout.CENTER);
        
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        add(mainPanel);

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
            if (selectedGroup != null && !selectedGroup.equals("公共聊天")) {
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
                if (message.startsWith("[群组-")) {
                    // 处理群组消息
                    jta.append(message + "\n");
                } else if (message.startsWith("成功创建群组:")) {
                    // 处理创建群组成功消息
                    String[] parts = message.substring("成功创建群组:".length()).trim().split(" - ");
                    if (parts.length == 2) {
                        String groupId = parts[0];
                        joinedGroups.add(groupId);
                        updateGroupSelector();
                        groupSelector.setSelectedItem(groupId); // 自动选择新创建的群组
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
                } else if (message.startsWith("[") && message.contains("私聊说:")) {
                    // 处理私聊消息
                    String sender = message.substring(1, message.indexOf("]"));
                    String content = message.substring(message.indexOf("私聊说:") + 4);
                    clientController.handlePrivateMessage(sender, content);
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

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ENTER) {
            handleSendAction(null);
        }
    }

    @Override public void keyReleased(KeyEvent e) {}
    @Override public void keyTyped(KeyEvent e) {}
}