package ch.vd.unireg.documentfiscal;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.StatusManager;
import ch.vd.unireg.editique.EditiqueException;
import ch.vd.unireg.editique.EditiqueResultat;
import ch.vd.unireg.foncier.DemandeDegrevementICI;
import ch.vd.unireg.foncier.EnvoiFormulairesDemandeDegrevementICIResults;
import ch.vd.unireg.foncier.RappelFormulairesDemandeDegrevementICIResults;
import ch.vd.unireg.registrefoncier.ImmeubleRF;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.type.TypeLettreBienvenue;

/**
 * Service qui gère les autres documents fiscaux
 */
public interface AutreDocumentFiscalService {

	/**
	 * Envoi des lettres de bienvenue en masse
	 * @param dateTraitement date de traitement (= date du jour, ou assimilée)
	 * @param delaiCarence délai (en jours) de décalage pour la prise en compte des assujettissements
	 * @param statusManager status manager (pour la gestion, par exemple, de l'interruption du job, ou de la mesure de progression...)
	 * @return les données du rapport d'exécution
	 */
	EnvoiLettresBienvenueResults envoyerLettresBienvenueEnMasse(RegDate dateTraitement, int delaiCarence, StatusManager statusManager);

	/**
	 * Envoi des rappels des lettres de bienvenue échues à la date de traitement
	 * @param dateTraitement date de traitement (= date du jour, ou assimilée)
	 * @param statusManager status manager (pour la gestion, par exemple, de l'interruption du job, ou de la mesure de progression...)
	 * @return les données du rapport d'exécution
	 */
	RappelLettresBienvenueResults envoyerRappelsLettresBienvenueEnMasse(RegDate dateTraitement, StatusManager statusManager);

	/**
	 * Envoi des formulaires de demande de dégrèvement ICI en masse
	 * @param dateTraitement date de traitement (= assimilée à la date du jour)
	 * @param nbThreads degrés de parallélisme du traitement
	 * @param nbMaxEnvois nombre maximal d'envois (optionnel)
	 * @param statusManager status manager (pour la gestion de l'interruption du job ou la mesure de la progression)
	 * @return les données du rapport d'exécution
	 */
	EnvoiFormulairesDemandeDegrevementICIResults envoyerFormulairesDemandeDegrevementICIEnMasse(RegDate dateTraitement, int nbThreads, @Nullable Integer nbMaxEnvois, StatusManager statusManager);

	/**
	 * Envoi des rappels des formulaires de demande de dégrèvement ICI en masse
	 * @param dateTraitement date de traitement (= assimilée à la date du jour)
	 * @param statusManager status manager (pour la gestion de l'interruption du job ou la mesure de la progression)
	 * @return les données du rapport d'exécution
	 */
	RappelFormulairesDemandeDegrevementICIResults envoyerRappelsFormulairesDemandeDegrevementICIEnMasse(RegDate dateTraitement, StatusManager statusManager);

	/**
	 * @param e entreprise pour laquelle on doit envoyer une lettre de bienvenue (en mode batch)
	 * @param dateTraitement date de traitement
	 * @param dateDebutNouvelAssujettissement date de début du nouvel assujettissement qui justifie de l'envoi de la lettre de bienvenue
	 * @return la lettre de bienvenue envoyée
	 * @throws AutreDocumentFiscalException en cas de souci
	 */
	LettreBienvenue envoyerLettreBienvenueBatch(Entreprise e, RegDate dateTraitement, RegDate dateDebutNouvelAssujettissement) throws AutreDocumentFiscalException;

	/**
	 *
	 * @param lettre
	 * @return
	 * @throws AutreDocumentFiscalException
	 */
	EditiqueResultat imprimeDuplicataLettreBienvenueOnline(LettreBienvenue lettre) throws AutreDocumentFiscalException;

	EditiqueResultat imprimeDuplicataDemandeDegrevementOnline(DemandeDegrevementICI demande) throws AutreDocumentFiscalException;

	/**
	 * Sauve et attache un délai sur un autre document fiscal.
	 * @param doc le document
	 * @param delai le délai
	 * @return l'instance persistée de délai
	 */
	DelaiAutreDocumentFiscal addAndSave(AutreDocumentFiscal doc, DelaiAutreDocumentFiscal delai);

	/**
	 * Sauve et attache un état sur un autre document fiscal.
	 * @param doc le document
	 * @param etat l'état
	 * @return l'instance persistée de l'état
	 */
	<T extends EtatAutreDocumentFiscal> T addAndSave(AutreDocumentFiscal doc, T etat);

	/**
	 * @param e entreprise pour laquelle on doit envoyer un formulaire de demande de dégrèvement (en mode batch)
	 * @param immeuble immeuble concerné par la demande de dégrèvement
	 * @param periodeFiscale période fiscale à partir de laquelle le dégrèvement peut intervenir
	 * @param dateTraitement date de traitement
	 * @return le formulaire envoyé
	 * @throws AutreDocumentFiscalException en cas de souci
	 */
	DemandeDegrevementICI envoyerDemandeDegrevementICIBatch(Entreprise e, ImmeubleRF immeuble, int periodeFiscale, RegDate dateTraitement) throws AutreDocumentFiscalException;

