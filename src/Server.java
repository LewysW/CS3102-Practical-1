import java.net.*;
import java.util.ArrayList;

public class Server {
    private DatagramSocket serverSocket;
    private FileHandler handler;
    private ArrayList<DatagramPacket> packetList = new ArrayList<>();
    private byte[] fileData;


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
