package com.example.chatroom.client.controller;

import com.example.chatroom.client.model.ClientModel;
import com.example.chatroom.client.service.DefaultChatClientService;
import com.example.chatroom.client.view.PrivateChatWindow;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ClientController {
    private final ClientModel clientModel;
    private final DefaultChatClientService chatClientService;
    private final Map<String, PrivateChatWindow> privateChatWindows;

    public ClientController(String host, int port) {
        this.clientModel = new ClientModel();
        this.chatClientService = new DefaultChatClientService(host, port);
        this.privateChatWindows = new ConcurrentHashMap<>();
    }

    public boolean login(String username, String password) throws IOException {
        boolean success = chatClientService.connect(username, password);
        if (success) {
            clientModel.setName(username);
        }
        return success;
    }

    public void sendMessage(String message) throws IOException {
        chatClientService.sendMessage(message);
    }

    public void sendPrivateMessage(String targetUser, String message) throws IOException {
        chatClientService.sendMessage("@" + targetUser + "：" + message);
    }

    public void handleSystemCommand(String command) throws IOException {
        if (command.equals("@@anonymous")) {
            clientModel.setAnonymous(!clientModel.isAnonymous());
        }
        chatClientService.handleSystemCommand(command);
    }

    public String receiveMessage() throws IOException {
        return chatClientService.receiveMessage();
    }

    public void disconnect() throws IOException {
        chatClientService.disconnect();
    }

    public boolean isAnonymous() {
        return clientModel.isAnonymous();
    }

    public String getName() {
        return clientModel.getName();
    }

    public void handlePrivateMessage(String sender, String message) {
        if (!PrivateChatWindow.canOpenNewWindow()) {
            PrivateChatWindow.showError();
            return;
        }

        PrivateChatWindow window = privateChatWindows.computeIfAbsent(sender, 
            name -> new PrivateChatWindow(name, this));
        window.appendMessage(sender + ": " + message);
        window.setVisible(true);
    }
}
