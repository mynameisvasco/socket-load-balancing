package shared;

import java.io.Serializable;

public class Message implements Serializable {

    private final int clientId;
    private final int requestId;
    private int serverId;
    private MessageCodes code;
    private final int numberOfIterations;
    private double pi;
    private final int deadline;
    private final SocketInfo client;

    public Message(int clientId, int requestId, int serverId, MessageCodes code, int numberOfIterations, double pi,
                      int deadline, SocketInfo client) {
        this.clientId = clientId;
        this.requestId = requestId;
        this.serverId = serverId;
        this.code = code;
        this.numberOfIterations = numberOfIterations;
        this.pi = pi;
        this.deadline = deadline;
        this.client = client;
    }

    public int getClientId() {
        return clientId;
    }

    public int getRequestId() {
        return requestId;
    }

    public int getServerId() {
        return serverId;
    }

    public MessageCodes getCode() {
        return code;
    }

    public int getNumberOfIterations() {
        return numberOfIterations;
    }

    public double getPi() {
        return pi;
    }

    public int getDeadline() {
        return deadline;
    }

    public SocketInfo getClient() {
        return client;
    }

    public void setServerId(int serverId) {
        this.serverId = serverId;
    }

    public void setCode(MessageCodes code) {
        this.code = code;
    }

    public void setPi(double pi) {
        this.pi = pi;
    }
}