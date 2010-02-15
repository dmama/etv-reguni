package ch.vd.uniregctb.evenement.naissance;

import java.util.List;

import ch.vd.uniregctb.evenement.EvenementCivil;
import ch.vd.uniregctb.interfaces.model.Individu;

/**
 * Evéneemnt de Naissance d'un individu.
 *
 * @author Ludovic Bertin
 *
 */
public interface Naissance extends EvenementCivil{

	/**
	 * Renvoie les parents du bébé.
	 * @return les parents du bébé
	 */
	List<Individu> getParents();

}
