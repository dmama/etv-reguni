package ch.vd.uniregctb.evenement.fin.permis;

import ch.vd.registre.civil.model.EnumTypePermis;
import ch.vd.uniregctb.evenement.common.MockEvenementCivil;

public class MockFinPermis extends MockEvenementCivil implements FinPermis {

	EnumTypePermis typePermis;
	
	public EnumTypePermis getTypePermis() {
		return typePermis;
	}

	public void setTypePermis(EnumTypePermis typePermis) {
		this.typePermis = typePermis;
	}
	
}
