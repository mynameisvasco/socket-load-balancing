package client;

import shared.RequestMessage;
import shared.ResponseMessage;

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

    public void addResponse(ResponseMessage response) {
        addRow(new Object[]{response.requestId(), response.serverId(), response.code(), response.numberOfIterations(), response.pi(), response.deadline()});
    }
}
