package main;

import java.io.*;
import java.net.*;
import java.util.Random;
import java.util.Scanner;
import java.util.logging.*;

public class Receiver {

	public final static int DEFAULT_PORT = 7;
	private final int bufferSize; // in bytes
	private final int port;

	String hostname = "localhost";

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
		Scanner inputs = new Scanner(System.in);
		Writer writer = new Writer();
		DatagramSocket socket = new DatagramSocket(DEFAULT_PORT);
		String seqNumber = "";
		String payloadString = "";
		Random random = new Random();
		byte[] payload;
		double corruption = 0;
		int timeout = 1000;
		int currentPacketNumber = 1;
		int cksumValue = 0;
		int ackNumber = 1;
		int counter = 0;
		byte[] data = new byte[1024];

		System.out.println("What is the percentage of packets that should be corrupted during transmission of the data?");
		corruption = inputs.nextInt();
		System.out.println("What is the time it should take to resend a packet if it is lost?");
		timeout = inputs.nextInt();

		socket.setSoTimeout(timeout);

		while (true) {
			try {
				String cksum = "";
				seqNumber = "";

				DatagramPacket receivePacket = new DatagramPacket(data, data.length);
				Random lost = new Random();
				if(lost.nextInt(100) < (corruption / 2)) {
					receivePacket = null;
				}
				socket.receive(receivePacket);
				for(int i = 0; i < 3; i++) {
					cksum = cksum + data[i];
				}
				cksumValue = Integer.parseInt(cksum);

				for (int i = 8; i < 12; i++) {
					seqNumber = seqNumber + data[i];
				}

				currentPacketNumber = Integer.parseInt(seqNumber);
				System.out.println("Waiting for packet number " + currentPacketNumber + " from the client...");

				payload = new byte[receivePacket.getLength() - 12];
				for (int i = 0; i < payload.length; i++) {
					payload[i] = data[i + 12];
				}
				
				if(cksumValue == 0) {
					System.out.println("Packet " + currentPacketNumber + " received from the client. Sending ACK to the client...\n");
					counter--;
					ackNumber = currentPacketNumber;

					if (currentPacketNumber == 1) {
						
						if(!payloadString.equals(new String(payload, "UTF-8"))) {
							payloadString = new String(payload, "UTF-8");
							writer.writeToFile(payloadString);
						}
					} else {
						if(!payloadString.equals(new String(payload, "UTF-8"))) {
							payloadString = new String(payload, "UTF-8");
							writer.writeToFile(payload);
						}
					}

				
					Packet ackPacket = new Packet((short) 0, (short) 8, ackNumber);

					if(random.nextInt(100) <= corruption) {
						ackPacket.setCksum((short) 1);
					}
					else {
						ackPacket.setCksum((short) 0);
					}

					DatagramPacket ack = new DatagramPacket(ackPacket.getData(), ackPacket.getLength(),receivePacket.getAddress(), receivePacket.getPort());
					socket.send(ack);
				}
				
				if(cksumValue != 0) {
					System.out.println("Packet " + currentPacketNumber + " was corrupt.");
				}
					
				if (cksumValue != 0) {
					System.out.println("Packet " + currentPacketNumber + " was lost. Waiting for the packet to be resent by the client...");
				/*	counter++;
					
					if(counter > 5) {
						socket.disconnect();
						System.exit(0);
					}*/
				}
					
				

			} catch(SocketTimeoutException | NullPointerException npe) {
				
				//counter++;
			} catch(SocketException se) {
				System.exit(0);
			}
		}//end while
	}//end main
}//end class