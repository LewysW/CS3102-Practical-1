import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.*;

public class Client {
    //Time until client times out after waiting for server
    private static final int TIMEOUT = 10000;
    //Number of packets to buffer before playing audio
    private static final int BUFFERED_PACKET_NUM = 1000;
    //Initial size of priority queue
    private static final int INITIAL_QUEUE_SIZE = 65536;
    //Interface to receive packets through
    private DatagramSocket clientSocket;
    //IP address
    private InetAddress ip;
    //port number
    private int port;
    //Name of file
    private String fileName = null;
    //Handler for conversion from file to bytes
    private FileHandler handler;
    //Audio interface class
    private AudioManager audioManager;
    //Whether file has been transmitted
    private boolean isTransmitted = false;


    /**
     Used the sample client-server UDP code from Computer Networking: A Top Down Approach, by Kurose and Ross
     */


    /**
     * Constructor for client
     * @param ip - host of server
     * @param port - port server application is running on
     * @throws Exception
     */
    private Client(String ip, int port) throws Exception {
        //Creates UDP socket for client-server communication
        clientSocket = new DatagramSocket();
        clientSocket.setSoTimeout(TIMEOUT);

        //Gets InetAddress from string ip
        this.ip = InetAddress.getByName(ip);
        this.port = port;

        //Create file handler and audio manager instances
        handler = new FileHandler();
        audioManager = new AudioManager();
    }

    /**
     * Initialises file name if third argument is specified
     * @param ip - host of server
     * @param port - port of server application
     * @param fileName - name of file to write data to
     * @throws Exception
     */
    private Client(String ip, int port, String fileName) throws Exception {
        this(ip, port);
        this.fileName = fileName;
    }

    /**
     * Runs the client which requests a file from the server,
     * streams and plays it,
     * and writes it to file
     * @throws Exception
     */
    private void run() throws Exception {
        byte[] receiveData = new byte[handler.PACKET_SIZE];
        byte[] sendData = ("MARCO").getBytes();
        List<DatagramPacket> packetList;

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

        //Streams the audio file and plays it, finally storing it in a packet array list
        packetList = streamFile(receivedPacket);

        //If filename argument has been provided, write stream audio to file.
        if (fileName != null) {
            handler.write("client.wav", handler.toByteArray(packetList));
        }

        System.out.println("SIZE OF RECEIVED DATA: " + handler.toByteArray(packetList).length);
        System.out.println("TOTAL NUMBER OF PACKETS RECEIVED: " + packetList.size());

        clientSocket.close();
    }

    /**
     * Simultaneously streams and plays the audio file from the server
     * @return - list of packets that make up file
     * @throws IOException
     */
    public List<DatagramPacket> streamFile(DatagramPacket received) throws Exception {
        //Priority queue to reorder received packets from server
        PriorityQueue<DatagramPacket> packetQueue = new PriorityQueue<>(INITIAL_QUEUE_SIZE, new SequenceNumberComparator());
        //Synchronized packet array list for audio playback
        List<DatagramPacket> packetList = Collections.synchronizedList(new ArrayList<>());
        //List to store the sequence numbers received thus far
        ArrayList<Integer> sequenceNumbers = new ArrayList<>();
        //Input stream for playing audio
        ByteArrayInputStream byteArrayInputStream = null;

        //Starts audio interface and playback thread
        audioManager.start();
        Thread thread = new Thread(new Player(packetList));
        thread.start();

        //Keeps track of the end of the block of contiguously received packets
        int BASE = 0;

        try {
            while (true) {
                //If there are packets in the queue and the packet is the next in sequence, pass it to playback thread
                if (packetQueue.size() > 0 && handler.getSequenceNumber(packetQueue.peek()) == BASE) {
                    packetList.add(packetQueue.remove());
                    BASE++;
                    continue;
                }

                //Receives a packet and sends an ack response to server
                clientSocket.receive(received);
                clientSocket.send(new DatagramPacket(handler.intToBytes(handler.getSequenceNumber(received)), handler.SEQUENCE_SIZE, ip, port));

                //If the packet has not yet been received, add it to the packet priority queue
                if (!sequenceNumbers.contains(handler.getSequenceNumber(received))) {
                    packetQueue.add(new DatagramPacket(received.getData().clone(), received.getLength()));
                    sequenceNumbers.add(handler.getSequenceNumber(received));
                }

                //If packet received is the first packet, use meta data from packet to initialise the audio input stream
                if (BASE == 0) {
                    audioManager.setAudioInputStream(new AudioInputStream(byteArrayInputStream, audioManager.getFormat(), received.getLength()));
                }
            }
        //If sockets times out, assume end of transmission
        } catch (SocketTimeoutException e) {
            System.out.println("TRANSMISSION FINISHED");

            //Add remaining packets from priority queue to list for playback
            while (!packetQueue.isEmpty()) {
                packetList.add(packetQueue.remove());
            }

            /*
            Finally, set isTransmitted to true, to indicate to playback
            thread to end when out of packets to play
             */
            isTransmitted = true;
        }

        //Join thread, end audio interface and return the list of packets
        thread.join();
        audioManager.end();
        return packetList;
    }

    /**
     * Class which runs in a separate thread for audio playback
     */
    class Player implements Runnable {
        /*
        Stores the list of packets, the current packet played, and whether
        enough packets have been buffer to begin playback
         */
        List<DatagramPacket> packets;
        int currentPacket = 0;
        boolean buffered = false;

        //Initialises the list of packets to be played
        public Player(List<DatagramPacket> packets) {
            this.packets = packets;
        }

        /**
         * Function to play audio packet data
         */
        @Override
        public void run() {
            while (true) {
                try {
                    //If there unplayed packets left in the packet list and the initial buffering period has occurred
                    if (packets.size() > currentPacket && (packets.size() > BUFFERED_PACKET_NUM || buffered)) {
                        //Play the packet
                        audioManager.playSound(handler.getPayload(packets.get(currentPacket++)));

                        //Sets buffered to true after the initial buffering period has occurred
                        buffered = true;
                    //If the file has been fully transmitted and the final packet has been played, thread exits
                    } else if (isTransmitted && packets.size() == currentPacket) {
                        break;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void main(String[] args) {
        //Ensures that there are two or three args (host and port, or host, port and file to write to)
        if (args.length == 2 || args.length == 3) {
            Client client;

            try {
                //Calls appropriate constructor based on number of args
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
