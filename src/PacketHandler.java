import java.net.DatagramPacket;

public class PacketHandler {
    private DatagramPacket packet;
    private long time;
    private boolean acked;

    public PacketHandler(DatagramPacket packet) {
        this.packet = packet;
        this.acked = false;
        this.time = -1;
    }

    public DatagramPacket getPacket() {
        return packet;
    }

    public void setPacket(DatagramPacket packet) {
        this.packet = packet;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public boolean isAcked() {
        return acked;
    }

    public void setAcked(boolean acked) {
        this.acked = acked;
    }
}
