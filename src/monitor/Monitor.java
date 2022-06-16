package monitor;

import shared.Message;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class Monitor {
    private ServerSocket serverSocket;
    private Socket clientSocket;
    private final ClusterStatusTableModel clusterStatusTableModel;
    private final RequestStatusTableModel requestStatusTableModel;

    private int port;

    public Monitor(int port) {
        this.port = port;
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
                var output = new ObjectOutputStream(client.getOutputStream());
                var input = new ObjectInputStream(client.getInputStream());
                var message = (Message) input.readObject();
                switch (message.getCode()) {
                    case RegisterServer:
                        clusterStatusTableModel.addRow(new Object[]{"Server", message.getServerId(),
                                client.getInetAddress().getHostAddress(), client.getPort(), "UP", 0});
                        System.out.printf("Server with ID %d on %s:%d registered on monitor\n",
                                message.getServerId(), client.getInetAddress().getHostAddress(), client.getPort());
                        break;
                    case RegisterLoadBalancer:
                        String type;
                        if (clusterStatusTableModel.activeLoadBalancerExists()) {
                            type = "Secondary LB";
                        } else {
                            type = "Primary LB";
                        }
                        clusterStatusTableModel.addRow(new Object[]{type, message.getServerId(),
                                client.getInetAddress().getHostAddress(), client.getPort(), "UP", "-"});
                        System.out.printf("Load balancer with ID %d on %s:%d registered on monitor\n",
                                message.getServerId(), client.getInetAddress().getHostAddress(), client.getPort());
                        break;
                }
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    public ClusterStatusTableModel getClusterStatusTableModel() {
        return clusterStatusTableModel;
    }

    public RequestStatusTableModel getRequestStatusTableModel() {
        return requestStatusTableModel;
    }
}
