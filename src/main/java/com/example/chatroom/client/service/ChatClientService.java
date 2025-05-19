package com.example.chatroom.client.service;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;

public abstract class ChatClientService {
    private Socket socket;
    private DataInputStream dis;
    private DataOutputStream dos;
    private final String host;
    private final int port;

    public ChatClientService(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public boolean connect(String username, String password) throws IOException {
        socket = new Socket(host, port);
        dis = new DataInputStream(socket.getInputStream());
        dos = new DataOutputStream(socket.getOutputStream());

        dos.writeUTF(username + "," + password);
        dos.flush();

        String response = dis.readUTF();
        return response.equals("success");
    }

    public void sendMessage(String message) throws IOException {
        System.out.println("发送消息: " + message);
        if (message.startsWith("@")) {
            dos.writeUTF("@" + message.substring(1));
        } else {
            dos.writeUTF(message);
        }
        dos.flush();
        System.out.println("消息已发送");
    }

    public void handleSystemCommand(String command) throws IOException {
        System.out.println("发送系统命令: " + command);
        dos.writeUTF(command);
        dos.flush();
        System.out.println("系统命令已发送");
    }

    public String receiveMessage() throws IOException {
        return dis.readUTF();
    }

    public void disconnect() throws IOException {
        if (dos != null) {
            dos.writeUTF("##exit");
            dos.flush();
        }
        if (socket != null) {
            socket.close();
        }
    }

    protected abstract Socket createSocket(String host, int port);

    public DataOutputStream getDos() {
        return dos;
    }

    public DataInputStream getDis() {
        return dis;
    }
}