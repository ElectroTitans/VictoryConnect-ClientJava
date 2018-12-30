package com.victoryforphil.victoryconnect;

import com.victoryforphil.victoryconnect.listeners.MDNSListener;
import com.victoryforphil.victoryconnect.listeners.PacketListener;
import com.victoryforphil.victoryconnect.networking.Packet;

public class Test2 {
    public static void main(String[] args){
        Client vcClient = new Client("java-recv", "Java Recv");
        
        vcClient.enableMDNS(new MDNSListener(){

            @Override
            public void onService(String type, String ip, String port) {
                switch(type){
                    case "TCP":
                     vcClient.enableTCP(ip,port);
                    break;

                }
            }
            
        });

      

        vcClient.setTickRate(500);
        /*
        vcClient.addSource(new TopicSource() {
            @Override
            public Object getData() {
                return System.nanoTime();
            }

            @Override
            public String getPath() {
                return "test/java/nanotime";
            }

            @Override
            public String getConnection() {
                return "TCP";
            }
        });
         */

        vcClient.registerCommand("web_test", new PacketListener() {
            @Override
            public void onCommand(Packet packet) {
                System.out.println(packet.data[1]);
            }
        });
        
    }
}
