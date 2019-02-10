import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

public class FileHandler {
    public byte[] read(String fileName) throws IOException {
        Path path = Paths.get(fileName);
        return Files.readAllBytes(path);
    }

    public void write(String path, byte[] data) throws IOException {
        FileOutputStream stream = new FileOutputStream(path);
        stream.write(data);
    }

    public ArrayList<DatagramPacket> toPacketList(byte[] file) {
        return null;
    }

    public byte[] toByteArray(ArrayList<DatagramPacket> packets) {
        return null;
    }
}
