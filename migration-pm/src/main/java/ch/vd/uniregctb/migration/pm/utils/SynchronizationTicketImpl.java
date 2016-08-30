package ch.vd.uniregctb.migration.pm.utils;

import java.util.SortedSet;

import org.jetbrains.annotations.Nullable;

/**
 * Implémentation d'un ticket de synchronisation interne
 */
public abstract class SynchronizationTicketImpl<T> implements SynchronizationTicket {

	/**
	 * Collection des données 'tenues' par ce ticket
	 */
	private final SortedSet<T> heldData;

	/**
	 * Ticket à relâcher en même temps que les données 'tenues' ici (en fait, juste après)
	 */
	@Nullable
	private final SynchronizationTicket prerequisiteTicket;

	/**
	 * @param heldData données à marquer comme 'tenues'
	 * @param prerequisiteTicket éventuel ticket obtenu en pré-requis à celui-ci (qui devra être relâché en même temps)
	 */
	public SynchronizationTicketImpl(SortedSet<T> heldData, @Nullable SynchronizationTicket prerequisiteTicket) {
		this.heldData = heldData;
		this.prerequisiteTicket = prerequisiteTicket;
	}

	@Override
	public final void release() {
		release(heldData);
		if (prerequisiteTicket != null) {
			prerequisiteTicket.release();
		}
	}

	/**
	 * Implémentation réelle de la restitution du ticket de synchronisation
	 * @param heldData données 'tenues' à relâcher
	 */
	protected abstract void release(SortedSet<T> heldData);
}
