package org.team4056.victoryconnect;

import org.team4056.victoryconnect.listeners.PacketListener;
import org.team4056.victoryconnect.util.Packet;

import javax.swing.*;

public class Test1 {
    public static void main(String[] args){
        Client vcClient = new Client("java-sender", "Java Sender");
        vcClient.EnableTCP("localhost","5000" );

        while(true){
            JOptionPane.showConfirmDialog(null, "Send Packet");
            vcClient.callCommand("java/command", System.currentTimeMillis());
        }
    }
}
