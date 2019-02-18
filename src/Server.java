import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.PriorityQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;

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

        //Loops and listens for incoming connections
        while (true) {
            //Constructs a datagram packet of a given length and uses this packet to receive data
            DatagramPacket receivedPacket = new DatagramPacket(incomingData, incomingData.length);
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

                Thread listenerThread = new Thread(new Listener());

                for (DatagramPacket packet: packetList) {
                    TimeUnit.MICROSECONDS.sleep(1);
                    serverSocket.send(packet);

                    if (!listenerThread.isAlive()) {
                        listenerThread.start();
                    }
                }
            }
        }
    }

    class Listener implements Runnable {
        @Override
        public void run() {
            byte[] incomingData = new byte[handler.PACKET_SIZE];
            DatagramPacket packet = new DatagramPacket(incomingData, incomingData.length);
            int sequenceNumber = -1;

            while (!serverSocket.isClosed()) {
                try {
                    serverSocket.receive(packet);
                    System.out.println("HAHA!");
                    if (handler.getSequenceNumber(packet) != sequenceNumber) {
                        sequenceNumber = handler.getSequenceNumber(packet);
                        serverSocket.send(packetList.get(sequenceNumber));
                    }

                } catch (IOException e) {
                    e.printStackTrace();
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
