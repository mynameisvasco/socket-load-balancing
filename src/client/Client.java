package client;

import shared.RequestMessage;
import shared.ResponseMessage;
import shared.SocketInfo;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;

public class Client {
    private final int id;
    private final PendingRequestsTableModel pendingRequestsTableModel = new PendingRequestsTableModel();
    private final ResponsesTableModel responsesTableModel = new ResponsesTableModel();
    private final List<SocketInfo> loadbalancersInfo;

    public Client(int id, List<SocketInfo> loadbalancersInfo) {
        this.id = id;
        this.loadbalancersInfo = loadbalancersInfo;
    }

    public int getId() {
        return id;
    }

    public void sendRequest(RequestMessage request) {
        pendingRequestsTableModel.addRequest(request);
        var thread = new Thread((() -> requestSender(request)));
        thread.start();
    }

    public PendingRequestsTableModel getPendingRequestsTableModel() {
        return pendingRequestsTableModel;
    }

    public ResponsesTableModel getResponsesTableModel() {
        return responsesTableModel;
    }

    private Socket getPrimaryLoadBalancer() {
        return loadbalancersInfo.get(0).createSocket();
    }

    private void requestSender(RequestMessage request) {
        try {
            var loadbalancer = getPrimaryLoadBalancer();
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
}
