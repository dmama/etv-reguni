package ch.vd.uniregctb.evenement.civil.ech;

import org.jetbrains.annotations.Nullable;

import ch.vd.uniregctb.evenement.civil.EvenementCivilErreurFactory;
import ch.vd.uniregctb.type.TypeEvenementErreur;

public class EvenementCivilEchErreurFactory extends EvenementCivilErreurFactory<EvenementCivilEchErreur> {

	@Override
	protected EvenementCivilEchErreur createErreur(String message, @Nullable Exception e, TypeEvenementErreur type) {
		final EvenementCivilEchErreur erreur = new EvenementCivilEchErreur();
		erreur.setMessage(buildActualMessage(message, e));
		erreur.setType(type);
		erreur.setCallstack(extractCallstack(e));
		return erreur;
	}
}
