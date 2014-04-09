package ch.vd.uniregctb.type;

import java.util.EnumSet;
import java.util.Set;

import ch.vd.registre.base.date.RegDate;

/**
 * Périodicité du décompte IS (listes récapitulatives)
 * Valeurs possibles (à compléter) :
 * - Mensuel
 * - Trimestriel (défaut)
 * - ...
 *
 * Longueur de colonne : 11
 */
public enum PeriodiciteDecompte {

	/**
	 * Détermine une période calée précisemment sur un mois du calendrier grégorien.
	 * <p>
	 * Exemples:
	 * <ul>
	 * <li>du 01.04.2000 au 30.04.2000</li>
	 * <li>du 01.07.2000 au 31.07.2000</li>
	 * <li>...</li>
	 * </ul>
	 */
	MENSUEL() {
		@Override
		public RegDate getDebutPeriode(RegDate reference) {
			return RegDate.get(reference.year(), reference.month(), 1);
		}

		@Override
		public RegDate getDebutPeriodeSuivante(RegDate reference) {
			return RegDate.get(reference.year(), reference.month(), 1).addMonths(1);
		}

		@Override
		public Set<PeriodiciteDecompte> getShorterPeriodicities() {
			return EnumSet.noneOf(PeriodiciteDecompte.class);
		}
	},

	/**
	 * Détermine une période calée précisemment sur une année du calendrier grégorien.
	 * <p>
	 * Exemples:
	 * <ul>
	 * <li>du 01.01.2000 au 31.12.2000</li>
	 * <li>du 01.01.2001 au 31.12.2001</li>
	 * <li>du 01.01.2006 au 31.12.2006</li>
	 * <li>...</li>
	 * </ul>
	 */
	ANNUEL() {
		@Override
		public RegDate getDebutPeriode(RegDate reference) {
			return RegDate.get(reference.year(), 1, 1);
		}

		@Override
		public RegDate getDebutPeriodeSuivante(RegDate reference) {
			return RegDate.get(reference.year() + 1, 1, 1);
		}

		@Override
		public Set<PeriodiciteDecompte> getShorterPeriodicities() {
			return EnumSet.of(MENSUEL, TRIMESTRIEL, SEMESTRIEL);
		}
	},

	/**
	 * Détermine une période calée précisemment sur un trimestre du calendrier grégorien.
	 * <p>
	 * Exemples:
	 * <ul>
	 * <li>du 01.01.2000 au 31.03.2000</li>
	 * <li>du 01.04.2000 au 30.06.2000</li>
	 * <li>du 01.10.2006 au 31.12.2006</li>
	 * <li>...</li>
	 * </ul>
	 */
	TRIMESTRIEL() {
		@Override
		public RegDate getDebutPeriode(RegDate reference) {
			final int trimestre = ((reference.month() - 1) / 3); // janvier-mars = 0, avril-juin = 1, ...
			return RegDate.get(reference.year(), (trimestre * 3) + 1, 1);
		}

		@Override
		public RegDate getDebutPeriodeSuivante(RegDate reference) {
			return getDebutPeriode(reference).addMonths(3);
		}

		@Override
		public Set<PeriodiciteDecompte> getShorterPeriodicities() {
			return EnumSet.of(MENSUEL);
		}
	},

	/**
	 * Détermine une période calée précisemment sur un semestre du calendrier grégorien.
	 * <p>
	 * Exemples:
	 * <ul>
	 * <li>du 01.01.2000 au 30.06.2000</li>
	 * <li>du 01.07.2000 au 31.12.2000</li>
	 * <li>du 01.01.2006 au 30.06.2006</li>
	 * <li>...</li>
	 * </ul>
	 */
	SEMESTRIEL() {
		@Override
		public RegDate getDebutPeriode(RegDate reference) {
			final int semestre = ((reference.month() - 1) / 6); // janvier-juin = 0, juillet-decembre = 1
			return RegDate.get(reference.year(), (semestre * 6) + 1, 1);
		}

		@Override
		public RegDate getDebutPeriodeSuivante(RegDate reference) {
			return getDebutPeriode(reference).addMonths(6);
		}

		@Override
		public Set<PeriodiciteDecompte> getShorterPeriodicities() {
			return EnumSet.of(MENSUEL, TRIMESTRIEL);
		}
	},

	/**
	 * Périodicité annuelle mais seulement sur une partie de l'année, chaque année
	 */
	UNIQUE() {
		@Override
		public RegDate getDebutPeriode(RegDate reference) {
			// [SIFISC-12027] on est quand-même sur une sorte de périodicité annuelle
			return RegDate.get(reference.year(), 1, 1);
		}

		@Override
		public RegDate getDebutPeriodeSuivante(RegDate reference) {
			// [SIFISC-12027] on est quand-même sur une sorte de périodicité annuelle
			return RegDate.get(reference.year() + 1, 1, 1);
		}

		@Override
		public Set<PeriodiciteDecompte> getShorterPeriodicities() {
			return EnumSet.noneOf(PeriodiciteDecompte.class);
		}
	};

	/**
	 * Calcule la date de début de la période. La période est déterminée par une date de référence située n'importe quand dans la période
	 * considérée.
	 *
	 * @param reference
	 *            la date de référence contenue dans la période considérée
	 * @return le début de la période
	 */
	public abstract RegDate getDebutPeriode(RegDate reference);

	/**
	 * Calcule la date de fin de la période. La période est déterminée par une date de référence située n'importe quand dans la période
	 * considérée.
	 *
	 * @param reference
	 *            la date de référence contenue dans la période considérée
	 * @return la fin de la période
	 */
	public final RegDate getFinPeriode(RegDate reference) {
		return getDebutPeriodeSuivante(reference).addDays(-1);
	}

	/**
	 * Calcule la date de début de la période suivant la période indiquée par la date de référence (c'est le lendemain de la fin
	 * de la période indiquée)
	 * @param reference date de référence
	 * @return le début de la période suivante
	 */
	public abstract RegDate getDebutPeriodeSuivante(RegDate reference);

	/**
	 * Renvoie un ensemble contenant les périodicités plus courtes
	 * @return les périodicités plus courtes
	 */
	public abstract Set<PeriodiciteDecompte> getShorterPeriodicities();
}
