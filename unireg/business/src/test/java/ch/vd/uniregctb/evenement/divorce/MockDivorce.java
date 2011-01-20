package ch.vd.uniregctb.evenement.divorce;

import ch.vd.uniregctb.evenement.common.MockEvenementCivil;
import ch.vd.uniregctb.interfaces.model.Individu;

public class MockDivorce extends MockEvenementCivil implements Divorce {

	private Individu ancienConjoint;
	
	public Individu getAncienConjoint() {
		return ancienConjoint;
	}

	public void setAncienConjoint(Individu ancienConjoint) {
		this.ancienConjoint = ancienConjoint;
	}
}
