package ch.vd.uniregctb.norentes.civil.annulationpermis;

import java.util.Collection;

import ch.vd.registre.civil.model.EnumAttributeIndividu;
import ch.vd.registre.civil.model.EnumTypePermis;
import ch.vd.uniregctb.interfaces.model.Permis;
import ch.vd.uniregctb.interfaces.model.mock.MockPermis;
import ch.vd.uniregctb.norentes.common.EvenementCivilScenario;

public abstract class AnnulationPermisNorentesScenario extends EvenementCivilScenario {

	public AnnulationPermisNorentesScenario() {
		super();
	}

	protected MockPermis searchPermis(long numeroIndividu, EnumTypePermis typePermis, int annee) {
		final Collection<Permis> listePermis = serviceCivilService.getIndividu(numeroIndividu, annee, EnumAttributeIndividu.PERMIS).getPermis();
		for (Permis permis : listePermis) {
			if (permis.getTypePermis().equals(typePermis)) {
				return (MockPermis) permis;
			}
		}
		return null;
	}

}