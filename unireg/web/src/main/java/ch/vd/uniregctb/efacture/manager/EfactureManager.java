package ch.vd.uniregctb.efacture.manager;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.editique.EditiqueException;
import ch.vd.uniregctb.type.TypeDocument;

public interface EfactureManager {


	/**
	 * Demande l'impression du document de demande de signature ou de demande de contact
	 *
	 * @param ctbId     le numéro de contribuable traité
	 * @param typeDocument permet de determiner le type de document à envoyer au contribuable
	 * @param idDemande l'id d ela demande à traiter
	 * @param dateDemande
	 */

	public void imprimerDocumentEfacture(Long ctbId, TypeDocument typeDocument, Long idDemande, RegDate dateDemande) throws EditiqueException;
}
