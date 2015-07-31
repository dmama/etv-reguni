package ch.vd.uniregctb.evenement.organisation.interne;

import ch.vd.uniregctb.type.EtatEvenementOrganisation;

public enum HandleStatus {
	/**
	 * L'événement a été bien traité et les données d'Unireg ont été modifiées en conséquences.
	 */
	TRAITE {
		@Override
		public EtatEvenementOrganisation toEtat() {
			return EtatEvenementOrganisation.TRAITE;
		}
	},
	/**
	 * L'événement a été détecté comme redondant, c'est-à-dire que les données d'Unireg étaient déjà dans l'état voulu.
	 */
	REDONDANT {
		@Override
		public EtatEvenementOrganisation toEtat() {
			return EtatEvenementOrganisation.REDONDANT;
		}
	};

	public abstract EtatEvenementOrganisation toEtat();
}
