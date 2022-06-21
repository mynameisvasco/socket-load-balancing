package monitor;

import shared.Message;

import javax.swing.table.DefaultTableModel;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;

public class RequestStatusTableModel extends DefaultTableModel {
    public RequestStatusTableModel() {
        addColumn("Request Id");
        addColumn("Client Id");
        addColumn("Client Address");
        addColumn("Status");
        addColumn("Time");
        addColumn("Number Iterations");
        addColumn("Deadline (s)");
    }

    @Override
    public boolean isCellEditable(int row, int column) {
        return false;
    }

    public void addRequest(Message request, Socket client) {
        var formatter = new SimpleDateFormat("HH:mm:ss");
        var date = new Date(System.currentTimeMillis());

        insertRow(0, new Object[]{request.getRequestId(), request.getClientId(),
                client.getLocalAddress().toString(), "Pending", formatter.format(date), request.getNumberOfIterations(),
                request.getDeadline()});
    }

    public void updateRequest(int requestId, String status) {
        for (int i = 0; i < getRowCount(); i++) {
            if (dataVector.get(i).get(0).toString().equals(String.valueOf(requestId))) {
                dataVector.get(i).set(3, status);
            }
        }

        fireTableDataChanged();
    }
}
