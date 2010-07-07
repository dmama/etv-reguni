package ch.vd.uniregctb.evenement.deces;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.evenement.common.MockEvenementCivil;
import ch.vd.uniregctb.interfaces.model.Adresse;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.type.TypeEvenementCivil;

public class MockDeces extends MockEvenementCivil implements Deces {

	Individu conjointSurvivant = null;

	public Individu getConjointSurvivant() {
		return conjointSurvivant;
	}

	public void setConjointSurvivant(Individu conjointSurvivant) {
		this.conjointSurvivant = conjointSurvivant;
	}
}
