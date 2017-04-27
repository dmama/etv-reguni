package ch.vd.uniregctb.evenement.organisation.interne.creation;

import ch.vd.registre.base.date.RegDate;
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
import ch.vd.uniregctb.type.GenreImpot;
import ch.vd.uniregctb.type.MotifRattachement;

/**
 * Evénement interne de création d'entreprise de catégorie "Personnes morales"
 *
 * @author Raphaël Marmier, 2015-09-02
 */
public class CreateEntrepriseVD extends CreateEntreprise {

	protected CreateEntrepriseVD(EvenementOrganisation evenement, Organisation organisation, Entreprise entreprise,
	                             EvenementOrganisationContext context,
	                             EvenementOrganisationOptions options,
	                             RegDate dateDeCreation,
	                             RegDate dateOuvertureFiscale,
	                             boolean isCreation) throws EvenementOrganisationException {
		super(evenement, organisation, entreprise, context, options, dateDeCreation, dateOuvertureFiscale, isCreation);
	}

	@Override
	public String describe() {
		return "Création d'une entreprise vaudoise";
	}


	@Override
	public void doHandle(EvenementOrganisationWarningCollector warnings, EvenementOrganisationSuiviCollector suivis) throws EvenementOrganisationException {
		super.doHandle(warnings, suivis);

		final boolean isSocieteDePersonnes = getOrganisation().isSocieteDePersonnes(getDateEvt());

		openRegimesFiscauxParDefautCHVD(getEntreprise(), getOrganisation(), getDateOuvertureFiscale(), suivis);

		openForFiscalPrincipal(getDateOuvertureFiscale(),
		                       getAutoriteFiscalePrincipale(),
		                       MotifRattachement.DOMICILE,
		                       determineMotifOuvertureFor(isCreation()),
		                       isSocieteDePersonnes ? GenreImpot.REVENU_FORTUNE : GenreImpot.BENEFICE_CAPITAL,
		                       warnings, suivis);

		if (isSocieteDePersonnes) {
			warnings.addWarning(String.format("Nouvelle société de personnes, date de début à contrôler%s.", getOrganisation().isInscriteAuRC(getDateEvt()) ? " (Publication FOSC)" : ""));
		}
		else {
			// Réglages exercice commercial
			createAddBouclement(getDateOuvertureFiscale(), isCreation(), suivis);
		}

		// Ajoute les for secondaires
		adapteForsSecondairesPourEtablissementsVD(getEntreprise(), warnings, suivis);

		if (getOrganisation().isAssociationFondation(getDateEvt())){
			// SIFISC-19335 - mettre à l'état "a vérifier" les annoces de créations d'APM
			warnings.addWarning(String.format("Vérification requise après la création de l'association/fondation n°%s.", FormatNumeroHelper.numeroCTBToDisplay(getEntreprise().getNumero())));
		}

	}

	@Override
	protected void validateSpecific(EvenementOrganisationErreurCollector erreurs, EvenementOrganisationWarningCollector warnings, EvenementOrganisationSuiviCollector suivis) throws EvenementOrganisationException {
		super.validateSpecific(erreurs, warnings, suivis);

		if (getOrganisation().isSocieteIndividuelle(getDateEvt()) || getOrganisation().isSocieteSimple(getDateEvt())) {
			throw new EvenementOrganisationException(String.format("Genre d'entreprise non supportée!: %s", getOrganisation().getFormeLegale(getDateEvt()).getLibelle()));
		}

		if (getOrganisation().isInscriptionRCObligatoire(getDateEvt()) && !inscriteAuRC()) {
			erreurs.addErreur("Inscription au RC manquante pour l'entreprise de type PM.");
		}

		// SIFISC-19723: Traitement manuel pour les associations/fondations non RC
		if (getOrganisation().isAssociationFondation(getDateEvt()) && !getOrganisation().isInscriteAuRC(getDateEvt())) {
			erreurs.addErreur(String.format("Pas de création automatique de l'association/fondation n°%d [%s] non inscrite au RC (risque de création de doublon). " +
					                                "Veuillez vérifier et le cas échéant créer le tiers associé.",
			                                getOrganisation().getNumeroOrganisation(), getOrganisation().getNom(getDateEvt())));
		}
	}
}
