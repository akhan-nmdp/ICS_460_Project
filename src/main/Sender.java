package main;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
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
        InetAddress ip = InetAddress.getByName("localhost");
        Scanner inputData = new Scanner(System.in);
        Random random = new Random();
        Packet currentPacket = null;
        int packetSize;
        int corruption = 0;
        int timeout = 2000;
        int ackNumberValue = 1;
        
        //Gathering input from user
        System.out.println("Please enter a packet size greater than 0:");
        packetSize = inputData.nextInt();
        System.out.println("Please enter the percentage of packet that should be corupputed while sending data:");
        corruption = inputData.nextInt();
        System.out.println("Please enter the time(in ms)to resend the packet: ");
        timeout = inputData.nextInt();
        socket.setSoTimeout(timeout);
        //close the scanner after data is entered
        inputData.close();
        PacketBuilder packetBuilder= new PacketBuilder();
        //read the file and put them in packets
        LinkedList<Packet> packets = packetBuilder.readFile(packetSize);
        
        while(!packets.isEmpty()) {
            try {
                String checksum = "";
                String ackNumber = "";
                int checksumValue = 0;

                //check the packet and set its checksum
                currentPacket = packets.removeFirst();
                if(random.nextInt(100) <= corruption) {
                    currentPacket.setCksum((short) 1);
                }
                else {
                    currentPacket.setCksum((short) 0);
                }
                //create a datagram packet and send it
                System.out.println("[SENDing] packet " + currentPacket.getSeqno());
                DatagramPacket output = new DatagramPacket(currentPacket.getData(), currentPacket.getLength(), ip, PORT);
                socket.send(output);

                //check whether Ack is sent from receiver
                System.out.println("Waiting for the [Ack] for packet " + currentPacket.getSeqno());
                byte[] receiveData = new byte[1024];
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                socket.receive(receivePacket);
                
                for(int i = 0; i < 3; i++) {
                    checksum = checksum + receiveData[i];
                }
                checksumValue = Integer.parseInt(checksum);

                for(int i = 4; i < 8; i++) {
                    ackNumber = ackNumber + receiveData[i];
                }
                ackNumberValue = Integer.parseInt(ackNumber);
                
                //if checksum came back as 0 from receiver then ack the packet
                if(checksumValue == 0) {
                    System.out.println("[AckRcvd] for packet "+ ackNumberValue);
                }
                else {
                    //Otherwise checksum was something else and need to resend packet
                    System.out.println("[ResSend.]: packet "+ (ackNumberValue + 1)+ "because packet was corrupt.");
                    packets.addFirst(currentPacket);
                }
                
            } catch (SocketTimeoutException ste) {
                //If we lost the packet during transmission--resend
                System.out.println("[ReSend.]: packet "+ (ackNumberValue +1)+ "because [Ack] was lost.");
                packets.addFirst(currentPacket);
            }

        }//end while
        
        socket.disconnect();
    }

}
