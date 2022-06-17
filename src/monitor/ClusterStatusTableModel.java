package monitor;

import shared.Message;

import javax.swing.table.DefaultTableModel;
import java.net.Socket;

public class ClusterStatusTableModel extends DefaultTableModel {
    public ClusterStatusTableModel() {
        addColumn("Type");
        addColumn("Id");
        addColumn("Address");
        addColumn("Port");
        addColumn("Status");
        addColumn("Number Iterations");
    }

    public boolean activeLoadBalancerExists() {
        boolean activeLoadBalancerExists = false;

        for (int i = 0; i < getRowCount(); i++) {
            if (dataVector.get(i).get(0).toString().equals("Primary LB")) {
                activeLoadBalancerExists = true;
            }
        }

        return activeLoadBalancerExists;
    }

    public void addServer(Message request, Socket client) {
        addRow(new Object[]{"Server", request.getServerId(), client.getInetAddress().getHostAddress(), client.getPort(), "UP", 0});
    }

    public void addLoadbalancer(Message request, Socket client) {
        String type;
        if (activeLoadBalancerExists()) type = "Secondary LB";
        else type = "Primary LB";
        addRow(new Object[]{type, request.getServerId(), client.getInetAddress().getHostAddress(), client.getPort(), "UP", "-"});
    }
}
