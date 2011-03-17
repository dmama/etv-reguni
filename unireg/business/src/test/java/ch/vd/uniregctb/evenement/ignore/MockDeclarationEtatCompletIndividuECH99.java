package ch.vd.uniregctb.evenement.ignore;

import ch.vd.uniregctb.evenement.common.MockEvenementCivil;
import ch.vd.uniregctb.type.TypeEvenementCivil;

public class MockDeclarationEtatCompletIndividuECH99 extends MockEvenementCivil {

	public MockDeclarationEtatCompletIndividuECH99() {
		super();
		super.setType(TypeEvenementCivil.ETAT_COMPLET);
	}

	@Override
	public void setType(TypeEvenementCivil type) {
		throw new RuntimeException("Type is immutable");
	}
}
