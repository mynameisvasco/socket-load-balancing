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
    private JTextField loadbalancerIpTextField;
    private JButton connectButton;
    private JTextField clientIdTextField;
    private JTextField deadlineTextField;

    public ClientGui() {
        super("Client");
        client = new Client();
        pendingRequestsTable.setModel(client.getPendingRequestsTableModel());
        responsesTable.setModel(client.getResponsesTableModel());
        sendButton.addActionListener(this::onSendRequest);
        connectButton.addActionListener(this::onConnect);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setContentPane(mainPanel);
        pack();
        setVisible(true);
    }

    private void onConnect(ActionEvent actionEvent) {
        if (!loadbalancerIpTextField.getText().contains(":")) {
            return;
        }

        try {
            var host = loadbalancerIpTextField.getText().split(":")[0];
            var port = Integer.parseInt(loadbalancerIpTextField.getText().split(":")[1]);
            client.setLoadbalancerSocketInfo(host, port);
        } catch (Exception e) {
            System.err.println(e.getMessage());
            JOptionPane.showMessageDialog(null, "Can't connect to loadbalancer.");
        }

        try {
            client.setId(Integer.parseInt(clientIdTextField.getText()));
        } catch (Exception e) {
            System.err.println(e.getMessage());
            JOptionPane.showMessageDialog(null, "Invalid client id or it's already in use.");
        }

        connectButton.setEnabled(false);
        JOptionPane.showMessageDialog(null, "Client id is now connected");
    }

    private void onSendRequest(ActionEvent actionEvent) {
        try {
            var numberOfIterations = Integer.parseInt(numberOfIterationsTextField.getText());
            var deadline = Integer.parseInt(deadlineTextField.getText());
            client.sendRequest(numberOfIterations,deadline);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Number of iterations or deadline is not valid.");
        }
    }

    public static void main(String[] args) {
        new ClientGui();
    }
}
