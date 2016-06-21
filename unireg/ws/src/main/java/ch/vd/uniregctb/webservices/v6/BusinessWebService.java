package ch.vd.uniregctb.webservices.v6;

import java.util.Date;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.ws.ack.v6.OrdinaryTaxDeclarationAckRequest;
import ch.vd.unireg.ws.ack.v6.OrdinaryTaxDeclarationAckResponse;
import ch.vd.unireg.ws.deadline.v6.DeadlineRequest;
import ch.vd.unireg.ws.deadline.v6.DeadlineResponse;
import ch.vd.unireg.ws.modifiedtaxpayers.v6.PartyNumberList;
import ch.vd.unireg.ws.parties.v6.Parties;
import ch.vd.unireg.ws.security.v6.SecurityResponse;
import ch.vd.unireg.ws.taxoffices.v6.TaxOffices;
import ch.vd.unireg.xml.party.v4.Party;
import ch.vd.unireg.xml.party.v4.PartyInfo;
import ch.vd.unireg.xml.party.v4.PartyPart;
import ch.vd.unireg.xml.party.withholding.v1.DebtorCategory;
import ch.vd.unireg.xml.party.withholding.v1.DebtorInfo;
import ch.vd.uniregctb.avatar.ImageData;
import ch.vd.uniregctb.indexer.IndexerException;
import ch.vd.uniregctb.webservices.common.AccessDeniedException;
import ch.vd.uniregctb.webservices.common.UserLogin;
import ch.vd.uniregctb.xml.ServiceException;

/**
 * Partie purement métier du traitement des appels web-service v6
 */
public interface BusinessWebService {

	/**
	 * Quels sont les droits d'accès de l'utilisateur donné par son visa sur le tiers donné par son numéro
	 * @param user visa de l'utilisateur
	 * @param partyNo numéro du tiers
	 * @return le droit d'accès possible (voir champ {@link SecurityResponse#allowedAccess})
	 */
	SecurityResponse getSecurityOnParty(String user, int partyNo);

	/**
	 * Modification du flag de blocage des remboursements automatiques sur un tiers donné
	 * @param partyNo numéro du tiers
	 * @param user désignation de l'opérateur qui demande l'opération
	 * @param blocked nouvel état du flag
	 * @throws AccessDeniedException si l'opérateur n'a pas le droit de faire ça
	 */
	void setAutomaticRepaymentBlockingFlag(int partyNo, UserLogin user, boolean blocked) throws AccessDeniedException;

	/**
	 * Interrogation du flag de blocage de remboursement automatique sur un tiers donné
	 * @param partyNo numéro du tiers
	 * @param user désination de l'opérateur qui veut savoir
	 * @return <code>true</code> si le blocage est actif, <code>false</code> sinon
	 * @throws AccessDeniedException si l'opérateur n'a pas le droit de lecture sur le dossier
	 */
	boolean getAutomaticRepaymentBlockingFlag(int partyNo, UserLogin user) throws AccessDeniedException;

	/**
	 * Quittancement d'une série de déclarations d'impôt
	 * @param user désignation de l'opérateur qui demande le quittancement
	 * @param request information sur les déclarations à quittancer
	 * @return un statut sur les quittancements opérés
	 * @throws AccessDeniedException si l'opérateur n'a pas le droit de faire ça
	 */
	OrdinaryTaxDeclarationAckResponse ackOrdinaryTaxDeclarations(UserLogin user, OrdinaryTaxDeclarationAckRequest request) throws AccessDeniedException;

	/**
	 * Demande de nouveau délai de dépôt pour une déclaration d'impôt
	 * @param partyNo numéro du contribuable auquel un nouveau délai a été accordé
	 * @param pf période fiscale de la déclaration concernée par le délai
	 * @param seqNo numéro de séquence de la déclaration concernée par le délai
	 * @param user désignation de l'opérateur qui demande le délai
	 * @param request informations sur les dates du délai demandé
	 * @return un statut sur le délai demandé
	 * @throws AccessDeniedException si l'opérateur n'a pas le droit de faire ça
	 */
	DeadlineResponse newOrdinaryTaxDeclarationDeadline(int partyNo, int pf, int seqNo, UserLogin user, DeadlineRequest request) throws AccessDeniedException;

	/**
	 * Récupère les offices d'impôt de la commune identifiée par son numéro OFS et la date de validité
	 * @param municipalityId numéro OFS de la commune
	 * @param date date de validité du numéro OFS (si <code>null</code>, on prendra la date du jour)
	 * @return une description des offices d'impôt (de district et de région) liés à la commune indiquée
	 * @throws ch.vd.uniregctb.common.ObjectNotFoundException si la commune est inconnue ou n'est pas vaudoise
	 */
	TaxOffices getTaxOffices(int municipalityId, @Nullable RegDate date);

