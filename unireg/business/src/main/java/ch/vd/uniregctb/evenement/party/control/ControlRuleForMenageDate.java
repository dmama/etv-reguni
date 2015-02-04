package ch.vd.uniregctb.evenement.party.control;

import java.util.ArrayList;
import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.ModeImposition;

/**
 * Règle MC.2 - Recherche de l'appartenance à un ménage commun (CTB couple) pour le numéro d'individu à la date déterminante :
 */
public class ControlRuleForMenageDate extends ControlRuleForMenage<ModeImposition> {

	private final RegDate date;

	public ControlRuleForMenageDate(RegDate date, TiersService tiersService) {
		super(tiersService,new ControlRuleForTiersDate(date, tiersService));
		this.date = date;
	}

	@Override
	public List<EnsembleTiersCouple> getEnsembleTiersCouple(PersonnePhysique pp) {
		final List<EnsembleTiersCouple> liste = new ArrayList<>(1);
		final EnsembleTiersCouple couple = tiersService.getEnsembleTiersCouple(pp, date);
		if (couple != null) {
			liste.add(couple);
		}

		return liste;
	}
}
