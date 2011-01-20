package ch.vd.uniregctb.evenement.annulationpermis;

import ch.vd.uniregctb.evenement.EvenementCivil;
import ch.vd.uniregctb.type.TypePermis;

/**
 * Evenement d'obtention d'un permis.
 * 
 * @author Pavel BLANCO
 *
 */
public interface AnnulationPermis extends EvenementCivil {

	/**
	 * Type du permis obtenu.
	 */
	TypePermis getTypePermis();
	
	

}
