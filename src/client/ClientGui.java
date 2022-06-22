package client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class ClientGui extends JFrame {
    private final Client client;
    private JPanel mainPanel;
    private JTextField numberOfIterationsTextField;
    private JButton sendButton;
    private JTable pendingRequestsTable;
    private JTable responsesTable;
    private JTextField loadBalancerIpTextField;
    private JTextField clientIdTextField;
    private JTextField deadlineTextField;

    public ClientGui() {
        super("Client");
        client = new Client();
        setSize(new Dimension(400, 820));
        pendingRequestsTable.setModel(client.getPendingRequestsTableModel());
        responsesTable.setModel(client.getResponsesTableModel());
        sendButton.addActionListener(this::onSendRequest);
        clientIdTextField.addKeyListener(new KeyAdapter() {
            public void keyTyped(KeyEvent e) {
                char c = e.getKeyChar();
                if (!((c >= '0') && (c <= '9') ||
                        (c == KeyEvent.VK_BACK_SPACE) ||
                        (c == KeyEvent.VK_DELETE))) {
                    getToolkit().beep();
                    e.consume();
                }
            }
        });
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setContentPane(mainPanel);
        setVisible(true);
    }

    private void onSendRequest(ActionEvent actionEvent) {
        try {
            var host = loadBalancerIpTextField.getText().split(":")[0];
            var port = Integer.parseInt(loadBalancerIpTextField.getText().split(":")[1]);
            client.setId(Integer.parseInt(clientIdTextField.getText()));
            client.setLoadbalancerSocketInfo(host, port);
        } catch (Exception e) {
            System.err.println(e.getMessage());
            JOptionPane.showMessageDialog(null, "Can't connect to load balancer.");
        }

        try {
            var numberOfIterations = Integer.parseInt(numberOfIterationsTextField.getText());
            var deadline = Integer.parseInt(deadlineTextField.getText());
            client.sendRequest(numberOfIterations, deadline);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Number of iterations or deadline is not valid.");
        }
    }

    public static void main(String[] args) {
        new ClientGui();
    }
}
