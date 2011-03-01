package ch.vd.uniregctb.evenement.changement.dateNaissance;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.evenement.common.MockEvenementCivil;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.type.TypeEvenementCivil;

public class MockCorrectionDateNaissance extends MockEvenementCivil implements CorrectionDateNaissance {

	public MockCorrectionDateNaissance(Individu individu, Long principalPPId, Individu conjoint, Long conjointPPId, RegDate date, Integer numeroOfsCommuneAnnonce) {
		super(individu, principalPPId, conjoint, conjointPPId, TypeEvenementCivil.CORREC_DATE_NAISSANCE, date, numeroOfsCommuneAnnonce);
	}

	public RegDate getDateNaissance() {
		return getDate();
	}

}
