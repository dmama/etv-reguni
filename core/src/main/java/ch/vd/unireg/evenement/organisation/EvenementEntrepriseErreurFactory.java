package ch.vd.unireg.evenement.organisation;

import org.jetbrains.annotations.Nullable;

import ch.vd.unireg.evenement.common.EvenementRegistreErreurFactory;
import ch.vd.unireg.type.TypeEvenementErreur;

public class EvenementEntrepriseErreurFactory extends EvenementRegistreErreurFactory<EvenementEntrepriseErreur> {

	@Override
	protected EvenementEntrepriseErreur createErreur(String message, @Nullable Exception e, TypeEvenementErreur type) {
		final EvenementEntrepriseErreur erreur = new EvenementEntrepriseErreur();
		erreur.setMessage(buildActualMessage(message, e));
		erreur.setType(type);
		erreur.setCallstack(extractCallstack(e));
		return erreur;
	}
}
