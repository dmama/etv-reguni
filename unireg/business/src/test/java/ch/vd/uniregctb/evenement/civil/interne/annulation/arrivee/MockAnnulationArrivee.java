package ch.vd.uniregctb.evenement.civil.interne.annulation.arrivee;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.evenement.civil.common.MockEvenementCivil;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.type.TypeEvenementCivil;

public class MockAnnulationArrivee extends MockEvenementCivil implements AnnulationArrivee {

	public MockAnnulationArrivee(Individu individu, Long principalPPId, Individu conjoint, Long conjointPPId, RegDate date, Integer numeroOfsCommuneAnnonce) {
		super(individu, principalPPId, conjoint, conjointPPId, TypeEvenementCivil.SUP_ARRIVEE_DANS_COMMUNE, date, numeroOfsCommuneAnnonce);
	}
}
