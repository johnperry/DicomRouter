package org.rsna.router;

import java.util.EventListener;

/**
 * The interface for listeners to TransferEvents.
 */
public interface TransferListener extends EventListener {

	public void attention(TransferEvent event);

}
