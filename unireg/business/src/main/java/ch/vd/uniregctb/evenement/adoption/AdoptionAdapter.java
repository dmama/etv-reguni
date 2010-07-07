package ch.vd.uniregctb.evenement.adoption;

import java.util.Date;

import org.apache.log4j.Logger;

import ch.vd.uniregctb.evenement.GenericEvenementAdapter;

/**
 * Modélise un événement d'adoption.
 * 
 * @author <a href="mailto:abenaissi@cross-systems.com">Akram BEN AISSI </a>
 */
public class AdoptionAdapter extends GenericEvenementAdapter implements Adoption {

	protected static Logger LOGGER = Logger.getLogger(AdoptionAdapter.class);

	/**
	 * La date de début d'adoption.
	 */
	private Date dateDebutAdoption;

	/**
	 * @return Returns the dateDebutAdoption.
	 */
	public Date getDateDebutAdoption() {
		return dateDebutAdoption;
	}

	/**
	 * @param dateDebutAdoption
	 *            The dateDebutAdoption to set.
	 */
	public void setDateDebutAdoption(Date dateDebutAdoption) {
		this.dateDebutAdoption = dateDebutAdoption;
	}

}
