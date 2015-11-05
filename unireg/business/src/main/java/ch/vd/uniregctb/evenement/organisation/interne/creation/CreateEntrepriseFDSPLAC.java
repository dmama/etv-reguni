package ch.vd.uniregctb.evenement.organisation.interne.creation;

import org.springframework.util.Assert;

import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.interfaces.organisation.data.FormeLegale;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisation;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationContext;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationException;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationOptions;
import ch.vd.uniregctb.evenement.organisation.audit.EvenementOrganisationErreurCollector;
import ch.vd.uniregctb.evenement.organisation.audit.EvenementOrganisationWarningCollector;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.type.CategorieEntreprise;

/**
 * Evénement interne de création d'entreprise de catégorie "Fonds de placement" (FDS PLAC)
 *
 *  Spécification:
 *  - Ti01SE03-Identifier et traiter les mutations entreprise.doc - Version 0.6 - 08.09.2015
 *
 * @author Raphaël Marmier, 2015-09-02
 */
public class CreateEntrepriseFDSPLAC extends CreateEntreprise {

	protected CreateEntrepriseFDSPLAC(EvenementOrganisation evenement, Organisation organisation, Entreprise entreprise,
	                                  EvenementOrganisationContext context,
	                                  EvenementOrganisationOptions options) throws EvenementOrganisationException {
		super(evenement, organisation, entreprise, context, options);
	}

	@Override
	public void doHandle(EvenementOrganisationWarningCollector warnings) throws EvenementOrganisationException {
		super.doHandle(warnings);

		warnings.addWarning(MSG_GENERIQUE_A_VERIFIER);
	}

	@Override
	protected void validateSpecific(EvenementOrganisationErreurCollector erreurs, EvenementOrganisationWarningCollector warnings) throws EvenementOrganisationException {
		super.validateSpecific(erreurs, warnings);

		if (getCategory() == null) {
			FormeLegale formeLegale = getOrganisation().getFormeLegale(getDateDeDebut());
			erreurs.addErreur(String.format("Catégorie introuvable pour l'organisation no %s de forme juridique %s, en date du %s.", getOrganisation().getNumeroOrganisation(),
			                                formeLegale != null ? formeLegale : "inconnue", RegDateHelper.dateToDisplayString(getDateDeDebut())));
		}
		Assert.state(getCategory() == CategorieEntreprise.FP, String.format("Catégorie d'entreprise non supportée! %s", getCategory()));
	}
}
