package org.team4056.victoryconnect;

import org.team4056.victoryconnect.listeners.PacketListener;
import org.team4056.victoryconnect.listeners.TopicSource;
import org.team4056.victoryconnect.networking.Packet;

public class Test2 {
    public static void main(String[] args){
        Client vcClient = new Client("java-recv", "Java Recv");
        vcClient.enableTCP("localhost","5000" );

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
