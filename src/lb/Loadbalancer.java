package lb;

import shared.RequestMessage;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;

public class Loadbalancer {
    private ServerSocket server;

    public Loadbalancer(int port) {
        try {
            server = new ServerSocket(port);
        } catch (IOException e) {
            System.err.format("Failed to initialize load balancer server on port %d\n", port);
            e.printStackTrace();
            System.exit(-1);
        }
    }

    public void listen() {
        System.out.println("Loadbalancer listen on port 8001");

        while (true) {
            try {
                var client = server.accept();
                System.out.printf("Incoming request from %s:%d\n", client.getInetAddress().getHostAddress(), client.getPort());
                var output = new ObjectOutputStream(client.getOutputStream());
                var input = new ObjectInputStream(client.getInputStream());
                var request = (RequestMessage) input.readObject();

            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        var lb = new Loadbalancer(8000);
        lb.listen();
    }
}
