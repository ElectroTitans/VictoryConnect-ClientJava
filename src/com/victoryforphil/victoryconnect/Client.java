package com.victoryforphil.victoryconnect;

import com.victoryforphil.victoryconnect.listeners.TopicSource;
import com.victoryforphil.victoryconnect.listeners.ClientListener;
import com.victoryforphil.victoryconnect.listeners.MDNSListener;
import com.victoryforphil.victoryconnect.listeners.PacketListener;
import com.victoryforphil.victoryconnect.networking.TCPConnection;
import com.victoryforphil.victoryconnect.networking.Packet;
import com.victoryforphil.victoryconnect.networking.UDPConnection;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceListener;


import java.util.*;

public class Client{
    public String ID = "vc-client-java";
    public String name = "Generic VictoryConnect Java Client";
    public HashMap<String, Object> connections = new HashMap<>();
    public HashMap<String, List<PacketListener>> commandListeners = new HashMap<>();
    public HashMap<String, List<PacketListener>> subscribeListeners = new HashMap<>();
    public List<TopicSource> topicSources = new ArrayList<>();
    public MDNSListener mdnsListener;
    private ClientListener clientListener;
    private String defaultConnection = "TCP";

    private HashMap<String, Packet> packetQueue = new HashMap<>();
    private boolean isASAP = false;

    private int tickRate = 50;
    private Thread tickThread;

    

    public Client(String id, String name){
        this.ID = id;
        this.name = name;
        this.isASAP = true;
        registerCommand("server/welcome", packet -> {
            String conType = packet.data[0];
            Double hbInterval = Double.parseDouble(packet.data[1]);
            int hbRetries = Integer.parseInt(packet.data[2]);

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

                case "UDP":
                    ((UDPConnection)supposedConnection).startHeartbeat(hbInterval);
                    break;
            }

            if(this.clientListener != null){
                clientListener.ready();
            }
        });

