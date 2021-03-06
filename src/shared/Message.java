package shared;

import java.io.Serializable;

public class Message implements Serializable, IPriorityItem {

    private final int clientId;
    private final int requestId;
    private int serverId;
    private MessageCodes code;
    private int numberOfIterations;
    private double pi;
    private final String status;
    private final int deadline;
    private final SocketInfo socketInfo;


    public Message(int clientId, int requestId, int serverId, MessageCodes code, int numberOfIterations, double pi,
                   int deadline, SocketInfo socketInfo, String status) {
        this.clientId = clientId;
        this.requestId = requestId;
        this.serverId = serverId;
        this.code = code;
        this.numberOfIterations = numberOfIterations;
        this.pi = pi;
        this.deadline = deadline;
        this.socketInfo = socketInfo;
        this.status = status;
    }

    public int getClientId() {
        return clientId;
    }

    public int getRequestId() {
        return requestId;
    }

    public void setNumberOfIterations(int numberOfIterations) {
        this.numberOfIterations = numberOfIterations;
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

    public SocketInfo getSocketInfo() {
        return socketInfo;
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

    public String getStatus() {
        return status;
    }

    @Override
    public int getPriority() {
        return this.deadline;
    }

    public Message copyWithCode(MessageCodes code, String status) {
        return new Message(clientId, requestId, serverId, code, numberOfIterations, pi, deadline, socketInfo, status);
    }
}