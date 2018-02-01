package ch.vd.unireg.evenement.organisation.interne;

import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.unireg.evenement.organisation.EvenementOrganisation;
import ch.vd.unireg.evenement.organisation.EvenementOrganisationAbortException;
import ch.vd.unireg.evenement.organisation.EvenementOrganisationContext;
import ch.vd.unireg.evenement.organisation.EvenementOrganisationException;
import ch.vd.unireg.evenement.organisation.EvenementOrganisationOptions;
import ch.vd.unireg.evenement.organisation.audit.EvenementOrganisationErreurCollector;
import ch.vd.unireg.evenement.organisation.audit.EvenementOrganisationSuiviCollector;
import ch.vd.unireg.evenement.organisation.audit.EvenementOrganisationWarningCollector;
import ch.vd.unireg.tiers.Entreprise;

public class CappingEnErreur extends EvenementOrganisationInterneDeTraitement {

	/**
	 * Exception lancée dans le cadre du capping
	 */
	public static class CappingException extends EvenementOrganisationAbortException {
		public CappingException(String message) {
			super(message);
		}
	}

	public CappingEnErreur(EvenementOrganisation evenement, Organisation organisation, Entreprise entreprise,
	                       EvenementOrganisationContext context, EvenementOrganisationOptions options) {
		super(evenement, organisation, entreprise, context, options);
	}

	@Override
	public String describe() {
		return null;        // On ne veut pas de message descriptif sur cet événement qui n'en est pas un.
	}

	@Override
	protected void validateSpecific(EvenementOrganisationErreurCollector erreurs, EvenementOrganisationWarningCollector warnings, EvenementOrganisationSuiviCollector suivis) throws EvenementOrganisationException {
		// rien à signaler, c'est plus tard qu'on va se manifester...
	}

	@Override
	public void doHandle(EvenementOrganisationWarningCollector warnings, EvenementOrganisationSuiviCollector suivis) throws EvenementOrganisationException {
		// et boom !!
		// il faut faire sauter la transaction mais conserver les messages récupérés jusque là... (d'où la classe spécifique d'exception lancée)
		throw new CappingException("Evénement explicitement placé 'en erreur' par configuration applicative. Toutes les modifications apportées pendant le traitement sont abandonnées.");
	}
}
