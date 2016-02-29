package ch.vd.uniregctb.norentes.civil.annulationpermis;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.data.AttributeIndividu;
import ch.vd.unireg.interfaces.civil.data.Individu;
import ch.vd.unireg.interfaces.civil.data.Permis;
import ch.vd.unireg.interfaces.civil.mock.MockPermis;
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
