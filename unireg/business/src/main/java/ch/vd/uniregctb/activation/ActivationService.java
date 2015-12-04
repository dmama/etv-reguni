package ch.vd.uniregctb.activation;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.tiers.Tiers;

public interface ActivationService {

	/**
	 * Désactive un tiers à partir du lendemain de la date donnée
	 * @param tiers
	 * @param dateAnnulation
	 */
	public void desactiveTiers(Tiers tiers, RegDate dateAnnulation) throws ActivationServiceException;

	/**
	 * Remplace un tiers par un autre (désactive le premier et ajoute un lien entre les deux)
	 * @param tiersRemplace
	 * @param tiersRemplacant
	 * @param dateRemplacement
	 */
	public void remplaceTiers(Tiers tiersRemplace, Tiers tiersRemplacant, RegDate dateRemplacement) throws ActivationServiceException;


	/**
	 * Réactive un tiers précédemment désactivé
	 * @param tiers
	 * @param dateReactivation
	 */
	public void reactiveTiers(Tiers tiers, RegDate dateReactivation) throws ActivationServiceException;
}
