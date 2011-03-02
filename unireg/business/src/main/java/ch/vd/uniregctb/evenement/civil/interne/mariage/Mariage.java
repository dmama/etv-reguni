package ch.vd.uniregctb.evenement.civil.interne.mariage;

import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterne;
import ch.vd.uniregctb.interfaces.model.Individu;

public interface Mariage extends EvenementCivilInterne {

	/**
	 * Renvoie le nouveau conjoint de l'individu.
	 * @return nouveau conjoint de l'individu
	 */
	Individu getNouveauConjoint();

}