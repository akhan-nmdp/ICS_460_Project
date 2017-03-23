package main;

import java.io.FileNotFoundException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.Scanner;

import sender.thread.ThreadOne;
import sender.thread.ThreadTwo;

public class SenderMain {

    public static void main(String args[]) {
        /*if (args.length > 0) {
            int packetSize = Integer.parseInt(args[0]);
            int timeout = Integer.parseInt(args[1]);
            int corruption = Integer.parseInt(args[2]);
            int windowSize = Integer.parseInt(args[3]);
            String ipAddress = args[4];
            int port = Integer.parseInt(args[5]);*/
        
        Scanner inputs = new Scanner(System.in);
        //Gathering program inputs
        System.out.println("Please enter hostanme");
        String ipAddress = inputs.nextLine();
        System.out.println("Please enter timeout");
        int timeout= inputs.nextInt();
        System.out.println("Please enter packetSize");
        int packetSize= inputs.nextInt();
        System.out.println("Please enter window");
        //window = 5;
        int windowSize = inputs.nextInt();
        System.out.println("Please enter corruption:");
        //corruption = 20;
        int corruption = inputs.nextInt();
        //packetSize = 10;
        //String hostname = inputs.nextLine();
        System.out.println("Please enter port");
        //timeout = 2000;
        int port = inputs.nextInt();
        inputs.close();
        

            InetAddress ip = null;
            try {
                ip = InetAddress.getByName(ipAddress);
            } catch (UnknownHostException ex) {
                System.out.println("Error happened while creating ip");
                disconnect();
            }

            DatagramSocket socket = null;
            try {
                socket = new DatagramSocket();
                socket.setSoTimeout(timeout);
            } catch (SocketException ex) {
                System.out.println("Error happened while creating socket");
                disconnect();
            }
            // object to help build packet
            PacketBuilder packetBuilder = new PacketBuilder();
            // read data and put them in packets
            LinkedList<Packet> packets = new LinkedList<Packet>();
            try {
                packets = packetBuilder.readFile(packetSize);
            } catch (FileNotFoundException ex) {
                System.out.println("Error happened while building packets");
                disconnect();
            }
            while (!packets.isEmpty()) {
                Packet currentPacket = packets.removeFirst();
                ThreadOne threadOne = new ThreadOne(currentPacket, packets, corruption, socket, timeout, ip, port);
                Thread threadS = new Thread(threadOne);
                threadS.start();
                ThreadTwo threadTwo= new ThreadTwo(socket, corruption, currentPacket, packets, threadOne);
                Thread threadR= new Thread(threadTwo);
                threadR.start();
            }
        //}//end of if
    }
    
    private static void disconnect(){
        System.exit(0);
    }
}
