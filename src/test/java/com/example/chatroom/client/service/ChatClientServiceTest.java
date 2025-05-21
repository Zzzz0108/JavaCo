package com.example.chatroom.client.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.net.Socket;

public class ChatClientServiceTest {
    private ChatClientService clientService;
    private ByteArrayOutputStream mockDos;
    private DataOutputStream dos;
    
    @Mock
    private Socket mockSocket;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        mockDos = new ByteArrayOutputStream();
        dos = new DataOutputStream(mockDos);
        clientService = new DefaultChatClientService("localhost", 8888);
    }

    @Test
    void testDisconnect() throws Exception {
        assertDoesNotThrow(() -> clientService.disconnect());
    }
}