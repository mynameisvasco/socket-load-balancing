package server;

import shared.Fifo;
import shared.RequestMessage;
import shared.ResponseCodes;

import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.ServerSocket;
import java.util.HashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Server {
    private final int id;
    private final int port;
    private final Fifo<RequestMessage> pendingRequests = new Fifo<>(2);
    private final HashMap<Integer, ObjectOutputStream> outputStreams = new HashMap<>();
    private final Lock totalIterationsLock = new ReentrantLock();
    private ServerSocket server;
    private int totalIterations = 0;

    public Server(int id, int port) {
        this.id = id;
        this.port = port;

        try {
            server = new ServerSocket(port);
        } catch (IOException e) {
            System.err.format("Failed to initialize server on port %d\n", port);
            e.printStackTrace();
            System.exit(-1);
        }
    }

    public void listen() {
        System.out.printf("Server listen on port %s\n", port);

        for (int i = 0; i < 3; i++) {
            var thread = new Thread(this::requestHandler);
            thread.start();
        }

        while (true) {
            try {
                var client = server.accept();
                var clientInfo = String.format("%s:%d", client.getInetAddress().getHostAddress(), client.getPort());
                System.out.printf("Incoming request from %s\n", clientInfo);
                var output = new ObjectOutputStream(client.getOutputStream());
                var input = new ObjectInputStream(client.getInputStream());
                var request = (RequestMessage) input.readObject();
                outputStreams.put(request.requestId(), output);

                if (canDoIterations(request.numberOfIterations())) {
                    System.out.printf("Request rejected because server has more than 20 iterations %s\n", request);
                    var response = request.respond(id, 0, ResponseCodes.Rejected);
                    output.writeObject(response);
                    continue;
                }

                var acceptedRequest = pendingRequests.enqueue(request);

                if (!acceptedRequest) {
                    System.out.printf("Request rejected because server has 3 pending requests %s\n", request);
                    var response = request.respond(id, 0, ResponseCodes.Rejected);
                    output.writeObject(response);
                }
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    private void requestHandler() {
        while (true) {
            var request = pendingRequests.dequeue();
            addIterations(request.numberOfIterations());
            var pi = truncateTo(Math.PI, request.numberOfIterations());
            var response = request.respond(id, pi, ResponseCodes.PiCalculation);

            try {
                for (int k = 0; k < request.numberOfIterations(); k++) {
                    Thread.sleep(5000);
                }

                outputStreams.get(request.requestId()).writeObject(response);
                outputStreams.remove(request.requestId());
                subIterations(request.numberOfIterations());
            } catch (IOException | InterruptedException e) {
                System.err.printf("Failed to respond to request %s\n", request);
                e.printStackTrace();
            }
        }
    }

    private void addIterations(int numberOfIterations) {
        totalIterationsLock.lock();
        totalIterations += numberOfIterations;
        totalIterationsLock.unlock();
    }

    private void subIterations(int numberOfIterations) {
        totalIterationsLock.lock();
        totalIterations -= numberOfIterations;
        totalIterationsLock.unlock();
    }

    private boolean canDoIterations(int numberOfIterations) {
        try {
            totalIterationsLock.lock();
            return totalIterations + numberOfIterations > 20;
        } finally {
            totalIterationsLock.unlock();
        }
    }

    private static double truncateTo(double number, int places) {
        return new BigDecimal(number)
                .setScale(places, RoundingMode.DOWN)
                .stripTrailingZeros()
                .doubleValue();
    }

    public static void main(String[] args) {
        var server = new Server(1, 9000);
        server.listen();
    }
}



