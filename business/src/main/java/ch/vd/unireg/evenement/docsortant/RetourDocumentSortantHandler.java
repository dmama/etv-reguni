package ch.vd.unireg.evenement.docsortant;

import java.util.Map;

import ch.vd.unireg.xml.event.docsortant.retour.v3.Quittance;
import ch.vd.unireg.jms.EsbBusinessException;

/**
 * Interface du handler métier (hors de toute considération de parsing de message XML...)
 * du traitement du retour (= quittance) d'annonce d'un document sortant
 */
public interface RetourDocumentSortantHandler {

	/**
	 * Nom du header attendu qui contient le type de document annoncé
	 */
	String TYPE_DOCUMENT_HEADER_NAME = "typeDocumentAnnonce";

	/**
	 * Nom du header attendu qui contient un identifiant permettant de retrouver l'entité sur laquelle charger la clé de visualisation externe du document
	 */
	String ID_ENTITE_DOCUMENT_ANNONCE_HEADER_NAME = "idDocumentAnnonce";

	/**
	 * Appelé à la réception d'un message de quittance
	 * @param quittance la quittance elle-même
	 * @param headers les méta-données autour du message
	 * @throws EsbBusinessException en cas d'erreur métier
	 */
	void onQuittance(Quittance quittance, Map<String, String> headers) throws EsbBusinessException;
}
