package ch.vd.uniregctb.common;

/**
 * Manager général de "tickets" applicatifs, qui peuvent servir par exemple à gérer des sections critiques au niveau applicatif (on ne devrait
 * pas pouvoir être plusieurs à générer des LR/DI pour un même tiers en même temps, par exemple...)
 */
public interface TicketService {

	/**
	 * Interface des tickets renvoyés.
	 * L'utilisation de l'interface {@link java.lang.AutoCloseable} permet d'utiliser ces tickets dans des constructions try-with-resource
	 */
	static interface Ticket {
	}

	/**
	 * Asks for a ticket corresponding to the given key
	 * @param key key identifying the action to get a ticket for
	 * @param timeout maximal time (in milliseconds) to wait for the ticket (0 means "wait forever if necessary")
	 * @return a ticket granting access to the action identified by the given key
	 * @throws ch.vd.uniregctb.common.TicketTimeoutException if no access could be granted in the given timeframe
	 * @throws java.lang.InterruptedException in case the thread is interrupted during wait
	 * @throws java.lang.NullPointerException if the key is <code>null</code>
	 */
	Ticket getTicket(Object key, long timeout) throws TicketTimeoutException, InterruptedException;

	/**
	 * Release the ticket gotten from the {@link #getTicket(Object, long)} method
	 * @param ticket the ticket to release
	 */
	void releaseTicket(Ticket ticket);
}
