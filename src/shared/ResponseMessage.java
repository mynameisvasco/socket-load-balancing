package shared;

import java.io.Serializable;

public record ResponseMessage(int clientId, int requestId, int serverId, ResponseCodes code, int numberOfIterations, double pi,
                              int deadline) implements Serializable {
}