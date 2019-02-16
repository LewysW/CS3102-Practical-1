import java.net.DatagramPacket;
import java.util.Comparator;

public class SequenceNumberComparator implements Comparator<DatagramPacket>{
    private static FileHandler fileHandler = new FileHandler();


    public int compare(DatagramPacket p1, DatagramPacket p2) {
        if (fileHandler.getSequenceNumber(p1) > fileHandler.getSequenceNumber(p2)) {
            return 1;
        } else if (fileHandler.getSequenceNumber(p1) < fileHandler.getSequenceNumber(p2)) {
            return -1;
        } else {
            return 0;
        }
    }
}
