package ch.vd.uniregctb.evenement.civil.interne.annulationtutelle;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.evenement.civil.common.MockEvenementCivil;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.type.TypeEvenementCivil;

public class MockAnnulationLeveeTutelle extends MockEvenementCivil implements AnnulationLeveeTutelle {
	public MockAnnulationLeveeTutelle(Individu individu, Individu conjoint, RegDate date, Integer numeroOfsCommuneAnnonce) {
		super(individu, conjoint, TypeEvenementCivil.ANNUL_LEVEE_TUTELLE, date, numeroOfsCommuneAnnonce);
	}
}
