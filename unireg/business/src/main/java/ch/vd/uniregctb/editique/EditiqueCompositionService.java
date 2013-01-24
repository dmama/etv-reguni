package ch.vd.uniregctb.editique;

import javax.jms.JMSException;
import java.util.Date;
import java.util.List;
import java.util.Set;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.declaration.DeclarationImpotSource;
import ch.vd.uniregctb.declaration.DelaiDeclaration;
import ch.vd.uniregctb.declaration.InformationsDocumentAdapter;
import ch.vd.uniregctb.declaration.ModeleFeuilleDocument;
import ch.vd.uniregctb.declaration.ordinaire.ModeleFeuilleDocumentEditique;
import ch.vd.uniregctb.mouvement.BordereauMouvementDossier;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.type.TypeDocument;

public interface EditiqueCompositionService {

	/**
	 * Imprime la déclaration spécifiée pour une visualisation on-line, et retourne le document imprimé. Il n'y a pas d'envoi vers inbox si c'est trop lent.
	 * <p/>
	 * <b>Note:</b> cette méthode n'envoie pas d'événement fiscal et ne devrait pas être appelée directement. Il faut utiliser la méthode {@link
	 * ch.vd.uniregctb.declaration.ordinaire.DeclarationImpotService#envoiDIOnline(DeclarationImpotOrdinaire, RegDate)}.
	 *
	 * @param declaration   la déclaration d'impôt ordinaire à imprimer
	 * @param dateEvenement la date d'impression
	 * @return le document imprimé
	 */
	EditiqueResultat imprimeDIOnline(DeclarationImpotOrdinaire declaration, RegDate dateEvenement) throws EditiqueException, JMSException;

	/**
	 * Imprime la déclaration spécifiée pour une visualisation on-line et retourne le document imprimé (ou le fait envoyer dans l'inbox si c'est trop lent)
	 * <p/>
	 * <b>Note:</b> cette méthode n'envoie pas d'événement fiscal et ne devrait pas être appelée directement. Il faut utiliser la méthode {@link
	 * ch.vd.uniregctb.declaration.ordinaire.DeclarationImpotService#envoiDuplicataDIOnline(DeclarationImpotOrdinaire, RegDate, TypeDocument, List)}.
	 *
	 * @param declaration   la déclaration d'impôt ordinaire à imprimer
	 * @param dateEvenement la date d'impression
	 * @param typeDocument  le type de document
	 * @param annexes       la liste des annexes
	 * @return le document imprimé
	 */
	EditiqueResultat imprimeDuplicataDIOnline(DeclarationImpotOrdinaire declaration, RegDate dateEvenement, TypeDocument typeDocument, List<ModeleFeuilleDocumentEditique> annexes) throws
			EditiqueException, JMSException;

	/**
	 * Imprime la déclaration spécifiée pour un envoi en masse. Cette méthode retourne immédiatement et du moment que la transaction est committée, il est de la responsabilité d'éditique d'imprimer la
	 * déclaration.
	 * <p/>
	 * <b>Note:</b> cette méthode n'envoie pas d'événement fiscal et ne devrait pas être appelée directement. Il faut utiliser la méthode {@link
	 * ch.vd.uniregctb.declaration.ordinaire.DeclarationImpotService#envoiDIForBatch(DeclarationImpotOrdinaire, RegDate)}.
	 *
	 * @param declaration   la déclaration d'impôt ordinaire à imprimer
	 * @param dateEvenement la date d'impression
	 */
	void imprimeDIForBatch(DeclarationImpotOrdinaire declaration, RegDate dateEvenement) throws EditiqueException;

	/**
	 * Imprime la lr spécifiée pour un envoi en masse. Cette méthode retourne immédiatement et du moment que la transaction est committée, il est de la responsabilité d'éditique d'imprimer la
	 * déclaration.
	 * <p/>
	 * <b>Note:</b> cette méthode n'envoie pas d'événement fiscal et ne devrait pas être appelée directement. Il faut utiliser la méthode {@link
	 * ch.vd.uniregctb.declaration.source.ListeRecapService#imprimerLR(ch.vd.uniregctb.tiers.DebiteurPrestationImposable, ch.vd.registre.base.date.RegDate, ch.vd.registre.base.date.RegDate)}
	 *
	 * @param lr            la LR à imprimer
	 * @param dateEvenement la date d'impression
	 */
	void imprimeLRForBatch(DeclarationImpotSource lr, RegDate dateEvenement) throws EditiqueException;

