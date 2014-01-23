package ch.vd.uniregctb.evenement.party.control;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.tiers.Parente;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersService;

/**
 * Régle PA.2:Déclenchée si demande porte sur date déterminante.
 *Recherche d’un parent assujetti à une date déterminante (PCAP) :
 */
public class ControlRuleForParentDate extends ControlRuleForParent {

	private final RegDate date;
	private final AbstractControlRule ruleForTiers;

	public ControlRuleForParentDate(RegDate date, TiersService tiersService) {
		super(tiersService);
		this.date = date;
		this.ruleForTiers = new ControlRuleForTiersDate(date, tiersService);
	}

	@Override
	protected List<Parente> extractParents(List<Parente> parentes) {
		final List<Parente> extraction = new ArrayList<>(parentes.size());
		for (Parente parente : parentes) {
			if (parente.isValidAt(date)) {
				extraction.add(parente);
			}
		}
		return extraction;
	}

	//vérification du for en vigueur à la date (règle A1.3) sur ce numéro de tiers
	@Override
	public boolean isAssujetti(@NotNull Tiers tiers) throws ControlRuleException {
		final TaxLiabilityControlResult result = ruleForTiers.check(tiers);
		return result.getIdTiersAssujetti() != null;
	}

	@Override
	public TaxLiabilityControlResult rechercheAssujettisementSurMenage(@NotNull PersonnePhysique parent) throws ControlRuleException {
		return new ControleRuleForMenageDate(date, tiersService).check(parent);
	}

}
