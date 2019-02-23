import java.net.DatagramPacket;
import java.util.Comparator;

/**
 * Custom comparator for priority queue used to reorder received packets
 */
public class SequenceNumberComparator implements Comparator<DatagramPacket>{
    private static FileHandler fileHandler = new FileHandler();

    /**
     * Compares the first packet with the second one using their sequence numbers
     * @param p1 - packet 1
     * @param p2 - packet 2
     * @return
     */
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
