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

	String hostname = "localhost";

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
		Scanner input = new Scanner(System.in);
		//create receiver's socket and set the default port
		DatagramSocket receiverSocket = new DatagramSocket(DEFAULT_PORT);
		
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

		//Gather the corruption and the timeout length from the user
		System.out.println("Please enter the percentage of packet that should be corrupted while sending data: ");
		corruption = input.nextInt();
		
		System.out.println("Please enter the time(in ms) to resend the packet, if it gets lost: ");
		timeout = input.nextInt();

		receiverSocket.setSoTimeout(timeout);
		
		//close the scanner after data is entered
        input.close();

		while (true) {
			try {
				String cksum = "";
				seqNumber = "";

				//packet received from sender
				DatagramPacket receivePacket = new DatagramPacket(data, data.length);
				
				Random lost = new Random();
				//if corruption/2 is more than random number between 1-100, then data was lost
				if (lost.nextInt(100) < (corruption / 2)) {
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

				packageByte = new byte[receivePacket.getLength() - 12];
				for (int i = 0; i < packageByte.length; i++) {
					packageByte[i] = data[i + 12];
				}

				//if checksum value equals zero, then there are no data corruption and send acknowledgement 
				if (cksumValue == 0) {
					System.out.println("Packet # " + currentPacketNumber
							+ " [RECV]\n" + "[SENDing]: [ACK]...\n");
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
						ackPacket.setCksum((short) 1);
					} else {
						ackPacket.setCksum((short) 0);
					}
					
					//send acknowledgement to the sender
					DatagramPacket ack = new DatagramPacket(ackPacket.getData(), ackPacket.getLength(), receivePacket.getAddress(), receivePacket.getPort());
					receiverSocket.send(ack);
				}
				
				//if checksum value not equal zero, current packet is corrupted and waits for re-send
				if (cksumValue != 0) {
					System.out.println("Packet # " + currentPacketNumber + " [CRPT]");
				}

				if (cksumValue != 0) {
					System.out.println("Packet # " + currentPacketNumber
							+ " LOST\n" + "Waiting on the [ReSend]...\n");
				}
				
				

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