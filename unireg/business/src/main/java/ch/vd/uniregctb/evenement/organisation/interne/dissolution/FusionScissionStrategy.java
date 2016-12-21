package ch.vd.uniregctb.evenement.organisation.interne.dissolution;

import java.util.List;

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
import ch.vd.uniregctb.evenement.organisation.interne.TraitementManuel;
import ch.vd.uniregctb.tiers.Entreprise;

/**
 * Evénements portant sur la fusion et la scission.
 *
 * @author Raphaël Marmier, 2016-02-18.
 */
public class FusionScissionStrategy extends AbstractOrganisationStrategy {

	private static final Logger LOGGER = LoggerFactory.getLogger(FusionScissionStrategy.class);

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

		final List<PublicationBusiness> publicationBusinessesPourDate = organisation.getSitePrincipal(dateApres).getPayload().getPublications(event.getDateEvenement());
		if (publicationBusinessesPourDate != null && !publicationBusinessesPourDate.isEmpty()) {
			for (PublicationBusiness publication : publicationBusinessesPourDate) { // Partant du principe qu'un seul type de fusion ne peut avoir lieu sur un même jour, on renvoie le premier trouvé.
				if (publication.getTypeDeFusion() != null) {
					switch (publication.getTypeDeFusion()) {
					case FUSION_SOCIETES_COOPERATIVES:
					case FUSION_SOCIETES_ANONYMES:
					case FUSION_SOCIETES_ANONYMES_ET_COMMANDITE_PAR_ACTIONS:
					case AUTRE_FUSION:
					case FUSION_INTERNATIONALE:
					case FUSION_ART_25_LFUS:
					case FUSION_INSTITUTIONS_DE_PREVOYANCE:
					case FUSION_SUISSE_VERS_ETRANGER:
						return new Fusion(event, organisation, entreprise, context, options);
					case SCISSION_ART_45_LFUS:
					case SCISSION_SUISSE_VERS_ETRANGER:
						return new Scission(event, organisation, entreprise, context, options);
					default:
						return new TraitementManuel(event, organisation, entreprise, context, options, String.format("Type de fusion inconnu: %s", publication.getTypeDeFusion()));
					}
				}
			}
		}
		LOGGER.info("Pas de fusion ou scission.");
		return null;
	}
}
