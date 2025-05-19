package com.example.chatroom.server.controller;

import com.example.chatroom.server.service.ChatServerService;

public class ServerController {
    private final ChatServerService chatServerService;

    public ServerController(int port) {
        this.chatServerService = new ChatServerService(port);
    }

    public void startServer() {
        chatServerService.startServer();
    }

    public void stopServer() {
        chatServerService.stopServer();
    }

    public String listOnlineUsers() {
        return chatServerService.listOnlineUsers();
    }

    public String listAllUsers() {
        return chatServerService.listAllUsers();
    }

    public void broadcast(String message) {
        chatServerService.broadcast(message);
    }
}
