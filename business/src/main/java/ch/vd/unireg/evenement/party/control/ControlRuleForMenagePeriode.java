package ch.vd.unireg.evenement.party.control;

import java.util.List;

import ch.vd.unireg.metier.assujettissement.AssujettissementService;
import ch.vd.unireg.metier.assujettissement.TypeAssujettissement;
import ch.vd.unireg.tiers.EnsembleTiersCouple;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.TiersService;

/**
 * Règle MC.1 - Recherche d'un ménage commun assujetti sur la PF
 */
public class ControlRuleForMenagePeriode extends ControlRuleForMenage<TypeAssujettissement> {

	private final int periode;

	public ControlRuleForMenagePeriode(int periode, TiersService tiersService, AssujettissementService assService) {
		super(tiersService, new ControlRuleForTiersPeriode(periode, tiersService, assService));
		this.periode = periode;

	}

	@Override
	public List<EnsembleTiersCouple> getEnsembleTiersCouple(PersonnePhysique pp) {
		return tiersService.getEnsembleTiersCouple(pp, periode);
	}
}