        registerCommand("server/hearbeat_resp", packet ->{
            long timestamp = Long.parseLong(packet.data[0]);
            String conType = packet.data[1];
            System.out.println(conType + " - " + timestamp);
            Object supposedConnection = connections.get(conType);
            if(supposedConnection == null){
                System.out.println(conType + " not registered!");
                return;
            }
            switch (conType){
                case "TCP":
                    ((TCPConnection)supposedConnection).recvHeartbeat(timestamp);
                    break;

                case "UDP":
                    ((UDPConnection)supposedConnection).recvHeartbeat(timestamp);
                    break;
            }

        });
        this.isASAP = false;
        startTickLoop();
    }

    public void setListener(ClientListener listener){
        this.clientListener = listener;
    }

    public void enableUDP(String ip, String port){
        UDPConnection udpConnection = new UDPConnection(ip,port, this);
        udpConnection.connect();
        udpConnection.sendPacket(new Packet(Packet.DataType.COMMAND,"server/register", new String[]{ID,name}));
        connections.put("UDP", udpConnection);
        this.defaultConnection = "UDP";
    }

    public void enableTCP(String ip, String port){
        TCPConnection tcpConnection = new TCPConnection(ip,port,this);
        tcpConnection.connect();
        tcpConnection.sendPacket(new Packet(Packet.DataType.COMMAND,"server/register", new String[]{ID,name}));
        connections.put("TCP", tcpConnection);
    }


    public void enableMDNS(MDNSListener listener){
        mdnsListener = listener;
        try {
            // Create a JmDNS instance
            JmDNS jmdns = JmDNS.create(InetAddress.getLocalHost());
            
            // Add a service listener
            jmdns.addServiceListener("_vc-server._tcp.local.", new ServiceListener(){
            
                @Override
                public void serviceResolved(ServiceEvent event) {
                    if(mdnsListener != null){
                        mdnsListener.onService("TCP", event.getInfo().getHostAddresses()[0], Integer.toString(event.getInfo().getPort()));
                    }
                }
            
                @Override
                public void serviceRemoved(ServiceEvent event) {
                    
                }
            
                @Override
                public void serviceAdded(ServiceEvent event) {
                    
                }
            });

            jmdns.addServiceListener("_vc-server._udp.local.", new ServiceListener(){
            
                @Override
                public void serviceResolved(ServiceEvent event) {
                    if(mdnsListener != null){
                        mdnsListener.onService("UDP", event.getInfo().getHostAddresses()[0], Integer.toString(event.getInfo().getPort()));
                    }
                }
            
                @Override
                public void serviceRemoved(ServiceEvent event) {
                    
                }
            
                @Override
                public void serviceAdded(ServiceEvent event) {
                    
                }
            });

           
        } catch (UnknownHostException e) {
            System.out.println("Err : " + e.getMessage());
        } catch (IOException e) {
            System.out.println("Err : " +e.getMessage());
        }
    }
    public void setDefaultConnection(String conType){
        defaultConnection = conType;
    }

    public void enableASAP(){
        isASAP = true;
    }


    public void sendPacket( Packet packet){
        packetQueue.put(packet.path, packet);
        if(isASAP){
            sendQueue();
        }
    }

    public void sendPacket(String protocol, Packet packet){
        packet.setProtocol(protocol);
        sendPacket(packet);
    }

    private void sendQueue(){
    
        Iterator<Packet> iter = packetQueue.values().iterator();

        while (iter.hasNext()) {
            Packet packet = iter.next();

            String conType = packet.protocol;

            if(conType == "DEFAULT"){
                conType = defaultConnection;
            }

            Object supposedConnection = connections.get(conType);
            if(supposedConnection == null && connections.keySet().size() > 0){
                String altCon = (String)connections.keySet().toArray()[0];
                System.out.println(conType + " not registered! Cannot send: " + packet + " Attempting to use: " + altCon );
                if(altCon != ""){
                    conType = altCon;
                    supposedConnection = connections.get(conType);

                }else{
                    return;
                }

               
            }
            if(supposedConnection != null){
                switch (conType) {
                    case "TCP":
                        ((TCPConnection) supposedConnection).sendPacket(packet);
                        break;
                    case "UDP":
                        System.out.println("Using UDP");
                        ((UDPConnection)supposedConnection).sendPacket(packet);
                        break;
                }
            }
            iter.remove();

        }
    }

    public void onPacket(Packet packet){

        switch (packet.type) {
            case ERROR:
                break;
            case SUBMIT:
                onSubmit(packet);
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

    private void onSubmit(Packet packet){
        List<PacketListener> listeners = new ArrayList<>();
        for(String key : subscribeListeners.keySet()){
            if(packet.path.startsWith(key) || key == packet.path || key == "*"){
                listeners.addAll(subscribeListeners.get(key));
            }
        }
        if(listeners == null){
            return;
        }
        System.out.println("Found Listeners for topic: " + packet.path + " - " + listeners.size());
        for(PacketListener listener : listeners){
            listener.onCommand(packet);
        }
    }

    // Network Commands

    public void newTopic(String name, String path, String protocol){
        sendPacket(defaultConnection, new Packet(Packet.DataType.COMMAND, "server/new_topic",new String[]{name, path, protocol}));
    }

    public void setTopic(String path, Object value){
        sendPacket(defaultConnection, new Packet(Packet.DataType.SUBMIT, path,value));
    }

    public void setTopic(String path, Object[] values){
        sendPacket(defaultConnection, new Packet(Packet.DataType.SUBMIT, path,values));
    }

    public void subscribe(String path, PacketListener topicListener){

        if(subscribeListeners.get(path) == null){
            subscribeListeners.put(path, new ArrayList<>());
        }
        subscribeListeners.get(path).add(topicListener);
        sendPacket(defaultConnection, new Packet(Packet.DataType.COMMAND, "server/subscribe", new String[]{path}));
    }

    public void registerCommand(String command, PacketListener commandListener){
        if(commandListeners.get(command) == null){
            commandListeners.put(command, new ArrayList<>());
        }
        commandListeners.get(command).add(commandListener);
        sendPacket(defaultConnection, new Packet(Packet.DataType.COMMAND, "server/command", new String[]{command}));
    }

    public void callCommand(String path, Object value){
        sendPacket(defaultConnection, new Packet(Packet.DataType.COMMAND, path,value));
    }

    public void callCommand(String path, Object[] values){
        sendPacket(defaultConnection, new Packet(Packet.DataType.COMMAND, path,values));
    }

    public void addSource(TopicSource source){
        newTopic(source.getPath(), source.getPath(), source.getConnection());
        topicSources.add(source);
    }

    public void setTickRate(int tickRate){
        sendPacket(defaultConnection, new Packet(Packet.DataType.COMMAND, "server/tickrate",tickRate));
        this.tickRate = tickRate;
        resetTickLoop();
    }
    boolean isTicking = true;
    private void startTickLoop(){
        isTicking = true;
        tickThread = new Thread(new Runnable() {
            @Override
            public void run() {
                int delay = 1000/tickRate;
                long next = System.currentTimeMillis() + delay;
                System.out.println("Starting Tick Loop with Delay: " + delay);
                while(isTicking){
                    if(System.currentTimeMillis() >= next){
                        next = System.currentTimeMillis() + delay;
                        onTick();
                    }
                }
            }
        });

        tickThread.start();
    }

    private void onTick(){

        sendTopicSources();
        sendQueue();
    }

    private void sendTopicSources(){
        for(TopicSource source : topicSources){
            Packet newPacket = new Packet(Packet.DataType.SUBMIT, source.getPath(), source.getData());

            sendPacket(source.getConnection(), newPacket);
        }
    }


    private void resetTickLoop(){
        System.out.println("Reseting Tick Loop.");
        isTicking = false;
        tickThread = null;
        startTickLoop();
    }


}
