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
    
    public final static int PORT= 7;
    
    public static void main(String[] args) throws IOException{
        //create socket and set its timeout
        DatagramSocket socket = new DatagramSocket();
        socket.setSoTimeout(2000);
        //Scanner inputData = new Scanner(System.in);
        Random random = new Random();
        Packet currentPacket = null;
        int packetSize;
        int corruption = 0;
        int timeout = 2000;
        int ackNumberValue = 1;
        String ipAddress;
        int port;
        boolean resend = false;
        
        packetSize= Integer.parseInt(args[0]);
        timeout= Integer.parseInt(args[1]);
        corruption= Integer.parseInt(args[2]);
        ipAddress= args[3];
        port= Integer.parseInt(args[4]);
        
        //Gathering input from user
        /*System.out.println("Please enter the ip address (ex: localhost):");
        ipAddress= inputData.next();
        System.out.println("Please enter the port number:");
        port=inputData.nextInt();
        System.out.println("Please enter a packet size greater than 0:");
        packetSize = inputData.nextInt();
        System.out.println("Please enter the percentage of packet that should be corupputed while sending data:");
        corruption = inputData.nextInt();
        System.out.println("Please enter the time(in ms)to resend the packet: ");
        timeout = inputData.nextInt();*/
        socket.setSoTimeout(timeout);
        //close the scanner after data is entered
        //inputData.close();
        
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
                if(random.nextInt(100) <= corruption) {
                    currentPacket.setCksum(badCheckSum);
                }
                else {
                    currentPacket.setCksum(goodCheckSum);
                }
                //create a datagram packet and send it
                //System.out.println("[SENDing] packet " + currentPacket.getSeqno());
                //new DatagramPacket(currentPacket.getData(), currentPacket.getLength(), ip, port);
                if (!resend) {
                System.out.println("[SENDing]: packet # " + currentPacket.getSeqno() + " with datasize of "+ currentPacket.getData().length );            
                } else {
                    System.out.println("[ReSend.]: packet # " + currentPacket.getSeqno() + " with datasize of "+ currentPacket.getData().length); 
                }
                DatagramPacket output= new DatagramPacket(currentPacket.getData(), currentPacket.getLength(), ip, port);
                //send the packet
                socket.send(output);
                
                long endTime= System.currentTimeMillis() - startTime;
                //check whether Ack is sent from receiver
                System.out.println("Waiting for the [Ack] for packet # " + currentPacket.getSeqno() + " which was send in "+ endTime + " ms");
                byte[] dataFromReceiver = new byte[1024];
                DatagramPacket receiverPacket = new DatagramPacket(dataFromReceiver, dataFromReceiver.length);
                socket.receive(receiverPacket);
                //go thru packet from position 0 to 2 to get the checksum
                for(int i = 0; i < 3; i++) {
                    checksum = checksum + dataFromReceiver[i];
                }
                checksumValue = Integer.parseInt(checksum);
                //go thru packet from positin 4 to 7 to get the ackNumber
                for(int i = 4; i < 8; i++) {
                    ackNumber = ackNumber + dataFromReceiver[i];
                }
                ackNumberValue = Integer.parseInt(ackNumber);
                
                //if checksum came back as 0 from receiver then ack the packet
                if(checksumValue == 0) {
                    System.out.println("[AckRcvd] for packet # "+ ackNumberValue+ "\n");
                }
                else {
                    //Otherwise checksum was something else and need to resend packet
                    //System.out.println("Need to resend packet "+ (ackNumberValue)+ " because packet was corrupt.");
                    System.out.println("[ErrAck.] occured need to resend packet # " + ackNumberValue);
                    resend= true;
                    packets.addFirst(currentPacket);
                }
                
            } catch (SocketTimeoutException ste) {
                //If we lost the packet during transmission--resend
//                System.out.println("[TimeOut.]: packet "+ (ackNumberValue)+ " because [Ack] was lost.");
                System.out.println("[TimeOut]: packet "+ currentPacket.getSeqno());
                resend= true;
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
