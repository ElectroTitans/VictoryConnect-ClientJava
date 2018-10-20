package org.team4056.victoryconnect;

import org.team4056.victoryconnect.listeners.TopicSource;

public class Test1 {
    public static void main(String[] args){
        Client vcClient = new Client("java-sender", "Java Sender");
        vcClient.enableUDP("localhost","5001" );
        vcClient.setDefaultConnection("UDP");
        vcClient.setTickRate(1);
        vcClient.enableASAP();
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
