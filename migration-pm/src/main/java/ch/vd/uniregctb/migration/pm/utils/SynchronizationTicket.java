package ch.vd.uniregctb.migration.pm.utils;

/**
 * Ticket de synchronisation
 */
public interface SynchronizationTicket {

	/**
	 * Release the ticket
	 */
	void release();
}
