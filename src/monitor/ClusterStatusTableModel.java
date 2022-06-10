package monitor;

import javax.swing.table.DefaultTableModel;

public class ClusterStatusTableModel extends DefaultTableModel {
    public ClusterStatusTableModel() {
        addColumn("Type");
        addColumn("Id");
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
}
