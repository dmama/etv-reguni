package ch.vd.uniregctb.evenement.annulationpermis;

import ch.vd.registre.civil.model.EnumTypePermis;
import ch.vd.uniregctb.evenement.common.MockEvenementCivil;

public class MockAnnulationPermis extends MockEvenementCivil implements AnnulationPermis {
	
	private EnumTypePermis typePermis;
	
	/**
	 * @param typePermis the typePermis to set
	 */
	public EnumTypePermis getTypePermis() {
		return typePermis;
	}

	/**
	 * @param typePermis the typePermis to set
	 */
	public void setTypePermis(EnumTypePermis typePermis) {
		this.typePermis = typePermis;
	}

}
