package ch.vd.uniregctb.editique;

import javax.jms.JMSException;
import java.math.BigInteger;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinairePM;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinairePP;
import ch.vd.uniregctb.declaration.DeclarationImpotSource;
import ch.vd.uniregctb.declaration.DelaiDeclaration;
import ch.vd.uniregctb.declaration.ModeleFeuilleDocument;
import ch.vd.uniregctb.declaration.QuestionnaireSNC;
import ch.vd.uniregctb.declaration.ordinaire.DeclarationImpotService;
import ch.vd.uniregctb.declaration.ordinaire.pp.InformationsDocumentAdapter;
import ch.vd.uniregctb.declaration.snc.QuestionnaireSNCService;
import ch.vd.uniregctb.documentfiscal.AutorisationRadiationRC;
import ch.vd.uniregctb.documentfiscal.DemandeBilanFinal;
import ch.vd.uniregctb.documentfiscal.LettreBienvenue;
import ch.vd.uniregctb.documentfiscal.LettreTypeInformationLiquidation;
import ch.vd.uniregctb.foncier.DemandeDegrevementICI;
import ch.vd.uniregctb.fourreNeutre.FourreNeutre;
import ch.vd.uniregctb.mouvement.BordereauMouvementDossier;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.type.TypeDocument;

public interface EditiqueCompositionService {

	/**
	 * Imprime la déclaration PP spécifiée pour une visualisation on-line, et retourne le document imprimé. Il n'y a pas d'envoi vers inbox si c'est trop lent.
	 * <p/>
	 * <b>Note:</b> cette méthode n'envoie pas d'événement fiscal et ne devrait pas être appelée directement. Il faut utiliser la méthode {@link
	 * DeclarationImpotService#envoiDIOnline(DeclarationImpotOrdinairePP, RegDate)}.
	 *
	 * @param declaration   la déclaration d'impôt ordinaire à imprimer
	 * @return le document imprimé
	 */
	EditiqueResultat imprimeDIOnline(DeclarationImpotOrdinairePP declaration) throws EditiqueException, JMSException;

	/**
	 * Imprime la déclaration PM spécifiée pour une visualisation on-line, et retourne le document imprimé. Il n'y a pas d'envoi vers inbox si c'est trop lent.
	 * <p/>
	 * <b>Note:</b> cette méthode n'envoie pas d'événement fiscal et ne devrait pas être appelée directement. Il faut utiliser la méthode {@link
	 * DeclarationImpotService#envoiDIOnline(DeclarationImpotOrdinairePM, RegDate)}.
	 *
	 * @param declaration   la déclaration d'impôt ordinaire à imprimer
	 * @return le document imprimé
	 */
	EditiqueResultat imprimeDIOnline(DeclarationImpotOrdinairePM declaration) throws EditiqueException, JMSException;

	/**
	 * Imprime le questionnaire SNC spécifié pour une visualisation online, et retourne le document imprimé. Il n'y a pas d'envoi vers l'inbox si c'est trop lent.
	 * <p/>
	 * <b>Note:</b> cette méthode n'envoie pas d'événement fiscal et ne devrait pas être appelée directement. Il faut utiliser la méthode {@link
	 * QuestionnaireSNCService#envoiQuestionnaireSNCOnline(QuestionnaireSNC, RegDate)}.
	 * @param questionnaire le questionnaire SNC à imprimer
	 * @return le document imprimé
	 */
	EditiqueResultat imprimeQuestionnaireSNCOnline(QuestionnaireSNC questionnaire) throws EditiqueException, JMSException;

	/**
	 * Imprime la déclaration PP spécifiée pour une visualisation on-line et retourne le document imprimé (ou le fait envoyer dans l'inbox si c'est trop lent)
	 * <p/>
	 * <b>Note:</b> cette méthode n'envoie pas d'événement fiscal et ne devrait pas être appelée directement. Il faut utiliser la méthode
	 * {@link DeclarationImpotService#envoiDuplicataDIOnline(DeclarationImpotOrdinairePP, TypeDocument, List)}.
	 *
	 * @param declaration   la déclaration d'impôt ordinaire à imprimer
	 * @param typeDocument  le type de document
	 * @param annexes       la liste des annexes
	 * @return le document imprimé
	 */
	EditiqueResultat imprimeDuplicataDIOnline(DeclarationImpotOrdinairePP declaration, TypeDocument typeDocument, List<ModeleFeuilleDocumentEditique> annexes) throws EditiqueException, JMSException;

