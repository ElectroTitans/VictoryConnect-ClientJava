package org.team4056.victoryconnect;

public class Test2 {
    public static void main(String[] args){
        Client vcClient = new Client("java-recv", "Java Recv");
        vcClient.enableTCP("localhost","5000" );

        vcClient.setTickRate(5000);
        vcClient.registerCommand("java/command", packet -> {
            long recivedTime = Long.parseLong(packet.data[0]);
            System.out.println("Ping: " + (System.currentTimeMillis() - recivedTime) + "ms");
        });
        
    }
}
