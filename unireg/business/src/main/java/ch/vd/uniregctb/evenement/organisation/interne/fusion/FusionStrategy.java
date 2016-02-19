package ch.vd.uniregctb.evenement.organisation.interne.fusion;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.unireg.interfaces.organisation.data.PublicationBusiness;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisation;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationContext;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationException;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationOptions;
import ch.vd.uniregctb.evenement.organisation.interne.AbstractOrganisationStrategy;
import ch.vd.uniregctb.evenement.organisation.interne.EvenementOrganisationInterne;
import ch.vd.uniregctb.tiers.Entreprise;

/**
 * Evénements portant sur la fusion.
 *
 * @author Raphaël Marmier, 2016-02-18.
 */
public class FusionStrategy extends AbstractOrganisationStrategy {

	private static final Logger LOGGER = LoggerFactory.getLogger(FusionStrategy.class);

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

		final Map<RegDate, List<PublicationBusiness>> publications = organisation.getSitePrincipal(event.getDateEvenement()).getPayload().getPublications();
		if (publications != null && !publications.isEmpty()) {
			for (PublicationBusiness publication : publications.get(event.getDateEvenement())) {
				if (publication.getTypeDeFusion() != null) {
					switch (publication.getTypeDeFusion()) {
					case SOCIETES_COOPERATIVES:
					case SOCIETES_ANONYMES:
					case SOCIETES_ANONYMES_ET_COMMANDITE_PAR_ACTIONS:
					case AUTRE_FUSION:
					case FUSION_INTERNATIONALE:
					case FUSION_ART_25_LFUS:
					case INSTITUTIONS_DE_PREVOYANCE:
					case FUSION_SUISSE_VERS_ETRANGER:
						return new Fusion(event, organisation, entreprise, context, options);
					default:
						// rien
					}
				}
			}
		}
		LOGGER.info("Pas de fusion.");
		return null;
	}
}
