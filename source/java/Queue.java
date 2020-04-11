/*---------------------------------------------------------------
*  Copyright 2005 by the Radiological Society of North America
*
*  This source software is released under the terms of the
*  RSNA Public License (http://mirc.rsna.org/rsnapubliclicense)
*----------------------------------------------------------------*/

package org.rsna.router;

import java.awt.Color;
import java.io.*;
import java.util.*;
import org.apache.log4j.Logger;
import org.rsna.ctp.objects.DicomObject;
import org.rsna.ctp.stdstages.dicom.DicomStorageSCU;

/**
 * The router configuration.
 */
public class Queue implements Comparator<Queue>, Comparable {

	static final Logger logger = Logger.getLogger(Queue.class);

	String name;
	int priority;
	String destination;
	DicomStorageSCU scu;
	String script;
	LinkedList<File> queue;
	long timeout = 0;

	/**
	 * Class constructor.
	 * @param name the name of the queue.
	 * @param priority the priority of the queue.
	 * @param destination the destination URL for the queue.
	 * @param script the script to be used to determine whether a DicomObject
	 * matches the criteria for entry into the queue.
	 */
	public Queue(String name,
				 int priority,
				 String destination,
				 String script) throws Exception {
		this.name = name;
		this.priority = priority;
		this.destination = destination;
		this.scu = new DicomStorageSCU(destination, 10000, true, 0, 0, 0, 0);
		this.script = script;
		this.queue = new LinkedList<File>();
		timeout = 0L;
		clear();
	}

	/**
	 * Remove all the entries from the queue. This method
	 * does not delete any of the files in the store.
	 */
	public void clear() {
		queue = new LinkedList<File>();
	}

	/**
	 * Get the name of the queue.
	 * @return the name of the queue.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Get the priority of the queue.
	 * @return the priority of the queue.
	 */
	public int getPriority() {
		return priority;
	}

	/**
	 * Get the destination URL of the queue in String form.
	 * @return the destination URL of the queue.
	 */
	public String getDestination() {
		return destination;
	}

	/**
	 * Get the SCU for storing objects from this queue to the destination.
	 * @return the SCU for storing objects from this queue to the destination.
	 */
	public DicomStorageSCU getDicomStorageSCU() {
		return scu;
	}

	/**
	 * Get the number of objects currently in the queue.
	 * @return the number of objects currently in the queue.
	 */
	public int getSize() {
		return queue.size();
	}

	/**
	 * Get the timeout time for the queue. The timeout is the
	 * next time (in milliseconds since 1970) when the queue
	 * should be activated. When the current time exceeds the
	 * timeout value, the queue is active.
	 * @return the timeout time for the queue.
	 */
	public long getTimeout() {
		return timeout;
	}

	/**
	 * Set the timeout time for the queue.
	 * @param timeout the new timeout time for the queue.
	 */
	public void setTimeout(long timeout) {
		this.timeout = timeout;
	}

	/**
	 * Suspend the queue for a specified length of time. This
	 * method adds delta to the current time to get
	 * the new timeout value and stores it.
	 * @param delta the timeout time increment for the queue.
	 * @return the new timeout time for the queue
	 */
	public long suspend(long delta) {
		timeout = System.currentTimeMillis() + delta;
		return timeout;
	}

	/**
	 * Determine whether the queue is currently suspended
	 * waiting for the timeout to expire.
	 * @return true if the queue is active; false if the
	 * queue is suspended.
	 */
	public boolean isSuspended() {
		if (System.currentTimeMillis() < timeout) return true;
		return false;
	}

	/**
	 * Determine whether the queue currently contains any elements.
	 * @return true if the queue contains elements; false otherwise.
	 */
	public boolean hasElements() {
		return (queue.size() != 0);
	}

	/**
	 * Get the first element in the queue without removing it from the queue.
	 * @return the first element in the queue, or null if the queue is empty.
	 */
	public File getFirstElement() {
		try { return queue.getFirst(); }
		catch (Exception ex) { }
		return null;
	}

	/**
	 * Remove the first element in the queue.
	 * @return the element that was removed, or null if the queue is empty.
	 */
	public File removeFirstElement() {
		try {
			File file = queue.removeFirst();
			StorageSCP.getInstance().dequeue(file);
			return file;
		}
		catch (Exception ex) { }
		return null;
	}

	/**
	 * Remove the first element in the queue and put it at the end of the queue.
	 */
	public void requeueFirstElement() {
		try {
			File file = queue.removeFirst();
			queue.add(file);
		}
		catch (Exception ex) { }
	}

	/**
	 * The Comparable interface, which sorts Queue objects by their
	 * priority fields. Queues are serviced in such a way that
	 * lower priority field integer values are processed first.
	 * @param q the Queue;
	 * @return a negative value if this Queue comes first, 0 if the Queues
	 * are equal in priority, and a positive value if this Queue comes second.
	 */
	public int compareTo(Object q) {
		return this.getPriority() - ((Queue)q).getPriority();
	}

	/**
	 * The compare part of the Comparator interface, which sorts Queue
	 * objects by their priority fields. Queues are serviced in such a way
	 * that lower priority field integer values are processed first.
	 * @param q1 the first Queue;
	 * @param q2 the second Queue.
	 * @return a negative value if q1 comes first, 0 if q1 and q2
	 * are equal in priority, and a positive value if q1 comes second.
	 */
	public int compare(Queue q1, Queue q2) {
		return q1.getPriority() - q2.getPriority();
	}

	/**
	 * Add a file to the list if it matches the criteria
	 * specified in the script. If the file is accepted into the
	 * queue, tell the StorageSCP about it, and then interrupt
	 * the ExportService to indicate that there is something in
	 * in the queue.
	 * @return true if the file was added to the queue; false otherwise.
	 */
	public boolean submit(DicomObject dicomObject) {
		File file = dicomObject.getFile();
		if (dicomObject.matches(script)) {
			queue.add(file);
			StorageSCP.getInstance().enqueue(file);
			return true;
		}
		return false;
	}
}