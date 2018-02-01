package ch.vd.unireg.evenement.organisation.interne.transformation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.organisation.data.DonneesRC;
import ch.vd.unireg.interfaces.organisation.data.InscriptionRC;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.unireg.interfaces.organisation.data.RaisonDeDissolutionRC;
import ch.vd.unireg.evenement.organisation.EvenementOrganisation;
import ch.vd.unireg.evenement.organisation.EvenementOrganisationContext;
import ch.vd.unireg.evenement.organisation.EvenementOrganisationException;
import ch.vd.unireg.evenement.organisation.EvenementOrganisationOptions;
import ch.vd.unireg.evenement.organisation.interne.AbstractOrganisationStrategy;
import ch.vd.unireg.evenement.organisation.interne.EvenementOrganisationInterne;
import ch.vd.unireg.evenement.organisation.interne.TraitementManuel;
import ch.vd.unireg.tiers.Entreprise;

/**
 * Evénements portant sur la fusion et la scission.
 *
 * @author Raphaël Marmier, 2016-02-18.
 */
public class DissolutionStrategy extends AbstractOrganisationStrategy {

	private static final Logger LOGGER = LoggerFactory.getLogger(DissolutionStrategy.class);

	/**
	 * @param context le context d'exécution de l'événement
	 * @param options des options de traitement
	 */
	public DissolutionStrategy(EvenementOrganisationContext context, EvenementOrganisationOptions options) {
		super(context, options);
	}

	/**
	 * Détecte les mutations pour lesquelles la création d'un événement interne est nécessaire.
	 *
	 * @param event   un événement organisation reçu de RCEnt
	 * @param organisation
	 * @return
	 * @throws EvenementOrganisationException
	 */
	@Override
	public EvenementOrganisationInterne matchAndCreate(EvenementOrganisation event, final Organisation organisation, Entreprise entreprise) throws EvenementOrganisationException {
		if (entreprise == null) {
			return null;
		}

		final RegDate dateApres = event.getDateEvenement();

		final DonneesRC donneesRC = organisation.getSitePrincipal(dateApres).getPayload().getDonneesRC();
		final InscriptionRC inscriptionRC = donneesRC.getInscription(dateApres);
		final RaisonDeDissolutionRC raisonDeDissolution = inscriptionRC != null ? inscriptionRC.getRaisonDissolutionVD() : null;
		if (raisonDeDissolution != null) {
			switch (raisonDeDissolution) {
			case FUSION:
			case LIQUIDATION:
			case FAILLITE:
			case TRANSFORMATION:
			case CARENCE_DANS_ORGANISATION:
				return new Dissolution(event, organisation, entreprise, context, options);
			default:
				return new TraitementManuel(event, organisation, entreprise, context, options, String.format("Type de dissolution inconnu: %s", raisonDeDissolution));
			}
		}
		LOGGER.info("Pas de dissolution.");
		return null;
	}
}
