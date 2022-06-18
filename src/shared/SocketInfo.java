package shared;

import java.io.IOException;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.net.Socket;

public record SocketInfo(String address, int port) implements Serializable {
    public Socket createSocket() throws IOException {
        var socket = new Socket();

        socket.connect(new InetSocketAddress(address, port));

        return socket;
    }
}
