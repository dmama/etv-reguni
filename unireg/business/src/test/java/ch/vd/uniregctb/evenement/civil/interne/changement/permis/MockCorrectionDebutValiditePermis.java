package ch.vd.uniregctb.evenement.civil.interne.changement.permis;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.evenement.civil.common.MockEvenementCivil;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.type.TypeEvenementCivil;

public class MockCorrectionDebutValiditePermis extends MockEvenementCivil {

	public MockCorrectionDebutValiditePermis(Individu individu, Long principalPPId, Individu conjoint, Long conjointPPId, RegDate date, Integer numeroOfsCommuneAnnonce) {
		super(individu, principalPPId, conjoint, conjointPPId, TypeEvenementCivil.CORREC_DEBUT_VALIDITE_PERMIS, date, numeroOfsCommuneAnnonce);
	}

	@Override
	public boolean isContribuablePresentBefore() {
		return true;
	}
}
