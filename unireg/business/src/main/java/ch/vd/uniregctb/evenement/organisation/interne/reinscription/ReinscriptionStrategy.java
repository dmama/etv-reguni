package ch.vd.uniregctb.evenement.organisation.interne.reinscription;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.interfaces.organisation.data.DateRanged;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.unireg.interfaces.organisation.data.SiteOrganisation;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisation;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationContext;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationException;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationOptions;
import ch.vd.uniregctb.evenement.organisation.interne.AbstractOrganisationStrategy;
import ch.vd.uniregctb.evenement.organisation.interne.EvenementOrganisationInterne;
import ch.vd.uniregctb.tiers.Entreprise;

/**
 * Radiation du RC
 *
 * @author Raphaël Marmier, 2015-11-11.
 */
public class ReinscriptionStrategy extends AbstractOrganisationStrategy {

	private static final Logger LOGGER = LoggerFactory.getLogger(ReinscriptionStrategy.class);

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

		if (sitePrincipalAvantRange == null) {
			if (isExisting(organisation, dateApres)) {
				throw new EvenementOrganisationException(
						String.format("Site principal introuvable sur organisation %s en date du %s", organisation.getNumeroOrganisation(), RegDateHelper.dateToDisplayString(dateAvant)));
			}
		} else {
			final SiteOrganisation sitePrincipalAvant = sitePrincipalAvantRange.getPayload();
			final SiteOrganisation sitePrincipalApres = organisation.getSitePrincipal(dateApres).getPayload();

			final RegDate dateRadiationRCAvant = sitePrincipalAvant.getDonneesRC().getDateRadiation(dateAvant);
			final RegDate dateRadiationRCApres = sitePrincipalApres.getDonneesRC().getDateRadiation(dateApres);

			if (dateRadiationRCAvant != null && dateRadiationRCApres == null) {
				LOGGER.info(String.format("Réinscription au RC de l'entreprise %s (civil: %s).", entreprise.getNumero(), organisation.getNumeroOrganisation()));
				return new Reinscription(event, organisation, entreprise, context, options);
			}
		}
		LOGGER.info("Pas de réinscription au RC de l'entreprise.");
		return null;
	}
}
