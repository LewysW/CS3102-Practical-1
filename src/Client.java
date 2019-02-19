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
import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;

public class Client {
    //TODO - localise some variables
    private static final int TIMEOUT = 1000;
    private static final int BUFFERED_PACKET_NUM = 5000;
    private DatagramSocket clientSocket;
    private InetAddress ip;
    private int port;
    private String fileName = null;
    private FileHandler handler;
    private AudioManager audioManager;
    /**
     Used the sample client-server UDP code from Computer Networking: A Top Down Approach, by Kurose and Ross
     */

    private Client(String ip, int port) throws Exception {
        //Creates UDP socket for client-server communication
        clientSocket = new DatagramSocket();
        clientSocket.setSoTimeout(TIMEOUT);

        //Gets localhost as ip address
        this.ip = InetAddress.getByName(ip);
        this.port = port;

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

        //Sends the initial stage of the 'handshake' 'MARCO'
        DatagramPacket receivedPacket = new DatagramPacket(receiveData, receiveData.length);
        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, ip, port);
        clientSocket.send(sendPacket);

        //Waits for server's response containing 'POLO'
        while (!(new String(receivedPacket.getData())).startsWith("POLO")) {
            try {
                clientSocket.receive(receivedPacket);
            } catch (SocketTimeoutException e) {
                clientSocket.send(sendPacket);
            }
        }

        packetList = streamFile(receivedPacket);

        //If filename argument has been provided, write stream audio to file.
        if (fileName != null) {
            handler.write("client.wav", handler.toByteArray(packetList));
        }

        System.out.println(handler.toByteArray(packetList).length);
        System.out.println(packetList.size());

        clientSocket.close();
    }

    /**
     * Requests that the server begin streaming the audio file
     * @return - list of packets that make up file
     * @throws IOException
     */
    public ArrayList<DatagramPacket> streamFile(DatagramPacket received) throws Exception {
        PriorityQueue<DatagramPacket> packetQueue = new PriorityQueue<>(63556, new SequenceNumberComparator());
        ArrayList<DatagramPacket> packetList = new ArrayList<>();
        ArrayList<Integer> seqNums = new ArrayList<>();
        boolean buffered = false;
        ByteArrayInputStream byteArrayInputStream = null;

        audioManager.start();

        while (true) {
            try {
                //Receives modified data from server and displays it
                clientSocket.receive(received);
                System.out.println("RECEIVED: " + handler.getSequenceNumber(received));
                clientSocket.send(new DatagramPacket(handler.intToBytes(handler.getSequenceNumber(received)), handler.SEQUENCE_SIZE, ip, port));
                byteArrayInputStream = new ByteArrayInputStream(received.getData());

                //If the packet has not already been delivered (e.g. a retransmitted packet) then add packet to queue
                if (!seqNums.contains(handler.getSequenceNumber(received))) {
                    packetQueue.add(new DatagramPacket(received.getData().clone(), received.getLength()));
                    seqNums.add(handler.getSequenceNumber(received));
                }

                //Initially fills up packet queue with packets to allow for some form of buffering
                if (packetQueue.size() > BUFFERED_PACKET_NUM || buffered) {
                    packetList.add(packetQueue.peek());
                    audioManager.setAudioInputStream(new AudioInputStream(byteArrayInputStream, audioManager.getFormat(), received.getLength()));
                    audioManager.playSound(handler.getPayload(packetQueue.remove()));
                    buffered = true;
                }
            } catch (SocketTimeoutException e) {
                //If server has finished receiving packets, play out remaining packets if they exist
                if (packetQueue.size() > 0) {
                    while (!packetQueue.isEmpty()) {
                        packetList.add(packetQueue.peek());
                        audioManager.setAudioInputStream(new AudioInputStream(byteArrayInputStream, audioManager.getFormat(), received.getLength()));
                        audioManager.playSound(handler.getPayload(packetQueue.remove()));
                    }
                } else {
                    e.printStackTrace();
                }

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