	/**
	 * Imprime les fourres de nouveaux dossiers
	 *
	 * @param contribuables
	 * @return
	 * @throws EditiqueException
	 */
	EditiqueResultat imprimeNouveauxDossiers(List<Contribuable> contribuables) throws EditiqueException, JMSException;

	/**
	 * Imprime la sommation pour la déclaration spécifiée pour un envoi en masse. Cette méthode retourne immédiatement et du moment que la transaction est committée, il est de la responsabilité
	 * d'éditique d'imprimer la déclaration.
	 * <p/>
	 *
	 * @param declaration   la déclaration d'impôt ordinaire dont la sommation est à imprimer
	 * @param dateEvenement la date d'impression
	 */
	void imprimeSommationDIForBatch(DeclarationImpotOrdinaire declaration, boolean miseSousPliImpossible, RegDate dateEvenement) throws EditiqueException;

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
	 * Imprime la sommation pour la déclaration spécifiée on-line.
	 *
	 * @param declaration
	 * @param dateEvenement
	 * @return l'id du document
	 * @throws EditiqueException
	 */
	EditiqueResultat imprimeSommationDIOnline(DeclarationImpotOrdinaire declaration, RegDate dateEvenement) throws EditiqueException, JMSException;

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
	 * Imprime la confirmation de délai pour la {@link DeclarationImpotOrdinaire} et le {@link ch.vd.uniregctb.declaration.DelaiDeclaration} spécifié
	 *
	 * @param di
	 * @param delai
	 * @return
	 * @throws EditiqueException
	 */
	EditiqueResultat imprimeConfirmationDelaiOnline(DeclarationImpotOrdinaire di, DelaiDeclaration delai) throws EditiqueException, JMSException;

	/**
	 * Imprime la liste récapitulative spécifiée on-line
	 *
	 * @param lr
	 * @param dateEvenement
	 * @param typeDocument
	 * @return
	 * @throws EditiqueException
	 */
	EditiqueResultat imprimeLROnline(DeclarationImpotSource lr, RegDate dateEvenement, TypeDocument typeDocument) throws EditiqueException, JMSException;

	/**
	 * Envoie à l'éditique le bordereau de mouvements de dossiers correspondant aux mouvement donnés
	 *
	 * @param bordereauMouvementDossier les mouvements qui doivent apparaître sur le bordereau
	 * @return l'identifiant du document (pour le récupérer ensuite)
	 */
	EditiqueResultat envoyerImpressionLocaleBordereau(BordereauMouvementDossier bordereauMouvementDossier) throws EditiqueException, JMSException;


	/**Imprime les document de confirmation ou de mise en attente lors d'une demande d'inscription à la E-facture
	 *
	 *
	 *
	 *
	 *
	 * @param tiers a qui le document doit être envoyé
	 * @param typeDoc permet de determiner le type de document a imprimer
	 * @param dateTraitement date du traitement
	 * @param dateDemande
	 * @throws EditiqueException
	 *
	 */
	String imprimeDocumentEfacture(Tiers tiers, TypeDocument typeDoc, Date dateTraitement, RegDate dateDemande) throws EditiqueException, JMSException;



	/**
	 * Envoie à l'éditique le formulaire immeuble à imprimer en masse
	 *
	 * @param infosDocument
	 * @param listeModele
	 * @param dateEvenement
	 * @param nombreAnnexesImmeuble
	 * @throws EditiqueException
	 */
	public int imprimeAnnexeImmeubleForBatch(InformationsDocumentAdapter infosDocument, Set<ModeleFeuilleDocument> listeModele, RegDate dateEvenement, int nombreAnnexesImmeuble) throws EditiqueException;

}
