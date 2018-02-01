package ch.vd.unireg.evenement.organisation.interne;

import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.unireg.evenement.organisation.EvenementOrganisation;
import ch.vd.unireg.evenement.organisation.EvenementOrganisationContext;
import ch.vd.unireg.evenement.organisation.EvenementOrganisationException;
import ch.vd.unireg.evenement.organisation.EvenementOrganisationOptions;
import ch.vd.unireg.evenement.organisation.audit.EvenementOrganisationErreurCollector;
import ch.vd.unireg.evenement.organisation.audit.EvenementOrganisationSuiviCollector;
import ch.vd.unireg.evenement.organisation.audit.EvenementOrganisationWarningCollector;
import ch.vd.unireg.tiers.Entreprise;

public class TraitementManuel extends EvenementOrganisationInterneDeTraitement {

	private static final String DEFAULT_MSG = "Cette opération doit être effectuée manuellement.";

	private String message = DEFAULT_MSG;

	public TraitementManuel(EvenementOrganisation evenement, Organisation organisation, Entreprise entreprise, EvenementOrganisationContext context,
	                        EvenementOrganisationOptions options, String message) throws EvenementOrganisationException {
		super(evenement, organisation, entreprise, context, options);
		this.message = message;
	}

	@Override
	public String describe() {
		return "Traitement manuel";
	}

	@Override
	public void doHandle(EvenementOrganisationWarningCollector warnings, EvenementOrganisationSuiviCollector suivis) throws EvenementOrganisationException {
		throw new IllegalStateException("Le traitement n'aurait jamais dû arriver jusqu'ici !");
	}

	@Override
	protected void validateSpecific(EvenementOrganisationErreurCollector erreurs, EvenementOrganisationWarningCollector warnings, EvenementOrganisationSuiviCollector suivis) throws EvenementOrganisationException {
		erreurs.addErreur(message);
	}
}
