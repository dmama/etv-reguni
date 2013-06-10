package ch.vd.uniregctb.evenement.party;

import java.util.LinkedList;

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


	private ControlRuleForTiers controlRuleForTiers;
	private ControlRuleForMenage controlRuleForMenage;
	private ControlRuleForParent controlRuleForParent;

	LinkedList<TaxliabilityControlRule> listeExecution;


	public TaxliabilityControlManager(Context context) {
		this.context = context;
		this.listeExecution = new LinkedList<TaxliabilityControlRule>();
	}


	public TaxliabilityControlResult runControlOnDate(Long tiersId, RegDate date, boolean rechercheMenageCommun, boolean rechercheParent) throws ControlRuleException {
		return runControl(tiersId,null,date,false,rechercheMenageCommun,rechercheParent);
	}

	public TaxliabilityControlResult runControlOnPeriode(Long tiersId, Integer periode, boolean rechercheMenageCommun, boolean rechercheParent) throws ControlRuleException {
		return runControl(tiersId,periode,null,true,rechercheMenageCommun,rechercheParent);
	}


	public TaxliabilityControlResult runControl(Long tiersId, Integer periode, RegDate date, boolean periodic, boolean rechercheMenageCommun, boolean rechercheParent) throws ControlRuleException {
		TaxliabilityControlResult result = null;


		if (periodic) {
			if (LOGGER.isDebugEnabled()) {
				String message = String.format("Controle d'assujetissement sur periode => :Numéro de tiers %d," +
						" periode: %d, Rechercche de menage: %b, Recherche de parent: %b",tiersId.intValue(),periode,rechercheMenageCommun,rechercheParent);
				LOGGER.debug(message);
			}

			loadControlRulesForPeriode(tiersId, periode);
		}
		else {
			String message = String.format("Controle d'assujetissement à une date => :Numéro de tiers %d," +
					" Date de contrôle: %s, Rechercche de menage: %b, Recherche de parent: %b",tiersId.intValue(), RegDateHelper.dateToDashString(date),rechercheMenageCommun,rechercheParent);
			LOGGER.debug(message);
			loadControlRulesForDate(tiersId, date);
		}

		//On Récupère le tiers
		Tiers tiers = context.tiersDAO.get(tiersId);

		//Construction de la liste d'exécution

		//Contrôle d'assujetissement sur le tiers en entrée
		listeExecution.add(controlRuleForTiers);
		//MC.0 si le tiers n'est pas un ménage commun  et que la recherche de ménage commun est demandée,
		//on rajoute la règle de contôle des ménage à l'exécution
		if (!(tiers instanceof MenageCommun) && rechercheMenageCommun) {
			if (LOGGER.isDebugEnabled()) {
				String message = "Règle de recherche sur les ménages communs chargée";
				LOGGER.debug(message);
			}
			listeExecution.add(controlRuleForMenage);
		}

		//MI.0 MI.1 MI.2
		if (!(tiers instanceof MenageCommun) && rechercheParent && controlRuleForTiers.isMineur(tiersId)) {
			String message = "Règle de recherche sur les parents chargée";
			LOGGER.debug(message);
			listeExecution.add(controlRuleForParent);

		}

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

	private void loadControlRulesForPeriode(Long idPP, Integer periode) {
		this.controlRuleForTiers = new ControlRuleForTiersPeriode(context, idPP, periode);
		this.controlRuleForMenage = new ControlRuleForMenagePeriode(context, idPP, periode);
		this.controlRuleForParent = new ControlRuleForParentPeriode(context, idPP, periode);
	}

	private void loadControlRulesForDate(Long idPP, RegDate date) {
		this.controlRuleForTiers = new ControlRuleForTiersDate(context, idPP, date);
		this.controlRuleForMenage = new ControleRuleForMenageDate(context, idPP, date);
		this.controlRuleForParent = new ControlRuleForParentDate(context, idPP, date);
	}
}
