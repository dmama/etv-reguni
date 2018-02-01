package ch.vd.unireg.evenement.party.control;

import java.util.ArrayList;
import java.util.List;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.metier.assujettissement.AssujettissementService;
import ch.vd.unireg.metier.assujettissement.TypeAssujettissement;
import ch.vd.unireg.tiers.Parente;
import ch.vd.unireg.tiers.TiersService;

/**
 * Régle PA.1: Recherche d’un parent assujetti sur la PF (ARI) : Détermination de toutes les relations parentales en vigueur sur la PF concernée
 */
public class ControlRuleForParentPeriode extends ControlRuleForParent<TypeAssujettissement> {

	private final DateRange periode;

	public ControlRuleForParentPeriode(int periode, TiersService tiersService, AssujettissementService assService) {
		super(tiersService,
		      new ControlRuleForTiersPeriode(periode, tiersService, assService),
		      new ControlRuleForMenagePeriode(periode, tiersService, assService));
		this.periode = new DateRangeHelper.Range(RegDate.get(periode, 1, 1), RegDate.get(periode, 12, 31));
	}

	@Override
	protected List<Parente> extractParents(List<Parente> parentes) {
		final List<Parente> filiationsParents = new ArrayList<>(parentes.size());
		for (Parente parente : parentes) {
			if (DateRangeHelper.intersect(periode, parente)) {
				filiationsParents.add(parente);
			}
		}
		return filiationsParents;
	}
}
