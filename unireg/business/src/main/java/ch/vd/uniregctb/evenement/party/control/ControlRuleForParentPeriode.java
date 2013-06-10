package ch.vd.uniregctb.evenement.party.control;

import java.util.ArrayList;
import java.util.List;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.evenement.party.TaxliabilityControlResult;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.RapportFiliation;
import ch.vd.uniregctb.xml.Context;

/**
 * Régle PA.1: Recherche d’un parent assujetti sur la PF (ARI) : Détermination de toutes les relations parentales en vigueur sur la PF concernée
 */
public class ControlRuleForParentPeriode extends ControlRuleForParent {
	private Integer periode;


	public ControlRuleForParentPeriode(Context contex, Long tiersId, Integer periode) {
		super(contex, tiersId);
		this.periode = periode;

	}

	//Si une seule relation parentale trouvée sur la période : vérification de l'assujettisse-ment sur la PF (règle A1.1) sur ce numéro de tiers.
	@Override
	public boolean isAssujetti(Long idTiers) throws ControlRuleException {
		ControlRuleForTiersPeriode controlRuleForTiersPeriode = new ControlRuleForTiersPeriode(context,idTiers,periode);
		TaxliabilityControlResult result = controlRuleForTiersPeriode.check();
		return result.getIdTiersAssujetti()!=null;
	}

	//Si tiers non assujetti > recherche d'un MC assujetti* pour ce tiers parent (règle MC.1)
	@Override
	public TaxliabilityControlResult rechercheAssujettisementSurMenageParent(Long tiersId) throws ControlRuleException {
		ControlRuleForMenagePeriode controleMenageParent = new ControlRuleForMenagePeriode(context, tiersId, periode);
		return controleMenageParent.check();
	}

	@Override
	public TaxliabilityControlResult rechercheAssujettisementSurMenage(Long parentId) throws ControlRuleException {
		return new ControlRuleForMenagePeriode(context, parentId, periode).check();
	}

	@Override
	protected boolean isEnMenage(PersonnePhysique parent) {
		List<EnsembleTiersCouple> ensemble = context.tiersService.getEnsembleTiersCouple(parent,periode);
		return (ensemble!=null && !ensemble.isEmpty());
	}


	@Override
	public List<RapportFiliation> extractParents(List<RapportFiliation> filiations) {
		DateRange rangePeriode = new DateRangeHelper.Range(RegDate.get(periode, 1, 1), RegDate.get(periode, 12, 31));

		List<RapportFiliation> filiationsParents = new ArrayList<RapportFiliation>();
		for (RapportFiliation filiation : filiations) {
			final boolean isDansPeriode = DateRangeHelper.intersect(rangePeriode, filiation);
			if (filiation.getType() == RapportFiliation.Type.PARENT && isDansPeriode) {
				filiationsParents.add(filiation);
			}
		}
		return filiationsParents;  //To change body of created methods use File | Settings | File Templates.
	}

}
