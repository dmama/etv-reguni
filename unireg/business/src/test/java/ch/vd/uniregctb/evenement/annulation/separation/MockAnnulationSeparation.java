package ch.vd.uniregctb.evenement.annulation.separation;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.evenement.common.MockEvenementCivil;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.type.TypeEvenementCivil;

public class MockAnnulationSeparation extends MockEvenementCivil implements AnnulationSeparation {

	public MockAnnulationSeparation(Individu individu, Individu conjoint, RegDate date, Integer numeroOfsCommuneAnnonce) {
		super(individu, conjoint, TypeEvenementCivil.ANNUL_SEPARATION, date, numeroOfsCommuneAnnonce);
	}
}
