package ch.vd.uniregctb.evenement.organisation.interne.transformation;

import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisation;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationContext;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationException;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationOptions;
import ch.vd.uniregctb.evenement.organisation.audit.EvenementOrganisationErreurCollector;
import ch.vd.uniregctb.evenement.organisation.audit.EvenementOrganisationSuiviCollector;
import ch.vd.uniregctb.evenement.organisation.audit.EvenementOrganisationWarningCollector;
import ch.vd.uniregctb.evenement.organisation.interne.EvenementOrganisationInterneDeTraitement;
import ch.vd.uniregctb.evenement.organisation.interne.HandleStatus;
import ch.vd.uniregctb.tiers.Entreprise;

/**
 * @author Raphaël Marmier, 2016-02-19
 */
public class Scission extends EvenementOrganisationInterneDeTraitement {

	protected Scission(EvenementOrganisation evenement, Organisation organisation,
	                   Entreprise entreprise, EvenementOrganisationContext context,
	                   EvenementOrganisationOptions options) throws EvenementOrganisationException {
		super(evenement, organisation, entreprise, context, options);
	}

	@Override
	public String describe() {
		return "Scission";
	}


	@Override
	public void doHandle(EvenementOrganisationWarningCollector warnings, EvenementOrganisationSuiviCollector suivis) throws EvenementOrganisationException {
		warnings.addWarning("Une vérification, pouvant aboutir à un traitement manuel (processus complexe), est requise pour cause de Scission de l'entreprise.");
		raiseStatusTo(HandleStatus.TRAITE);
	}

	@Override
	protected void validateSpecific(EvenementOrganisationErreurCollector erreurs, EvenementOrganisationWarningCollector warnings, EvenementOrganisationSuiviCollector suivis) throws EvenementOrganisationException {
	}
}
