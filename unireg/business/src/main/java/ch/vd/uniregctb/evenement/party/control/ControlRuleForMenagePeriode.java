package ch.vd.uniregctb.evenement.party.control;

import java.util.List;

import ch.vd.uniregctb.evenement.party.TaxliabilityControlResult;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.xml.Context;

/**
 * Règle MC.1 - Recherche d'un ménage commun assujetti sur la PF
 */
public class ControlRuleForMenagePeriode extends ControlRuleForMenage {

	private final int periode;

	public ControlRuleForMenagePeriode(Context context, long tiersId, int periode) {
		super(context,tiersId);
		this.periode = periode;
	}

	//Lancer le CTRL d'assujettissement (A1.1) pour chacun des numéros de couple trouvés
	@Override
	public boolean isAssujetti(long tiersId) throws ControlRuleException {
		final ControlRuleForTiersPeriode controlRuleForTiersPeriode = new ControlRuleForTiersPeriode(context, tiersId, periode);
		final TaxliabilityControlResult result = controlRuleForTiersPeriode.check();
		return result.getIdTiersAssujetti() != null;
	}

	@Override
	public List<EnsembleTiersCouple> getEnsembleTiersCouple(PersonnePhysique pp) {
		return context.tiersService.getEnsembleTiersCouple(pp, periode);
	}
}
