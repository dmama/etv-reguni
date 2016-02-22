package ch.vd.uniregctb.evenement.organisation.engine.translator;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.StringsUtils;
import ch.vd.unireg.interfaces.organisation.data.DateRanged;
import ch.vd.unireg.interfaces.organisation.data.FormeLegale;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.unireg.interfaces.organisation.data.SiteOrganisation;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.common.CollectionsUtils;
import ch.vd.uniregctb.data.DataEventService;
import ch.vd.uniregctb.evenement.fiscal.EvenementFiscalService;
import ch.vd.uniregctb.evenement.identification.contribuable.CriteresEntreprise;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisation;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationContext;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationException;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationOptions;
import ch.vd.uniregctb.evenement.organisation.interne.EvenementOrganisationInterne;
import ch.vd.uniregctb.evenement.organisation.interne.EvenementOrganisationInterneComposite;
import ch.vd.uniregctb.evenement.organisation.interne.Indexation;
import ch.vd.uniregctb.evenement.organisation.interne.IndexationPure;
import ch.vd.uniregctb.evenement.organisation.interne.MessageSuivi;
import ch.vd.uniregctb.evenement.organisation.interne.TraitementManuel;
import ch.vd.uniregctb.evenement.organisation.interne.creation.CreateOrganisationStrategy;
import ch.vd.uniregctb.evenement.organisation.interne.decisionaci.DecisionAciStrategy;
import ch.vd.uniregctb.evenement.organisation.interne.demenagement.DemenagementSiegeStrategy;
import ch.vd.uniregctb.evenement.organisation.interne.dissolution.DissolutionStrategy;
import ch.vd.uniregctb.evenement.organisation.interne.dissolution.FusionScissionStrategy;
import ch.vd.uniregctb.evenement.organisation.interne.dissolution.LiquidationStrategy;
import ch.vd.uniregctb.evenement.organisation.interne.doublon.DoublonEntrepriseStrategy;
import ch.vd.uniregctb.evenement.organisation.interne.doublon.DoublonEtablissementStrategy;
import ch.vd.uniregctb.evenement.organisation.interne.formejuridique.ChangementFormeJuridiqueStrategy;
import ch.vd.uniregctb.evenement.organisation.interne.information.FailliteConcordatStrategy;
import ch.vd.uniregctb.evenement.organisation.interne.information.ModificationButsStrategy;
import ch.vd.uniregctb.evenement.organisation.interne.information.ModificationCapitalStrategy;
import ch.vd.uniregctb.evenement.organisation.interne.information.ModificationStatutsStrategy;
import ch.vd.uniregctb.evenement.organisation.interne.radiation.RadiationStrategy;
import ch.vd.uniregctb.evenement.organisation.interne.reinscription.ReinscriptionStrategy;
import ch.vd.uniregctb.identification.contribuable.IdentificationContribuableService;
import ch.vd.uniregctb.identification.contribuable.TooManyIdentificationPossibilitiesException;
import ch.vd.uniregctb.indexer.tiers.GlobalTiersIndexer;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.interfaces.service.ServiceOrganisationService;
import ch.vd.uniregctb.metier.MetierServicePM;
import ch.vd.uniregctb.metier.assujettissement.AssujettissementService;
import ch.vd.uniregctb.parametrage.ParametreAppService;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.RaisonSocialeFiscaleEntreprise;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.TiersService;

/**
 * TODO: triple check everything
 * Convertisseur d'événements reçus de RCEnt en événements organisation internes
 */
public class EvenementOrganisationTranslatorImpl implements EvenementOrganisationTranslator, InitializingBean {

	private static final Logger LOGGER = LoggerFactory.getLogger(EvenementOrganisationTranslatorImpl.class);

	private ServiceOrganisationService serviceOrganisationService;
	private ServiceInfrastructureService serviceInfrastructureService;
	private TiersDAO tiersDAO;
	private DataEventService dataEventService;
	private TiersService tiersService;
	private MetierServicePM metierServicePM;
	private AdresseService adresseService;
	private GlobalTiersIndexer indexer;
	private IdentificationContribuableService identCtbService;
	private EvenementFiscalService evenementFiscalService;
	private AssujettissementService assujettissementService;
	private ParametreAppService parametreAppService;

	/*
	 * Non injecté mais créé ci-dessous dans afterPropertiesSet()
	 */
	private EvenementOrganisationContext context;
	private List<EvenementOrganisationTranslationStrategy> strategies;

