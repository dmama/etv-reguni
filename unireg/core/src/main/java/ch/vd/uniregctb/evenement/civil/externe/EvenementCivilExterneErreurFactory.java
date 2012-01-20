package ch.vd.uniregctb.evenement.civil.externe;

import org.jetbrains.annotations.Nullable;

import ch.vd.uniregctb.evenement.civil.EvenementCivilErreurFactory;
import ch.vd.uniregctb.type.TypeEvenementErreur;

public class EvenementCivilExterneErreurFactory extends EvenementCivilErreurFactory<EvenementCivilExterneErreur> {

	@Override
	protected EvenementCivilExterneErreur createErreur(String message, @Nullable Exception e, TypeEvenementErreur type) {
		final EvenementCivilExterneErreur erreur = new EvenementCivilExterneErreur();
		erreur.setMessage(buildActualMessage(message, e));
		erreur.setType(type);
		erreur.setCallstack(extractCallstack(e));
		return erreur;
	}
}
