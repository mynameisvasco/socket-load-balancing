package server;

import shared.Fifo;
import shared.Message;
import shared.MessageCodes;
import shared.SocketInfo;

import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.ServerSocket;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Entity responsible for providing math services (PI) to the clients
 */
public class Server {
    private final int id;
    private final int port;
    private final SocketInfo monitorInfo;
    private final Fifo<Message> pendingRequests = new Fifo<>(2);
    private final ServerStateTableModel serverStateTableModel = new ServerStateTableModel();
    private final ResponsesTableModel responsesTableModel = new ResponsesTableModel();
    private final Lock totalIterationsLock = new ReentrantLock();
    private final Lock serverStateLock = new ReentrantLock();
    private ServerSocket server;
    private int totalIterations = 0;

    /**
     * Creates new Server
     * @param id Id of the server
     * @param port Port used by the server socket
     */
    public Server(int id, int port, String monitorIp) {
        this.id = id;
        this.port = port;
        this.monitorInfo = new SocketInfo(monitorIp, 6999);

        try {
            server = new ServerSocket(port);
        } catch (IOException e) {
            System.err.format("Failed to initialize server on port %d\n", port);
            e.printStackTrace();
            System.exit(-1);
        }
    }

    /**
     * Starts listening for incoming requests and reasons about the possibility to handle the request.
     */
    public void listen() {
        System.out.printf("Server listen on port %s\n", port);
        this.registerServer();

        for (int i = 0; i < 3; i++) {
            int threadNr = i + 1;
            var thread = new Thread(() -> responseSender(threadNr));
            thread.start();
        }

        while (true) {
            try {
                var client = server.accept();
                var clientInfo = String.format("%s:%d", client.getInetAddress().getHostAddress(), client.getPort());
                var output = new ObjectOutputStream(client.getOutputStream());
                var input = new ObjectInputStream(client.getInputStream());
                var request = (Message) input.readObject();

                if (request.getCode() == MessageCodes.HeartBeat) {
                    var heartbeatMessage = request.copyWithCode(MessageCodes.HeartBeat, "");
                    heartbeatMessage.setServerId(id);
                    heartbeatMessage.setNumberOfIterations(getIterations());
                    output.writeObject(heartbeatMessage);
                    output.flush();
                    continue;
                }

                System.out.printf("Request received from %s\n", clientInfo);

                serverStateLock.lock();

                if (!canDoIterations(request.getNumberOfIterations())) {
                    var monitor = monitorInfo.createSocket();
                    var monitorOutput = new ObjectOutputStream(monitor.getOutputStream());
                    var updateRequestMessage = request.copyWithCode(MessageCodes.UpdateRequest, "Rejected");
                    updateRequestMessage.setServerId(id);
                    monitorOutput.writeObject(updateRequestMessage);
                    monitorOutput.flush();
                    var receiver = request.getSocketInfo().createSocket();
                    var receiverOutput = new ObjectOutputStream(receiver.getOutputStream());
                    System.out.printf("Request rejected because server has more than 20 iterations %s\n", request);
                    request.setServerId(id);
                    request.setCode(MessageCodes.PiCalculationRejection);
                    responsesTableModel.addResponse(request);
                    receiverOutput.writeObject(request);
                    continue;
                }

                var acceptedRequest = pendingRequests.enqueue(request);

                if (!acceptedRequest) {
                    var monitor = monitorInfo.createSocket();
                    var monitorOutput = new ObjectOutputStream(monitor.getOutputStream());
                    var updateRequestMessage = request.copyWithCode(MessageCodes.UpdateRequest, "Rejected");
                    updateRequestMessage.setServerId(id);
                    monitorOutput.writeObject(updateRequestMessage);
                    monitorOutput.flush();
                    var receiver = request.getSocketInfo().createSocket();
                    var receiverOutput = new ObjectOutputStream(receiver.getOutputStream());
                    System.out.printf("Request rejected because server has 2 pending requests %s\n", request);
                    request.setServerId(id);
                    request.setCode(MessageCodes.PiCalculationRejection);
                    responsesTableModel.addResponse(request);
                    receiverOutput.writeObject(request);
                } else {
                    serverStateTableModel.addRequestToQueue(request);
                }

                serverStateLock.unlock();

            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Picks the most priority request from the fifo and replies with the correct response
     */
    private void responseSender(int threadNr) {
        while (true) {

            var request = pendingRequests.dequeue();
            serverStateTableModel.dequeueResquest(request, threadNr);
            try {
                var monitor = monitorInfo.createSocket();
                var monitorOutput = new ObjectOutputStream(monitor.getOutputStream());
                var updateRequestMessage = request.copyWithCode(MessageCodes.UpdateRequest, "Processing");
                updateRequestMessage.setServerId(id);
                monitorOutput.writeObject(updateRequestMessage);
                monitorOutput.flush();
                addIterations(request.getNumberOfIterations());
                var pi = truncateTo(Math.PI, request.getNumberOfIterations());
                request.setServerId(id);
                request.setPi(pi);
                request.setCode(MessageCodes.PiCalculationResult);

                for (int k = 0; k < request.getNumberOfIterations(); k++) {
                    Thread.sleep(5000);
                }

                var receiver = request.getSocketInfo().createSocket();
                serverStateTableModel.removeRequest(request);
                responsesTableModel.addResponse(request);
                var output = new ObjectOutputStream(receiver.getOutputStream());
                output.writeObject(request);
                output.flush();
                System.out.printf("Response sent to %s\n", String.format("%s:%d", receiver.getInetAddress().getHostAddress(), receiver.getPort()));
                monitor = monitorInfo.createSocket();
                monitorOutput = new ObjectOutputStream(monitor.getOutputStream());
                monitorOutput.writeObject(updateRequestMessage.copyWithCode(MessageCodes.UpdateRequest, "Completed"));
                monitorOutput.flush();
            } catch (IOException | InterruptedException e) {
                System.err.printf("Failed to respond to request %s\n", request);
                e.printStackTrace();
            }

            subIterations(request.getNumberOfIterations());
        }
    }

    /**
     * Adds thread-safely to the number of current iterations
     * @param numberOfIterations Number of iterations to add
     */
    private void addIterations(int numberOfIterations) {
        totalIterationsLock.lock();
        totalIterations += numberOfIterations;
        totalIterationsLock.unlock();
    }

    /**
     * Subtract thread-safely to the number of current iterations
     * @param numberOfIterations Number of iterations to subtract
     */
    private void subIterations(int numberOfIterations) {
        totalIterationsLock.lock();
        totalIterations -= numberOfIterations;
        totalIterationsLock.unlock();
    }

    /**
     * Check thread-safely if the server supports more iterations
     * @param numberOfIterations Number of iterations to check
     * @return true if it's possible, false otherwise.
     */
    private boolean canDoIterations(int numberOfIterations) {
        try {
            totalIterationsLock.lock();
            return totalIterations + numberOfIterations <= 20;
        } finally {
            totalIterationsLock.unlock();
        }
    }

    /**
     * Get the current number of active iterations
     * @return iterations number
     */
    private int getIterations() {
        try {
            totalIterationsLock.lock();
            return totalIterations;
        } finally {
            totalIterationsLock.unlock();
        }
    }

    /**
     * Truncates a number to a specified number of decimal places
     * @param number Any double
     * @param places Number of decimal places
     * @return Truncated number
     */
    private static double truncateTo(double number, int places) {
        return new BigDecimal(number)
                .setScale(places, RoundingMode.DOWN)
                .stripTrailingZeros()
                .doubleValue();
    }

    /**
     * Register the server on the monitor's clusters table
     */
    private void registerServer() {
        try {
            var monitor = monitorInfo.createSocket();
            var output = new ObjectOutputStream(monitor.getOutputStream());
            var input = new ObjectInputStream(monitor.getInputStream());
            var registerMessage = new Message(0, 0, id, MessageCodes.RegisterServer,
                    0, 0, 0, new SocketInfo("localhost", port), "pending");
            output.writeObject(registerMessage);
            output.flush();
            System.out.printf("Server registered on monitor at %s:%d\n", monitorInfo.address(), monitorInfo.port());
            monitor.close();
        } catch (IOException e) {
            System.err.printf("Failed to register server on monitor at %s:%d\n", monitorInfo.address(),
                    monitorInfo.port());
        }
    }

    public ServerStateTableModel getRequestsTableModel() {
        return serverStateTableModel;
    }

    public ResponsesTableModel getResponsesTableModel() {
        return responsesTableModel;
    }
}


