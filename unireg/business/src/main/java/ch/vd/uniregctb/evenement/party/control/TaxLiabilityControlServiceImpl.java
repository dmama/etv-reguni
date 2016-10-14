package ch.vd.uniregctb.evenement.party.control;

import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.metier.assujettissement.AssujettissementService;
import ch.vd.uniregctb.metier.assujettissement.TypeAssujettissement;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.ModeImposition;

public class TaxLiabilityControlServiceImpl implements TaxLiabilityControlService {

	private static final Logger LOGGER = LoggerFactory.getLogger(TaxLiabilityControlServiceImpl.class);

	private TiersService tiersService;
	private AssujettissementService assujettissementService;

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	public void setAssujettissementService(AssujettissementService assujettissementService) {
		this.assujettissementService = assujettissementService;
	}

	public TaxLiabilityControlResult<ModeImposition> doControlOnDate(@NotNull Tiers tiers, RegDate date, boolean rechercheMenageCommun, boolean rechercheParent, boolean controleDateDansFutur,
	                                                                 @Nullable Set<ModeImposition> modeImpositionARejeter) throws ControlRuleException {
		if (LOGGER.isDebugEnabled()) {
			final String message = String.format("Contrôle d'assujettissement à une date => tiers %d, date %s, menage? %b, parent? %b",
			                                     tiers.getNumero(), RegDateHelper.dateToDashString(date), rechercheMenageCommun, rechercheParent);
			LOGGER.debug(message);
		}

		//Contrôle sur la date activé avec date dans le futur
		if (controleDateDansFutur && date.isAfter(RegDate.get())) {
			return new TaxLiabilityControlResult<>(new TaxLiabilityControlEchec(TaxLiabilityControlEchec.EchecType.DATE_OU_PF_DANS_FUTUR));
		}

		final List<TaxLiabilityControlRule<ModeImposition>> rules = getControlRulesForDate(tiers, date, rechercheMenageCommun, rechercheParent);
		return doControl(tiers, rules, modeImpositionARejeter);
	}

	public TaxLiabilityControlResult<TypeAssujettissement> doControlOnPeriod(@NotNull Tiers tiers, int periode, boolean rechercheMenageCommun, boolean rechercheParent, boolean controlePeriodeDansFutur,
	                                                                         @Nullable Set<TypeAssujettissement> assujettissementsARejeter) throws ControlRuleException {
		if (LOGGER.isDebugEnabled()) {
			final String message = String.format("Contrôle d'assujettissement sur periode => tiers %d, periode %d, menage? %b, parent? %b",
			                                     tiers.getNumero(), periode, rechercheMenageCommun, rechercheParent);
			LOGGER.debug(message);
		}
		//Contrôle sur la Période activé avec la période dans le futur
		if (controlePeriodeDansFutur && periode > RegDate.get().year()) {
			return new TaxLiabilityControlResult<>(new TaxLiabilityControlEchec(TaxLiabilityControlEchec.EchecType.DATE_OU_PF_DANS_FUTUR));
		}

		final List<TaxLiabilityControlRule<TypeAssujettissement>> rules = getControlRulesForPeriod(tiers, periode, rechercheMenageCommun, rechercheParent);
		return doControl(tiers, rules, assujettissementsARejeter);
	}

	private <T extends Enum<T>> TaxLiabilityControlResult<T> doControl(@NotNull Tiers tiers, List<TaxLiabilityControlRule<T>> rules, Set<T> aRejeter) throws ControlRuleException {

		TaxLiabilityControlResult<T> echecARetourner = null;
		TaxLiabilityControlResult<T> analyse = null;
		for (TaxLiabilityControlRule<T> taxLiabilityControlRule : rules) {
			analyse = taxLiabilityControlRule.check(tiers,aRejeter);
			if (analyse.getIdTiersAssujetti() != null) {
				// on a trouvé un controle OK on retourne le résultat
				return analyse;
			}
			else{
				final TaxLiabilityControlEchec.EchecType echecType = analyse.getEchec().getType();
				if(echecType == TaxLiabilityControlEchec.EchecType.AUCUN_MC_ASSOCIE_TROUVE) {
					if (echecARetourner == null || !echecARetourner.getEchec().isAssujetissementNonConforme()) {
						echecARetourner = analyse;
					}
				}
				else{
					echecARetourner = analyse;
				}
			}
		}

		//On a un controle Ko
		return echecARetourner;
	}

