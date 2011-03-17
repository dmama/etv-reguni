package ch.vd.uniregctb.evenement.separation;

import ch.vd.uniregctb.evenement.EvenementCivil;
import ch.vd.uniregctb.interfaces.model.Individu;

/**
 * Modélise un événement de séparation.
 *  
 * @author Pavel BLANCO
 *
 */
public interface Separation extends EvenementCivil {

	/**
	 * Renvoie l'ancien conjoint de l'individu.
	 * @return l'ancien conjoint de l'individu
	 */
	Individu getAncienConjoint();

}