	/**
	 * Imprime la déclaration PM spécifiée pour une visualisation on-line et retourne le document imprimé (ou le fait envoyer dans l'inbox si c'est trop lent)
	 * <p/>
	 * <b>Note:</b> cette méthode n'envoie pas d'événement fiscal et ne devrait pas être appelée directement. Il faut utiliser la méthode
	 * {@link DeclarationImpotService#envoiDuplicataDIOnline(DeclarationImpotOrdinairePM, List)}.
	 *
	 * @param declaration   la déclaration d'impôt ordinaire à imprimer
	 * @param annexes       la liste des annexes
	 * @return le document imprimé
	 */
	EditiqueResultat imprimeDuplicataDIOnline(DeclarationImpotOrdinairePM declaration, List<ModeleFeuilleDocumentEditique> annexes) throws EditiqueException, JMSException;

	/**
	 * Imprime le questionnaire SNC spécifié pour une visualisation online, et retourne le document imprimé (ou le fait envoyer dans l'inbox si c'est trop lent)
	 * <p/>
	 * <b>Note:</b> cette méthode n'envoie pas d'événement fiscal et ne devrait pas être appelée directement. Il faut utiliser la méthode {@link
	 * QuestionnaireSNCService#envoiDuplicataQuestionnaireSNCOnline(QuestionnaireSNC)}.
	 * @param questionnaire le questionnaire SNC à imprimer
	 * @return le document imprimé
	 */
	EditiqueResultat imprimeDuplicataQuestionnaireSNCOnline(QuestionnaireSNC questionnaire) throws EditiqueException, JMSException;

	/**
	 * Imprime la déclaration PP spécifiée pour un envoi en masse. Cette méthode retourne immédiatement et du moment que la transaction est committée, il est de la responsabilité d'éditique d'imprimer la
	 * déclaration.
	 * <p/>
	 * <b>Note:</b> cette méthode n'envoie pas d'événement fiscal et ne devrait pas être appelée directement. Il faut utiliser la méthode {@link
	 * DeclarationImpotService#envoiDIForBatch(DeclarationImpotOrdinairePP, RegDate)}.
	 *
	 * @param declaration   la déclaration d'impôt ordinaire à imprimer
	 */
	void imprimeDIForBatch(DeclarationImpotOrdinairePP declaration) throws EditiqueException;

	/**
	 * Imprime la déclaration PM spécifiée pour un envoi en masse. Cette méthode retourne immédiatement et du moment que la transaction est committée, il est de la responsabilité d'éditique d'imprimer la
	 * déclaration.
	 * <p/>
	 * <b>Note:</b> cette méthode n'envoie pas d'événement fiscal et ne devrait pas être appelée directement. Il faut utiliser la méthode {@link
	 * DeclarationImpotService#envoiDIForBatch(DeclarationImpotOrdinairePP, RegDate)}.
	 *
	 * @param declaration   la déclaration d'impôt ordinaire à imprimer
	 */
	void imprimeDIForBatch(DeclarationImpotOrdinairePM declaration) throws EditiqueException;

	/**
	 * Imprime le questionnaire SNC spécifié pour un envoi en masse. Cette méthode retourne immédiatement, et du moment que la transaction est committée, il est de la responsabilité
	 * d'éditique d'imprimer le questionnaire
	 * <p/>
	 * <b>Note:</b> cette méthode n'envoie pas d'événement fiscal et ne devrait pas être appelée directement. Il faut utiliser la méthode {@link
	 * QuestionnaireSNCService#envoiQuestionnaireSNCOnline(QuestionnaireSNC, RegDate)}.
	 * @param questionnaire le questionnaire SNC à imprimer
	 * @return le document imprimé
	 */
	void imprimerQuestionnaireSNCForBatch(QuestionnaireSNC questionnaire) throws EditiqueException;

	/**
	 * Imprime la lr spécifiée pour un envoi en masse. Cette méthode retourne immédiatement et du moment que la transaction est committée, il est de la responsabilité d'éditique d'imprimer la
	 * déclaration.
	 * <p/>
	 * <b>Note:</b> cette méthode n'envoie pas d'événement fiscal et ne devrait pas être appelée directement. Il faut utiliser la méthode {@link
	 * ch.vd.uniregctb.declaration.source.ListeRecapService#imprimerLR(ch.vd.uniregctb.tiers.DebiteurPrestationImposable, ch.vd.registre.base.date.RegDate, ch.vd.registre.base.date.RegDate)}
	 *
	 * @param lr            la LR à imprimer
	 */
	void imprimeLRForBatch(DeclarationImpotSource lr) throws EditiqueException;

