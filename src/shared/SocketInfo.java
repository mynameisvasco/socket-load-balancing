package shared;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

public record SocketInfo(int id, String address, int port) {
    public Socket createSocket() {
        var socket = new Socket();

        try {
            socket.connect(new InetSocketAddress(address, port));
            return socket;
        } catch (IOException e) {
            System.err.printf("Failed to create socket for %s:%s\n", address, port);
            e.printStackTrace();
        }

        return null;
    }
}
