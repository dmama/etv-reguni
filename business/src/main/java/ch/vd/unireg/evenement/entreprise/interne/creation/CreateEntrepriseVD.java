package ch.vd.unireg.evenement.entreprise.interne.creation;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.FormatNumeroHelper;
import ch.vd.unireg.evenement.entreprise.EvenementEntreprise;
import ch.vd.unireg.evenement.entreprise.EvenementEntrepriseContext;
import ch.vd.unireg.evenement.entreprise.EvenementEntrepriseException;
import ch.vd.unireg.evenement.entreprise.EvenementEntrepriseOptions;
import ch.vd.unireg.evenement.entreprise.audit.EvenementEntrepriseErreurCollector;
import ch.vd.unireg.evenement.entreprise.audit.EvenementEntrepriseSuiviCollector;
import ch.vd.unireg.evenement.entreprise.audit.EvenementEntrepriseWarningCollector;
import ch.vd.unireg.interfaces.entreprise.data.EntrepriseCivile;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.type.GenreImpot;
import ch.vd.unireg.type.MotifRattachement;

/**
 * Evénement interne de création d'entreprise de catégorie "Personnes morales"
 *
 * @author Raphaël Marmier, 2015-09-02
 */
public class CreateEntrepriseVD extends CreateEntreprise {

	protected CreateEntrepriseVD(EvenementEntreprise evenement, EntrepriseCivile entrepriseCivile, Entreprise entreprise,
	                             EvenementEntrepriseContext context,
	                             EvenementEntrepriseOptions options,
	                             RegDate dateDeCreation,
	                             RegDate dateOuvertureFiscale,
	                             boolean isCreation) throws EvenementEntrepriseException {
		super(evenement, entrepriseCivile, entreprise, context, options, dateDeCreation, dateOuvertureFiscale, isCreation);
	}

	@Override
	public String describe() {
		return "Création d'une entreprise vaudoise";
	}


	@Override
	public void doHandle(EvenementEntrepriseWarningCollector warnings, EvenementEntrepriseSuiviCollector suivis) throws EvenementEntrepriseException {
		super.doHandle(warnings, suivis);

		final boolean isSocieteDePersonnes = getEntrepriseCivile().isSocieteDePersonnes(getDateEvt());

		openRegimesFiscauxParDefautCHVD(getEntreprise(), getEntrepriseCivile(), getDateOuvertureFiscale(), suivis);

		openForFiscalPrincipal(getDateOuvertureFiscale(),
		                       getAutoriteFiscalePrincipale(),
		                       MotifRattachement.DOMICILE,
		                       determineMotifOuvertureFor(isCreation),
		                       isSocieteDePersonnes ? GenreImpot.REVENU_FORTUNE : GenreImpot.BENEFICE_CAPITAL,
		                       warnings, suivis);

		if (isSocieteDePersonnes) {
			warnings.addWarning(String.format("Nouvelle société de personnes, date de début à contrôler%s.", getEntrepriseCivile().isInscriteAuRC(getDateEvt()) ? " (Publication FOSC)" : ""));
		}
		else {
			// On renseigne la date de début du premier exercice commercial (SIFISC-30696 : pour tous les types d'entreprises, sauf les SP)
			regleDateDebutPremierExerciceCommercial(getEntreprise(), getDateOuvertureFiscale(), suivis);

			// Réglages exercice commercial
			createAddBouclement(getDateOuvertureFiscale(), isCreation, suivis);
		}

		// Ajoute les for secondaires
		adapteForsSecondairesPourEtablissementsVD(getEntreprise(), warnings, suivis);

		if (getEntrepriseCivile().isAssociationFondation(getDateEvt())){
			// SIFISC-19335 - mettre à l'état "a vérifier" les annoces de créations d'APM
			warnings.addWarning(String.format("Vérification requise après la création de l'association/fondation n°%s.", FormatNumeroHelper.numeroCTBToDisplay(getEntreprise().getNumero())));
		}

	}

	@Override
	protected void validateSpecific(EvenementEntrepriseErreurCollector erreurs, EvenementEntrepriseWarningCollector warnings, EvenementEntrepriseSuiviCollector suivis) throws EvenementEntrepriseException {
		super.validateSpecific(erreurs, warnings, suivis);

		if (getEntrepriseCivile().isSocieteIndividuelle(getDateEvt()) || getEntrepriseCivile().isSocieteSimple(getDateEvt())) {
			throw new EvenementEntrepriseException(String.format("Genre d'entreprise non supportée!: %s", getEntrepriseCivile().getFormeLegale(getDateEvt()).getLibelle()));
		}

		if (getEntrepriseCivile().isInscriptionRCObligatoire(getDateEvt()) && !inscriteAuRC()) {
			erreurs.addErreur("Inscription au RC manquante pour l'entreprise de type PM.");
		}

		// SIFISC-19723: Traitement manuel pour les associations/fondations non RC
		if (getEntrepriseCivile().isAssociationFondation(getDateEvt()) && !getEntrepriseCivile().isInscriteAuRC(getDateEvt())) {
			erreurs.addErreur(String.format("Pas de création automatique de l'association/fondation n°%d [%s] non inscrite au RC (risque de création de doublon). " +
					                                "Veuillez vérifier et le cas échéant créer le tiers associé.",
			                                getEntrepriseCivile().getNumeroEntreprise(), getEntrepriseCivile().getNom(getDateEvt())));
		}
	}
}
