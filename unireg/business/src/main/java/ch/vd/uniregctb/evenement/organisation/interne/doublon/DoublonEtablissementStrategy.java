package ch.vd.uniregctb.evenement.organisation.interne.doublon;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.unireg.interfaces.organisation.data.SiteOrganisation;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisation;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationContext;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationException;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationOptions;
import ch.vd.uniregctb.evenement.organisation.interne.AbstractOrganisationStrategy;
import ch.vd.uniregctb.evenement.organisation.interne.EvenementOrganisationInterne;
import ch.vd.uniregctb.evenement.organisation.interne.EvenementOrganisationInterneComposite;
import ch.vd.uniregctb.evenement.organisation.interne.TraitementManuel;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.Etablissement;

/**
 * Modification de capital à propager sans effet.
 *
 * @author Raphaël Marmier, 2015-11-05.
 */
public class DoublonEtablissementStrategy extends AbstractOrganisationStrategy {

	private static final Logger LOGGER = LoggerFactory.getLogger(DoublonEtablissementStrategy.class);

	/**
	 * Détecte les mutations pour lesquelles la création d'un événement interne est nécessaire.
	 *
	 * @param event   un événement organisation reçu de RCEnt
	 * @param organisation
	 * @param context le context d'exécution de l'événement
	 * @param options des options de traitement
	 * @return
	 * @throws EvenementOrganisationException
	 */
	@Override
	public EvenementOrganisationInterne matchAndCreate(EvenementOrganisation event,
	                                                   final Organisation organisation,
	                                                   Entreprise entreprise,
	                                                   EvenementOrganisationContext context,
	                                                   EvenementOrganisationOptions options) throws EvenementOrganisationException {

		// On ne s'occupe que d'entités déjà connues
		if (entreprise == null) {
			return null;
		}

		final RegDate dateAvant = event.getDateEvenement().getOneDayBefore();
		final RegDate dateApres = event.getDateEvenement();

		List<EvenementOrganisationInterne> doublons = new ArrayList<>();

		for (SiteOrganisation site : organisation.getDonneesSites()) {
			final Long remplaceParAvant = site.getIdeRemplacePar(dateAvant);
			final Long remplaceParApres = site.getIdeRemplacePar(dateApres);

			if (remplaceParAvant == null && remplaceParApres!= null) {

				final Etablissement etablissement = context.getTiersService().getEtablissementByNumeroSite(site.getNumeroSite());
				final Etablissement etablissementRemplacant = context.getTiersService().getEtablissementByNumeroSite(remplaceParApres);

				final String message = String.format("Doublon de site à l'IDE. L'établissement %s (civil: %d) est remplacé par l'établissement %s (civil: %d).",
				                                     etablissement == null ? "non encore connue d'Unireg" : "n°" + FormatNumeroHelper.numeroCTBToDisplay(etablissement.getNumero()),
				                                     site.getNumeroSite(),
				                                     etablissementRemplacant == null ? "non encore connue d'Unireg" : "n°" + FormatNumeroHelper.numeroCTBToDisplay(etablissementRemplacant.getNumero()),
				                                     remplaceParApres);
				doublons.add(new TraitementManuel(event, organisation, entreprise, context, options, "Traitement manuel requis: " + message));
			}

			final Long enRemplacementDeAvant = site.getIdeEnRemplacementDe(dateAvant);
			final Long enRemplacementDeApres = site.getIdeEnRemplacementDe(dateApres);

			if (enRemplacementDeAvant == null && enRemplacementDeApres!= null) {

				final Etablissement etablissement = context.getTiersService().getEtablissementByNumeroSite(site.getNumeroSite());
				final Etablissement etablissementRemplace = context.getTiersService().getEtablissementByNumeroSite(enRemplacementDeApres);

				final String message = String.format("Doublon de site à l'IDE. L'établissement %s (civil: %d) remplace l'établissement %s (civil: %d).",
				                                     etablissement == null ? "non encore connue d'Unireg" : "n°" + FormatNumeroHelper.numeroCTBToDisplay(etablissement.getNumero()),
				                                     site.getNumeroSite(),
				                                     etablissementRemplace == null ? "non encore connue d'Unireg" : "n°" + FormatNumeroHelper.numeroCTBToDisplay(etablissementRemplace.getNumero()),
				                                     enRemplacementDeApres);
				doublons.add(new TraitementManuel(event, organisation, entreprise, context, options, "Traitement manuel requis: " + message));
			}
		}

		if (!doublons.isEmpty()) {
			if (doublons.size() == 1) {
				LOGGER.info("Un doublon d'établissement détecté.");
				return doublons.get(0);
			} else {
				LOGGER.info(String.format("%d doublons d'établissement détectés.", doublons.size()));
				return new EvenementOrganisationInterneComposite(event, organisation, entreprise, context, options, doublons);
			}
		}
		LOGGER.info("Pas de doublon d'établissement.");
		return null;
	}
}
