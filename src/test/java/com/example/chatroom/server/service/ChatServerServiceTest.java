package com.example.chatroom.server.service;

import com.example.chatroom.server.model.ChatGroup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ChatServerServiceTest {
    private ChatServerService chatServerService;
    private final int testPort = 11111;

    @BeforeEach
    void setUp() {
        chatServerService = new ChatServerService(testPort);
    }

    @Test
    void testCreateGroup() {
        ChatGroup group = chatServerService.createGroup("1", "测试群组");
        assertNotNull(group);
        assertEquals("1", group.getGroupId());
        assertEquals("测试群组", group.getGroupName());
        assertEquals(0, group.getMemberCount());
    }
}
