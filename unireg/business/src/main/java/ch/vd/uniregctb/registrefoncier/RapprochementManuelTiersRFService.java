package ch.vd.uniregctb.registrefoncier;

import ch.vd.uniregctb.tiers.Tiers;

/**
 * Service qui expose les méthodes utilisées dans le cadre des rapprochements manuels entre les tiers RF
 * et les contribuables Unireg
 */
public interface RapprochementManuelTiersRFService {

	/**
	 * Nom de l'attribut placé dans les headers du message d'identification
	 * (qui sera donc retourné à l'appelant)
	 */
	String ID_TIERS_RF = "idTiersRF";

	/**
	 * Génère une demande d'identification manuelle pour le tiers RF donné
	 * @param tiersRF le tiers RF pour lequel il convient de générer une demande d'identification manuelle
	 */
	void genererDemandeIdentificationManuelle(TiersRF tiersRF);

	/**
	 * Retrouve les éventuelles demandes d'identification manuelle encore en suspens associées à ce tiers RF
	 * et passe les à l'état "Traité automatiquement" vers le tiers unireg donné
	 * @param tiersRF le tiers RF dont on vient de traiter le rapprochement par un autre biais que l'identification manuelle
	 * @param tiersUnireg le tiers Unireg identifié
	 */
	void marquerDemandesIdentificationManuelleEventuelles(TiersRF tiersRF, Tiers tiersUnireg);

}
