package ch.vd.uniregctb.evenement.organisation.interne.creation;

import org.springframework.util.Assert;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.interfaces.organisation.data.Domicile;
import ch.vd.unireg.interfaces.organisation.data.FormeLegale;
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
import ch.vd.uniregctb.type.GenreImpot;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;

/**
 * Evénement interne de création d'entreprise de catégorie "Personnes morales de droit public" (DP/PM)
 *
 * @author Raphaël Marmier, 2015-09-02
 */
public class CreateEntreprisePM extends CreateEntreprise {

	protected CreateEntreprisePM(EvenementOrganisation evenement, Organisation organisation, Entreprise entreprise,
	                             EvenementOrganisationContext context,
	                             EvenementOrganisationOptions options,
	                             RegDate dateDeCreation,
	                             RegDate dateOuvertureFiscale,
	                             boolean isCreation) throws EvenementOrganisationException {
		super(evenement, organisation, entreprise, context, options, dateDeCreation, dateOuvertureFiscale, isCreation);
	}

	@Override
	public String describe() {
		return "Création d'une entreprise de catégorie PM";
	}


	@Override
	public void doHandle(EvenementOrganisationWarningCollector warnings, EvenementOrganisationSuiviCollector suivis) throws EvenementOrganisationException {
		super.doHandle(warnings, suivis);

		openRegimesFiscauxOrdinairesCHVD(getEntreprise(), getOrganisation(), getDateOuvertureFiscale(), suivis);

		Domicile autoriteFiscalePrincipale = getAutoriteFiscalePrincipale();

		MotifFor motifOuverture = determineMotifOuvertureFor(isCreation());

		openForFiscalPrincipal(getDateOuvertureFiscale(),
		                       autoriteFiscalePrincipale,
		                       MotifRattachement.DOMICILE,
		                       motifOuverture,
		                       GenreImpot.BENEFICE_CAPITAL,
		                       warnings, suivis);

		// Création du bouclement
		createAddBouclement(getDateOuvertureFiscale(), isCreation(), suivis);

		// Ajoute les for secondaires
		adapteForsSecondairesPourEtablissementsVD(getEntreprise(), warnings, suivis);
	}

	@Override
	protected void validateSpecific(EvenementOrganisationErreurCollector erreurs, EvenementOrganisationWarningCollector warnings, EvenementOrganisationSuiviCollector suivis) throws EvenementOrganisationException {
		super.validateSpecific(erreurs, warnings, suivis);

		Assert.state(getCategory() == CategorieEntreprise.PM, String.format("Catégorie d'entreprise non supportée! %s", getCategory()));

		if (getCategory() == null) {
			FormeLegale formeLegale = getOrganisation().getFormeLegale(getDateEvt());
			erreurs.addErreur(String.format("Catégorie introuvable pour l'organisation no %d de forme juridique %s, en date du %s.", getOrganisation().getNumeroOrganisation(),
			                                formeLegale != null ? formeLegale : "inconnue", RegDateHelper.dateToDisplayString(getDateEvt())));
		}

		if (!inscriteAuRC()) {
			erreurs.addErreur("Inscription au RC manquante.");
		}
	}
}
