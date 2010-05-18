package ch.vd.uniregctb.evenement.mariage;

import ch.vd.uniregctb.evenement.common.MockEvenementCivil;
import ch.vd.uniregctb.interfaces.model.Individu;

public class MockMariage extends MockEvenementCivil implements Mariage {

	private Individu nouveauConjoint;

	public Individu getNouveauConjoint() {
		return nouveauConjoint;
	}

	public void setNouveauConjoint(Individu nouveauConjoint) {
		this.nouveauConjoint = nouveauConjoint;
	}

	public boolean isContribuablePresentBefore() {
		return true;
	}
}
