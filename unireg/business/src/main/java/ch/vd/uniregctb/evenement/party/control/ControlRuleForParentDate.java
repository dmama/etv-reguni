package ch.vd.uniregctb.evenement.party.control;

import java.util.ArrayList;
import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.evenement.party.TaxliabilityControlResult;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.RapportFiliation;
import ch.vd.uniregctb.xml.Context;

/**
 * Régle PA.2:Déclenchée si demande porte sur date déterminante.
 *Recherche d’un parent assujetti à une date déterminante (PCAP) :
 */
public class ControlRuleForParentDate extends ControlRuleForParent {


	private RegDate date;


	public ControlRuleForParentDate(Context contex, Long tiersId, RegDate date) {
		super(contex, tiersId);
		this.date = date;

	}



	@Override
	public List<RapportFiliation> extractParents(List<RapportFiliation> filiations) {

		List<RapportFiliation> filiationsParents = new ArrayList<RapportFiliation>();
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
	public boolean isAssujetti(Long idTiers) throws ControlRuleException {
		ControlRuleForTiersDate controlRuleForTiersDate = new ControlRuleForTiersDate(context,idTiers,date);
		TaxliabilityControlResult result = controlRuleForTiersDate.check();
		return result.getIdTiersAssujetti()!=null;
	}

	@Override
	public TaxliabilityControlResult rechercheAssujettisementSurMenageParent(Long parentId) throws ControlRuleException {
		ControleRuleForMenageDate controleMenageParent = new ControleRuleForMenageDate(context, parentId,date );
		return controleMenageParent.check();
	}

	@Override
	public TaxliabilityControlResult rechercheAssujettisementSurMenage(Long parentId) throws ControlRuleException {
		return new ControleRuleForMenageDate(context, parentId,date).check();
	}

	@Override
	protected boolean isEnMenage(PersonnePhysique parent) {
		EnsembleTiersCouple ensemble = context.tiersService.getEnsembleTiersCouple(parent,date);
		return ensemble!=null;
	}

}