	@Override
	public void afterPropertiesSet() throws Exception {
		context = new EvenementOrganisationContext(serviceOrganisationService, serviceInfrastructureService, dataEventService, tiersService, indexer, metierServicePM, tiersDAO, adresseService,
		                                           evenementFiscalService, assujettissementService, parametreAppService);

		// Construction des stratégies
		strategies = new ArrayList<>();

		/*
			L'ordre des stratégies est important.
		 */
		strategies.add(new DecisionAciStrategy());
		strategies.add(new CreateOrganisationStrategy());
		strategies.add(new ReinscriptionStrategy());
		strategies.add(new ChangementFormeJuridiqueStrategy());
		strategies.add(new FailliteConcordatStrategy());
		strategies.add(new ModificationCapitalStrategy());
		strategies.add(new ModificationButsStrategy());
		strategies.add(new ModificationStatutsStrategy());
		strategies.add(new DoublonEntrepriseStrategy());
		strategies.add(new DoublonEtablissementStrategy());
		strategies.add(new DemenagementSiegeStrategy());
		strategies.add(new RadiationStrategy());
		strategies.add(new DissolutionStrategy());
		strategies.add(new FusionScissionStrategy());
		strategies.add(new LiquidationStrategy());
	}

	/**
	 * Traduit un événement organisation en zéro, un ou plusieurs événements externes correspondant (Dans ce cas, un événement composite est retourné.
	 * <p>
	 *     AVERTISSEMENT: Contrairement au traitement des événements Civil Ech, pour lesquel un cablage explicite des stratégies est en vigueur, le traitement des événements
	 *     organisation fait l'objet d'un essai de chaque stratégie disponible, chacune détectant un cas particulier de changement et donnant lieu à un événement interne.
	 *     De fait, les nouveaux types d'événements organisation sont donc silencieusement ignorés jusqu'à ce qu'une stratégie soit codée pour les reconnaître.
	 * </p>
	 * <p>
	 *     NOTE: Au cas ou l'on ne trouve pas de tiers par le biais du numéro cantonal, le service d'identification est utilisé pour rechercher le tiers correspondant
	 *     par le numéro IDE et la raison sociale. Si cette recherche ne retourne aucun tiers on considère que le tiers n'existe pas en base. Dans tous les cas, un
	 *     événement interne de suivi est généré à titre de renseignement.
	 * </p>
	 * <p>
	 *     Au cas ou aucune stratégie ne retourne d'événement interne, un événement d'indexation pure est AUTOMATIQUEMENT ajouté. Dans le cas contraire, un événement
	 *     d'indexation neutre (qui ne modifie pas le statut) est ajouté afin que la réindexation du tiers ait lieu quoi qu'il arrive.
	 * </p>
	 * @param event   un événement organisation externe
	 * @param options les options d'exécution de l'événement
	 * @return Un événement interne correspondant à l'événement passé en paramètre
	 * @throws EvenementOrganisationException En cas d'erreur dans la création de l'événements interne, null s'il n'y a pas lieu de créer un événement
	 */
	@Override
	public EvenementOrganisationInterne toInterne(EvenementOrganisation event, EvenementOrganisationOptions options) throws EvenementOrganisationException {
		final Organisation organisation = serviceOrganisationService.getOrganisationHistory(event.getNoOrganisation());
		// Sanity check. Pourquoi ici? Pour ne pas courir le risque d'ignorer des entreprise (passer à TRAITE) sur la foi d'information manquantes.
		sanityCheck(event, organisation);

		final String organisationDescription = serviceOrganisationService.createOrganisationDescription(organisation, event.getDateEvenement());
		Audit.info(event.getId(), String.format("Organisation trouvée: %s", organisationDescription));

		final String raisonSocialeCivile = organisation.getNom(event.getDateEvenement());
		final String noIdeCivil = organisation.getNumeroIDE(event.getDateEvenement());

		final List<EvenementOrganisationInterne> evenements = new ArrayList<>();

		Entreprise entreprise = context.getTiersDAO().getEntrepriseByNumeroOrganisation(organisation.getNumeroOrganisation());

		/* L'entreprise est retrouvé grâce au numéro cantonal. Pas de doute possible. */
		if (entreprise != null) {
			final String message = String.format("%s (%s%s) identifiée sur la base du numéro civil %s (numéro cantonal).",
			                                     entreprise.toString(),
			                                     raisonSocialeCivile,
			                                     noIdeCivil != null ? ", IDE: " + noIdeCivil : "",
			                                     organisation.getNumeroOrganisation());
			Audit.info(event.getId(), message);
			evenements.add(new MessageSuivi(event, organisation, entreprise, context, options, message));

		/* L'entreprise n'a pas été retrouvée par identifiant cantonal, on utilise le service d'identification pour tenter de la retrouver
		 si elle existe quand même sans avoir été rapprochée. */
		} else {
			final CriteresEntreprise criteres = new CriteresEntreprise();
			criteres.setRaisonSociale(raisonSocialeCivile);
			criteres.setIde(noIdeCivil);
			try {
				final List<Long> found = identCtbService.identifieEntreprise(criteres);

				// L'identificatione est un succès
				if (found.size() == 1) {
					entreprise = (Entreprise) tiersDAO.get(found.get(0));
					if (entreprise == null) {
						panicNotFoundAfterIdent(found.get(0), organisationDescription);
					}

					final String derniereRaisonSocialeFiscale = getDerniereRaisonSocialeFiscale(entreprise);

					final String message = String.format("%s%s identifiée sur la base de ses attributs civils [%s].",
					                                     entreprise.toString(),
					                                     StringUtils.isNotBlank(derniereRaisonSocialeFiscale) ? " (" + derniereRaisonSocialeFiscale + ")" : "",
					                                     attributsCivilsAffichage(raisonSocialeCivile, noIdeCivil));
					Audit.info(event.getId(), message);
					evenements.add(new MessageSuivi(event, organisation, entreprise, context, options, message));

					// L'identificatione est un échec: selon toute vraisemblance, on connait le tiers mais on n'arrive pas à l'identifier avec certitude.
				} else if (found.size() > 1) {
					final String listeTrouves = StringsUtils.appendsWithDelimiter(", ", found);
					String message = String.format("Plusieurs entreprises ont été trouvées (numéros %s) pour les attributs civils [%s]. Arrêt du traitement.",
					                               listeTrouves,
					                               attributsCivilsAffichage(raisonSocialeCivile, noIdeCivil));
					Audit.info(event.getId(), message);
					return new TraitementManuel(event, organisation, null, context, options, message);
				}
			}
			// L'identificatione est un échec: selon toute vraisemblance, on connait le tiers mais il y a beaucoup trop de résultat.
			catch (TooManyIdentificationPossibilitiesException e) {
				String message = String.format("L'identification de l'organisation a renvoyé un trop grand nombre de résultats pour les attributs civils [%s]! Arrêt du traitement.",
				                               attributsCivilsAffichage(raisonSocialeCivile, noIdeCivil));
				Audit.info(event.getId(), message);
				return new TraitementManuel(event, organisation, null, context, options, message);
			}
			// L'identification n'a rien retourné. Cela veut dire qu'on ne connait pas déjà le tiers. On rapporte simplement cet état de fait.
			if (entreprise == null) {
				final String message = String.format("Aucune entreprise identifiée pour le numéro civil %s ou les attributs civils [%s].",
				                                     organisation.getNumeroOrganisation(),
				                                     attributsCivilsAffichage(raisonSocialeCivile, noIdeCivil));
				Audit.info(event.getId(), message);
				evenements.add(new MessageSuivi(event, organisation, null, context, options, message));
			}
		}

		/* Essayer chaque stratégie. Chacune est responsable de détecter l'événement dans les données. */
		final List<EvenementOrganisationInterne> resultatEvaluationStrategies = new ArrayList<>();
		for (EvenementOrganisationTranslationStrategy strategy : strategies) {
			EvenementOrganisationInterne e = strategy.matchAndCreate(event, organisation, entreprise, context, options);
			if (e != null) {
				resultatEvaluationStrategies.add(e);
			}
		}

		/* Pas de véritable traitement à exécuter. Indexation seulement. Le status sera TRAITE. */
		if (resultatEvaluationStrategies.size() == 0) {
			LOGGER.info("Pas de changement ni d'événement fiscal. L'entité sera simplement réindexée (si connue).");
			evenements.add(new IndexationPure(event, organisation, entreprise, context, options));

		/* Il y a des traitements à exécuter. Indexation obligatoire pour toute entité connue d'Unireg. Le status sera laissé inchangé. */
		} else {
			evenements.addAll(resultatEvaluationStrategies);
			LOGGER.info("L'entité sera (re)indexée.");
			evenements.add(new Indexation(event, organisation, entreprise, context, options));
		}
		return new EvenementOrganisationInterneComposite(event, organisation, evenements.get(0).getEntreprise(), context, options, evenements);
	}

