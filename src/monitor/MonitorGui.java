package monitor;

import javax.swing.*;
import java.awt.event.*;

public class MonitorGui extends JFrame {
    private JPanel mainPanel;
    private JTextField portTextField;
    private JButton launchButton;
    private JButton closeButton;
    private JLabel portLabel;
    private JLabel clusterStatusLabel;
    private JLabel requestsStatus;
    private JTable clusterStatusTable;
    private JTable requestStatusTable;

    private Monitor monitor;

    public MonitorGui() {
        super("Monitor");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setContentPane(mainPanel);
        closeButton.addActionListener(this::onClose);
        launchButton.addActionListener(this::onLaunch);
        portTextField.addKeyListener(new KeyAdapter() {
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
        pack();
        setVisible(true);
    }

    private void onClose(ActionEvent actionEvent) {
        System.exit(0);
    }

    private void onLaunch(ActionEvent actionEvent) {
        launchButton.setEnabled(false);
        portTextField.setEnabled(false);
        monitor = new Monitor(Integer.parseInt(portTextField.getText()));
        clusterStatusTable.setModel(monitor.getClusterStatusTableModel());
        var listenThread = new Thread(() -> monitor.listen());
        listenThread.start();
    }

    public static void main(String[] args) {
        new MonitorGui();
    }
}
