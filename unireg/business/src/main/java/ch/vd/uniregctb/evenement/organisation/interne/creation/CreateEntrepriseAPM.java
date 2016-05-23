package ch.vd.uniregctb.evenement.organisation.interne.creation;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.Assert;
import ch.vd.unireg.interfaces.organisation.data.FormeLegale;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisation;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationContext;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationException;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationOptions;
import ch.vd.uniregctb.evenement.organisation.audit.EvenementOrganisationErreurCollector;
import ch.vd.uniregctb.evenement.organisation.audit.EvenementOrganisationSuiviCollector;
import ch.vd.uniregctb.evenement.organisation.audit.EvenementOrganisationWarningCollector;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.type.CategorieEntreprise;
import ch.vd.uniregctb.type.GenreImpot;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;

/**
 * Evénement interne de création d'entreprise de catégorie "Personnes morales de droit public" (DP/PM)
 *
 * @author Raphaël Marmier, 2015-09-02
 */
public class CreateEntrepriseAPM extends CreateEntreprise {

	protected CreateEntrepriseAPM(EvenementOrganisation evenement, Organisation organisation, Entreprise entreprise,
	                              EvenementOrganisationContext context,
	                              EvenementOrganisationOptions options,
	                              RegDate dateDeCreation,
	                              boolean isCreation) throws EvenementOrganisationException {
		super(evenement, organisation, entreprise, context, options, dateDeCreation, isCreation);
	}

	@Override
	public String describe() {
		return "Création d'une entreprise de catégorie APM";
	}

	@Override
	public void doHandle(EvenementOrganisationWarningCollector warnings, EvenementOrganisationSuiviCollector suivis) throws EvenementOrganisationException {
		super.doHandle(warnings, suivis);

		MotifFor motifOuverture = determineMotifOuvertureFor(isCreation());

		openForFiscalPrincipal(getDateDeCreation(),
		                       getAutoriteFiscalePrincipale(),
		                       MotifRattachement.DOMICILE,
		                       motifOuverture,
		                       GenreImpot.BENEFICE_CAPITAL,
		                       warnings, suivis);

		// Création du bouclement
		createAddBouclement(getDateDeCreation(), isCreation(), suivis);
		// Ajoute les for secondaires
		adapteForsSecondairesPourEtablissementsVD(getEntreprise(), getDateDeCreation(), warnings, suivis);

		// SIFISC-19335 - mettre à l'état "a vérifier" les annoces de créations d'APM
		warnings.addWarning(String.format("Vérification requise après la création de l'APM n°%s.", FormatNumeroHelper.numeroCTBToDisplay(getEntreprise().getNumero())));
	}

	@Override
	protected void validateSpecific(EvenementOrganisationErreurCollector erreurs, EvenementOrganisationWarningCollector warnings, EvenementOrganisationSuiviCollector suivis) throws EvenementOrganisationException {
		super.validateSpecific(erreurs, warnings, suivis);

		if (getCategory() == null) {
			FormeLegale formeLegale = getOrganisation().getFormeLegale(getDateDeCreation());
			erreurs.addErreur(String.format("Catégorie introuvable pour l'organisation no %d de forme juridique %s, en date du %s.", getOrganisation().getNumeroOrganisation(),
			                                formeLegale != null ? formeLegale : "inconnue", RegDateHelper.dateToDisplayString(getDateDeCreation())));
		}

		// Vérifier qu'on est bien en présence d'un type qu'on supporte.
		Assert.state(getCategory() == CategorieEntreprise.APM, String.format("Catégorie d'entreprise non supportée! %s", getCategory()));
	}
}
