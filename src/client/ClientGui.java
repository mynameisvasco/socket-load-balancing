package client;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class ClientGui extends JFrame {
    private final Client client;
    private JPanel mainPanel;
    private JTextField numberOfIterationsTextField;
    private JButton sendButton;
    private JTable pendingRequestsTable;
    private JTable responsesTable;

    public ClientGui(int id) {
        super("Client");
        client = new Client(id);
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
        client.sendRequest(numberOfIterations);
    }

    public static void main(String[] args) {
        new ClientGui(1);
    }
}
