package com.example.chatroom.client.service;

import javax.sound.sampled.*;
import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class VoiceChatService {
    private static final int SAMPLE_RATE = 44100;
    private static final int SAMPLE_SIZE_IN_BITS = 16;
    private static final int CHANNELS = 1;
    private static final boolean SIGNED = true;
    private static final boolean BIG_ENDIAN = true;
    private static final int BUFFER_SIZE = 4096;
    private static final int PORT = 8888;

    private final ExecutorService executorService;
    private final ConcurrentHashMap<String, DatagramSocket> voiceSessions;
    private boolean isRecording;
    private boolean isPlaying;
    private TargetDataLine microphone;
    private SourceDataLine speakers;
    private DatagramSocket socket;

    public VoiceChatService() {
        this.executorService = Executors.newCachedThreadPool();
        this.voiceSessions = new ConcurrentHashMap<>();
        this.isRecording = false;
        this.isPlaying = false;
    }

    public void startVoiceChat(String targetAddress, int targetPort) {
        try {
            socket = new DatagramSocket();
            voiceSessions.put(targetAddress + ":" + targetPort, socket);
            
            // 启动录音和播放线程
            startRecording(targetAddress, targetPort);
            startPlaying();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void startGroupVoiceChat(String groupId, String[] members) {
        try {
            socket = new DatagramSocket();
            for (String member : members) {
                String[] parts = member.split(":");
                if (parts.length == 2) {
                    voiceSessions.put(member, socket);
                }
            }
            
            // 启动录音和播放线程
            startGroupRecording(groupId, members);
            startPlaying();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void startRecording(String targetAddress, int targetPort) {
        try {
            AudioFormat format = new AudioFormat(SAMPLE_RATE, SAMPLE_SIZE_IN_BITS, CHANNELS, SIGNED, BIG_ENDIAN);
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
            microphone = (TargetDataLine) AudioSystem.getLine(info);
            microphone.open(format);
            microphone.start();

            isRecording = true;
            executorService.execute(() -> {
                byte[] buffer = new byte[BUFFER_SIZE];
                while (isRecording) {
                    int count = microphone.read(buffer, 0, buffer.length);
                    if (count > 0) {
                        try {
                            DatagramPacket packet = new DatagramPacket(buffer, count, 
                                InetAddress.getByName(targetAddress), targetPort);
                            socket.send(packet);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void startGroupRecording(String groupId, String[] members) {
        try {
            AudioFormat format = new AudioFormat(SAMPLE_RATE, SAMPLE_SIZE_IN_BITS, CHANNELS, SIGNED, BIG_ENDIAN);
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
            microphone = (TargetDataLine) AudioSystem.getLine(info);
            microphone.open(format);
            microphone.start();

            isRecording = true;
            executorService.execute(() -> {
                byte[] buffer = new byte[BUFFER_SIZE];
                while (isRecording) {
                    int count = microphone.read(buffer, 0, buffer.length);
                    if (count > 0) {
                        for (String member : members) {
                            String[] parts = member.split(":");
                            if (parts.length == 2) {
                                try {
                                    DatagramPacket packet = new DatagramPacket(buffer, count,
                                        InetAddress.getByName(parts[0]), Integer.parseInt(parts[1]));
                                    socket.send(packet);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void startPlaying() {
        try {
            AudioFormat format = new AudioFormat(SAMPLE_RATE, SAMPLE_SIZE_IN_BITS, CHANNELS, SIGNED, BIG_ENDIAN);
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
            speakers = (SourceDataLine) AudioSystem.getLine(info);
            speakers.open(format);
            speakers.start();

            isPlaying = true;
            executorService.execute(() -> {
                byte[] buffer = new byte[BUFFER_SIZE];
                while (isPlaying) {
                    try {
                        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                        socket.receive(packet);
                        speakers.write(packet.getData(), 0, packet.getLength());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stopVoiceChat() {
        isRecording = false;
        isPlaying = false;
        
        if (microphone != null) {
            microphone.stop();
            microphone.close();
        }
        
        if (speakers != null) {
            speakers.stop();
            speakers.close();
        }
        
        if (socket != null) {
            socket.close();
        }
        
        voiceSessions.clear();
    }

    public void addMemberToGroup(String memberAddress, int memberPort) {
        String key = memberAddress + ":" + memberPort;
        if (!voiceSessions.containsKey(key)) {
            voiceSessions.put(key, socket);
        }
    }

    public void removeMemberFromGroup(String memberAddress, int memberPort) {
        String key = memberAddress + ":" + memberPort;
        voiceSessions.remove(key);
    }
} 