package ch.vd.uniregctb.evenement.civil.interne.fin.permis;

import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterne;
import ch.vd.uniregctb.type.TypePermis;

/**
 * Evénement de fin obtention d'un permis.
 * 
 * @author Pavel BLANCO
 *
 */
public interface FinPermis extends EvenementCivilInterne {

	/**
	 * Type du permis obtenu.
	 */
	TypePermis getTypePermis();
	
}
