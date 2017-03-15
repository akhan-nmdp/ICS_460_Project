package main;

import java.io.*;
import java.net.*;
import java.util.Random;
import java.util.Scanner;

public class Receiver {
	// initialize default port to 7
	public final static int DEFAULT_PORT = 7;
	// declare integer buffer size and port number
	private final int bufferSize;
	private final int port;

	static String hostname = "localhost";

	// FileOutputStream for writeToFile method
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
		// Scanner input = new Scanner(System.in);

		String seqNumber = "";
		String packageString = "";
		Random random = new Random();

		byte[] packageByte;
		double corruption = 0;
		int timeout = 1000;
		int currentPacketNumber = 1;
		int cksumValue = 0;
		int ackNumber = 1;
		//int counter = 0;
		byte[] data = new byte[1024];
		int oldPacketNumber = 0;
		int prevPacketNumber= 0;
		int expectedPacketNumber= 1;
		int port = DEFAULT_PORT;

		// way to get data from command line
        if (args.length > 0) {
            corruption = Integer.parseInt(args[0]);
            hostname = args[1];
            port = Integer.parseInt(args[2]);
        }
		/* Scanner inputData = new Scanner(System.in);
		 System.out.println("Please enter the ip address (ex: localhost):");
	        hostname= inputData.next();
	        System.out.println("Please enter the port number:");
	        port=inputData.nextInt();
	        System.out.println("Please enter the percentage of packet that should be corupputed while sending data:");
	        corruption = inputData.nextInt();
	        System.out.println("Please enter the time(in ms)to resend the packet: ");
	        timeout = inputData.nextInt();*/

		// create receiver's socket and set the default port
		DatagramSocket receiverSocket = new DatagramSocket(port);

		/*
		 * //Gather the corruption and the timeout length from the user
		 * System.out.
		 * println("Please enter the percentage of packet that should be corrupted while sending data: "
		 * ); corruption = input.nextInt();
		 * 
		 * System.out.
		 * println("Please enter the time(in ms) to resend the packet, if it gets lost: "
		 * ); timeout = input.nextInt();
		 */

		receiverSocket.setSoTimeout(timeout);

		// close the scanner after data is entered
		// input.close();

		while (true) {
			try {
				String cksum = "";
				seqNumber = "";

				// packet received from sender
				DatagramPacket receivePacket = new DatagramPacket(data, data.length);

				//Random lost = new Random();
				// if corruption/2 is more than random number between 1-100,
				// then data was lost
				/*if (lost.nextInt(100) < (corruption / 2)) {
					receivePacket = null;
				}*/
				
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
				
				//System.out.println("Waiting on packet # " + currentPacketNumber + "...");
				//System.out.println("Waiting on packet # "+ oldPacketNumber );
				System.out.println("Waiting on packet # "+ expectedPacketNumber);
				
				//check whether we have received packet before 
				if (oldPacketNumber != currentPacketNumber) {
				    System.out.println("Packet # " + currentPacketNumber + " is [RECV] \n" );
              } else {
                  System.out.println("Packet # " + currentPacketNumber + " is [DUPL]" + "\n");
              }
				//if the packet is not what we expecting exit out of loop
				/*if (expectedPacketNumber != currentPacketNumber){
				    System.out.println("[!Seq] Sender sent " +  currentPacketNumber + " when it should have sent "+ expectedPacketNumber);
				    continue;//break out of the while loop
				}*/
				
				// if the cksumValue is not zero packet is [CRPT] exit out
				if (cksumValue != 0) {
					System.out.println("packet # " + currentPacketNumber + " is [CRPT] need to recieve again" + "\n");
					oldPacketNumber= currentPacketNumber;
					continue;//break out of while loop
				}
				
				if (corruption > 0) {
                    if (random.nextInt(10) == 5) {
                        System.out.println("[DROP] packet # " + currentPacketNumber + "\n");
                        oldPacketNumber= currentPacketNumber;
                        //droppedPacket = currentPacket;
                        continue;// start from the while loop again
                    } 
				}
                    //this condition is not needed as receiever doesnt need to send negative ACK
//                    else if (random.nextInt(10) == 6) {
//                        // this packet is bad packet
//                        System.out.println("[ERRR] packet # " + currentPacketNumber);
//                        // check if we need to make this a good or bad packet
//                        //currentPacket.setCksum(badCheckSum);
//                        continue;//break out of while loop
//                    } 

				packageByte = new byte[receivePacket.getLength() - 12];
				for (int i = 0; i < packageByte.length; i++) {
					packageByte[i] = data[i + 12];
				}

				// if checksum value equals zero and no error occurred above proceed with creating 
				if (cksumValue == 0) {
//					if (oldPacketNumber == currentPacketNumber || ) {
//	                      System.out.println("Packet # " + currentPacketNumber + " is [RECV] \n" );
//					} else {
//					    System.out.println("Packet # " + currentPacketNumber + " is [DUPL]" + "\n");
//						//+ "[SENDing]: [ACK] ");
//								//+ currentPacketNumber + "...");
//					}
					//counter--;
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

					// if random number between 1-100 is less or equal to corruption number, then set checksum value is 1 else 0
//					if (random.nextInt(100) <= corruption) {
//						// if the above condition is true then print out the [drop] msg for ack (akhan)
//						System.out.println("[ACK] # " + currentPacketNumber + " is [DROP]");
//						ackPacket.setCksum((short) 1);
//					} else {
						// otherwise print you are preaprig an ack
						//System.out.println("Preparing an [ACK] for packet number "+ ackNumber);
						//ackPacket.setCksum((short) 0);
					//}
					//randomly drop ACK packets and start over
                    if (corruption > 0) {
                        if (random.nextInt(10) == 7) {
                            System.out.println("[DROP] ACK for packet number "+ ackNumber + "\n");
                            oldPacketNumber= ackNumber;
                            continue;//break out of while loop
                        } 
                       /* else if (random.nextInt(10) == 8){
                            System.out.println("[ERRR] ACK for packet number "+ ackNumber + "\n");
                            ackPacket.setCksum((short) 1);
                        }*/
                    }
					
					// send acknowledgement to the sender
					DatagramPacket ack = new DatagramPacket(ackPacket.getData(), ackPacket.getLength(),
							receivePacket.getAddress(), receivePacket.getPort());
					receiverSocket.send(ack);
					//assign oldpacketNumber to the packet that was just acked
					oldPacketNumber = ackNumber;
					//check which packet we need to expect next
                    if (ackPacket.getCksum() == 0) {
                        // increase the packetnumber once ack was sent
                        expectedPacketNumber++;
                        System.out.println("[ACK] [SENT] for packet number " + ackNumber + "\n" + " next packet # should be " + (ackNumber + 1) + "\n"+ "\n");
                    } 
                    /*else { NO NEED TO SENDING NEGATIVE ACK
                        // otherwise if there was an ErrAck sent we still are
                        // expecting the same packet
                        expectedPacketNumber = ackNumber;
                        System.out.println("[ERRR] ACK occurred need to receieve packet # " + expectedPacketNumber + "\n");
                    }*/
				}

				// if checksum value not equal zero, current packet is corrupted and waits for re-send
				/*
				 * if (cksumValue != 0) { System.out.println("Packet # " +
				 * currentPacketNumber + " [CRPT]"); }
				 * 
				 * if (cksumValue != 0) { System.out.println("Packet # " +
				 * currentPacketNumber + " LOST\n" +
				 * "Waiting on the [ReSend]...\n"); }
				 */

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