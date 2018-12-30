package com.victoryforphil.victoryconnect.listeners;

import com.victoryforphil.victoryconnect.networking.Packet;

public interface PacketListener {
    void onCommand(Packet packet);
}
