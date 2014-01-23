package ch.vd.uniregctb.webservices.v5;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.ws.ack.v1.OrdinaryTaxDeclarationAckRequest;
import ch.vd.unireg.ws.ack.v1.OrdinaryTaxDeclarationAckResponse;
import ch.vd.unireg.ws.deadline.v1.DeadlineRequest;
import ch.vd.unireg.ws.deadline.v1.DeadlineResponse;
import ch.vd.unireg.ws.security.v1.SecurityResponse;
import ch.vd.unireg.ws.taxoffices.v1.TaxOffices;
import ch.vd.uniregctb.webservices.common.AccessDeniedException;
import ch.vd.uniregctb.webservices.common.UserLogin;

/**
 * Partie purement métier du traitement des appels web-service v5
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
	 * @param login désignation de l'opérateur qui demande l'opération
	 * @param blocked nouvel état du flag
	 * @throws AccessDeniedException si l'opérateur n'a pas le droit de faire ça
	 */
	void setAutomaticRepaymentBlockingFlag(int partyNo, UserLogin login, boolean blocked) throws AccessDeniedException;

	/**
	 * Interrogation du flag de blocage de remboursement automatique sur un tiers donné
	 * @param partyNo numéro du tiers
	 * @param login désination de l'opérateur qui veut savoir
	 * @return <code>true</code> si le blocage est actif, <code>false</code> sinon
	 * @throws AccessDeniedException si l'opérateur n'a pas le droit de lecture sur le dossier
	 */
	boolean getAutomaticRepaymentBlockingFlag(int partyNo, UserLogin login) throws AccessDeniedException;

	/**
	 * Quittancement d'une série de déclarations d'impôt
	 * @param login désignation de l'opérateur qui demande le quittancement
	 * @param request information sur les déclarations à quittancer
	 * @return un statut sur les quittancements opérés
	 * @throws AccessDeniedException si l'opérateur n'a pas le droit de faire ça
	 */
	OrdinaryTaxDeclarationAckResponse ackOrdinaryTaxDeclarations(UserLogin login, OrdinaryTaxDeclarationAckRequest request) throws AccessDeniedException;

	/**
	 * Demande de nouveau délai de dépôt pour une déclaration d'impôt
	 * @param partyNo numéro du contribuable auquel un nouveau délai a été accordé
	 * @param pf période fiscale de la déclaration concernée par le délai
	 * @param seqNo numéro de séquence de la déclaration concernée par le délai
	 * @param login désignation de l'opérateur qui demande le délai
	 * @param request informations sur les dates du délai demandé
	 * @return un statut sur le délai demandé
	 * @throws AccessDeniedException si l'opérateur n'a pas le droit de faire ça
	 */
	DeadlineResponse newOrdinaryTaxDeclarationDeadline(int partyNo, int pf, int seqNo, UserLogin login, DeadlineRequest request) throws AccessDeniedException;

	/**
	 * Récupère les offices d'impôt de la commune identifiée par son numéro OFS et la date de validité
	 * @param municipalityId numéro OFS de la commune
	 * @param date date de validité du numéro OFS (si <code>null</code>, on prendra la date du jour)
	 * @return une description des offices d'impôt (de district et de région) liés à la commune indiquée
	 * @throws ch.vd.uniregctb.common.ObjectNotFoundException si la commune est inconnue ou n'est pas vaudoise
	 */
	TaxOffices getTaxOffices(int municipalityId, @Nullable RegDate date);
}
