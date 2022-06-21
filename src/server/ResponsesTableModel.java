package server;

import shared.Message;

import javax.swing.table.DefaultTableModel;

public class ResponsesTableModel extends DefaultTableModel {
    public ResponsesTableModel() {
        addColumn("Request Id");
        addColumn("Server Id");
        addColumn("Code");
        addColumn("Number Iterations");
        addColumn("Pi");
        addColumn("Deadline");
    }

    public void addResponse(Message response) {
        addRow(new Object[]{response.getRequestId(), response.getServerId(), response.getCode(), response.getNumberOfIterations(), response.getPi(), response.getDeadline()});
    }
}
