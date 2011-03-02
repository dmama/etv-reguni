package ch.vd.uniregctb.evenement.civil.interne.separation;

import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterne;
import ch.vd.uniregctb.interfaces.model.Individu;

/**
 * Modélise un événement de séparation.
 *  
 * @author Pavel BLANCO
 *
 */
public interface Separation extends EvenementCivilInterne {

	/**
	 * Renvoie l'ancien conjoint de l'individu.
	 * @return l'ancien conjoint de l'individu
	 */
	Individu getAncienConjoint();

}