	/**
	 * Imprime les fourres de nouveaux dossiers
	 *
	 * @param contribuables
	 * @return
	 * @throws EditiqueException
	 */
	EditiqueResultat imprimeNouveauxDossiers(List<Contribuable> contribuables) throws EditiqueException, JMSException;

	/**
	 * Imprime la sommation pour la déclaration PP spécifiée pour un envoi en masse. Cette méthode retourne immédiatement et du moment que la transaction est committée, il est de la responsabilité
	 * d'éditique d'imprimer la déclaration.
	 * <p/>
	 *
	 * @param declaration   la déclaration d'impôt ordinaire dont la sommation est à imprimer
	 * @param dateEvenement la date d'impression
	 * @param emolument     montant de l'émolument (optionnel) à percevoir pour la sommation émise, en francs suisses
	 */
	void imprimeSommationDIForBatch(DeclarationImpotOrdinairePP declaration, boolean miseSousPliImpossible, RegDate dateEvenement, @Nullable Integer emolument) throws EditiqueException;

	/**
	 * Imprime la sommation pour la déclaration PM spécifiée pour un envoi en masse. Cette méthode retourne immédiatement et du moment que la transaction est committée, il est de la responsabilité
	 * d'éditique d'imprimer la déclaration.
	 * <p/>
	 *
	 * @param declaration   la déclaration d'impôt ordinaire dont la sommation est à imprimer
	 * @param dateTraitement la date d'impression
	 */
	void imprimeSommationDIForBatch(DeclarationImpotOrdinairePM declaration, RegDate dateTraitement, RegDate dateOfficielleEnvoi) throws EditiqueException;

	/**
	 * Imprime la sommation pour la LR spécifiée pour un envoi en masse. Cette méthode retourne immédiatement et du moment que la transaction est committée, il est de la responsabilité d'éditique
	 * d'imprimer la déclaration.
	 *
	 * @param lr
	 * @param dateEvenement
	 * @throws EditiqueException
	 */
	void imprimeSommationLRForBatch(DeclarationImpotSource lr, RegDate dateEvenement) throws EditiqueException;

	/**
	 * Imprime le rappel pour le questionnaire SNC spécifié pour un envoi en masse. Cette méthode retourne immédiatement et du moment que la transaction est committée, il est de la responsabilité
	 * d'éditique d'imprimer la déclaration
	 * @param questionnaire le questionnaire dont on veut envoyer le rappel
	 * @param dateTraitement date de traitement
	 * @param dateOfficielleEnvoi date à placer sur le courrier comme date officielle d'envoi
	 * @throws EditiqueException en cas de souci
	 */
	void imprimeRappelQuestionnaireSNCForBatch(QuestionnaireSNC questionnaire, RegDate dateTraitement, RegDate dateOfficielleEnvoi) throws EditiqueException;

	/**
	 * Imprime la sommation pour la déclaration d'impôt PP spécifiée on-line.
	 *
	 * @param declaration
	 * @param dateEvenement
	 * @param emolument     montant de l'émolument (optionnel) à percevoir pour la sommation émise, en francs suisses
	 * @return l'id du document
	 * @throws EditiqueException
	 */
	EditiqueResultat imprimeSommationDIOnline(DeclarationImpotOrdinairePP declaration, RegDate dateEvenement, @Nullable Integer emolument) throws EditiqueException, JMSException;

	/**
	 * Imprime la sommation pour la déclaration d'impôt PM spécifiée on-line.
	 *
	 * @param declaration
	 * @param dateTraitement
	 * @return l'id du document
	 * @throws EditiqueException
	 */
	EditiqueResultat imprimeSommationDIOnline(DeclarationImpotOrdinairePM declaration, RegDate dateTraitement, RegDate dateOfficielleEnvoi) throws EditiqueException, JMSException;

	/**
	 * Imprime la sommation pour la lr spécifiée on-line.
	 *
	 * @param lr
	 * @param dateEvenement
	 * @return
	 * @throws EditiqueException
	 */
	EditiqueResultat imprimeSommationLROnline(DeclarationImpotSource lr, RegDate dateEvenement) throws EditiqueException, JMSException;

