package org.team4056.victoryconnect.util;

import java.util.ArrayList;
import java.util.List;

public class PacketParser {
    public static List<Packet> parse(String raw){
        String[] packetsString = raw.split("~");
        List<Packet> packets = new ArrayList<>();
        for(String packetString : packetsString){

        }

        return packets;
    }

    public static Packet parseSingle(String raw){
        if(raw.indexOf("~") < 1){
            raw = raw.substring(raw.indexOf("~")+1);
        }
        String[] segments = raw.split(" ");

        String packetTypeRaw = segments[0];
        Packet.DataType packetType = Packet.typeFromInt(Integer.parseInt(packetTypeRaw));

        String packetPath = segments[1];

        String dataString  = raw.substring(raw.indexOf("{")+1, raw.indexOf("}"));
        String[] dataList;

        if(dataString.indexOf(";") != -1){
            dataList = dataString.split(";");
        }else{
            dataList = new String[]{dataString};
        }
        Packet newPacket = new Packet(packetType,packetPath,dataList);
        newPacket.setRaw(raw);
        return newPacket;
    }
}
