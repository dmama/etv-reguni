package ch.vd.uniregctb.evenement.organisation.interne.creation;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.unireg.interfaces.organisation.data.Domicile;
import ch.vd.unireg.interfaces.organisation.data.FormeLegale;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
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

		final InformationDeDateEtDeCreation info;
		try {

			// Entreprises qui n'existent qu'au REE (ou autre registre non utile à la fiscalité)
			if (organisation.getNumeroIDE(dateEvenement) == null) {
				final String message;

				if (organisation.isInscriteAuRC(dateEvenement)) {
					message = String.format("Numéro IDE manquant pour l'organisation %s (civil: n°%d), domiciliée à %s, pourtant inscrite au RC. Impossible de continuer.",
					                                     organisation.getNom(dateEvenement),
					                                     organisation.getNumeroOrganisation(),
					                                     getCommuneDomicile(sitePrincipal, dateEvenement, context).getNomOfficielAvecCanton()
					);
				}
				else if (organisation.isInscriteIDE(dateEvenement)) {
					message = String.format("Numéro IDE manquant pour l'organisation %s (civil: n°%d), domiciliée à %s, pourtant inscrite à l'IDE. Impossible de continuer.",
					                                     organisation.getNom(dateEvenement),
					                                     organisation.getNumeroOrganisation(),
					                                     getCommuneDomicile(sitePrincipal, dateEvenement, context).getNomOfficielAvecCanton()
					);
				}
				else {
					message = String.format("L'organisation %s (civil: n°%d), domiciliée à %s, n'existe pas à l'IDE ni au RC. Pas de création automatique.",
					                                     organisation.getNom(dateEvenement),
					                                     organisation.getNumeroOrganisation(),
					                                     getCommuneDomicile(sitePrincipal, dateEvenement, context).getNomOfficielAvecCanton()
					);
				}
				LOGGER.info(message);
				return new TraitementManuel(event, organisation, null, context, options, message);
			}

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
						                                    String.format("L'organisation n°%d est une entreprise individuelle vaudoise. Pas de création.", organisation.getNumeroOrganisation()));

					// Sociétés de personnes
					case SP:
						LOGGER.info("L'organisation n°{} est installée sur Vaud. Catégorie [{}] -> Création.", organisation.getNumeroOrganisation(), category);
						info = extraireInformationDeDateEtDeCreation(event, organisation);
						return new CreateEntrepriseSP(event, organisation, null, context, options, info.getDateDeCreation(), info.getDateOuvertureFiscale(), info.isCreation());

					// Personnes morales
					case PM:
						LOGGER.info("L'organisation n°{} est installée sur Vaud. Catégorie [{}] -> Création.", organisation.getNumeroOrganisation(), category);
						info = extraireInformationDeDateEtDeCreation(event, organisation);
						return new CreateEntreprisePM(event, organisation, null, context, options, info.getDateDeCreation(), info.getDateOuvertureFiscale(), info.isCreation());
					// Associations personne morale
					case APM:
						/*
						 SIFISC-19723 Pour éviter les doublons lors de la mauvaise identification d'APM créées à la main par l'ACI et simultanément enregistrée par SiTi,
						 pas de création automatique des APM, sauf lorsque l'inscription provient du RC, qui dans ce cas est nécessairement l'institution émettrice.
						 SIFISC-21588 et SIFISC-19660: le traitement manuel s'impose car on ne peut établir automatiquement si l'association doit être créée ou non.
						  */
						if (organisation.isInscriteAuRC(dateEvenement)) {
							LOGGER.info("L'organisation inscrite au RC n°{} est installée sur Vaud. Catégorie [{}] -> Création.", organisation.getNumeroOrganisation(), category);
							info = extraireInformationDeDateEtDeCreation(event, organisation);
							return new CreateEntrepriseAPMAuRC(event, organisation, null, context, options, info.getDateDeCreation(), info.getDateOuvertureFiscale(), info.isCreation());
						}
						else {
							LOGGER.info("L'organisation n°{}, installée sur Vaud, n'est pas inscrite au RC. Catégorie [{}] -> Traitement manurel.", organisation.getNumeroOrganisation(), category);
							final String message = String.format("Pas de création automatique de l'APM n°%d [%s] non inscrite au RC (risque de création de doublon). " +
									                                     "Veuillez vérifier et le cas échéant créer le tiers associé à la main.",
							                                     organisation.getNumeroOrganisation(), organisation.getNom(dateEvenement));
							return new TraitementManuel(event, organisation, null, context, options, message);
						}
					// Fonds de placements
					case FP:
						LOGGER.info("L'organisation n°{} est installée sur Vaud. Catégorie [{}] -> Création.", organisation.getNumeroOrganisation(), category);
						info = extraireInformationDeDateEtDeCreation(event, organisation);
						return new CreateEntrepriseFDSPLAC(event, organisation, null, context, options, info.getDateDeCreation(), info.getDateOuvertureFiscale(), info.isCreation());

					// Personnes morales de droit public
					case DPPM:
						LOGGER.info("L'organisation n°{} est installée sur Vaud. Catégorie [{}] -> Création.", organisation.getNumeroOrganisation(), category);
						info = extraireInformationDeDateEtDeCreation(event, organisation);
						return new CreateEntrepriseDPPM(event, organisation, null, context, options, info.getDateDeCreation(), info.getDateOuvertureFiscale(), info.isCreation());

					// Catégories qu'on ne peut pas traiter automatiquement, catégories éventuellement inconnues.
					case DPAPM:
						return new TraitementManuel(event, organisation, null, context, options,
						                            "Traitement manuel requis pour nouvelle DP/APM ou organisation sans catégorie d’entreprise avec siège VD.");
					default:
						// SIFISC-19863 - Ignorer les sociétés simples
						if (organisation.getFormeLegale(dateEvenement) == FormeLegale.N_0302_SOCIETE_SIMPLE) {
							LOGGER.info("L'organisation n°{} est installée sur Vaud. Catégorie [{}] -> Pas de création.", organisation.getNumeroOrganisation(), category);
							return new MessageSuiviPreExecution(event, organisation, null, context, options,
							                                    String.format("L'organisation n°%d est une société simple vaudoise. Pas de création.", organisation.getNumeroOrganisation()));
						}
						LOGGER.info("L'organisation n°{} est installée sur Vaud. Catégorie [{}] -> Traitement manuel.", organisation.getNumeroOrganisation(), category);
						return new TraitementManuel(event, organisation, null, context, options, MSG_CREATION_AUTOMATIQUE_IMPOSSIBLE);
					}
				}
				if (organisation.hasSiteVD(dateEvenement)) {
					switch (category) {

					case PP:
						LOGGER.info("L'organisation n°{} a une présence secondaire sur Vaud. Catégorie [{}] -> Pas de création.", organisation.getNumeroOrganisation(), category);
						return new MessageSuiviPreExecution(event, organisation, null, context, options,
						                                    String.format("L'organisation n°%d est une entreprise individuelle hors canton avec une présence sur Vaud. Pas de traitement.", organisation.getNumeroOrganisation()));
					default:
						// SIFISC-19863 - Ignorer les sociétés simples
						if (organisation.getFormeLegale(dateEvenement) == FormeLegale.N_0302_SOCIETE_SIMPLE) {
							LOGGER.info("L'organisation n°{} a une présence secondaire sur Vaud. Catégorie [{}] -> Pas de création.", organisation.getNumeroOrganisation(), category);
							return new MessageSuiviPreExecution(event, organisation, null, context, options,
							                                    String.format("L'organisation n°%d est une société simple hors canton avec une présence sur Vaud. Pas de traitement.", organisation.getNumeroOrganisation()));
						}
						final List<SiteOrganisation> succursalesRCVD = organisation.getSuccursalesRCVD(dateEvenement);
						// On ne crée l'entreprise que si elle a une présence vaudoise concrétisée par une succursale au RC VD active. Ceci pour éviter les établissements REE.
						if (succursalesRCVD.isEmpty()) {
							final String message =
									String.format("L'organisation n°%d (catégorie: %s) n'a pas de succursale active au RC Vaud (inscrite et non radiée). Pas de création.", organisation.getNumeroOrganisation(), category);
							LOGGER.info(message);
							return new MessageSuiviPreExecution(event, organisation, null, context, options,
							                                    message);
						}
						LOGGER.info("L'organisation n°{} a une ou plusieurs succursales sur Vaud. Catégorie [{}] -> Création.", organisation.getNumeroOrganisation(), category);
						info = extraireInformationDeDateEtDeCreation(event, organisation);
						return new CreateEntrepriseHorsVD(event, organisation, null, context, options, info.isCreation(), succursalesRCVD);
					}
				} else {
					final String message = String.format("L'organisation n°%d (%s) n'a pas de présence sur Vaud. Pas de création.", organisation.getNumeroOrganisation(), category);
					LOGGER.info(message);
					return new MessageSuiviPreExecution(event, organisation, null, context, options,
					                                    message);
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
		final String message = String.format("L'organisation %s (civil: n°%d), domiciliée à %s, est de catégorie indéterminée (forme juridique inconnue). Traitement manuel.",
		                                     organisation.getNom(dateEvenement),
		                                     organisation.getNumeroOrganisation(),
		                                     getCommuneDomicile(sitePrincipal, dateEvenement, context).getNomOfficielAvecCanton()
		);
		LOGGER.info(message);
		return new TraitementManuel(event, organisation, null, context, options, message);
	}

	private Commune getCommuneDomicile(SiteOrganisation site, RegDate dateEvenement, EvenementOrganisationContext context) {
		final Domicile domicile = site.getDomicile(dateEvenement);
		if (domicile != null) {
			return context.getServiceInfra().getCommuneByNumeroOfs(domicile.getNumeroOfsAutoriteFiscale(), dateEvenement);
		}
		return null;
	}

}
