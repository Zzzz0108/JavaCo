package com.example.chatroom.server.model;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;

public class ClientConnection {
    private final Socket socket;
    private final DataInputStream dis;
    private final DataOutputStream dos;
    private String name;
    private String anonymousName;
    private boolean isAnonymous = false;

    public ClientConnection(Socket socket, DataInputStream dis, DataOutputStream dos) {
        this.socket = socket;
        this.dis = dis;
        this.dos = dos;
        this.anonymousName = "匿名用户" + (int) (Math.random() * 10000);
    }

    // Getters and Setters
    public Socket getSocket() { return socket; }
    public DataInputStream getDis() { return dis; }
    public DataOutputStream getDos() { return dos; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getAnonymousName() { return anonymousName; }
    public boolean isAnonymous() { return isAnonymous; }
    public void setAnonymous(boolean anonymous) { isAnonymous = anonymous; }
}