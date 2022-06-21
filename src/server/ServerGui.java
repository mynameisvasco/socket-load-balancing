package server;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class ServerGui extends JFrame {
    private JPanel mainPanel;
    private JLabel idLabel;
    private JButton launchButton;
    private JButton closeButton;
    private JLabel serverStateLabel;
    private JTable requestsTable;
    private JLabel responsesLabel;
    private JTable responsesTable;
    private JTextField portTextField;
    private JLabel portLabel;
    private JTextField idTextField;
    private Server server;

    public ServerGui() {
        super("Server");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setContentPane(mainPanel);
        closeButton.addActionListener(this::onClose);
        launchButton.addActionListener(this::onLaunch);
        pack();
        setVisible(true);
    }

    private void onClose(ActionEvent actionEvent) {
        System.exit(0);
    }

    private void onLaunch(ActionEvent actionEvent) {
        launchButton.setEnabled(false);
        portTextField.setEnabled(false);
        server = new Server(Integer.parseInt(idTextField.getText()), Integer.parseInt(portTextField.getText()));
        requestsTable.setModel(server.getRequestsTableModel());
        responsesTable.setModel(server.getResponsesTableModel());
        Thread listenThread = new Thread(() -> server.listen());
        listenThread.start();
    }

    public static void main(String[] args) {
        new ServerGui();
    }
}
