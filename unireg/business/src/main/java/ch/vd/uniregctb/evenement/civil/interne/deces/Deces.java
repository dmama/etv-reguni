package ch.vd.uniregctb.evenement.civil.interne.deces;

import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterne;
import ch.vd.uniregctb.interfaces.model.Individu;

/**
 * Modélise un événement de décès.
 *
 * @author <a href="mailto:abenaissi@cross-systems.com">Akram BEN AISSI </a>
 */
public interface Deces extends EvenementCivilInterne {

	/**
	 * Renvoie le conjoint veuf s'il existe.
	 * @return le conjoint veuf s'il existe
	 */
	public Individu getConjointSurvivant();

}
