package sender.thread;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.LinkedList;
import java.util.Random;

import javax.swing.plaf.SliderUI;

import main.Packet;

public class ThreadOne implements Runnable {

    private LinkedList<Packet> packets;
    private int corruption;
    private DatagramSocket socket;
    private int timeout;
    private InetAddress ip;
    private int port;
    private int windowSize;
    //track the packets
    private Integer previousPacketNumber;
    private Integer delayedPacketNumber;
    private Packet currentPacket;
    //varaible to know when to send next packet
    private Integer nextPacketNumber= 1;
    private Integer ackedPacket;
    
    public ThreadOne(Packet currentPacket, LinkedList<Packet> packets, int corruption, DatagramSocket socket, int timeout, InetAddress ip, int port) {
        super();
        this.currentPacket = currentPacket;
        this.packets = packets;
        this.corruption = corruption;
        this.socket = socket;
        this.timeout = timeout;
        this.ip= ip;
        this.port= port;
    }


    public ThreadOne(LinkedList<Packet> packets2, int windowSize, int corruption2, DatagramSocket socket2, int timeout2, InetAddress ip2, int port2) {
        this.packets = packets2;
        this.corruption = corruption2;
        this.socket = socket2;
        this.timeout = timeout2;
        this.ip= ip2;
        this.port= port2;
        this.windowSize= windowSize;
    }


    @Override
    public void run() {
        System.out.println("Inside run method for threadOne");
        // get the start time
        long startTime = System.currentTimeMillis();
        // variable to store checksum and ack
        String checksum = "";
        String ackNumber = "";
        int checksumValue = 0;
        short goodCheckSum = 0;
        short badCheckSum = 1;
        Packet currentPacket = null;
        Integer delayedPacketNumber = 0;
        Integer prevPacketNumber = 0;
        Random random = new Random();
        try {
            // check the packet and set its checksum
            // currentPacket = packets.removeFirst();
            while (!packets.isEmpty()){
            // get the packet to send based on window size
            LinkedList<Packet> packetsInTransit = choosePackets(windowSize, packets);
            if (!packetsInTransit.isEmpty()) {
                // get the packet that needs to be sent
                currentPacket = packetsInTransit.removeFirst();
                System.out.println("Getting ready packet "+currentPacket.getSeqno());
            } else {
                System.out.println("No more packets to send so need to make threadOne to sleep for " + timeout);
                Thread.sleep(timeout);
            }
            if (currentPacket != null) {
                //System.out.println("Current packet number "+ currentPacket.getSeqno() + " And expected packet number "+ getNextPacketNumber()+ " matched proceed with sending packet");
                // by default the checksum will be good checksum of 0
                currentPacket.setCksum(goodCheckSum);

                // if user had specified packets to be corrupted, then packets
                // will be error and delay
                if (corruption > 0) {
                    // randomly make this packet a bad packet
                    if (random.nextInt(5) == 2) {
                        // this packet is bad packet
                        System.out.println("[ERRR] packet # " + currentPacket.getSeqno() + " is bad packet \n");
                        // assign bad checksum to packet
                        currentPacket.setCksum(badCheckSum);
                    } else if (random.nextInt(5) == 3) {
                        // randomly make this packet delayed
                        System.out.println("[DLYD] packet # " + currentPacket.getSeqno() + "\n");
                        for (int z = 0; z <= timeout; z++) {
                            // do nothing just wait
                        }
                        // assign current packet as delayedPacket and timeout
                        delayedPacketNumber = currentPacket.getSeqno();
                        throw new SocketTimeoutException();
                    }
                }// end of if (corruption > 0)


                // create datagram packet which will be sent to receiver
                sendPacket(currentPacket);

                // check which msg to print
                if (prevPacketNumber.equals(currentPacket.getSeqno()) && !delayedPacketNumber.equals(currentPacket.getSeqno())) {
                    // only need to do [RESEND] when packet was not a
                    // delayedPacket
                    // and when packet was sent before but no ack was received
                    System.out.println("[ReSend.]: packet # " + currentPacket.getSeqno() + " with datasize of " + currentPacket.getData().length + " -----> \n");
                } else {
                    long endTime = System.currentTimeMillis() - startTime;
                    // otherwise for delayedPackets and normalPackets print SENT
                    System.out.println("[SENDing]: packet # " + currentPacket.getSeqno() + " with datasize of " + currentPacket.getData().length + "\n");
                    System.out.println("[SENT] packet # " + currentPacket.getSeqno() + " in " + endTime + " ms ----->" + "\n");
                    setCurrentPacket(currentPacket);
                }
            }//end of while currentPacket != null
            //System.out.println("Done with threadOne since no more packet to send");
            }//end of while
            } catch (SocketTimeoutException ex) {
            // while waiting for receiver sender timed out
            System.out.println("[TimeOut] while sending packet # " + currentPacket.getSeqno() + " \n");
            // note down this packet will have to resent
            prevPacketNumber = currentPacket.getSeqno();
            setPreviousPacketNumber(prevPacketNumber);
            // add this packet in front as it need to be resent
            packets.addFirst(currentPacket);
        } catch (InterruptedException ex) {
            // TODO Auto-generated catch block
            ex.printStackTrace();
        }
    }
    

