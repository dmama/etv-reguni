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
 * Doublon d'entreprise, l'organisation est remplacée par une autre.
 *
 * @author Raphaël Marmier, 2015-11-05.
 */
public class DoublonEntrepriseRemplaceeParStrategy extends AbstractOrganisationStrategy {

	private static final Logger LOGGER = LoggerFactory.getLogger(DoublonEntrepriseRemplaceeParStrategy.class);

	/**
	 * Détecte les mutations pour lesquelles la création d'un événement interne est nécessaire.
	 *
	 * @param event        un événement organisation reçu de RCEnt
	 * @param organisation
	 * @param context      le context d'exécution de l'événement
	 * @param options      des options de traitement
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

		final Long remplaceParSiteAvant;
		final Long remplaceParSiteApres = organisation.getSitePrincipal(dateApres).getPayload().getIdeRemplacePar(dateApres);

		final DateRanged<SiteOrganisation> sitePrincipalAvantRange = organisation.getSitePrincipal(dateAvant);
		if (sitePrincipalAvantRange != null) {
			remplaceParSiteAvant = sitePrincipalAvantRange.getPayload().getIdeRemplacePar(dateAvant);
		}
		else {
			remplaceParSiteAvant = null;
		}

		if (remplaceParSiteAvant == null && remplaceParSiteApres != null) {
			final Entreprise entrepriseRemplacante;
			final Long noOrganisationRemplacante = context.getServiceOrganisation().getOrganisationPourSite(remplaceParSiteApres);
			if (noOrganisationRemplacante != null) {
				entrepriseRemplacante = context.getTiersService().getEntrepriseByNumeroOrganisation(noOrganisationRemplacante);
			}
			else {
				entrepriseRemplacante = null;
			}


			final String message = String.format("Doublon d’organisation à l'IDE. Cette entreprise n°%s (civil: %d) est remplacée par l'entreprise %s (civil: %d).",
			                                     FormatNumeroHelper.numeroCTBToDisplay(entreprise.getNumero()),
			                                     organisation.getNumeroOrganisation(),
			                                     entrepriseRemplacante == null ? "non encore connue d'Unireg" : "n°" + FormatNumeroHelper.numeroCTBToDisplay(entrepriseRemplacante.getNumero()),
			                                     noOrganisationRemplacante);
			LOGGER.info(message);
			return new TraitementManuel(event, organisation, entreprise, context, options, "Traitement manuel requis: " + message);
		}

		LOGGER.info("Pas de doublon, l'organisation n'est pas remplacée.");
		return null;
	}
}