	private <T extends Enum<T>> TaxLiabilityControlResult<T> verifierAssujettissement(TaxLiabilityControlResult<T> analyse, Set<T> aRejeter) {
		//Si le contrôle est en echec, on le renvoie tel quel
		if (analyse.getEchec() != null) {
			return analyse;
		}

		// Vérification de la conformité des assujettissements trouvés
		final Set<T> trouves = analyse.getSourceAssujettissements();
		if (trouves != null && !trouves.isEmpty() && aRejeter != null && !aRejeter.isEmpty()) {
			final Set<T> remaining = EnumSet.copyOf(trouves);
			remaining.removeAll(aRejeter);
			if (remaining.isEmpty()) {
				final TaxLiabilityControlEchec echec;
				switch (analyse.getOrigine()) {
				case MENAGE_COMMUN:
					echec = new TaxLiabilityControlEchec(TaxLiabilityControlEchec.EchecType.UN_PLUSIEURS_MC_NON_ASSUJETTI_TROUVES);
					echec.setMenageCommunIds(Collections.singletonList(analyse.getIdTiersAssujetti()));
					break;
				case PARENT:
					echec = new TaxLiabilityControlEchec(TaxLiabilityControlEchec.EchecType.CONTROLE_SUR_PARENTS_KO);
					echec.setParentsIds(Collections.singletonList(analyse.getIdTiersAssujetti()));
					break;
				case MENAGE_COMMUN_PARENT:
					echec = new TaxLiabilityControlEchec(TaxLiabilityControlEchec.EchecType.CONTROLE_SUR_PARENTS_KO);
					echec.setMenageCommunParentsIds(Collections.singletonList(analyse.getIdTiersAssujetti()));
					break;
				default:
					echec = new TaxLiabilityControlEchec(TaxLiabilityControlEchec.EchecType.CONTROLE_NUMERO_KO);
					break;
				}

				echec.setAssujetissementNonConforme(true);
				return new TaxLiabilityControlResult<>(echec);
			}
		}

		return analyse;
	}



	private <T extends Enum<T>> List<TaxLiabilityControlRule<T>> buildRuleList(Tiers tiers, ControlRuleForTiers<T> ruleTiers, ControlRuleForMenage<T> ruleMenage, ControlRuleForParent<T> ruleParents) {

		//Construction de la liste d'exécution
		final List<TaxLiabilityControlRule<T>> rules = new LinkedList<>();

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

	private List<TaxLiabilityControlRule<TypeAssujettissement>> getControlRulesForPeriod(@NotNull Tiers tiers, int periode, boolean rechercheMenageCommun, boolean rechercheParent) throws ControlRuleException {
		final ControlRuleForTiersPeriode controlRuleForTiers = new ControlRuleForTiersPeriode(periode, tiersService, assujettissementService);
		final ControlRuleForMenagePeriode controlRuleForMenage = rechercheMenageCommun ? new ControlRuleForMenagePeriode(periode, tiersService, assujettissementService) : null;
		final ControlRuleForParentPeriode controlRuleForParent = rechercheParent ? new ControlRuleForParentPeriode(periode, tiersService, assujettissementService) : null;
		return buildRuleList(tiers, controlRuleForTiers, controlRuleForMenage, controlRuleForParent);
	}

	private List<TaxLiabilityControlRule<ModeImposition>> getControlRulesForDate(@NotNull Tiers tiers, RegDate date, boolean rechercheMenageCommun, boolean rechercheParent) {
		final ControlRuleForTiersDate controlRuleForTiers = new ControlRuleForTiersDate(date, tiersService);
		final ControlRuleForMenageDate controlRuleForMenage = rechercheMenageCommun ? new ControlRuleForMenageDate(date, tiersService) : null;
		final ControlRuleForParentDate controlRuleForParent = rechercheParent ? new ControlRuleForParentDate(date, tiersService) : null;
		return buildRuleList(tiers, controlRuleForTiers, controlRuleForMenage, controlRuleForParent);
	}
}
