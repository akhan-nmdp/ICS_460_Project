package main;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;
/**
 * Class responsible of reading data from text file and putting data in packets
 *
 */
public class PacketBuilder {
    
    private final String defaultPath= "C:\\DevApps\\GitLocal\\ICS_460_Project\\src\\resources\\";
    private String fileName= "test.txt";
    /**
     * Read data from file and puts data in packets
     * @param fileName
     * @param packetLength
     * @return
     * @throws FileNotFoundException 
     */
    public LinkedList<Packet> readFile(int packetLength) throws FileNotFoundException {
        // create a new data file from default path
        LinkedList<Packet> packets = null;
        BufferedReader bufferedReader = null;

        FileReader fileReader = new FileReader(defaultPath + fileName);
        if (fileReader != null) {
            // store the content of the file
            StringBuilder contents = new StringBuilder();
            String line;

            try {
                bufferedReader = new BufferedReader(fileReader);
                // read the lines from the files and add it to content
                while ((line = bufferedReader.readLine()) != null) {
                    contents.append(line + "\n");
                }
                // once all data is present in content create the packets
                packets = buildPackets(fileName, contents.toString(), packetLength);

            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    // close out the bufferedReader and fileReader
                    if (bufferedReader != null)
                        bufferedReader.close();
                    if (fileReader != null)
                        fileReader.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }//end of finally try/catch block
            }//end of finally

        }//end of fileReader != null
        return packets;
    }
    
    /**
     * build the packet based on packent length
     * @param fileName
     * @param dataFromFile
     * @param payload
     * @return
     */
    private LinkedList<Packet> buildPackets(String fileName, String dataFromFile, int payload) {
        LinkedList<byte[]> dataList = new LinkedList<byte[]>();
        LinkedList<Packet> packetList = new LinkedList<Packet>();
        byte[] fullData = dataFromFile.getBytes();
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
