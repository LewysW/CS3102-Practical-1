import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class Client {
    //TODO - validate input size
    private static final int DATA_SIZE = 1024;
    private static final int PORT = 30751;
    /**
     Used the sample client-server UDP code from Computer Networking: A Top Down Approach, by Kurose and Ross
     */

    public static void main(String[] args) {
        try {
            BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));
            DatagramSocket clientSocket = new DatagramSocket();
            InetAddress ip = InetAddress.getLocalHost();

            byte[] sendData = new byte[DATA_SIZE];
            byte[] receiveData = new byte[DATA_SIZE];

            String message;
            while ((message = stdIn.readLine()) != null) {
                sendData = message.getBytes();
                DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, ip, PORT);
                clientSocket.send(sendPacket);

                DatagramPacket receivedPacket = new DatagramPacket(receiveData, receiveData.length);
                clientSocket.receive(receivedPacket);
                String serverMessage = new String(receivedPacket.getData());
                System.out.println("FROM SERVER: " + serverMessage);
            }

            clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
