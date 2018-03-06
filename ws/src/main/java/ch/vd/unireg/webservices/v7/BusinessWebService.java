package ch.vd.unireg.webservices.v7;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.avatar.ImageData;
import ch.vd.unireg.indexer.IndexerException;
import ch.vd.unireg.webservices.common.AccessDeniedException;
import ch.vd.unireg.ws.ack.v7.OrdinaryTaxDeclarationAckRequest;
import ch.vd.unireg.ws.ack.v7.OrdinaryTaxDeclarationAckResponse;
import ch.vd.unireg.ws.deadline.v7.DeadlineRequest;
import ch.vd.unireg.ws.deadline.v7.DeadlineResponse;
import ch.vd.unireg.ws.fiscalevents.v7.FiscalEvents;
import ch.vd.unireg.ws.landregistry.v7.BuildingList;
import ch.vd.unireg.ws.landregistry.v7.CommunityOfOwnersList;
import ch.vd.unireg.ws.landregistry.v7.ImmovablePropertyList;
import ch.vd.unireg.ws.landregistry.v7.ImmovablePropertySearchResult;
import ch.vd.unireg.ws.modifiedtaxpayers.v7.PartyNumberList;
import ch.vd.unireg.ws.parties.v7.Parties;
import ch.vd.unireg.ws.security.v7.SecurityListResponse;
import ch.vd.unireg.ws.security.v7.SecurityResponse;
import ch.vd.unireg.xml.ServiceException;
import ch.vd.unireg.xml.infra.taxoffices.v1.TaxOffices;
import ch.vd.unireg.xml.party.communityofheirs.v1.CommunityOfHeirs;
import ch.vd.unireg.xml.party.landregistry.v1.Building;
import ch.vd.unireg.xml.party.landregistry.v1.CommunityOfOwners;
import ch.vd.unireg.xml.party.landregistry.v1.ImmovableProperty;
import ch.vd.unireg.xml.party.v5.Party;
import ch.vd.unireg.xml.party.v5.PartyInfo;
import ch.vd.unireg.xml.party.v5.PartyPart;
import ch.vd.unireg.xml.party.withholding.v1.DebtorCategory;
import ch.vd.unireg.xml.party.withholding.v1.DebtorInfo;

/**
 * Partie purement métier du traitement des appels web-service v7
 */
public interface BusinessWebService {

	/**
	 * Quels sont les droits d'accès de l'utilisateur donné par son visa sur le tiers donné par son numéro
	 *
	 * @param user    visa de l'utilisateur
	 * @param partyNo numéro du tiers
	 * @return le droit d'accès possible (voir champ {@link SecurityResponse#allowedAccess})
	 */
	SecurityResponse getSecurityOnParty(String user, int partyNo);

	/**
	 * Quels sont les droits d'accès de l'utilisateur donné par son visa sur plusieurs tiers
	 *
	 * @param user     visa de l'utilisateur
	 * @param partyNos des numéros de tiers
	 * @return la list des droits d'accès possible
	 */
	SecurityListResponse getSecurityOnParties(@NotNull String user, @NotNull List<Integer> partyNos);

	/**
	 * Modification du flag de blocage des remboursements automatiques sur un tiers donné
	 *
	 * @param partyNo numéro du tiers
	 * @param blocked nouvel état du flag
	 * @throws AccessDeniedException si l'opérateur courant n'a pas le droit de faire ça
	 */
	void setAutomaticRepaymentBlockingFlag(int partyNo, boolean blocked) throws AccessDeniedException;

	/**
	 * Interrogation du flag de blocage de remboursement automatique sur un tiers donné
	 *
	 * @param partyNo numéro du tiers
	 * @return <code>true</code> si le blocage est actif, <code>false</code> sinon
	 * @throws AccessDeniedException si l'opérateur courant n'a pas le droit de lecture sur le dossier
	 */
	boolean getAutomaticRepaymentBlockingFlag(int partyNo) throws AccessDeniedException;

