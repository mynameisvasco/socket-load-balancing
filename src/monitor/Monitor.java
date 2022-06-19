package monitor;

import shared.Message;
import shared.MessageCodes;
import shared.ServerState;
import shared.SocketInfo;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ConnectException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Monitor {
    private ServerSocket serverSocket;
    private final Lock serverStatesLock;
    private final List<ServerState> serverStates;
    private final ClusterStatusTableModel clusterStatusTableModel;
    private final RequestStatusTableModel requestStatusTableModel;

    // configuration variables
    private int port;
    private int nrHeartBeatTries;
    private int heartBeatInterval;  // in miliseconds

    public Monitor(int port) {
        this.port = port;
        this.nrHeartBeatTries = 1;
        this.heartBeatInterval = 1000;

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
                    serverStatesLock.lock();
                    serverStates.add(new ServerState(client.getInetAddress(), message.getSocketInfo().port(),
                            message.getServerId()));
                    clusterStatusTableModel.addServer(message);
                    serverStatesLock.unlock();
                    System.out.printf("Server with ID %d on %s:%d registered on monitor\n", message.getServerId(),
                            client.getInetAddress().getHostAddress(), message.getSocketInfo().port());
                }
                case RegisterLoadBalancer -> {
                    clusterStatusTableModel.addLoadbalancer(message);
                    System.out.printf("Load balancer with ID %d on %s:%d registered on monitor\n",
                            message.getServerId(), client.getInetAddress().getHostAddress(),
                            message.getSocketInfo().port());
                    heartBeat(message.getServerId(), message.getSocketInfo(), message.getCode());
                }
                case RegisterRequest -> {
                    serverStatesLock.lock();

                    serverStates.forEach(s -> {
                        if(s.getServerId() == message.getServerId())  {
                            s.increaseTotalNumberOfIterations(message.getNumberOfIterations());
                        }
                    });

                    requestStatusTableModel.addRequest(message, client);
                    serverStatesLock.unlock();
                    output.writeObject(serverStates);
                }
                case UpdateRequest -> {
                    serverStatesLock.lock();

                    serverStates.forEach(s -> {
                        if(s.getServerId() == message.getServerId())  {
                            s.decreaseTotalNumberOfIterations(message.getNumberOfIterations());
                        }
                    });

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

    private void heartBeat(int id, SocketInfo socketInfo, MessageCodes originalMessageCode) {

        int nrHeartBeatsFailed = 0;
        while (nrHeartBeatsFailed < nrHeartBeatTries) {
            try {
                Thread.sleep(heartBeatInterval);
            } catch (InterruptedException ignored) {};
            try {
                var loadBalancer = clusterStatusTableModel.getInfoById(id).createSocket();
                var output = new ObjectOutputStream(loadBalancer.getOutputStream());
                var input = new ObjectInputStream(loadBalancer.getInputStream());
                var heartBeatMessage = new Message(0, 0, 0, MessageCodes.HeartBeat,
                        0, 0, 0, null, "");
                output.writeObject(heartBeatMessage);
                output.flush();
                loadBalancer.close();
            } catch (SocketException e) {
                nrHeartBeatsFailed++;
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (originalMessageCode == MessageCodes.RegisterLoadBalancer) {
            handleLoadBalancerCrash(id, socketInfo);
        } else if (originalMessageCode == MessageCodes.RegisterServer) {
            // server crash handling

        }
    }

    private void handleLoadBalancerCrash(int id, SocketInfo socketInfo) {
        SocketInfo crashedLoadBalancerInfo = clusterStatusTableModel.markLoadBalancerDown(id);
        System.out.printf("Load balancer with ID %d at %s:%d failed to provide a heart beat too many times and " +
                "was marked as down\n", id, socketInfo.address(), socketInfo.port());
        if (!clusterStatusTableModel.activeLoadBalancerExists()) {
            var loadBalancerToPromoteInfo =
                    clusterStatusTableModel.markLoadBalancerPromotion(crashedLoadBalancerInfo);
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
}
