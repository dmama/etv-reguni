package ch.vd.uniregctb.evenement.civil.interne.mariage;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.evenement.civil.common.MockEvenementCivil;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.type.TypeEvenementCivil;

public class MockMariage extends MockEvenementCivil implements Mariage {

	private Individu nouveauConjoint;

	public MockMariage(Individu individu, Long principalPPId, Individu conjoint, Long conjointPPId, RegDate date, Integer numeroOfsCommuneAnnonce) {
		super(individu, principalPPId, conjoint, conjointPPId, TypeEvenementCivil.MARIAGE, date, numeroOfsCommuneAnnonce);
		this.nouveauConjoint = conjoint;
	}

	public Individu getNouveauConjoint() {
		return nouveauConjoint;
	}

	public boolean isContribuablePresentBefore() {
		return true;
	}
}
