package com.example.chatroom.client;

import com.example.chatroom.client.view.ClientGUI;

public class ClientMain {
    public static void main(String[] args) {
        String host = "127.0.0.1";
        int port = 1111;

        if (args.length > 0) {
            host = args[0];
            if (args.length > 1) {
                try {
                    port = Integer.parseInt(args[1]);
                } catch (NumberFormatException e) {
                    System.err.println("端口号必须是数字，使用默认端口1111");
                }
            }
        }

        new ClientGUI(host, port).setVisible(true);
    }
}
