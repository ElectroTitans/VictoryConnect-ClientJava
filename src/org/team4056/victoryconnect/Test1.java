package org.team4056.victoryconnect;

import org.team4056.victoryconnect.listeners.PacketListener;
import org.team4056.victoryconnect.listeners.TopicSource;
import org.team4056.victoryconnect.util.Packet;

import javax.swing.*;

public class Test1 {
    public static void main(String[] args){
        Client vcClient = new Client("java-sender", "Java Sender");
        vcClient.EnableTCP("localhost","5000" );
        vcClient.setTickRate(1);
        vcClient.addSource(new TopicSource() {
            @Override
            public Object getData() {
                return System.currentTimeMillis();
            }

            @Override
            public String getPath() {
                return "test/source";
            }

            @Override
            public String getConnection() {
                return "TCP";
            }
        });
    }
}
