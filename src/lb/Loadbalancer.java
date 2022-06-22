package lb;

import shared.Message;
import shared.MessageCodes;
import shared.ServerState;
import shared.SocketInfo;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

/**
 * Entity representing the loadbalancer capable of balance all the math service requests through different servers. It
 * should be launched after the monitor process
 */
public class Loadbalancer {
    private final int id;
    private ServerSocket loadBalancer;
    private final int port;
    private SocketInfo monitorInfo;

    /**
     * Creates a new Loadbalancer
     * @param id Id of the loadbalancer
     * @param loadBalancerPort Port that clients use to connect to the load balancer
     * @param monitorIP Monitor entity socket ip
     * @param monitorPort Monitor entity socket port
     */
    public Loadbalancer(int id, int loadBalancerPort) {
        this.id = id;
        this.port = loadBalancerPort;

        try {
            loadBalancer = new ServerSocket(port);
        } catch (IOException e) {
            System.err.format("Failed to initialize load balancer server on port %d\n", port);
            e.printStackTrace();
            System.exit(-1);
        }
    }

    public void setMonitorSocketInfo(String monitorIP, int monitorPort) {
        monitorInfo = new SocketInfo(monitorIP, monitorPort);
    }

    /**
     * Starts listening for incoming requests
     */
    public void listen() {
        System.out.printf("Load balancer listening on port %d\n", port);

        while (true) {
            try {
                var client = loadBalancer.accept();
                new ObjectOutputStream(client.getOutputStream());
                var input = new ObjectInputStream(client.getInputStream());
                var message = (Message) input.readObject();

                switch (message.getCode()) {
                    case PiCalculationRequest -> {
                        System.out.printf("Request received from %s:%d\n", client.getInetAddress().getHostAddress(), client.getPort());
                        var thread = new Thread(() -> requestHandler(message));
                        thread.start();
                    }
                    case PromoteLoadBalancer -> {
                        loadBalancer.close();
                        loadBalancer = new ServerSocket(message.getSocketInfo().port());
                        System.out.printf("Promoted to primary and changed to port %d\n", message.getSocketInfo().port());
                    }
                }
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Returns the server with less load
     * @param serverStates List of the states of all registered servers
     * @return The state of the server with less load
     */
    private ServerState getLessLoadServer(List<ServerState> serverStates) {
        return serverStates.stream()
                .min(Comparator.comparingInt(ServerState::getTotalNumberOfIterations))
                .get();
    }

    /**
     * Receives and redirect the incoming request to the correct server
     * @param request Request to redirect
     */
    private void requestHandler(Message request) {
        try {
            var monitor = monitorInfo.createSocket();
            var monitorOutput = new ObjectOutputStream(monitor.getOutputStream());
            var monitorInput = new ObjectInputStream(monitor.getInputStream());
            monitorOutput.writeObject(request.copyWithCode(MessageCodes.RegisterRequest, "Pending"));
            var serversStates = (LinkedList<ServerState>) monitorInput.readObject();
            monitorOutput.flush();
            var serverState = getLessLoadServer(serversStates);
            var server = serverState.createSocket();
            var serverOutput = new ObjectOutputStream(server.getOutputStream());
            serverOutput.writeObject(request);
            serverOutput.flush();
            server.close();
            monitor.close();
            System.out.printf("Request redirected to %s:%d\n", server.getInetAddress().getHostAddress(), server.getPort());
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * Register the loadbalancer on the monitor's clusters table
     */
    public void registerLoadBalancer() {
        try {
            var monitor = monitorInfo.createSocket();
            var output = new ObjectOutputStream(monitor.getOutputStream());
            var registerMessage = new Message(0, 0, id, MessageCodes.RegisterLoadBalancer, 0, 0, 0, new SocketInfo("localhost", port), "pending");
            output.writeObject(registerMessage);
            output.flush();
            monitor.close();
            System.out.printf("Load balancer registered on monitor at %s:%d\n", monitorInfo.address(), monitorInfo.port());
        } catch (IOException e) {
            System.err.printf("Failed to register load balancer on monitor at %s:%d\n", monitorInfo.address(), monitorInfo.port());
        }
    }

}
