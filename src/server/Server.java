package server;

import shared.Fifo;
import shared.Message;
import shared.MessageCodes;

import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.ServerSocket;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Server {
    private final int id;
    private final int port;
    private final Fifo<Message> pendingRequests = new Fifo<>(2);
    private final Lock totalIterationsLock = new ReentrantLock();
    private ServerSocket server;
    private int totalIterations = 0;

    public Server(int id, int port) {
        this.id = id;
        this.port = 8999 + id;

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
            var thread = new Thread(this::responseSender);
            thread.start();
        }

        while (true) {
            try {
                var client = server.accept();
                var clientInfo = String.format("%s:%d", client.getInetAddress().getHostAddress(), client.getPort());
                new ObjectOutputStream(client.getOutputStream());
                var input = new ObjectInputStream(client.getInputStream());
                var request = (Message) input.readObject();
                System.out.printf("Request received from %s\n", clientInfo);

                if (canDoIterations(request.getNumberOfIterations())) {
                    var receiver = request.getClient().createSocket();
                    var output = new ObjectOutputStream(receiver.getOutputStream());
                    System.out.printf("Request rejected because server has more than 20 iterations %s\n", request);
                    request.setServerId(id);
                    request.setCode(MessageCodes.PiCalculationRejection);
                    output.writeObject(request);
                    receiver.close();
                    continue;
                }

                var acceptedRequest = pendingRequests.enqueue(request);

                if (!acceptedRequest) {
                    var receiver = request.getClient().createSocket();
                    var output = new ObjectOutputStream(receiver.getOutputStream());
                    System.out.printf("Request rejected because server has 3 pending requests %s\n", request);
                    request.setServerId(id);
                    request.setCode(MessageCodes.PiCalculationRejection);
                    output.writeObject(request);
                    receiver.close();
                }

                addIterations(request.getNumberOfIterations());
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    private void responseSender() {
        while (true) {
            var request = pendingRequests.dequeue();
            var receiver = request.getClient().createSocket();
            var pi = truncateTo(Math.PI, request.getNumberOfIterations());
            request.setServerId(id);
            request.setPi(pi);
            request.setCode(MessageCodes.PiCalculationResult);

            try {
                for (int k = 0; k < request.getNumberOfIterations(); k++) {
                    Thread.sleep(5000);
                }

                var output = new ObjectOutputStream(receiver.getOutputStream());
                output.writeObject(request);
                System.out.printf("Response sent to %s\n", String.format("%s:%d", receiver.getInetAddress().getHostAddress(), receiver.getPort()));
                receiver.close();
            } catch (IOException | InterruptedException e) {
                System.err.printf("Failed to respond to request %s\n", request);
                e.printStackTrace();
            }

            subIterations(request.getNumberOfIterations());
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



