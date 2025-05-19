package com.example.chatroom.server;

import com.example.chatroom.server.view.ServerGUI;

public class ServerMain {
    public static void main(String[] args) {
        int port = 1111; // 默认端口
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("端口号必须是数字，使用默认端口1111");
            }
        }

        new ServerGUI(port).setVisible(true);
    }
}