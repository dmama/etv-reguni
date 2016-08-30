package ch.vd.uniregctb.migration.pm.utils;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import org.jetbrains.annotations.Nullable;

public class EntityMigrationSynchronizer {

	/**
	 * Ticket interne qui gère un ensemble d'identifiants verrouillés
	 */
	private abstract class SlaveSynchronizationTicket extends SynchronizationTicketImpl<Long> {

		private final Set<Long> lockedIds;

		protected SlaveSynchronizationTicket(SortedSet<Long> heldData, Set<Long> lockedIds, @Nullable SynchronizationTicket prerequisiteTicket) {
			super(heldData, prerequisiteTicket);
			this.lockedIds = lockedIds;
		}

		@Override
		protected void release(SortedSet<Long> heldData) {
			releaseIds(heldData, lockedIds);
		}
	}

	/**
	 * Variante du {@link SlaveSynchronizationTicket} pour le niveau des identifiants d'entreprise
	 */
	private final class EntrepriseIdsSychronizationTicket extends SlaveSynchronizationTicket {
		public EntrepriseIdsSychronizationTicket(SortedSet<Long> heldData, @Nullable SynchronizationTicket prerequisiteTicket) {
			super(heldData, lockedEntrepriseIds, prerequisiteTicket);
		}
	}

	/**
	 * Variante du {@link SlaveSynchronizationTicket} pour le niveau des identifiants d'individu PM
	 */
	private final class IndividualIdsSynchronizationTicket extends SlaveSynchronizationTicket {
		public IndividualIdsSynchronizationTicket(SortedSet<Long> heldData, @Nullable SynchronizationTicket prerequisiteTicket) {
			super(heldData, lockedIndividualIds, prerequisiteTicket);
		}
	}

	/**
	 * Implémentation du ticket effectivement exposé à l'extérieur (il gère un verrou global pendant la
	 * phase de restitution du ticket)
	 */
	private final class MasterSynchronizationTicket implements SynchronizationTicket {

		private final SynchronizationTicket ticket;

		public MasterSynchronizationTicket(SynchronizationTicket ticket) {
			this.ticket = ticket;
		}

		@Override
		public void release() {
			lock.lock();
			try {
				ticket.release();
				somethingUnlocked.signalAll();
			}
			finally {
				lock.unlock();
			}
		}
	}

	/**
	 * Identifiants d'entreprises verrouillés (dont l'accès doit être protégé par un verrou centralisé)
	 */
	private final Set<Long> lockedEntrepriseIds = new HashSet<>();

	/**
	 * Identifiants d'individus verrouillés (dont l'accès doit être protégé par un verrou centralisé)
	 */
	private final Set<Long> lockedIndividualIds = new HashSet<>();

	/**
	 * Verrou de synchronisation sur les ensembles d'identifiants
	 */
	private final ReentrantLock lock = new ReentrantLock();

	/**
	 * Condition levée quand des resources sont libérées (pour permettre à ceux qui attendent de se réveiller)
	 */
	private final Condition somethingUnlocked = lock.newCondition();

	/**
	 * Dans la procédure de pose d'un verrou, il peut y avoir plusieurs étapes, dont chacune est représentée
	 * par une instance de {@link SynchronizationStage}
	 * @param <T> type des données synchronisées
	 */
	private static class SynchronizationStage<T> {

		/**
		 * Données pour lesquelles l'étape de pose de verrou demande l'exclusivité
		 * (= ce qui constituera le coeur du ticket)
		 */
		private final SortedSet<T> requestedIds;

		/**
		 * L'ensemble des données verrouillées dans le système
		 */
		private final Set<T> lockedIds;

		/**
		 * Une fonction de construction du ticket pour cette étape
		 */
		private final BiFunction<SortedSet<T>, SynchronizationTicket, SynchronizationTicket> ticketGenerator;

		public SynchronizationStage(SortedSet<T> requestedIds,
		                            Set<T> lockedIds,
		                            BiFunction<SortedSet<T>, SynchronizationTicket, SynchronizationTicket> ticketGenerator) {
			this.requestedIds = requestedIds;
			this.lockedIds = lockedIds;
			this.ticketGenerator = ticketGenerator;
		}
	}

