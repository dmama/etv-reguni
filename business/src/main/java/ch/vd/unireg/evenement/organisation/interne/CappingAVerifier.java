package ch.vd.unireg.evenement.organisation.interne;

import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.unireg.evenement.organisation.EvenementOrganisation;
import ch.vd.unireg.evenement.organisation.EvenementOrganisationContext;
import ch.vd.unireg.evenement.organisation.EvenementOrganisationException;
import ch.vd.unireg.evenement.organisation.EvenementOrganisationOptions;
import ch.vd.unireg.evenement.organisation.audit.EvenementOrganisationErreurCollector;
import ch.vd.unireg.evenement.organisation.audit.EvenementOrganisationSuiviCollector;
import ch.vd.unireg.evenement.organisation.audit.EvenementOrganisationWarningCollector;
import ch.vd.unireg.tiers.Entreprise;

/**
 * Capping de l'état final de l'événement organisation à l'état {@link ch.vd.unireg.type.EtatEvenementOrganisation#A_VERIFIER A_VERIFIER}
 */
public class CappingAVerifier extends EvenementOrganisationInterneDeTraitement {

	public CappingAVerifier(EvenementOrganisation evenement, Organisation organisation, Entreprise entreprise,
	                        EvenementOrganisationContext context, EvenementOrganisationOptions options) {
		super(evenement, organisation, entreprise, context, options);
	}

	@Override
	public String describe() {
		return null;        // On ne veut pas de message descriptif sur cet événement qui n'en est pas un.
	}

	@Override
	protected void validateSpecific(EvenementOrganisationErreurCollector erreurs, EvenementOrganisationWarningCollector warnings, EvenementOrganisationSuiviCollector suivis) throws EvenementOrganisationException {
		// rien de spécial à faire
	}

	@Override
	public void doHandle(EvenementOrganisationWarningCollector warnings, EvenementOrganisationSuiviCollector suivis) throws EvenementOrganisationException {
		// on émet un warning pour faire passer l'événement dans cet état de toute façon
		// (rien à faire s'il y a déjà des warnings, qui placeraient déjà l'événement dans cet état...)
		if (!warnings.hasWarnings()) {
			warnings.addWarning("Evénement explicitement placé 'à vérifier' par configuration applicative.");
		}
	}
}
