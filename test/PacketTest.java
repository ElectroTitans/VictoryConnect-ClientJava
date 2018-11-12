import org.team4056.victoryconnect.*;
import static org.junit.jupiter.api.Assertions.fail;
import java.io.File;
import java.io.IOException;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.team4056.victoryconnect.networking.Packet;

public class PacketTest {
    @Test
    public void createPacketTest(){
        Packet packet = new Packet(Packet.DataType.COMMAND,"test/command", "test");
        if(packet == null){
            fail("Test packet null");
        }
    }

    @Test
    public void initType(){
        Packet packet = new Packet(Packet.DataType.COMMAND,"test/command", "test");
        if(packet.type != Packet.DataType.COMMAND){
            fail("Test packet was not equal to COMMAND, instead: " + packet.type);
        }
    }
    @Test
    public void initPath(){
        Packet packet = new Packet(Packet.DataType.COMMAND,"test/command", "test");
        if(packet.path.equals("test/command") == false){
            fail("Test packet was not equal to COMMAND, instead: " + packet.type);
        }
    }


    @Test
    public void initData(){
        Packet packet = new Packet(Packet.DataType.COMMAND,"test/command", "test");
        if(packet.type != Packet.DataType.COMMAND){
            fail("Test packet was not equal to COMMAND, instead: " + packet.type);
        }
    }



    @Test
    public void packetToString(){
        Packet packet = new Packet(Packet.DataType.COMMAND,"test/command", "test");
        String stringVersion = packet.toString();

        if(stringVersion.equals("2 test/command {test}~\n")){
            fail("To String failed");
        }
    }
}
