package ch.vd.uniregctb.evenement.civil.regpp;

import org.jetbrains.annotations.Nullable;

import ch.vd.uniregctb.evenement.common.EvenementRegistreErreurFactory;
import ch.vd.uniregctb.type.TypeEvenementErreur;

public class EvenementCivilRegPPErreurFactory extends EvenementRegistreErreurFactory<EvenementCivilRegPPErreur> {

	@Override
	protected EvenementCivilRegPPErreur createErreur(String message, @Nullable Exception e, TypeEvenementErreur type) {
		final EvenementCivilRegPPErreur erreur = new EvenementCivilRegPPErreur();
		erreur.setMessage(buildActualMessage(message, e));
		erreur.setType(type);
		erreur.setCallstack(extractCallstack(e));
		return erreur;
	}
}
