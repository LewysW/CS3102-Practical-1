import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;

public class FileHandler {
    public static final int PACKET_SIZE = 512;
    //public static final int PAYLOAD_SIZE

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

        for (int i = 0; i < file.length; i += PACKET_SIZE) {
            byte[] packetData = Arrays.copyOfRange(file, i, i + PACKET_SIZE);
            DatagramPacket packet = new DatagramPacket(packetData, PACKET_SIZE, ip, port);
            packets.add(packet);
        }

        return packets;
    }

    public byte[] toByteArray(ArrayList<DatagramPacket> packets) {
        byte[] fileData = new byte[packets.size() * PACKET_SIZE];
        int index = 0;

        for (DatagramPacket packet : packets) {
            System.arraycopy(packet.getData(), 0, fileData, index, packet.getLength());
            index += packet.getLength();
        }

        return fileData;
    }
}
