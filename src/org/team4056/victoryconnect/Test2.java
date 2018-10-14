package org.team4056.victoryconnect;

import jdk.nashorn.internal.scripts.JO;
import org.team4056.victoryconnect.listeners.PacketListener;
import org.team4056.victoryconnect.util.Packet;

import javax.swing.*;

public class Test2 {
    public static void main(String[] args){
        Client vcClient = new Client("java-recv", "Java Recv");
        vcClient.EnableTCP("localhost","5000" );


        vcClient.registerCommand("java/command", packet -> System.out.println(packet.toString()));
        
    }
}
