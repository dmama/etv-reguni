package ch.vd.uniregctb.evenement.organisation.interne;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisation;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationContext;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationException;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationOptions;
import ch.vd.uniregctb.evenement.organisation.audit.EvenementOrganisationErreurCollector;
import ch.vd.uniregctb.evenement.organisation.audit.EvenementOrganisationWarningCollector;
import ch.vd.uniregctb.tiers.Entreprise;

/**
 * @author Raphaël Marmier, 2015-09-04
 */
public class IndexationPure extends EvenementOrganisationInterne {

	private static final String MESSAGE_INDEXATION_PURE = "Événement traité sans modification Unireg.";

	EvenementOrganisation event;

	public IndexationPure(EvenementOrganisation evenement, Organisation organisation, Entreprise entreprise,
	                      EvenementOrganisationContext context, EvenementOrganisationOptions options) throws
			EvenementOrganisationException {
		super(evenement, organisation, entreprise, context, options);
		event = evenement;
	}

	@NotNull
	@Override
	public HandleStatus handle(EvenementOrganisationWarningCollector warnings) throws EvenementOrganisationException {
		final Entreprise pm = getEntreprise();
		if (pm != null) {
			context.getIndexer().schedule(pm.getNumero());
		}
		if (!StringUtils.isBlank(event.getCommentaireTraitement())) {
			event.setCommentaireTraitement(event.getCommentaireTraitement() + " " + MESSAGE_INDEXATION_PURE);
		} else {
			event.setCommentaireTraitement(MESSAGE_INDEXATION_PURE);
		}
		return HandleStatus.TRAITE;
	}

	@Override
	protected void validateSpecific(EvenementOrganisationErreurCollector erreurs, EvenementOrganisationWarningCollector warnings) throws EvenementOrganisationException {
		// rien à valider
	}
}
