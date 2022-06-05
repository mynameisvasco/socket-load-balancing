package client;

import shared.RequestMessage;
import shared.ResponseMessage;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

public class Client {
    private final int id;
    private final PendingRequestsTableModel pendingRequestsTableModel = new PendingRequestsTableModel();
    private final ResponsesTableModel responsesTableModel = new ResponsesTableModel();

    public Client(int id) {
        this.id = id;
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

    private void requestSender(RequestMessage request) {
        try {
            var socket = new Socket();
            System.out.println("Trying to establish connection");
            socket.connect(new InetSocketAddress("localhost", 8000));
            var output = new ObjectOutputStream(socket.getOutputStream());
            var input = new ObjectInputStream(socket.getInputStream());
            output.writeObject(request);
            output.flush();
            System.out.printf("Request sent successfully %s\n", request);
            var response = (ResponseMessage) input.readObject();
            System.out.printf("Response received successfully %s\n", response);
            responsesTableModel.addResponse(response);
            socket.close();
        } catch (IOException e) {
            System.err.printf("Failed to send request %s\n", request);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}
