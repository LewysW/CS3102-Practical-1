import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.ArrayList;

public class Client {
    //TODO - validate input size
    private static final int TIMEOUT = 5000;
    private BufferedReader stdIn;
    private DatagramSocket clientSocket;
    private InetAddress ip;
    private int port;
    private String fileName;
    private ArrayList<DatagramPacket> packets;
    private FileHandler handler;
    private AudioManager audioManager;
    /**
     Used the sample client-server UDP code from Computer Networking: A Top Down Approach, by Kurose and Ross
     */

    private Client(String ip, int port) throws Exception {
        //Initialises input stream for user input
        stdIn = new BufferedReader(new InputStreamReader(System.in));

        //Creates UDP socket for client-server communication
        clientSocket = new DatagramSocket();
        clientSocket.setSoTimeout(TIMEOUT);

        //Gets localhost as ip address
        this.ip = InetAddress.getByName(ip);
        this.port = port;

        packets = new ArrayList<>();
        handler = new FileHandler();
        audioManager = new AudioManager();
    }

    private Client(String ip, int port, String fileName) throws Exception {
        this(ip, port);
        this.fileName = fileName;
    }

    private void run() throws Exception {
        //Arrays for sending and receiving data
        byte[] sendData = new byte[handler.PACKET_SIZE];
        byte[] receiveData = new byte[handler.PACKET_SIZE];


        String message;
        while ((message = stdIn.readLine()) != null) {
            //Reads in message from user and sends it to server
            sendData = message.getBytes();
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, ip, port);
            clientSocket.send(sendPacket);

            while (true) {
                try {
                    //Receives modified data from server and displays it
                    DatagramPacket receivedPacket = new DatagramPacket(receiveData, receiveData.length);
                    clientSocket.receive(receivedPacket);
                    System.out.println("Sequence Number: " + handler.getSequenceNumber(receivedPacket) + " Timestamp: " + handler.getTimeStamp(receivedPacket));
                    packets.add(receivedPacket);
                } catch (SocketTimeoutException e) {
                    e.printStackTrace();
                    break;
                }
            }

            System.out.println(handler.toByteArray(packets).length);
            System.out.println(packets.size());
            handler.write("client.wav", handler.toByteArray(packets));
        }

        clientSocket.close();
    }

    public static void main(String[] args) {
        if (args.length == 2 || args.length == 3) {
            Client client;

            try {
                if (args.length == 2) {
                    client = new Client(args[0], Integer.parseInt(args[1]));
                } else {
                    client = new Client(args[0], Integer.parseInt(args[1]), args[2]);
                }

                client.run();
            } catch(Exception e) {
                e.printStackTrace();
            }

        } else {
            System.out.println("Usage: 'java Client <host> <port> <optional filename>");
        }
    }
}
