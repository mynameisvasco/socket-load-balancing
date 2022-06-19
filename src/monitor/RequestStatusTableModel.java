package monitor;

import shared.Message;

import javax.swing.table.DefaultTableModel;
import java.net.Socket;

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

    public void addRequest(Message request, Socket client) {
        addRow(new Object[]{request.getRequestId(), request.getClientId(), client.getInetAddress().getAddress(), client.getPort(), "Pending", request.getNumberOfIterations(), request.getDeadline()});
    }

    public void updateRequest(int requestId, String status) {
        for (int i = 0; i < getRowCount(); i++) {
            if (dataVector.get(i).get(0).toString().equals(String.valueOf(requestId))) {
                dataVector.get(i).set(4, status);
            }
        }

        fireTableDataChanged();
    }
}
