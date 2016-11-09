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
 * Evénement interne de création d'entreprise de catégorie APM
 *
 * Uniquement les APM inscrites au RC: SIFISC-19723, SIFISC-19660 (Traitement des événements RCEnt de création d'APM et Annonces à l'IDE des APM VD)
 *
 * Les nouvelles APM non inscrites au RC ne sont pas prises en charge ici. Elle sont censées avoir été créées dans Unireg au préalable à une
 * annonce à l'IDE. L'annonce de RCEnt signalant leur création doit avoir été détectée en amont afin d'apparier correctement le tiers à l'organisation
 * RCEnt.
 *
 * @author Raphaël Marmier, 2015-09-02
 */
public class CreateEntrepriseAPMAuRC extends CreateEntreprise {

	protected CreateEntrepriseAPMAuRC(EvenementOrganisation evenement, Organisation organisation, Entreprise entreprise,
	                                  EvenementOrganisationContext context,
	                                  EvenementOrganisationOptions options,
	                                  RegDate dateDeCreation,
	                                  RegDate dateOuvertureFiscale,
	                                  boolean isCreation) throws EvenementOrganisationException {
		super(evenement, organisation, entreprise, context, options, dateDeCreation, dateOuvertureFiscale, isCreation);
	}

	@Override
	public String describe() {
		return "Création d'une entreprise de catégorie APM";
	}

	@Override
	public void doHandle(EvenementOrganisationWarningCollector warnings, EvenementOrganisationSuiviCollector suivis) throws EvenementOrganisationException {
		super.doHandle(warnings, suivis);

		MotifFor motifOuverture = determineMotifOuvertureFor(isCreation());

		openRegimesFiscauxOrdinairesCHVD(getEntreprise(), getOrganisation(), getDateOuvertureFiscale(), suivis);

		openForFiscalPrincipal(getDateOuvertureFiscale(),
		                       getAutoriteFiscalePrincipale(),
		                       MotifRattachement.DOMICILE,
		                       motifOuverture,
		                       GenreImpot.BENEFICE_CAPITAL,
		                       warnings, suivis);

		// Création du bouclement
		createAddBouclement(getDateOuvertureFiscale(), isCreation(), suivis);
		// Ajoute les for secondaires
		adapteForsSecondairesPourEtablissementsVD(getEntreprise(), warnings, suivis);

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

		/*
		 SIFISC-19723 Pour éviter les doublons lors de la mauvaise identification d'APM créées à la main par l'ACI et simultanément enregistrée par SiTi,
		 pas de création automatique des APM, sauf lorsque l'inscription provient du RC, qui dans ce cas est nécessairement l'institution émettrice.
		  */
		// Traitement manuel pour SIFISC-19723
		if (!getOrganisation().isInscriteAuRC(getDateEvt())) {
			erreurs.addErreur(String.format("Pas de création automatique de l'APM n°%d [%s] non inscrite au RC (risque de création de doublon). " +
					                                  "Veuillez vérifier et le cas échéant créer le tiers associé.",
			                                  getOrganisation().getNumeroOrganisation(), getOrganisation().getNom(getDateEvt())));
		}
	}
}