	/**
	 * @param idsEntreprise les identifiants d'entreprises qui vont être prises en charge par ce thread
	 * @param idsIndividus les identifiants des individus pris en charge par ce thread
	 * @param timeout attente maximale
	 * @return le ticket à relâcher plus tard (= accès accordé), ou <code>null</code> (= resource toujours pas accessible après le temps maximal d'attente)
	 * @throws InterruptedException si le thread d'attente a été interrompu
	 */
	public SynchronizationTicket hold(Collection<Long> idsEntreprise, Collection<Long> idsIndividus, Duration timeout) throws InterruptedException {

		//
		// il est important d'ordonner les identifiants (= toujours prendre le même) afin d'éviter les deadlocks :
		// 1. d'abord les identifiants d'entreprises, puis les identifiants de personnes
		// 2. dans chacune des catégories, les identifiants sont traités par ordre croissant
		//

		final Instant timeoutExpiration = Instant.now().plus(timeout);
		final List<SynchronizationStage<Long>> stages = Arrays.asList(new SynchronizationStage<>(buildSortedSet(idsEntreprise), lockedEntrepriseIds, EntrepriseIdsSychronizationTicket::new),
		                                                              new SynchronizationStage<>(buildSortedSet(idsIndividus), lockedIndividualIds, IndividualIdsSynchronizationTicket::new));

		SynchronizationTicket ticket = null;
		lock.lock();
		try {
			// plusieurs phases, toujours dans le même ordre
			for (SynchronizationStage<Long> stage : stages) {
				final boolean ok = lockIds(stage.requestedIds, stage.lockedIds, timeoutExpiration);
				if (ok) {
					// génération du ticket à ce niveau...
					ticket = stage.ticketGenerator.apply(stage.requestedIds, ticket);
				}
				else {
					// c'est mort, l'accès exclusif sur ces identifiants n'est pas accessible dans le temps imparti !

					// on relâche tout ceux qu'on avait commencé à prendre...
					if (ticket != null) {
						ticket.release();
					}

					// .. et on abandonne (peut-être la prochaine fois ?)
					return null;
				}
			}

			// c'est tout bon, faites-en bon usage !
			return new MasterSynchronizationTicket(ticket);
		}
		catch (InterruptedException | RuntimeException e) {

			// on relâche tout ce qu'on avait commencé à prendre...
			if (ticket != null) {
				ticket.release();
			}

			throw e;
		}
		finally {
			lock.unlock();
		}
	}

	/**
	 * Pose un verrou sur les identifiants donnés par rapport à la collection des verrous posés
	 * @param idsToLock identifiants à verrouiller
	 * @param lockedIds ensemble des identifiants (de même catégorie) verrouillés
	 * @param timeoutExpiration date ultime pour répondre
	 * @return <code>true</code> si tous les verrous ont pu être posés, <code>false</code> si la cloche a sonné avant
	 * @throws InterruptedException si le thread d'attente a été interrompu
	 */
	private boolean lockIds(SortedSet<Long> idsToLock, Set<Long> lockedIds, Instant timeoutExpiration) throws InterruptedException {
		if (!lock.isHeldByCurrentThread()) {
			throw new IllegalMonitorStateException("Lock is not held by current thread!");
		}

		final List<Long> lockProgression = new ArrayList<>(idsToLock.size());
		for (Long id : idsToLock) {
			while (true) {
				final Duration remainingTime = Duration.between(Instant.now(), timeoutExpiration);
				if (remainingTime.isNegative()) {
					// il faut tout relâcher ce qu'on avait commencé à prendre
					lockedIds.removeAll(lockProgression);
					return false;
				}
				final boolean locked = lockId(id, lockedIds, timeoutExpiration);
				if (locked) {
					lockProgression.add(id);
					break;
				}
			}
		}

		// c'est bon, on a tout le monde...
		return true;
	}

	/**
	 * @param id l'indentifiant d'une entreprise qui va être prise en charge
	 * @param lockedIds ensemble des identifiants (de même catégorie) verrouillés
	 * @param timeoutExpiration date ultime pour répondre
	 * @return <code>true</code> si le lock a été obtenu, <code>false</code> si la cloche a sonné avant
	 * @throws InterruptedException si le thread d'attente a été interrompu
	 */
	private boolean lockId(Long id, Set<Long> lockedIds, Instant timeoutExpiration) throws InterruptedException {
		if (!lock.isHeldByCurrentThread()) {
			throw new IllegalMonitorStateException("Lock is not held by current thread!");
		}

		while (lockedIds.contains(id)) {
			final Duration remainingTime = Duration.between(Instant.now(), timeoutExpiration);
			if (remainingTime.isNegative() || remainingTime.isZero()) {
				// trop tard, c'est fini !
				return false;
			}
			else {
				// on attend le temps qui reste...
				somethingUnlocked.awaitNanos(remainingTime.toNanos());
			}
		}

		// on l'a eu !!
		lockedIds.add(id);
		return true;
	}

	private void releaseIds(SortedSet<Long> toRelease, Set<Long> lockedIds) {
		if (!lock.isHeldByCurrentThread()) {
			throw new IllegalMonitorStateException("Lock is not held by current thread!");
		}
		lockedIds.removeAll(toRelease);
	}

	private static SortedSet<Long> buildSortedSet(Collection<Long> ids) {
		if (ids == null || ids.isEmpty()) {
			return Collections.emptyNavigableSet();
		}
		if (ids instanceof SortedSet) {
			return (SortedSet<Long>) ids;
		}

		return ids.stream()
				.filter(Objects::nonNull)
				.collect(Collectors.toCollection(TreeSet::new));
	}
}
