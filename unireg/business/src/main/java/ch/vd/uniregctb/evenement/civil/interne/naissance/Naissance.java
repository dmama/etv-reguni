package ch.vd.uniregctb.evenement.civil.interne.naissance;

import java.util.List;

import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterne;
import ch.vd.uniregctb.interfaces.model.Individu;

/**
 * Evéneemnt de Naissance d'un individu.
 *
 * @author Ludovic Bertin
 *
 */
public interface Naissance extends EvenementCivilInterne {

	/**
	 * Renvoie les parents du bébé.
	 * @return les parents du bébé
	 */
	List<Individu> getParents();

}
