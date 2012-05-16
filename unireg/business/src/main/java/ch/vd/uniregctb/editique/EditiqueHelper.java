package ch.vd.uniregctb.editique;

import java.rmi.RemoteException;

import noNamespace.InfoArchivageDocument.InfoArchivage;
import noNamespace.InfoDocumentDocument1.InfoDocument;
import noNamespace.InfoDocumentDocument1.InfoDocument.Affranchissement;
import noNamespace.InfoEnteteDocumentDocument1.InfoEnteteDocument;
import noNamespace.InfoEnteteDocumentDocument1.InfoEnteteDocument.Destinataire;
import noNamespace.InfoEnteteDocumentDocument1.InfoEnteteDocument.Expediteur;
import noNamespace.TypAdresse;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureException;
import ch.vd.uniregctb.adresse.AdresseEnvoiDetaillee;
import ch.vd.uniregctb.adresse.AdresseException;
import ch.vd.uniregctb.adresse.AdressesResolutionException;
import ch.vd.uniregctb.declaration.Declaration;
import ch.vd.uniregctb.tiers.CollectiviteAdministrative;
import ch.vd.uniregctb.tiers.Tiers;

public interface EditiqueHelper {

	public static final String DI_ID = "DI_ID";

	public static final String ZONE_AFFRANCHISSEMENT_SUISSE="CH";
	public static final String ZONE_AFFRANCHISSEMENT_EUROPE="EU";
	public static final String ZONE_AFFRANCHISSEMENT_RESTE_MONDE="RM";

	/**
	 * Alimente la partie PorteAdresse du document
	 *
	 * @param tiers
	 * @param infoEnteteDocument
	 * @return
	 * @throws AdressesResolutionException
	 */
	public TypAdresse remplitPorteAdresse(Tiers tiers, InfoEnteteDocument infoEnteteDocument) throws AdresseException;

	/**
	 * Alimente la partie expéditeur ACI du document
	 *
	 * @param infoEnteteDocument
	 * @return
	 * @throws ServiceInfrastructureException
	 * @throws RemoteException
	 * @throws ServiceInfrastructureException
	 * @throws ServiceInfrastructureException
	 */
	public Expediteur remplitExpediteurACI(InfoEnteteDocument infoEnteteDocument) throws ServiceInfrastructureException;

	/**
	 * Alimente la partie expéditeur CAT du document
	 *
	 * @param infoEnteteDocument
	 * @return
	 * @throws ServiceInfrastructureException
	 * @throws RemoteException
	 * @throws ServiceInfrastructureException
	 * @throws ServiceInfrastructureException
	 */
	public Expediteur remplitExpediteurCAT(InfoEnteteDocument infoEnteteDocument) throws ServiceInfrastructureException;


	/**
	 * Alimente la partie Destinataire du document Alimente la partie expéditeur du document
	 *
	 * @param ca
	 * @return
	 * @throws ServiceInfrastructureException
	 */
	public Expediteur remplitExpediteur(CollectiviteAdministrative ca, InfoEnteteDocument infoEnteteDocument) throws ServiceInfrastructureException;

	/**
	 * Alimente la partie Destinataire (contribuable/débiteur) du document
	 *
	 * @param tiers
	 * @param infoEnteteDocument
	 * @return
	 * @throws AdressesResolutionException
	 */
	public Destinataire remplitDestinataire(Tiers tiers, InfoEnteteDocument infoEnteteDocument) throws AdresseException;

	/**
	 * Alimente la partie Destinataire (collectivité administrative) du document
	 *
	 * @param collAdm
	 * @param infoEnteteDocument
	 * @return
	 * @throws AdressesResolutionException
	 */
	public Destinataire remplitDestinataire(CollectiviteAdministrative collAdm, InfoEnteteDocument infoEnteteDocument) throws AdresseException;

	/**
	 * Alimente la partie Destinataire du document avec la mention "Archives"
	 *
	 * @param infoEnteteDocument
	 * @return
	 */
	public Destinataire remplitDestinataireArchives(InfoEnteteDocument infoEnteteDocument);

	/**
	 * Retrouve le nom de la commune à mettre dans le champs OFS des documents de sommation de DI et de confirmation de délai.
	 *
	 * @param di - la di concernée par la sommation ou la demande de délai
	 * @return le nom de la commune
	 */
	public String getCommune(Declaration di) throws EditiqueException;


	/**
	 *  Renseigne la partie expediteur avec les données et régles spécifiques
	 * à l'envoi de sommation de lR
	 *  @param declaration
	 * @param infoEnteteDocument
	 * @param traitePar
	 * @throws ServiceInfrastructureException
	 */
	public Expediteur remplitExpediteurPourSommationLR(Declaration declaration, InfoEnteteDocument infoEnteteDocument, String traitePar) throws ServiceInfrastructureException;

	/**  Renseigne la partie expediteur avec les données et régles spécifiques
	 * à l'envoi de lR
	 * @param declaration
	 * @param infoEnteteDocument
	 * @param traitePar
	 * @return
	 * @throws ServiceInfrastructureException
	 */
	public Expediteur remplitExpediteurPourEnvoiLR(Declaration declaration, InfoEnteteDocument infoEnteteDocument, String traitePar) throws ServiceInfrastructureException;

	/**
	 * Construit une structure éditique pour une demande d'archivage de document lors de sa génération
	 * @param typeDocument le type de document qui nous intéresse
	 * @param noTiers le numéro du tiers concerné par le document
	 * @param cleArchivage la clé d'archivage du document
	 * @param dateTraitement la date de génération du document
	 * @return la structure de demande d'archivage remplie
	 */
	public InfoArchivage buildInfoArchivage(TypeDocumentEditique typeDocument, long noTiers, String cleArchivage, RegDate dateTraitement);

	/**
	 * @return le code du type de dossier à donner au service d'archivage (pour tous les documents gérés par Unireg)
	 */
	public String getTypeDossierArchivage();


	/**
	 * Determine le code affranchissement à ajouter à l'infoDocument à partir du tiers passé en paramètre
	 *@param infoDocument les informations du doccument a envoyer à éditique
	 * @param tiers le tiers concerné par l'envoi
	 * @return l'affranchissement correspondant à l'adresse courrier du tiers
	 * @throws EditiqueException
	 *
	 */

	public Affranchissement getAffranchissement(InfoDocument infoDocument, Tiers tiers) throws EditiqueException;

	/**
	 *  Determine le code affranchissement à ajouter à l'infoDocument à partir de l'adresse passé en paramètre
	 * @param infoDocument les informations du doccument a envoyer à éditique
	 * @param adresseEnvoiDetaillee adresse qui va nous permettre de determiner l'affranchissement
	 * @return l'affranchissement correspondant à l'adresse
	 * @throws EditiqueException
	 */
	public Affranchissement getAffranchissement(InfoDocument infoDocument, AdresseEnvoiDetaillee adresseEnvoiDetaillee) throws EditiqueException;
}
