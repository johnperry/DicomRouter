package org.rsna.router;

import java.awt.*;
import java.io.*;
import java.util.*;
import javax.swing.event.EventListenerList;
import javax.swing.SwingUtilities;
import org.apache.log4j.Logger;
import org.rsna.ui.FileEvent;
import org.rsna.ui.FileListener;
import org.rsna.ctp.objects.DicomObject;
import org.rsna.ctp.stdstages.dicom.SimpleDicomStorageSCP;

/**
 * An encapsulation of a simple DICOM Storage SCP.
 */
public class StorageSCP implements FileListener {

	static final Logger logger = Logger.getLogger(StorageSCP.class);
	
	static StorageSCP instance = null;
	static File store = new File("store");
	
	SimpleDicomStorageSCP scp;
	EventListenerList transferListenerList;
	int scpport;
	String aetitle;
	File temp;
	Hashtable<String,Integer> countTable;
	
	LogPanel logPanel = LogPanel.getInstance();
	
	public static StorageSCP getInstance() {
		if (instance == null) {
			instance = new StorageSCP();
		}
		return instance;
	}

	/**
	 * Class constructor; creates a new instance of the StorageSCP.
	 * @param config the router configuration, carrying the SCP parameters.
	 * @param store the directory to be used by the SCP for
	 * storage of received DICOM objects.
	 */
	protected StorageSCP() {
		transferListenerList = new EventListenerList();
		store.mkdirs();
		countTable = new Hashtable<String,Integer>();
	}

	/**
	 * Requeue any files in the store directory.
	 * This method is intended to be called on startup
	 * after the system is ready to run and before
	 * this thread has been started. The purpose of
	 * this method is to ensure that files left in the
	 * store when the application was shut down are
	 * transmitted when it is restarted.
	 */
	public void requeue() {
		if (!store.exists()) return;
		File[] files = store.listFiles();
		for (int k=0; k<files.length; k++) {
			if (files[k].isFile()) {
				try {
					String filename = files[k].getName();
					DicomObject dicomObject = new DicomObject(files[k]);
					Queue[] queues = Configuration.getInstance().getQueues();
					for (int i=0; i<queues.length; i++) {
						if (queues[i].submit(dicomObject)) {
							String queueName = queues[i].getName();
							//Log the queue insertion
							logPanel.log(Color.BLUE, "Stored instance queued to "+queueName+":\n"+filename);
							logger.info("Stored instance queued to "+queueName+": " +filename);
							sendTransferEvent("Instance queued");
						}
					}
				}
				catch (Exception ex) {
					logPanel.log(Color.RED, "Stored instance failed to parse:\n"+files[k].getName());
					logger.info("Stored instance failed to parse: " + files[k].getName());
					sendTransferEvent("Stored instance failed to parse");
				}
			}
		}
	}

	/**
	 * Get the number of files in the store directory.
	 * @return the number of files in the store directory
	 * that have not been moved out by the Store.
	 */
	public int getFileCount() {
		if (!store.exists()) return 0;
		File[] files = store.listFiles();
		return files.length;
	}

	/**
	 * Delete all the files in the dicomStoreDir.
	 */
	public void deleteAllFiles() {
		if (!store.exists()) return;
		File[] files = store.listFiles();
		for (int i=0; i<files.length; i++)
			files[i].delete();
	}

	/**
	 * Stop the SCP if it is running, reinitialize the SCP from the
	 * the config object, and restart the SCP.
	 * @return true if the SCP started; false otherwise.
	 */
	public boolean startSCP() {
		if (scp != null) {
			scp.stop();
			logPanel.log(Color.BLACK, aetitle+" stopped on port "+scpport);
			sendTransferEvent(aetitle+" stopped");
		}
		Configuration config = Configuration.getInstance();
		aetitle = config.getStoreAETitle();
		scpport = config.getStorePortInt();
		scp = new SimpleDicomStorageSCP(store, scpport);
		try { scp.start(); }
		catch (Exception e) {
			logPanel.log(Color.RED, aetitle+" failed to start on port "+scpport
						+"\n" + e.getMessage());
			sendTransferEvent(aetitle+" failed to start on port "+scpport);
			scp = null;
			return false;
		}
		scp.addFileListener(this);
		logPanel.log(Color.BLACK, aetitle+" started on port "+scpport);
		sendTransferEvent(aetitle+" started on port "+scpport);
		return true;
	}

