package main;

import java.io.*;
import java.net.*;
import java.util.Random;

public class Receiver {
	// initialize default port to 7
	public final static int DEFAULT_PORT = 7;

	static String hostname = "localhost";

	// FileOutputStream for writeToFile method
	static FileOutputStream output = null;

	public static void main(String[] args) throws IOException {

		String seqNumber = "";
		String packageString = "";
		Random random = new Random();

		byte[] packageByte;
		double corruption = 0;
		int timeout = 1000;
		int currentPacketNumber = 1;
		int cksumValue = 0;
		int ackNumber = 1;
		// int counter = 0;
		byte[] data = new byte[1024];
		int oldPacketNumber = 0;
		int prevPacketNumber = 0;
		int expectedPacketNumber = 1;
		int port = DEFAULT_PORT;

		// way to get data from command line
		if (args.length > 0) {
			corruption = Integer.parseInt(args[0]);
			hostname = args[1];
			port = Integer.parseInt(args[2]);
		}

		// create receiver's socket and set the default port
		DatagramSocket receiverSocket = new DatagramSocket(port);

		receiverSocket.setSoTimeout(timeout);

		while (true) {
			try {
				String cksum = "";
				seqNumber = "";

				// packet received from sender
				DatagramPacket receivePacket = new DatagramPacket(data, data.length);

				receiverSocket.receive(receivePacket);

				// get checksum value
				for (int i = 0; i < 3; i++) {
					cksum = cksum + data[i];
				}
				cksumValue = Integer.parseInt(cksum);

				// get the current packet number
				for (int i = 8; i < 12; i++) {
					seqNumber = seqNumber + data[i];
				}
				currentPacketNumber = Integer.parseInt(seqNumber);

				System.out.println("Waiting on packet # " + expectedPacketNumber);

				// check whether we have received packet before
				if (oldPacketNumber != currentPacketNumber) {
					System.out.println("[RECV] Packet # " + currentPacketNumber + "\n");
				} else {
					System.out.println("[RECV] [DUPL] Packet # " + currentPacketNumber + "\n");
				}

				// if the cksumValue is not zero packet is [CRPT] exit out
				if (cksumValue != 0) {
					System.out.println("[CRPT] packet # " + currentPacketNumber + " and need to recieve again  <----------" + "\n");
					oldPacketNumber = currentPacketNumber;
					continue;// start from the while loop again
				}

				if (corruption > 0) {
					if (random.nextInt(10) == 5) {
						System.out.println("[DROP] packet # " + currentPacketNumber + " <----------\n");
						oldPacketNumber = currentPacketNumber;
						continue;// start from the while loop again
					}
				}

				packageByte = new byte[receivePacket.getLength() - 12];
				for (int i = 0; i < packageByte.length; i++) {
					packageByte[i] = data[i + 12];
				}

				// if checksum value equals zero and no error occurred above
				// proceed with creating
				if (cksumValue == 0) {
					ackNumber = currentPacketNumber;

					// write the data in the new file
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

					// create acknowledgement packet
					Packet ackPacket = new Packet((short) 0, (short) 8, ackNumber);

					if (corruption > 0) {
						if (random.nextInt(10) == 7) {
							System.out.println("[DROP] ACK for packet number " + ackNumber + " <----------\n");
							oldPacketNumber = ackNumber;
							continue;// break out of while loop
						}
					}

					// send acknowledgement to the sender
					DatagramPacket ack = new DatagramPacket(ackPacket.getData(), ackPacket.getLength(),
							receivePacket.getAddress(), receivePacket.getPort());
					receiverSocket.send(ack);
					// assign oldpacketNumber to the packet that was just acked
					oldPacketNumber = ackNumber;
					// check which packet we need to expect next
					if (ackPacket.getCksum() == 0) {
						// increase the packetnumber once ack was sent
						expectedPacketNumber++;
						System.out.println("[ACK] [SENT] for packet number " + ackNumber + "\n"
								+ " next packet # should be " + (ackNumber + 1) + " <----------\n" + "\n");
					}
				}

			} catch (SocketTimeoutException | NullPointerException npe) {

			} catch (SocketException se) {
				System.exit(0);
			}
		}
	}

	// methods for creating new file and output the data from sender
	public static void writeToFile(String packageString) throws IOException {
		output = new FileOutputStream(new File("output_" + packageString));
	}

	public static void writeToFile(byte[] packageByte) throws IOException {
		output.write(packageByte);
	}
}