import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.Date;

public class Server {
    //Interface to sends packets to client
    private DatagramSocket serverSocket;
    //Handler to convert been files and bytes
    private FileHandler handler;
    //Array to store raw audio data
    private byte[] fileData;
    //Default timeout period for a transmitted packet
    private static double ACK_TIMEOUT = 1000;
    //Multiple to determine size of SR buffer (determined by multiplying this by average delay)
    private  static final int BUFFER_DELAY_FACTOR = 10;
    //Initial buffer size
    private static final int INITIAL_BUFFER_SIZE = 50;
    //Maximum possible buffer size
    private static final int MAX_BUFFER_SIZE = 2500;
    //Constant to mark packet as not sent
    private static final int PACKET_NOT_SENT = -1;


    /**
     Used the sample client-server UDP code from Computer Networking: A Top Down Approach, by Kurose and Ross
     */

    /**
     * Constructor for server
     * Assigns socket, file handler for converting file to packet list, and file data to stream
     * @param port
     * @param fileName
     * @throws Exception
     */
    private Server(int port, String fileName) throws Exception {
        serverSocket = new DatagramSocket(port);
        handler = new FileHandler();
        fileData = handler.read(fileName);
    }

    /**
     * Runs server to stream audio files.
     * Waits for requests in the form of "MARCO" and sends a "POLO" acknowledgment, then streams the audio file
     * @throws Exception
     */
    private void run() throws Exception {
        //Stores the packets in preparation for transmission
        ArrayList<DatagramPacket> packetList;

        //Creates server socket, client socket, and output and input objects
        byte[] incomingData = new byte[handler.PACKET_SIZE];
        byte[] outgoing;

        DatagramPacket receivedPacket = new DatagramPacket(incomingData, incomingData.length);
        serverSocket.setSoTimeout(0);
        //Loops and listens for incoming segments
        while (true) {
            serverSocket.receive(receivedPacket);

            //Converts datagram data to string and displays it
            String message = new String(receivedPacket.getData());
            System.out.println("MESSAGE: " + message);

            //Gets ip address and port of sender, and data to send back
            InetAddress ip = receivedPacket.getAddress();
            int port = receivedPacket.getPort();

            //If a client has sent the message "MARCO", respond with "POLO" and send file
            if (message.startsWith("MARCO")) {
                outgoing = ("POLO").getBytes();
                serverSocket.send(new DatagramPacket(outgoing, outgoing.length, ip, port));

                packetList = handler.toPacketList(fileData, ip, port);
                System.out.println("TOTAL NUMBER OF PACKETS TO TRANSMIT: "  + packetList.size());

                srBufferStream(packetList, receivedPacket);

                System.out.println("TRANSMISSION FINISHED");
            }
        }
    }

    /**
     * Uses selective repeat in order to transmit the packets of the audio file to the client
     * @param packetList - list of packets to be transmitted
     * @param receivedPacket - incoming packet
     * @throws IOException
     */
    public void srBufferStream(ArrayList<DatagramPacket> packetList, DatagramPacket receivedPacket) throws IOException {
        ArrayList<PacketHandler> srBuffer = new ArrayList<>();

        //Used to store the start time of transmission, the elapsed time since a packet was sent
        // and the total delay of packets sent and acknowledged
        long start = System.currentTimeMillis();
        long totalDelay = 0;

        //Sets the lower and upper boundaries of the SR buffer, upper boundary is initially a default value
        int BASE = 0;
        int N = INITIAL_BUFFER_SIZE;

        //Initialises the SR buffer with the first N packets
        for (int i = 0; i < N; i++) srBuffer.add(new PacketHandler(packetList.get(i)));


        //Iterates through packets while SR buffer has not reached end of packet list or there are still packets in the buffer
        while (N < packetList.size() || !srBuffer.isEmpty()) {
            if (BASE == INITIAL_BUFFER_SIZE) {
                N = resizeBuffer(srBuffer, packetList, N, calcBufferSize(totalDelay / INITIAL_BUFFER_SIZE));
            }

            //Shifts the boundaries of the SR (Selective Repeat) buffer if the first packet has been acknowledged
            if (srBuffer.get(BASE % (N - BASE)).isAcked()) {
                srBuffer.remove(BASE % (N - BASE));

                if (N < packetList.size()) {
                    srBuffer.add(new PacketHandler(packetList.get(N++)));
                }

                BASE++;
                continue;
            }

            //Sends packets that have not been sent, or resends packets that have timed out
            sendPackets(srBuffer, BASE, N, start);

            //Receives ack packet
            serverSocket.receive(receivedPacket);

            //Acknowledges packets that have been received and returns the delay of those packets in being acknowledged
            totalDelay += acknowledgePackets(srBuffer, receivedPacket, start);
        }
    }

