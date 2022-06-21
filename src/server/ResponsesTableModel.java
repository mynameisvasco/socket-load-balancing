package server;

import shared.Message;
import shared.MessageCodes;

import javax.swing.table.DefaultTableModel;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ResponsesTableModel extends DefaultTableModel {
    public ResponsesTableModel() {
        addColumn("Request Id");
        addColumn("Client Id");
        addColumn("Send time");
        addColumn("Status");
        addColumn("Number Iterations");
        addColumn("Pi");
        addColumn("Deadline (s)");
    }

    @Override
    public boolean isCellEditable(int row, int column) {
        return false;
    }
    public void addResponse(Message response) {
        var formatter = new SimpleDateFormat("HH:mm:ss");
        var date = new Date(System.currentTimeMillis());

        insertRow(0, new Object[]{response.getRequestId(), response.getClientId(), formatter.format(date),
                                       response.getCode() == MessageCodes.PiCalculationResult ?
                                               "Completed" : "Rejected",
                                       response.getNumberOfIterations(),
                                       response.getCode() == MessageCodes.PiCalculationResult ?
                                               response.getPi() : "-", response.getDeadline()});
    }
}
