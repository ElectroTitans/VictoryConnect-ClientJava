package com.victoryforphil.victoryconnect.listeners;

public interface MDNSListener  {
    void onService(String type, String ip, String port);
}
