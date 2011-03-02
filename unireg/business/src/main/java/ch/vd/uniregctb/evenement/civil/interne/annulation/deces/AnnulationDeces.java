package ch.vd.uniregctb.evenement.civil.interne.annulation.deces;

import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterne;
import ch.vd.uniregctb.interfaces.model.Individu;

public interface AnnulationDeces extends EvenementCivilInterne {

	/**
	 * Renvoie le conjoint veuf s'il existe.
	 * @return le conjoint veuf s'il existe
	 */
	public Individu getConjointSurvivant();
}
