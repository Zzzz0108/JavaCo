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
        return onlineMembers;
    }

    public List<String> getAllMembers() {
        return new ArrayList<>(members);
    }

    public int getMemberCount() {
        return members.size();
    }

    public void addMember(ClientConnection member) {
        if (!members.contains(member.getName())) {
            members.add(member.getName());
        }
        onlineMembers.add(member);
    }

    public void removeMember(ClientConnection member) {
        onlineMembers.remove(member);
        // 不要从members中移除，保持群组成员记录
    }

    public boolean hasMember(ClientConnection member) {
        return onlineMembers.contains(member);
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