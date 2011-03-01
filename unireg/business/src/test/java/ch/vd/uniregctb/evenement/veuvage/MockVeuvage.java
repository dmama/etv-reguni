package ch.vd.uniregctb.evenement.veuvage;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.evenement.common.MockEvenementCivil;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.type.TypeEvenementCivil;

public class MockVeuvage extends MockEvenementCivil implements Veuvage {

	public MockVeuvage(Individu individu, Individu conjoint, RegDate date, Integer numeroOfsCommuneAnnonce) {
		super(individu, conjoint, TypeEvenementCivil.VEUVAGE, date, numeroOfsCommuneAnnonce);
	}

	public RegDate getDateVeuvage() {
		return getDate();
	}

}
