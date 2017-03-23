package receiver.thread;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Random;

public class ThreadThree implements Runnable {

    private int windowSize;
    private int corruption;
    private int port;
    private String ipAddress;
    private DatagramSocket socket;
    private FileOutputStream output = null;
    //variable to be used by threadFour
    private DatagramPacket packet;
    private int ackNumber;
    private int expectedPacketNumber;
    private int checksumValue;
    private int oldPacketNumber;
    
    public ThreadThree(int windowSize, int corruption, int port, String ipAddress, DatagramSocket socket) {
        super();
        this.windowSize = windowSize;
        this.corruption = corruption;
        this.port = port;
        this.ipAddress = ipAddress;
        this.socket= socket;
    }

    public int getWindowSize() {
        return this.windowSize;
    }

    public void setWindowSize(int windowSize) {
        this.windowSize = windowSize;
    }

    public int getCorruption() {
        return this.corruption;
    }

    public void setCorruption(int corruption) {
        this.corruption = corruption;
    }

    public int getPort() {
        return this.port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getIpAddress() {
        return this.ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }
    
    public DatagramSocket getSocket() {
        return this.socket;
    }

    public void setSocket(DatagramSocket socket) {
        this.socket = socket;
    }

    public DatagramPacket getPacket() {
        return this.packet;
    }

    public void setPacket(DatagramPacket packet) {
        this.packet = packet;
    }

    public int getAckNumber() {
        return this.ackNumber;
    }

    public void setAckNumber(int ackNumber) {
        this.ackNumber = ackNumber;
    }

    public int getExpectedPacketNumber() {
        return this.expectedPacketNumber;
    }

    public void setExpectedPacketNumber(int expectedPacketNumber) {
        this.expectedPacketNumber = expectedPacketNumber;
    }

    public int getChecksumValue() {
        return this.checksumValue;
    }

    public void setChecksumValue(int checksumValue) {
        this.checksumValue = checksumValue;
    }

    public int getOldPacketNumber() {
        return this.oldPacketNumber;
    }

    public void setOldPacketNumber(int oldPacketNumber) {
        this.oldPacketNumber = oldPacketNumber;
    }

    @Override
    public void run() {
        while(true){
        // object to generate random number
        Random random = new Random();
        // supported character type
        String characterSet = "UTF-8";

        // variable to to store package properties
        String seqNumber = "";
        String packageString = "";
        byte[] packageByte;
        // packet and ack number that will be used keep track of packets
        int currentPacketNumber = 1;
        int ackNumber = 1;
        // checksum value coming from sender
        int cksumValue = 0;
        // variable to store data that comes from sender
        byte[] data = new byte[1024];
        // variable to keep track of packets that arrived and are coming
        int oldPacketNumber = 0;
        int expectedPacketNumber = 1;

        // variable to extract checksum and seq
        String cksum = "";
        seqNumber = "";
        // packet received from sender
        DatagramPacket receivePacket = new DatagramPacket(data, data.length);
        try {
            socket.receive(receivePacket);
        } catch (IOException ex) {
            System.out.println("Error while receiving packet");
        }

        // parse the data to get checksum
        for (int i = 0; i < 3; i++) {
            cksum = cksum + data[i];
        }
        // convert checksum into a number
        cksumValue = Integer.parseInt(cksum);

        // parse data to get current packet number
        for (int i = 8; i < 12; i++) {
            seqNumber = seqNumber + data[i];
        }
        // convert seq into a number
        currentPacketNumber = Integer.parseInt(seqNumber);
        // print out what packet is coming
        System.out.println("Waiting on packet # " + expectedPacketNumber + "\n");

        // check whether we have received packet before
        if (oldPacketNumber != currentPacketNumber) {
            // if this packet has not arrived before then we
            // receiving it for first time
            System.out.println("[RECV] Packet # " + currentPacketNumber + "\n");
        } else {
            // otherwise this packet came before
            System.out.println("[RECV] [DUPL] Packet # " + currentPacketNumber + "\n");
        }

        // if the cksumValue is not zero packet is [CRPT] exit out
        if (cksumValue != 0) {
            System.out.println("[CRPT] packet # " + currentPacketNumber + " and need to recieve again  <-----" + "\n");
            // note down this packetNumber since it arrived but was
            // could not proceed further
            oldPacketNumber = currentPacketNumber;
            continue;// start from the while loop again
        }
        // we need to randomly [DROP] packet and exit out
        if (corruption > 0) {
            if (random.nextInt(10) == 5) {
                System.out.println("[DROP] packet # " + currentPacketNumber + " <-----\n");
                // note down this packetNumber since it arrived but was
                // could not proceed further
                oldPacketNumber = currentPacketNumber;
                continue;// start from the while loop again
            }
        }
        // get the data that was sent
        packageByte = new byte[receivePacket.getLength() - 12];
        for (int i = 0; i < packageByte.length; i++) {
            packageByte[i] = data[i + 12];
        }

        // if checksum value equals zero and no error occurred above
        // proceed with creating ack
        if (cksumValue == 0) {
            // assign ack for this packetNumber
            ackNumber = currentPacketNumber;
            try {
                // write the data in the new file
                if (currentPacketNumber == 1) {
                    // if its the first packet then it has the name of file
                    // get name of file and write to output file
                    if (!packageString.equals(new String(packageByte, characterSet))) {
                        packageString = new String(packageByte, characterSet);
                        writeToFile(packageString);
                    }
                } else {
                    // otherwise the packet has content of file, get the
                    // content and write to output file
                    if (!packageString.equals(new String(packageByte, characterSet))) {
                        packageString = new String(packageByte, characterSet);
                        writeToFile(packageByte);
                    }
                }
            } catch (IOException ex) {
                System.out.println("Error happened while writing to file");
            }//end of catch
        }//end of if (cksumValue == 0)
        setPacket(receivePacket);//set the packet that was received
        setAckNumber(ackNumber);
        setChecksumValue(cksumValue);
        setExpectedPacketNumber(expectedPacketNumber);
        setOldPacketNumber(oldPacketNumber);
        }//end of while
    }
   
            /**
             * Write the name of file that was sent by sender to output file
             * @param packageString
             * @throws IOException
             */
            public void writeToFile(String packageString) throws IOException {
                output = new FileOutputStream(new File("output_" + packageString));
            }
            /**
             * Write content of file that was sent by sender to output file
             * @param packageByte
             * @throws IOException
             */
            public void writeToFile(byte[] packageByte) throws IOException {
                output.write(packageByte);
            }
        

}
