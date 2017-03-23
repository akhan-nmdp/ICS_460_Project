package receiver.thread;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Random;

import main.Packet;

public class ThreadFour implements Runnable {

    private int ackNumber;
    private int corruption;
    private int oldPacketNumber;
    private DatagramPacket packet;
    private DatagramSocket socket;
    private int expectedPacketNumber;
    private int checksumValue;
    private ThreadThree threadThree;
    
    public ThreadFour(int ackNumber, int corruption, int oldPacketNumber, DatagramPacket packet, DatagramSocket socket, int expectedPacketNumber, int checksumValue, ThreadThree threadThree) {
        super();
        this.ackNumber = ackNumber;
        this.corruption = corruption;
        this.oldPacketNumber = oldPacketNumber;
        this.packet = packet;
        this.socket = socket;
        this.expectedPacketNumber = expectedPacketNumber;
        this.checksumValue= checksumValue;
        this.threadThree= threadThree;
    }

    public ThreadFour(int corruption2, DatagramSocket socket2, ThreadThree threadThree2) {
        this.corruption=corruption2;
        this.socket= socket2;
        this.threadThree= threadThree2;
    }

    public int getAckNumber() {
        return this.ackNumber;
    }

    public void setAckNumber(int ackNumber) {
        this.ackNumber = ackNumber;
    }

    public int getCorruption() {
        return this.corruption;
    }

    public void setCorruption(int corruption) {
        this.corruption = corruption;
    }

    public int getOldPacketNumber() {
        return this.oldPacketNumber;
    }

    public void setOldPacketNumber(int oldPacketNumber) {
        this.oldPacketNumber = oldPacketNumber;
    }

    public DatagramPacket getPacket() {
        return this.packet;
    }

    public void setPacket(DatagramPacket packet) {
        this.packet = packet;
    }

    public DatagramSocket getSocket() {
        return this.socket;
    }

    public void setSocket(DatagramSocket socket) {
        this.socket = socket;
    }

    public int getExpectedPacketNumber() {
        return this.expectedPacketNumber;
    }

    public void setExpectedPacketNumber(int expectedPacketNumber) {
        this.expectedPacketNumber = expectedPacketNumber;
    }

    @Override
    public void run() {
        while (true) {
            if (checksumValue == 0) {
                Random random = new Random();
                // create acknowledgement packet
                Packet ackPacket = new Packet((short) 0, (short) 8, ackNumber);
                // we need to randomly need to [DROP] ack packet
                if (corruption > 0) {
                    if (random.nextInt(10) == 7) {
                        System.out.println("[DROP] ACK for packet number " + ackNumber + " <----- \n");
                        // note down this packetNumber since it arrived and ack
                        // was prepareed
                        // but could not proceed further
                        oldPacketNumber = ackNumber;
                        continue;// start from while loop again
                    }
                }

                // send acknowledgement to the sender
                // move below code into separate senderThread
                DatagramPacket ack = new DatagramPacket(ackPacket.getData(), ackPacket.getLength(), threadThree.getPacket().getAddress(), threadThree.getPacket().getPort());
                try {
                    socket.send(ack);
                } catch (IOException ex) {
                    System.out.println("Error while sending ack packet");
                    disconnect();// exit out of program
                }
                // note down the packet that was just acked
                // oldPacketNumber = ackNumber;
                // check which packet we need to expect next
                if (ackPacket.getCksum() == 0) {
                    // increase the packetNumber once ack was sent
                    expectedPacketNumber = ackNumber + 1;
                    System.out.println("[ACK] [SENT] for packet number " + ackNumber + "\n" + "next packet # should be " + (ackNumber + 1) + " <-----" + "\n" + "\n");
                }
            }// end of if (cksumValue == 0)
        }// end of while
    }

    public void disconnect(){
        System.exit(0);
    }
}
