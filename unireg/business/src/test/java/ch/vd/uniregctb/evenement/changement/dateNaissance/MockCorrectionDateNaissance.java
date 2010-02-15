package ch.vd.uniregctb.evenement.changement.dateNaissance;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.evenement.common.MockEvenementCivil;

public class MockCorrectionDateNaissance extends MockEvenementCivil implements CorrectionDateNaissance {

	public RegDate getDateNaissance() {
		return getDate();
	}

}
