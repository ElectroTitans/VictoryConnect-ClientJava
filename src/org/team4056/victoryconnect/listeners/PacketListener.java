package org.team4056.victoryconnect.listeners;

import org.team4056.victoryconnect.util.Packet;

public interface PacketListener {
    void onCommand(Packet packet);
}
