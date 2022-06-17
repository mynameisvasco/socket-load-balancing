package client;

import shared.*;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;

public class Client {
    private static int requestCount = 0;
    private final PendingRequestsTableModel pendingRequestsTableModel = new PendingRequestsTableModel();
    private final ResponsesTableModel responsesTableModel = new ResponsesTableModel();
    private int receiverPort;
    private int id;
    private ServerSocket receiver;
    private SocketInfo loadBalancerInfo;

    public void setId(int id) {
        this.id = id;
        this.receiverPort = 5999 + id;

        try {
            if (this.receiver != null && !this.receiver.isClosed()) {
                this.receiver.close();
            }

            this.receiver = new ServerSocket(receiverPort);
        } catch (IOException e) {
            System.err.printf("Failed to create receiver socket at port %d\n", receiverPort);
            e.printStackTrace();
            System.exit(-1);
        }
    }

    public void sendRequest(int numberOfIterations, int deadline) {
        if (loadBalancerInfo == null || id == 0) {
            System.err.println("It's not possible to send a request without setting the loadbalancer ip first and the client id");
            return;
        }

        var requestId = 1000 * id + requestCount;
        var request = new Message(id, requestId, 0, MessageCodes.PiCalculationRequest, numberOfIterations,
                0, deadline, new SocketInfo("localhost", receiverPort), "pending");

        pendingRequestsTableModel.addRequest(request);
        var senderThread = new Thread(() -> requestSender(request));
        var receiverThread = new Thread(this::responseReceiver);
        senderThread.start();
        receiverThread.start();
        requestCount++;
    }

    public void setLoadbalancerSocketInfo(String host, int port) {
        loadBalancerInfo = new SocketInfo(host, port);
    }

    public PendingRequestsTableModel getPendingRequestsTableModel() {
        return pendingRequestsTableModel;
    }

    public ResponsesTableModel getResponsesTableModel() {
        return responsesTableModel;
    }

    private void requestSender(Message request) {
        try {
            var loadbalancer = loadBalancerInfo.createSocket();
            var output = new ObjectOutputStream(loadbalancer.getOutputStream());
            var input = new ObjectInputStream(loadbalancer.getInputStream());
            output.writeObject(request);
            output.flush();
            System.out.printf("Request sent %s\n", request.getRequestId());
            var response = (Message) input.readObject();
            System.out.printf("Response received %s\n", response.getRequestId());
            responsesTableModel.addResponse(response);
            loadbalancer.close();
        } catch (IOException e) {
            System.err.printf("Failed to send request %s\n", request.getRequestId());
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void responseReceiver() {
        try {
            var sender = receiver.accept();
            var input = new ObjectInputStream(sender.getInputStream());
            var response = (Message) input.readObject();
            System.out.printf("Response received %s\n", response.getRequestId());
            responsesTableModel.addResponse(response);
            sender.close();
        } catch (IOException e) {
            System.err.println("Failed to receive response");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}
