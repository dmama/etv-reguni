package ch.vd.uniregctb.evenement.adoption;

import java.util.Date;

import ch.vd.uniregctb.evenement.EvenementCivil;

/**
 * Modélise un événement d'adoption.
 * 
 * @author <a href="mailto:abenaissi@cross-systems.com">Akram BEN AISSI </a>
 */
public interface Adoption extends EvenementCivil {

	/**
	 * @return Returns the dateDebutAdoption.
	 */
	Date getDateDebutAdoption();
}
