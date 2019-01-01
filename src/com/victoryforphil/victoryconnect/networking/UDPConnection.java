package com.victoryforphil.victoryconnect.networking;

import com.victoryforphil.victoryconnect.Client;
import com.victoryforphil.victoryconnect.util.PacketParser;

import java.io.*;
import java.net.*;

public class UDPConnection {

    private String serverIP;
    private String serverPort;

    private Thread recieveThread;
    private Client parent;
    private DatagramSocket clientSocket;
    private int reconnectTime = 100;
    private int reconnectAttempt = 0;
    private boolean isReconnecting = false;

    private byte[] receiveData = new byte[1024];
    private byte[] sendData = new byte[1024];
    private long ping = -1;
    public UDPConnection(String serverIP, String serverPort, Client client){
        this.serverIP = serverIP;
        this.serverPort = serverPort;
        this.parent = client;

    }

    public void connect(){
        try {
            clientSocket = new DatagramSocket();
        } catch (SocketException e) {
            e.printStackTrace();
        }

        startListening();

    }


    private boolean startListening(){
        recieveThread = new Thread(() -> {
            while(clientSocket.isBound()){
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                try {
                    clientSocket.receive(receivePacket);
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                }

                String packetString = new String( receivePacket.getData());
                Packet packet = PacketParser.parseSingle(packetString);
                parent.onPacket(packet);
            }

        });

        recieveThread.start();
        return true;
    }

    public void startHeartbeat(double interval){
        Thread heartbeatThread = new Thread(()->{
           double nextBeat =  System.currentTimeMillis() + (interval - 50);
           while (clientSocket.isBound()){
               if( System.currentTimeMillis() >= nextBeat){
                   sendPacket(new Packet(Packet.DataType.COMMAND, "server/heartbeat", new String[]{System.currentTimeMillis()+"", ping + ""}));
                   nextBeat =  System.currentTimeMillis() + (interval - 50);
                   //System.out.println("ADAD");
               }
           }
        });

        heartbeatThread.start();
    }

    public void recvHeartbeat(long timestamp){
        long currentTime = System.currentTimeMillis();
        ping = currentTime - timestamp;
    }


    public boolean sendPacket(Packet toSend){
        if(clientSocket == null){
            //System.out.println("Not Initd!");
            return false;
        }
        if(!clientSocket.isBound()){
           // System.out.println("Not Connected!");
            return false;
        }
        String stringPacket = toSend.toString();
        try {
            sendData = stringPacket.getBytes();
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getByName(serverIP), Integer.parseInt(serverPort));
            clientSocket.send(sendPacket);
            //System.out.println("UDP Send: " + stringPacket);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
}
