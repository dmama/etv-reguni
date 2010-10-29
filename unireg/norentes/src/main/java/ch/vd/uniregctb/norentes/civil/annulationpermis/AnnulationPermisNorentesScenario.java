package ch.vd.uniregctb.norentes.civil.annulationpermis;

import java.util.Collection;

import ch.vd.uniregctb.interfaces.model.AttributeIndividu;
import ch.vd.uniregctb.interfaces.model.Permis;
import ch.vd.uniregctb.interfaces.model.mock.MockPermis;
import ch.vd.uniregctb.norentes.common.EvenementCivilScenario;
import ch.vd.uniregctb.type.TypePermis;

public abstract class AnnulationPermisNorentesScenario extends EvenementCivilScenario {

	public AnnulationPermisNorentesScenario() {
		super();
	}

	protected MockPermis searchPermis(long numeroIndividu, TypePermis typePermis, int annee) {
		final Collection<Permis> listePermis = serviceCivilService.getIndividu(numeroIndividu, annee, AttributeIndividu.PERMIS).getPermis();
		for (Permis permis : listePermis) {
			if (permis.getTypePermis() == typePermis) {
				return (MockPermis) permis;
			}
		}
		return null;
	}

}
