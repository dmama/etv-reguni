package ch.vd.uniregctb.evenement.mariage;

import ch.vd.uniregctb.evenement.EvenementCivil;
import ch.vd.uniregctb.interfaces.model.Individu;

public interface Mariage extends EvenementCivil {

	/**
	 * Renvoie le nouveau conjoint de l'individu.
	 * @return nouveau conjoint de l'individu
	 */
	Individu getNouveauConjoint();

}