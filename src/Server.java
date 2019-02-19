import javax.xml.crypto.Data;
import java.net.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.Timer;

public class Server {
    private DatagramSocket serverSocket;
    private FileHandler handler;
    private ArrayList<DatagramPacket> packetList = new ArrayList<>();
    private byte[] fileData;
    //Timeout is 100ms
    private static final int TIMEOUT = 100;


    /**
     Used the sample client-server UDP code from Computer Networking: A Top Down Approach, by Kurose and Ross
     */

    private Server(int port, String fileName) throws Exception {
        serverSocket = new DatagramSocket(port);
        handler = new FileHandler();
        fileData = handler.read(fileName);
    }

    private void run() throws Exception {
        //Creates server socket, client socket, and output and input objects
        byte[] incomingData = new byte[handler.PACKET_SIZE];
        byte[] outgoing = new byte[handler.PACKET_SIZE];

        DatagramPacket receivedPacket = new DatagramPacket(incomingData, incomingData.length);
        boolean ack;

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
                int BASE = 0;
                int N = 10;


                for (int i = 0; i < N; i++) srBuffer.add(new PacketHandler(packetList.get(i)));

                long start = System.currentTimeMillis();
                long elapsed;

                //TODO - put into selectiveResend() function
                //Iterates through packets while SR buffer has not reached end of packet list
                while (N < packetList.size()) {
                    //Shifts the boundaries of the SR (Selective Resend) buffer if the first packet has been acknowledged
                    if (srBuffer.get(BASE % (N - BASE)).isAcked()) {
                        srBuffer.add(new PacketHandler(packetList.get(N)));
                        srBuffer.remove(BASE % (N - BASE));
                        BASE++;
                        N++;
                        continue;
                    }

                    //Iterates over the SR buffer and works out whether the resend the packets
                    for (int i = BASE % (N - BASE); i < (N - BASE); i++) {
                        PacketHandler current = srBuffer.get(i);
                        elapsed = (new Date().getTime() - start);

                        //If packets has not been sent, or its ack time has elapsed - resend. Otherwise if it has been acknowledged
                        //do nothing
                        if ((current.getTime() == -1 || elapsed - current.getTime() > TIMEOUT) && !current.isAcked()) {
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
                            break;
                        }
                    }
                }

                //TODO - put into separate stopAndWait() function
                /*
                for (DatagramPacket packet: packetList) {
                    ack = false;

                    while (!ack) {
                        try {
                            serverSocket.send(packet);
                            serverSocket.receive(receivedPacket);

                            if (handler.getSequenceNumber(receivedPacket) == handler.getSequenceNumber(packet)) {
                                ack = true;
                            }
                        } catch (SocketTimeoutException e) {
                            System.out.println("Resending " + handler.getSequenceNumber(packet));
                        }

                    }
                }
                */
            }
        }
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
