package ch.vd.uniregctb.evenement.fin.nationalite;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.evenement.common.MockEvenementCivil;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.type.TypeEvenementCivil;

public class MockFinNationalite extends MockEvenementCivil implements FinNationalite {
	public MockFinNationalite(Individu individu, Individu conjoint, RegDate date, Integer numeroOfsCommuneAnnonce, boolean nationaliteSuisse) {
		super(individu, conjoint, nationaliteSuisse ? TypeEvenementCivil.FIN_NATIONALITE_SUISSE : TypeEvenementCivil.FIN_NATIONALITE_NON_SUISSE, date, numeroOfsCommuneAnnonce);
	}
}
