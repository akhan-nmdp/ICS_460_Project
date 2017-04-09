package sender.thread;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketTimeoutException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.LinkedList;
import java.util.Random;

import main.Packet;

public class ThreadTwo implements Runnable {

    private DatagramSocket socket;
    private int corruption;
    private Packet currentPacket;
    private LinkedList<Packet> packets;
    private ThreadOne threadOne;
    //
    private final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss:SSS");
    Timestamp timestamp = new Timestamp(System.currentTimeMillis());
    
    public ThreadTwo(DatagramSocket socket, int corruption, Packet currentPacket, LinkedList<Packet> packets, ThreadOne threadOne) {
        super();
        this.socket = socket;
        this.corruption = corruption;
        this.currentPacket = currentPacket;
        this.packets = packets;
        this.threadOne = threadOne;
    }

    public ThreadTwo(DatagramSocket socket2, int corruption2, LinkedList<Packet> packets2, ThreadOne threadOne2) {
        this.socket = socket2;
        this.corruption = corruption2;
        this.packets = packets2;
        this.threadOne = threadOne2;
    }

    @Override
    public void run() {
        System.out.println("Inside run method for threadTwo");
        int checksumValue;

        int ackNumberValue;
        Integer prevAckNumber = 0;
        Integer prevPacketNumber= 0;
        Integer expectedPacketNumber= 1;
        Packet currentPacket= null;
        
        Random random= new Random();
        while(true){
            String ackNumber= "";
            String checksum= "";
            //threadOne.getCurrentPacket() != null && expectedPacketNumber.equals(threadOne.getCurrentPacket().getSeqno())){
//            System.out.println(" Current packet from threadOne is packet "+ threadOne.getCurrentPacket().getSeqno() + " And expected packet "+ expectedPacketNumber);
         if (threadOne.getCurrentPacket() != null && expectedPacketNumber.equals(threadOne.getCurrentPacket().getSeqno())){
            try {
             currentPacket= threadOne.getCurrentPacket();
        //wait for Ack from receiver
        System.out.println("Waiting for [Ack] for packet # " + expectedPacketNumber + "\n");
        //get the datagramPacket from receiver
        byte[] dataFromReceiver = new byte[1024];
        DatagramPacket receiverPacket = new DatagramPacket(dataFromReceiver, dataFromReceiver.length);
//        try {
            socket.receive(receiverPacket);
//        } catch (IOException ex) {
//            System.err.println("Closing socket as no more incoming packets!");
//            socket.disconnect();
//            disconnect();//exit out of program 
//        }
            
            System.out.println("Received packet and getting data from packet " + currentPacket.getSeqno() + "  " + sdf.format(timestamp));

        //go thru packet from position 0 to 2 to get the checksum
        for(int i = 0; i < 3; i++) {
            checksum = checksum + dataFromReceiver[i];
        }
        checksumValue = Integer.parseInt(checksum);
        //go thru packet from position 4 to 7 to get the ackNumber
        for(int i = 4; i < 8; i++) {
            ackNumber = ackNumber + dataFromReceiver[i];
        }
        ackNumberValue = Integer.parseInt(ackNumber);
        
        System.out.println("Received packet "+ackNumberValue);
        
//        if (threadOne.getCurrentPacket().getSeqno() == ackNumberValue){
//            System.out.println("ThreadOne current packet "+threadOne.getCurrentPacket().getSeqno()+ " matched the received ack packet "+ ackNumberValue);
//            currentPacket= threadOne.getCurrentPacket();
        //randomly drop the Ack that was sent to by receiver, test out maybe need to remove for project 1?????
        if (corruption > 0){
            //randomly [DROP] the Ack
            if (random.nextInt(5) == 4) {
                System.out.println("[DROP] Ack for packet # " + ackNumberValue + "  " + sdf.format(timestamp) +"\n");
                //note down the ackNumber that was dropped
                prevAckNumber= ackNumberValue;
                //note down which packet needs to resend since ack was dropped
                prevPacketNumber= currentPacket.getSeqno();
                threadOne.setPreviousPacketNumber(prevPacketNumber);
                //add this packet in front as it need to be resent
                if (ackNumberValue == currentPacket.getSeqno()) {
                    packets.addFirst(currentPacket);
                    continue; //start from while loop again
                }
            }//end of  if (random.nextInt(5) == 4)
        }//end of if (corruption > 0)
        
        // if checksum came back as 0 from receiver then ack the packet
        if (checksumValue == 0) {
            //if ack was received for this packet before then this is a Dupl Ack
            if (prevAckNumber.equals(ackNumberValue)) {
                System.out.println("[DuplAck] for packet # " + ackNumberValue + "  " + sdf.format(timestamp) + "\n"+ "\n");
            } else {
                //otherwise this is the first time we are receiving ack for this packet
                System.out.println("[AckRcvd] for packet # " + ackNumberValue + "  " + sdf.format(timestamp) + "\n"+ "\n");
            }
            expectedPacketNumber= ackNumberValue+1;
            System.out.println("The next packet that should be ACK is packet "+ expectedPacketNumber);
            System.out.println("Tell threadOne acked for packet "+ackNumberValue+" was received");
            threadOne.setAckedPacket(ackNumberValue);
            //threadOne.setNextPacketNumber(expectedPacketNumber);
            }//end of if (checksumValue == 0)

         } catch (SocketTimeoutException ex) {
            //while waiting for receiver sender timed out
            System.out.println("[TimeOut] while waiting to receieve ACK for packet # "+ currentPacket.getSeqno()+ "  " + sdf.format(timestamp) + " \n");
            //note down this packet will have to resent
            prevPacketNumber= currentPacket.getSeqno();
            threadOne.setPreviousPacketNumber(prevPacketNumber);
            System.out.println(" Timedout so need to receive agai to be acked packet "+ expectedPacketNumber);
            expectedPacketNumber= currentPacket.getSeqno();
            System.out.println(" Timedout so next packet should be packet "+ expectedPacketNumber);
            //add this packet in front as it need to be resent
            packets.addFirst(currentPacket);
        } catch (IOException ex) {
            // TODO Auto-generated catch block
            ex.printStackTrace();
        }
      }//end if threadOne.getCurrentPacket() != null
        }//end while
    }

    public void disconnect(){
        System.exit(0);
    }
}
