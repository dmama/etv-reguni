package ch.vd.uniregctb.evenement.organisation.interne.dissolution;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.organisation.data.InscriptionRC;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.unireg.interfaces.organisation.data.PublicationBusiness;
import ch.vd.unireg.interfaces.organisation.data.SiteOrganisation;
import ch.vd.unireg.interfaces.organisation.data.StatusInscriptionRC;
import ch.vd.unireg.interfaces.organisation.data.StatusRegistreIDE;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisation;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationContext;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationException;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationOptions;
import ch.vd.uniregctb.evenement.organisation.interne.AbstractOrganisationStrategy;
import ch.vd.uniregctb.evenement.organisation.interne.EvenementOrganisationInterne;
import ch.vd.uniregctb.evenement.organisation.interne.TraitementManuel;
import ch.vd.uniregctb.tiers.Entreprise;

/**
 * Evénements portant sur la liquidation.
 *
 * @author Raphaël Marmier, 2016-02-18.
 */
public class LiquidationStrategy extends AbstractOrganisationStrategy {

	private static final Logger LOGGER = LoggerFactory.getLogger(LiquidationStrategy.class);

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
		final SiteOrganisation sitePrincipal = organisation.getSitePrincipal(dateApres).getPayload();

		final List<PublicationBusiness> publicationBusinessesDuJour = sitePrincipal.getPublications(event.getDateEvenement());
		final InscriptionRC inscriptionRC = sitePrincipal.getDonneesRC().getInscription(dateApres);
		final StatusInscriptionRC statusInscription = inscriptionRC != null ? inscriptionRC.getStatus() : null;
		final StatusRegistreIDE statusRegistreIDE = sitePrincipal.getDonneesRegistreIDE().getStatus(dateApres);

		if (statusInscription == StatusInscriptionRC.EN_LIQUIDATION
				&& statusRegistreIDE == StatusRegistreIDE.RADIE
				&& publicationBusinessesDuJour != null
				&& !publicationBusinessesDuJour.isEmpty()) {

			for (PublicationBusiness publication : publicationBusinessesDuJour) {
				if (publication.getTypeDeLiquidation() != null) { // Partant du principe qu'un seul type de liquidation ne peut avoir lieu sur un même jour, on renvoie le premier trouvé.
					switch (publication.getTypeDeLiquidation()) {
					case SOCIETE_ANONYME:
					case SOCIETE_RESPONSABILITE_LIMITE:
					case SOCIETE_COOPERATIVE:
					case ASSOCIATION:
					case FONDATION:
					case SOCIETE_NOM_COLLECTIF:
					case SOCIETE_COMMANDITE:
					case SOCIETE_COMMANDITE_PAR_ACTION:
						return new Liquidation(event, organisation, entreprise, context, options);
					default:
						return new TraitementManuel(event, organisation, entreprise, context, options, String.format("Type de liquidation inconnu: %s", publication.getTypeDeLiquidation()));
					}
				}
			}
		}
		LOGGER.info("Pas de liquidation.");
		return null;
	}
}
