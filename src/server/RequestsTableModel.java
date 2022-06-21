package server;

import shared.Message;

import javax.swing.table.DefaultTableModel;

public class RequestsTableModel extends DefaultTableModel {
    public RequestsTableModel() {
        addColumn("Request Id");
        addColumn("Code");
        addColumn("Number Iterations");
        addColumn("Deadline");
    }

    public void addRequest(Message request) {
        addRow(new Object[] {request.getRequestId(), request.getCode().ordinal(), request.getNumberOfIterations(), request.getDeadline()});
    }
}
