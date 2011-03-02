package ch.vd.uniregctb.evenement.civil.interne.annulation.veuvage;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.evenement.civil.common.MockEvenementCivil;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.type.TypeEvenementCivil;

public class MockAnnulationVeuvage extends MockEvenementCivil implements AnnulationVeuvage {
	public MockAnnulationVeuvage(Individu individu, Individu conjoint, RegDate date, Integer numeroOfsCommuneAnnonce) {
		super(individu, conjoint, TypeEvenementCivil.ANNUL_VEUVAGE, date, numeroOfsCommuneAnnonce);
	}
}
