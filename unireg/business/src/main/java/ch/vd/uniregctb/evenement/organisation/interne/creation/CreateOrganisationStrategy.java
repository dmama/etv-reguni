package ch.vd.uniregctb.evenement.organisation.interne.creation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisation;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationContext;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationException;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationOptions;
import ch.vd.uniregctb.evenement.organisation.interne.AbstractOrganisationStrategy;
import ch.vd.uniregctb.evenement.organisation.interne.EvenementOrganisationInterne;
import ch.vd.uniregctb.evenement.organisation.interne.TraitementManuel;
import ch.vd.uniregctb.tiers.CategorieEntrepriseHelper;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.type.CategorieEntreprise;

/**
 * @author Raphaël Marmier, 2015-09-02
 */
public class CreateOrganisationStrategy extends AbstractOrganisationStrategy {

	private static final String MSG_CREATION_AUTOMATIQUE_IMPOSSIBLE = "Création automatique non prise en charge.";

	private static final Logger LOGGER = LoggerFactory.getLogger(CreateOrganisationStrategy.class);

	/**
	 * Détecte les mutations pour lesquelles la création d'un événement interne {@link CreateEntreprise} est
	 * pertinente.
	 *
	 * Spécifications:
	 *  - Ti01SE03-Identifier et traiter les mutations entreprise.doc - Version 1.1 - 23.09.2015
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

		// On décide qu'on a affaire à une création uniquement selon la présence d'un tiers entreprise dans Unireg, et rien d'autre.
		// TODO: Retrouver aussi les entreprises n'ayant pas d'id cantonal.
		if (entreprise != null) {
			return null;
		}

		final RegDate dateEvenement = event.getDateEvenement();

		// On doit connaître la catégorie pour continuer en mode automatique
		CategorieEntreprise category = CategorieEntrepriseHelper.getCategorieEntreprise(organisation, dateEvenement);
		if (category != null) {

			// On crée une entreprise pour les organisations ayant un siège dans la canton de VD
			if (hasSitePrincipalVD(organisation, dateEvenement)) {

				switch (category) {

				// On ne crée pas d'entreprise pour les entreprises individuelles
				case PP:
					LOGGER.info("L'entité organisation {} est installée sur Vaud. Catégorie [{}] -> Pas de création.", organisation.getNumeroOrganisation(), category);
					return null;

				// Sociétés de personnes
				case SP:
					LOGGER.info("L'entité organisation {} est installée sur Vaud. Catégorie [{}] -> Création.", organisation.getNumeroOrganisation(), category);
					return new CreateEntrepriseSP(event, organisation, null, context, options);

				// Personnes morales
				case PM:
					LOGGER.info("L'entité organisation {} est installée sur Vaud. Catégorie [{}] -> Création.", organisation.getNumeroOrganisation(), category);
					return new CreateEntreprisePM(event, organisation, null, context, options);
				// Associations personne morale
				case APM:
					LOGGER.info("L'entité organisation {} est installée sur Vaud. Catégorie [{}] -> Création.", organisation.getNumeroOrganisation(), category);
					return new CreateEntrepriseAPM(event, organisation, null, context, options);

				// Fonds de placements
				case FP:
					LOGGER.info("L'entité organisation {} est installée sur Vaud. Catégorie [{}] -> Création.", organisation.getNumeroOrganisation(), category);
					return new CreateEntrepriseFDSPLAC(event, organisation, null, context, options);

				// Personnes morales de droit public
				case DPPM:
					LOGGER.info("L'entité organisation {} est installée sur Vaud. Catégorie [{}] -> Création.", organisation.getNumeroOrganisation(), category);
					return new CreateEntrepriseDPPM(event, organisation, null, context, options);

				// Catégories qu'on ne peut pas traiter automatiquement, catégories éventuellement inconnues.
				case DP:
				default:
					LOGGER.info("L'entité organisation {} est installée sur Vaud. Catégorie [{}] -> Traitement manuel.", organisation.getNumeroOrganisation(), category);
					return new TraitementManuel(event, organisation, null, context, options, MSG_CREATION_AUTOMATIQUE_IMPOSSIBLE);
				}
			} else if (hasSiteVD(organisation, dateEvenement)) {
				switch (category) {

				case PP:
					LOGGER.info("L'entité organisation {} a une présence secondaire sur Vaud. Catégorie [{}] -> Pas de création.", organisation.getNumeroOrganisation(), category);
					return null;
				default:
					LOGGER.info("L'entité organisation {} a une présence secondaire sur Vaud. Catégorie [{}] -> Création.", organisation.getNumeroOrganisation(), category);
					return new CreateEntrepriseHorsVD(event, organisation, null, context, options);
				}
			} else {
				LOGGER.info("L'entité organisation {} n'a pas de présence connue sur Vaud. Catégorie [{}] -> Pas de création.", organisation.getNumeroOrganisation(), category);
				return null;
			}
		}

		// Catchall traitement manuel
		LOGGER.info("L'entité organisation {} est de catégorie indéterminée. Traitement manuel.", organisation.getNumeroOrganisation());
		return new TraitementManuel(event, organisation, null, context, options, MSG_CREATION_AUTOMATIQUE_IMPOSSIBLE);
	}
}
