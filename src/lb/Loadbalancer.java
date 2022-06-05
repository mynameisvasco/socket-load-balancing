package lb;

import shared.RequestMessage;
import shared.ResponseMessage;
import shared.SocketInfo;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;

public class Loadbalancer {
    private ServerSocket loadbalancer;
    private final int port;
    private final List<SocketInfo> serverInfos;

    public Loadbalancer(int port, List<SocketInfo> serverInfos) {
        this.port = port;
        this.serverInfos = serverInfos;

        try {
            loadbalancer = new ServerSocket(port);
        } catch (IOException e) {
            System.err.format("Failed to initialize load balancer server on port %d\n", port);
            e.printStackTrace();
            System.exit(-1);
        }
    }

    public void listen() {
        System.out.printf("Loadbalancer listen on port %d\n", port);

        while (true) {
            try {
                var client = loadbalancer.accept();
                System.out.printf("Incoming request from %s:%d\n", client.getInetAddress().getHostAddress(), client.getPort());
                var clientOutput = new ObjectOutputStream(client.getOutputStream());
                var clientInput = new ObjectInputStream(client.getInputStream());
                var request = (RequestMessage) clientInput.readObject();
                var thread = new Thread((() -> requestHandler(client, clientOutput, request)));
                thread.start();

            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    private Socket getLessLoadServer() {
        return serverInfos.get(0).createSocket();
    }

    private void requestHandler(Socket client, ObjectOutputStream clientOutput, RequestMessage request) {
        try {
            var server = getLessLoadServer();
            System.out.printf("Redirecting request to %s:%d\n", server.getInetAddress().getHostAddress(), server.getPort());
            var serverOutput = new ObjectOutputStream(server.getOutputStream());
            var serverInput = new ObjectInputStream(server.getInputStream());
            serverOutput.writeObject(request);
            var response = (ResponseMessage) serverInput.readObject();
            System.out.printf("Redirecting response to %s:%d\n", client.getInetAddress().getHostAddress(), client.getPort());
            clientOutput.writeObject(response);
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        var lb = new Loadbalancer(8000, List.of(new SocketInfo(1, "localhost", 9000)));
        lb.listen();
    }
}
