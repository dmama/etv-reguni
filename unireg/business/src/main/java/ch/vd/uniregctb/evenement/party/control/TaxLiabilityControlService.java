package ch.vd.uniregctb.evenement.party.control;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.tiers.Tiers;

public interface TaxLiabilityControlService {

	/**
	 * Contrôle d'assujettissement apériodique
	 *
	 *
	 * @param tiers tiers en entrée
	 * @param date date de référence
	 * @param rechercheMenageCommun <code>true</code> s'il faut activer la recherche de ménage commun
	 * @param rechercheParent <code>true</code> s'il faut activer la recherche des parents de mineur
	 * @param controleDateDansFuture  <code>true</code> s'il faut verifier que la date de référence n'est pas dans le futur
	 * @return le résultat du contrôle
	 * @throws ControlRuleException en cas de problème
	 */
	TaxLiabilityControlResult doControlOnDate(@NotNull Tiers tiers, RegDate date, boolean rechercheMenageCommun, boolean rechercheParent, boolean controleDateDansFuture) throws ControlRuleException;

	/**
	 * Contrôle d'assujettissement périodique
	 *
	 * @param tiers tiers en entrée
	 * @param periode periode fiscale de référence
	 * @param rechercheMenageCommun <code>true</code> s'il faut activer la recherche de ménage commun
	 * @param rechercheParent <code>true</code> s'il faut activer la recherche des parents de mineur
	 * @param controlePeriodDansFutur <code>true</code> s'il faut vérifier que la période de référence n'est pas dans le futur
	 * @return le résultat du contrôle
	 * @throws ControlRuleException en cas de problème
	 */
	TaxLiabilityControlResult doControlOnPeriod(@NotNull Tiers tiers, int periode, boolean rechercheMenageCommun, boolean rechercheParent, boolean controlePeriodDansFutur) throws ControlRuleException;

}
