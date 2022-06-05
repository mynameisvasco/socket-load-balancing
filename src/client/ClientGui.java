package client;

import shared.RequestCodes;
import shared.RequestMessage;
import shared.SocketInfo;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.List;

public class ClientGui extends JFrame {
    private static int RequestCount = 0;
    private final Client client;
    private JPanel mainPanel;
    private JTextField numberOfIterationsTextField;
    private JButton sendButton;
    private JTable pendingRequestsTable;
    private JTable responsesTable;

    public ClientGui(int id) {
        super("Client");
        client = new Client(id, List.of(new SocketInfo(1, "localhost", 8000)));
        pendingRequestsTable.setModel(client.getPendingRequestsTableModel());
        responsesTable.setModel(client.getResponsesTableModel());
        sendButton.addActionListener(this::onSendRequest);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setContentPane(mainPanel);
        pack();
        setVisible(true);
    }

    private void onSendRequest(ActionEvent actionEvent) {
        var numberOfIterations = Integer.parseInt(numberOfIterationsTextField.getText());
        var requestId = 1000 * client.getId() + RequestCount;
        var request = new RequestMessage(client.getId(), requestId, RequestCodes.PiCalculation, numberOfIterations, 1);
        client.sendRequest(request);
        RequestCount++;
    }

    public static void main(String[] args) {
        new ClientGui(1);
    }
}
