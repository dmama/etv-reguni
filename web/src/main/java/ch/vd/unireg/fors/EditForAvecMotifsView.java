package ch.vd.uniregctb.fors;

import ch.vd.uniregctb.type.MotifFor;

public interface EditForAvecMotifsView extends EditForView {

	/**
	 * Dans le contexte de l'édition d'un for, selon le même principe que pour la méthode {@link #isDateDebutNulleAutorisee()}
	 * @return si le champ du motif d'ouverture du for peut être accepté à <code>null</code>
	 */
	boolean isMotifDebutNullAutorise();

	MotifFor getMotifDebut();

	MotifFor getMotifFin();
}
