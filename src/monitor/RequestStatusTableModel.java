package monitor;

import javax.swing.table.DefaultTableModel;

public class RequestStatusTableModel extends DefaultTableModel {
    public RequestStatusTableModel() {
        addColumn("Request Id");
        addColumn("Client Id");
        addColumn("Client Address");
        addColumn("Client Port");
        addColumn("Status");
        addColumn("Number Iterations");
        addColumn("Deadline");
    }

}
