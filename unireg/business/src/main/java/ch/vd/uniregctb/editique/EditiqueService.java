package ch.vd.uniregctb.editique;

import java.util.List;

import javax.jms.JMSException;

import ch.vd.editique.service.enumeration.TypeFormat;
import ch.vd.infrastructure.service.InfrastructureException;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.declaration.DeclarationImpotSource;
import ch.vd.uniregctb.declaration.DelaiDeclaration;
import ch.vd.uniregctb.declaration.ordinaire.DeclarationImpotService;
import ch.vd.uniregctb.declaration.ordinaire.ModeleFeuilleDocumentEditique;
import ch.vd.uniregctb.mouvement.BordereauMouvementDossier;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.type.TypeDocument;

/**
 * Service Editique. Ce service est dédié à la communication avec le service Editique permettant l'impression des divers documents.
 * <p>
 * <b>Note : </b>La delegation est obligatoire pour démarrer la réception des documents afin de synchronizer les documents reçus et le
 * traitement ä effectuer sur ces documents. Lors de l'obtention de ce service, vous devrez uliser la méthode
 * {@link #setDelegate(DelegateEditique)} en lui passant le processus de traitement.
 * </p>
 *
 * @author xcifwi (last modified by $Author: xcicfh $ @ $Date: 2007/08/15 06:14:15 $)
 * @version $Revision: 1.8 $
 */
public interface EditiqueService {

	/**
	 * mime pour une fichier PDF
	 */
	String PDF_MIME = "application/pdf";

	/**
	 * Sérialise au format XML et transmet l'object en paramètre au service Editique JMS d'impression directe.
	 *
	 * @param object
	 *            l'object à sérialiser et à transmettre.
	 * @param nomDocument
	 *            le nom du document à transmettre à Editique.
	 * @param archive
	 * 			  indicateur d'archivage
	 * @throws EditiqueException
	 *             si un probl�me survient durant la génération du XML ou durant la transmission du message au serveur JMS.
	 */
	String creerDocumentImmediatement(String nomDocument, String typeDocument, TypeFormat typeFormat, Object object, boolean archive) throws EditiqueException;

	/**
	 * Sérialise au format XML et transmet l'object en param�tre au service Editique JMS d'impression de masse.
	 *
	 * @param object
	 *            l'object à sérialiser et à transmettre.
	 * @param typeDocument
	 *            le type de document
	 * @param archive
	 * 			  indicateur d'archivage
	 * @throws EditiqueException
	 *             si un problème survient durant la génération du XML ou durant la transmission du message au serveur JMS.
	 */
	void creerDocumentParBatch(Object object, String typeDocument, boolean archive) throws EditiqueException;

	/**
	 * Obtient le document après avoir appelé la méthode {@link #creerDocumentImmediatement(String, Object)}.
	 *
	 * @param docId
	 *            l'id du document d'impression.
	 * @param appliqueDelai
	 *            indique si la méthode doit attendre le document.
	 * @return Retourne le resultat de la creation du document, sinon retourne <b>null</b>.
	 */
	EditiqueResultat getDocument(String docId, boolean appliqueDelai) throws JMSException;

	/**
	 * Obitent un document pdf, sous forme binaire, identifié par les différents paramètres.
	 *
	 * @param noContribuable
	 *            l'identifiant du contribuable.
	 * @param typeDocument
	 *            le type de document.
	 * @param nomDocument
	 *            le nom du document.
	 * @return un document pdf, sous forme binaire.
	 * @throws ExchangeBusinessException
	 *             si un problème survient lors de l'exécution du traitement.
	 */
	byte[] getPDFDocument(Long noContribuable, String typeDocument, String nomDocument) throws EditiqueException;

	/**
	 * Obtient le temps d'attente en seconde lors de la réception d'un document.
	 *
	 * @return Retourne le temps d'attente en seconde lors de la réception d'un document.
	 */
	int getReceiveTimeout();

	/**
	 * Imprime la déclaration spécifiée pour une visualisation on-line. Cette méthode retourne immédiatement, le document d'impression doit
	 * être récupéré par un appel à la méthode {@link #getDocument(String, boolean)}.
	 * <p>
	 * <b>Note:</b> cette méthode n'envoie pas d'événement fiscal et ne devrait pas être appelée directement. Il faut utiliser la méthode
	 * {@link DeclarationImpotService#envoiDIOnline()}.
	 *
	 * @param declaration
	 *            la déclaration d'impôt ordinaire à imprimer
	 * @param dateEvenement
	 *            la date d'impression
	 * @return l'ID du document d'impression
	 * @see #getDocument(String, boolean)
	 */
	public String imprimeDIOnline(DeclarationImpotOrdinaire declaration, RegDate dateEvenement) throws EditiqueException;

	/**
	 * Imprime la déclaration spécifiée pour une visualisation on-line. Cette méthode retourne immédiatement, le document d'impression doit
	 * être récupéré par un appel à la méthode {@link #getDocument(String, boolean)}.
	 * <p>
	 * <b>Note:</b> cette méthode n'envoie pas d'événement fiscal et ne devrait pas être appelée directement. Il faut utiliser la méthode
	 * {@link DeclarationImpotService#envoiDIOnline()}.
	 *
	 * @param declaration
	 *            la déclaration d'impôt ordinaire à imprimer
	 * @param dateEvenement
	 *            la date d'impression
	 * @param typeDocument
	 *            le type de document
	 * @param annexes
	 *            la liste des annexes
	 * @param isDuplicata
	 * 				true si impression d'un duplicata
	 * @return l'ID du document d'impression
	 * @see #getDocument(String, boolean)
	 */
	public String imprimeDIOnline(DeclarationImpotOrdinaire declaration, RegDate dateEvenement, TypeDocument typeDocument,
			List<ModeleFeuilleDocumentEditique> annexes, boolean isDuplicata) throws EditiqueException;

