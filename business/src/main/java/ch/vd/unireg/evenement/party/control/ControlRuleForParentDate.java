package ch.vd.unireg.evenement.party.control;

import java.util.ArrayList;
import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.tiers.Parente;
import ch.vd.unireg.tiers.TiersService;
import ch.vd.unireg.type.ModeImposition;

/**
 * Régle PA.2:Déclenchée si demande porte sur date déterminante.
 *Recherche d’un parent assujetti à une date déterminante (PCAP) :
 */
public class ControlRuleForParentDate extends ControlRuleForParent<ModeImposition> {

	private final RegDate date;

	public ControlRuleForParentDate(RegDate date, TiersService tiersService) {
		super(tiersService,
		      new ControlRuleForTiersDate(date, tiersService),
		      new ControlRuleForMenageDate(date, tiersService));
		this.date = date;
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
}
