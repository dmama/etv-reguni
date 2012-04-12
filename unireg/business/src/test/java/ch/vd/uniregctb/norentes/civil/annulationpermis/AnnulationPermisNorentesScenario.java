package ch.vd.uniregctb.norentes.civil.annulationpermis;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.interfaces.model.Permis;
import ch.vd.uniregctb.interfaces.model.mock.MockPermis;
import ch.vd.uniregctb.norentes.common.EvenementCivilScenario;
import ch.vd.uniregctb.type.TypePermis;

public abstract class AnnulationPermisNorentesScenario extends EvenementCivilScenario {

	public AnnulationPermisNorentesScenario() {
		super();
	}

	protected MockPermis searchPermis(long numeroIndividu, TypePermis typePermis, RegDate date) {
		final Permis permis = serviceCivilService.getPermisActif(numeroIndividu, date);
		if (permis == null || permis.getTypePermis() != typePermis) {
			return null;
		}
		return (MockPermis) permis;
	}
}
