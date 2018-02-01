package ch.vd.unireg.common;

import java.time.Duration;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Manager général de "tickets" applicatifs, qui peuvent servir par exemple à gérer des sections critiques au niveau applicatif (on ne devrait
 * pas pouvoir être plusieurs à générer des LR/DI pour un même tiers en même temps, par exemple...)
 */
public interface TicketService {

	/**
	 * Interface des tickets renvoyés par la méthode {@link #getTicket(Object, Duration)}
	 */
	interface Ticket {
		/**
		 * Release the ticket
		 */
		void release();
	}

	/**
	 * Asks for a ticket corresponding to the given key
	 * @param key key identifying the action to get a ticket for
	 * @param timeout maximal time to wait for the ticket (<code>null</code> means "wait forever if necessary", 0 or negative means "do not wait")
	 * @return a ticket granting access to the action identified by the given key
	 * @throws ch.vd.unireg.common.TicketTimeoutException if no access could be granted in the given timeframe
	 * @throws java.lang.InterruptedException in case the thread is interrupted during wait
	 * @throws java.lang.NullPointerException if the key is <code>null</code>
	 */
	@NotNull
	Ticket getTicket(Object key, @Nullable Duration timeout) throws TicketTimeoutException, InterruptedException;
}
