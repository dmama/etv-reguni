package ch.vd.uniregctb.evenement.organisation.interne;

import org.jetbrains.annotations.NotNull;

import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisation;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationContext;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationException;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationOptions;
import ch.vd.uniregctb.evenement.organisation.engine.translator.EvenementOrganisationTranslationStrategy;
import ch.vd.uniregctb.tiers.Entreprise;

/**
 * Stratégie applicable aux événements organisation dont le traitement consiste en une ré-indexation seule
 */
public class IndexationPureOrganisationTranslationStrategy implements EvenementOrganisationTranslationStrategy {

	@NotNull
	@Override
	public EvenementOrganisationInterne matchAndCreate(final EvenementOrganisation event, Organisation organisation, Entreprise entreprise, EvenementOrganisationContext context, EvenementOrganisationOptions options)
			throws EvenementOrganisationException {

		return new IndexationPure(event, organisation, entreprise, context, options);
	}
}
