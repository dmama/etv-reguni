package ch.vd.unireg.declaration.snc;

import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.StatusManager;
import ch.vd.unireg.declaration.DeclarationException;
import ch.vd.unireg.declaration.QuestionnaireSNC;
import ch.vd.unireg.editique.EditiqueException;
import ch.vd.unireg.editique.EditiqueResultat;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.type.EtatDelaiDocumentFiscal;

/**
 * Service autour des questionnaires SNC : génération de tâches en masse, émissions, rappels...
 */
public interface QuestionnaireSNCService {

	int codeSegment = 6;

	/**
	 * @param periodeFiscale la période fiscale à considérer pour la génération des tâches
	 * @param dateTraitement la date de traitement (doit être postérieure à la fin de la période fiscale)
	 * @param nbThreads nombre de threads pour le processing
	 * @param statusManager status manager
	 * @return un résumé des actions accomplies
	 */
	DeterminationQuestionnairesSNCResults determineQuestionnairesAEmettre(int periodeFiscale, RegDate dateTraitement, int nbThreads, StatusManager statusManager) throws DeclarationException;

	/**
	 * @param periodeFiscale la période fiscale à considérer pour la génération des tâches
	 * @param dateTraitement la date de traitement (doit être postérieure à la fin de la période fiscale)
	 * @param statusManager status manager
	 * @return un résumé des actions accomplies
	 */
	EnvoiQuestionnairesSNCEnMasseResults envoiQuestionnairesSNCEnMasse(int periodeFiscale, RegDate dateTraitement, @Nullable Integer nbMaxEnvois, StatusManager statusManager) throws DeclarationException;

	/**
	 * @param dateTraitement la date de traitement
	 * @param periodeFiscale si présent, la période fiscale des questionnaires candidats au rappel
	 * @param statusManager status manager
	 * @return un résumé des actions accomplies
	 */
	EnvoiRappelsQuestionnairesSNCResults envoiRappelsQuestionnairesSNCEnMasse(RegDate dateTraitement, @Nullable Integer periodeFiscale, @Nullable Integer nbMaxEnvois, StatusManager statusManager) throws DeclarationException;

	/**
	 * @param entreprise une entreprise
	 * @param pourEmissionAutoSeulement <code>true</code> si on ne veut que les périodes qui doivent générer des tâches automatique, <code>false</code> sinon
	 * @return la liste des périodes pour lesquelles il serait de bon ton d'avoir un questionnaire SNC
	 */
	@NotNull
	Set<Integer> getPeriodesFiscalesTheoriquementCouvertes(Entreprise entreprise, boolean pourEmissionAutoSeulement);

	/**
	 * Envoi d'un questionnaire SNC pour impression locale
	 * @param questionnaire nouveau questionnaire à envoyer
	 * @param dateEvenement date de traitement
	 * @return données du document imprimé
	 * @throws DeclarationException en cas de souci
	 */
	EditiqueResultat envoiQuestionnaireSNCOnline(QuestionnaireSNC questionnaire, RegDate dateEvenement) throws DeclarationException;

	/**
	 * Envoi d'un questionnaire SNC pour impression batch
	 * @param questionnaire nouveau questionnaire à envoyer
	 * @param dateEvenement date de traitement
	 * @throws DeclarationException en cas de souci
	 */
	void envoiQuestionnaireSNCForBatch(QuestionnaireSNC questionnaire, RegDate dateEvenement) throws DeclarationException;

	/**
	 * Envoi pour impression locale d'un duplicata du questionnaire SNC
	 * @param questionnaire questionnaire à envoyer
	 * @return données du document imprimé
	 * @throws DeclarationException en cas de souci
	 */
	EditiqueResultat envoiDuplicataQuestionnaireSNCOnline(QuestionnaireSNC questionnaire) throws DeclarationException;

	/**
	 * Création de l'état "rappelé" et envoi pour impression locale d'un rappel du questionnaire SNC
	 * @param questionnaire questionnaire à rappeler
	 * @param dateTraitement date de traitement (= date d'obtention de l'état)
	 * @return données du document imprimé
	 * @throws DeclarationException en cas de souci
	 */
	EditiqueResultat envoiRappelQuestionnaireSNCOnline(QuestionnaireSNC questionnaire, RegDate dateTraitement) throws DeclarationException;

	/**
	 * Création de l'état "rappelé" et envoi pour impression locale d'un rappel du questionnaire SNC
	 * @param questionnaire questionnaire à rappeler
	 * @param dateTraitement date de traitement (= date d'obtention de l'état)
	 * @param dateExpedition date à placer sur le courrier
	 * @throws DeclarationException en cas de souci
	 */
	void envoiRappelQuestionnaireSNCForBatch(QuestionnaireSNC questionnaire, RegDate dateTraitement, RegDate dateExpedition) throws DeclarationException;

	/**
	 * Récupération du PDF de copie conforme
	 * @param questionnaire questionnaire rappelé dont on veut récupérer le rappel
	 * @return données du document de rappel
	 * @throws EditiqueException en cas de souci
	 */
	EditiqueResultat getCopieConformeRappelQuestionnaireSNC(QuestionnaireSNC questionnaire) throws EditiqueException;

	/**
	 * Ajout d'un nouvel état "retourné" au questionnaire donné
	 * @param questionnaire questionnaire
	 * @param dateRetour date de quittancement
	 * @param source source du quittancement
	 * @throws DeclarationException en cas de souci
	 */
	void quittancerQuestionnaire(QuestionnaireSNC questionnaire, RegDate dateRetour, String source) throws DeclarationException;

	/**
	 * Annule le questionnaire donné
	 * @param questionnaire le questionnaire à annuler
	 * @throws DeclarationException en cas de souci
	 */
	void annulerQuestionnaire(QuestionnaireSNC questionnaire) throws DeclarationException;

	/**
	 * Ajoute un délai sur un questionnaire SNC.
	 *
	 * @param questionnaireId l'id du questionnaire SNC
	 * @param dateDemande     la date de la demande
	 * @param delaiAccordeAu  le délai accordé
	 * @param etatDelai       l'état du délai accordé
	 * @return l'id du délai ajouté
	 */
	Long ajouterDelai(long questionnaireId, RegDate dateDemande, RegDate delaiAccordeAu, EtatDelaiDocumentFiscal etatDelai);

	/**
	 * Fait passer à l'état <i>échu</i> le questionnaire SNC spécifié.
	 *
	 * @param qsnc          un questionnaire SNC
	 * @param dateObtention la date d'obtention de l'état échu
	 */
	void echoirQuestionnaire(@NotNull QuestionnaireSNC qsnc, @NotNull RegDate dateObtention);

	EditiqueResultat envoiDemandeDelaiQuestionnaireSNCOnline(Long idDelai, RegDate dateTraitement) throws EditiqueException;

	String envoiDemandeDelaiQuestionnaireSNCBatch(Long idDelai, RegDate dateTraitement) throws EditiqueException;
}