	/**
	 * Récupère une liste des numéros de contribuables modifiés entre les date données (toutes deux comprises)
	 * @param user désignation de l'opérateur qui demande la liste
	 * @param since <i>timestamp</i> du début de la période interessante
	 * @param until <i>timestamp</i> de la fin de la période intéressante
	 * @return la liste des numéros des contribuables modifiés
	 * @throws AccessDeniedException si l'opérateur n'a pas accès à cette liste
	 */
	PartyNumberList getModifiedTaxPayers(UserLogin user, Date since, Date until) throws AccessDeniedException;

	/**
	 * Récupère un état des LR émises et à émettre sur le débiteur de prestation imposable indiqué pour une période fiscale donnée
	 * @param user désignation de l'opérateur qui demande l'information
	 * @param debtorNo numéro du débiteur concerné par la demande
	 * @param pf période fiscale d'intérêt
	 * @return une descriptions des listes émises et restant à émettre pour ce débiteur et cette période fiscale
	 * @throws AccessDeniedException si l'opérateur n'a pas accès à cette information
	 */
	DebtorInfo getDebtorInfo(UserLogin user, int debtorNo, int pf) throws AccessDeniedException;

	/**
	 * Effectue une recherche de tiers selon les critères donnés
	 *
	 * @param user désignation de l'opérateur pour le compte duquel la recherche est faite
	 * @param partyNo [optionnel] le numéro du tiers
	 * @param name [optionnel] un ou plusieurs éléments du nom (de la raison sociale) du tiers
	 * @param nameSearchMode mode d'interprétation du contenu du champ "name"
	 * @param townOrCountry [optionnel] nom de la localité ou du pays du tiers recherché (adresse courrier)
	 * @param dateOfBirth [optionnel] date de naissance (peut être partielle) du tiers recherché
	 * @param socialInsuranceNumber [optionnel] numéro AVS (à 11 ou 13 positions) du tiers recherché
	 * @param uidNumber [optionnel] numéro IDE associé au tiers recherché
	 * @param taxResidenceFSOId [optionnel] numéro OFS du for du tiers recherché
	 * @param onlyActiveMainTaxResidence [optionnel] si le critère du fors est renseigné, impose une contrainte sur la présence d'un for principal actif
	 * @param partyTypes [optionnel] ensemble des types de tiers recherchés (si vide ou <code>null</code>, tous les types seront considérés)
	 * @param debtorCategory [optionnel] catégorie de débiteur recherchée
	 * @param activeParty
	 * @param oldWithholdingNumber [optionnel] ancien numéro de sourcier du tiers recherché
	 * @return les informations sur les tiers correspondants aux critères donnés
	 * @throws AccessDeniedException si l'opérateur n'a pas le droit de faire des recherches de tiers
	 * @throws IndexerException si une erreur est levée dans la recherche (critères vides, trop de résultats...)
	 */
	List<PartyInfo> searchParty(UserLogin user, @Nullable String partyNo, @Nullable String name, SearchMode nameSearchMode, @Nullable String townOrCountry,
	                            @Nullable RegDate dateOfBirth, @Nullable String socialInsuranceNumber, @Nullable String uidNumber, @Nullable Integer taxResidenceFSOId,
	                            boolean onlyActiveMainTaxResidence, @Nullable Set<PartySearchType> partyTypes, @Nullable DebtorCategory debtorCategory, @Nullable Boolean activeParty,
	                            @Nullable Long oldWithholdingNumber) throws AccessDeniedException, IndexerException;

	/**
	 * Récupère les informations sur le tiers demandé
	 * @param user désignation de l'opérateur pour le compte duquel les informations sont glânées
	 * @param partyNo numéro du tiers
	 * @param parts [optionnel] ensemble des parts supplémentaires qui intéressent l'appelant
	 * @return les informations demandées sur le tiers
	 * @throws AccessDeniedException si l'opérateur n'a pas le droit de faire ce genre de recherche
	 * @throws ServiceException en cas de problème à la constitution de l'entité à exporter
	 */
	Party getParty(UserLogin user, int partyNo, @Nullable Set<PartyPart> parts) throws AccessDeniedException, ServiceException;

	/**
	 * Récupère les informations sur les tiers demandés
	 * @param user désignation de l'opérateur pour le compte duquel les informations sont glânées
	 * @param partyNos numéro des tiers
	 * @param parts [optionnel] ensemble des parts supplémentaires qui intéressent l'appelant
	 * @return les informations demandées sur les tiers (<code>null</code> si la liste d'identifiants en entrée était vide)
	 * @throws AccessDeniedException si l'opérateur n'a pas le droit de faire ce genre de recherche
	 * @throws ServiceException en cas de problème à la constitution de l'entité à exporter
	 */
	Parties getParties(UserLogin user, List<Integer> partyNos, @Nullable Set<PartyPart> parts) throws AccessDeniedException, ServiceException;

	/**
	 * Récupère l'image de l'avatar du tiers demandé
	 * @param partyNo numéro du tiers
	 * @return l'image de l'avatar
	 * @throws ServiceException en cas de problème
	 */
	ImageData getAvatar(int partyNo) throws ServiceException;
}
