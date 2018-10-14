package org.team4056.victoryconnect.networking;
import org.team4056.victoryconnect.Client;
import org.team4056.victoryconnect.util.Packet;
import org.team4056.victoryconnect.util.PacketParser;
import sun.plugin2.main.server.HeartbeatThread;

import java.io.*;
import java.net.*;
import java.sql.Time;

public class TCPConnection {

    private String serverIP;
    private String serverPort;

    private Socket clientSocket;
    private BufferedReader dataReader;
    private DataOutputStream outToServer;

    private Thread recieveThread;
    private Client parent;

    public TCPConnection(String serverIP, String serverPort, Client client){
        this.serverIP = serverIP;
        this.serverPort = serverPort;
        this.parent = client;
    }

    public boolean connect(){
        if(clientSocket != null){
            if(clientSocket.isConnected()){
                disconnected();
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



        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }


        return false;
    }

    public boolean disconnected(){

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
                    e.printStackTrace();
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
               }
           }
        });

        heartbeatThread.start();
    }


    public boolean sendPacket(Packet toSend){
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
