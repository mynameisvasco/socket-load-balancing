package monitor;

import shared.Message;
import shared.ServerState;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;
import java.util.List;

public class Monitor {
    private ServerSocket serverSocket;
    private Socket clientSocket;
    private final List<ServerState> serverStates;
    private final ClusterStatusTableModel clusterStatusTableModel;
    private final RequestStatusTableModel requestStatusTableModel;

    private int port;

    public Monitor(int port) {
        this.port = port;
        this.serverStates = new LinkedList<>();
        clusterStatusTableModel = new ClusterStatusTableModel();
        requestStatusTableModel = new RequestStatusTableModel();
        try {
            serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            System.err.format("Failed to initialize monitor server on port %d\n", port);
            e.printStackTrace();
            System.exit(-1);
        }
    }

    public void listen() {
        System.out.printf("Monitor listening on port %d\n", port);

        while (true) {
            try {
                var client = serverSocket.accept();
                var thread = new Thread(() -> handleClient(client));
                thread.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void handleClient(Socket client) {
        try {
            var output = new ObjectOutputStream(client.getOutputStream());
            var input = new ObjectInputStream(client.getInputStream());
            var message = (Message) input.readObject();

            switch (message.getCode()) {
                case RegisterServer -> {
                    clusterStatusTableModel.addServer(message, client);
                    System.out.printf("Server with ID %d on %s:%d registered on monitor\n", message.getServerId(), client.getInetAddress().getHostAddress(), client.getPort());
                }
                case RegisterLoadBalancer -> {
                    clusterStatusTableModel.addLoadbalancer(message, client);
                    System.out.printf("Load balancer with ID %d on %s:%d registered on monitor\n", message.getServerId(), client.getInetAddress().getHostAddress(), client.getPort());
                }
                case RegisterRequest -> {
                    requestStatusTableModel.addRequest(message, client);
                    output.writeObject(serverStates);
                }
                case UpdateRequest -> {
                    requestStatusTableModel.updateRequest(message.getRequestId(), message.getStatus());
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }


    public ClusterStatusTableModel getClusterStatusTableModel() {
        return clusterStatusTableModel;
    }

    public RequestStatusTableModel getRequestStatusTableModel() {
        return requestStatusTableModel;
    }
}
