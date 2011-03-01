package ch.vd.uniregctb.evenement.annulationpermis;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.evenement.common.MockEvenementCivil;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.type.TypeEvenementCivil;
import ch.vd.uniregctb.type.TypePermis;

public class MockAnnulationPermis extends MockEvenementCivil implements AnnulationPermis {
	
	private TypePermis typePermis;

	public MockAnnulationPermis(Individu individu, Individu conjoint, RegDate date, Integer numeroOfsCommuneAnnonce, TypePermis typePermis) {
		super(individu, conjoint, TypeEvenementCivil.ANNUL_CATEGORIE_ETRANGER, date, numeroOfsCommuneAnnonce);
		this.typePermis = typePermis;
	}

	public TypePermis getTypePermis() {
		return typePermis;
	}
}