	/**
	 * The FileListener implementation; listens for DICOM objects,
	 * parses them to determine whether they are instances or manifests,
	 * and moves them to the appropriate store directory.
	 */
	public synchronized void fileEventOccurred(FileEvent e) {
		File file = e.getFile();
		//Parse the file and submit it to the queues.
		try {
			String callingAET = e.getInfo();
			String filename = file.getName();
			DicomObject dicomObject = new DicomObject(file);
			//Log the file reception.
			logPanel.log(Color.BLUE, "Instance received from "+callingAET+":\n" + filename);
			logger.info("Instance received from "+callingAET+": " + filename);
			sendTransferEvent("Instance received");

			Queue[] queues = Configuration.getInstance().getQueues();
			for (Queue q : queues) {
				if (q.submit(dicomObject)) {
					String queueName = q.getName();
					//Log the queue insertion
					logPanel.log(Color.BLUE, "Stored instance queued to "+queueName+":\n"+filename);
					logger.info("Instance queued to "+queueName+": " + filename);
					sendTransferEvent("Instance queued");
				}
			}
		}
		catch (Exception ex) {
			logPanel.log(Color.RED, "Object received from "+e.getInfo()+":\n"
									+"Object failed to parse: "+file.getName());
			logger.info("Object failed to parse: " + file.getName());
			sendTransferEvent("Object failed to parse");
		}
	}

	/**
	 * Increase the count for a file, indicating that it has been entered into a queue.
	 * No attempt is made to determine whether the file actually exists in the store. If the file
	 * does not exist in the hashtable, an entry is created with the value 1; otherwise, the
	 * existing value is incremented by 1.
	 * @param file the file whose count is to be incremented.
	 */
	public synchronized void enqueue(File file) {
		Integer count = countTable.get(file.getName());
		if (count == null)
			countTable.put(file.getName(), new Integer(1));
		else
			countTable.put(file.getName(), new Integer(count.intValue()+1));
	}

	/**
	 * Decrease the count for a file, indicating that it has been removed from a queue.
	 * If the file does not exist in the table, no action is taken. If the file exists in the
	 * table, its count is decreased by 1. If the count reaches 0, the table entry is removed
	 * and the file is deleted. If the file does not exist in the store when its count reaches
	 * 0, no action is taken.
	 * @param file the file whose count is to be incremented.
	 */
	public synchronized void dequeue(File file) {
		Integer count = countTable.get(file.getName());
		if (count != null) {
			int ct = count.intValue() - 1;
			if (ct > 0)
				countTable.put(file.getName(), new Integer(ct));
			else {
				countTable.remove(file.getName());
				boolean success = file.delete();
				if (success) logger.info("Deleted file from the store: "+file.getName());
				else logger.info("Unable to delete file from the store: "+file.getName());
			}
		}
	}

	/**
	 * Add a TransferListener to the transfer listener list.
	 * @param listener the TransferListener.
	 */
	public void addTransferListener(TransferListener listener) {
		transferListenerList.add(TransferListener.class, listener);
	}

	/**
	 * Remove a TransferListener from the transfer listener list.
	 * @param listener the TransferListener.
	 */
	public void removeTransferListener(TransferListener listener) {
		transferListenerList.remove(TransferListener.class, listener);
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
		final EventListener[] listeners = transferListenerList.getListeners(TransferListener.class);
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