	/**
	 * Quittancement d'une série de déclarations d'impôt
	 *
	 * @param request information sur les déclarations à quittancer
	 * @return un statut sur les quittancements opérés
	 * @throws AccessDeniedException si l'opérateur courant n'a pas le droit de faire ça
	 */
	OrdinaryTaxDeclarationAckResponse ackOrdinaryTaxDeclarations(OrdinaryTaxDeclarationAckRequest request) throws AccessDeniedException;

	/**
	 * Demande de nouveau délai de dépôt pour une déclaration d'impôt
	 *
	 * @param partyNo numéro du contribuable auquel un nouveau délai a été accordé
	 * @param pf      période fiscale de la déclaration concernée par le délai
	 * @param seqNo   numéro de séquence de la déclaration concernée par le délai
	 * @param request informations sur les dates du délai demandé
	 * @return un statut sur le délai demandé
	 * @throws AccessDeniedException si l'opérateur courant n'a pas le droit de faire ça
	 */
	DeadlineResponse newOrdinaryTaxDeclarationDeadline(int partyNo, int pf, int seqNo, DeadlineRequest request) throws AccessDeniedException;

	/**
	 * Récupère les offices d'impôt de la commune identifiée par son numéro OFS et la date de validité
	 *
	 * @param municipalityId numéro OFS de la commune
	 * @param date           date de validité du numéro OFS (si <code>null</code>, on prendra la date du jour)
	 * @return une description des offices d'impôt (de district et de région) liés à la commune indiquée
	 * @throws ch.vd.unireg.common.ObjectNotFoundException si la commune est inconnue ou n'est pas vaudoise
	 */
	TaxOffices getTaxOffices(int municipalityId, @Nullable RegDate date);

	/**
	 * Récupère une liste des numéros de contribuables modifiés entre les date données (toutes deux comprises)
	 *
	 * @param since <i>timestamp</i> du début de la période interessante
	 * @param until <i>timestamp</i> de la fin de la période intéressante
	 * @return la liste des numéros des contribuables modifiés
	 * @throws AccessDeniedException si l'opérateur courant n'a pas accès à cette liste
	 */
	PartyNumberList getModifiedTaxPayers(Date since, Date until) throws AccessDeniedException;

	/**
	 * Récupère un état des LR émises et à émettre sur le débiteur de prestation imposable indiqué pour une période fiscale donnée
	 *
	 * @param debtorNo numéro du débiteur concerné par la demande
	 * @param pf       période fiscale d'intérêt
	 * @return une descriptions des listes émises et restant à émettre pour ce débiteur et cette période fiscale
	 * @throws AccessDeniedException si l'opérateur courant n'a pas accès à cette information
	 */
	DebtorInfo getDebtorInfo(int debtorNo, int pf) throws AccessDeniedException;

