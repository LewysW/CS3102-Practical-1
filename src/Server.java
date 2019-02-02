import java.io.IOException;
import java.net.*;

public class Server {
    private static final int PORT = 30751;
    private static final int DATA_SIZE = 1024;

    /**
     Used the sample client-server UDP code from Computer Networking: A Top Down Approach, by Kurose and Ross
     */

    public static void main(String[] args) {
        try {
            //Creates server socket, client socket, and output and input objects
            DatagramSocket serverSocket = new DatagramSocket(PORT);
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
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
