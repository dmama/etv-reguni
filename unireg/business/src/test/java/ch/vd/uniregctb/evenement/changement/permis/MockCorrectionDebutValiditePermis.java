package ch.vd.uniregctb.evenement.changement.permis;

import ch.vd.registre.base.utils.NotImplementedException;
import ch.vd.uniregctb.evenement.common.MockEvenementCivil;
import ch.vd.uniregctb.type.TypeEvenementCivil;

public class MockCorrectionDebutValiditePermis extends MockEvenementCivil {

	@Override
	public boolean isContribuablePresentBefore() {
		return true;
	}

	@Override
	public TypeEvenementCivil getType() {
		return TypeEvenementCivil.CORREC_DEBUT_VALIDITE_PERMIS;
	}

	@Override
	public void setType(TypeEvenementCivil type) {
		throw new NotImplementedException();
	}
}
