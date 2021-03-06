package monitor;

import shared.*;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ConnectException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * Entity responsible for monitoring all load balancers and servers using heartbeat system, it should be launched
 * before the servers' and load balancers' processes so that they are registered and the heartbeat begins
 */
public class Monitor {
    private ServerSocket serverSocket;
    private final Lock serverStatesLock;
    private final List<ServerState> serverStates;
    private final ClusterStatusTableModel clusterStatusTableModel;
    private final RequestStatusTableModel requestStatusTableModel;
    private final Map<Integer, Message> pendingMessages;
    private final int port;
    private final int nrHeartBeatTries;
    private final int heartBeatInterval;  // in miliseconds

    /**
     * Creates a new Monitor
     * @param port Port used by the monitor server
     */
    public Monitor(int port, int heartBeatInterval, int nrHeartBeatTries) {
        this.port = port;
        this.nrHeartBeatTries = nrHeartBeatTries;
        this.heartBeatInterval = heartBeatInterval;
        this.pendingMessages = new HashMap<>();
        this.serverStatesLock = new ReentrantLock();
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

    /**
     * Starts listening for incoming requests
     */
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

    /**
     * Handles a specific connection of a client
     */
    private void handleClient(Socket client) {
        try {
            var output = new ObjectOutputStream(client.getOutputStream());
            var input = new ObjectInputStream(client.getInputStream());
            var message = (Message) input.readObject();

            switch (message.getCode()) {
                case RegisterServer -> {
                    serverStatesLock.lock();
                    serverStates.add(new ServerState(message.getSocketInfo(), message.getServerId()));
                    clusterStatusTableModel.addServer(message);
                    serverStatesLock.unlock();
                    System.out.printf("Server with ID %d on %s registered on monitor\n", message.getServerId(), message.getSocketInfo());
                    heartBeat(message.getServerId(), message.getSocketInfo(), message.getCode());
                }
                case RegisterLoadBalancer -> {
                    serverStatesLock.lock();
                    clusterStatusTableModel.addLoadbalancer(message);
                    System.out.printf("Load balancer with ID %d on %s:%d registered on monitor\n",
                            message.getServerId(), client.getInetAddress().getHostAddress(),
                            message.getSocketInfo().port());
                    serverStatesLock.unlock();
                    heartBeat(message.getServerId(), message.getSocketInfo(), message.getCode());
                }
                case RegisterRequest -> {
                    serverStatesLock.lock();
                    requestStatusTableModel.addRequest(message, client);
                    serverStatesLock.unlock();
                    output.writeObject(serverStates);
                }
                case UpdateRequest -> {
                    serverStatesLock.lock();

                    switch (message.getStatus()) {
                        case "Processing" -> pendingMessages.put(message.getRequestId(), message);
                        case "Completed", "Rejected" -> pendingMessages.remove(message.getRequestId());
                    }

                    requestStatusTableModel.updateRequest(message.getRequestId(), message.getStatus());
                    serverStatesLock.unlock();
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

    /**
     * Sends heartbeat request to specified loadbalancer or server
     */
    private void heartBeat(int id, SocketInfo socketInfo, MessageCodes originalMessageCode) {

        int nrHeartBeatsFailed = 0;

        while (nrHeartBeatsFailed < nrHeartBeatTries) {
            try {
                Thread.sleep(heartBeatInterval);
                var server = clusterStatusTableModel.getInfoById(id).createSocket();
//                System.out.println(server.getPort());
                var output = new ObjectOutputStream(server.getOutputStream());
                var input = new ObjectInputStream(server.getInputStream());
                var heartBeatMessage = new Message(0, 0, 0, MessageCodes.HeartBeat, 0, 0, 0, null, "");
                output.writeObject(heartBeatMessage);
                output.flush();

                if (originalMessageCode == MessageCodes.RegisterServer) {
                    var message = (Message) input.readObject();
                    clusterStatusTableModel.setNumberOfIterations(message);
                    serverStates.forEach(s -> {
                        if (s.getServerId() == message.getServerId()) {
                            s.setTotalNumberOfIterations(message.getNumberOfIterations());
                        }
                    });
                }
                server.close();
            } catch (SocketException e) {
                nrHeartBeatsFailed++;
            } catch (IOException | InterruptedException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }

        if (originalMessageCode == MessageCodes.RegisterLoadBalancer) {
            handleLoadBalancerCrash(id, socketInfo);
        } else if (originalMessageCode == MessageCodes.RegisterServer) {
            handleServerCrash(id, socketInfo);
        }
    }

    /**
     * Handles load balancer crash by promoting a new primary load balancer if necessary
     * @param id Id of the crashed load balancer
     * @param socketInfo Socket info of the crashed load balancer
     */
    private void handleLoadBalancerCrash(int id, SocketInfo socketInfo) {
        var crashedLoadBalancerInfo = clusterStatusTableModel.markLoadBalancerDown(id);
        System.out.printf("Load balancer with ID %d at %s:%d failed to provide a heart beat too many times and was marked as down\n", id, socketInfo.address(), socketInfo.port());

        if (!clusterStatusTableModel.activeLoadBalancerExists()) {
            var loadBalancerToPromoteInfo = clusterStatusTableModel.markLoadBalancerPromotion(crashedLoadBalancerInfo);

            try {
                var loadBalancerToPromote = loadBalancerToPromoteInfo.createSocket();
                var output = new ObjectOutputStream(loadBalancerToPromote.getOutputStream());
                var input = new ObjectInputStream(loadBalancerToPromote.getInputStream());
                var promoteMessage = new Message(0, 0, id, MessageCodes.PromoteLoadBalancer,
                        0, 0, 0,
                        new SocketInfo("localhost", crashedLoadBalancerInfo.port()), "");
                output.writeObject(promoteMessage);
                output.flush();
                System.out.printf("Promoted load balancer at %s:%d\n", loadBalancerToPromoteInfo.address(),
                        loadBalancerToPromoteInfo.port());
                loadBalancerToPromote.close();
            } catch (IOException e) {
                System.err.printf("Failed promotion to primary for load balancer at %s:%d\n",
                        loadBalancerToPromoteInfo.address(), loadBalancerToPromoteInfo.port());
                e.printStackTrace();
            }
        }
    }

    /**
     * Handles server crash by redirecting requests to the loadbalancer again
     * @param id Id of the crashed server
     * @param socketInfo Socket info of the crashed server
     */
    private void handleServerCrash(int id, SocketInfo socketInfo) {
        clusterStatusTableModel.downServer(id);
        var messages = pendingMessages.values()
                .stream()
                .filter(m -> m.getServerId() == id)
                .toList();


        for (var message : messages) {
            var lb = clusterStatusTableModel.getPrimaryLoadBalancer();
            message.setCode(MessageCodes.PiCalculationRequest);

            try {
                var loadbalancer = lb.createSocket();
                var output = new ObjectOutputStream(loadbalancer.getOutputStream());
                output.writeObject(message);
                output.flush();
                output.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
