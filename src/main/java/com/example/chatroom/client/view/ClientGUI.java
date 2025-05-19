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

public class ClientGUI extends JFrame implements KeyListener {
    private final JTextArea jta;
    private final JTextField jtf;
    private final ClientController clientController;

    public ClientGUI(String host, int port) {
        this.clientController = new ClientController(host, port);

        setTitle("聊天室客户端");
        setBounds(700, 300, 300, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        jta = new JTextArea();
        jta.setEditable(false);
        JScrollPane jsp = new JScrollPane(jta);
        add(jsp, BorderLayout.CENTER);

        JPanel jp = new JPanel();
        jtf = new JTextField(15);
        jtf.addKeyListener(this);
        JButton jb1 = new JButton("发送信息");
        jb1.addActionListener(this::handleSendAction);
        jp.add(jtf);
        jp.add(jb1);
        add(jp, BorderLayout.SOUTH);

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
        try {
            if (text.startsWith("@@")) {
                clientController.handleSystemCommand(text);
                if (text.equals("@@anonymous")) {
                    jta.append("聊天方式已切换为:" + (clientController.isAnonymous() ? "匿名" : "实名") + "\n");
                } else if (text.equals("@@showanonymous")) {
                    jta.append("当前聊天方式为:" + (clientController.isAnonymous() ? "匿名" : "实名") + "\n");
                }
            } else {
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
                if (message.startsWith("[") && message.contains("私聊说:")) {
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

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ENTER) {
            handleSendAction(null);
        }
    }

    @Override public void keyReleased(KeyEvent e) {}
    @Override public void keyTyped(KeyEvent e) {}
}