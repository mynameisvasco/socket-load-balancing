package client;

import shared.RequestMessage;

import javax.swing.table.DefaultTableModel;

public class PendingRequestsTableModel extends DefaultTableModel {
    public PendingRequestsTableModel() {
        addColumn("Request Id");
        addColumn("Code");
        addColumn("Number Iterations");
        addColumn("Deadline");
    }

    public void addRequest(RequestMessage request) {
        addRow(new Object[] {request.requestId(), request.code(), request.numberOfIterations(), request.deadline()});
    }
}
