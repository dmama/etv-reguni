package ch.vd.uniregctb.efacture;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.editique.EditiqueException;
import ch.vd.uniregctb.type.TypeDocument;

public interface EFactureService {

	/**
	 * Permet de notfier à l'E-facture que la demande est mise en attente
	 * @param idDemande identifiant de la demande
	 * @param typeDocument le type de document qui a été imprimée
	 * @param idArchivage la clé d'archivage générée
	 * @param retourAttendu vrai si un retour est attendu suite à la notification
	 */

	public void notifieMiseEnattenteInscription(String idDemande, TypeDocument typeDocument, String idArchivage, boolean retourAttendu) throws EvenementEfactureException;

	/**
	 * Demande l'impression du document de demande de signature ou de demande de contact
	 *
	 * @param ctbId     le numéro de contribuable traité
	 * @param typeDocument permet de determiner le type de document à envoyer au contribuable
	 * @param dateDemande date à laquel le contribuable a fait sa demande d'inscription
	 */
	String imprimerDocumentEfacture(Long ctbId, TypeDocument typeDocument, RegDate dateDemande) throws EditiqueException;
}
