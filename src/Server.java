import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class Server {
    private DatagramSocket serverSocket;
    private FileHandler handler;
    private byte[] fileData;
    //Timeout is 100ms TODO - dynamically assign timeout time
    private static double ACK_TIMEOUT = 1000;
    //Size of selective resend buffer in number of packets
    //private static final int SR_BUFFER_SIZE = 60;
    private static final int SR_BUFFER_SIZE = 2000;
    private  static final int BUFFER_DELAY_FACTOR = 13;
    private static final int TEST_PACKET_NUM = 100;


    /**
     Used the sample client-server UDP code from Computer Networking: A Top Down Approach, by Kurose and Ross
     */

    private Server(int port, String fileName) throws Exception {
        serverSocket = new DatagramSocket(port);
        handler = new FileHandler();
        fileData = handler.read(fileName);
    }

    private void run() throws Exception {
        //Stores the packets in preparation for transmission
        ArrayList<DatagramPacket> packetList;

        //Creates server socket, client socket, and output and input objects
        byte[] incomingData = new byte[handler.PACKET_SIZE];
        byte[] outgoing = new byte[handler.PACKET_SIZE];

        DatagramPacket receivedPacket = new DatagramPacket(incomingData, incomingData.length);

        //Loops and listens for incoming connections
        while (true) {
            serverSocket.receive(receivedPacket);

            //Converts datagram data to string and displays it
            String message = new String(receivedPacket.getData());
            System.out.println("MESSAGE: " + message);

            //Gets ip address and port of sender, and data to send back
            InetAddress ip = receivedPacket.getAddress();
            int port = receivedPacket.getPort();

            if (message.startsWith("MARCO")) {
                outgoing = ("POLO").getBytes();
                serverSocket.send(new DatagramPacket(outgoing, outgoing.length, ip, port));

                packetList = handler.toPacketList(fileData, ip, port);
                System.out.println(packetList.size());

                ArrayList<PacketHandler> srBuffer = new ArrayList<>();
                long start = System.currentTimeMillis();
                long elapsed;

                int BASE = TEST_PACKET_NUM;
                int N = TEST_PACKET_NUM + calcBufferSize(packetList, receivedPacket, TEST_PACKET_NUM, start);

                System.out.println("BASE: " + BASE + " N: " + N);

                for (int i = 0; i < N; i++) srBuffer.add(new PacketHandler(packetList.get(i)));


                //Iterates through packets while SR buffer has not reached end of packet list
                while (N < packetList.size() || !srBuffer.isEmpty()) {
                    //Shifts the boundaries of the SR (Selective Resend) buffer if the first packet has been acknowledged
                    if (srBuffer.get(BASE % (N - BASE)).isAcked()) {
                        srBuffer.remove(BASE % (N - BASE));

                        if (N < packetList.size()) {
                            srBuffer.add(new PacketHandler(packetList.get(N++)));
                        }

                        BASE++;
                        continue;
                    }

                    //Iterates over the SR buffer and works out whether the resend the packets
                    for (int i = BASE % (N - BASE); i < (N - BASE); i++) {
                        PacketHandler current = srBuffer.get(i);
                        elapsed = (new Date().getTime() - start);

                        //If packets has not been sent, or its ack time has elapsed - resend. Otherwise if it has been acknowledged
                        //do nothing
                        if ((current.getTime() == -1 || elapsed - current.getTime() > ACK_TIMEOUT) && !current.isAcked()) {
                            srBuffer.get(i).setTime(new Date().getTime() - start);
                            serverSocket.send(srBuffer.get(i).getPacket());
                        }
                    }

                    //Receives ack packet
                    serverSocket.receive(receivedPacket);

                    //If the sequence number of the ack packet matches one of the packets in the buffer, mark as acknowledged
                    for (PacketHandler packetHandler: srBuffer) {
                        if (handler.getSequenceNumber(packetHandler.getPacket()) == handler.getSequenceNumber(receivedPacket)) {
                            packetHandler.setAcked(true);
                            ACK_TIMEOUT = 1.02 * (new Date().getTime() - packetHandler.getTime() - start);
                            //System.out.println(ACK_TIMEOUT);
                            break;
                        }
                    }

                }

                System.out.println("TRANSMISSION FINISHED");
            }
        }
    }

    /**
     * Uses the average delay of packet acknowledgements to determine the optimal SR buffer size
     * @param packets - packets to send to user
     * @param n - number of packets to send to determine optimal buffer size
     * @return
     */
    public int calcBufferSize(ArrayList<DatagramPacket> packets, DatagramPacket received, int n, long start) {
        ArrayList<PacketHandler> meta_data = new ArrayList<>();
        PacketHandler packetHandler;
        long delay = 0;
        long averageDelay = 0;

        for (int i = 0; i < n; i++) {
            packetHandler = new PacketHandler(packets.get(i));
            packetHandler.setTime(new Date().getTime() - start);
            meta_data.add(packetHandler);

            try {
                serverSocket.send(packetHandler.getPacket());
                serverSocket.receive(received);
                delay = (new Date().getTime() - packetHandler.getTime() - start);
                averageDelay = averageDelay + delay;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        averageDelay /= n;

        return (int) (averageDelay * BUFFER_DELAY_FACTOR);

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
