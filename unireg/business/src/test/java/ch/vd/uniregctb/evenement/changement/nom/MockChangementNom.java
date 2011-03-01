package ch.vd.uniregctb.evenement.changement.nom;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.evenement.common.MockEvenementCivil;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.type.TypeEvenementCivil;

public class MockChangementNom extends MockEvenementCivil implements ChangementNom {
	public MockChangementNom(Individu individu, Long principalPPId, Individu conjoint, Long conjointPPId, RegDate date, Integer numeroOfsCommuneAnnonce) {
		super(individu, principalPPId, conjoint, conjointPPId, TypeEvenementCivil.CHGT_CORREC_NOM_PRENOM, date, numeroOfsCommuneAnnonce);
	}

	public String getNouveauNom() {
		return null;
	}

	public String getNouveauPrenom() {
		return null;
	}
}
