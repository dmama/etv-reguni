package ch.vd.uniregctb.evenement.party.control;

import java.util.ArrayList;
import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.evenement.party.TaxliabilityControlResult;
import ch.vd.uniregctb.tiers.RapportFiliation;
import ch.vd.uniregctb.xml.Context;

/**
 * Régle PA.2:Déclenchée si demande porte sur date déterminante.
 *Recherche d’un parent assujetti à une date déterminante (PCAP) :
 */
public class ControlRuleForParentDate extends ControlRuleForParent {

	private final RegDate date;

	public ControlRuleForParentDate(Context context, long tiersId, RegDate date) {
		super(context, tiersId);
		this.date = date;
	}

	@Override
	public List<RapportFiliation> extractParents(List<RapportFiliation> filiations) {

		List<RapportFiliation> filiationsParents = new ArrayList<>();
		for (RapportFiliation filiation : filiations) {
			final boolean isValide = filiation.isValidAt(date);
			if (filiation.getType() == RapportFiliation.Type.PARENT && isValide) {
				filiationsParents.add(filiation);
			}
		}
		return filiationsParents;
	}

	//vérification du for en vigueur à la date (règle A1.3) sur ce numéro de tiers
	@Override
	public boolean isAssujetti(long idTiers) throws ControlRuleException {
		final ControlRuleForTiersDate controlRuleForTiersDate = new ControlRuleForTiersDate(context, idTiers, date);
		final TaxliabilityControlResult result = controlRuleForTiersDate.check();
		return result.getIdTiersAssujetti() != null;
	}

	@Override
	public TaxliabilityControlResult rechercheAssujettisementSurMenage(Long parentId) throws ControlRuleException {
		return new ControleRuleForMenageDate(context, parentId,date).check();
	}

}