    /**
     * Sends packets which have not been acknowledged or have not yet been sent
     * within a specified timeout period
     * @param srBuffer - buffer of packets to send
     * @param BASE - lower bound packet in buffer
     * @param N - upper bound packet in buffer
     * @param start - time since the start of transmission
     * @throws IOException
     */
    public void sendPackets(ArrayList<PacketHandler> srBuffer, int BASE, int N, long start) throws IOException {
        long elapsed;

        //Iterates over the SR buffer and works out whether the resend the packets
        for (int i = BASE % (N - BASE); i < (N - BASE); i++) {
            PacketHandler current = srBuffer.get(i);
            elapsed = (new Date().getTime() - start);

            //If packets has not been sent, or its ack time has elapsed - resend. Otherwise if it has been acknowledged
            //do nothing
            if ((current.getTime() == PACKET_NOT_SENT || elapsed - current.getTime() > ACK_TIMEOUT) && !current.isAcked()) {
                srBuffer.get(i).setTime(new Date().getTime() - start);
                serverSocket.send(srBuffer.get(i).getPacket());
            }
        }
    }

    /**
     * Marks a packet as acknowledged if its acknowledgement packets have been received
     * @param srBuffer - buffer containing packets to send/acknowledge
     * @param receivedPacket - most recently received packet
     * @param start - time of start of transmission
     * @return
     */
    public long acknowledgePackets(ArrayList<PacketHandler> srBuffer, DatagramPacket receivedPacket, long start) {
        long delay = 0;
        long sampleRTT;
        double alpha = 0.125;
        //If the sequence number of the ack packet matches one of the packets in the buffer, mark as acknowledged
        for (PacketHandler packetHandler: srBuffer) {
            if (handler.getSequenceNumber(packetHandler.getPacket()) == handler.getSequenceNumber(receivedPacket)) {
                packetHandler.setAcked(true);

                //Adjusts timeout time of packets in real time as the delay changes
                sampleRTT = (new Date().getTime() - packetHandler.getTime() - start);
                ACK_TIMEOUT = (1 - alpha) * ACK_TIMEOUT + (alpha) * sampleRTT;
                delay += ACK_TIMEOUT;
                System.out.println(ACK_TIMEOUT + "ms");
                break;
            }
        }

        return delay;
    }

    /**
     * Uses the average delay of packet acknowledgements to determine the optimal SR buffer size
     * @param averageDelay - used to calculate new buffer size
     * @return - the new size of the SR buffer based on the delay, or the maximum buffer size if too large
     */
    public int calcBufferSize(long averageDelay) {
        if (INITIAL_BUFFER_SIZE + (averageDelay * BUFFER_DELAY_FACTOR) > MAX_BUFFER_SIZE) {
            return MAX_BUFFER_SIZE;
        } else if (((averageDelay * BUFFER_DELAY_FACTOR) < INITIAL_BUFFER_SIZE)) {
            return INITIAL_BUFFER_SIZE;
        }

        return (int) (INITIAL_BUFFER_SIZE + (averageDelay * BUFFER_DELAY_FACTOR));
    }


    /**
     * Resizes the buffer used to store and send packets
     * @param srBuffer - buffer to resize
     * @param packets - packets to add to end of buffer
     * @param N - upper packet contained in buffer
     * @param newUpperLimit - new upper limit of N
     * @return - returns the number of the new uppermost packet of the buffer
     */
    public int resizeBuffer(ArrayList<PacketHandler> srBuffer, ArrayList<DatagramPacket> packets, int N, int newUpperLimit) {
        while (N != newUpperLimit) {
            srBuffer.add(new PacketHandler(packets.get(N++)));
        }

        return N;
    }

    public static void main(String[] args) {
        if (args.length == 2) {
            try {
                Server server = new Server(Integer.parseInt(args[0]), args[1]);
                server.run();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("Usage: 'java Server <port> <audiofile>'");
        }
    }
}
