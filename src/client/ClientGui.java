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
    private JButton saveLoadBalancerIp;
    private JTextField clientIdTextField;
    private JButton saveClientId;

    public ClientGui() {
        super("Client");
        client = new Client();
        pendingRequestsTable.setModel(client.getPendingRequestsTableModel());
        responsesTable.setModel(client.getResponsesTableModel());
        sendButton.addActionListener(this::onSendRequest);
        saveLoadBalancerIp.addActionListener(this::onSaveLoadBalancerIp);
        saveClientId.addActionListener(this::onSaveClientId);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setContentPane(mainPanel);
        pack();
        setVisible(true);
    }

    private void onSaveClientId(ActionEvent actionEvent) {
        try {
            client.setId(Integer.parseInt(clientIdTextField.getText()));
        }catch (Exception e) {
            System.err.println(e.getMessage());
            JOptionPane.showMessageDialog(null, "Invalid client id or it's already in use");
        }
    }

    private void onSaveLoadBalancerIp(ActionEvent actionEvent) {
        if (!loadbalancerIpTextField.getText().contains(":")) {
            return;
        }

        try {
            var host = loadbalancerIpTextField.getText().split(":")[0];
            var port = Integer.parseInt(loadbalancerIpTextField.getText().split(":")[1]);
            client.setLoadbalancerSocketInfo(host, port);
        } catch (Exception e) {
            //Ignored
        }
    }

    private void onSendRequest(ActionEvent actionEvent) {
        var numberOfIterations = Integer.parseInt(numberOfIterationsTextField.getText());
        client.sendRequest(numberOfIterations);
    }

    public static void main(String[] args) {
        new ClientGui();
    }
}