	/**
	 * Imprime la déclaration spécifiée pour un envoi en masse. Cette méthode retourne immédiatement et du moment que la transaction est
	 * committée, il est de la responsabilité d'éditique d'imprimer la déclaration.
	 * <p>
	 * <b>Note:</b> cette méthode n'envoie pas d'événement fiscal et ne devrait pas être appelée directement. Il faut utiliser la méthode
	 * {@link DeclarationImpotService#envoiDIForBatch()}.
	 *
	 * @param declaration
	 *            la déclaration d'impôt ordinaire à imprimer
	 * @param dateEvenement
	 *            la date d'impression
	 */
	public void imprimeDIForBatch(DeclarationImpotOrdinaire declaration, RegDate dateEvenement) throws EditiqueException;

	/**
	 * Imprime la lr spécifiée pour un envoi en masse. Cette méthode retourne immédiatement et du moment que la transaction est
	 * committée, il est de la responsabilité d'éditique d'imprimer la déclaration.
	 * <p>
	 * <b>Note:</b> cette méthode n'envoie pas d'événement fiscal et ne devrait pas être appelée directement. Il faut utiliser la méthode
	 * {@link DeclarationImpotService#envoiDIForBatch()}.
	 * @param lr
	 *            la LR à imprimer
	 * @param dateEvenement
	 *            la date d'impression
	 *
	 */
	public void imprimeLRForBatch(DeclarationImpotSource lr, RegDate dateEvenement) throws EditiqueException ;

	/**
	 * Imprime un nouveau dossier
	 *
	 * @param contribuables
	 * @return
	 * @throws EditiqueException
	 * @throws InfrastructureException
	 */
	public String imprimeNouveauxDossiers(List<Contribuable> contribuables) throws EditiqueException, InfrastructureException ;

	/**
	 * Imprime la sommation pour la déclaration spécifiée pour un envoi en masse. Cette méthode retourne immédiatement et du moment que la transaction est
	 * committée, il est de la responsabilité d'éditique d'imprimer la déclaration.
	 * <p>
	 *
	 * @param declaration
	 *            la déclaration d'impôt ordinaire dont la sommation est à imprimer
	 * @param dateEvenement
	 *            la date d'impression
	 */
	public void imprimeSommationDIForBatch(DeclarationImpotOrdinaire declaration, boolean miseSousPliImpossible, RegDate dateEvenement) throws EditiqueException;

	/**
	 * Imprime la sommation pour la LR spécifiée pour un envoi en masse. Cette méthode retourne immédiatement et du moment que la transaction est
	 * committée, il est de la responsabilité d'éditique d'imprimer la déclaration.
	 * @param lr
	 * @param dateEvenement
	 * @throws EditiqueException
	 */
	public void imprimeSommationLRForBatch(DeclarationImpotSource lr, RegDate dateEvenement) throws EditiqueException ;

	/**
	 * Imprime la sommation pour la déclaration spécifiée on-line.
	 *
	 * @param declaration
	 * @param dateEvenement
	 * @throws EditiqueException
	 * @return l'id du document
	 */
	public String imprimeSommationDIOnline(DeclarationImpotOrdinaire declaration, RegDate dateEvenement) throws EditiqueException ;

	/**
	 * Imprime la sommation pour la lr spécifiée on-line.
	 *
	 * @param lr
	 * @param dateEvenement
	 * @return
	 * @throws EditiqueException
	 */
	public String imprimeSommationLROnline(DeclarationImpotSource lr, RegDate dateEvenement) throws EditiqueException ;

	/**
	 * Imprime la confirmation de délai pour la {@link DeclarationImpotOrdinaire} et le {@link DelaiDeclaration} spécifié
	 *
	 * @param di
	 * @param delai
	 * @return
	 * @throws EditiqueException
	 */
	public String imprimeConfirmationDelaiOnline(DeclarationImpotOrdinaire di, DelaiDeclaration delai) throws EditiqueException;

	/**
	 * Imprime la liste récapitulative spécifiée on-line
	 *
	 * @param lr
	 * @param dateEvenement
	 * @param typeDocument
	 * @return
	 * @throws EditiqueException
	 */
	public String imprimeLROnline(DeclarationImpotSource lr, RegDate dateEvenement, TypeDocument typeDocument) throws EditiqueException ;

	/**
	 * Imprime une chemise de taxation d'office on-line
	 *
	 * @param contribuable
	 * @return
	 * @throws EditiqueException
	 * @throws InfrastructureException
	 */
	public String imprimeTaxationOfficeOnline(DeclarationImpotOrdinaire declaration) throws EditiqueException ;

	/**
	 * Imprime une chemise de taxation d'office en batch
	 *
	 * @param declaration
	 * @throws EditiqueException
	 */
	public void imprimeTaxationOfficeBatch(DeclarationImpotOrdinaire declaration) throws EditiqueException;

	/**
	 * Envoie à l'éditique le bordereau de mouvements de dossiers
	 * correspondant aux mouvement donnés
	 * @param bordereauMouvementDossier les mouvements qui doivent apparaître sur le bordereau
	 * @return l'identifiant du document (pour le récupérer ensuite)
	 */
	String envoyerImpressionLocaleBordereau(BordereauMouvementDossier bordereauMouvementDossier) throws EditiqueException;

}
