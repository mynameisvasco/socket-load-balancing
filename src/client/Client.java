package client;

import shared.*;

import javax.swing.*;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;

/**
 * Entity representing the client capable of using the math service using a TCP Socket.
 */
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

    /**
     * Creates and start the threads responsible for sending a request and receiving the response
     *
     * @param numberOfIterations Number of decimal places of PI
     * @param deadline           Priority of the request
     */
    public void sendRequest(int numberOfIterations, int deadline) {
        if (loadBalancerInfo == null || id == 0) {
            System.err.println("It's not possible to send a request without setting the loadbalancer ip first and the client id");
            return;
        }

        var requestId = 1000 * id + requestCount;
        var request = new Message(id, requestId, 0, MessageCodes.PiCalculationRequest, numberOfIterations,
                0, deadline, new SocketInfo("localhost", receiverPort), "pending");

        var senderThread = new Thread(() -> requestSender(request));
        var receiverThread = new Thread(this::responseReceiver);
        receiverThread.start();
        senderThread.start();
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

    /**
     * Sends a request to the math service
     *
     * @param request Request to be sent
     */
    private void requestSender(Message request) {
        try {
            var loadbalancer = loadBalancerInfo.createSocket();
            var output = new ObjectOutputStream(loadbalancer.getOutputStream());
            output.writeObject(request);
            output.flush();
            SwingUtilities.invokeLater(() -> {
                pendingRequestsTableModel.addRequest(request);
            });
            System.out.printf("Request sent %s\n", request.getRequestId());
            loadbalancer.close();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Loadbalancer is not active or ip and port are incorrect.");
            System.err.printf("Failed to send request %s\n", request.getRequestId());
        }
    }

    /**
     * Receives a response from the math service
     */
    private void responseReceiver() {
        try {
            var sender = receiver.accept();
            var input = new ObjectInputStream(sender.getInputStream());
            var response = (Message) input.readObject();
            System.out.printf("Response received %s\n", response.getRequestId());
            pendingRequestsTableModel.removeRequest(response);
            responsesTableModel.addResponse(response);
            sender.close();
        } catch (IOException | ClassNotFoundException e) {
            responseReceiver();
            System.err.println("Failed to receive response");
        }
    }
}
