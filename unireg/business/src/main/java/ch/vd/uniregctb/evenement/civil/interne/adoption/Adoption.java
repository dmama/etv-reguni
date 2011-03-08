package ch.vd.uniregctb.evenement.civil.interne.adoption;

import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;

import ch.vd.registre.base.utils.NotImplementedException;
import ch.vd.registre.base.utils.Pair;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilHandlerException;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilInterneException;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterne;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterneErreur;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterne;
import ch.vd.uniregctb.tiers.PersonnePhysique;

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

	protected Adoption(EvenementCivilExterne evenement, EvenementCivilContext context) throws EvenementCivilInterneException {
		super(evenement, context);
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
	public void checkCompleteness(List<EvenementCivilExterneErreur> erreurs, List<EvenementCivilExterneErreur> warnings) {
		throw new NotImplementedException();
	}

	@Override
	public void validateSpecific(List<EvenementCivilExterneErreur> erreurs, List<EvenementCivilExterneErreur> warnings) {
		throw new NotImplementedException();
	}

	@Override
	public Pair<PersonnePhysique, PersonnePhysique> handle(List<EvenementCivilExterneErreur> warnings) throws EvenementCivilHandlerException {
		throw new NotImplementedException();
	}
}
