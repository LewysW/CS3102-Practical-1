import java.net.DatagramPacket;

/**
 * Class for storing meta data about a packet for the purposes of transmission
 */
public class PacketHandler {
    /*
    Stores the packet, the time the packet was sent,
    and whether the packet has been acknowledged
     */
    private DatagramPacket packet;
    private long time;
    private boolean acked;

    /**
     * Constructor for PacketHandler,
     * assigns the packet, set acknowledged initially to false
     * and the time initially to -1 (meaning not yet sent)
     * @param packet
     */
    public PacketHandler(DatagramPacket packet) {
        this.packet = packet;
        this.acked = false;
        this.time = -1;
    }

    /**
     * Getter for packet
     * @return datagram packet
     */
    public DatagramPacket getPacket() {
        return packet;
    }

    /**
     * Getter for time of transmission
     * @return - time packet was transmitted
     */
    public long getTime() {
        return time;
    }

    /**
     * Setter for transmission time
     * @param time - time packet was transmitted
     */
    public void setTime(long time) {
        this.time = time;
    }

    /**
     * Getter for acked
     * @return - whether packet has been acknowledged by client
     */
    public boolean isAcked() {
        return acked;
    }

    /**
     * Setter for acked
     * @param acked - whether packet has been acknowledged
     */
    public void setAcked(boolean acked) {
        this.acked = acked;
    }
}