	/**
	 * @param e entreprise pour laquelle on doit envoyer un formulaire de demande de dégrèvement (en mode online)
	 * @param immeuble immeuble concerné par la demande de dégrèvement
	 * @param periodeFiscale période fiscale à partir de laquelle le dégrèvement peut intervenir
	 * @param dateTraitement date de traitement
	 * @param delaiAccorde délai de retour accordé
	 * @return le formulaire envoyé
	 * @throws AutreDocumentFiscalException en cas de souci
	 */
	EditiqueResultat envoyerDemandeDegrevementICIOnline(Entreprise e, ImmeubleRF immeuble, int periodeFiscale, RegDate dateTraitement, RegDate delaiAccorde) throws AutreDocumentFiscalException;

	/**
	 * @param document autre document fiscal dont on veut récupérer le courrier initial
	 * @return document PDF correspondant au courrier initial
	 * @throws EditiqueException en cas de souci
	 */
	EditiqueResultat getCopieConformeDocumentInitial(AutreDocumentFiscal document) throws EditiqueException;

	/**
	 * @param document autre document fiscal dont on veut récupérer le courrier de rappel
	 * @return document PDF correspondant au courrier de rappel
	 * @throws EditiqueException en cas de souci
	 */
	EditiqueResultat getCopieConformeDocumentRappel(AutreDocumentFiscalAvecSuivi document) throws EditiqueException;

	/**
	 * @param lettre la lettre de bienvenue à rappeler
	 * @param dateTraitement date de traitement
	 * @param dateEnvoiRappel date de rappel effective
	 * @throws AutreDocumentFiscalException en cas de souci
	 */
	void envoyerRappelLettreBienvenueBatch(LettreBienvenue lettre, RegDate dateTraitement, RegDate dateEnvoiRappel) throws AutreDocumentFiscalException;

	/**
	 * @param formulaire le formulaire de demande de dégrèvement ICI à rappeler
	 * @param dateTraitement date de traitement
	 * @param dateEnvoiRappel date de rappel effective
	 * @throws AutreDocumentFiscalException en cas de souci
	 */
	void envoyerRappelFormulaireDemandeDegrevementICIBatch(DemandeDegrevementICI formulaire, RegDate dateTraitement, RegDate dateEnvoiRappel) throws AutreDocumentFiscalException;

	/**
	 * Génération d'une nouvelle lettre d'autorisation de radiation du RC en impression locale
	 * @param e entreprise concernée
	 * @param dateTraitement date de traitement (= date d'envoi)
	 * @param dateDemandeInitiale date de la demande initiale du RC
	 * @return le document imprimé
	 * @throws AutreDocumentFiscalException en cas de souci
	 */
	EditiqueResultat envoyerAutorisationRadiationRCOnline(Entreprise e, RegDate dateTraitement, RegDate dateDemandeInitiale) throws AutreDocumentFiscalException;

	/**
	 * Génération d'une nouvelle lettre de demande de bilan final en impression locale
	 * @param e entreprise concernée
	 * @param dateTraitement date de traitement (= date d'envoi)
	 * @param dateRequisitionRadiation date de la réquisition de radiation du RC
	 * @return le document imprimé
	 * @throws AutreDocumentFiscalException en cas de souci
	 */
	EditiqueResultat envoyerDemandeBilanFinalOnline(Entreprise e, RegDate dateTraitement, int periodeFiscale, RegDate dateRequisitionRadiation) throws AutreDocumentFiscalException;

	/**
	 * Génération d'une nouvelle lettre type de liquidation en impression locale
	 * @param e entreprise concernée
	 * @param dateTraitement date de traitement (= date d'envoi)
	 * @return le document imprimé
	 * @throws AutreDocumentFiscalException en cas de souci
	 */
	EditiqueResultat envoyerLettreTypeInformationLiquidationOnline(Entreprise e, RegDate dateTraitement) throws AutreDocumentFiscalException;

	/**
	 * Génération d'une nouvelle lettre de bienvenue en impression locale
	 *
	 * @param entreprise     entreprise concernée
	 * @param dateTraitement date de traitement (= date d'envoi)
	 * @param typeLettre     le type de lettre à imprimer
	 * @param delaiRetour    délai de retour initial
	 * @return le document imprimé
	 * @throws AutreDocumentFiscalException en cas de souci
	 */
	EditiqueResultat envoyerLettreBienvenueOnline(@NotNull Entreprise entreprise, @NotNull RegDate dateTraitement, @NotNull TypeLettreBienvenue typeLettre, @NotNull RegDate delaiRetour) throws AutreDocumentFiscalException;
}
