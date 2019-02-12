import java.net.*;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class Server {
    private DatagramSocket serverSocket;
    private FileHandler handler;
    private ArrayList<DatagramPacket> packets;
    private byte[] fileData;
    private AudioManager audioManager;


    /**
     Used the sample client-server UDP code from Computer Networking: A Top Down Approach, by Kurose and Ross
     */

    private Server(int port, String fileName) throws Exception {
        serverSocket = new DatagramSocket(port);
        handler = new FileHandler();
        fileData = handler.read(fileName);
        audioManager = new AudioManager();
        audioManager.playSound(handler.toByteArray(handler.toPacketList(fileData, InetAddress.getLocalHost(), port)));
    }

    private void run() throws Exception {
        //Creates server socket, client socket, and output and input objects
        byte[] incomingData = new byte[handler.PACKET_SIZE];

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

            if (message.startsWith("GET")) {
                packets = handler.toPacketList(fileData, ip, port);
                System.out.println(packets.size());

                for (DatagramPacket packet : packets) {
                    TimeUnit.MILLISECONDS.sleep(1);
                    serverSocket.send(packet);
                }
            }

            System.out.println(handler.toByteArray(packets).length);
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
