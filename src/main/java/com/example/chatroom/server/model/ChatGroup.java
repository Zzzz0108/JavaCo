package com.example.chatroom.server.model;

import java.util.HashSet;
import java.util.Set;

public class ChatGroup {
    private final String groupId;
    private final String groupName;
    private final Set<ClientConnection> members;

    public ChatGroup(String groupId, String groupName) {
        this.groupId = groupId;
        this.groupName = groupName;
        this.members = new HashSet<>();
    }

    public String getGroupId() {
        return groupId;
    }

    public String getGroupName() {
        return groupName;
    }

    public Set<ClientConnection> getMembers() {
        return members;
    }

    public void addMember(ClientConnection member) {
        members.add(member);
    }

    public void removeMember(ClientConnection member) {
        members.remove(member);
    }

    public boolean hasMember(ClientConnection member) {
        return members.contains(member);
    }

    public int getMemberCount() {
        return members.size();
    }
} 