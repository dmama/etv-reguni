package ch.vd.uniregctb.evenement.veuvage;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.evenement.common.MockEvenementCivil;

public class MockVeuvage extends MockEvenementCivil implements Veuvage {

	public RegDate getDateVeuvage() {
		return getDate();
	}

}
