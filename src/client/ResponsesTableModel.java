package client;

import shared.Message;
import shared.MessageCodes;

import javax.swing.table.DefaultTableModel;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ResponsesTableModel extends DefaultTableModel {
    public ResponsesTableModel() {
        addColumn("Request Id");
        addColumn("Server Id");
        addColumn("Status");
        addColumn("Number Iterations");
        addColumn("Pi");
        addColumn("Deadline (s)");
        addColumn("Time");
    }

    public void addResponse(Message response) {
        var formatter = new SimpleDateFormat("yyyy-MM-dd 'at' HH:mm:ss z");
        var date = new Date(System.currentTimeMillis());

        insertRow(0, new Object[]{
                        response.getRequestId(), response.getServerId(),
                        response.getCode() == MessageCodes.PiCalculationResult ? "Completed" : "Rejected",
                        response.getNumberOfIterations(), response.getPi() == 0 ? "-" : response.getPi(),
                        response.getDeadline(),
                        formatter.format(date)
                }
        );
    }

    @Override
    public boolean isCellEditable(int row, int column) {
        return false;
    }
}
