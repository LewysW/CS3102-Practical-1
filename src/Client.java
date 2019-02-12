import javax.sound.sampled.*;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class Client {
    private static final int TIMEOUT = 1000;
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
        String message;

        while ((message = stdIn.readLine()) != null) {
            //Reads in message from user and sends it to server

            if (message.startsWith("GET")) {
                packets = streamFile();
            }

            for (int i = 0; i < packets.size(); i++) {
                System.out.println("Sequence Number: " + handler.getSequenceNumber(packets.get(i)) + " Timestamp: " + handler.getTimeStamp(packets.get(i)));
            }

            System.out.println(handler.toByteArray(packets).length);
            System.out.println(packets.size());

            //TODO - only write to file if argument is provided
            handler.write("client.wav", handler.toByteArray(packets));
        }

        clientSocket.close();
    }

    /**
     * Requests that the server begin streaming the audio file
     * @return - list of packets that make up file
     * @throws IOException
     */
    public ArrayList<DatagramPacket> streamFile() throws Exception {
        ArrayList<DatagramPacket> packets = new ArrayList<>();
        String message = "GET";
        byte[] receiveData = new byte[handler.PACKET_SIZE];
        byte[] sendData = message.getBytes();

        DatagramPacket receivedPacket = new DatagramPacket(receiveData, receiveData.length);
        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, ip, port);
        clientSocket.send(sendPacket);
        audioManager.start();

        while (true) {
            try {
                //Receives modified data from server and displays it
                clientSocket.receive(receivedPacket);
                ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(receivedPacket.getData());
                packets.add(new DatagramPacket(receivedPacket.getData().clone(), receivedPacket.getLength()));
                audioManager.setAudioInputStream(new AudioInputStream(byteArrayInputStream, audioManager.getFormat(), receivedPacket.getLength()));
                audioManager.playSound(handler.getPayload(receivedPacket));
            } catch (SocketTimeoutException e) {
                e.printStackTrace();
                break;
            }
        }

        audioManager.end();

        for (DatagramPacket p: packets) {
            System.out.println("Sequence Number: " + handler.getSequenceNumber(p));
        }

        return packets;
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
