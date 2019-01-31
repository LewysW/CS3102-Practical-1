import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {
    private static final int PORT = 30751;

    public static void main(String[] args) {
        try {
            //Creates server socket, client socket, and output and input objects
            ServerSocket serverSocket = new ServerSocket(PORT);
            Socket clientSocket;
            PrintWriter out;
            BufferedReader in;

            //Loops and listens for incoming connections
            while (true) {
                //Accepts an incoming connection
                clientSocket = serverSocket.accept();

                //Sets the input and output streams to the connected client socket
                out = new PrintWriter(clientSocket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

                //Reads input from the client and echos the data as a string
                String s;
                while ((s = in.readLine()) != null) {
                    out.println(s);
                    System.out.println("Received: " + s);
                }

                //closes all streams
                out.close();
                in.close();
                clientSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
