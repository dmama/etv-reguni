package ch.vd.uniregctb.evenement.civil.interne.changement.nationalite;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.evenement.civil.common.MockEvenementCivil;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.type.TypeEvenementCivil;

public class MockRemiseBlancDateFinNationalite extends MockEvenementCivil implements RemiseBlancDateFinNationalite {

	public MockRemiseBlancDateFinNationalite(Individu individu, Individu conjoint, RegDate date, Integer numeroOfsCommuneAnnonce, boolean suisse) {
		super(individu, conjoint, (suisse ? TypeEvenementCivil.ANNUL_DATE_FIN_NATIONALITE_SUISSE : TypeEvenementCivil.ANNUL_DATE_FIN_NATIONALITE_NON_SUISSE), date, numeroOfsCommuneAnnonce);
	}
}
