package server;

import shared.Message;

import javax.swing.table.DefaultTableModel;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.locks.ReentrantLock;

public class ServerStateTableModel extends DefaultTableModel {

    ReentrantLock serverStateUpdateLock;

    public ServerStateTableModel() {
        serverStateUpdateLock = new ReentrantLock();

        addColumn("Slot");
        addColumn("Request Id");
        addColumn("Client Id");
        addColumn("Arrival time");
        addColumn("Number Iterations");
        addColumn("Deadline (s)");

        addRow(new Object[] {"Thread 1", "-", "-", "-", "-", "-"});
        addRow(new Object[] {"Thread 2", "-", "-", "-", "-", "-"});
        addRow(new Object[] {"Thread 3", "-", "-", "-", "-", "-"});
        addRow(new Object[] {"Queue Entry 1", "-", "-", "-", "-", "-"});
        addRow(new Object[] {"Queue Entry 2", "-", "-", "-", "-", "-"});
    }

    @Override
    public boolean isCellEditable(int row, int column) {
        return false;
    }

    public void dequeueResquest(Message request, int threadNr) {

        serverStateUpdateLock.lock();

        var formatter = new SimpleDateFormat("HH:mm:ss");
        var date = new Date(System.currentTimeMillis());

        int i = 0;
        while (i < getRowCount() && !(dataVector.get(i).get(1).toString().equals(String.valueOf(request.getRequestId())))) {
            i++;
        }
        dataVector.get(i).set(1, "-");
        dataVector.get(i).set(2, "-");
        dataVector.get(i).set(3, "-");
        dataVector.get(i).set(4, "-");
        dataVector.get(i).set(5, "-");

        i = 0;
        while (i < getRowCount() && !(dataVector.get(i).get(0).toString().equals(String.format("Thread %d", threadNr)))) {
            i++;
        }
            dataVector.get(i).set(1, request.getRequestId());
            dataVector.get(i).set(2, request.getClientId());
            dataVector.get(i).set(3, formatter.format(date));
            dataVector.get(i).set(4, request.getNumberOfIterations());
            dataVector.get(i).set(5, request.getDeadline());

        fireTableDataChanged();

        serverStateUpdateLock.unlock();
    }

    public void addRequestToQueue(Message request) {

        serverStateUpdateLock.lock();

        var formatter = new SimpleDateFormat("HH:mm:ss");
        var date = new Date(System.currentTimeMillis());

        int queueIndex = 0;
        if (dataVector.get(3).get(1).toString().equals("-")) {
            queueIndex = 3;
        } else if (dataVector.get(4).get(1).toString().equals("-")) {
            queueIndex = 4;
        }

        dataVector.get(queueIndex).set(1, request.getRequestId());
        dataVector.get(queueIndex).set(2, request.getClientId());
        dataVector.get(queueIndex).set(3, formatter.format(date));
        dataVector.get(queueIndex).set(4, request.getNumberOfIterations());
        dataVector.get(queueIndex).set(5, request.getDeadline());

        fireTableDataChanged();

        serverStateUpdateLock.unlock();
    }

    public void addRequest(Message request) {
        var formatter = new SimpleDateFormat("HH:mm:ss");
        var date = new Date(System.currentTimeMillis());

        insertRow(0, new Object[] {"", request.getRequestId(), request.getClientId(), formatter.format(date),
                                               request.getNumberOfIterations(), request.getDeadline()});
    }

    public void removeRequest(Message request) {

        serverStateUpdateLock.lock();

        var formatter = new SimpleDateFormat("HH:mm:ss");
        var date = new Date(System.currentTimeMillis());

        int i = 0;
        while (i < getRowCount() && !(dataVector.get(i).get(1).toString().equals(String.valueOf(request.getRequestId())))) {
            i++;
        }
        dataVector.get(i).set(1, "-");
        dataVector.get(i).set(2, "-");
        dataVector.get(i).set(3, "-");
        dataVector.get(i).set(4, "-");
        dataVector.get(i).set(5, "-");

        fireTableDataChanged();

        serverStateUpdateLock.unlock();
    }
}
