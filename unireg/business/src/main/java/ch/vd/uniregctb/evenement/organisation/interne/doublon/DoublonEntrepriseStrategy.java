package ch.vd.uniregctb.evenement.organisation.interne.doublon;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.organisation.data.DateRanged;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.unireg.interfaces.organisation.data.SiteOrganisation;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisation;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationContext;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationException;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationOptions;
import ch.vd.uniregctb.evenement.organisation.interne.AbstractOrganisationStrategy;
import ch.vd.uniregctb.evenement.organisation.interne.EvenementOrganisationInterne;
import ch.vd.uniregctb.evenement.organisation.interne.TraitementManuel;
import ch.vd.uniregctb.tiers.Entreprise;

/**
 * Modification de capital à propager sans effet.
 *
 * @author Raphaël Marmier, 2015-11-05.
 */
public class DoublonEntrepriseStrategy extends AbstractOrganisationStrategy {

	private static final Logger LOGGER = LoggerFactory.getLogger(DoublonEntrepriseStrategy.class);

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

		final DateRanged<SiteOrganisation> sitePrincipalAvantRange = organisation.getSitePrincipal(dateAvant);
		if (sitePrincipalAvantRange != null) {

			final Long remplaceParAvant = sitePrincipalAvantRange.getPayload().getIdeRemplacePar(dateAvant);
			final Long remplaceParApres = organisation.getSitePrincipal(dateApres).getPayload().getIdeRemplacePar(dateApres);

			if (remplaceParAvant == null && remplaceParApres != null) {

				final Entreprise entrepriseRemplacante = context.getTiersService().getEntrepriseByNumeroOrganisation(remplaceParApres);

				final String message = String.format("Doublon d’organisation à l'IDE. Cette entreprise n°%s (civil: %d) est remplacée par l'entreprise %s (civil: %d).",
				                                     FormatNumeroHelper.numeroCTBToDisplay(entreprise.getNumero()),
				                                     organisation.getNumeroOrganisation(),
				                                     entrepriseRemplacante == null ? "non encore connue d'Unireg" : "n°" + FormatNumeroHelper.numeroCTBToDisplay(entrepriseRemplacante.getNumero()),
				                                     remplaceParApres);
				LOGGER.info(message);
				return new TraitementManuel(event, organisation, entreprise, context, options, "Traitement manuel requis: " + message);
			}

			final Long enRemplacementDeAvant = sitePrincipalAvantRange.getPayload().getIdeEnRemplacementDe(dateAvant);
			final Long enRemplacementDeApres = organisation.getSitePrincipal(dateApres).getPayload().getIdeEnRemplacementDe(dateApres);

			if (enRemplacementDeAvant == null && enRemplacementDeApres != null) {

				final Entreprise entrepriseRemplacee = context.getTiersService().getEntrepriseByNumeroOrganisation(enRemplacementDeApres);

				final String message = String.format("Doublon d’organisation à l'IDE. Cette entreprise n°%s (civil: %d) remplace l'entreprise %s (civil: %d).",
				                                     FormatNumeroHelper.numeroCTBToDisplay(entreprise.getNumero()),
				                                     organisation.getNumeroOrganisation(),
				                                     entrepriseRemplacee == null ? "non encore connue d'Unireg" : "n°" + FormatNumeroHelper.numeroCTBToDisplay(entrepriseRemplacee.getNumero()),
				                                     enRemplacementDeApres);
				LOGGER.info(message);
				return new TraitementManuel(event, organisation, entreprise, context, options, "Traitement manuel requis: " + message);
			}
		}

		LOGGER.info("Pas de doublon d'organisation.");
		return null;
	}
}
