package ch.vd.unireg.evenement.registrefoncier;

import ch.vd.unireg.jms.EsbBusinessException;

/**
 * Interface du handler interne de génération des rapprochements propriétaire
 * à la réception d'une identification positive
 */
public interface RapprochementProprietaireHandler {

	/**
	 * Création des rapprochements RF corrects pour le contribuable et le tiers RF cités
	 * @param idContribuable le contribuable à rapprocher
	 * @param idTiersRF le tiers RF à rapprocher
	 * @throws EsbBusinessException lancée en cas de souci "métier"
	 */
	void addRapprochement(long idContribuable, long idTiersRF) throws EsbBusinessException;
}
