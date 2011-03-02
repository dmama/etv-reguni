package ch.vd.uniregctb.evenement.civil.interne.changement.arrivee;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.evenement.civil.common.MockEvenementCivil;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.type.TypeEvenementCivil;

public class MockCorrectionDateArrivee extends MockEvenementCivil implements CorrectionDateArrivee {

	public MockCorrectionDateArrivee(Individu individu, Long principalPPId, Individu conjoint, Long conjointPPId, RegDate date, Integer numeroOfsCommuneAnnonce) {
		super(individu, principalPPId, conjoint, conjointPPId, TypeEvenementCivil.CORREC_DATE_ARRIVEE, date, numeroOfsCommuneAnnonce);
	}
}
