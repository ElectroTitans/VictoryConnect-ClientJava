package org.team4056.victoryconnect;

public class Test2 {
    public static void main(String[] args){
        Client vcClient = new Client("java-recv", "Java Recv");
        vcClient.enableTCP("localhost","5000" );

        vcClient.setTickRate(10);
        vcClient.registerCommand("test/command", packet -> {
            long recivedTime = Long.parseLong(packet.data[0]);
            System.out.println("Ping: " + (System.currentTimeMillis() - recivedTime) + "ms");
        });
        
    }
}
