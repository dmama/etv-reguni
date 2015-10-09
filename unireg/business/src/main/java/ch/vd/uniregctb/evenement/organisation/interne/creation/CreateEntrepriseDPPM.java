package ch.vd.uniregctb.evenement.organisation.interne.creation;

import org.springframework.util.Assert;

import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.interfaces.organisation.data.DateRanged;
import ch.vd.unireg.interfaces.organisation.data.FormeLegale;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisation;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationContext;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationException;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationOptions;
import ch.vd.uniregctb.evenement.organisation.audit.EvenementOrganisationErreurCollector;
import ch.vd.uniregctb.evenement.organisation.audit.EvenementOrganisationWarningCollector;
import ch.vd.uniregctb.evenement.organisation.interne.helper.CategorieEntreprise;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;

/**
 * Evénement interne de création d'entreprise de catégorie "Personnes morales de droit public" (DP/PM)
 *
 *  Spécification:
 *  - Ti01SE03-Identifier et traiter les mutations entreprise.doc - Version 0.6 - 08.09.2015
 *
 * @author Raphaël Marmier, 2015-09-02
 */
public class CreateEntrepriseDPPM extends CreateEntrepriseBase {

	protected CreateEntrepriseDPPM(EvenementOrganisation evenement, Organisation organisation, Entreprise entreprise,
	                               EvenementOrganisationContext context,
	                               EvenementOrganisationOptions options) throws EvenementOrganisationException {
		super(evenement, organisation, entreprise, context, options);
	}

	@Override
	public void doHandle(EvenementOrganisationWarningCollector warnings) throws EvenementOrganisationException {
		super.doHandle(warnings);

		MotifFor motifOuverture = determineMotifOuvertureFor();

		openForFiscalPrincipal(getDateDeDebut(),
		                       getAutoriteFiscalePrincipale().getTypeAutoriteFiscale(),
		                       getAutoriteFiscalePrincipale().getNoOfs(),
		                       MotifRattachement.DOMICILE,
		                       motifOuverture,
		                       warnings);

		// Création du bouclement
		createAddBouclement(getDateDeDebut());

		warnings.addWarning("Veuillez vérifier que le traitement automatique de création de l'entreprise donne bien le résultat escompté.");
	}

	@Override
	protected void validateSpecific(EvenementOrganisationErreurCollector erreurs, EvenementOrganisationWarningCollector warnings) throws EvenementOrganisationException {
		super.validateSpecific(erreurs, warnings);

		DateRanged<FormeLegale> formeLegaleRange = DateRangeHelper.rangeAt(getOrganisation().getFormeLegale(), getDateDeDebut());
		if (getCategory() == null) {
			erreurs.addErreur(String.format("Catégorie introuvable pour l'organisation no %s de forme juridique %s, en date du %s.", getOrganisation().getNumeroOrganisation(),
			                                formeLegaleRange != null ? formeLegaleRange.getPayload() : "inconnue", RegDateHelper.dateToDisplayString(getDateDeDebut())));
		}

		Assert.state(getCategory() == CategorieEntreprise.DP_PM, String.format("Catégorie d'entreprise non supportée! %s", getCategory()));

		if (getCapital() == null) {
			erreurs.addErreur(String.format("Création impossible, capital introuvable. %s", getOrganisationDescription()));
		}
	}
}
