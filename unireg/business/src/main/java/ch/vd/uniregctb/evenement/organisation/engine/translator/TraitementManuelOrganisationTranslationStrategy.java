package ch.vd.uniregctb.evenement.organisation.engine.translator;

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

/**
 * TODO: Est-ce vraiment applicable à RCEnt sous cette forme? On devrait avoir cette notion au niveau des evts interne, puisqu'il peut y en avoir plusieurs.
 *
 * Stratégie utilisable pour les événements organisation qui partent systématiquement en traitement manuel
 */
public class TraitementManuelOrganisationTranslationStrategy implements EvenementOrganisationTranslationStrategy {

	public static final String MSG = "Traitement automatique non implémenté. Veuillez effectuer cette opération manuellement.";

	@NotNull
	@Override
	public EvenementOrganisationInterne create(EvenementOrganisation event, Organisation organisation, EvenementOrganisationContext context, EvenementOrganisationOptions options) throws EvenementOrganisationException {
		return new EvenementOrganisationInterne(event, organisation, context, options) {

			@NotNull
			@Override
			public OrganisationHandleStatus handle(EvenementOrganisationWarningCollector warnings) throws EvenementOrganisationException {
				throw new IllegalArgumentException("Le traitement n'aurait jamais dû arriver jusqu'ici !");
			}

			@Override
			protected void validateSpecific(EvenementOrganisationErreurCollector erreurs, EvenementOrganisationWarningCollector warnings) throws EvenementOrganisationException {
				erreurs.addErreur(MSG);
			}
		};
	}

	@Override
	public EvenementOrganisationInterne match(EvenementOrganisation event, Organisation organisation, EvenementOrganisationContext context, EvenementOrganisationOptions options) throws EvenementOrganisationException {
		return create(event, organisation, context, options);
	}
}
