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

public class Loadbalancer {
    private final int id;
    private ServerSocket loadBalancer;
    private final int port;
    private final SocketInfo monitorInfo;

    public Loadbalancer(int id, int loadBalancerPort, String monitorIP, int monitorPort) {
        this.id = id;
        this.port = loadBalancerPort;
        this.monitorInfo = new SocketInfo(monitorIP, monitorPort);

        try {
            loadBalancer = new ServerSocket(port);
        } catch (IOException e) {
            System.err.format("Failed to initialize load balancer server on port %d\n", port);
            e.printStackTrace();
            System.exit(-1);
        }
    }

    public void listen() {
        System.out.printf("Load balancer listening on port %d\n", port);

        while (true) {
            try {
                var client = loadBalancer.accept();
                new ObjectOutputStream(client.getOutputStream());
                var input = new ObjectInputStream(client.getInputStream());
                var message = (Message) input.readObject();
                switch (message.getCode()) {
                    case PiCalculationRequest:
                        System.out.printf("Request received from %s:%d\n", client.getInetAddress().getHostAddress(), client.getPort());
                        var thread = new Thread(() -> requestHandler(message));
                        thread.start();
                        break;
                    case HeartBeat:
                        break;
                }
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    private ServerState getLessLoadServer(List<ServerState> serverStates) {
        return serverStates.stream()
                .min(Comparator.comparingInt(ServerState::getTotalNumberOfIterations))
                .get();
    }

    private void requestHandler(Message request) {
        try {
            var monitor = monitorInfo.createSocket();
            var monitorOutput = new ObjectOutputStream(monitor.getOutputStream());
            var monitorInput = new ObjectInputStream(monitor.getInputStream());
            monitorOutput.writeObject(request.copyWithCode(MessageCodes.RegisterRequest, "pending"));
            var serversStates = (LinkedList<ServerState>) monitorInput.readObject();
            var serverState = getLessLoadServer(serversStates);
            var server = serverState.createSocket();
            var serverOutput = new ObjectOutputStream(server.getOutputStream());
            serverOutput.writeObject(request);
            System.out.printf("Request redirected to %s:%d\n", server.getInetAddress().getHostAddress(), server.getPort());
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void registerLoadBalancer() {
        try {
            var monitor = monitorInfo.createSocket();
            var output = new ObjectOutputStream(monitor.getOutputStream());
            var input = new ObjectInputStream(monitor.getInputStream());
            var registerMessage = new Message(0, 0, id, MessageCodes.RegisterLoadBalancer,
                    0, 0, 0, new SocketInfo("localhost", port), "pending");
            output.writeObject(registerMessage);
            output.flush();
            System.out.printf("Load balancer registered on monitor at %s:%d\n", monitorInfo.address(), monitorInfo.port());
            monitor.close();
        } catch (IOException e) {
            System.err.printf("Failed to register load balancer on monitor at %s:%d\n", monitorInfo.address(),
                    monitorInfo.port());
        }
    }

}
