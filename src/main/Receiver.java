package main;

import java.io.*;
import java.net.*;
import java.util.Random;
import java.util.Scanner;

public class Receiver {
	//initialize default port to 7
	public final static int DEFAULT_PORT = 7;
	//declare integer buffer size and port number
	private final int bufferSize;
	private final int port;

	static String hostname = "localhost";

	//FileOutputStream for writeToFile method 
	static FileOutputStream output = null;

	public Receiver(int port, int bufferSize) {
		this.bufferSize = bufferSize;
		this.port = port;
	}

	public Receiver(int port) {
		this(port, 8192);
	}

	public Receiver() {
		this(DEFAULT_PORT);
	}

	public static void main(String[] args) throws IOException {
		//Scanner input = new Scanner(System.in);
		
		String seqNumber = "";
		String packageString = "";
		Random random = new Random();
		
		byte[] packageByte;
		double corruption = 0;
		int timeout = 1000;
		int currentPacketNumber = 1;
		int cksumValue = 0;
		int ackNumber = 1;
		int counter = 0;
		byte[] data = new byte[1024];
		int oldPacketNumber = 0;	
		int port = DEFAULT_PORT;
		
		//way to get data from command line
		corruption= Integer.parseInt(args[0]);
        hostname = args[1];
        port = Integer.parseInt(args[2]);
        
        //create receiver's socket and set the default port
        DatagramSocket receiverSocket = new DatagramSocket(DEFAULT_PORT);
		
		/*
		//Gather the corruption and the timeout length from the user
		System.out.println("Please enter the percentage of packet that should be corrupted while sending data: ");
		corruption = input.nextInt();
		
		System.out.println("Please enter the time(in ms) to resend the packet, if it gets lost: ");
		timeout = input.nextInt();*/

		receiverSocket.setSoTimeout(timeout);
		
		//close the scanner after data is entered
        //input.close();

		while (true) {
			try {
				String cksum = "";
				seqNumber = "";

				//packet received from sender
				DatagramPacket receivePacket = new DatagramPacket(data, data.length);
				
				Random lost = new Random();
				//if corruption/2 is more than random number between 1-100, then data was lost
				if (lost.nextInt(100) < (corruption / 2)) {
					//if the receivepacket is null throw error [CRPT] packet sent (akhan)
					System.out.println("\npacket is null.....\n");
					receivePacket = null;
				}
				receiverSocket.receive(receivePacket);
				
				//get checksum value
				for (int i = 0; i < 3; i++) {
					cksum = cksum + data[i];
				}
				cksumValue = Integer.parseInt(cksum);

				//get the current packet number
				for (int i = 8; i < 12; i++) {
					seqNumber = seqNumber + data[i];
				}
				currentPacketNumber = Integer.parseInt(seqNumber);
				
				System.out.println("Waiting on packet # " + currentPacketNumber + "...");
				
				if(cksumValue != 0){
					System.out.println("packet # " + currentPacketNumber + " is [CRPT]");
				}
				
				packageByte = new byte[receivePacket.getLength() - 12];
				for (int i = 0; i < packageByte.length; i++) {
					packageByte[i] = data[i + 12];
				}

				//if checksum value equals zero, then there are no data corruption and send acknowledgement 
				if (cksumValue == 0) {
					//check the packetnuber and see if it equals the old packet number. if the old and current packet number mathc then print dupl msg (akhan)
 					if(oldPacketNumber == currentPacketNumber){
						System.out.println("Packet # " + currentPacketNumber + " is [DUPL]");
					}else{
						System.out.println("Packet # " + currentPacketNumber + " is [RECV]\n" + "[SENDing]: [ACK] " + currentPacketNumber + "...");
					}
					counter--;
					ackNumber = currentPacketNumber;

					//write the data in the new file
					if (currentPacketNumber == 1) {

						if (!packageString.equals(new String(packageByte, "UTF-8"))) {
							packageString = new String(packageByte, "UTF-8");
							writeToFile(packageString);
						}
					} else {
						if (!packageString.equals(new String(packageByte, "UTF-8"))) {
							packageString = new String(packageByte, "UTF-8");
							writeToFile(packageByte);
						}
					}
					
					//create acknowledgement packet
					Packet ackPacket = new Packet((short) 0, (short) 8, ackNumber);

					//if random number between 1-100 is less or equal to corruption number, then set checksum value is 1 else 0
					if (random.nextInt(100) <= corruption) {
						//if the above condition is true then print out the [drop] msg for ack (akhan)
						System.out.println("[ACK] # " + currentPacketNumber + " is [DROP]");
						ackPacket.setCksum((short) 1);
					} else {
						//otherwise print you are preaprig an ack
						System.out.println("Preparing an ack...");
						ackPacket.setCksum((short) 0);
					}
					
					//check to see if checksum== 0 then print the [sent] ack msg (akhan) (seems like redundant)
					if(ackPacket.getCksum() == 0){
						System.out.println("[ACK][SENT]\n");
					}
					//else print out the [err] ack msg (akhan)
					else{
						System.out.println("[ErrAck]\n");
					}
					//send acknowledgement to the sender
					DatagramPacket ack = new DatagramPacket(ackPacket.getData(), ackPacket.getLength(), receivePacket.getAddress(), receivePacket.getPort());
					receiverSocket.send(ack);
					oldPacketNumber = currentPacketNumber;
				}
				
				//if checksum value not equal zero, current packet is corrupted and waits for re-send
				/*if (cksumValue != 0) {
					System.out.println("Packet # " + currentPacketNumber + " [CRPT]");
				}

				if (cksumValue != 0) {
					System.out.println("Packet # " + currentPacketNumber
							+ " LOST\n" + "Waiting on the [ReSend]...\n");
				}*/
				
				

			} catch (SocketTimeoutException | NullPointerException npe) {
				
			} catch (SocketException se) {
				System.exit(0);
			}
		}
	}

	//methods for creating new file and output the data from sender
	public static void writeToFile(String packageString) throws IOException {
		output = new FileOutputStream(new File("output_" + packageString));
	}

	public static void writeToFile(byte[] packageByte) throws IOException {
		output.write(packageByte);
	}
}