package ch.vd.uniregctb.evenement.changement.identificateur;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.evenement.common.MockEvenementCivil;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.type.TypeEvenementCivil;

public class MockChangementIdentificateur extends MockEvenementCivil implements ChangementIdentificateur {

	public MockChangementIdentificateur(Individu individu, Long principalPPId, Individu conjoint, Long conjointPPId, RegDate date, Integer numeroOfsCommuneAnnonce) {
		super(individu, principalPPId, conjoint, conjointPPId, TypeEvenementCivil.CHGT_CORREC_IDENTIFICATION, date, numeroOfsCommuneAnnonce);
	}

	public String getNouvelIdentificateur() {
		return null;
	}
}
