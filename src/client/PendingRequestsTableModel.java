package client;

import shared.Message;

import javax.swing.table.DefaultTableModel;

public class PendingRequestsTableModel extends DefaultTableModel {
    public PendingRequestsTableModel() {
        addColumn("Request Id");
        addColumn("Number Iterations");
        addColumn("Deadline (s)");
    }

    public void addRequest(Message request) {
        insertRow(0, new Object[] {request.getRequestId(), request.getNumberOfIterations(), request.getDeadline()});
    }

    public void removeRequest(Message request) {
        for (int i = 0; i < getRowCount(); i++) {
            if ((getValueAt(i, 0)).equals(request.getRequestId())) {
                removeRow(i);
            }
        }
    }

    @Override
    public boolean isCellEditable(int row, int column) {
        return false;
    }
}
