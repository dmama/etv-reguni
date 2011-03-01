package ch.vd.uniregctb.evenement.naissance;

import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.evenement.common.MockEvenementCivil;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.type.TypeEvenementCivil;

/**
 * Bouchon pour un événement de typeNaissance
 *
 * @author Ludovic Bertin
 */
public class MockNaissance extends MockEvenementCivil implements Naissance {

	List<Individu> parents = null;

	public MockNaissance(Individu individu, Individu conjoint, RegDate date, Integer numeroOfsCommuneAnnonce, List<Individu> parents) {
		super(individu, conjoint, TypeEvenementCivil.NAISSANCE, date, numeroOfsCommuneAnnonce);
		this.parents = parents;
	}

	public List<Individu> getParents() {
		return parents;
	}

	public boolean isContribuablePresentBefore() {
		return false;
	}
}
