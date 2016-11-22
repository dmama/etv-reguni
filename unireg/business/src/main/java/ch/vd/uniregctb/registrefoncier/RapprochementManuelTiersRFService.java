package ch.vd.uniregctb.registrefoncier;

/**
 * Service qui expose les méthodes utilisées dans le cadre des rapprochements manuels entre les tiers RF
 * et les contribuables Unireg
 */
public interface RapprochementManuelTiersRFService {

	/**
	 * Génère une demande d'identification manuelle pour le tiers RF donné
	 * @param tiersRF le tiers RF pour lequel il convient de générer une demande d'identification manuelle
	 */
	void genererDemandeIdentificationManuelle(TiersRF tiersRF);


}
