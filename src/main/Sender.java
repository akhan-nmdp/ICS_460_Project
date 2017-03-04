package main;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.LinkedList;
import java.util.Random;
import java.util.Scanner;

public class Sender {
    
    public final static int PORT= 7;
    
    public static void main(String[] args) throws IOException{
        //create socket and set its timeout
        DatagramSocket socket = new DatagramSocket();
        socket.setSoTimeout(2000);
        InetAddress ip = InetAddress.getByName("localhost");
        Scanner inputs = new Scanner(System.in);
        String fileName;
        Random random = new Random();
        Packet currentPacket = null;
        int packetSize;
        int corruption = 0;
        int timeout = 2000;
        int ackNumberValue = 1;
        
      //Gathering program inputs
        System.out.println("Please enter a text file to trasmfer: ");
        fileName = inputs.nextLine();
        System.out.println("Please enter a packet size greater than 0:");
        packetSize = inputs.nextInt();
        System.out.println("Please enter the percentage of packet that should be corupputed while sending data:");
        corruption = inputs.nextInt();
        System.out.println("Please enter the time(in ms)to resend the packet: ");
        timeout = inputs.nextInt();
        socket.setSoTimeout(timeout);

        LinkedList<Packet> packets = readFile(fileName,packetSize);

        while(!packets.isEmpty()) {
            try {
                String cksum = "";
                String ackNumber = "";
                int cksumValue = 0;

                //this is where the data is sent from client to server
                currentPacket = packets.removeFirst();
                if(random.nextInt(100) <= corruption) {
                    currentPacket.setCksum((short) 1);
                }
                else {
                    currentPacket.setCksum((short) 0);
                }
                System.out.println("[SENDing] packet " + currentPacket.getSeqno());
                DatagramPacket output = new DatagramPacket(currentPacket.getData(), currentPacket.getLength(), ip, PORT);
                socket.send(output);

                //this is where the ack is received
                System.out.println("Waiting for the [Ack] for packet " + currentPacket.getSeqno());
                byte[] receiveData = new byte[1024];
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                socket.receive(receivePacket);

                for(int i = 0; i < 3; i++) {
                    cksum = cksum + receiveData[i];
                }
                cksumValue = Integer.parseInt(cksum);

                for(int i = 4; i < 8; i++) {
                    ackNumber = ackNumber + receiveData[i];
                }
                ackNumberValue = Integer.parseInt(ackNumber);
                
                if(cksumValue == 0) {
                    System.out.println("[AckRcvd] for packet "+ ackNumberValue);
//                    System.out.println("Packet " + ackNumberValue + " was received from the server.\n");
                }
                else {
                    //If we received a bad packet--resend
                    System.out.println("[ResSend.]: packet "+ (ackNumberValue + 1)+ "because packet was corrupt.");
//                    System.out.println("Packet " + ackNumberValue + " was corrupt. Waiting for packet " + ackNumberValue + " to be resent by the server...");
                    packets.addFirst(currentPacket);
                }
                
            } catch (SocketTimeoutException ste) {
                //If we lost the packet during transmission--resend
                System.out.println("[ReSend.]: packet "+ (ackNumberValue +1)+ "because [Ack] was lost.");
//                System.out.println("ACK for packet " + (ackNumberValue + 1) + " was lost. Waiting for packet " + (ackNumberValue + 1) + " to be [ReSend.]: by the server...");
                packets.addFirst(currentPacket);
            }

        }//end while
        
        socket.disconnect();
    }
    
    public static LinkedList<Packet> readFile(String fileName, int packetLength) {

        File transferFile = new File(fileName);
        if(transferFile.exists()) {
            StringBuilder contents = new StringBuilder();
            String data = "";
            String line = "";
            BufferedReader reader;

            try {
                reader = new BufferedReader(new FileReader(transferFile));

                while((line = reader.readLine()) != null) {
                    contents.append(line + "\n");
                }
                data = contents.toString();
                reader.close();

            } catch (IOException e) {
                e.printStackTrace();
            }

            return (buildPackets(fileName, data, packetLength));

        }
        else {
            return null;
        }
    }
    
    private static LinkedList<Packet> buildPackets(String fileName, String dataString, int payload) {
        LinkedList<byte[]> dataList = new LinkedList<byte[]>();
        LinkedList<Packet> packetList = new LinkedList<Packet>();
        byte[] fullData = dataString.getBytes();
        int remainder = fullData.length % payload;
        int fullArraysNeeded = fullData.length / payload;
        int packetNumber = 1;
        int location = 0;

        Packet namePacket = new Packet((short) 0, (short) (12 + fileName.length()), packetNumber, packetNumber, fileName.getBytes());
        packetNumber++;
        packetList.add(namePacket);

        for(int i = 0; i < fullArraysNeeded; i++) {
            byte[] newD = new byte[payload];
            System.arraycopy(fullData, payload * i, newD, 0, newD.length);
            dataList.add(newD);
            location = payload * (i + 1);
        }

        if(remainder != 0) {
            byte[] newD2 = new byte[remainder];
            System.arraycopy(fullData, location, newD2, 0, newD2.length);
            dataList.add(newD2);
        }

        while(!dataList.isEmpty()) {
            byte[] data = dataList.remove();
            Packet packet = new Packet((short) 0, (short) (12 + data.length), packetNumber, packetNumber, data);
            packetNumber++;
            packetList.add(packet);
        }

        return packetList;
    }
    

}
