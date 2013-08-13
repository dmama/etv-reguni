package ch.vd.uniregctb.evenement.party.control;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.metier.assujettissement.AssujettissementService;
import ch.vd.uniregctb.tiers.Filiation;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersService;

/**
 * Régle PA.1: Recherche d’un parent assujetti sur la PF (ARI) : Détermination de toutes les relations parentales en vigueur sur la PF concernée
 */
public class ControlRuleForParentPeriode extends ControlRuleForParent {

	private final DateRange periode;
	private final AbstractControlRule ruleForTiers;
	private final AbstractControlRule ruleForMenage;

	public ControlRuleForParentPeriode(int periode, TiersService tiersService, AssujettissementService assService) {
		super(tiersService);
		this.periode = new DateRangeHelper.Range(RegDate.get(periode, 1, 1), RegDate.get(periode, 12, 31));
		this.ruleForTiers = new ControlRuleForTiersPeriode(periode, tiersService, assService);
		this.ruleForMenage = new ControlRuleForMenagePeriode(periode, tiersService, assService);
	}

	//Si une seule relation parentale trouvée sur la période : vérification de l'assujettisse-ment sur la PF (règle A1.1) sur ce numéro de tiers.
	@Override
	public boolean isAssujetti(@NotNull Tiers tiers) throws ControlRuleException {
		final TaxLiabilityControlResult result = ruleForTiers.check(tiers);
		return result.getIdTiersAssujetti() != null;
	}

	@Override
	public TaxLiabilityControlResult rechercheAssujettisementSurMenage(@NotNull PersonnePhysique parent) throws ControlRuleException {
		return ruleForMenage.check(parent);
	}

	@Override
	protected List<Filiation> extractParents(List<Filiation> filiations) {
		final List<Filiation> filiationsParents = new ArrayList<>(filiations.size());
		for (Filiation filiation : filiations) {
			if (DateRangeHelper.intersect(periode, filiation)) {
				filiationsParents.add(filiation);
			}
		}
		return filiationsParents;
	}
}
