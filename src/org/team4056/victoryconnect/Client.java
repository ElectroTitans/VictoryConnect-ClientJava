package org.team4056.victoryconnect;

import org.team4056.victoryconnect.listeners.PacketListener;
import org.team4056.victoryconnect.networking.TCPConnection;
import org.team4056.victoryconnect.util.Packet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Client{
    public String ID = "vc-client-java";
    public String name = "Generic VictoryConnect Java Client";
    public HashMap<String, Object> connections = new HashMap<>();

    public HashMap<String, List<PacketListener>> commandListeners = new HashMap<>();

    public Client(){
        addCommandListener("server/welcome", packet -> {
            String conType = packet.data[1];
            Double hbInterval = Double.parseDouble(packet.data[2]);
            int hbRetries = Integer.parseInt(packet.data[3]);

            System.out.println("Heartbeat Information: \n\tConType: "+ conType +"\n\tInterval: " + hbInterval);

            Object supposedConnection = connections.get(conType);
            if(supposedConnection == null){
                System.out.println(conType + " not registered!");
                return;
            }
            switch (conType){
                case "TCP":
                    ((TCPConnection)supposedConnection).startHeartbeat(hbInterval);
                    break;
            }
        });
    }

    public void EnableUDP(String ip, int port){


    }

    public void EnableTCP(String ip, String port){
        TCPConnection tcpConnection = new TCPConnection(ip,port,this);
        tcpConnection.connect();
        tcpConnection.sendPacket(new Packet(Packet.DataType.COMMAND,"server/register", new String[]{ID,name}));
        connections.put("TCP", tcpConnection);


    }

    public void SetDefaultConnection(String conType){

    }



    public void sendPacket(String conType, Packet packet){
        Object supposedConnection = connections.get(conType);
        if(supposedConnection == null){
            System.out.println(conType + " not registered!");
            return;
        }
        switch (conType){
            case "TCP":
                ((TCPConnection)supposedConnection).sendPacket(packet);
                break;
        }
    }
    public void addCommandListener(String command, PacketListener commandListener){
        if(commandListeners.get(command) == null){
            commandListeners.put(command, new ArrayList<>());
        }
        commandListeners.get(command).add(commandListener);
        System.out.println("Registering command listerning: " + command);
    }

    public void onPacket(Packet packet){

        switch (packet.type) {
            case ERROR:
                break;
            case SUBMIT:
                break;
            case REQUEST:
                break;
            case COMMAND:
                onCommand(packet);
                break;
        }
    }

    private void onCommand(Packet commandPacket){
        List<PacketListener> listeners = commandListeners.get(commandPacket.path);
        if(listeners == null){
            return;
        }
        System.out.println("Found Listeners for command: " + commandPacket.path + " - " + listeners.size());
        for(PacketListener listener : listeners){
            listener.onCommand(commandPacket);
        }
    }
}
