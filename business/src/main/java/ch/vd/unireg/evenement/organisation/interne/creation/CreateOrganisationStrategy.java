package ch.vd.unireg.evenement.organisation.interne.creation;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureException;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.unireg.interfaces.organisation.data.DateRanged;
import ch.vd.unireg.interfaces.organisation.data.Domicile;
import ch.vd.unireg.interfaces.organisation.data.FormeLegale;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.unireg.interfaces.organisation.data.SiteOrganisation;
import ch.vd.unireg.evenement.organisation.EvenementOrganisation;
import ch.vd.unireg.evenement.organisation.EvenementOrganisationContext;
import ch.vd.unireg.evenement.organisation.EvenementOrganisationException;
import ch.vd.unireg.evenement.organisation.EvenementOrganisationOptions;
import ch.vd.unireg.evenement.organisation.interne.AbstractOrganisationStrategy;
import ch.vd.unireg.evenement.organisation.interne.EvenementOrganisationInterne;
import ch.vd.unireg.evenement.organisation.interne.MessageSuiviPreExecution;
import ch.vd.unireg.evenement.organisation.interne.TraitementManuel;
import ch.vd.unireg.tiers.Entreprise;

/**
 * @author Raphaël Marmier, 2015-09-02
 */
public class CreateOrganisationStrategy extends AbstractOrganisationStrategy {

	private static final String MSG_CREATION_AUTOMATIQUE_IMPOSSIBLE = "Création automatique non prise en charge.";

	private static final Logger LOGGER = LoggerFactory.getLogger(CreateOrganisationStrategy.class);

	/**
	 * @param context le context d'exécution de l'événement
	 * @param options des options de traitement
	 */
	public CreateOrganisationStrategy(EvenementOrganisationContext context, EvenementOrganisationOptions options) {
		super(context, options);
	}

