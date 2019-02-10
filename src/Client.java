import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class Client {
    //TODO - validate input size
    private static final int DATA_SIZE = 1024;
    private BufferedReader stdIn;
    private DatagramSocket clientSocket;
    private InetAddress ip;
    private int port;
    private String fileName;
    /**
     Used the sample client-server UDP code from Computer Networking: A Top Down Approach, by Kurose and Ross
     */

    private Client(String ip, int port) throws Exception {
        //Initialises input stream for user input
        stdIn = new BufferedReader(new InputStreamReader(System.in));

        //Creates UDP socket for client-server communication
        clientSocket = new DatagramSocket();

        //Gets localhost as ip address
        this.ip = InetAddress.getByName(ip);
        this.port = port;
    }

    private Client(String ip, int port, String fileName) throws Exception {
        this(ip, port);
        this.fileName = fileName;
    }

    private void run() throws IOException {
        //Arrays for sending and receiving data
        byte[] sendData = new byte[DATA_SIZE];
        byte[] receiveData = new byte[DATA_SIZE];


        String message;
        while ((message = stdIn.readLine()) != null) {
            //Reads in message from user and sends it to server
            sendData = message.getBytes();
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, ip, port);
            clientSocket.send(sendPacket);

            //Receives modified data from server and displays it
            DatagramPacket receivedPacket = new DatagramPacket(receiveData, receiveData.length);
            clientSocket.receive(receivedPacket);
            String serverMessage = new String(receivedPacket.getData());
            System.out.println("FROM SERVER: " + serverMessage);
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
