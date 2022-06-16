package lb;

import shared.Message;
import shared.MessageCodes;
import shared.SocketInfo;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;

public class Loadbalancer {
    private final int id;
    private ServerSocket loadbalancer;
    private final int port;
    private final SocketInfo monitorInfo;
    private final List<SocketInfo> serverInfos;

    public Loadbalancer(int id, List<SocketInfo> serverInfos) {
        this.id = id;
        this.port = 7999 + id;
        this.monitorInfo = new SocketInfo("localhost", 6999);
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
                new ObjectOutputStream(client.getOutputStream());
                var input = new ObjectInputStream(client.getInputStream());
                var request = (Message) input.readObject();
                System.out.printf("Request received from %s:%d\n", client.getInetAddress().getHostAddress(), client.getPort());
                var thread = new Thread(() -> requestHandler(request));
                thread.start();
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    private Socket getLessLoadServer() {
        return serverInfos.get(0).createSocket();
    }

    private void requestHandler(Message request) {
        try {
            var server = getLessLoadServer();
            var serverOutput = new ObjectOutputStream(server.getOutputStream());
            serverOutput.writeObject(request);
            System.out.printf("Request redirected to %s:%d\n", server.getInetAddress().getHostAddress(), server.getPort());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void registerLoadBalancer() {
        try {
            var monitor = monitorInfo.createSocket();
            var output = new ObjectOutputStream(monitor.getOutputStream());
            var input = new ObjectInputStream(monitor.getInputStream());
            var registerMessage = new Message(0, 0, id, MessageCodes.RegisterLoadBalancer,
                    0, 0, 0, new SocketInfo("localhost", port));
            output.writeObject(registerMessage);
            output.flush();
            System.out.printf("Load balancer registered on monitor at %s:%d\n", monitorInfo.address(), monitorInfo.port());
            monitor.close();
        } catch (IOException e) {
            System.err.printf("Failed to register load balancer on monitor at %s:%d\n", monitorInfo.address(),
                    monitorInfo.port());
        }
    }

    public static void main(String[] args) {
        var lb = new Loadbalancer(8000, List.of(new SocketInfo("localhost", 9000)));
        lb.registerLoadBalancer();
        lb.listen();
    }
}
