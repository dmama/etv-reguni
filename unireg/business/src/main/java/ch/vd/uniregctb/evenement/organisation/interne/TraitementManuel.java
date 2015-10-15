package ch.vd.uniregctb.evenement.organisation.interne;

import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisation;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationContext;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationException;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationOptions;
import ch.vd.uniregctb.evenement.organisation.audit.EvenementOrganisationErreurCollector;
import ch.vd.uniregctb.evenement.organisation.audit.EvenementOrganisationWarningCollector;
import ch.vd.uniregctb.tiers.Entreprise;

public class TraitementManuel extends EvenementOrganisationInterne {

	private static final String DEFAULT_MSG = "Cette opération doit être effectuée manuellement.";

	private String message = DEFAULT_MSG;

	public TraitementManuel(EvenementOrganisation evenement, Organisation organisation, Entreprise entreprise, EvenementOrganisationContext context,
	                        EvenementOrganisationOptions options, String message) throws EvenementOrganisationException {
		super(evenement, organisation, entreprise, context, options);
		this.message = message;
	}

	@Override
	public void doHandle(EvenementOrganisationWarningCollector warnings) throws EvenementOrganisationException {
		throw new IllegalStateException("Le traitement n'aurait jamais dû arriver jusqu'ici !");
	}

	@Override
	protected void validateSpecific(EvenementOrganisationErreurCollector erreurs, EvenementOrganisationWarningCollector warnings) throws EvenementOrganisationException {
		erreurs.addErreur(message);
	}
}
