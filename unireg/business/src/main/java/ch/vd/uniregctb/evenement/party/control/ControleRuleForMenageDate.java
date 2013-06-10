package ch.vd.uniregctb.evenement.party.control;

import java.util.ArrayList;
import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.evenement.party.TaxliabilityControlResult;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.xml.Context;

/**
 * Règle MC.2 - Recherche de l'appartenance à un ménage commun (CTB couple) pour le numéro d'individu à la date déterminante :
 */
public class ControleRuleForMenageDate extends ControlRuleForMenage {
	private Long tiersId;
	private RegDate date;


	public ControleRuleForMenageDate(Context context, Long tiersId, RegDate date) {
		super(context, tiersId);
		this.date = date;

	}
	//Si un num CTB couple est en vigueur à la date déterminante, lancement du contrôle d'assujettissement sur ce numéro (A1.3)
	@Override
	public boolean isAssujetti(Long tiersId) throws ControlRuleException {
		ControlRuleForTiersDate controlRuleForTiersDate = new ControlRuleForTiersDate(context,tiersId,date);
		TaxliabilityControlResult result = controlRuleForTiersDate.check();
		return result.getIdTiersAssujetti()!=null;
	}

	@Override
	public List<EnsembleTiersCouple> getEnsembleTiersCouple(PersonnePhysique pp) {
		final List<EnsembleTiersCouple> liste = new ArrayList<EnsembleTiersCouple>();
		final EnsembleTiersCouple couple = context.tiersService.getEnsembleTiersCouple(pp, date);
		if (couple != null) {
			liste.add(couple);
		}

		return liste;
	}

}
