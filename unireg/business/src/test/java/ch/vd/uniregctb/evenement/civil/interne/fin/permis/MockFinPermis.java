package ch.vd.uniregctb.evenement.civil.interne.fin.permis;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.evenement.civil.common.MockEvenementCivil;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.type.TypeEvenementCivil;
import ch.vd.uniregctb.type.TypePermis;

public class MockFinPermis extends MockEvenementCivil implements FinPermis {

	TypePermis typePermis;

	public MockFinPermis(Individu individu, Individu conjoint, RegDate date, Integer numeroOfsCommuneAnnonce, TypePermis typePermis) {
		super(individu, conjoint, TypeEvenementCivil.FIN_CHANGEMENT_CATEGORIE_ETRANGER, date, numeroOfsCommuneAnnonce);
		this.typePermis = typePermis;
	}

	public TypePermis getTypePermis() {
		return typePermis;
	}
}
