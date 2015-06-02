package ch.vd.uniregctb.common;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class TicketServiceImpl implements TicketService {

	private static final AtomicLong SEQUENCE = new AtomicLong(0L);

	/**
	 * Implémentation des tickets présentés à l'extérieur
	 */
	private static final class TicketImpl implements Ticket {
		private final long id = SEQUENCE.incrementAndGet();
	}

	/**
	 * Toutes les clés actuellement connues
	 */
	private final Set<Object> heldKeys = new HashSet<>();

	/**
	 * Les tickets exposés à l'extérieur, et pas encore relâchés (les valeurs sont les clés des ressources)
	 */
	private final Map<Long, Object> tickets = new HashMap<>();

	private static long getCurrentTimeMillis() {
		return TimeUnit.NANOSECONDS.toMillis(System.nanoTime());
	}

	@Override
	public Ticket getTicket(Object key, long timeout) throws TicketTimeoutException, InterruptedException {
		if (key == null) {
			throw new NullPointerException("key");
		}

		final long start = getCurrentTimeMillis();
		synchronized (heldKeys) {
			while (heldKeys.contains(key)) {
				// attendons le temps qu'on peut pour voir si la ressource se libère
				final long now = getCurrentTimeMillis();
				if (timeout == 0 || timeout - (now - start) > 0) {
					heldKeys.wait(timeout == 0 ? 0 : timeout - (now - start));
				}
				else {
					// trop tard...
					throw new TicketTimeoutException();
				}
			}

			final TicketImpl ticket = new TicketImpl();
			heldKeys.add(key);
			tickets.put(ticket.id, key);
			return ticket;
		}
	}

	@Override
	public void releaseTicket(Ticket ticket) {
		if (ticket == null) {
			throw new NullPointerException("ticket");
		}
		if (! (ticket instanceof TicketImpl)) {
			throw new IllegalArgumentException("Wrong ticket!");
		}

		synchronized (heldKeys) {
			final Long id = ((TicketImpl) ticket).id;
			final Object key = tickets.remove(id);
			if (key == null) {
				// puisque la clé ne peut pas être nulle à l'insertion dans cette map, une valeur nulle ici signifie
				// que le mapping est absent -> double release ?
				throw new IllegalStateException("Already released!");
			}
			heldKeys.remove(key);
			heldKeys.notifyAll();
		}
	}
}
