package org.rsna.router;

import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.*;
import javax.swing.event.*;
import javax.swing.SwingUtilities;
import org.apache.log4j.Logger;
import org.rsna.ctp.pipeline.Status;
import org.rsna.ctp.objects.DicomObject;
import org.rsna.ctp.stdstages.dicom.DicomStorageSCU;
import org.rsna.ctp.pipeline.Status;

/**
 * The Thread that services the export queues and transmits DicomObjects.
 */
public class ExportService extends Thread {

	static final Logger logger = Logger.getLogger(ExportService.class);
	
	static ExportService instance = null;

	Configuration config;
	EventListenerList listenerList;
	LogPanel logPanel = LogPanel.getInstance();
	
	public static ExportService getInstance() {
		if (instance == null) {
			instance = new ExportService();
		}
		return instance;
	}

	protected ExportService() {
		this.config = Configuration.getInstance();
		listenerList = new EventListenerList();
	}

	/**
	 * The Runnable implementation; starts the thread, polls the
	 * export queues and exports files when they appear.
	 */
	public void run() {
		logPanel.log(Color.BLACK, "Export Service started");
		sendTransferEvent("Export Service started");
		while (true) {
			try {
				exportFiles();
				if (!interrupted()) sleep(2000);
			}
			catch (Exception ignore) { }
		}
	}

	//Process all the files in the queues.
	private void exportFiles() {
		Queue queue;
		Queue[] queues = config.getQueues();
		while ((queue = getQueue(queues)) != null) {
			File file = queue.getFirstElement();
			if (!file.exists()) {
				queue.removeFirstElement();
			}
			else {
				Status status = dicomExport(file, queue.getDicomStorageSCU());
				if (status.equals(Status.OK)) {
					logPanel.log(Color.BLACK, queue.getName()+": export successful:\n"+file.getName());
					logger.info(queue.getName()+": export successful: "+file.getName());
					sendTransferEvent(queue.getName()+": export successful");
					queue.removeFirstElement();
				}
				else {
					//If we get here, the transmission failed.
					//There is no easy way to know why, but we know
					//that the DicomObject parsed because it wouldn't
					//have gotten into the queue if it didn't. The
					//safe thing to do is to move the element to the
					//end of the queue, suspend the queue for one second
					//and try again.
					queue.suspend(1000L);
					queue.requeueFirstElement();
					logPanel.log(Color.RED, queue.getName()+": "+status.toString()+": "+file.getName());
					logger.warn(queue.getName()+": "+status.toString()+": "+file.getName());
					sendTransferEvent(queue.getName()+": transmission failure");
				}
			}
			yield();
		}
	}

	//Find the first active, non-empty queue
	private Queue getQueue(Queue[] queues) {
		for (Queue q : queues) {
			if (!q.isSuspended() && q.hasElements()) return q;
		}
		return null;
	}

	//Export one file using the DICOM protocol.
	private Status dicomExport(File file, DicomStorageSCU scu) {
		Status status = Status.FAIL;
		try { status = scu.send(file); }
		catch (Exception e) {
			logger.warn(
				"DicomSend Exception: (" + e.getClass().getName() + "): "+
				((e.getMessage() != null) ? e.getMessage() : "[no error message]")
			);
		}
		return status;
	}

	/**
	 * Add a TransferListener to the listener list.
	 * @param listener the TransferListener.
	 */
	public void addTransferListener(TransferListener listener) {
		listenerList.add(TransferListener.class, listener);
	}

	/**
	 * Remove a TransferListener from the listener list.
	 * @param listener the TransferListener.
	 */
	public void removeTransferListener(TransferListener listener) {
		listenerList.remove(TransferListener.class, listener);
	}

	//Send a message via a TransferEvent to all TransferListeners.
	private void sendTransferEvent(String message) {
		sendTransferEvent(this,message);
	}

	//Send a TransferEvent to all TransferListeners.
	private void sendTransferEvent(TransferEvent event) {
		sendTransferEvent(this,event.message);
	}

	//Send a TransferEvent to all TransferListeners.
	//The event is sent in the event thread to make it safe for
	//GUI components.
	private void sendTransferEvent(Object object, String message) {
		final TransferEvent event = new TransferEvent(object,message);
		final EventListener[] listeners = listenerList.getListeners(TransferListener.class);
		Runnable fireEvents = new Runnable() {
			public void run() {
				for (int i=0; i<listeners.length; i++) {
					((TransferListener)listeners[i]).attention(event);
				}
			}
		};
		SwingUtilities.invokeLater(fireEvents);
	}

}