    private synchronized LinkedList<Packet> choosePackets(int windowSize, LinkedList<Packet> allPreparedPackets) {
        LinkedList<Packet> packetsToSend = new LinkedList<Packet>();
        if (allPreparedPackets != null && !allPreparedPackets.isEmpty()) {
            if (windowSize > 1) {
                for (int i = 0; i < windowSize; i++) {
                    packetsToSend.add(allPreparedPackets.removeFirst());
                }
            } else {
                Packet packet = allPreparedPackets.getFirst();
                if (packet.getSeqno() > 1) {
                    System.out.println(" Need wait for packet "+packet.getSeqno()+" until we receive ack for previous packet");
                    try {
                        wait();
                    } catch (InterruptedException ex) {
                        // TODO Auto-generated catch block
                        ex.printStackTrace();
                    }
                }
                packetsToSend.add(allPreparedPackets.removeFirst());
                System.out.println(" window size is just one so adding packet " + packet.getSeqno());
            }
        }
        return packetsToSend;
    }
    

    private void sendPacket(Packet currentPacket) {
/*        if (currentPacket.getSeqno() > 1) {
            try {
                System.out.println("Waiting until someone tells me to proceed with sending packets ");
                wait();
            } catch (InterruptedException ex1) {
                // TODO Auto-generated catch block
                ex1.printStackTrace();
            }
        }*/
        DatagramPacket output = new DatagramPacket(currentPacket.getData(), currentPacket.getLength(), ip, port);
        // send the packet
        try {
            socket.send(output);
        } catch (IOException ex) {
            // TODO Auto-generated catch block
            ex.printStackTrace();
        }

    }

    public synchronized void setAckedPacket(int ackedPacket){
        System.out.println("Notified that we have recieved ack and can send next packet");
        notify();
        this.ackedPacket= ackedPacket;
    }

    public Integer getAckedPacket() {
        return this.ackedPacket;
    }


    public Integer getNextPacketNumber() {
        return this.nextPacketNumber;
    }


    public void setNextPacketNumber(Integer nextPacketNumber) {
        this.nextPacketNumber = nextPacketNumber;
    }


    public Integer getPreviousPacketNumber() {
        return this.previousPacketNumber;
    }


    public void setPreviousPacketNumber(Integer previousPacketNumber) {
        this.previousPacketNumber = previousPacketNumber;
    }


    public Integer getDelayedPacketNumber() {
        return this.delayedPacketNumber;
    }


    public void setDelayedPacketNumber(Integer delayedPacketNumber) {
        this.delayedPacketNumber = delayedPacketNumber;
    }


    public  Packet getCurrentPacket() {
//        if (this.currentPacket == null){
//            try {
//                wait();
//            } catch (InterruptedException ex) {
//                // TODO Auto-generated catch block
//                ex.printStackTrace();
//            }
//        }
        return this.currentPacket;
    }


    public  void setCurrentPacket(Packet currentPacket) {
//        System.out.println("Notified that we have sent next packet");
//        notify();
        this.currentPacket = currentPacket;
    }

}
