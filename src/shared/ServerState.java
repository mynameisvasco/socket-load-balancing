package shared;

import java.io.IOException;
import java.io.Serializable;
import java.net.Socket;

public class ServerState implements Serializable {
    private final SocketInfo socketInfo;
    private final int serverId;
    private int totalNumberOfIterations;

    public ServerState(SocketInfo socketInfo, int serverId) {
        this.socketInfo = socketInfo;
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

    public Socket createSocket()  {
        try {
            return socketInfo.createSocket();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

}