	private String attributsCivilsAffichage(@NotNull String raisonSociale, @Nullable String noIde) {
		return String.format("%s%s", raisonSociale, noIde != null ? ", IDE: " + noIde : "");
	}
	private void panicNotFoundAfterIdent(Long found, String organisationDescription) throws EvenementOrganisationException {
		throw new EvenementOrganisationException(String.format("L'identifiant de tiers %s retourné par le service d'identification ne correspond à aucun tiers! Organisation recherchée: %s",
		                                                       found, organisationDescription));
	}

	// Selon SIFISC-16998 : mise en erreur des annonces sans données obligatoires
	private void sanityCheck(EvenementOrganisation event, Organisation organisation) throws EvenementOrganisationException {
		final RegDate dateEvenement = event.getDateEvenement();

		List<DateRanged<String>> noms = organisation.getNom();
		if (noms.size() > 0) {
			if (dateEvenement.isBefore(noms.get(0).getDateDebut())) {
				throw new EvenementOrganisationException(
						String.format("Erreur fatale: la date de l'événement %s (%s) est antérieure à la date de création (%s) de l'organisation telle que rapportée par RCEnt. No civil: %s, nom: %s.",
						              event.getId(), dateEvenement, noms.get(0).getDateDebut(), organisation.getNumeroOrganisation(), noms.get(0).getPayload()));
			}
			StringBuilder champs = new StringBuilder();
			FormeLegale formeLegale = organisation.getFormeLegale(dateEvenement);
			if (formeLegale == null) {
				champs.append("[legalForm] ");
			}
			for (SiteOrganisation site : organisation.getDonneesSites()) {
				String nom = site.getNom(dateEvenement); // Le nom (obligatoire de par le xsd) nous permet de déduire si le site est existant pour la date données.
				if (nom != null && site.getDomicile(dateEvenement) == null) {
					champs.append(String.format("[Etablissement %s: seat] ", site.getNumeroSite()));
				}
			}
			if (champs.length() > 0) {
				throw new EvenementOrganisationException(String.format("Donnée RCEnt invalide, champ(s) nécessaires manquant(s): %s.", champs));
			}
		} else {
			throw new EvenementOrganisationException(
					String.format("Donnée RCEnt invalide: Champ obligatoire 'nom' pas trouvé pour l'organisation no civil: %s",
					              organisation.getNumeroOrganisation()));
		}
	}

