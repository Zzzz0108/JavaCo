package com.example.chatroom.server.service;

import com.example.chatroom.server.model.ClientConnection;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ChatServerServiceTest {
    private ChatServerService chatServerService;
    private final int testPort = 11111; // 使用不同端口避免冲突

    @BeforeEach
    void setUp() {
        chatServerService = new ChatServerService(testPort);
        chatServerService.startServer();
    }

    @AfterEach
    void tearDown() {
        chatServerService.stopServer();
    }

    @Test
    void testAuthenticate() {
        assertTrue(chatServerService.listAllUsers().contains("user1"));
    }

    @Test
    void testBroadcast() throws Exception {
        // 模拟客户端连接
        Socket mockSocket = mock(Socket.class);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        when(mockSocket.getOutputStream()).thenReturn(baos);
        when(mockSocket.getInputStream()).thenReturn(new ByteArrayInputStream("user1,pass1".getBytes()));

        // 测试广播功能
        chatServerService.broadcast("test message");
        assertTrue(baos.toString().contains("test message"));
    }
}
