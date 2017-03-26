package main;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Random;
import java.util.Scanner;

import receiver.thread.ReceiverThread;
import receiver.thread.ThreadFour;
import receiver.thread.ThreadThree;

public class ReceiverMain {

    public static void main(String args[]){
        //get data from command line
//      if (args.length > 0) {
//          int windowSize= Integer.parseInt(args[0]);
//          int corruption = Integer.parseInt(args[1]);
//          String hostname = args[2];
//          int port = Integer.parseInt(args[3]);
        
            Scanner inputs = new Scanner(System.in);
          //Gathering program inputs
          System.out.println("Please enter hostanme");
          String hostname = inputs.nextLine();
          System.out.println("Please enter window");
          //window = 5;
          int windowSize = inputs.nextInt();
          System.out.println("Please enter corruption:");
          //corruption = 20;
          int corruption = inputs.nextInt();
          //packetSize = 10;
          System.out.println("Please enter timeout:");
          int timeout= inputs.nextInt();
          //String hostname = inputs.nextLine();
          System.out.println("Please enter port");
          //timeout = 2000;
          int port = inputs.nextInt();
          inputs.close();
          
          if (timeout == 0)
              timeout= 2000;
          DatagramSocket socket = null;
        try {
            socket = new DatagramSocket(port, InetAddress.getByName(hostname));
            socket.setSoTimeout(timeout);
        } catch (SocketException ex) {
           System.out.println("Error while creating socket");
           System.exit(0);
        } catch (UnknownHostException ex) {
            System.out.println("Error while getting hostname");
            System.exit(0);
        }
          ThreadThree threadThree= new ThreadThree(windowSize, corruption, port, hostname, socket);
          Thread threadR = new Thread(threadThree);
          threadR.start();
          
          ThreadFour threadFour = new ThreadFour(corruption, socket, threadThree);
          Thread threadS= new Thread(threadFour);
          threadS.start();
          
          /*//object to generate random number
          Random random = new Random();
          //supported character type 
          String characterSet= "UTF-8";
          
          //variable to to store package properties
          String seqNumber = "";
          String packageString = "";
          byte[] packageByte;
          //packet and ack number that will be used keep track of packets
          int currentPacketNumber = 1;
          int ackNumber = 1;
          //checksum value coming from sender
          int cksumValue = 0;
          //variable to store data that comes from sender
          byte[] data = new byte[1024];
          //variable to keep track of packets that arrived and are coming
          int oldPacketNumber = 0;
          int expectedPacketNumber = 1;
          
          while (true) {
        try {
            //variable to extract checksum and seq
            String cksum = "";
            seqNumber = "";
            
            DatagramSocket socket = new DatagramSocket(port, InetAddress.getByName(hostname));
            
            ReceiverThread receiverThread= new ReceiverThread(socket, data);
            Thread threadForReceiver= new Thread(receiverThread);
            threadForReceiver.start();
            
            // packet received from sender
            DatagramPacket receivePacket = new DatagramPacket(data, data.length);
            socket.receive(receivePacket);
            
            // parse the data to get checksum 
            for (int i = 0; i < 3; i++) {
                cksum = cksum + data[i];
            }
            //convert checksum into a number
            cksumValue = Integer.parseInt(cksum);

            // parse data to get current packet number
            for (int i = 8; i < 12; i++) {
                seqNumber = seqNumber + data[i];
            }
            //convert seq into a number
            currentPacketNumber = Integer.parseInt(seqNumber);
            //print out what packet is coming
            System.out.println("Waiting on packet # " + expectedPacketNumber+ "\n");
            
        } catch (SocketException ex) {
            // TODO Auto-generated catch block
            ex.printStackTrace();
        } catch (UnknownHostException ex) {
            // TODO Auto-generated catch block
            ex.printStackTrace();
        } catch (IOException ex) {
            // TODO Auto-generated catch block
            ex.printStackTrace();
        }
       } 
          
      }*/
        //}//end of if
    }

}