	/**
	 * Détecte les mutations pour lesquelles la création d'un événement interne {@link CreateEntreprise} est
	 * pertinente.
	 *
	 * Spécifications:
	 *  - Ti01SE03-Identifier et traiter les mutations entreprise.doc - Version 1.1 - 23.09.2015
	 *
	 * @param event   un événement organisation reçu de RCEnt
	 * @param organisation
	 * @return
	 * @throws EvenementOrganisationException
	 */
	@Override
	public EvenementOrganisationInterne matchAndCreate(EvenementOrganisation event, final Organisation organisation, Entreprise entreprise) throws EvenementOrganisationException {

		// On décide qu'on a affaire à une création uniquement selon la présence d'un tiers entreprise dans Unireg, et rien d'autre.
		if (entreprise != null) {
			return null;
		}

		final RegDate dateEvenement = event.getDateEvenement();

		final long numeroOrganisation = organisation.getNumeroOrganisation();

		try {

			SiteOrganisation sitePrincipal = getSitePrincipal(organisation, dateEvenement);

			/*
			    Organisation hors canton
			  */
			final Domicile domicile = sitePrincipal.getDomicile(dateEvenement);
			if (domicile == null) {
				// Commune de siège non renseignée au civil. Selon toute probabilité l'organisation se trouve hors canton.
				return handleSiegeVide(event, organisation, context, options, dateEvenement, sitePrincipal);
			}

			final Commune communeDomicile = getCommuneByNumeroOfs(organisation, sitePrincipal, context, dateEvenement, domicile);

			final FormeLegale formeLegale = organisation.getFormeLegale(dateEvenement);

			/*
			    Traitement manuel pour les organisations qui n'existent qu'au REE (ou autre registre non utile à la fiscalité).
			    Si la forme juridique est présente dans les données RCEnt, ne pas s'occuper des entreprises individuelles et les sociétés simples qui sont ignorées plus loin (SIFISC-25308).
			  */
			if (organisation.getNumeroIDE(dateEvenement) == null && formeLegale != FormeLegale.N_0101_ENTREPRISE_INDIVIDUELLE && formeLegale != FormeLegale.N_0302_SOCIETE_SIMPLE) {
				final String message;

				if (organisation.isInscriteAuRC(dateEvenement)) {
					message = String.format("Numéro IDE manquant pour l'organisation %s (civil: n°%d), domiciliée à %s, pourtant inscrite au RC. Impossible de continuer.",
					                        organisation.getNom(dateEvenement), numeroOrganisation, communeDomicile.getNomOfficielAvecCanton());
				}
				else if (organisation.isInscriteIDE(dateEvenement)) {
					message = String.format("Numéro IDE manquant pour l'organisation %s (civil: n°%d), domiciliée à %s, pourtant inscrite à l'IDE. Impossible de continuer.",
					                        organisation.getNom(dateEvenement), numeroOrganisation, communeDomicile.getNomOfficielAvecCanton());
				}
				else {
					message = String.format("L'organisation %s (civil: n°%d), domiciliée à %s, n'existe pas à l'IDE ni au RC. Pas de création automatique.",
					                        organisation.getNom(dateEvenement), numeroOrganisation, communeDomicile.getNomOfficielAvecCanton());
				}
				LOGGER.info(message);
				return new TraitementManuel(event, organisation, null, context, options, message);
			}

			/*
			    S'assurer qu'on a bien une forme juridique à ce stade.
			  */
			if (formeLegale == null) {
				final String message = String.format("L'organisation n°%d n'a pas de forme juridique (legalForm). Pas de création.", numeroOrganisation);
				LOGGER.info(message);
				return null;
			}

			/*
				Organisations à ignorer
			 */
			if (formeLegale == FormeLegale.N_0101_ENTREPRISE_INDIVIDUELLE) {
				return handleRaisonIndividuelle(event, organisation, context, options);
			}
			// SIFISC-19863 - Ignorer les sociétés simples
			else if (formeLegale == FormeLegale.N_0302_SOCIETE_SIMPLE) {
				return handleSocieteSimple(event, organisation, context, options);
			}

			InformationDeDateEtDeCreation info;

			/*
				Organisations VD
			 */
			if (organisation.hasSitePrincipalVD(dateEvenement)) {

				/*
					 SIFISC-19723 Pour éviter les doublons lors de la mauvaise identification d'association/fondation créées à la main par l'ACI et simultanément enregistrée par SiTi,
					 pas de création automatique des association/fondation, sauf lorsque l'inscription provient du RC, qui dans ce cas est nécessairement l'institution émettrice.
					 SIFISC-21588 et SIFISC-19660: le traitement manuel s'impose car on ne peut établir automatiquement si l'association doit être créée ou non.
				*/
				if (organisation.isAssociationFondation(dateEvenement) && !organisation.isInscriteAuRC(dateEvenement)) {
					final String message = String.format("Pas de création automatique de l'association/fondation n°%d [%s] non inscrite au RC (risque de création de doublon). " +
							                                     "Veuillez vérifier et le cas échéant créer le tiers associé à la main.",
					                                     numeroOrganisation, organisation.getNom(dateEvenement));
					LOGGER.info(message);
					return new TraitementManuel(event, organisation, null, context, options, message);
				}

				final String message = String.format("Création du tiers entreprise pour l'organisation vaudoise n°%s.", numeroOrganisation);
				LOGGER.info(message);
				info = extraireInformationDeDateEtDeCreation(event, organisation);
				return new CreateEntrepriseVD(event, organisation, null, context, options, info.getDateDeCreation(), info.getDateOuvertureFiscale(), info.isCreation());
			}
			/*
				Organisations hors VD avec présence VD
			 */
			else if (organisation.hasSiteVD(dateEvenement)) {
				final List<SiteOrganisation> succursalesRCVD = organisation.getSuccursalesRCVD(dateEvenement);
				// On ne crée l'entreprise que si elle a une présence vaudoise concrétisée par une succursale au RC VD active. Ceci pour éviter les établissements REE.
				if (succursalesRCVD.isEmpty()){
					final String message = String.format("L'organisation n°%d n'a pas de succursale active au RC Vaud (inscrite et non radiée). Pas de création.", numeroOrganisation);
					LOGGER.info(message);
					return new MessageSuiviPreExecution(event, organisation, null, context, options, message);
				}
				final String message = String.format("Création du tiers entreprise pour l'organisation non-vaudoise n°%s avec succursale vaudoise active.", numeroOrganisation);
				LOGGER.info(message);
				info = extraireInformationDeDateEtDeCreation(event, organisation);
				return new CreateEntrepriseHorsVD(event, organisation, null, context, options, info.isCreation(), succursalesRCVD);
			}
			/*
				Organisations strictement hors VD
			 */
			final String message = String.format("L'organisation n°%d n'a pas de présence sur Vaud. Pas de création.", numeroOrganisation);
			LOGGER.info(message);
			return new MessageSuiviPreExecution(event, organisation, null, context, options, message);
		}
		catch (EvenementOrganisationException e) {
			final String message = String.format("Une erreur est survenue lors de l'analyse de l'événement RCEnt: %s", e.getMessage());
			LOGGER.info(message);
			return new TraitementManuel(event, organisation, null, context, options, message);
		}
	}

