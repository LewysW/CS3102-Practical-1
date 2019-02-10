import javax.xml.crypto.Data;
import java.io.IOException;
import java.net.*;
import java.util.ArrayList;

public class Server {
    private static final int DATA_SIZE = 1024;
    private DatagramSocket serverSocket;
    private FileHandler handler;
    private ArrayList<DatagramPacket> packets;
    private String fileName;


    /**
     Used the sample client-server UDP code from Computer Networking: A Top Down Approach, by Kurose and Ross
     */

    private Server(int port, String fileName) throws Exception {
        serverSocket = new DatagramSocket(port);
        handler = new FileHandler();
        this.fileName = fileName;


        handler.write("test.wav", handler.toByteArray(handler.toPacketList(handler.read(fileName), InetAddress.getLocalHost(), port)));
    }

    private void run() throws IOException {
        //Creates server socket, client socket, and output and input objects
        byte[] incomingData = new byte[DATA_SIZE];
        byte[] outgoingData;

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
            String modified = message.toUpperCase();
            outgoingData = modified.getBytes();

            //Constructs a new packet to send and sends it to ip address and port
            DatagramPacket sendPacket = new DatagramPacket(outgoingData, outgoingData.length, ip, port);
            serverSocket.send(sendPacket);
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
