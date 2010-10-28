package ch.vd.uniregctb.evenement.fin.permis;

import ch.vd.uniregctb.evenement.common.MockEvenementCivil;
import ch.vd.uniregctb.type.TypePermis;

public class MockFinPermis extends MockEvenementCivil implements FinPermis {

	TypePermis typePermis;
	
	public TypePermis getTypePermis() {
		return typePermis;
	}

	public void setTypePermis(TypePermis typePermis) {
		this.typePermis = typePermis;
	}
	
}
