package shared;

import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

public class ServerState implements Serializable {
    private final InetAddress address;
    private final int port;
    private final int serverId;
    private int totalNumberOfIterations;

    public ServerState(InetAddress address, int port, int serverId) {
        this.address = address;
        this.port = port;
        this.serverId = serverId;
        this.totalNumberOfIterations = 0;
    }

    public int getServerId() {
        return serverId;
    }

    public int getTotalNumberOfIterations() {
        return totalNumberOfIterations;
    }

    public void increaseTotalNumberOfIterations(int value) {
        totalNumberOfIterations += value;
    }

    public void decreaseTotalNumberOfIterations(int value) {
        totalNumberOfIterations += value;
    }

    public Socket createSocket() {
        var socket = new Socket();

        try {
            socket.connect(new InetSocketAddress(address, port));
            return socket;
        } catch (IOException e) {
            System.err.printf("Failed to create socket for %s:%s\n", address, port);
            System.exit(-1);
            e.printStackTrace();
        }

        return socket;
    }

}
