package ch.vd.uniregctb.evenement.annulation.deces;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.evenement.common.MockEvenementCivil;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.type.TypeEvenementCivil;

public class MockAnnulationDeces extends MockEvenementCivil implements AnnulationDeces {

	Individu conjointSurvivant = null;

	public MockAnnulationDeces(Individu individu, Individu conjoint, RegDate date, Integer numeroOfsCommuneAnnonce) {
		super(individu, conjoint, TypeEvenementCivil.ANNUL_DECES, date, numeroOfsCommuneAnnonce);
		this.conjointSurvivant = conjoint;
	}

	public MockAnnulationDeces(Individu individu, Long principalPPId, Individu conjoint, Long conjointPPId, RegDate date, Integer numeroOfsCommuneAnnonce) {
		super(individu, principalPPId, conjoint, conjointPPId, TypeEvenementCivil.ANNUL_DECES, date, numeroOfsCommuneAnnonce);
		this.conjointSurvivant = conjoint;
	}

	public Individu getConjointSurvivant() {
		return conjointSurvivant;
	}
}
