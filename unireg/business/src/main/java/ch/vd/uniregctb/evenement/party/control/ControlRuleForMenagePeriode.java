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
	private Integer periode;


	public ControlRuleForMenagePeriode(Context context, Long tiersId, Integer periode) {
		super(context,tiersId);
		this.periode = periode;
	}

	//Lancer le CTRL d'assujettissement (A1.1) pour chacun des numéros de couple trouvés
	@Override
	public boolean isAssujetti(Long tiersId) throws ControlRuleException {
		ControlRuleForTiersPeriode controlRuleForTiersPeriode = new ControlRuleForTiersPeriode(context,tiersId,periode);
		TaxliabilityControlResult result = controlRuleForTiersPeriode.check();
		return result.getIdTiersAssujetti()!=null;
	}

	@Override
	public List<EnsembleTiersCouple> getEnsembleTiersCouple(PersonnePhysique pp) {
		final List<EnsembleTiersCouple> listeCouples = context.tiersService.getEnsembleTiersCouple(pp,periode);
		return listeCouples;
	}

}
