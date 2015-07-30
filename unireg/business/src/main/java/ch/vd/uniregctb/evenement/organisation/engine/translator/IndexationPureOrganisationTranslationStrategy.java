package ch.vd.uniregctb.evenement.organisation.engine.translator;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisation;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationContext;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationException;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationOptions;
import ch.vd.uniregctb.evenement.organisation.audit.EvenementOrganisationErreurCollector;
import ch.vd.uniregctb.evenement.organisation.audit.EvenementOrganisationWarningCollector;
import ch.vd.uniregctb.evenement.organisation.interne.EvenementOrganisationInterne;
import ch.vd.uniregctb.evenement.organisation.interne.OrganisationHandleStatus;
import ch.vd.uniregctb.tiers.Entreprise;

/**
 * Stratégie applicable aux événements organisation dont le traitement consiste en une ré-indexation seule
 */
public class IndexationPureOrganisationTranslationStrategy implements EvenementOrganisationTranslationStrategy {

	private static final String MESSAGE_INDEXATION_PURE = "Événement traité sans modification Unireg.";

	@NotNull
	@Override
	public EvenementOrganisationInterne create(final EvenementOrganisation event, Organisation organisation, EvenementOrganisationContext context, EvenementOrganisationOptions options)
			throws EvenementOrganisationException {

		return new EvenementOrganisationInterne(event, organisation, context, options) {
			@NotNull
			@Override
			public OrganisationHandleStatus handle(EvenementOrganisationWarningCollector warnings) throws EvenementOrganisationException {
				final Entreprise pm = getEntreprise();
				if (pm != null) {
					context.getIndexer().schedule(pm.getNumero());
				}
				if (!StringUtils.isBlank(event.getCommentaireTraitement())) {
					event.setCommentaireTraitement(event.getCommentaireTraitement() + " " + MESSAGE_INDEXATION_PURE);
				} else {
					event.setCommentaireTraitement(MESSAGE_INDEXATION_PURE);
				}
				return OrganisationHandleStatus.TRAITE;
			}

			@Override
			protected void validateSpecific(EvenementOrganisationErreurCollector erreurs, EvenementOrganisationWarningCollector warnings) throws EvenementOrganisationException {
				// rien à valider
			}
		};
	}

	@Override
	public EvenementOrganisationInterne match(EvenementOrganisation event, Organisation organisation, EvenementOrganisationContext context, EvenementOrganisationOptions options) throws
			EvenementOrganisationException {
		return create(event, organisation, context, options);
	}
}
