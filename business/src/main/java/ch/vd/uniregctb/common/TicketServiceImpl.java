package ch.vd.uniregctb.common;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.InstantHelper;

public class TicketServiceImpl implements TicketService {

	private static final AtomicLong SEQUENCE = new AtomicLong(0L);

	/**
	 * Implémentation des tickets présentés à l'extérieur
	 */
	private final class TicketImpl implements Ticket {

		private final long id = SEQUENCE.incrementAndGet();

		@Override
		public void release() {
			synchronized (heldKeys) {
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

	/**
	 * Toutes les clés actuellement connues
	 */
	private final Set<Object> heldKeys = new HashSet<>();

	/**
	 * Les tickets exposés à l'extérieur, et pas encore relâchés (les valeurs sont les clés des ressources)
	 */
	private final Map<Long, Object> tickets = new HashMap<>();

	@NotNull
	@Override
	public Ticket getTicket(Object key, @Nullable Duration timeout) throws TicketTimeoutException, InterruptedException {
		if (key == null) {
			throw new NullPointerException("key");
		}

		final Instant max = timeout == null ? null : InstantHelper.get().plus(timeout);

		synchronized (heldKeys) {
			while (heldKeys.contains(key)) {
				// attendons le temps qu'on peut pour voir si la ressource se libère
				if (max == null) {
					// on attends ce qu'il faudra...
					heldKeys.wait(0L);
				}
				else {
					final Duration remainingTime = Duration.between(InstantHelper.get(), max);
					if (remainingTime.isNegative()) {
						// trop tard...
						throw new TicketTimeoutException();
					}

					// conversion nano-secondes -> millisecondes. Attention ! On perd en précision et on risque de tomber sur un résultat
					// de 0 ms qui veut dire 'attente infinie' : on s'assure donc que l'attente minimale soit de 1 ms au minimum.
					final long waitMillis = Math.max(remainingTime.toMillis(), 1);
					heldKeys.wait(waitMillis);
				}
			}

			final TicketImpl ticket = new TicketImpl();
			heldKeys.add(key);
			tickets.put(ticket.id, key);
			return ticket;
		}
	}
}
