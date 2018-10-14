package org.team4056.victoryconnect;

import org.team4056.victoryconnect.listeners.PacketListener;
import org.team4056.victoryconnect.util.Packet;

public class Test1 {
    public static void main(String[] args){
        Client vcClient = new Client();
        vcClient.EnableTCP("localhost","5000" );

        vcClient.addCommandListener("bot/motors", packet -> {
            int leftPower  = Integer.parseInt(packet.data[0]);
            int rightPower = Integer.parseInt(packet.data[1]);
        });
    }
}
