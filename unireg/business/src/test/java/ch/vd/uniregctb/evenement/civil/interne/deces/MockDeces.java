package ch.vd.uniregctb.evenement.civil.interne.deces;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.evenement.civil.common.MockEvenementCivil;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.type.TypeEvenementCivil;

public class MockDeces extends MockEvenementCivil implements Deces {

	Individu conjointSurvivant = null;

	public MockDeces(Individu individu, Long principalPPId, Individu conjoint, Long conjointPPId, RegDate date, Integer numeroOfsCommuneAnnonce) {
		super(individu, principalPPId, conjoint, conjointPPId, TypeEvenementCivil.DECES, date, numeroOfsCommuneAnnonce);
		this.conjointSurvivant = conjoint;
	}

	public Individu getConjointSurvivant() {
		return conjointSurvivant;
	}
}
