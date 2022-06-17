package shared;

public class ServerState {
    private final int serverId;
    private final int totalNumberOfIterations;

    public ServerState(int serverId, int totalNumberOfIterations) {
        this.serverId = serverId;
        this.totalNumberOfIterations = totalNumberOfIterations;
    }

    public int getServerId() {
        return serverId;
    }

    public int getTotalNumberOfIterations() {
        return totalNumberOfIterations;
    }

}
