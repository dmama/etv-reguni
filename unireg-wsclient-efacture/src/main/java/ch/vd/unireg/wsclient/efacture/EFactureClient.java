package ch.vd.unireg.wsclient.efacture;

import ch.vd.evd0025.v1.PayerWithHistory;

/**
 * Interface du client du service REST e-Facture
 */
public interface EFactureClient {

	/**
	 * @param ctbId numero du contribuable
	 * @param billerId identifiant de l'émetteur de factures (ACI)
	 * @return  l'historique complet des inscriptions / demandes d'inscription d'un contribuable vis à vis de la e-Facture
	 */
	PayerWithHistory getHistory(long ctbId, String billerId);
}
