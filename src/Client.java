import javax.sound.sampled.*;
import javax.xml.crypto.Data;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.PriorityQueue;

public class Client {
    private static final int TIMEOUT = 1000;
    private BufferedReader stdIn;
    private DatagramSocket clientSocket;
    private InetAddress ip;
    private int port;
    private String fileName = null;
    private PriorityQueue<DatagramPacket> packetQueue;
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

        packetQueue = new PriorityQueue<>(63556, new SequenceNumberComparator());
        handler = new FileHandler();
        audioManager = new AudioManager();
    }

    private Client(String ip, int port, String fileName) throws Exception {
        this(ip, port);
        this.fileName = fileName;
    }

    private void run() throws Exception {
        byte[] receiveData = new byte[handler.PACKET_SIZE];
        byte[] sendData = ("MARCO").getBytes();
        ArrayList<DatagramPacket> packetList;

        DatagramPacket receivedPacket = new DatagramPacket(receiveData, receiveData.length);
        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, ip, port);
        clientSocket.send(sendPacket);

        while (!(new String(receivedPacket.getData())).startsWith("POLO")) {
            try {
                clientSocket.receive(receivedPacket);
            } catch (SocketTimeoutException e) {
                clientSocket.send(sendPacket);
            }
        }

        //If filename argument has been provided, write stream audio to file.
        if (fileName != null) {
            packetList = streamFile(sendPacket, receivedPacket);
            handler.write("client.wav", handler.toByteArray(packetQueue));
        }

        System.out.println(handler.toByteArray(packetQueue).length);
        System.out.println(packetQueue.size());

        //TODO - enable writing to file if argument is provided (may have to preserve data from PriorityQueue)

        clientSocket.close();
    }

    /**
     * Requests that the server begin streaming the audio file
     * @return - list of packets that make up file
     * @throws IOException
     */
    public ArrayList<DatagramPacket> streamFile(DatagramPacket sent, DatagramPacket received) throws Exception {
        PriorityQueue<DatagramPacket> packetQueue = new PriorityQueue<>(63556, new SequenceNumberComparator());
        ArrayList<DatagramPacket> packetList = new ArrayList<>();
        audioManager.start();

        while (true) {
            try {
                //Receives modified data from server and displays it
                clientSocket.receive(received);
                ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(received.getData());
                packetQueue.add(new DatagramPacket(received.getData().clone(), received.getLength()));

                packetList.add(packetQueue.peek());
                audioManager.setAudioInputStream(new AudioInputStream(byteArrayInputStream, audioManager.getFormat(), received.getLength()));
                audioManager.playSound(handler.getPayload(packetQueue.remove()));
            } catch (SocketTimeoutException e) {
                e.printStackTrace();
                break;
            }
        }

        audioManager.end();

        return packetList;
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
