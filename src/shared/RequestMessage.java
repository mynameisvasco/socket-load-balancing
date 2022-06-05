package shared;

import java.io.Serializable;

public record RequestMessage(int clientId, int requestId, RequestCodes code, int numberOfIterations,
                             int deadline) implements Serializable {

    public ResponseMessage respond(int serverId, double pi, ResponseCodes code) {
        return new ResponseMessage(clientId(), requestId(), serverId, code, numberOfIterations(), pi, deadline());
    }
}