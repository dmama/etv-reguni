package ch.vd.unireg.editique;

import java.rmi.RemoteException;

import noNamespace.InfoArchivageDocument.InfoArchivage;
import noNamespace.InfoDocumentDocument1.InfoDocument;
import noNamespace.InfoEnteteDocumentDocument1.InfoEnteteDocument;
import noNamespace.InfoEnteteDocumentDocument1.InfoEnteteDocument.Destinataire;
import noNamespace.InfoEnteteDocumentDocument1.InfoEnteteDocument.Expediteur;
import noNamespace.TypAdresse;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureException;
import ch.vd.unireg.adresse.AdresseEnvoi;
import ch.vd.unireg.adresse.AdresseEnvoiDetaillee;
import ch.vd.unireg.adresse.AdresseException;
import ch.vd.unireg.adresse.AdressesResolutionException;
import ch.vd.unireg.declaration.Declaration;
import ch.vd.unireg.tiers.CollectiviteAdministrative;
import ch.vd.unireg.tiers.Tiers;

/**
 * Interface du Helper éditique pour le bon vieux temps où les XSD éditiques étaient gérées par XmlBeans,
 * sans namespace (i.e. tous les documents d'avant l'avènement des PM)...
 */
public interface LegacyEditiqueHelper {

	/**
	 * Alimente la partie PorteAdresse du document
	 *
	 * @param tiers
	 * @param infoEnteteDocument
	 * @return
	 * @throws AdressesResolutionException
	 */
	TypAdresse remplitPorteAdresse(Tiers tiers, InfoEnteteDocument infoEnteteDocument) throws AdresseException;

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
	Expediteur remplitExpediteurACI(InfoEnteteDocument infoEnteteDocument) throws ServiceInfrastructureException;

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
	Expediteur remplitExpediteurCAT(InfoEnteteDocument infoEnteteDocument) throws ServiceInfrastructureException;


	/**
	 * Alimente la partie Destinataire du document Alimente la partie expéditeur du document
	 *
	 * @param ca
	 * @return
	 * @throws ServiceInfrastructureException
	 */
	Expediteur remplitExpediteur(CollectiviteAdministrative ca, InfoEnteteDocument infoEnteteDocument) throws ServiceInfrastructureException;

	/**
	 * Alimente la partie Destinataire (contribuable/débiteur) du document
	 *
	 * @param tiers
	 * @param infoEnteteDocument
	 * @return
	 * @throws AdressesResolutionException
	 */
	Destinataire remplitDestinataire(Tiers tiers, InfoEnteteDocument infoEnteteDocument) throws AdresseException;

	/**
	 * Alimente la partie Destinataire (collectivité administrative) du document
	 *
	 * @param collAdm
	 * @param infoEnteteDocument
	 * @return
	 * @throws AdressesResolutionException
	 */
	Destinataire remplitDestinataire(CollectiviteAdministrative collAdm, InfoEnteteDocument infoEnteteDocument) throws AdresseException;

	/**
	 * Alimente la partie Destinataire du document avec la mention "Archives"
	 *
	 * @param infoEnteteDocument
	 * @return
	 */
	Destinataire remplitDestinataireArchives(InfoEnteteDocument infoEnteteDocument);

	/**
	 * Retrouve le nom de la commune à mettre dans le champs OFS des documents de sommation de DI et de confirmation de délai.
	 *
	 * @param di - la di concernée par la sommation ou la demande de délai
	 * @return le nom de la commune
	 */
	String getCommune(Declaration di) throws EditiqueException;


	/**
	 *  Renseigne la partie expediteur avec les données et régles spécifiques
	 * à l'envoi de sommation de lR
	 *  @param declaration
	 * @param infoEnteteDocument
	 * @param traitePar
	 * @throws ServiceInfrastructureException
	 */
	Expediteur remplitExpediteurPourSommationLR(Declaration declaration, InfoEnteteDocument infoEnteteDocument, String traitePar) throws ServiceInfrastructureException;

	/**  Renseigne la partie expediteur avec les données et régles spécifiques
	 * à l'envoi de lR
	 * @param declaration
	 * @param infoEnteteDocument
	 * @param traitePar
	 * @return
	 * @throws ServiceInfrastructureException
	 */
	Expediteur remplitExpediteurPourEnvoiLR(Declaration declaration, InfoEnteteDocument infoEnteteDocument, String traitePar) throws ServiceInfrastructureException;

	/**
	 * Construit une structure éditique pour une demande d'archivage de document lors de sa génération
	 *
	 * @param typeDocument le type de document qui nous intéresse
	 * @param noTiers le numéro du tiers concerné par le document
	 * @param cleArchivage la clé d'archivage du document
	 * @param dateTraitement la date de génération du document
	 * @return la structure de demande d'archivage remplie
	 */
	InfoArchivage buildInfoArchivage(TypeDocumentEditique typeDocument, long noTiers, String cleArchivage, RegDate dateTraitement);

	/**
	 * Remplit une structure éditique pour une demande d'archivage de document lors de sa génération
	 *
	 * @param infoArchivage  la structure à remplir
	 * @param typeDocument   le type de document qui nous intéresse
	 * @param noTiers        le numéro du tiers concerné par le document
	 * @param cleArchivage   la clé d'archivage du document
	 * @param dateTraitement la date de génération du document
	 * @return la structure de demande d'archivage remplie
	 */
	InfoArchivage fillInfoArchivage(InfoArchivage infoArchivage, TypeDocumentEditique typeDocument, long noTiers, String cleArchivage, RegDate dateTraitement);

	/**
	 * Determine le code affranchissement à ajouter à l'infoDocument à partir du tiers passé en paramètre
	 * @param infoDocument les informations du doccument a envoyer à éditique
	 * @param tiers le tiers concerné par l'envoi
	 * @return l'affranchissement correspondant à l'adresse
	 * @throws EditiqueException
	 *
	 */
	ZoneAffranchissementEditique remplitAffranchissement(InfoDocument infoDocument, Tiers tiers) throws EditiqueException;

	/**
	 *  Determine le code affranchissement à ajouter à l'infoDocument à partir de l'adresse passé en paramètre
	 * @param infoDocument les informations du doccument a envoyer à éditique
	 * @param adresseEnvoiDetaillee adresse qui va nous permettre de determiner l'affranchissement
	 * @return l'affranchissement correspondant à l'adresse
	 * @throws EditiqueException
	 */
	ZoneAffranchissementEditique remplitAffranchissement(InfoDocument infoDocument, AdresseEnvoiDetaillee adresseEnvoiDetaillee) throws EditiqueException;

	/**
	 * Remplit un objet de type {@link TypAdresse.Adresse} avec les données d'un objet du type {@link AdresseEnvoi}
	 *
	 * @param adresseEnvoiSource la source
	 * @param adresseCible la cible
	 *
	 * @return la cible
	 */
	TypAdresse.Adresse remplitAdresse(AdresseEnvoi adresseEnvoiSource, TypAdresse.Adresse adresseCible);

}
