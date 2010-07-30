package ch.vd.uniregctb.evenement.naissance;

import java.util.List;

import ch.vd.uniregctb.evenement.common.MockEvenementCivil;
import ch.vd.uniregctb.interfaces.model.Individu;

/**
 * Bouchon pour un événement de typeNaissance
 *
 * @author Ludovic Bertin
 */
public class MockNaissance extends MockEvenementCivil implements Naissance {

	List<Individu> parents = null;

	public List<Individu> getParents() {
		return parents;
	}

	public boolean isContribuablePresentBefore() {
		return false;
	}

	public void setParents(List<Individu> parents) {
		this.parents = parents;
	}
}
