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
	void envoyerDocumentAvecNotificationEFacture(long ctbId, TypeDocument typeDocument, String idDemande, RegDate dateDemande) throws EditiqueException, EvenementEfactureException;

	/**Permet de rechercher l'historique des états  et des demandes d'un destinataire
	 *
	 * @param ctbId numéro de contribuable du destinataire
	 * @return l'historique du destinataire
	 */
	HistoriqueDestinataire getHistoriqueDestinataire(long ctbId);
}