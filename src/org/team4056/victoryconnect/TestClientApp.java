package org.team4056.victoryconnect;

import org.team4056.victoryconnect.listeners.PacketListener;
import org.team4056.victoryconnect.listeners.TopicSource;
import org.team4056.victoryconnect.networking.Packet;

public class TestClientApp {
    static Client vcClient;
    public static void main(String[] args){
        vcClient = new Client("pid-controller", "PID Controller");
        vcClient.enableUDP("localhost","5001" );
        vcClient.setDefaultConnection("UDP");
        vcClient.setTickRate(100);
        vcClient.enableASAP();
        

        vcClient.registerCommand("pid/controller/new", new PacketListener() {
            @Override
            public void onCommand(Packet packet) {
                String conntrollerName = packet.data[0];
                String setPointPath    = packet.data[1];
                String inputPath       = packet.data[2];
                String outputPath      = packet.data[3];


            }
        });


    }

}