	/**
	 * Imprime le rappel pour le questionnaire SNC spécifié pour un envoi en masse. Cette méthode retourne immédiatement et du moment que la transaction est committée, il est de la responsabilité
	 * d'éditique d'imprimer la déclaration
	 * @param questionnaire le questionnaire dont on veut envoyer le rappel
	 * @param dateTraitement date de traitement
	 * @return le document imprimé
	 * @throws EditiqueException en cas de souci
	 */
	EditiqueResultat imprimeRappelQuestionnaireSNCOnline(QuestionnaireSNC questionnaire, RegDate dateTraitement) throws EditiqueException, JMSException;

	/**
	 * Imprime la confirmation de délai pour la {@link DeclarationImpotOrdinairePP} et le {@link ch.vd.uniregctb.declaration.DelaiDeclaration} spécifié
	 *
	 * @param di
	 * @param delai
	 * @return
	 * @throws EditiqueException
	 */
	Pair<EditiqueResultat, String> imprimeConfirmationDelaiOnline(DeclarationImpotOrdinairePP di, DelaiDeclaration delai) throws EditiqueException, JMSException;

	/**
	 * Imprime la lettre de décision d'accord/refus de délai pour la {@link DeclarationImpotOrdinairePM}
	 *
	 * @param di la déclaration d'impôt PM
	 * @param delai le délai accordé/refusé
	 * @return un accesseur vers le document éditique généré, et l'identifiant d'archivage du document généré
	 * @throws EditiqueException en cas de souci
	 */
	Pair<EditiqueResultat, String> imprimeLettreDecisionDelaiOnline(DeclarationImpotOrdinairePM di, DelaiDeclaration delai) throws EditiqueException, JMSException;

	/**
	 * Demande l'impression et l'envoi de la lettre de décision d'accord de délai pour la {@link DeclarationImpotOrdinairePM}
	 *
	 * @param di la déclaration d'impôt PM
	 * @param delai le délai accordé/refusé
	 * @return l'identifiant d'archivage du document généré
	 * @throws EditiqueException en cas de souci
	 */
	String imprimeLettreDecisionDelaiForBatch(DeclarationImpotOrdinairePM di, DelaiDeclaration delai) throws EditiqueException, JMSException;

	/**
	 * Imprime la liste récapitulative spécifiée on-line
	 *
	 * @param lr
	 * @param typeDocument
	 * @return
	 * @throws EditiqueException
	 */
	EditiqueResultat imprimeLROnline(DeclarationImpotSource lr, TypeDocument typeDocument) throws EditiqueException, JMSException;

	/**
	 * Envoie à l'éditique le bordereau de mouvements de dossiers correspondant aux mouvement donnés
	 *
	 * @param bordereauMouvementDossier les mouvements qui doivent apparaître sur le bordereau
	 * @return l'identifiant du document (pour le récupérer ensuite)
	 */
	EditiqueResultat envoyerImpressionLocaleBordereau(BordereauMouvementDossier bordereauMouvementDossier) throws EditiqueException, JMSException;

	/**
	 * Imprime les document de confirmation ou de mise en attente lors d'une demande d'inscription à la E-facture
	 * @param tiers a qui le document doit être envoyé
	 * @param typeDoc permet de determiner le type de document a imprimer
	 * @param dateTraitement date du traitement
	 * @param dateDemande date de la demande d'inscription pour laquelle on imprime ce document
	 * @param noAdherent numéro d'adhérent e-facture de la demande d'inscription en cours de traitement
	 * @param dateDemandePrecedente date de la demande d'inscription précédente remplacée par celle-ci
	 * @param noAdherentPrecedent numéro d'adhérent e-facture de l'inscription précédente remplacée par celle-ci
	 * @return l'identifiant d'archivage du document
	 * @throws EditiqueException
	 */
	String imprimeDocumentEfacture(Tiers tiers, TypeDocument typeDoc, Date dateTraitement, RegDate dateDemande, BigInteger noAdherent, RegDate dateDemandePrecedente, BigInteger noAdherentPrecedent) throws EditiqueException, JMSException;

