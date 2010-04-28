package ch.vd.uniregctb.activation;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.tiers.Tiers;

public interface ActivationService {

	/**
	 * Annule un tiers
	 * @param tiers
	 * @param dateAnnulation
	 */
	public void annuleTiers(Tiers tiers, RegDate dateAnnulation) throws ActivationServiceException;

	/**
	 * Annule un tiers
	 * @param tiersRemplace
	 * @param tiersRemplacant
	 * @param dateRemplacement
	 */
	public void remplaceTiers(Tiers tiersRemplace, Tiers tiersRemplacant, RegDate dateRemplacement) throws ActivationServiceException;


	/**
	 * Réactive un tiers annulé
	 * @param tiers
	 * @param dateReactivation
	 */
	public void reactiveTiers(Tiers tiers, RegDate dateReactivation);
}
