package ch.vd.uniregctb.efacture.manager;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.editique.EditiqueException;
import ch.vd.uniregctb.efacture.EvenementEfactureException;
import ch.vd.uniregctb.efacture.HistoriqueDestinataire;
import ch.vd.uniregctb.type.TypeDocument;

public interface EfactureManager {

	/**
	 * Demande l'impression du document de demande de signature ou de demande de contact
	 *
	 * @param ctbId     le numéro de contribuable traité
	 * @param typeDocument permet de determiner le type de document à envoyer au contribuable
	 * @param idDemande l'id de la demande à traiter
	 * @param dateDemande
	 */
	String envoyerDocumentAvecNotificationEFacture(long ctbId, TypeDocument typeDocument, String idDemande, RegDate dateDemande) throws EditiqueException, EvenementEfactureException;

	/**Permet de rechercher l'historique des états  et des demandes d'un destinataire
	 *
	 * @param ctbId numéro de contribuable du destinataire
	 * @return l'historique du destinataire
	 */
	HistoriqueDestinataire getHistoriqueDestinataire(long ctbId);

	/**Demande la suspension d'un contribuable
	 *
	 * @param ctbId
	 * @return l'identifant du message demandant la suspension
	 */
	String suspendreContribuable(long ctbId) throws EvenementEfactureException;


	/**Demande l'activation d'un contribuable
	 *
	 * @param ctbId
	 * @return l'identifant du message demandant l'activation
	 */
	String activerContribuable(long ctbId) throws EvenementEfactureException;

	/**Retourne indique si on a reçu une réponse concernant le message dont le business id est passé en parametre
	 *
	 *
	 * @param businessId@return <code>true</code> si on a reçu une réponse, <code>false</code> sinon.
	 */
	boolean isReponseReçuDeEfacture(String businessId);

	/**
	 * Permet de demander la validation de l'inscription
	 *
	 * @param idDemande identifiant de la demande
	 * @return business id de la demande de validation
	 */
	String accepterDemande(String idDemande) throws EvenementEfactureException;

	/**
	 * permet de demander un refus d'inscription
	 * @param idDemande
	 * @return business id de la demande de refus
	 * @throws EvenementEfactureException
	 */
	String refuserDemande(String idDemande) throws EvenementEfactureException;;
}