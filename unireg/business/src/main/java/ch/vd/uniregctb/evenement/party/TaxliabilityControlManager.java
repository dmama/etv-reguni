package ch.vd.uniregctb.evenement.party;

import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.evenement.party.control.ControlRuleException;
import ch.vd.uniregctb.evenement.party.control.ControlRuleForMenage;
import ch.vd.uniregctb.evenement.party.control.ControlRuleForMenagePeriode;
import ch.vd.uniregctb.evenement.party.control.ControlRuleForParent;
import ch.vd.uniregctb.evenement.party.control.ControlRuleForParentDate;
import ch.vd.uniregctb.evenement.party.control.ControlRuleForParentPeriode;
import ch.vd.uniregctb.evenement.party.control.ControlRuleForTiers;
import ch.vd.uniregctb.evenement.party.control.ControlRuleForTiersDate;
import ch.vd.uniregctb.evenement.party.control.ControlRuleForTiersPeriode;
import ch.vd.uniregctb.evenement.party.control.ControleRuleForMenageDate;
import ch.vd.uniregctb.evenement.party.control.TaxliabilityControlRule;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.xml.Context;

public class TaxliabilityControlManager {

	private static final Logger LOGGER = Logger.getLogger(TaxliabilityControlManager.class);
	private Context context;

	public TaxliabilityControlManager(Context context) {
		this.context = context;
	}

	public TaxliabilityControlResult runControlOnDate(long tiersId, RegDate date, boolean rechercheMenageCommun, boolean rechercheParent) throws ControlRuleException {
		if (LOGGER.isDebugEnabled()) {
			final String message = String.format("Contrôle d'assujettissement à une date => tiers %d, date %s, menage? %b, parent? %b",
			                                     tiersId, RegDateHelper.dateToDashString(date), rechercheMenageCommun, rechercheParent);
			LOGGER.debug(message);
		}
		final List<TaxliabilityControlRule> listeExecution = loadControlRulesForDate(tiersId, date, rechercheMenageCommun, rechercheParent);
		return runControl(listeExecution);
	}

	public TaxliabilityControlResult runControlOnPeriode(long tiersId, int periode, boolean rechercheMenageCommun, boolean rechercheParent) throws ControlRuleException {
		if (LOGGER.isDebugEnabled()) {
			final String message = String.format("Contrôle d'assujettissement sur periode => tiers %d, periode %d, menage? %b, parent? %b",
			                                     tiersId, periode, rechercheMenageCommun, rechercheParent);
			LOGGER.debug(message);
		}
		final List<TaxliabilityControlRule> listeExecution = loadControlRulesForPeriode(tiersId, periode, rechercheMenageCommun, rechercheParent);
		return runControl(listeExecution);
	}

	private TaxliabilityControlResult runControl(List<TaxliabilityControlRule> listeExecution) throws ControlRuleException {
		TaxliabilityControlResult result = null;
		for (TaxliabilityControlRule taxliabilityControlRule : listeExecution) {
			result = taxliabilityControlRule.check();
			if (result.getIdTiersAssujetti() != null) {
				// on a trouvé un controle OK on retourne le résultat
				return result;
			}
		}

		//On a un controle Ko
		return result;
	}

	private List<TaxliabilityControlRule> buildRuleList(long tiersId, ControlRuleForTiers ruleTiers, ControlRuleForMenage ruleMenage, ControlRuleForParent ruleParents) {

		//On Récupère le tiers
		Tiers tiers = context.tiersDAO.get(tiersId);

		//Construction de la liste d'exécution
		final List<TaxliabilityControlRule> listeExecution = new LinkedList<>();

		//Contrôle d'assujetissement sur le tiers en entrée
		listeExecution.add(ruleTiers);

		//MC.0 si le tiers n'est pas un ménage commun  et que la recherche de ménage commun est demandée,
		//on rajoute la règle de contôle des ménage à l'exécution
		if (ruleMenage != null && !(tiers instanceof MenageCommun)) {
			if (LOGGER.isDebugEnabled()) {
				String message = "Règle de recherche sur les ménages communs chargée";
				LOGGER.debug(message);
			}
			listeExecution.add(ruleMenage);
		}

		//MI.0 MI.1 MI.2
		if (ruleParents != null && !(tiers instanceof MenageCommun) && ruleTiers.isMineur(tiersId)) {
			if (LOGGER.isDebugEnabled()) {
				String message = "Règle de recherche sur les parents chargée";
				LOGGER.debug(message);
			}
			listeExecution.add(ruleParents);
		}

		return listeExecution;
	}

	private List<TaxliabilityControlRule> loadControlRulesForPeriode(long tiersId, Integer periode, boolean rechercheMenageCommun, boolean rechercheParent) {
		final ControlRuleForTiersPeriode controlRuleForTiers = new ControlRuleForTiersPeriode(context, tiersId, periode);
		final ControlRuleForMenagePeriode controlRuleForMenage = rechercheMenageCommun ? new ControlRuleForMenagePeriode(context, tiersId, periode) : null;
		final ControlRuleForParentPeriode controlRuleForParent = rechercheParent ? new ControlRuleForParentPeriode(context, tiersId, periode) : null;
		return buildRuleList(tiersId, controlRuleForTiers, controlRuleForMenage, controlRuleForParent);
	}

	private List<TaxliabilityControlRule> loadControlRulesForDate(long tiersId, RegDate date, boolean rechercheMenageCommun, boolean rechercheParent) {
		final ControlRuleForTiersDate controlRuleForTiers = new ControlRuleForTiersDate(context, tiersId, date);
		final ControleRuleForMenageDate controlRuleForMenage = rechercheMenageCommun ? new ControleRuleForMenageDate(context, tiersId, date) : null;
		final ControlRuleForParentDate controlRuleForParent = rechercheParent ? new ControlRuleForParentDate(context, tiersId, date) : null;
		return buildRuleList(tiersId, controlRuleForTiers, controlRuleForMenage, controlRuleForParent);
	}
}