	/**
	 * Effectue une recherche de tiers selon les critères donnés
	 *
	 * @param partyNo                    [optionnel] le numéro du tiers
	 * @param name                       [optionnel] un ou plusieurs éléments du nom (de la raison sociale) du tiers
	 * @param nameSearchMode             mode d'interprétation du contenu du champ "name"
	 * @param townOrCountry              [optionnel] nom de la localité ou du pays du tiers recherché (adresse courrier)
	 * @param dateOfBirth                [optionnel] date de naissance (peut être partielle) du tiers recherché
	 * @param socialInsuranceNumber      [optionnel] numéro AVS (à 11 ou 13 positions) du tiers recherché
	 * @param uidNumber                  [optionnel] numéro IDE associé au tiers recherché
	 * @param taxResidenceFSOId          [optionnel] numéro OFS du for du tiers recherché
	 * @param onlyActiveMainTaxResidence [optionnel] si le critère du fors est renseigné, impose une contrainte sur la présence d'un for principal actif
	 * @param partyTypes                 [optionnel] ensemble des types de tiers recherchés (si vide ou <code>null</code>, tous les types seront considérés)
	 * @param debtorCategory             [optionnel] catégorie de débiteur recherchée
	 * @param activeParty                [optionnel] vrai s'il ne faut retourner que les tiers actifs
	 * @param oldWithholdingNumber       [optionnel] ancien numéro de sourcier du tiers recherché
	 * @return les informations sur les tiers correspondants aux critères donnés
	 * @throws AccessDeniedException si l'opérateur courant n'a pas le droit de faire des recherches de tiers
	 * @throws IndexerException      si une erreur est levée dans la recherche (critères vides, trop de résultats...)
	 */
	List<PartyInfo> searchParty(@Nullable String partyNo, @Nullable String name, SearchMode nameSearchMode, @Nullable String townOrCountry,
	                            @Nullable RegDate dateOfBirth, @Nullable String socialInsuranceNumber, @Nullable String uidNumber, @Nullable Integer taxResidenceFSOId,
	                            boolean onlyActiveMainTaxResidence, @Nullable Set<PartySearchType> partyTypes, @Nullable DebtorCategory debtorCategory, @Nullable Boolean activeParty,
	                            @Nullable Long oldWithholdingNumber) throws AccessDeniedException, IndexerException;

	/**
	 * Récupère les informations sur le tiers demandé
	 *
	 * @param partyNo numéro du tiers
	 * @param parts   [optionnel] ensemble des parts supplémentaires qui intéressent l'appelant
	 * @return les informations demandées sur le tiers
	 * @throws AccessDeniedException si l'opérateur courant n'a pas le droit de faire ce genre de recherche
	 * @throws ServiceException      en cas de problème à la constitution de l'entité à exporter
	 */
	@Nullable
	Party getParty(int partyNo, @Nullable Set<PartyPart> parts) throws AccessDeniedException, ServiceException;

	/**
	 * Récupère les informations sur les tiers demandés
	 *
	 * @param partyNos numéro des tiers
	 * @param parts    [optionnel] ensemble des parts supplémentaires qui intéressent l'appelant
	 * @return les informations demandées sur les tiers (<code>null</code> si la liste d'identifiants en entrée était vide)
	 * @throws AccessDeniedException si l'opérateur courant n'a pas le droit de faire ce genre de recherche
	 * @throws ServiceException      en cas de problème à la constitution de l'entité à exporter
	 */
	@NotNull
	Parties getParties(List<Integer> partyNos, @Nullable Set<PartyPart> parts) throws AccessDeniedException, ServiceException;

	/**
	 * @param deceasedId le numéro de contribuable du décédé
	 * @return la communité d'héritiers, ou <i>null</i> si le contribuable n'existe pas ou ne possède pas d'héritiers.
	 * @throws AccessDeniedException si l'opérateur courant n'a pas le droit de faire ce genre de recherche
	 * @throws ServiceException      en cas de problème à la constitution de l'entité à exporter
	 */
	CommunityOfHeirs getCommunityOfHeirs(int deceasedId) throws AccessDeniedException, ServiceException;

	/**
	 * Récupère l'image de l'avatar du tiers demandé
	 *
	 * @param partyNo numéro du tiers
	 * @return l'image de l'avatar
	 * @throws ServiceException en cas de problème
	 */
	ImageData getAvatar(int partyNo) throws ServiceException;

	/**
	 * Récupère les événements fiscaux liés au tiers donné
	 *
	 * @param partyNo numéro du tiers
	 * @return les événements fiscaux liés au tiers
	 * @throws AccessDeniedException si l'opérateur courant n'a pas le droit de faire ce genre de recherche
	 */
	FiscalEvents getFiscalEvents(int partyNo) throws AccessDeniedException;

