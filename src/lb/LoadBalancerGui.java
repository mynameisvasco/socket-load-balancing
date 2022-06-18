package lb;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class LoadBalancerGui extends JFrame {
    private JPanel mainPanel;
    private JTextField loadBalancerPortTextField;
    private JButton launchButton;
    private JTextField monitorIPTextField;
    private JTextField monitorPortTextField;
    private JButton closeButton;
    private JTextField loadBalancerIDTextField;

    private Loadbalancer loadbalancer;

    public LoadBalancerGui() {
        super("Load Balancer");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setContentPane(mainPanel);
        pack();
        setVisible(true);
        launchButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                launchButton.setEnabled(false);
                monitorIPTextField.setEnabled(false);
                monitorPortTextField.setEnabled(false);
                loadBalancerPortTextField.setEnabled(false);
                loadBalancerIDTextField.setEnabled(false);
                loadbalancer = new Loadbalancer(Integer.parseInt(loadBalancerIDTextField.getText().toString()),
                        Integer.parseInt(loadBalancerPortTextField.getText().toString()), monitorIPTextField.getText().toString(),
                        Integer.parseInt(monitorPortTextField.getText().toString()));
                loadbalancer.registerLoadBalancer();
                Thread listenThread = new Thread(() -> loadbalancer.listen());
                listenThread.start();
            }
        });
        closeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                System.exit(0);
            }
        });
        monitorPortTextField.addKeyListener(new KeyAdapter() {
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
        loadBalancerIDTextField.addKeyListener(new KeyAdapter() {
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
        loadBalancerPortTextField.addKeyListener(new KeyAdapter() {
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
    }

    public static void main(String[] args) {
        new LoadBalancerGui();
    }
}
