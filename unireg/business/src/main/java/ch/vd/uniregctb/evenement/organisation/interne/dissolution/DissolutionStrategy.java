package ch.vd.uniregctb.evenement.organisation.interne.dissolution;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.organisation.data.DonneesRC;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.unireg.interfaces.organisation.data.RaisonDeDissolutionRC;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisation;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationContext;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationException;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationOptions;
import ch.vd.uniregctb.evenement.organisation.interne.AbstractOrganisationStrategy;
import ch.vd.uniregctb.evenement.organisation.interne.EvenementOrganisationInterne;
import ch.vd.uniregctb.tiers.Entreprise;

/**
 * Evénements portant sur la fusion et la scission.
 *
 * @author Raphaël Marmier, 2016-02-18.
 */
public class DissolutionStrategy extends AbstractOrganisationStrategy {

	private static final Logger LOGGER = LoggerFactory.getLogger(DissolutionStrategy.class);

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
		if (entreprise == null) {
			return null;
		}

		final RegDate dateApres = event.getDateEvenement();

		final DonneesRC donneesRC = organisation.getSitePrincipal(dateApres).getPayload().getDonneesRC();
		RaisonDeDissolutionRC raisonDeDissolution = donneesRC.getRaisonDeDissolutionVd(dateApres);
		if (raisonDeDissolution != null) {
			switch (raisonDeDissolution) {
			case FUSION:
			case LIQUIDATION:
			case FAILLITE:
			case TRANSFORMATION:
			case CARENCE_DANS_ORGANISATION:
				return new Dissolution(event, organisation, entreprise, context, options);
			default:
				throw new EvenementOrganisationException("Type de dissolution inconnu: " + raisonDeDissolution);
			}
		}
		LOGGER.info("Pas de dissolution.");
		return null;
	}
}
