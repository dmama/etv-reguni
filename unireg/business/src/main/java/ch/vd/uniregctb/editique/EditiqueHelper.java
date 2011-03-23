package ch.vd.uniregctb.editique;

import java.rmi.RemoteException;

import noNamespace.TypAdresse;
import noNamespace.InfoEnteteDocumentDocument1.InfoEnteteDocument;
import noNamespace.InfoEnteteDocumentDocument1.InfoEnteteDocument.Destinataire;
import noNamespace.InfoEnteteDocumentDocument1.InfoEnteteDocument.Expediteur;

import ch.vd.infrastructure.service.InfrastructureException;
import ch.vd.uniregctb.adresse.AdresseException;
import ch.vd.uniregctb.adresse.AdressesResolutionException;
import ch.vd.uniregctb.declaration.Declaration;
import ch.vd.uniregctb.tiers.CollectiviteAdministrative;
import ch.vd.uniregctb.tiers.Tiers;

public interface EditiqueHelper {

	public static final String DI_ID = "DI_ID";

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
	 * @throws InfrastructureException
	 * @throws RemoteException
	 * @throws InfrastructureException
	 * @throws InfrastructureException
	 */
	public Expediteur remplitExpediteurACI(InfoEnteteDocument infoEnteteDocument) throws InfrastructureException;

	/**
	 * Alimente la partie expéditeur CAT du document
	 *
	 * @param infoEnteteDocument
	 * @return
	 * @throws InfrastructureException
	 * @throws RemoteException
	 * @throws InfrastructureException
	 * @throws InfrastructureException
	 */
	public Expediteur remplitExpediteurCAT(InfoEnteteDocument infoEnteteDocument) throws InfrastructureException;


	/**
	 * Alimente la partie Destinataire du document Alimente la partie expéditeur du document
	 *
	 * @param ca
	 * @return
	 * @throws InfrastructureException
	 */
	public Expediteur remplitExpediteur(CollectiviteAdministrative ca, InfoEnteteDocument infoEnteteDocument) throws InfrastructureException;

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
	 * @throws InfrastructureException
	 */
	public Expediteur remplitExpediteurPourSommationLR(Declaration declaration, InfoEnteteDocument infoEnteteDocument, String traitePar) throws InfrastructureException;

	/**  Renseigne la partie expediteur avec les données et régles spécifiques
	 * à l'envoi de lR
	 * @param declaration
	 * @param infoEnteteDocument
	 * @param traitePar
	 * @return
	 * @throws InfrastructureException
	 */
	public Expediteur remplitExpediteurPourEnvoiLR(Declaration declaration, InfoEnteteDocument infoEnteteDocument, String traitePar) throws InfrastructureException;

}
