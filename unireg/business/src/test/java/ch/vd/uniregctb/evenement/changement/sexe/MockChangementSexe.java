package ch.vd.uniregctb.evenement.changement.sexe;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.evenement.common.MockEvenementCivil;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypeEvenementCivil;

public class MockChangementSexe extends MockEvenementCivil implements ChangementSexe {

	public MockChangementSexe(Individu individu, Long principalPPId, Individu conjoint, Long conjointPPId, RegDate date, Integer numeroOfsCommuneAnnonce) {
		super(individu, principalPPId, conjoint, conjointPPId, TypeEvenementCivil.CHGT_SEXE, date, numeroOfsCommuneAnnonce);
	}

	public Sexe getNouveauSexe() {
		return null;
	}
}
