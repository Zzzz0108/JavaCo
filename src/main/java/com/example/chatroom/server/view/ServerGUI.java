package com.example.chatroom.server.view;

import com.example.chatroom.server.controller.ServerController;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class ServerGUI extends JFrame {
    private final JTextArea window;
    private final ServerController serverController;
    private final JLabel statusLabel;
    private final JLabel userListLabel;
    private Timer updateTimer;

    public ServerGUI(int port) {
        this.serverController = new ServerController(port);

        setTitle("聊天室服务端 - 端口: " + port);
        setBounds(200, 100, 600, 600);

        // 顶部：状态栏
        JPanel statusPanel = new JPanel(new BorderLayout());
        statusLabel = new JLabel("服务器状态: 未启动");
        userListLabel = new JLabel("在线用户: ");
        statusPanel.add(statusLabel, BorderLayout.WEST);
        statusPanel.add(userListLabel, BorderLayout.EAST);
        add(statusPanel, BorderLayout.NORTH);

        // 中间：日志区域
        window = new JTextArea();
        JScrollPane jsp = new JScrollPane(window);
        window.setEditable(false);
        window.setFont(new Font("宋体", Font.PLAIN, 14));
        add(jsp, BorderLayout.CENTER);

        // 底部：控制面板
        JPanel controlPanel = new JPanel(new BorderLayout());
        
        // 左侧：命令输入区
        JPanel inputPanel = new JPanel();
        JTextField inputField = new JTextField(20);
        JButton sendBtn = new JButton("发送命令");
        inputPanel.add(inputField);
        inputPanel.add(sendBtn);
        controlPanel.add(inputPanel, BorderLayout.CENTER);

        // 右侧：控制按钮
        JPanel buttonPanel = new JPanel();
        JButton stopBtn = new JButton("终止服务器");
        JButton clearBtn = new JButton("清空日志");
        buttonPanel.add(clearBtn);
        buttonPanel.add(stopBtn);
        controlPanel.add(buttonPanel, BorderLayout.EAST);
        
        add(controlPanel, BorderLayout.SOUTH);

        // 事件监听
        sendBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String command = inputField.getText().trim();
                if (command.isEmpty()) return;
                
                switch (command) {
                    case "list":
                        window.append(serverController.listOnlineUsers() + "\n");
                        break;
                    case "listall":
                        window.append(serverController.listAllUsers() + "\n");
                        break;
                    case "quit":
                        serverController.stopServer();
                        window.append("服务器已关闭\n");
                        System.exit(0);
                        break;
                    case "help":
                        showHelp();
                        break;
                    default:
                        window.append("无效命令，输入 'help' 查看可用命令\n");
                        break;
                }
                inputField.setText("");
            }
        });

        stopBtn.addActionListener(e -> {
            int result = JOptionPane.showConfirmDialog(
                this,
                "确定要终止服务器吗？",
                "确认",
                JOptionPane.YES_NO_OPTION
            );
            if (result == JOptionPane.YES_OPTION) {
            serverController.stopServer();
            window.append("服务器已关闭\n");
            System.exit(0);
            }
        });

        clearBtn.addActionListener(e -> {
            window.setText("");
            window.append("日志已清空\n");
        });

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                int result = JOptionPane.showConfirmDialog(
                    ServerGUI.this,
                    "确定要关闭服务器吗？",
                    "确认",
                    JOptionPane.YES_NO_OPTION
                );
                if (result == JOptionPane.YES_OPTION) {
                serverController.stopServer();
                window.append("服务器已关闭\n");
                System.exit(0);
                }
            }
        });

        // 启动服务器
        serverController.startServer();
        statusLabel.setText("服务器状态: 运行中");
        window.append("服务器已启动，等待客户端连接...\n");
        window.append("输入 'help' 查看可用命令\n");

        // 启动定时器更新在线用户列表
        startUpdateTimer();
    }

    private void startUpdateTimer() {
        updateTimer = new Timer(1000, e -> {
            String onlineUsers = serverController.listOnlineUsers();
            userListLabel.setText("在线用户: " + onlineUsers);
        });
        updateTimer.start();
    }

    public void appendLog(String message) {
        SwingUtilities.invokeLater(() -> {
            window.append(message + "\n");
            // 自动滚动到底部
            window.setCaretPosition(window.getDocument().getLength());
        });
    }

    private void showHelp() {
        window.append("\n=== 可用命令 ===\n");
        window.append("list    - 显示当前在线用户\n");
        window.append("listall - 显示所有注册用户\n");
        window.append("quit    - 关闭服务器\n");
        window.append("help    - 显示此帮助信息\n");
        window.append("================\n\n");
    }
}
