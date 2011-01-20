package ch.vd.uniregctb.evenement.fin.permis;

import ch.vd.uniregctb.evenement.EvenementCivil;
import ch.vd.uniregctb.type.TypePermis;

/**
 * Evénement de fin obtention d'un permis.
 * 
 * @author Pavel BLANCO
 *
 */
public interface FinPermis extends EvenementCivil {

	/**
	 * Type du permis obtenu.
	 */
	TypePermis getTypePermis();
	
}