	@NotNull
	private EvenementOrganisationInterne handleRaisonIndividuelle(EvenementOrganisation event, Organisation organisation, EvenementOrganisationContext context, EvenementOrganisationOptions options)
			throws EvenementOrganisationException {
		final String message = String.format("L'organisation n°%d est une entreprise individuelle %s. Pas de création.", organisation.getNumeroOrganisation(), getQualificatifLieu(organisation, event.getDateEvenement()));
		LOGGER.info(message);
		return new MessageSuiviPreExecution(event, organisation, null, context, options, message);
	}

	@NotNull
	private String getQualificatifLieu(Organisation organisation, RegDate dateEvenement) {
		return organisation.hasSitePrincipalVD(dateEvenement) ? "vaudoise" : organisation.hasSiteVD(dateEvenement) ? "hors canton avec présence sur VD" :  "strictement hors canton";
	}

	@NotNull
	private EvenementOrganisationInterne handleSocieteSimple(EvenementOrganisation event, Organisation organisation, EvenementOrganisationContext context, EvenementOrganisationOptions options)
			throws EvenementOrganisationException {
		final String message = String.format("L'organisation n°%d est une société simple %s. Pas de création.", organisation.getNumeroOrganisation(), getQualificatifLieu(organisation, event.getDateEvenement()));
		LOGGER.info(message);
		return new MessageSuiviPreExecution(event, organisation, null, context, options, message);
	}

	@NotNull
	private EvenementOrganisationInterne handleSiegeVide(EvenementOrganisation event, Organisation organisation, EvenementOrganisationContext context, EvenementOrganisationOptions options,
	                                                     RegDate dateEvenement, SiteOrganisation sitePrincipal) throws EvenementOrganisationException {
		final String message = String.format(
				"Autorité fiscale (siège) introuvable pour le site principal %s de l'organisation %s %s, en date du %s. Site probablement à l'étranger. Impossible de créer le domicile de l'établissement principal.",
				sitePrincipal.getNumeroSite(), organisation.getNumeroOrganisation(), organisation.getNom(dateEvenement), RegDateHelper.dateToDisplayString(dateEvenement));
		LOGGER.info(message);
		return new TraitementManuel(event, organisation, null, context, options, message);
	}

	@NotNull
	private SiteOrganisation getSitePrincipal(Organisation organisation, RegDate dateEvenement) throws EvenementOrganisationException {
		final DateRanged<SiteOrganisation> sitePrincipalRange = organisation.getSitePrincipal(dateEvenement);
		if (sitePrincipalRange == null) {
			final String message =
					String.format("Site principal introuvable pour l'organisation n°%d en date du %s.", organisation.getNumeroOrganisation(), RegDateHelper.dateToDisplayString(dateEvenement));
			throw new EvenementOrganisationException(message);
		}
		return sitePrincipalRange.getPayload();
	}

	@NotNull
	private Commune getCommuneByNumeroOfs(Organisation organisation, SiteOrganisation site, EvenementOrganisationContext context, RegDate dateEvenement, Domicile domicile) throws EvenementOrganisationException {
		try {
			return context.getServiceInfra().getCommuneByNumeroOfs(domicile.getNumeroOfsAutoriteFiscale(), dateEvenement);
		}
		catch (ServiceInfrastructureException e) {
			final String message = String.format("Une erreur est survenue lors de la récupération des information de la commune n°%d (Ofs) du site n°%d de l'organisation n°%d: %s",
			                                     domicile.getNumeroOfsAutoriteFiscale(), site.getNumeroSite(), organisation.getNumeroOrganisation(), e.getMessage());
			throw new EvenementOrganisationException(message);
		}
	}
}
