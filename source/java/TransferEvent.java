package org.rsna.router;

import java.awt.AWTEvent;

/**
 * The event that passes a message to TransferListeners.
 */
public class TransferEvent extends AWTEvent {

	public static final int TRANSFER_EVENT = AWTEvent.RESERVED_ID_MAX + 4267 + 32;

	/** The message */
	public String message;

	/**
	 * Class constructor to capture a message for transmission via the event mechanism.
	 * @param source the source of the event.
	 * @param message the message.
	 */
	public TransferEvent(Object source, String message) {
		super(source, TRANSFER_EVENT);
		this.message = message;
	}
}
