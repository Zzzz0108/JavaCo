package com.example.chatroom.server.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

public class ChatGroup {
    private final String groupId;
    private final String groupName;
    private final List<ClientConnection> onlineMembers;
    private final Set<String> members;  // 存储所有成员的用户名

    public ChatGroup(String groupId, String groupName) {
        this.groupId = groupId;
        this.groupName = groupName;
        this.onlineMembers = new ArrayList<>();
        this.members = new HashSet<>();
    }

    public String getGroupId() {
        return groupId;
    }

    public String getGroupName() {
        return groupName;
    }

    public List<ClientConnection> getOnlineMembers() {
        // 直接返回onlineMembers列表，因为它在用户连接和断开时已经被正确维护
        return new ArrayList<>(onlineMembers);
    }

    public List<String> getAllMembers() {
        return new ArrayList<>(members);
    }

    public int getMemberCount() {
        return members.size();
    }

    public void addMember(ClientConnection member) {
        System.out.println("添加成员到群组 - 成员名: " + member.getName());
        System.out.println("添加前成员数: " + members.size());
        System.out.println("添加前在线成员数: " + onlineMembers.size());
        System.out.println("添加前在线成员列表: " + onlineMembers.stream()
                .map(ClientConnection::getName)
                .collect(java.util.stream.Collectors.joining(", ")));
        
        // 先添加到成员集合
        if (!members.contains(member.getName())) {
            members.add(member.getName());
            System.out.println("添加到成员集合");
        }
        
        // 检查并移除旧的连接（如果存在）
        onlineMembers.removeIf(m -> m != null && m.getName().equals(member.getName()));
        
        // 添加到在线成员列表
        onlineMembers.add(member);
        System.out.println("添加到在线成员列表");
        
        System.out.println("添加后成员数: " + members.size());
        System.out.println("添加后在线成员数: " + onlineMembers.size());
        System.out.println("添加后在线成员列表: " + onlineMembers.stream()
                .map(ClientConnection::getName)
                .collect(java.util.stream.Collectors.joining(", ")));
    }

    public void removeMember(ClientConnection member) {
        if (member != null) {
            onlineMembers.removeIf(m -> m != null && m.getName().equals(member.getName()));
        }
        // 不要从members中移除，保持群组成员记录
    }

    public boolean hasMember(ClientConnection member) {
        return members.contains(member.getName());
    }

    public boolean hasMember(String username) {
        return members.contains(username);
    }

    // 添加成员（通过用户名）
    public void addMemberByName(String username) {
        members.add(username);
    }

    // 移除成员（通过用户名）
    public void removeMemberByName(String username) {
        members.remove(username);
    }

    // 获取群组信息的字符串表示（用于保存到文件）
    public String toFileString() {
        return groupId + "," + groupName + "," + String.join(";", members);
    }

    // 从文件字符串创建群组
    public static ChatGroup fromFileString(String line) {
        String[] parts = line.split(",");
        if (parts.length >= 3) {
            ChatGroup group = new ChatGroup(parts[0], parts[1]);
            // 添加所有成员
            String[] memberNames = parts[2].split(";");
            for (String memberName : memberNames) {
                if (!memberName.isEmpty()) {
                    group.members.add(memberName);
                }
            }
            return group;
        }
        return null;
    }
} 