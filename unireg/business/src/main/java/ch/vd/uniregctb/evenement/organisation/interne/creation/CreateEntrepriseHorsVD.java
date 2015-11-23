package ch.vd.uniregctb.evenement.organisation.interne.creation;

import org.springframework.util.Assert;

import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisation;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationContext;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationException;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationOptions;
import ch.vd.uniregctb.evenement.organisation.audit.EvenementOrganisationErreurCollector;
import ch.vd.uniregctb.evenement.organisation.audit.EvenementOrganisationSuiviCollector;
import ch.vd.uniregctb.evenement.organisation.audit.EvenementOrganisationWarningCollector;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.type.CategorieEntreprise;

/**
 * Evénement interne de création d'entreprises dont le siège principal est hors VD
 *
 *  Spécification:
 *  - Ti01SE03-Identifier et traiter les mutations entreprise.doc - Version 0.6 - 08.09.2015
 *
 * @author Raphaël Marmier, 2015-09-02
 */
public class CreateEntrepriseHorsVD extends CreateEntreprise {

	protected CreateEntrepriseHorsVD(EvenementOrganisation evenement, Organisation organisation, Entreprise entreprise,
	                                 EvenementOrganisationContext context,
	                                 EvenementOrganisationOptions options) throws EvenementOrganisationException {
		super(evenement, organisation, entreprise, context, options);
	}

	@Override
	public void doHandle(EvenementOrganisationWarningCollector warnings, EvenementOrganisationSuiviCollector suivis) throws EvenementOrganisationException {
		super.doHandle(warnings, suivis);
	}

	@Override
	protected void validateSpecific(EvenementOrganisationErreurCollector erreurs, EvenementOrganisationWarningCollector warnings) throws EvenementOrganisationException {
		super.validateSpecific(erreurs, warnings);

		if (getCategory() != null) {
			Assert.state(getCategory() != CategorieEntreprise.PP, String.format("Catégorie d'entreprise non supportée! %s", getCategory()));
		}
	}
}