	/**
	 * @param immoId l'id technique Unireg de l'immeuble.
	 * @return un immmeuble du registre foncier avec son historique; ou <b>null</b> si l'immeuble est inconnu.
	 * @throws AccessDeniedException si l'opérateur courant n'a pas le droit de voir les immeubles.
	 */
	@Nullable
	ImmovableProperty getImmovableProperty(long immoId) throws AccessDeniedException;

	/**
	 * @param municipalityFsoId le numéro OFS de la commune de l'immeuble (obligatoire)
	 * @param parcelNumber      le numéro de parcelle de l'immeuble (obligatoire)
	 * @param index1            l'index n°1 (optionnel, si pas renseigné retourne l'immeuble avec un index1 nul)
	 * @param index2            l'index n°2 (optionnel, si pas renseigné retourne l'immeuble avec un index2 nul)
	 * @param index3            l'index n°3 (optionnel, si pas renseigné retourne l'immeuble avec un index3 nul)
	 * @return l'immeuble correspondant ou null si aucun immeuble ne correspond.
	 */
	@Nullable
	ImmovableProperty getImmovablePropertyByLocation(int municipalityFsoId, int parcelNumber, @Nullable Integer index1, @Nullable Integer index2, @Nullable Integer index3) throws AccessDeniedException;

	/**
	 * Recherche un ou plusieurs immeubles en fonction de plusieurs critères.
	 *
	 * @param municipalityFsoId le numéro OFS de la commune de l'immeuble (obligatoire)
	 * @param parcelNumber      le numéro de parcelle de l'immeuble (obligatoire)
	 * @param index1            l'index n°1 (optionnel)
	 * @param index2            l'index n°2 (optionnel)
	 * @param index3            l'index n°3 (optionnel)
	 * @return une liste d'immeubles correspondant à la demande
	 */
	@NotNull
	ImmovablePropertySearchResult findImmovablePropertyByLocation(int municipalityFsoId, int parcelNumber, @Nullable Integer index1, @Nullable Integer index2, @Nullable Integer index3) throws AccessDeniedException;

	/**
	 * @param immoIds les ids techniques Unireg des immeubles.
	 * @return la liste des immmeubles du registre foncier avec leurs historiques.
	 * @throws AccessDeniedException si l'opérateur courant n'a pas le droit de voir les immeubles.
	 */
	@NotNull
	ImmovablePropertyList getImmovableProperties(List<Long> immoIds) throws AccessDeniedException;

	/**
	 * @param buildingId l'id technique Unireg du bâtiment.
	 * @return un bâtiment du registre foncier avec son historique; ou <b>null</b> si le bâtiment est inconnu.
	 * @throws AccessDeniedException si l'opérateur courant n'a pas le droit de voir les immeubles.
	 */
	@Nullable
	Building getBuilding(long buildingId) throws AccessDeniedException;

	/**
	 * @param buildingIds les ids techniques Unireg des bâtiments.
	 * @return une liste de bâtiments du registre foncier avec son historique
	 * @throws AccessDeniedException si l'opérateur courant n'a pas le droit de voir les immeubles.
	 */
	@NotNull
	BuildingList getBuildings(List<Long> buildingIds) throws AccessDeniedException;

	/**
	 * @param communityId l'id technique Unireg de la communauté de propriétaires
	 * @return la communauté de propriétaires; ou <b>null</b> si elle est inconnue.
	 * @throws AccessDeniedException si l'opérateur courant n'a pas le droit de voir les immeubles.
	 */
	@Nullable
	CommunityOfOwners getCommunityOfOwners(long communityId) throws AccessDeniedException;

	/**
	 * @param communityIds les ids techniques Unireg des communautés de propriétaires
	 * @return une liste de communautés de propriétaires
	 * @throws AccessDeniedException si l'opérateur courant n'a pas le droit de voir les immeubles.
	 */
	@NotNull
	CommunityOfOwnersList getCommunitiesOfOwners(List<Long> communityIds) throws AccessDeniedException;

	/**
	 * @return la définition Swagger du service
	 */
	@NotNull
	String getSwaggerJson() throws IOException;
}
