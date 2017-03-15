package main;

import java.io.*;
import java.net.*;
import java.util.Random;
/**
 * This class receives packet from sender and acknowledges the packet
 *
 */
public class Receiver {
    
	// FileOutputStream for writeToFile method
	static FileOutputStream output = null;

	public static void main(String[] args) throws IOException {

		//object to generate random number
		Random random = new Random();
		//supported characte type 
		String characterSet= "UTF-8";
		
	    //variable to to store package properties
        String seqNumber = "";
        String packageString = "";
		byte[] packageByte;
		//variable to hold the corruption value
		int corruption = 0;
		//default timeout vaulue
		int timeout = 2000;
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
		//default connection config
		int port = 7;
		String hostname = "localhost";

		//get data from command line
		if (args.length > 0) {
			corruption = Integer.parseInt(args[0]);
			hostname = args[1];
			port = Integer.parseInt(args[2]);
		}
		
		//create the ip
		InetAddress ip = InetAddress.getByName(hostname);
		// create receiver's socket
		DatagramSocket receiverSocket = new DatagramSocket(port, ip);
		//set the timeout
		receiverSocket.setSoTimeout(timeout);

		while (true) {
			try {
			    //variable to extract checksum and seq
				String cksum = "";
				seqNumber = "";
				// packet received from sender
				DatagramPacket receivePacket = new DatagramPacket(data, data.length);
				receiverSocket.receive(receivePacket);

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

				// check whether we have received packet before
				if (oldPacketNumber != currentPacketNumber) {
				    //if this packet has not arrived before then we 
				    //receiving it for first time
					System.out.println("[RECV] Packet # " + currentPacketNumber + "\n");
				} else {
				    //otherwise this packet came before
					System.out.println("[RECV] [DUPL] Packet # " + currentPacketNumber + "\n");
				}

				// if the cksumValue is not zero packet is [CRPT] exit out
				if (cksumValue != 0) {
					System.out.println("[CRPT] packet # " + currentPacketNumber + " and need to recieve again  <-----" + "\n");
					//note down this packetNumber since it arrived but was 
					//could not proceed further
					oldPacketNumber = currentPacketNumber;
					continue;// start from the while loop again
				}
				//we need to randomly [DROP] packet and exit out
				if (corruption > 0) {
					if (random.nextInt(10) == 5) {
						System.out.println("[DROP] packet # " + currentPacketNumber + " <-----\n");
						//note down this packetNumber since it arrived but was 
	                    //could not proceed further
						oldPacketNumber = currentPacketNumber;
						continue;// start from the while loop again
					}
				}
				//get the data that was sent
				packageByte = new byte[receivePacket.getLength() - 12];
				for (int i = 0; i < packageByte.length; i++) {
					packageByte[i] = data[i + 12];
				}

				// if checksum value equals zero and no error occurred above
				// proceed with creating ack
				if (cksumValue == 0) {
				    //assign ack for this packetNumber
					ackNumber = currentPacketNumber;

					// write the data in the new file
					if (currentPacketNumber == 1) {
					    //if its the first packet then it has the name of file
					    //get name of file and write to output file
						if (!packageString.equals(new String(packageByte, characterSet))) {
							packageString = new String(packageByte, characterSet);
							writeToFile(packageString);
						}
					} else {
					    //otherwise the packet has content of file, get the 
					    //content and write to output file 
						if (!packageString.equals(new String(packageByte, characterSet))) {
							packageString = new String(packageByte, characterSet);
							writeToFile(packageByte);
						}
					}

					// create acknowledgement packet
					Packet ackPacket = new Packet((short) 0, (short) 8, ackNumber);
					//we need to randomly need to [DROP] ack packet
					if (corruption > 0) {
						if (random.nextInt(10) == 7) {
							System.out.println("[DROP] ACK for packet number " + ackNumber + " <----- \n");
							//note down this packetNumber since it arrived and ack was prepareed 
							//but could not proceed further
							oldPacketNumber = ackNumber;
							continue;// start from while loop again
						}
					}

					// send acknowledgement to the sender
					DatagramPacket ack = new DatagramPacket(ackPacket.getData(), ackPacket.getLength(),
							receivePacket.getAddress(), receivePacket.getPort());
					receiverSocket.send(ack);
					// note down the packet that was just acked
					//oldPacketNumber = ackNumber;
					// check which packet we need to expect next
					if (ackPacket.getCksum() == 0) {
						// increase the packetNumber once ack was sent
						expectedPacketNumber= ackNumber + 1;
						System.out.println("[ACK] [SENT] for packet number " + ackNumber + "\n"
								+ "next packet # should be " + (ackNumber + 1) + " <-----"  + "\n" + "\n");
					}
				}//end of if (cksumValue == 0)

			} catch (SocketTimeoutException | NullPointerException npe) {

			} catch (SocketException se) {
				System.exit(0);
			}
		}
	}

	/**
	 * Write the name of file that was sent by sender to output file
	 * @param packageString
	 * @throws IOException
	 */
	public static void writeToFile(String packageString) throws IOException {
		output = new FileOutputStream(new File("output_" + packageString));
	}
	/**
	 * Write content of file that was sent by sender to output file
	 * @param packageByte
	 * @throws IOException
	 */
	public static void writeToFile(byte[] packageByte) throws IOException {
		output.write(packageByte);
	}
}