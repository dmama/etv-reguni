package ch.vd.uniregctb.evenement.civil.interne;

import ch.vd.uniregctb.type.EtatEvenementCivil;

public enum HandleStatus {
	/**
	 * L'événement a été bien traité et les données d'Unireg ont été modifiées en conséquences.
	 */
	TRAITE {
		@Override
		public EtatEvenementCivil toEtat() {
			return EtatEvenementCivil.TRAITE;
		}
	},
	/**
	 * L'événement a été détecté comme redondant, c'est-à-dire que les données d'Unireg étaient déjà dans l'état voulu.
	 */
	REDONDANT {
		@Override
		public EtatEvenementCivil toEtat() {
			return EtatEvenementCivil.REDONDANT;
		}
	};

	public abstract EtatEvenementCivil toEtat();
}
