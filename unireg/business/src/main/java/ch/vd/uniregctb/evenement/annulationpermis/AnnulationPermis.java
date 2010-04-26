package ch.vd.uniregctb.evenement.annulationpermis;

import ch.vd.registre.civil.model.EnumTypePermis;
import ch.vd.uniregctb.evenement.EvenementCivil;

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
	EnumTypePermis getTypePermis();
	
	

}
