package ch.vd.uniregctb.migration.pm.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

public class EntityMigrationSynchronizer {

	/**
	 * Interface de ticket l'on prend ou relâche
	 */
	public static interface Ticket {
	}

	/**
	 * Implémentation du ticket (interne...)
	 */
	private class TicketImpl implements Ticket {

		private final Set<Long> lockedEntrepriseIds;
		private final Set<Long> lockedIndividualIds;

		private TicketImpl(Set<Long> entrepriseIds, Set<Long> individualIds) {
			this.lockedEntrepriseIds = entrepriseIds;
			this.lockedIndividualIds = individualIds;
		}

		private EntityMigrationSynchronizer outer() {
			return EntityMigrationSynchronizer.this;
		}
	}

	/**
	 * Identifiants d'entreprises verrouillés (les accès doivent être protégés par un verrou centralisé)
	 */
	private final Set<Long> lockedEntrepriseIds = new HashSet<>();

	/**
	 * Identifiants d'individus verrouillés (les accès doivent être protégés par un verrou centralisé)
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
	 * @param idsEntreprise les identifiants d'entreprises qui vont être prises en charge par ce thread
	 * @param idsIndividus les identifiants des individus pris en charge par ce thread
	 * @param timeout attente maximale, en millisecondes (0 = infini)
	 * @return le ticket à relâcher plus tard (= accès accordé), ou <code>null</code> (= resource toujours pas accessible après le temps maximal d'attente)
	 * @throws InterruptedException si le thread d'attente a été interrompu
	 */
	public Ticket hold(Collection<Long> idsEntreprise, Collection<Long> idsIndividus, long timeout) throws InterruptedException {

		//
		// il est important d'ordonner les identifiants (= toujours prendre le même) afin d'éviter les deadlocks :
		// 1. d'abord les identifiants d'entreprises, puis les identifiants de personnes
		// 2. dans chacune des catégories, les identifiants sont traités par ordre croissant
		//

		final SortedSet<Long> idsPm = buildSortedSet(idsEntreprise);
		final SortedSet<Long> idsInd = buildSortedSet(idsIndividus);
		final long start = now();

		lock.lock();
		try {
			boolean ok = lockIds(idsPm, lockedEntrepriseIds, timeout);
			if (ok) {
				final long remainingTime;
				if (timeout > 0L) {
					// trop tard ?
					remainingTime = start + timeout - now();
					if (remainingTime <= 0L && !idsInd.isEmpty()) {
						// plus le temps, alors qu'il reste des trucs à faire... -> on abandonne !
						ok = false;
					}
				}
				else {
					remainingTime = 0L;
				}

				ok = ok && lockIds(idsInd, lockedIndividualIds, remainingTime);
				if (!ok) {
					// il faut annuler tous les verrous posés...
					lockedEntrepriseIds.removeAll(idsPm);
				}
			}

			return ok ? new TicketImpl(idsPm, idsInd) : null;
		}
		finally {
			lock.unlock();
		}
	}

	/**
	 * Pose un verrou sur les identifiants donnés par rapport à la collection des verrous posés
	 * @param idsToLock identifiants à verrouiller
	 * @param lockedIds ensemble des identifiants (de même catégorie) verrouillés
	 * @param timeout temps maximal d'attente autorisé
	 * @return <code>true</code> si tous les verrous ont pu être posés, <code>false</code> si la cloche a sonné avant
	 * @throws InterruptedException si le thread d'attente a été interrompu
	 */
	private boolean lockIds(SortedSet<Long> idsToLock, Set<Long> lockedIds, long timeout) throws InterruptedException {
		if (!lock.isHeldByCurrentThread()) {
			throw new IllegalMonitorStateException("Lock is not held by current thread!");
		}

		final List<Long> lockProgression = new ArrayList<>(idsToLock.size());
		final long start = now();
		for (Long id : idsToLock) {
			while (true) {
				final long remainingTime;
				if (timeout > 0L) {
					// trop tard ?
					remainingTime = start + timeout - now();
					if (remainingTime <= 0L) {
						// il faut tout relâcher ce qu'on avait commencé à prendre
						lockedIds.removeAll(lockProgression);
						return false;
					}
				}
				else {
					remainingTime = 0L;
				}
				final boolean locked = lockId(id, lockedIds, remainingTime);
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
	 * @param timeout attente maximale, en millisecondes (0 = infini)
	 * @return <code>true</code> si le lock a été obtenu, <code>false</code> si la cloche a sonné avant
	 * @throws InterruptedException si le thread d'attente a été interrompu
	 */
	private boolean lockId(Long id, Set<Long> lockedIds, long timeout) throws InterruptedException {
		if (!lock.isHeldByCurrentThread()) {
			throw new IllegalMonitorStateException("Lock is not held by current thread!");
		}

		long timeoutNanos = TimeUnit.MILLISECONDS.toNanos(timeout);
		while (lockedIds.contains(id)) {
			if (timeout == 0L) {
				// on attend aussi longtemps qu'il le faut...
				somethingUnlocked.await();
			}
			else if (timeoutNanos <= 0L) {
				// trop tard, c'est fini !
				return false;
			}
			else {
				// on attend le temps qui reste...
				timeoutNanos = somethingUnlocked.awaitNanos(timeoutNanos);
			}
		}

		// on l'a eu !!
		lockedIds.add(id);
		return true;
	}

	/**
	 * Relâche les identifiants d'entreprises pris en charge par le ticket donné
	 * @param ticket ticket qui avait été fourni par l'appel à {@link #hold(java.util.Collection, java.util.Collection, long)}
	 */
	public void release(Ticket ticket) {
		if (ticket == null) {
			throw new NullPointerException("ticket");
		}
		if (!(ticket instanceof TicketImpl) || ((TicketImpl) ticket).outer() != this) {
			throw new IllegalArgumentException("Wrong ticket!");
		}

		lock.lock();
		try {
			final TicketImpl impl = (TicketImpl) ticket;
			lockedEntrepriseIds.removeAll(impl.lockedEntrepriseIds);
			lockedIndividualIds.removeAll(impl.lockedIndividualIds);
			somethingUnlocked.signalAll();
		}
		finally {
			lock.unlock();
		}
	}

	private static long now() {
		return TimeUnit.NANOSECONDS.toMillis(System.nanoTime());
	}

	private static SortedSet<Long> buildSortedSet(Collection<Long> idsEntreprise) {
		if (idsEntreprise == null || idsEntreprise.isEmpty()) {
			return Collections.emptyNavigableSet();
		}

		return idsEntreprise.stream()
				.filter(id -> id != null)
				.collect(Collectors.toCollection(TreeSet::new));
	}
}
