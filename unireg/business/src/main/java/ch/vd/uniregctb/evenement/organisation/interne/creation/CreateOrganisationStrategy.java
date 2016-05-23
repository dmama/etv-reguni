package ch.vd.uniregctb.evenement.organisation.interne.creation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.unireg.interfaces.organisation.data.DateRanged;
import ch.vd.unireg.interfaces.organisation.data.Domicile;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.unireg.interfaces.organisation.data.OrganisationHelper;
import ch.vd.unireg.interfaces.organisation.data.SiteOrganisation;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisation;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationContext;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationException;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationOptions;
import ch.vd.uniregctb.evenement.organisation.interne.AbstractOrganisationStrategy;
import ch.vd.uniregctb.evenement.organisation.interne.EvenementOrganisationInterne;
import ch.vd.uniregctb.evenement.organisation.interne.MessageSuiviPreExecution;
import ch.vd.uniregctb.evenement.organisation.interne.TraitementManuel;
import ch.vd.uniregctb.tiers.CategorieEntrepriseHelper;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.type.CategorieEntreprise;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

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
		if (entreprise != null) {
			return null;
		}

		final RegDate dateEvenement = event.getDateEvenement();

		// On veut s'arrêter si on tombe sur un cas de siège vide.
		SiteOrganisation sitePrincipal = organisation.getSitePrincipal(dateEvenement).getPayload();
		final Domicile siege = sitePrincipal.getDomicile(dateEvenement);
		if (siege == null) {
			return new TraitementManuel(event, organisation, null, context, options,
			                            String.format(
					                            "Autorité fiscale (siège) introuvable pour le site principal %s de l'organisation %s %s. Site probablement à l'étranger. Impossible de créer le domicile de l'établissement principal.",
					                            sitePrincipal.getNumeroSite(), organisation.getNumeroOrganisation(), organisation.getNom(dateEvenement))
			);
		}

		// On contrôle si on existe avant, où et depuis quand. Si cela fait trop longtemps sur Vaud, c'est qu'on a un problème d'identification.
		final RegDate datePasseeTropAncienne = dateEvenement.getOneDayBefore().addDays(- OrganisationHelper.NB_JOURS_TOLERANCE_DE_DECALAGE_RC);
		final DateRanged<SiteOrganisation> sitePrincipalAvantRange = organisation.getSitePrincipal(datePasseeTropAncienne);
		if (sitePrincipalAvantRange != null) {
			SiteOrganisation sitePrincipalAvant = sitePrincipalAvantRange.getPayload();
			final Domicile domicilePasse = sitePrincipalAvant.getDomicile(datePasseeTropAncienne);
			if (domicilePasse != null) {
				if (domicilePasse.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD) {
					final Commune commune = context.getServiceInfra().getCommuneByNumeroOfs(domicilePasse.getNoOfs(), datePasseeTropAncienne);
					return new TraitementManuel(event, organisation, null, context, options,
					                            String.format(
							                            "L'organisation n°%d est présente sur Vaud (%s) depuis plus de %d jours et devrait être déjà connue d'Unireg. Il est très probable que l'identification n'ait pas fonctionné. Veuillez traiter le cas à la main.",
							                            organisation.getNumeroOrganisation(), commune != null ? commune.getNomOfficielAvecCanton() : "", OrganisationHelper.NB_JOURS_TOLERANCE_DE_DECALAGE_RC)
					);
				}
			}
		}

		final InformationDeDateEtDeCreation info;
		try {

		// On doit connaître la catégorie pour continuer en mode automatique
		CategorieEntreprise category = CategorieEntrepriseHelper.getCategorieEntreprise(organisation, dateEvenement);
		if (category != null) {

			// On crée une entreprise pour les organisations ayant un siège dans la canton de VD
			if (organisation.hasSitePrincipalVD(dateEvenement)) {

				switch (category) {

				// On ne crée pas d'entreprise pour les entreprises individuelles
				case PP:
					LOGGER.info("L'organisation n°{} est installée sur Vaud. Catégorie [{}] -> Pas de création.", organisation.getNumeroOrganisation(), category);
					return new MessageSuiviPreExecution(event, organisation, null, context, options,
					                                    String.format("L'organisation n°%d est une entreprise individuelle vaudoise. Pas de traitement.", organisation.getNumeroOrganisation()));

				// Sociétés de personnes
				case SP:
					LOGGER.info("L'organisation n°{} est installée sur Vaud. Catégorie [{}] -> Création.", organisation.getNumeroOrganisation(), category);
					info = extraireInformationDeDateEtDeCreation(event, organisation);
					return new CreateEntrepriseSP(event, organisation, null, context, options, info.dateDeCreation, info.isCreation);

				// Personnes morales
				case PM:
					LOGGER.info("L'organisation n°{} est installée sur Vaud. Catégorie [{}] -> Création.", organisation.getNumeroOrganisation(), category);
					info = extraireInformationDeDateEtDeCreation(event, organisation);
					return new CreateEntreprisePM(event, organisation, null, context, options, info.dateDeCreation, info.isCreation);
				// Associations personne morale
				case APM:
					LOGGER.info("L'organisation n°{} est installée sur Vaud. Catégorie [{}] -> Création.", organisation.getNumeroOrganisation(), category);
					info = extraireInformationDeDateEtDeCreation(event, organisation);
					return new CreateEntrepriseAPM(event, organisation, null, context, options, info.dateDeCreation, info.isCreation);

				// Fonds de placements
				case FP:
					LOGGER.info("L'organisation n°{} est installée sur Vaud. Catégorie [{}] -> Création.", organisation.getNumeroOrganisation(), category);
					info = extraireInformationDeDateEtDeCreation(event, organisation);
					return new CreateEntrepriseFDSPLAC(event, organisation, null, context, options, info.dateDeCreation, info.isCreation);

				// Personnes morales de droit public
				case DPPM:
					LOGGER.info("L'organisation n°{} est installée sur Vaud. Catégorie [{}] -> Création.", organisation.getNumeroOrganisation(), category);
					info = extraireInformationDeDateEtDeCreation(event, organisation);
					return new CreateEntrepriseDPPM(event, organisation, null, context, options, info.dateDeCreation, info.isCreation);

				// Catégories qu'on ne peut pas traiter automatiquement, catégories éventuellement inconnues.
				case DPAPM:
					return new TraitementManuel(event, organisation, null, context, options,
					                            "Traitement manuel requis pour nouvelle DP/APM ou organisation sans catégorie d’entreprise avec siège VD.");
				default:
					LOGGER.info("L'organisation n°{} est installée sur Vaud. Catégorie [{}] -> Traitement manuel.", organisation.getNumeroOrganisation(), category);
					return new TraitementManuel(event, organisation, null, context, options, MSG_CREATION_AUTOMATIQUE_IMPOSSIBLE);
				}
			} else if (organisation.hasSiteVD(dateEvenement)) {
				switch (category) {

				case PP:
					LOGGER.info("L'organisation n°{} a une présence secondaire sur Vaud. Catégorie [{}] -> Pas de création.", organisation.getNumeroOrganisation(), category);
					return new MessageSuiviPreExecution(event, organisation, null, context, options,
					                                    String.format("L'organisation n°%d est une entreprise individuelle hors canton avec une présence sur Vaud. Pas de traitement.", organisation.getNumeroOrganisation()));
				default:
					LOGGER.info("L'organisation n°{} a une présence secondaire sur Vaud. Catégorie [{}] -> Création.", organisation.getNumeroOrganisation(), category);
					info = extraireInformationDeDateEtDeCreation(event, organisation);
					return new CreateEntrepriseHorsVD(event, organisation, null, context, options, info.dateDeCreation, info.isCreation);
				}
			} else {
				LOGGER.info("L'organisation n°{} n'a pas de présence connue sur Vaud. Catégorie [{}] -> Pas de création.", organisation.getNumeroOrganisation(), category);
				return new MessageSuiviPreExecution(event, organisation, null, context, options,
				                                    String.format("L'organisation n°%d (%s) n'a pas de présence sur Vaud. Pas de traitement.", organisation.getNumeroOrganisation(), category));
			}
		}
		} catch (EvenementOrganisationException e) {
			return new TraitementManuel(event, organisation, null, context, options,
			                            String.format(
					                            "Erreur lors de l'examen des données RCEnt: %s",
					                            e.getMessage())
			);
		}

		// Catchall traitement manuel
		LOGGER.info("L'organisation n°{} est de catégorie indéterminée. Traitement manuel.", organisation.getNumeroOrganisation());
		return new TraitementManuel(event, organisation, null, context, options, MSG_CREATION_AUTOMATIQUE_IMPOSSIBLE);
	}

	private InformationDeDateEtDeCreation extraireInformationDeDateEtDeCreation(EvenementOrganisation event, Organisation organisation) throws EvenementOrganisationException {
		final RegDate dateEvenement = event.getDateEvenement();

		SiteOrganisation sitePrincipal = organisation.getSitePrincipal(dateEvenement).getPayload();
		final Domicile siege = sitePrincipal.getDomicile(dateEvenement);
		final boolean isVaudoise = siege.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD;
		final boolean inscritAuRC = organisation.isInscritAuRC(dateEvenement);
		final RegDate dateInscriptionRCVd;
		final RegDate dateInscriptionRC;
		final RegDate dateDeCreation;
		final boolean isCreation;
		if (inscritAuRC) {
			dateInscriptionRCVd = sitePrincipal.getDateInscriptionRCVd(dateEvenement);
			if (isVaudoise && dateInscriptionRCVd == null) {
				throw new EvenementOrganisationException("Date d'inscription au régistre vaudois du commerce introuvable pour l'établissement principal vaudois.");
			}
			dateInscriptionRC = sitePrincipal.getDateInscriptionRC(dateEvenement);
			isCreation = isCreation(event.getType(), organisation,
			                        dateEvenement); // On ne peut pas l'appeler avant car on doit d'abord s'assurer que l'inscription RC VD existe si on est inscrit au RC et vaudois.
			if (isCreation) {
				if (isVaudoise) {
					dateDeCreation = dateInscriptionRCVd.getOneDayAfter();
				}
				else {
					dateDeCreation = dateInscriptionRC.getOneDayAfter();
				}
			}
			else { // Une arrivée
				dateDeCreation = dateInscriptionRCVd;
			}
		}
		else {
			isCreation = isCreation(event.getType(), organisation, dateEvenement);
			if (isCreation) {
				dateDeCreation = dateEvenement.getOneDayAfter();
			}
			else {
				dateDeCreation = dateEvenement;
			}
		}
		return new InformationDeDateEtDeCreation(dateDeCreation, isCreation);
	}

	private static class InformationDeDateEtDeCreation {
		RegDate dateDeCreation;
		boolean isCreation;

		public InformationDeDateEtDeCreation(RegDate dateDeCreation, boolean isCreation) {
			this.dateDeCreation = dateDeCreation;
			this.isCreation = isCreation;
		}
	}
}