	@Nullable
	private static String getDerniereRaisonSocialeFiscale(Entreprise e) {
		final List<RaisonSocialeFiscaleEntreprise> toutes = e.getRaisonsSocialesNonAnnuleesTriees();
		if (toutes.isEmpty()) {
			return null;
		}
		return CollectionsUtils.getLastElement(toutes).getRaisonSociale();
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setServiceOrganisationService(ServiceOrganisationService serviceOrganisationService) {
		this.serviceOrganisationService = serviceOrganisationService;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setServiceInfrastructureService(ServiceInfrastructureService serviceInfrastructureService) {
		this.serviceInfrastructureService = serviceInfrastructureService;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setTiersDAO(TiersDAO tiersDAO) {
		this.tiersDAO = tiersDAO;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setDataEventService(DataEventService dataEventService) {
		this.dataEventService = dataEventService;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setMetierServicePM(MetierServicePM metierServicePM) {
		this.metierServicePM = metierServicePM;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setAdresseService(AdresseService adresseService) {
		this.adresseService = adresseService;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setIndexer(GlobalTiersIndexer indexer) {
		this.indexer = indexer;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setIdentCtbService(IdentificationContribuableService identCtbService) {
		this.identCtbService = identCtbService;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setEvenementFiscalService(EvenementFiscalService evenementFiscalService) {
		this.evenementFiscalService = evenementFiscalService;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setAssujettissementService(AssujettissementService assujettissementService) {
		this.assujettissementService = assujettissementService;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setParametreAppService(ParametreAppService parametreAppService) {
		this.parametreAppService = parametreAppService;
	}
}
