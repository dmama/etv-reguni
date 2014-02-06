package ch.vd.uniregctb.evenement.party.control;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.metier.assujettissement.AssujettissementService;
import ch.vd.uniregctb.metier.assujettissement.TypeAssujettissement;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.ModeImposition;

public class TaxLiabilityControlServiceImpl implements TaxLiabilityControlService {

	private static final Logger LOGGER = Logger.getLogger(TaxLiabilityControlServiceImpl.class);

	private TiersService tiersService;
	private AssujettissementService assujettissementService;

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	public void setAssujettissementService(AssujettissementService assujettissementService) {
		this.assujettissementService = assujettissementService;
	}

	public TaxLiabilityControlResult doControlOnDate(@NotNull Tiers tiers, RegDate date, boolean rechercheMenageCommun, boolean rechercheParent, boolean controleDateDansFuture,
	                                                 @Nullable Set<ModeImposition> modeImpositionARejeter) throws ControlRuleException {
		if (LOGGER.isDebugEnabled()) {
			final String message = String.format("Contrôle d'assujettissement à une date => tiers %d, date %s, menage? %b, parent? %b",
			                                     tiers.getNumero(), RegDateHelper.dateToDashString(date), rechercheMenageCommun, rechercheParent);
			LOGGER.debug(message);
		}

		//Contrôle sur la date activé avec date dans le futur
		if (controleDateDansFuture && date.isAfter(RegDate.get())) {
				TaxLiabilityControlResult result = new TaxLiabilityControlResult();
				result.setEchec(new TaxLiabilityControlEchec(TaxLiabilityControlEchec.EchecType.DATE_OU_PF_DANS_FUTURE));
			return result;
		}
		final List<TaxLiabilityControlRule> rules = getControlRulesForDate(tiers, date, rechercheMenageCommun, rechercheParent,modeImpositionARejeter);
		return doControl(tiers, rules);
	}

	public TaxLiabilityControlResult doControlOnPeriod(@NotNull Tiers tiers, int periode, boolean rechercheMenageCommun, boolean rechercheParent, boolean controlePeriodDansFutur,
	                                                   Set<TypeAssujettissement> assujettissementsARejeter) throws ControlRuleException {
		if (LOGGER.isDebugEnabled()) {
			final String message = String.format("Contrôle d'assujettissement sur periode => tiers %d, periode %d, menage? %b, parent? %b",
			                                     tiers.getNumero(), periode, rechercheMenageCommun, rechercheParent);
			LOGGER.debug(message);
		}
		//Contrôle sur la Période activé avec la période dans le futur
		if (controlePeriodDansFutur && periode > RegDate.get().year()) {
			TaxLiabilityControlResult result = new TaxLiabilityControlResult();
			result.setEchec(new TaxLiabilityControlEchec(TaxLiabilityControlEchec.EchecType.DATE_OU_PF_DANS_FUTURE));
			return result;
		}
		final List<TaxLiabilityControlRule> rules = getControlRulesForPeriod(tiers, periode, rechercheMenageCommun, rechercheParent,assujettissementsARejeter);
		return doControl(tiers, rules);
	}

	private TaxLiabilityControlResult doControl(@NotNull Tiers tiers, List<TaxLiabilityControlRule> rules) throws ControlRuleException {
		TaxLiabilityControlResult result = null;
		for (TaxLiabilityControlRule taxLiabilityControlRule : rules) {
			result = taxLiabilityControlRule.check(tiers);
			if (result.getIdTiersAssujetti() != null) {
				// on a trouvé un controle OK on retourne le résultat
				return result;
			}
		}

		//On a un controle Ko
		return result;
	}

	private List<TaxLiabilityControlRule> buildRuleList(Tiers tiers, ControlRuleForTiers ruleTiers, ControlRuleForMenage ruleMenage, ControlRuleForParent ruleParents) {

		//Construction de la liste d'exécution
		final List<TaxLiabilityControlRule> rules = new LinkedList<>();

		//Contrôle d'assujetissement sur le tiers en entrée
		rules.add(ruleTiers);

		//MC.0 si le tiers n'est pas un ménage commun  et que la recherche de ménage commun est demandée,
		//on rajoute la règle de contôle des ménage à l'exécution
		// [SIFISC-9064] : en fait, il fallait lire "si le tiers est une personne physique et que la recherche de ménage commun est demandée"...
		if (ruleMenage != null && tiers instanceof PersonnePhysique) {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Règle de recherche sur les ménages communs chargée");
			}
			rules.add(ruleMenage);
		}

		//MI.0 MI.1 MI.2
		if (ruleParents != null && tiers instanceof PersonnePhysique && ruleTiers.isMineur((PersonnePhysique) tiers)) {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Règle de recherche sur les parents chargée");
			}
			rules.add(ruleParents);
		}

		return rules;
	}

	private List<TaxLiabilityControlRule> getControlRulesForPeriod(@NotNull Tiers tiers, int periode, boolean rechercheMenageCommun,
	                                                               boolean rechercheParent,Set<TypeAssujettissement> assujettissementsARejeter) throws ControlRuleException {
		final ControlRuleForTiersPeriode controlRuleForTiers = new ControlRuleForTiersPeriode(periode, tiersService, assujettissementService,assujettissementsARejeter);
		final ControlRuleForMenagePeriode controlRuleForMenage = rechercheMenageCommun ? new ControlRuleForMenagePeriode(periode, tiersService, assujettissementService,assujettissementsARejeter) : null;
		final ControlRuleForParentPeriode controlRuleForParent = rechercheParent ? new ControlRuleForParentPeriode(periode, tiersService, assujettissementService,assujettissementsARejeter) : null;
		return buildRuleList(tiers, controlRuleForTiers, controlRuleForMenage, controlRuleForParent);
	}

	private List<TaxLiabilityControlRule> getControlRulesForDate(@NotNull Tiers tiers, RegDate date, boolean rechercheMenageCommun, boolean rechercheParent,
	                                                             Set<ModeImposition> modeImpositionARejeter) {
		final ControlRuleForTiersDate controlRuleForTiers = new ControlRuleForTiersDate(date, tiersService,modeImpositionARejeter);
		final ControleRuleForMenageDate controlRuleForMenage = rechercheMenageCommun ? new ControleRuleForMenageDate(date, tiersService,modeImpositionARejeter) : null;
		final ControlRuleForParentDate controlRuleForParent = rechercheParent ? new ControlRuleForParentDate(date, tiersService,modeImpositionARejeter) : null;
		return buildRuleList(tiers, controlRuleForTiers, controlRuleForMenage, controlRuleForParent);
	}
}
