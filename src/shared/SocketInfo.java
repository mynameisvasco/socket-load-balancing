package shared;

import java.io.IOException;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.net.Socket;

public record SocketInfo(String address, int port) implements Serializable {
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
