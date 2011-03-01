package ch.vd.uniregctb.evenement.tutelle;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.evenement.common.MockEvenementCivil;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.type.TypeEvenementCivil;

public class MockLeveeTutelle extends MockEvenementCivil implements LeveeTutelle {

	public MockLeveeTutelle(Individu individu, Individu conjoint, RegDate date, Integer numeroOfsCommuneAnnonce) {
		super(individu, conjoint, TypeEvenementCivil.LEVEE_TUTELLE, date, numeroOfsCommuneAnnonce);
	}
}
