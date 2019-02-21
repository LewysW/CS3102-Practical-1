import javax.sound.sampled.*;
import javax.xml.crypto.Data;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;

public class Client {
    //TODO - localise some variables
    private static final int TIMEOUT = 3000;
    private static final int BUFFERED_PACKET_NUM = 10000;
    private DatagramSocket clientSocket;
    private InetAddress ip;
    private int port;
    private String fileName = null;
    private FileHandler handler;
    private AudioManager audioManager;
    private boolean isTransmitted = false;
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
    public List<DatagramPacket> streamFile(DatagramPacket received) throws Exception {
        PriorityQueue<DatagramPacket> packetQueue = new PriorityQueue<>(63556, new SequenceNumberComparator());
        List<DatagramPacket> packetList = Collections.synchronizedList(new ArrayList<>());
        ArrayList<Integer> sequenceNumbers = new ArrayList<>();
        ByteArrayInputStream byteArrayInputStream = null;

        audioManager.start();
        Thread thread = new Thread(new Player(packetList));
        thread.start();
        int BASE = 0;

        try {
            while (true) {
                //System.out.println(packetQueue.size());
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

                //If packet received is the first packet, set AudioInputStream
                if (BASE == 0) {
                    audioManager.setAudioInputStream(new AudioInputStream(byteArrayInputStream, audioManager.getFormat(), received.getLength()));
                }
            }
        } catch (SocketTimeoutException e) {
            System.out.println("TRANMISSION FINISHED");

            while (!packetQueue.isEmpty()) {
                packetList.add(packetQueue.remove());
            }
            isTransmitted = true;
        }

        thread.join();
        audioManager.end();
        return packetList;
    }

    class Player implements Runnable {
        List<DatagramPacket> packets;
        int currentPacket = 0;
        boolean buffered = false;

        public Player(List<DatagramPacket> packets) {
            this.packets = packets;
        }

        @Override
        public void run() {
            while (true) {
                try {
                    //If the packet buffer is full
                    System.out.println("packets.size(): " + packets.size() + " Current Packet: " + currentPacket);
                    if (packets.size() > currentPacket && (packets.size() > BUFFERED_PACKET_NUM || buffered)) {
                        System.out.println("PLAYING PACKET: " + currentPacket);
                        audioManager.playSound(handler.getPayload(packets.get(currentPacket++)));
                        buffered = true;
                    } else if (isTransmitted && packets.size() == currentPacket) {
                        System.exit(0);
                    }
                } catch (IndexOutOfBoundsException e) {
                    System.out.println("No packets left to play!");
                }
            }
        }
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
