package com.example.chatroom.server.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

public class ChatGroup {
    private final String groupId;
    private final String groupName;
    private final List<ClientConnection> onlineMembers;
    private final Set<String> allMembers;  // 存储所有成员的用户名

    public ChatGroup(String groupId, String groupName) {
        this.groupId = groupId;
        this.groupName = groupName;
        this.onlineMembers = new ArrayList<>();
        this.allMembers = new HashSet<>();
    }

    public String getGroupId() {
        return groupId;
    }

    public String getGroupName() {
        return groupName;
    }

    public List<ClientConnection> getOnlineMembers() {
        return onlineMembers;
    }

    public Set<String> getAllMembers() {
        return allMembers;
    }

    public int getMemberCount() {
        return allMembers.size();
    }

    public void addMember(ClientConnection member) {
        if (!onlineMembers.contains(member)) {
            onlineMembers.add(member);
            allMembers.add(member.getName());
        }
    }

    public void removeMember(ClientConnection member) {
        onlineMembers.remove(member);
        // 注意：这里不删除 allMembers 中的用户名，因为我们要保持成员记录
    }

    public boolean hasMember(ClientConnection member) {
        return onlineMembers.contains(member);
    }

    public boolean hasMember(String username) {
        return allMembers.contains(username);
    }

    // 添加成员（通过用户名）
    public void addMemberByName(String username) {
        allMembers.add(username);
    }

    // 移除成员（通过用户名）
    public void removeMemberByName(String username) {
        allMembers.remove(username);
    }

    // 获取群组信息的字符串表示（用于保存到文件）
    public String toFileString() {
        return groupId + "," + groupName + "," + String.join(";", allMembers);
    }

    // 从文件字符串创建群组
    public static ChatGroup fromFileString(String line) {
        String[] parts = line.split(",");
        if (parts.length >= 2) {
            ChatGroup group = new ChatGroup(parts[0], parts[1]);
            if (parts.length > 2) {
                String[] members = parts[2].split(";");
                for (String member : members) {
                    if (!member.isEmpty()) {
                        group.addMemberByName(member);
                    }
                }
            }
            return group;
        }
        return null;
    }
} 