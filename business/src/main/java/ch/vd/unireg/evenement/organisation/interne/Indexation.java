package ch.vd.unireg.evenement.organisation.interne;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.unireg.evenement.organisation.EvenementOrganisation;
import ch.vd.unireg.evenement.organisation.EvenementOrganisationContext;
import ch.vd.unireg.evenement.organisation.EvenementOrganisationException;
import ch.vd.unireg.evenement.organisation.EvenementOrganisationOptions;
import ch.vd.unireg.evenement.organisation.audit.EvenementOrganisationErreurCollector;
import ch.vd.unireg.evenement.organisation.audit.EvenementOrganisationSuiviCollector;
import ch.vd.unireg.evenement.organisation.audit.EvenementOrganisationWarningCollector;
import ch.vd.unireg.tiers.Entreprise;

/**
 * Evénement de réindexation silencieuse, à créer lorsqu'on veut être sûr que l'entité sera
 * réindexée, mais sans que cette réindexation soit rapportée dans le cadre du traitement de l'événement,
 * ni que le status ne soit modifié.
 *
 * @author Raphaël Marmier, 2015-09-04
 */
public class Indexation extends EvenementOrganisationInterneInformationPure {

	private static final Logger LOGGER = LoggerFactory.getLogger(Indexation.class);

	final EvenementOrganisation event;

	public Indexation(EvenementOrganisation evenement, Organisation organisation, Entreprise entreprise,
	                  EvenementOrganisationContext context, EvenementOrganisationOptions options) throws
			EvenementOrganisationException {
		super(evenement, organisation, entreprise, context, options);
		event = evenement;
	}

	@Override
	public String describe() {
		return "Indexation";
	}

	@Override
	public void doHandle(EvenementOrganisationWarningCollector warnings, EvenementOrganisationSuiviCollector suivis) throws EvenementOrganisationException {
		final Entreprise pm = getEntreprise();
		if (pm != null) {
			programmeReindexation(pm, suivis);

		}
		// Cet événement reste en status REDONDANT. Utiliser la classe dérivée IndexationPure pour obtenir un statut TRAITE.
	}

	@Override
	protected void validateSpecific(EvenementOrganisationErreurCollector erreurs, EvenementOrganisationWarningCollector warnings, EvenementOrganisationSuiviCollector suivis) throws EvenementOrganisationException {
		// rien à valider
	}
}
