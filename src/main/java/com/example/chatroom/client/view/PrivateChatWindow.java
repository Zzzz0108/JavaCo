package com.example.chatroom.client.view;

import com.example.chatroom.client.controller.ClientController;
import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;

public class PrivateChatWindow extends JFrame {
    private final JTextArea chatArea;
    private final JTextField messageField;
    private final String targetUser;
    private final ClientController clientController;
    private static int windowCount = 0;
    private static final int MAX_WINDOWS = 5;

    public PrivateChatWindow(String targetUser, ClientController clientController) {
        this.targetUser = targetUser;
        this.clientController = clientController;

        setTitle("与 " + targetUser + " 的私聊");
        setBounds(300 + windowCount * 30, 200 + windowCount * 30, 400, 500);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        // 聊天区域
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setFont(new Font("宋体", Font.PLAIN, 14));
        JScrollPane scrollPane = new JScrollPane(chatArea);
        add(scrollPane, BorderLayout.CENTER);

        // 输入区域
        JPanel inputPanel = new JPanel(new BorderLayout());
        messageField = new JTextField();
        JButton sendButton = new JButton("发送");
        inputPanel.add(messageField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);
        add(inputPanel, BorderLayout.SOUTH);

        // 发送消息
        sendButton.addActionListener(e -> sendMessage());
        messageField.addActionListener(e -> sendMessage());

        // 窗口关闭时减少计数
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                windowCount--;
            }
        });

        windowCount++;
    }

    private void sendMessage() {
        String message = messageField.getText().trim();
        if (!message.isEmpty()) {
            try {
                clientController.sendPrivateMessage(targetUser, message);
                appendMessage("我: " + message);
                messageField.setText("");
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "发送消息失败: " + ex.getMessage());
            }
        }
    }

    public void appendMessage(String message) {
        chatArea.append(message + "\n");
        chatArea.setCaretPosition(chatArea.getDocument().getLength());
    }

    public static boolean canOpenNewWindow() {
        return windowCount < MAX_WINDOWS;
    }

    public static void showError() {
        JOptionPane.showMessageDialog(null, 
            "已达到最大私聊窗口数量限制(" + MAX_WINDOWS + "个)，请关闭一些窗口后再试。",
            "警告",
            JOptionPane.WARNING_MESSAGE);
    }
} 