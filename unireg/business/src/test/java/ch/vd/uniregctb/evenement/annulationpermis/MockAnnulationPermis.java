package ch.vd.uniregctb.evenement.annulationpermis;

import ch.vd.uniregctb.evenement.common.MockEvenementCivil;
import ch.vd.uniregctb.type.TypePermis;

public class MockAnnulationPermis extends MockEvenementCivil implements AnnulationPermis {
	
	private TypePermis typePermis;
	
	/**
	 */
	public TypePermis getTypePermis() {
		return typePermis;
	}

	/**
	 * @param typePermis the typePermis to set
	 */
	public void setTypePermis(TypePermis typePermis) {
		this.typePermis = typePermis;
	}

}
