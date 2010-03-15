package ch.vd.uniregctb.evenement.fin.permis;

import ch.vd.registre.civil.model.EnumTypePermis;
import ch.vd.uniregctb.evenement.EvenementCivil;

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
	EnumTypePermis getTypePermis();
	
}
