package ch.vd.uniregctb.evenement.civil.interne.annulation.mariage;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.evenement.civil.common.MockEvenementCivil;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.type.TypeEvenementCivil;

public class MockAnnulationMariage extends MockEvenementCivil implements AnnulationMariage {
	public MockAnnulationMariage(Individu individu, Individu conjoint, RegDate date, Integer numeroOfsCommuneAnnonce) {
		super(individu, conjoint, TypeEvenementCivil.ANNUL_MARIAGE, date, numeroOfsCommuneAnnonce);
	}
}
