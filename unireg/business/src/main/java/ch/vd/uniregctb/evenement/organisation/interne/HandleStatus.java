package ch.vd.uniregctb.evenement.organisation.interne;

import ch.vd.uniregctb.type.EtatEvenementOrganisation;

public enum HandleStatus {

	/**
	 * L'événement a été bien traité et les données d'Unireg ont été modifiées en conséquences.
	 */
	TRAITE(1) {
		@Override
		public EtatEvenementOrganisation toEtat() {
			return EtatEvenementOrganisation.TRAITE;
		}
	},
	/**
	 * L'événement a été bien traité et les données d'Unireg ont été modifiées en conséquences. Mais
	 * une vérification par un opérateur est nécessaire.
	 */
	A_VERIFIER(2) {
		@Override
		public EtatEvenementOrganisation toEtat() {
			return EtatEvenementOrganisation.A_VERIFIER;
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

	private int index;

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
