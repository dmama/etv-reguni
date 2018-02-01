package ch.vd.unireg.evenement.organisation.interne;

import ch.vd.unireg.type.EtatEvenementOrganisation;

public enum HandleStatus {

	/**
	 * L'événement a été bien traité. N'implique pas nécessairement la modification des données d'Unireg. On devrait
	 * toujours ressortir dans cet état, à moins qu'on ait déterminé que l'on est en redondance.
	 */
	TRAITE(1) {
		@Override
		public EtatEvenementOrganisation toEtat() {
			return EtatEvenementOrganisation.TRAITE;
		}
	},
	/**
	 * L'événement a été détecté comme redondant, c'est-à-dire que les données d'Unireg étaient déjà dans l'état voulu.
	 */
	REDONDANT(0) {
		@Override
		public EtatEvenementOrganisation toEtat() {
			return EtatEvenementOrganisation.REDONDANT;
		}
	};

	private final int index;

	HandleStatus(int index) {
		this.index = index;
	}

	public int getIndex() {
		return index;
	}

	public HandleStatus raiseTo(HandleStatus nouveau) {
		if (nouveau.getIndex() > this.getIndex()) {
			return nouveau;
		}
		return this;
	}

	public abstract EtatEvenementOrganisation toEtat();
}
