package main;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.LinkedList;
import java.util.Random;
import java.util.Scanner;
/**
 * This class builds packet sends to receiver and takes care for acknowledgement
 *
 */
public class Sender {
    
    //public final static int PORT= 7;
    
    public static void main(String[] args) throws IOException{
        //create socket and set its timeout
        DatagramSocket socket = new DatagramSocket();
        socket.setSoTimeout(2000);
        Scanner inputData = new Scanner(System.in);
        Random random = new Random();
        Packet currentPacket = null;
        int packetSize = 1;
        int corruption = 0;
        int timeout = 2000;
        int ackNumberValue = 1;
        String ipAddress = null;
        int port= 7;
        boolean resend = false;
        boolean duplAck= false;
        Packet droppedPacket= null;
        Integer prevAckNumber = 0;
        Integer prevPacketNumber= 0;
        //new way to get data from command line
        if (args.length > 0){
        packetSize= Integer.parseInt(args[0]);
        timeout= Integer.parseInt(args[1]);
        corruption= Integer.parseInt(args[2]);
        ipAddress= args[3];
        port= Integer.parseInt(args[4]);
        }
        //Gathering input from user
/*        System.out.println("Please enter the ip address (ex: localhost):");
        ipAddress= inputData.next();
        System.out.println("Please enter the port number:");
        port=inputData.nextInt();
        System.out.println("Please enter a packet size greater than 0:");
        packetSize = inputData.nextInt();
        System.out.println("Please enter the percentage of packet that should be corupputed while sending data:");
        corruption = inputData.nextInt();
        System.out.println("Please enter the time(in ms)to resend the packet: ");
        timeout = inputData.nextInt();
        socket.setSoTimeout(timeout);
        //close the scanner after data is entered
        inputData.close();*/
        
        InetAddress ip = InetAddress.getByName(ipAddress);
        PacketBuilder packetBuilder= new PacketBuilder();
        //read the file and put them in packets
        LinkedList<Packet> packets = packetBuilder.readFile(packetSize);
        
        while(!packets.isEmpty()) {
            try {
                long startTime= System.currentTimeMillis();

                String checksum = "";
                String ackNumber = "";
                int checksumValue = 0;
                short goodCheckSum= 0;
                short badCheckSum= 1;

                //check the packet and set its checksum
                currentPacket = packets.removeFirst();
                //by default the checksum will be good checksum of 0
                currentPacket.setCksum(goodCheckSum);
                //create a datagram packet and send it
                //System.out.println("[SENDing] packet " + currentPacket.getSeqno());
                //new DatagramPacket(currentPacket.getData(), currentPacket.getLength(), ip, port);
               
                //check and see if this is an error data packet
               /* if(random.nextInt(100) <= corruption) {
                    currentPacket.setCksum(badCheckSum);
                }
                else {
                    currentPacket.setCksum(goodCheckSum);
                }*/
                
                //check and see if we need to dop the packet, if we want error to occur
                if (corruption > 0) {
                    if (random.nextInt(10) == 1) {
                        System.out.println("[DROP] packet # " + currentPacket.getSeqno());
                        droppedPacket = currentPacket;
                        continue;// start from the while loop again
                    } else if (random.nextInt(10) == 2) {
                        // this packet is bad packet
                        System.out.println("[ERRR] packet # " + currentPacket.getSeqno());
                        // check if we need to make this a good or bad packet
                        currentPacket.setCksum(badCheckSum);
                    } else if (random.nextInt(10) == 3) {
                        for (int z = 0; z < timeout; z++) {
                            // do nothing just need to delay
                        }
                        System.out.println("[DLYD] packet # " + currentPacket.getSeqno());
                    }
                }
                //to keep track of the what packet is being sent
                //oldPacketNumber= currentPacket.getSeqno();
                DatagramPacket output= new DatagramPacket(currentPacket.getData(), currentPacket.getLength(), ip, port);
                //send the packet
                socket.send(output);
                //check which msg to send 
                if (prevPacketNumber.equals(currentPacket.getSeqno())) {
                    System.out.println("[ReSend.]: packet # " + currentPacket.getSeqno() + " with datasize of " + currentPacket.getData().length);
                } else {
                    System.out.println("[SENDing]: packet # " + currentPacket.getSeqno() + " with datasize of " + currentPacket.getData().length);
                }
                
                long endTime= System.currentTimeMillis() - startTime;
                //check whether Ack is sent from receiver
                System.out.println("Waiting for the [Ack] for packet # " + currentPacket.getSeqno() + " which was send in "+ endTime + " ms");
                byte[] dataFromReceiver = new byte[1024];
                DatagramPacket receiverPacket = new DatagramPacket(dataFromReceiver, dataFromReceiver.length);
                socket.receive(receiverPacket);
                //since this packet was sent turn off flag
                resend= false;
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
                
                // if checksum came back as 0 from receiver then ack the packet
                if (checksumValue == 0) {
                    if (prevAckNumber.equals(ackNumberValue)) {
                        System.out.println("[DuplAck] for packet # " + ackNumberValue + "\n");
                    } else {
                        System.out.println("[AckRcvd] for packet # " + ackNumberValue + "\n");
                        // turn off duplAck msg
                        //duplAck = false;
                    }
                } else {
                    // Otherwise checksum was something else and need to resend
                    // packet
                    // System.out.println("Need to resend packet "+
                    // (ackNumberValue)+ " because packet was corrupt.");
                    System.out.println("[ErrAck.] occured need to resend packet # " + ackNumberValue);
                    resend = true;
                    prevPacketNumber= currentPacket.getSeqno();
                    duplAck = true;
                    prevAckNumber= ackNumberValue;
                    packets.addFirst(currentPacket);
                }
                //oldAckNumber= ackNumberValue;

                
            } catch (SocketTimeoutException ste) {
                //If while waiting for ACK we timeout we need to resend this packet 
                System.out.println("[TimeOut] while waiting to receieve ACK for packet # "+ currentPacket.getSeqno());
                resend= true;
                prevPacketNumber= currentPacket.getSeqno();
                packets.addFirst(currentPacket);
            }

        }//end while
        
        socket.disconnect();
    }

    private static DatagramPacket sendPacket(Packet packetTosend, InetAddress ip, int port, boolean resending){
        long time= System.currentTimeMillis();
        if (!resending) {
        System.out.println("[SENDing]: packet " + packetTosend.getSeqno() + " with datasize of "+ packetTosend.getData().length + " in "+ time + " ms");            
        } else {
            System.out.println("[ReSend.]: packet " + packetTosend.getSeqno() + " with datasize of "+ packetTosend.getData().length+ " in "+ time + " ms"); 
        }
        return new DatagramPacket(packetTosend.getData(), packetTosend.getLength(), ip, port);
    }
}
