package com.example.chatroom.server.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ChatGroupTest {
    private ChatGroup group;

    @BeforeEach
    void setUp() {
        group = new ChatGroup("1", "测试群组");
    }

    @Test
    void testToFileString() {
        // 只测试文件字符串转换
        group.getAllMembers().add("user1");
        group.getAllMembers().add("user2");
        String fileString = group.toFileString();
        assertTrue(fileString.startsWith("1,测试群组"));
    }

    @Test
    void testFromFileString() {
        String fileString = "2,测试群组2,user1;user2;user3";
        ChatGroup newGroup = ChatGroup.fromFileString(fileString);
        assertNotNull(newGroup);
        assertEquals("2", newGroup.getGroupId());
        assertEquals("测试群组2", newGroup.getGroupName());
        assertEquals(3, newGroup.getMemberCount());
        assertTrue(newGroup.hasMember("user1"));
        assertTrue(newGroup.hasMember("user2"));
        assertTrue(newGroup.hasMember("user3"));
    }

    @Test
    void testInvalidFromFileString() {
        assertNull(ChatGroup.fromFileString("invalid"));
        assertNull(ChatGroup.fromFileString("1,group"));
    }
} 