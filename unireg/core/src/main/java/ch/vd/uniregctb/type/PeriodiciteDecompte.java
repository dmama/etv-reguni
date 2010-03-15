package ch.vd.uniregctb.type;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.NotImplementedException;

/**
 * <!-- begin-user-doc --> Périodicité du décompte IS (listes récapitulatives) <!-- end-user-doc -->
 * Périodicité du décompte IS (listes récapitulatives)
 * Valeurs possibles (à compléter) :
 * - Mensuel
 * - Trimestriel (défaut)
 * - ...
 *
 * Longueur de colonne : 11
 *
 * @uml.annotations
 *     derived_abstraction="platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_TNdu0GHuEdydo47IZ53QMw"
 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_TNdu0GHuEdydo47IZ53QMw"
 */
public enum PeriodiciteDecompte {
	/**
	 * <!-- begin-user-doc --> Détermine une période calée précisemment sur un mois du calendrier grégorien.
	 * <p>
	 * Exemples:
	 * <ul>
	 * <li>du 01.04.2000 au 30.04.2000</li>
	 * <li>du 01.07.2000 au 31.07.2000</li>
	 * <li>...</li>
	 * </ul>
	 * <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_WzjkYGHxEdydo47IZ53QMw"
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
	},
	/**
	 * <!-- begin-user-doc --> Détermine une période calée précisemment sur une année du calendrier grégorien.
	 * <p>
	 * Exemples:
	 * <ul>
	 * <li>du 01.01.2000 au 31.12.2000</li>
	 * <li>du 01.01.2001 au 31.12.2001</li>
	 * <li>du 01.01.2006 au 31.12.2006</li>
	 * <li>...</li>
	 * </ul>
	 * <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_WqiyUGHxEdydo47IZ53QMw"
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
	},
	/**
	 * <!-- begin-user-doc --> Détermine une période calée précisemment sur un trimestre du calendrier grégorien.
	 * <p>
	 * Exemples:
	 * <ul>
	 * <li>du 01.01.2000 au 31.03.2000</li>
	 * <li>du 01.04.2000 au 30.06.2000</li>
	 * <li>du 01.10.2006 au 31.12.2006</li>
	 * <li>...</li>
	 * </ul>
	 * <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_WhZdYGHxEdydo47IZ53QMw"
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
	},
	/**
	 * <!-- begin-user-doc --> Détermine une période calée précisemment sur un semestre du calendrier grégorien.
	 * <p>
	 * Exemples:
	 * <ul>
	 * <li>du 01.01.2000 au 30.06.2000</li>
	 * <li>du 01.07.2000 au 31.12.2000</li>
	 * <li>du 01.01.2006 au 30.06.2006</li>
	 * <li>...</li>
	 * </ul>
	 * <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_WYO6UGHxEdydo47IZ53QMw"
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
	},
	/**
	 * <!-- begin-user-doc -->  <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_D_4L8PZ7EdyDE6gdiHo60A"
	 */
	UNIQUE() {
		@Override
		public RegDate getDebutPeriode(RegDate reference) {
			//TODO (FDE a implementer)
			throw new NotImplementedException();
		}

		@Override
		public RegDate getDebutPeriodeSuivante(RegDate reference) {
			throw new NotImplementedException();
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
	 * @param reference
	 * @return le début de la période suivante
	 */
	public abstract RegDate getDebutPeriodeSuivante(RegDate reference);
}
