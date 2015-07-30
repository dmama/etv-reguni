package ch.vd.uniregctb.evenement.organisation.engine.translator;

import ch.vd.uniregctb.evenement.organisation.EvenementOrganisation;
import ch.vd.uniregctb.evenement.organisation.interne.EvenementOrganisationInterne;

/**
 * @author Raphaël Marmier, 2015-07-29
 */
public interface EvenementOrganisationInterneFactory {
	/**
	 *
	 * @param evt
	 * @return
	 */
	EvenementOrganisationInterne extractEvent(EvenementOrganisation evt);
}
