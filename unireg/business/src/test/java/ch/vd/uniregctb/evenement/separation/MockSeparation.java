package ch.vd.uniregctb.evenement.separation;

import ch.vd.uniregctb.evenement.common.MockEvenementCivil;
import ch.vd.uniregctb.interfaces.model.Individu;

public class MockSeparation extends MockEvenementCivil implements Separation {

	private Individu ancienConjoint;
	
	public Individu getAncienConjoint() {
		return ancienConjoint;
	}

	public void setAncienConjoint(Individu ancienConjoint) {
		this.ancienConjoint = ancienConjoint;
	}
}
