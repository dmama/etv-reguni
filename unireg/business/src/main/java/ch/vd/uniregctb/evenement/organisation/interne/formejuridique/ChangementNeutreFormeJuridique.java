package ch.vd.uniregctb.evenement.organisation.interne.formejuridique;

import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisation;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationContext;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationException;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationOptions;
import ch.vd.uniregctb.evenement.organisation.audit.EvenementOrganisationErreurCollector;
import ch.vd.uniregctb.evenement.organisation.audit.EvenementOrganisationWarningCollector;
import ch.vd.uniregctb.evenement.organisation.interne.EvenementOrganisationInterne;
import ch.vd.uniregctb.tiers.Entreprise;

import static ch.vd.uniregctb.evenement.fiscal.EvenementFiscalInformationComplementaire.TypeInformationComplementaire;

/**
 * @author Raphaël Marmier, 2015-10-15
 */
public class ChangementNeutreFormeJuridique extends EvenementOrganisationInterne {

	protected ChangementNeutreFormeJuridique(EvenementOrganisation evenement, Organisation organisation,
	                                         Entreprise entreprise, EvenementOrganisationContext context,
	                                         EvenementOrganisationOptions options) throws EvenementOrganisationException {
		super(evenement, organisation, entreprise, context, options);
	}

	@Override
	public void doHandle(EvenementOrganisationWarningCollector warnings) throws EvenementOrganisationException {
		TypeInformationComplementaire type = TypeInformationComplementaire.CHANGEMENT_FORME_JURIDIQUE_MEME_CATEGORIE;
		context.getEvenementFiscalService().publierEvenementFiscalInformationComplementaire(getEntreprise(), type, getDateEvt());
	}

	@Override
	protected void validateSpecific(EvenementOrganisationErreurCollector erreurs, EvenementOrganisationWarningCollector warnings) throws EvenementOrganisationException {

	}
}
