package ch.vd.unireg.evenement.organisation;

import org.jetbrains.annotations.Nullable;

import ch.vd.unireg.evenement.common.EvenementRegistreErreurFactory;
import ch.vd.unireg.type.TypeEvenementErreur;

public class EvenementOrganisationErreurFactory extends EvenementRegistreErreurFactory<EvenementOrganisationErreur> {

	@Override
	protected EvenementOrganisationErreur createErreur(String message, @Nullable Exception e, TypeEvenementErreur type) {
		final EvenementOrganisationErreur erreur = new EvenementOrganisationErreur();
		erreur.setMessage(buildActualMessage(message, e));
		erreur.setType(type);
		erreur.setCallstack(extractCallstack(e));
		return erreur;
	}
}
