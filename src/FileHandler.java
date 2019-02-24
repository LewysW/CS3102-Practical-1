import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Class to convert between data/file types, as well as read and write data from/to files.
 */
public class FileHandler {
    //Size of int for conversion between array of bytes and integer
    public static final int SIZE_OF_INT = 4;
    //Arbitrary timestamp interval, timestamps not used
    public static final int TIMESTAMP_INTERVAL = 20;
    //Size of a packet (sequence number + timestamp + payload)
    public static final int PACKET_SIZE = 224;
    public static final int SEQUENCE_SIZE = 4;
    public static final int TIMESTAMP_SIZE = 4;
    public static final int PAYLOAD_SIZE = 216;

    /**
     * Reads a file into a byte array given its name
     * @param fileName - name of file
     * @return the byte array corresponding to the file read in
     * @throws IOException
     */
    public byte[] read(String fileName) throws IOException {
        Path path = Paths.get(fileName);
        return Files.readAllBytes(path);
    }

    public void write(String path, byte[] data) throws IOException {
        FileOutputStream stream = new FileOutputStream(path);
        stream.write(data);
    }

    /**
     * Converts a file (as an array of bytes) to an array list of datagram packets
     * @param file - raw file data to be converted to a list of packets
     * @param ip - host address packets are destined for
     * @param port - port of process to deliver packet to
     * @return - an array list of datagram packets bound for a given ip address and port
     */
    public ArrayList<DatagramPacket> toPacketList(byte[] file, InetAddress ip, int port) {
        ArrayList<DatagramPacket> packets = new ArrayList<>();
        int seqNum = 0;
        int timeStamp = 0;

        //Iterates through file data by the length of a single packet payload
        for (int i = 0; i < file.length; i += PAYLOAD_SIZE) {
            byte[] packetData = new byte[PACKET_SIZE];

            //Copies sequence number byte array to packet
            System.arraycopy(intToBytes(seqNum++), 0, packetData, 0, SEQUENCE_SIZE);

            //Copies time stamp byte array to packet
            System.arraycopy(intToBytes(timeStamp += TIMESTAMP_INTERVAL), 0, packetData, SEQUENCE_SIZE, TIMESTAMP_SIZE);

            //Copies audio file data (payload) to packet if amount of data remaining is the size of a payload
            if (i + PAYLOAD_SIZE < file.length) {
                System.arraycopy(file, i, packetData, SEQUENCE_SIZE + TIMESTAMP_SIZE, PAYLOAD_SIZE);
            } else {
                //Copies audio file data if rest of file is less than the payload of a packet
                System.arraycopy(file, i, packetData, SEQUENCE_SIZE + TIMESTAMP_SIZE, file.length - i);
            }

            //Adds the packet to the packet list
            DatagramPacket packet = new DatagramPacket(packetData, PACKET_SIZE, ip, port);
            packets.add(packet);
        }

        return packets;
    }

    /**
     * Converts a list of datagram packets to an array of bytes
     * @param packets - list of packets to convert to bytes
     * @return
     */
    public byte[] toByteArray(List<DatagramPacket> packets) {
        byte[] fileData = new byte[packets.size() * PACKET_SIZE];
        int index = 0;

        //Iterates over each packet and copies the payload data to the array of bytes
        for (DatagramPacket packet: packets) {
            System.arraycopy(packet.getData(), SEQUENCE_SIZE + TIMESTAMP_SIZE, fileData, index, PAYLOAD_SIZE);
            index += PAYLOAD_SIZE;
        }
        return fileData;
    }

    /**
     * SOURCE FOR CONVERTING INTS TO BYTES AND VICE VERSA:
     * https://stackoverflow.com/questions/6374915/java-convert-int-to-byte-array-of-4-bytes
     */

    /**
     * Converts an integer to an array of bytes in little endian order
     * @param myInt - integer to convert
     * @return array of bytes
     */
    public byte[] intToBytes(int myInt) {
        return ByteBuffer.allocate(SIZE_OF_INT).order(ByteOrder.LITTLE_ENDIAN).putInt(myInt).array();
    }

    /**
     * Converts a little endian array of bytes to an integer
     * @param myBytes - array of bytes to convert to an integer
     * @return integer
     */
    public int bytesToInt(byte[] myBytes) {
        return ByteBuffer.wrap(myBytes).order(ByteOrder.LITTLE_ENDIAN).getInt();
    }

    /**
     * Extracts and returns the sequence number of a datagram packet
     * @param packet - packet to get sequence number from
     * @return sequence number
     */
    public int getSequenceNumber(DatagramPacket packet) {
        byte[] data = Arrays.copyOfRange(packet.getData(), 0, SEQUENCE_SIZE);
        return bytesToInt(data);
    }

    /**
     * Extracts the payload of a datagram packet and returns it
     * @param packet - to get payload of
     * @return datagram packet payload
     */
    public byte[] getPayload(DatagramPacket packet) {
        return Arrays.copyOfRange(packet.getData(), SEQUENCE_SIZE + TIMESTAMP_SIZE, PACKET_SIZE);
    }
}
