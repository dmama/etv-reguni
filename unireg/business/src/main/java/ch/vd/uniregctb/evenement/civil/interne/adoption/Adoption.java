package ch.vd.uniregctb.evenement.civil.interne.adoption;

import java.util.Date;

import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterne;

/**
 * Modélise un événement d'adoption.
 * 
 * @author <a href="mailto:abenaissi@cross-systems.com">Akram BEN AISSI </a>
 */
public interface Adoption extends EvenementCivilInterne {

	/**
	 * @return Returns the dateDebutAdoption.
	 */
	Date getDateDebutAdoption();
}
