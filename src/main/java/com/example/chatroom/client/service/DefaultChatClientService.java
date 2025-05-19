package com.example.chatroom.client.service;

import java.net.Socket;

public class DefaultChatClientService extends ChatClientService {
    public DefaultChatClientService(String host, int port) {
        super(host, port);
    }

    @Override
    protected Socket createSocket(String host, int port) {
        try {
            return new Socket(host, port);
        } catch (Exception e) {
            throw new RuntimeException("无法创建Socket连接: " + e.getMessage(), e);
        }
    }
} 