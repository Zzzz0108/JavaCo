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
        if (message.startsWith("@")) {
            dos.writeUTF("@" + message.substring(1));
        } else {
            dos.writeUTF(message);
        }
        dos.flush();
    }

    public void handleSystemCommand(String command) throws IOException {
        dos.writeUTF(command);
        dos.flush();
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
}