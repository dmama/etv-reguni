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
 * Doublon d'entreprise, l'organisation en remplace une autre.
 *
 * @author Raphaël Marmier, 2015-11-05.
 */
public class DoublonEntrepriseRemplacanteStrategy extends AbstractOrganisationStrategy {

	private static final Logger LOGGER = LoggerFactory.getLogger(DoublonEntrepriseRemplacanteStrategy.class);

	/**
	 * @param context le context d'exécution de l'événement
	 * @param options des options de traitement
	 */
	public DoublonEntrepriseRemplacanteStrategy(EvenementOrganisationContext context, EvenementOrganisationOptions options) {
		super(context, options);
	}

	/**
	 * Détecte les mutations pour lesquelles la création d'un événement interne est nécessaire.
	 *
	 * @param event        un événement organisation reçu de RCEnt
	 * @param organisation
	 * @return
	 * @throws EvenementOrganisationException
	 */
	@Override
	public EvenementOrganisationInterne matchAndCreate(EvenementOrganisation event, final Organisation organisation, Entreprise entreprise) throws EvenementOrganisationException {

		// On ne s'occupe que d'entités déjà connues
		if (entreprise == null) {
			return null;
		}

		final RegDate dateAvant = event.getDateEvenement().getOneDayBefore();
		final RegDate dateApres = event.getDateEvenement();

		final Long enRemplacementSiteDeAvant;
		final Long enRemplacementDeSiteApres = organisation.getSitePrincipal(dateApres).getPayload().getIdeEnRemplacementDe(dateApres);

		final DateRanged<SiteOrganisation> sitePrincipalAvantRange = organisation.getSitePrincipal(dateAvant);
		if (sitePrincipalAvantRange != null) {
			enRemplacementSiteDeAvant = sitePrincipalAvantRange.getPayload().getIdeEnRemplacementDe(dateAvant);
		}
		else {
			enRemplacementSiteDeAvant = null;
		}

		if (enRemplacementSiteDeAvant == null && enRemplacementDeSiteApres != null) {
			final Entreprise entrepriseRemplacee;
			final Long noOrganisationRemplacee = context.getServiceOrganisation().getOrganisationPourSite(enRemplacementDeSiteApres);
			if (noOrganisationRemplacee != null) {
				entrepriseRemplacee = context.getTiersService().getEntrepriseByNumeroOrganisation(noOrganisationRemplacee);
			}
			else {
				entrepriseRemplacee = null;
			}

			final String message = String.format("Doublon d’organisation à l'IDE. Cette entreprise n°%s (civil: %d) remplace l'entreprise %s (civil: %d).",
			                                     FormatNumeroHelper.numeroCTBToDisplay(entreprise.getNumero()),
			                                     organisation.getNumeroOrganisation(),
			                                     entrepriseRemplacee == null ? "non encore connue d'Unireg" : "n°" + FormatNumeroHelper.numeroCTBToDisplay(entrepriseRemplacee.getNumero()),
			                                     noOrganisationRemplacee);
			LOGGER.info(message);
			return new TraitementManuel(event, organisation, entreprise, context, options, "Traitement manuel requis: " + message);
		}

		LOGGER.info("Pas de doublon, l'organisation n'en remplace pas d'autre.");
		return null;
	}
}
