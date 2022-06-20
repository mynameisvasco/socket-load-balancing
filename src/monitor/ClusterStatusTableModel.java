package monitor;

import shared.Message;
import shared.SocketInfo;

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
            if (dataVector.get(i).get(0).toString().equals("Primary LB") &&
                    dataVector.get(i).get(4).toString().equals("UP")) {
                activeLoadBalancerExists = true;
            }
        }

        return activeLoadBalancerExists;
    }

    public void setNumberOfIterations(Message message) {
        int i = 0;

        while (i < getRowCount() &&!(dataVector.get(i).get(1).toString().equals(String.valueOf(message.getServerId())))) {
            i++;
        }

        if(i != getRowCount()) {
            dataVector.get(i).set(5, message.getNumberOfIterations());
            fireTableDataChanged();
        }
    }

    public void addServer(Message request) {
        int i = 0;
        while (i < getRowCount() &&!(dataVector.get(i).get(1).toString().equals(String.valueOf(request.getServerId())))) {
            i++;
        }

        if(i != getRowCount()) {
            dataVector.get(i).set(4, "UP");
            fireTableDataChanged();
        } else {
            addRow(new Object[]{"Server", request.getServerId(), request.getSocketInfo().address(),
                    request.getSocketInfo().port(), "UP", 0});
        }
    }

    public void downServer(int id) {
        int i = 0;
        while (i < getRowCount() &&!(dataVector.get(i).get(1).toString().equals(String.valueOf(id)))) {
            i++;
        }

        dataVector.get(i).set(4, "DOWN");
        dataVector.get(i).set(5, "0");
        fireTableDataChanged();
    }

    public void addLoadbalancer(Message request) {
        int i = 0;

        while (i < getRowCount() &&!(dataVector.get(i).get(1).toString().equals(String.valueOf(request.getServerId())))) {
            i++;
        }

        String type;
        if (activeLoadBalancerExists()) type = "Secondary LB";
        else type = "Primary LB";

        if(i == getRowCount()) {
            addRow(new Object[]{type, request.getServerId(), request.getSocketInfo().address(),
                    request.getSocketInfo().port(), "UP", "-"});
        }
        else {
            dataVector.get(i).set(0, type);
            dataVector.get(i).set(4, "UP");
            fireTableDataChanged();
        }
    }

    public SocketInfo markLoadBalancerDown(int loadBalancerID) {
        int i = 0;

        while (i < getRowCount() &&!(dataVector.get(i).get(1).toString().equals(String.valueOf(loadBalancerID)))) {
            i++;
        }

        dataVector.get(i).set(0, "LB");
        dataVector.get(i).set(4, "DOWN");
        fireTableDataChanged();

        return new SocketInfo(dataVector.get(i).get(2).toString(),
                              Integer.parseInt(dataVector.get(i).get(3).toString()));
    }

    public SocketInfo markLoadBalancerPromotion(SocketInfo crashedLoadBalancerInfo) {
        int i = 0;

       while (i < getRowCount() && !(dataVector.get(i).get(0).toString().equals("Secondary LB") && dataVector.get(i).get(4).toString().equals("UP"))) {
            i++;
        }

        dataVector.get(i).set(0, "Primary LB");
        int loadBalancerToPromotePort = Integer.parseInt(dataVector.get(i).get(3).toString());
        dataVector.get(i).set(3, crashedLoadBalancerInfo.port());
        fireTableDataChanged();
        return new SocketInfo(dataVector.get(i).get(2).toString(), loadBalancerToPromotePort);
    }

    public SocketInfo getInfoById(int id) {
        int i = 0;
        while (i < getRowCount() &&!(dataVector.get(i).get(1).toString().equals(String.valueOf(id)))) {
            i++;
        }
        return new SocketInfo(dataVector.get(i).get(2).toString(),
                              Integer.parseInt(dataVector.get(i).get(3).toString()));
    }
}
