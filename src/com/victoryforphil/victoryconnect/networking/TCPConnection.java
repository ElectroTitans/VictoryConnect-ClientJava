package com.victoryforphil.victoryconnect.networking;
import com.victoryforphil.victoryconnect.Client;
import com.victoryforphil.victoryconnect.util.PacketParser;

import java.io.*;
import java.net.*;
import java.util.Base64;

public class TCPConnection {

    private String serverIP;
    private String serverPort;

    private Socket clientSocket;
    private BufferedReader dataReader;
    private DataOutputStream outToServer;

    private Thread recieveThread;
    private Client parent;
    private int reconnectTime = 100;
    private int reconnectAttempt = 0;
    private boolean isReconnecting = false;



    public TCPConnection(String serverIP, String serverPort, Client client){
        this.serverIP = serverIP;
        this.serverPort = serverPort;
        this.parent = client;
    }

    public boolean connect(){
        if(clientSocket != null){
            if(clientSocket.isConnected()){
                disconnect();
            }
            clientSocket = null;
        }


        try {
            clientSocket = new Socket(serverIP, Integer.parseInt(serverPort));
            System.out.println("Connected:  " + clientSocket.isConnected());
            clientSocket.setKeepAlive(true);
            outToServer = new DataOutputStream(clientSocket.getOutputStream());
            dataReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            startListening();
            isReconnecting = false;
            reconnectAttempt = 0;
            return true;


        } catch (IOException e) {
            System.out.println("Error Connecting: " + e.getMessage());
            attemptReconnect();
            return false;
        }



    }

    public boolean attemptReconnect(){
        if(isReconnecting){
            return false;
        }
        disconnect();
        System.out.println("Starting Reconnection process with delay: " + reconnectTime);
        double nextAttempt = System.currentTimeMillis() + reconnectTime;
        boolean isConnected = false;
        isReconnecting = true;
        while(!isConnected){
            if(System.currentTimeMillis() >=nextAttempt){
                nextAttempt = System.currentTimeMillis() + reconnectTime;
                isConnected = connect();
                reconnectAttempt++;
               // System.out.println("\t Reconnect Attempt #"+reconnectAttempt+" result: " + isConnected);
            }
        }
        return false;
    }
    public boolean disconnect(){

        if(clientSocket == null){
            return false;
        }
        if(clientSocket.isConnected() != true){
            return false;
        }

        try {
            clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private boolean startListening(){
        recieveThread = new Thread(() -> {
            while(clientSocket.isConnected()){
                try {
                    if(clientSocket.isConnected()){
                        String packetString = dataReader.readLine();

                        Packet packet = PacketParser.parseSingle(packetString);
                        parent.onPacket(packet);
                    }


                } catch (IOException e) {
                    System.out.println("Error Reading Line: " + e.getMessage());
                    attemptReconnect();
                }

            }
        });

        recieveThread.start();
        return true;
    }

    public void startHeartbeat(double interval){
        Thread heartbeatThread = new Thread(()->{
           double nextBeat =  System.currentTimeMillis() + (interval - 50);
           while (clientSocket.isConnected()){
               if( System.currentTimeMillis() >= nextBeat){
                   sendPacket(new Packet(Packet.DataType.COMMAND, "server/heartbeat", new String[]{System.currentTimeMillis()+""}));
                   nextBeat =  System.currentTimeMillis() + (interval - 50);
               }
           }
        });

        heartbeatThread.start();
    }


    public boolean sendPacket(Packet toSend){
        if(clientSocket == null){
            System.out.println("Not Initd!");
            return false;
        }
        if(!clientSocket.isConnected()){
            System.out.println("Not Connected!");
            return false;
        }
        String stringPacket = toSend.toString();
        try {
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);

            out.println(stringPacket);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
}
