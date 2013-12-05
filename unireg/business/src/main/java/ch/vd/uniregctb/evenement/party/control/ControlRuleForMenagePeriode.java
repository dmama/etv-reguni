package ch.vd.uniregctb.evenement.party.control;

import java.util.List;

import org.jetbrains.annotations.NotNull;

import ch.vd.uniregctb.metier.assujettissement.AssujettissementService;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersService;

/**
 * Règle MC.1 - Recherche d'un ménage commun assujetti sur la PF
 */
public class ControlRuleForMenagePeriode extends ControlRuleForMenage {

	private final int periode;
	private final AbstractControlRule ruleForTiers;

	public ControlRuleForMenagePeriode(int periode, TiersService tiersService, AssujettissementService assService) {
		super(tiersService);
		this.periode = periode;
		this.ruleForTiers = new ControlRuleForTiersPeriode(periode, tiersService, assService);
	}

	//Lancer le CTRL d'assujettissement (A1.1) pour chacun des numéros de couple trouvés
	@Override
	public boolean isAssujetti(@NotNull Tiers tiers) throws ControlRuleException {
		final TaxLiabilityControlResult result = ruleForTiers.check(tiers);
		return result.getIdTiersAssujetti() != null;
	}

	@Override
	public List<EnsembleTiersCouple> getEnsembleTiersCouple(PersonnePhysique pp) {
		return tiersService.getEnsembleTiersCouple(pp, periode);
	}
}
