package ch.vd.uniregctb.norentes.civil.annulationpermis;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.interfaces.model.AttributeIndividu;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.model.Permis;
import ch.vd.uniregctb.interfaces.model.mock.MockPermis;
import ch.vd.uniregctb.norentes.common.EvenementCivilScenario;
import ch.vd.uniregctb.type.TypePermis;

public abstract class AnnulationPermisNorentesScenario extends EvenementCivilScenario {

	public AnnulationPermisNorentesScenario() {
		super();
	}

	protected MockPermis searchPermis(long numeroIndividu, TypePermis typePermis, RegDate date) {
		final Individu ind = serviceCivilService.getIndividu(numeroIndividu, date, AttributeIndividu.PERMIS);
		if (ind == null) {
			return null;
		}
		final Permis permis = ind.getPermis().getPermisActif(date);
		if (permis == null || permis.getTypePermis() != typePermis) {
			return null;
		}
		return (MockPermis) permis;
	}
}
