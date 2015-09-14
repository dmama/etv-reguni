package ch.vd.uniregctb.evenement.organisation.engine.translator;

import org.jetbrains.annotations.NotNull;

import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisation;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationContext;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationException;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationOptions;
import ch.vd.uniregctb.evenement.organisation.interne.EvenementOrganisationInterne;
import ch.vd.uniregctb.evenement.organisation.interne.TraitementManuel;
import ch.vd.uniregctb.tiers.Entreprise;

/**
 * Stratégie utilisable pour les événements organisation qui partent systématiquement en traitement manuel
 */
public class TraitementManuelOrganisationTranslationStrategy implements EvenementOrganisationTranslationStrategy {

	public static final String MSG = "Traitement automatique non implémenté. Veuillez effectuer cette opération manuellement.";

	@NotNull
	@Override
	public EvenementOrganisationInterne matchAndCreate(EvenementOrganisation event, Organisation organisation, EvenementOrganisationContext context, EvenementOrganisationOptions options) throws EvenementOrganisationException {

		// TODO: Implmementer la détection des cas nécessitant le départ en traitement manuel. P. ex. Décision ACI sur l'organisation.

		Entreprise entreprise = context.getTiersDAO().getEntrepriseByNumeroOrganisation(organisation.getNo());

		options.setTraitementManuelMessage(MSG);
		return new TraitementManuel(event, organisation, entreprise, context, options);

	}
}
