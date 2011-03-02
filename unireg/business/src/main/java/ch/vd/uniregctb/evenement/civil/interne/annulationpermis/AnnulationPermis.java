package ch.vd.uniregctb.evenement.civil.interne.annulationpermis;

import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterne;
import ch.vd.uniregctb.type.TypePermis;

/**
 * Evenement d'obtention d'un permis.
 * 
 * @author Pavel BLANCO
 *
 */
public interface AnnulationPermis extends EvenementCivilInterne {

	/**
	 * Type du permis obtenu.
	 */
	TypePermis getTypePermis();
	
	

}
