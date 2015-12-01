package ch.vd.uniregctb.evenement.organisation.interne.demenagement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.unireg.interfaces.organisation.data.Siege;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisation;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationContext;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationException;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationOptions;
import ch.vd.uniregctb.evenement.organisation.interne.AbstractOrganisationStrategy;
import ch.vd.uniregctb.evenement.organisation.interne.EvenementOrganisationInterne;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

/**
 * @author Raphaël Marmier, 2015-09-02
 */
public class DemenagementSiegeStrategy extends AbstractOrganisationStrategy {

	private static final Logger LOGGER = LoggerFactory.getLogger(DemenagementSiegeStrategy.class);

	private static final String MESSAGE_HORS_SUISSE = "Les changements de siège impliquant un/des sièges hors Suisse ne sont pas pris en charge. Veuillez traiter manuellement.";

	/**
	 * Détecte les mutations pour lesquelles la création d'un événement interne est pertinente.
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

		// On vérifie qu'on a bien retrouvé l'entreprise concernée par ce type de changement
		// TODO: Retrouver aussi les entreprises n'ayant pas d'id cantonal.
		if (entreprise == null) {
			return null;
		}

		final RegDate dateAvant = event.getDateEvenement().getOneDayBefore();
		final RegDate dateApres = event.getDateEvenement();

		final Siege communeDeSiegeAvant = organisation.getSiegePrincipal(dateAvant);
		final Siege communeDeSiegeApres = organisation.getSiegePrincipal(dateApres);


		if (communeDeSiegeApres == null) {
			throw new EvenementOrganisationException(MESSAGE_HORS_SUISSE);
		}

		if (communeDeSiegeAvant == null) {
			if (isExisting(organisation, dateApres)) {
				throw new EvenementOrganisationException(MESSAGE_HORS_SUISSE);
			} else {
				LOGGER.info("Pas de déménagement trouvé car l'organisation n'était pas connue avant au civil.");
				return null; // On n'existait pas hier, en fait.
			}
		}

		// Passé ce point on a forcément un déménagement

		if (communeDeSiegeAvant.getNoOfs() == communeDeSiegeApres.getNoOfs()) { // Pas un changement, pas de traitement
			LOGGER.info("Pas de changement d'autorité politique. La commune d'autorité fiscale reste no {}", communeDeSiegeAvant.getNoOfs());
			return null;
		}
		else if (isDemenagementVD(communeDeSiegeAvant, communeDeSiegeApres)) {
			LOGGER.info("Déménagement VD -> VD: commune {} vers commune {}.", communeDeSiegeAvant.getNoOfs(), communeDeSiegeApres.getNoOfs());
			return new DemenagementVD(event, organisation, entreprise, context, options);
		}
		else if (isDemenagementHC(communeDeSiegeAvant, communeDeSiegeApres)) {
			LOGGER.info("Déménagement HC -> HC: commune {} vers commune {}.", communeDeSiegeAvant.getNoOfs(), communeDeSiegeApres.getNoOfs());
			return new DemenagementHC(event, organisation, entreprise, context, options);
		}
		else if (isDepart(communeDeSiegeAvant, communeDeSiegeApres)) {
			LOGGER.info("Départ VD -> HC: commune {} vers commune {}.", communeDeSiegeAvant.getNoOfs(), communeDeSiegeApres.getNoOfs());
			return new DemenagementDepart(event, organisation, entreprise, context, options);
		}
		else if (isArrivee(communeDeSiegeAvant, communeDeSiegeApres)) {
			LOGGER.info("Arrivée HC -> VD: commune {} vers commune {}.", communeDeSiegeAvant.getNoOfs(), communeDeSiegeApres.getNoOfs());
			return new DemenagementArrivee(event, organisation, entreprise, context, options);
		}
		else {
			throw new EvenementOrganisationException(
					String.format("Il existe manifestement un type de siège qu'Unireg ne sait pas traiter. Type avant: %s. Type après: %s. Impossible de continuer.",
					              communeDeSiegeAvant.getTypeAutoriteFiscale(), communeDeSiegeApres.getTypeAutoriteFiscale()));
		}
	}

	private boolean isDemenagementVD(Siege siegeAvant, Siege siegeApres) {
		return siegeAvant.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD &&
				siegeApres.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD;
	}

	private boolean isDemenagementHC(Siege siegeAvant, Siege siegeApres) {
		return siegeAvant.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_HC &&
				siegeApres.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_HC;
	}

	private boolean isDepart(Siege siegeAvant, Siege siegeApres) {
		return siegeAvant.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD &&
				(siegeApres.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_HC || siegeApres.getTypeAutoriteFiscale() == TypeAutoriteFiscale.PAYS_HS);
	}

	private boolean isArrivee(Siege siegeAvant, Siege siegeApres) {
		return (siegeAvant.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_HC || siegeAvant.getTypeAutoriteFiscale() == TypeAutoriteFiscale.PAYS_HS) &&
				siegeApres.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD;
	}
}