	/**
	 * Envoie à l'éditique le formulaire immeuble à imprimer en masse
	 *
	 * @param infosDocument
	 * @param listeModele
	 * @param nombreAnnexesImmeuble
	 * @throws EditiqueException
	 */
	int imprimeAnnexeImmeubleForBatch(InformationsDocumentAdapter infosDocument, Set<ModeleFeuilleDocument> listeModele, int nombreAnnexesImmeuble) throws EditiqueException;

	/**
	 * Envoie à l'éditique la lettre de bienvenue à imprimer en masse
	 * @param lettre la lettre en question
	 * @param dateTraitement la date de traitement de l'envoi
	 * @throws EditiqueException en cas de problème
	 */
	void imprimeLettreBienvenueForBatch(LettreBienvenue lettre, RegDate dateTraitement) throws EditiqueException;

	/**
	 * Envoie à l'éditique le rappel de la lettre de bienvenue à imprimer en masse
	 * @param lettre la lettre rappelée
	 * @param dateTraitement la date de traitement de l'envoi du rappel
	 * @throws EditiqueException en cas de problème
	 */
	void imprimeRappelLettreBienvenueForBatch(LettreBienvenue lettre, RegDate dateTraitement) throws EditiqueException;

	/**
	 * Envoie à l'éditique une lettre d'autorisation de radiation du RC à imprimer localement
	 * @param lettre la lettre d'autorisation
	 * @param dateTraitement la date de traitement de l'envoi
	 * @return le document imprimé, en quelque sorte...
	 * @throws EditiqueException en cas de souci éditique
	 * @throws JMSException en cas de souci lié au transport JMS de la demande d'impression et du document imprimé
	 */
	EditiqueResultat imprimeAutorisationRadiationRCOnline(AutorisationRadiationRC lettre, RegDate dateTraitement) throws EditiqueException, JMSException;

	/**
	 * Envoie à l'éditique une lettre de demande de bilan final à imprimer localement
	 * @param lettre la lettre de demande de bilan final
	 * @param dateTraitement la date de traitement de l'envoi
	 * @return le document imprimé, en quelque sorte...
	 * @throws EditiqueException en cas de souci éditique
	 * @throws JMSException en cas de souci lié au transport JMS de la demande d'impression et du document imprimé
	 */
	EditiqueResultat imprimeDemandeBilanFinalOnline(DemandeBilanFinal lettre, RegDate dateTraitement) throws EditiqueException, JMSException;

	/**
	 * Envoie à l'éditique une lettre type de liquidation à imprimer localement
	 * @param lettre la lettre
	 * @param dateTraitement la date de traitement de l'envoi
	 * @return le document imprimé, en quelque sorte...
	 * @throws EditiqueException en cas de souci éditique
	 * @throws JMSException en cas de souci lié au transport JMS de la demande d'impression et du document imprimé
	 */
	EditiqueResultat imprimeLettreTypeInformationLiquidationOnline(LettreTypeInformationLiquidation lettre, RegDate dateTraitement) throws EditiqueException, JMSException;

	/**
	 * Envoie à l'éditique le formulaire de demande de dégrèvement ICI à imprimer en masse
	 * @param demande le formulaire à imprimer
	 * @param dateTraitement la date de traitement de l'envoi
	 * @throws EditiqueException en cas de problème
	 */
	void imprimeDemandeDegrevementICIForBatch(DemandeDegrevementICI demande, RegDate dateTraitement) throws EditiqueException;

	/**
	 * Envoie à l'éditique le formulaire de demande de dégrèvement ICI à imprimer localement
	 * @param demande le formulaire à imprimer
	 * @param dateTraitement la date de traitement de l'envoi
	 * @return le document imprimé, en quelque sorte...
	 * @throws EditiqueException en cas de problème
	 * @throws JMSException en cas de souci lié au transport JMS de la demande d'impression et du document imprimé
	 */
	EditiqueResultat imprimeDemandeDegrevementICIOnline(DemandeDegrevementICI demande, RegDate dateTraitement) throws EditiqueException, JMSException;

	/**
	 * Envoie à l'éditique une fourre neutre à imprimer localement
	 * @param dateTraitement
	 * @return le document imprimé
	 * @throws EditiqueException si l'éditique n'est pas content
	 * @throws JMSException en cas de problème avec l'esb
	 */
	EditiqueResultat imprimerFourreNeutre(FourreNeutre fourreNeutre, RegDate dateTraitement) throws EditiqueException, JMSException;
}
