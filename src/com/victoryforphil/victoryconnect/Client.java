package com.victoryforphil.victoryconnect;

import java.util.concurrent.TimeUnit;
import com.victoryforphil.victoryconnect.listeners.TopicSource;
import com.victoryforphil.logger.VictoryLogger;
import com.victoryforphil.victoryconnect.listeners.ClientListener;
import com.victoryforphil.victoryconnect.listeners.MDNSListener;
import com.victoryforphil.victoryconnect.listeners.PacketListener;
import com.victoryforphil.victoryconnect.networking.TCPConnection;
import com.victoryforphil.victoryconnect.networking.Packet;
import com.victoryforphil.victoryconnect.networking.UDPConnection;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;

import javax.jmdns.JmDNS;
import javax.jmdns.NetworkTopologyEvent;
import javax.jmdns.NetworkTopologyListener;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceListener;
import javax.jmdns.impl.NetworkTopologyDiscoveryImpl;
import javax.jmdns.impl.NetworkTopologyEventImpl;
import javax.jmdns.impl.JmDNSImpl.ServiceTypeEntry;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Client {
    public String ID = "vc-client-java";
    public String name = "Generic VictoryConnect Java Client";
    public HashMap<String, Object> connections = new HashMap<>();
    public HashMap<String, List<PacketListener>> commandListeners = new HashMap<>();
    public HashMap<String, List<PacketListener>> subscribeListeners = new HashMap<>();
    public List<TopicSource> topicSources = new ArrayList<>();
    public MDNSListener mdnsListener;
    public List<ClientListener> clientListeners = new ArrayList<>();
    private String defaultConnection = "TCP";

    private ConcurrentHashMap<String, Packet> packetQueue = new ConcurrentHashMap<>();
    private boolean isASAP = false;

    private int tickRate = 50;
    private Thread tickThread;

    public Client(String id, String name) {
        this.ID = id;
        this.name = name;
        this.isASAP = true;

        VictoryLogger.info("Client", "constructor", "Creating new client with id: " + id + ", name:" + name);

        registerCommand("server/welcome", packet -> {
            String conType = packet.data[0];
            Double hbInterval = Double.parseDouble(packet.data[1]);
            int hbRetries = Integer.parseInt(packet.data[2]);

            VictoryLogger.success("Client", "server/welcome",
                    "Server welcomed " + conType + " with H/B interval: " + hbInterval);

            Object supposedConnection = connections.get(conType);
            if (supposedConnection == null) {
                VictoryLogger.error("Client", "server/welcome",
                        "Server welcomed " + conType + " but that does not exist yet");
                return;
            }
            switch (conType) {
            case "TCP":
                ((TCPConnection) supposedConnection).startHeartbeat(hbInterval);
                break;

            case "UDP":
                ((UDPConnection) supposedConnection).startHeartbeat(hbInterval);
                break;
            }

            if (this.clientListeners.size() > 0) {
                for(ClientListener listener : this.clientListeners){
                    listener.ready();
                }
            }
        });

        registerCommand("server/hearbeat_resp", packet -> {
            long timestamp = Long.parseLong(packet.data[0]);
            String conType = packet.data[1];

            VictoryLogger.debug("Client", "server/hearbeat_resp",
                    "Server replied " + conType + " with H/B timestamp: " + conType);
            // System.out.println(conType + " - " + timestamp);
            Object supposedConnection = connections.get(conType);
            if (supposedConnection == null) {
                VictoryLogger.error("Client", "server/hearbeat_resp",
                        "Server replied " + conType + " but that does not exist yet");
                return;
            }
            switch (conType) {
            case "TCP":
                ((TCPConnection) supposedConnection).recvHeartbeat(timestamp);
                break;

            case "UDP":
                ((UDPConnection) supposedConnection).recvHeartbeat(timestamp);
                break;
            }

        });
        this.isASAP = false;
        startTickLoop();
    }

    public void setListener(ClientListener listener) {
        this.clientListeners.add(listener);
        VictoryLogger.info("Client", "setListener", "Setting listener");
    }

    public void enableUDP(String ip, String port) {
        VictoryLogger.info("Client", "enableUDP", "Enabling UDP to: " + ip + ":" + port);
        UDPConnection udpConnection = new UDPConnection(ip, port, this);
        udpConnection.connect();
        VictoryLogger.success("Client", "enableUDP", "UDP Connected! Sending register packet!");
        udpConnection.sendPacket(new Packet(Packet.DataType.COMMAND, "server/register", new String[] { ID, name }));
        connections.put("UDP", udpConnection);

    }

    public void enableTCP(String ip, String port) {
        VictoryLogger.info("Client", "enableTCP", "Enabling TCP to: " + ip + ":" + port);
        TCPConnection tcpConnection = new TCPConnection(ip, port, this);
        tcpConnection.connect();
        VictoryLogger.success("Client", "enableTCP", "TCP Connected! Sending register packet!");
        tcpConnection.sendPacket(new Packet(Packet.DataType.COMMAND, "server/register", new String[] { ID, name }));
        connections.put("TCP", tcpConnection);
    }

    public void enableMDNS(MDNSListener listener) {
        VictoryLogger.info("Client", "enableMDNS", "Enabling MDNS Support");
        mdnsListener = listener;
        try {

            // Create a JmDNS instance
            JmDNS jmdns = JmDNS.create(Inet4Address.getLocalHost());

            
            // Add a service listener
            jmdns.addServiceListener("_vc-server._tcp.local.", new ServiceListener() {

                @Override
                public void serviceResolved(ServiceEvent event) {
                    System.out.println("Service serviceResolved: " + event.getInfo());

                    VictoryLogger.info("Client", "enableMDNS", "Found TCP service");
                    if (mdnsListener != null) {

                        mdnsListener.onService("TCP", event.getInfo().getHostAddresses()[0],
                                Integer.toString(event.getInfo().getPort()));
                    }
                }

                @Override
                public void serviceRemoved(ServiceEvent event) {
                    System.out.println("Service removed: " + event.getInfo());
                }

                @Override
                public void serviceAdded(ServiceEvent event) {
                    VictoryLogger.info("Client", "enableMDNS", "Found TCP service");
                }
            });

            jmdns.addServiceListener("_vc-server._udp.local", new ServiceListener() {

                @Override
                public void serviceResolved(ServiceEvent event) {
                    VictoryLogger.info("Client", "enableMDNS", "Found UDP service");
                    if (mdnsListener != null) {

                        mdnsListener.onService("UDP", event.getInfo().getHostAddresses()[0],
                                Integer.toString(event.getInfo().getPort()));
                    }
                }

                @Override
                public void serviceRemoved(ServiceEvent event) {

                }

                @Override
                public void serviceAdded(ServiceEvent event) {

                }
            });
         

        } catch (Exception e) {
            System.err.println("UnknownHostException : " + e.getMessage());
            
            try{
                TimeUnit.SECONDS.sleep(1);
                enableMDNS(listener);
            }catch(Exception e2){

            }
           
        } 
    }

    public void setDefaultConnection(String conType) {
        VictoryLogger.info("Client", "setDefaultConnection", "Setting default connection to: " + conType);
        defaultConnection = conType;
    }

    public void enableASAP() {
        isASAP = true;
    }

    

    public void sendPacket(Packet packet) {
        packetQueue.put(packet.path, packet);
        VictoryLogger.debug("Client", "sendPacket",
                "Adding packet to queue: " + packet.path + " using " + packet.protocol);
        if (isASAP) {
            sendQueue();
        }
    }

    public void sendPacket(String protocol, Packet packet) {
        packet.setProtocol(protocol);
        sendPacket(packet);
    }

    private void sendQueue() {

        Iterator<Packet> iter = packetQueue.values().iterator();

        while (iter.hasNext()) {
            Packet packet = iter.next();

            String conType = packet.protocol;

            if (conType == "DEFAULT") {
                conType = defaultConnection;
            }

            Object supposedConnection = connections.get(conType);
            if (supposedConnection == null && connections.keySet().size() > 0) {
                String altCon = (String) connections.keySet().toArray()[0];
                VictoryLogger.warning("Client", "sendQueue",
                        conType + " not registered! Cannot send: " + packet + " Attempting to use: " + altCon);
                // System.out.println(conType + " not registered! Cannot send: " + packet + "
                // Attempting to use: " + altCon );
                if (altCon != "") {
                    conType = altCon;
                    supposedConnection = connections.get(conType);

                } else {

                    return;
                }

            }
            if (supposedConnection != null) {
                switch (conType) {
                case "TCP":
                    ((TCPConnection) supposedConnection).sendPacket(packet);
                    break;
                case "UDP":

                    ((UDPConnection) supposedConnection).sendPacket(packet);
                    break;
                }
            }
            iter.remove();

        }
    }

    public void onPacket(Packet packet) {

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

    private void onCommand(Packet commandPacket) {
        List<PacketListener> listeners = commandListeners.get(commandPacket.path);
        if (listeners == null) {
            return;
        }
        VictoryLogger.debug("Client", "onCommand",
                "Found Listeners for command: " + commandPacket.path + " - " + listeners.size());
        // System.out.println("Found Listeners for command: " + commandPacket.path + " -
        // " + listeners.size());
        for (PacketListener listener : listeners) {
            listener.onCommand(commandPacket);
        }
    }

    private void onSubmit(Packet packet) {
        List<PacketListener> listeners = new ArrayList<>();
        for (String key : subscribeListeners.keySet()) {
            if (packet.path.startsWith(key) || key == packet.path || key == "*") {
                listeners.addAll(subscribeListeners.get(key));
            }
        }
        if (listeners == null) {
            return;
        }
        VictoryLogger.debug("Client", "onSubmit",
                "Found Listeners for command: " + packet.path + " - " + listeners.size());
        for (PacketListener listener : listeners) {
            listener.onCommand(packet);
        }
    }

    // Network Commands

    public void newTopic(String name, String path, String protocol) {
        sendPacket(defaultConnection,
                new Packet(Packet.DataType.COMMAND, "server/new_topic", new String[] { name, path, protocol }));
    }

    public void setTopic(String path, Object value) {
        sendPacket(defaultConnection, new Packet(Packet.DataType.SUBMIT, path, value));
    }

    public void setTopic(String path, Object[] values) {
        sendPacket(defaultConnection, new Packet(Packet.DataType.SUBMIT, path, values));
    }

    public void subscribe(String path, PacketListener topicListener) {

        if (subscribeListeners.get(path) == null) {
            subscribeListeners.put(path, new ArrayList<>());
        }
        subscribeListeners.get(path).add(topicListener);
        sendPacket(defaultConnection, new Packet(Packet.DataType.COMMAND, "server/subscribe", new String[] { path }));
    }

    public void registerCommand(String command, PacketListener commandListener) {
        if (commandListeners.get(command) == null) {
            commandListeners.put(command, new ArrayList<>());
        }
        commandListeners.get(command).add(commandListener);
        sendPacket(defaultConnection, new Packet(Packet.DataType.COMMAND, "server/command", new String[] { command }));
    }

    public void callCommand(String path, Object value) {
        sendPacket(defaultConnection, new Packet(Packet.DataType.COMMAND, path, value));
    }

    public void callCommand(String path, Object[] values) {
        sendPacket(defaultConnection, new Packet(Packet.DataType.COMMAND, path, values));
    }

    public void addSource(TopicSource source) {
        newTopic(source.getPath(), source.getPath(), source.getConnection());
        topicSources.add(source);
    }

    public void setTickRate(int tickRate) {
        sendPacket(defaultConnection, new Packet(Packet.DataType.COMMAND, "server/tickrate", tickRate));
        this.tickRate = tickRate;
        resetTickLoop();
    }

    boolean isTicking = true;

    private void startTickLoop() {
        isTicking = true;
        tickThread = new Thread(new Runnable() {
            @Override
            public void run() {
                int delay = 1000 / tickRate;
                long next = System.currentTimeMillis() + delay;
                // System.out.println("Starting Tick Loop with Delay: " + delay);
                while (isTicking) {
                    if (System.currentTimeMillis() >= next) {
                        next = System.currentTimeMillis() + delay;
                        onTick();
                    }
                }
            }
        });

        tickThread.start();
    }

    private void onTick() {

        sendTopicSources();
        sendQueue();
    }

    private void sendTopicSources() {
        for (TopicSource source : topicSources) {
            Packet newPacket = new Packet(Packet.DataType.SUBMIT, source.getPath(), source.getData());

            sendPacket(source.getConnection(), newPacket);
        }
    }

    private void resetTickLoop() {
        // System.out.println("Reseting Tick Loop.");
        isTicking = false;
        tickThread = null;
        startTickLoop();
    }

}
