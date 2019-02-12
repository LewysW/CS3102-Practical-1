import javax.xml.crypto.Data;
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

import static java.util.Arrays.copyOfRange;

//TODO - add functions to extract data from a single packet
public class FileHandler {
    public static final int SIZE_OF_INT = 4;
    //20ms timestamp interval
    public static final int TIMESTAMP_INTERVAL = 20;
    public static final int PACKET_SIZE = 232;
    public static final int SEQUENCE_SIZE = 4;
    public static final int TIMESTAMP_SIZE = 4;
    public static final int PAYLOAD_SIZE = 224;

    public byte[] read(String fileName) throws IOException {
        Path path = Paths.get(fileName);
        return Files.readAllBytes(path);
    }

    public void write(String path, byte[] data) throws IOException {
        FileOutputStream stream = new FileOutputStream(path);
        stream.write(data);
    }

    public ArrayList<DatagramPacket> toPacketList(byte[] file, InetAddress ip, int port) {
        ArrayList<DatagramPacket> packets = new ArrayList<>();
        int seqNum = 0;
        int timeStamp = 0;

        for (int i = 0; i < file.length; i += PAYLOAD_SIZE) {
            byte[] packetData = new byte[PACKET_SIZE];

            //Copies sequence number byte array to packet
            System.arraycopy(intToBytes(seqNum++), 0, packetData, 0, SEQUENCE_SIZE);

            //Copies time stamp byte array to packet
            System.arraycopy(intToBytes(timeStamp += TIMESTAMP_INTERVAL), 0, packetData, SEQUENCE_SIZE, TIMESTAMP_SIZE);

            //Copies audio file data (payload) to packet if amount of data remaining is >= 504 bytes
            if (i + PAYLOAD_SIZE < file.length) {
                System.arraycopy(file, i, packetData, SEQUENCE_SIZE + TIMESTAMP_SIZE, PAYLOAD_SIZE);
            } else {
                //Copies audio file data if rest of file is less than the payload of a packet
                //TODO - null terminate rest of packet
                System.arraycopy(file, i, packetData, SEQUENCE_SIZE + TIMESTAMP_SIZE, file.length - i);
            }

            DatagramPacket packet = new DatagramPacket(packetData, PACKET_SIZE, ip, port);
            packets.add(packet);
        }

        return packets;
    }

    public byte[] toByteArray(ArrayList<DatagramPacket> packets) {
        byte[] fileData = new byte[packets.size() * PACKET_SIZE];
        int index = 0;

        for (DatagramPacket packet : packets) {
            System.arraycopy(packet.getData(), SEQUENCE_SIZE + TIMESTAMP_SIZE, fileData, index, PAYLOAD_SIZE);
            index += PAYLOAD_SIZE;
        }

        return fileData;
    }

    public byte[] toByteArray(DatagramPacket packet) {
        byte[] fileData = new byte[PACKET_SIZE];
        System.arraycopy(packet.getData(), SEQUENCE_SIZE + TIMESTAMP_SIZE, fileData, 0, PAYLOAD_SIZE);

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

    public int getSequenceNumber(DatagramPacket packet) {
        byte[] data = Arrays.copyOfRange(packet.getData(), 0, SEQUENCE_SIZE);
        return bytesToInt(data);
    }

    public int getTimeStamp(DatagramPacket packet) {
        byte[] data = Arrays.copyOfRange(packet.getData(), SEQUENCE_SIZE, SEQUENCE_SIZE + TIMESTAMP_SIZE);
        return bytesToInt(data);
    }

    public byte[] getPayload(DatagramPacket packet) {
        return Arrays.copyOfRange(packet.getData(), SEQUENCE_SIZE + TIMESTAMP_SIZE, PACKET_SIZE);
    }
}
