package client;

import shared.RequestCodes;
import shared.RequestMessage;
import shared.ResponseMessage;
import shared.SocketInfo;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;

public class Client {
    private static int requestCount = 0;
    private final int id;
    private final PendingRequestsTableModel pendingRequestsTableModel = new PendingRequestsTableModel();
    private final ResponsesTableModel responsesTableModel = new ResponsesTableModel();
    private final int receiverPort;
    private ServerSocket receiver;
    private SocketInfo primaryLbInfo;
    private SocketInfo secondaryLbInfo;


    public Client(int id) {
        this.id = id;
        this.receiverPort = 5999 + id;

        try {
            this.receiver = new ServerSocket(receiverPort);
        } catch (IOException e) {
            System.err.printf("Failed to create receiver socket at port %d\n", receiverPort);
            e.printStackTrace();
            System.exit(-1);
        }

        this.primaryLbInfo = new SocketInfo(1, "localhost", 8000);
        this.secondaryLbInfo = new SocketInfo(2, "localhost", 8001);
    }

    public void sendRequest(int numberOfIterations) {
        var requestId = 1000 * id + requestCount;
        var request = new RequestMessage(id, requestId, RequestCodes.PiCalculation, numberOfIterations, 1,
                new SocketInfo(id, "localhost", receiverPort));

        pendingRequestsTableModel.addRequest(request);
        var senderThread = new Thread(() -> requestSender(request));
        var receiverThread = new Thread(this::responseReceiver);
        senderThread.start();
        receiverThread.start();
        requestCount++;
    }

    public PendingRequestsTableModel getPendingRequestsTableModel() {
        return pendingRequestsTableModel;
    }

    public ResponsesTableModel getResponsesTableModel() {
        return responsesTableModel;
    }

    private void requestSender(RequestMessage request) {
        try {
            var loadbalancer = primaryLbInfo.createSocket();
            var output = new ObjectOutputStream(loadbalancer.getOutputStream());
            var input = new ObjectInputStream(loadbalancer.getInputStream());
            output.writeObject(request);
            output.flush();
            System.out.printf("Request sent %s\n", request);
            var response = (ResponseMessage) input.readObject();
            System.out.printf("Response received %s\n", response);
            responsesTableModel.addResponse(response);
            loadbalancer.close();
        } catch (IOException e) {
            System.err.printf("Failed to send request %s\n", request);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void responseReceiver() {
        try {
            var sender = receiver.accept();
            var input = new ObjectInputStream(sender.getInputStream());
            var response = (ResponseMessage) input.readObject();
            System.out.printf("Response received %s\n", response);
            responsesTableModel.addResponse(response);
            sender.close();
        } catch (IOException e) {
            System.err.println("Failed to receive response");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}
