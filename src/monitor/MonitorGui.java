package monitor;

import javax.swing.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

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
        pack();
        setVisible(true);
        portTextField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                portTextField.setEditable((e.getKeyChar() >= KeyEvent.VK_0 && e.getKeyChar() <= KeyEvent.VK_9)
                        || e.getKeyChar() == KeyEvent.VK_BACK_SPACE || e.getKeyChar() == KeyEvent.VK_DELETE);
            }
        });
        closeButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                System.exit(0);
            }
        });
        launchButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                launchButton.setEnabled(false);
                portTextField.setEnabled(false);
                monitor = new Monitor(Integer.parseInt(portTextField.getText()));
                clusterStatusTable.setModel(monitor.getClusterStatusTableModel());
                Thread listenThread = new Thread(() -> monitor.listen());
                listenThread.start();
            }
        });
    }

    public static void main(String[] args) {
        new MonitorGui();
    }
}
