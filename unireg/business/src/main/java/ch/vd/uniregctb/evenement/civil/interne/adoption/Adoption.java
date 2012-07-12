package ch.vd.uniregctb.evenement.civil.interne.adoption;

import java.util.Date;

import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.utils.NotImplementedException;
import ch.vd.uniregctb.evenement.civil.EvenementCivilErreurCollector;
import ch.vd.uniregctb.evenement.civil.EvenementCivilWarningCollector;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilException;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilOptions;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterne;
import ch.vd.uniregctb.evenement.civil.interne.HandleStatus;
import ch.vd.uniregctb.evenement.civil.regpp.EvenementCivilRegPP;

/**
 * Modélise un événement d'adoption.
 * 
 * @author <a href="mailto:abenaissi@cross-systems.com">Akram BEN AISSI </a>
 */
public class Adoption extends EvenementCivilInterne {

	protected static Logger LOGGER = Logger.getLogger(Adoption.class);

	/**
	 * La date de début d'adoption.
	 */
	private Date dateDebutAdoption;

	protected Adoption(EvenementCivilRegPP evenement, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
		super(evenement, context, options);
	}

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

	@Override
	public void validateSpecific(EvenementCivilErreurCollector erreurs, EvenementCivilWarningCollector warnings) throws EvenementCivilException {
		throw new NotImplementedException();
	}

	@NotNull
	@Override
	public HandleStatus handle(EvenementCivilWarningCollector warnings) throws EvenementCivilException {
		throw new NotImplementedException();
	}
}
