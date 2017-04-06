package receiver.thread;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Random;

import main.Packet;

public class ThreadFour implements Runnable {

    private int ackNumber;
    private int corruption;
    private int oldPacketNumber;
    private DatagramPacket packet;
    private DatagramSocket socket;
    private int expectedPacketNumber= 1;
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
    
    private final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss:SSS");
    Timestamp timestamp = new Timestamp(System.currentTimeMillis());

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
        //while (true){
        //DatagramPacket packetFromReceiver= checkPacketReady();
        int expectedPacketNum= 1;
        while(threadThree.getPacket() != null){
            if (expectedPacketNum == threadThree.getAckNumber()) {//&& threadThree.getChecksumValue() == 0 removed this last condition
                System.out.println("ThreadFour is expecting packet "+ expectedPacketNum+ " And threadThree is sending packet that needs to be ack for packet "+ threadThree.getAckNumber()+ " And threadThree has expectedPacketNumber to be "+ threadThree.getExpectedPacketNumber() );
                int ackNum= threadThree.getAckNumber();
                Random random = new Random();
                // create acknowledgement packet
                Packet ackPacket = new Packet((short) 0, (short) 8, ackNum);
                // we need to randomly need to [DROP] ack packet
                if (corruption > 0) {
                    if (random.nextInt(10) == 7) {
                        System.out.println("[DROP] ACK for packet number " + ackNumber + " <----- " + sdf.format(timestamp) + "\n");
                        // note down this packetNumber since it arrived and ack
                        // was prepareed
                        // but could not proceed further
                        threadThree.setOldPacketNumber(ackNum);
                        continue;// start from while loop again
                    }
                }
                
//                if (threadThree.getPacket() == null){
//                    continue;//exit out of while loop since no received packet
//                }

                System.out.println("Sending Ack for packet"+ ackNum);
                // send acknowledgement to the sender
                // move below code into separate senderThread
                DatagramPacket ack = new DatagramPacket(ackPacket.getData(), ackPacket.getLength(), threadThree.getPacket().getAddress(), threadThree.getPacket().getPort());
                try {
                    socket.send(ack);
                } catch (IOException ex) {
                    System.out.println("Error while sending ack packet" + "  " + sdf.format(timestamp));
                    disconnect();// exit out of program
                }
                // note down the packet that was just acked
                // oldPacketNumber = ackNumber;
                // check which packet we need to expect next
                if (ackPacket.getCksum() == 0) {
                    // increase the packetNumber once ack was sent
                    expectedPacketNum = ackNum + 1;
                    System.out.println("[ACK] [SENT] for packet number " + ackNum + " <-----" + " " + sdf.format(timestamp) + "\n" + "next packet # should be " + (ackNum + 1) + "\n" + "\n");
                    threadThree.setExpectedPacketNumber(expectedPacketNum);
                    threadThree.setOldPacketNumber(ackNum);
                }
            }// end of if (cksumValue == 0)
            //System.out.println("Iteration of while loop done");
        }// end of while packet != null
      // }//end of while
    }

    public void disconnect(){
        System.exit(0);
    }
    
    private synchronized DatagramPacket checkPacketReady(){
        while (threadThree.getPacket() == null){
            try {
                System.out.println("Inside threadFour notify threadThree packet not ready to ack");
                notifyAll();
                System.out.println("Inside threadFour waiting for threadThree to send packet to ack");
                wait();
            } catch (InterruptedException ex) {
                // TODO Auto-generated catch block
                ex.printStackTrace();
            }
        }
        System.out.println("Inside threadFour notify threadthree has packet ready"+ threadThree.getPacket());
        notifyAll();
        return threadThree.getPacket();
    }
}
