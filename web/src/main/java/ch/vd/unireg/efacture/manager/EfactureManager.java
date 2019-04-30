package ch.vd.unireg.efacture.manager;

import java.math.BigInteger;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.editique.EditiqueException;
import ch.vd.unireg.efacture.DestinataireAvecHistoView;
import ch.vd.unireg.efacture.EFactureException;
import ch.vd.unireg.interfaces.efacture.data.ResultatQuittancement;
import ch.vd.unireg.type.TypeDocument;

public interface EfactureManager {

	/**
	 * Demande l'impression du document de demande de signature ou de demande de contact
	 *
	 * @param ctbId     le numéro de contribuable traité
	 * @param typeDocument permet de determiner le type de document à envoyer au contribuable
	 * @param idDemande l'id de la demande à traiter
	 * @param dateDemande la date
	 * @param noAdherent numéro d'adhérent e-facture de la demande d'inscription en cours de traitement
	 * @param dateDemandePrecedente date de la demande d'inscription précédente remplacée par celle-ci
	 * @param noAdherentPrecedent numéro d'adhérent e-facture de l'inscription précédente remplacée par celle-ci
	 * @return business id du message jms
	 */
	String envoyerDocumentAvecNotificationEFacture(long ctbId, TypeDocument typeDocument, String idDemande, RegDate dateDemande, BigInteger noAdherent, RegDate dateDemandePrecedente, BigInteger noAdherentPrecedent) throws EditiqueException,
			EFactureException;

	/**Permet de rechercher l'historique des états  et des demandes d'un destinataire
	 *
	 * @param ctbId numéro de contribuable du destinataire
	 * @return l'historique du destinataire
	 */
	DestinataireAvecHistoView getDestinataireAvecSonHistorique(long ctbId);

	/**Demande la suspension d'un contribuable
	 *
	 * @param ctbId le numéro de contribuable
	 * @param comment commentaire libre si saisi par l'utilisateur
	 * @return l'identifant du message demandant la suspension
	 */
	String suspendreContribuable(long ctbId, @Nullable String comment) throws EFactureException;


	/**Demande l'activation d'un contribuable
	 *
	 * @param ctbId le numéro de contribuable
	 * @param comment commentaire libre si saisi par l'utilisateur
	 * @return l'identifant du message demandant l'activation
	 */
	String activerContribuable(long ctbId, @Nullable String comment) throws EFactureException;

	/**Retourne indique si on a reçu une réponse concernant le message dont le business id est passé en parametre
	 *
	 * @param businessId le business id du message pour lequel on attend un réponse
	 *
	 * @return <code>true</code> si on a reçu une réponse, <code>false</code> sinon.
	 */
	boolean isReponseRecueDeEfacture(String businessId);

	/**
	 * Permet de demander la validation de l'inscription
	 *
	 * @param idDemande identifiant de la demande
	 * @return business id de la demande de validation
	 */
	String accepterDemande(String idDemande) throws EFactureException;

	/**
	 * permet de demander un refus d'inscription
	 * @param idDemande id de la demande
	 * @return business id de la demande de refus
	 * @throws EFactureException
	 */
	String refuserDemande(String idDemande) throws EFactureException;

	/**
	 * Quittance le contribuable
	 *
	 * @param noCtb le numéro du contribuable à quittancer
	 * @return ok si le quittancement s'est déroulé correctement
	 */
	ResultatQuittancement quittancer(Long noCtb) throws EFactureException;

	/**
	 * Demande le changement d'adresse mail du contribuable inscrit à la e-facture
	 * @param noCtb numéro du contribuable
	 * @param newEmail nouvelle valeur de l'adresse mail (peut être nulle si inconnue)
	 */
	String modifierEmail(long noCtb, @Nullable String newEmail) throws EFactureException;

	String getMessageQuittancement(ResultatQuittancement resultatQuittancement, long noCtb);

	/**
	 * @return la longueur maximale autorisée pour un commentaire saisi manuellement à l'activation ou la suspension d'un contribuable
	 */
	int getMaxLengthForManualComment();
}