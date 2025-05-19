package com.example.chatroom.client.service;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.Socket;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ChatClientServiceTest {
    @Test
    void testSendMessage() throws IOException {
        // 模拟Socket
        Socket mockSocket = mock(Socket.class);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        when(mockSocket.getOutputStream()).thenReturn(baos);
        when(mockSocket.getInputStream()).thenReturn(new ByteArrayInputStream("success".getBytes()));

        ChatClientService clientService = new ChatClientService("localhost", 1111) {
            @Override
            protected Socket createSocket(String host, int port) {
                return mockSocket;
            }
        };

        assertTrue(clientService.connect("user1", "pass1"));
        clientService.sendMessage("test message");
        assertTrue(baos.toString().contains("test message"));
    }
}