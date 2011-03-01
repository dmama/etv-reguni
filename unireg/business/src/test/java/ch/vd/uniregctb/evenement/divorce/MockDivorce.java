package ch.vd.uniregctb.evenement.divorce;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.evenement.common.MockEvenementCivil;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.type.TypeEvenementCivil;

public class MockDivorce extends MockEvenementCivil implements Divorce {

	private Individu ancienConjoint;

	public MockDivorce(Individu individu, Individu conjoint, RegDate date, Integer numeroOfsCommuneAnnonce) {
		super(individu, conjoint, TypeEvenementCivil.DIVORCE, date, numeroOfsCommuneAnnonce);
		this.ancienConjoint = conjoint;
	}

	public Individu getAncienConjoint() {
		return ancienConjoint;
	}
}
