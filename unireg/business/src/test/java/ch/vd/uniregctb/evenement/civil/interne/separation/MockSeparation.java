package ch.vd.uniregctb.evenement.civil.interne.separation;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.evenement.civil.common.MockEvenementCivil;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.type.TypeEvenementCivil;

public class MockSeparation extends MockEvenementCivil implements Separation {

	private Individu ancienConjoint;

	public MockSeparation(Individu individu, Individu conjoint, RegDate date, Integer numeroOfsCommuneAnnonce) {
		super(individu, conjoint, TypeEvenementCivil.SEPARATION, date, numeroOfsCommuneAnnonce);
		this.ancienConjoint = conjoint;
	}

	public Individu getAncienConjoint() {
		return ancienConjoint;
	}
}
