package ch.vd.unireg.evenement.organisation.engine.translator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.NonUniqueResultException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.StringsUtils;
import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.audit.Audit;
import ch.vd.unireg.common.CollectionsUtils;
import ch.vd.unireg.common.FormatNumeroHelper;
import ch.vd.unireg.common.StringRenderer;
import ch.vd.unireg.data.DataEventService;
import ch.vd.unireg.evenement.fiscal.EvenementFiscalService;
import ch.vd.unireg.evenement.ide.ReferenceAnnonceIDE;
import ch.vd.unireg.evenement.identification.contribuable.CriteresEntreprise;
import ch.vd.unireg.evenement.organisation.EvenementOrganisation;
import ch.vd.unireg.evenement.organisation.EvenementOrganisationCappingLevelProvider;
import ch.vd.unireg.evenement.organisation.EvenementOrganisationContext;
import ch.vd.unireg.evenement.organisation.EvenementOrganisationException;
import ch.vd.unireg.evenement.organisation.EvenementOrganisationOptions;
import ch.vd.unireg.evenement.organisation.EvenementOrganisationService;
import ch.vd.unireg.evenement.organisation.interne.CappingAVerifier;
import ch.vd.unireg.evenement.organisation.interne.CappingEnErreur;
import ch.vd.unireg.evenement.organisation.interne.EvenementOrganisationInterne;
import ch.vd.unireg.evenement.organisation.interne.EvenementOrganisationInterneComposite;
import ch.vd.unireg.evenement.organisation.interne.Indexation;
import ch.vd.unireg.evenement.organisation.interne.IndexationPure;
import ch.vd.unireg.evenement.organisation.interne.MessageSuiviPreExecution;
import ch.vd.unireg.evenement.organisation.interne.MessageWarningPreExectution;
import ch.vd.unireg.evenement.organisation.interne.TraitementManuel;
import ch.vd.unireg.evenement.organisation.interne.ValideurDebutDeTraitement;
import ch.vd.unireg.evenement.organisation.interne.adresse.AdresseStrategy;
import ch.vd.unireg.evenement.organisation.interne.creation.CreateOrganisationStrategy;
import ch.vd.unireg.evenement.organisation.interne.decisionaci.DecisionAciStrategy;
import ch.vd.unireg.evenement.organisation.interne.demenagement.DemenagementSiegeStrategy;
import ch.vd.unireg.evenement.organisation.interne.donneeinvalide.FormeJuridiqueInvalideStrategy;
import ch.vd.unireg.evenement.organisation.interne.donneeinvalide.FormeJuridiqueManquanteStrategy;
import ch.vd.unireg.evenement.organisation.interne.doublon.DoublonEntrepriseRemplacanteStrategy;
import ch.vd.unireg.evenement.organisation.interne.doublon.DoublonEntrepriseRemplaceeParStrategy;
import ch.vd.unireg.evenement.organisation.interne.doublon.DoublonEtablissementStrategy;
import ch.vd.unireg.evenement.organisation.interne.etablissement.EtablissementsSecondairesStrategy;
import ch.vd.unireg.evenement.organisation.interne.formejuridique.ChangementFormeJuridiqueStrategy;
import ch.vd.unireg.evenement.organisation.interne.information.FailliteConcordatStrategy;
import ch.vd.unireg.evenement.organisation.interne.information.ModificationButsStrategy;
import ch.vd.unireg.evenement.organisation.interne.information.ModificationCapitalStrategy;
import ch.vd.unireg.evenement.organisation.interne.information.ModificationStatutsStrategy;
import ch.vd.unireg.evenement.organisation.interne.inscription.InscriptionStrategy;
import ch.vd.unireg.evenement.organisation.interne.radiation.RadiationStrategy;
import ch.vd.unireg.evenement.organisation.interne.raisonsociale.RaisonSocialeStrategy;
import ch.vd.unireg.evenement.organisation.interne.reinscription.ReinscriptionStrategy;
import ch.vd.unireg.evenement.organisation.interne.retour.annonce.RetourAnnonceIDE;
import ch.vd.unireg.evenement.organisation.interne.transformation.DissolutionStrategy;
import ch.vd.unireg.evenement.organisation.interne.transformation.FusionScissionStrategy;
import ch.vd.unireg.evenement.organisation.interne.transformation.LiquidationStrategy;
import ch.vd.unireg.identification.contribuable.IdentificationContribuableService;
import ch.vd.unireg.identification.contribuable.TooManyIdentificationPossibilitiesException;
import ch.vd.unireg.indexer.tiers.GlobalTiersIndexer;
import ch.vd.unireg.interfaces.organisation.data.AnnonceIDEEnvoyee;
import ch.vd.unireg.interfaces.organisation.data.DateRanged;
import ch.vd.unireg.interfaces.organisation.data.Domicile;
import ch.vd.unireg.interfaces.organisation.data.FormeLegale;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.unireg.interfaces.organisation.data.ServiceOrganisationEvent;
import ch.vd.unireg.interfaces.organisation.data.SiteOrganisation;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;
import ch.vd.unireg.interfaces.service.ServiceOrganisationService;
import ch.vd.unireg.metier.MetierServiceException;
import ch.vd.unireg.metier.MetierServicePM;
import ch.vd.unireg.metier.RattachementOrganisationResult;
import ch.vd.unireg.metier.assujettissement.AssujettissementService;
import ch.vd.unireg.parametrage.ParametreAppService;
import ch.vd.unireg.regimefiscal.RegimeFiscalService;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.Etablissement;
import ch.vd.unireg.tiers.FormeJuridiqueFiscaleEntreprise;
import ch.vd.unireg.tiers.OrganisationNotFoundException;
import ch.vd.unireg.tiers.PlusieursEntreprisesAvecMemeNumeroOrganisationException;
import ch.vd.unireg.tiers.RaisonSocialeFiscaleEntreprise;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.tiers.TiersDAO;
import ch.vd.unireg.tiers.TiersService;
import ch.vd.unireg.tiers.rattrapage.appariement.AppariementService;
import ch.vd.unireg.type.TypeAutoriteFiscale;
import ch.vd.unireg.type.TypeEvenementOrganisation;

/**
 * Convertisseur d'événements reçus de RCEnt en événements organisation internes
 */
public class EvenementOrganisationTranslatorImpl implements EvenementOrganisationTranslator, InitializingBean {

	private static final Logger LOGGER = LoggerFactory.getLogger(EvenementOrganisationTranslatorImpl.class);

	private static final StringRenderer<Tiers> TIERS_NO_RENDERER = tiers -> String.format("n°%s", FormatNumeroHelper.numeroCTBToDisplay(tiers.getNumero()));

	private static final StringRenderer<String> NO_IDE_RENDERER = FormatNumeroHelper::formatNumIDE;

	private static final StringRenderer<SiteOrganisation> SITE_RENDERER = site -> String.format("n°%d", site.getNumeroSite());

	private ServiceOrganisationService serviceOrganisationService;
	private ServiceInfrastructureService serviceInfrastructureService;
	private RegimeFiscalService regimeFiscalService;
	private TiersDAO tiersDAO;
	private DataEventService dataEventService;
	private TiersService tiersService;
	private MetierServicePM metierServicePM;
	private AdresseService adresseService;
	private GlobalTiersIndexer indexer;
	private IdentificationContribuableService identCtbService;
	private EvenementFiscalService evenementFiscalService;
	private AssujettissementService assujettissementService;
	private EvenementOrganisationService evenementOrganisationService;
	private AppariementService appariementService;
	private ParametreAppService parametreAppService;
	private boolean useOrganisationsOfNotice;
	private EvenementOrganisationCappingLevelProvider cappingLevelProvider;

	/*
	 * Non injecté mais créé ci-dessous dans afterPropertiesSet()
	 */
	private EvenementOrganisationContext context;
	private List<EvenementOrganisationTranslationStrategy> strategies;

	public EvenementOrganisationTranslatorImpl(boolean useOrganisationsOfNotice) {
		this.useOrganisationsOfNotice = useOrganisationsOfNotice;
	}

	public EvenementOrganisationTranslatorImpl() {
		this.useOrganisationsOfNotice = false;
	}

	private final EvenementOrganisationOptions options = new EvenementOrganisationOptions();

	@Override
	public void afterPropertiesSet() throws Exception {
		context = new EvenementOrganisationContext(serviceOrganisationService, evenementOrganisationService, serviceInfrastructureService, regimeFiscalService, dataEventService, tiersService, indexer, metierServicePM, tiersDAO, adresseService,
		                                           evenementFiscalService, assujettissementService, appariementService, parametreAppService);

		// Construction des stratégies
		strategies = new ArrayList<>();

		/*
			L'ordre des stratégies est important.
		 */
		strategies.add(new FormeJuridiqueManquanteStrategy(context, options));
		strategies.add(new FormeJuridiqueInvalideStrategy(context, options));
		strategies.add(new DecisionAciStrategy(context, options));
		strategies.add(new CreateOrganisationStrategy(context, options));
		strategies.add(new EtablissementsSecondairesStrategy(context, options));
		strategies.add(new RaisonSocialeStrategy(context, options));
		strategies.add(new InscriptionStrategy(context, options));
		strategies.add(new ReinscriptionStrategy(context, options));
		strategies.add(new ChangementFormeJuridiqueStrategy(context, options));
		strategies.add(new FailliteConcordatStrategy(context, options));
		strategies.add(new ModificationCapitalStrategy(context, options));
		strategies.add(new ModificationButsStrategy(context, options));
		strategies.add(new ModificationStatutsStrategy(context, options));
		strategies.add(new DoublonEntrepriseRemplacanteStrategy(context, options));
		strategies.add(new DoublonEntrepriseRemplaceeParStrategy(context, options));
		strategies.add(new DoublonEtablissementStrategy(context, options));
		strategies.add(new DemenagementSiegeStrategy(context, options));
		strategies.add(new AdresseStrategy(context, options));
		strategies.add(new RadiationStrategy(context, options));
		strategies.add(new DissolutionStrategy(context, options));
		strategies.add(new FusionScissionStrategy(context, options));
		strategies.add(new LiquidationStrategy(context, options));
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
	 * <p>
	 *     Si plusieurs tiers détiennent le même numéro cantonal d'entreprise, le traitement est mis en erreur.
	 * </p>
	 * @param event   un événement organisation externe
	 * @return Un événement interne correspondant à l'événement passé en paramètre
	 * @throws EvenementOrganisationException En cas d'erreur dans la création de l'événements interne, null s'il n'y a pas lieu de créer un événement
	 */
	@Override
	public EvenementOrganisationInterne toInterne(EvenementOrganisation event) throws EvenementOrganisationException {
		final Organisation organisation;
		if (useOrganisationsOfNotice) {
			final ServiceOrganisationEvent serviceOrganisationEvent = serviceOrganisationService.getOrganisationEvent(event.getNoEvenement()).get(event.getNoOrganisation());
			if (serviceOrganisationEvent == null) {
				throw new EvenementOrganisationException(
						String.format("Fatal: l'événement %d ne contient pas de données pour l'organisation %d!", event.getNoEvenement(), event.getNoOrganisation())
				);
			}
			organisation = serviceOrganisationEvent.getPseudoHistory();
		} else {
			LOGGER.warn("Utilisation du service RCEnt WS Organisation à la place du WS OrganisationsOfNotice! Traitements à double possibles.");
			organisation = serviceOrganisationService.getOrganisationHistory(event.getNoOrganisation());
			if (organisation == null) {
				throw new OrganisationNotFoundException(event.getNoOrganisation());
			}
		}

		// Sanity check. Pourquoi ici? Pour ne pas courir le risque d'ignorer des entreprises (passer à TRAITE) sur la foi d'information manquantes.
		sanityCheck(event, organisation);

		final String organisationDescription = serviceOrganisationService.createOrganisationDescription(organisation, event.getDateEvenement());
		Audit.info(event.getNoEvenement(), String.format("Organisation trouvée: %s", organisationDescription));

		final String raisonSocialeCivile = organisation.getNom(event.getDateEvenement());
		final String noIdeCivil = organisation.getNumeroIDE(event.getDateEvenement());

		Entreprise entreprise;
		final List<EvenementOrganisationInterne> evenements = new ArrayList<>();

		try {
			entreprise = context.getTiersDAO().getEntrepriseByNumeroOrganisation(organisation.getNumeroOrganisation());
		}
		catch (PlusieursEntreprisesAvecMemeNumeroOrganisationException e) {
			return new TraitementManuel(event, organisation, null, context, options, e.getMessage());
		}

		// Flag d'activation de l'application des stratégies de découverte de mutations.
		boolean evaluateStrategies = true;

		/* Cas spécial: on reçoit le retour d'une annonce à l'IDE. Il faut rechercher l'entreprise et apparier si c'est une création. */
		final ReferenceAnnonceIDE referenceAnnonceIDE = event.getReferenceAnnonceIDE();
		if (referenceAnnonceIDE != null) {
			final Long noAnnonceIDE = referenceAnnonceIDE.getId();
			final Etablissement etablissement = referenceAnnonceIDE.getEtablissement();

			/* On utilise la date de l'événement comme référence, mais quid si l'association entreprise <-> etablissement avait changé entretemps? */
			entreprise = tiersService.getEntreprise(etablissement, event.getDateEvenement());
			if (entreprise == null) {
				throw new EvenementOrganisationException(
						String.format(
								"Fatal: Impossible de traiter l'événement de retour d'annonce à l'IDE n°%d: impossible de retrouver l'entreprise pour l'annonce à l'IDE %d (établissement n°%s)! " +
										"Le rapport entre tiers a peut être changé depuis?",
								event.getNoEvenement(), noAnnonceIDE, FormatNumeroHelper.numeroCTBToDisplay(etablissement.getNumeroEtablissement())
						)
				);
			}

			final AnnonceIDEEnvoyee annonceIDE = serviceOrganisationService.getAnnonceIDE(noAnnonceIDE);
			if (annonceIDE == null) {
				throw new EvenementOrganisationException(
						String.format(
								"Fatal: Impossible de traiter l'événement de retour d'annonce à l'IDE n°%d: impossible de retrouver l'annonce à l'IDE %d à l'origine de l'événement! " +
										"Le rapport entre tiers a peut être changé depuis?",
								event.getNoEvenement(), noAnnonceIDE
						)
				);
			}
			try {
				final EvenementOrganisation evenementOrganisation = evenementOrganisationService.getEvenementForNoAnnonceIDE(noAnnonceIDE);
				if (event.getId() != evenementOrganisation.getId()) {
					final String message =
							String.format("Un événement RCEnt est déjà associé à l'annonce à l'IDE n°%d. Le présent événement n°%d ne peut lui aussi " +
									              "provenir de cette annonce à l'IDE. C'est un bug du registre civil. Traitement manuel.",
							              event.getNoEvenement(), noAnnonceIDE
							);
					Audit.error(event.getNoEvenement(), message);
					throw new EvenementOrganisationException(message);
				}
			}
			catch (NonUniqueResultException e) {
				final String message =
						String.format("Un ou plusieurs précédant événements RCEnt sont déjà associés à l'annonce à l'IDE n°%d. Le présent événement n°%d ne peut lui aussi " +
								              "provenir de cette annonce à l'IDE. C'est un bug du registre civil. Traitement manuel.",
						              event.getNoEvenement(), noAnnonceIDE
						);
				Audit.error(event.getNoEvenement(), message);
				throw new EvenementOrganisationException(message);
			}

			evenements.add(new RetourAnnonceIDE(event, organisation, entreprise, context, options, annonceIDE));
			// Pas question de traiter normallement, on connait déjà les changements.
			evaluateStrategies = false;
		}
		/* L'entreprise est retrouvé grâce au numéro cantonal. Pas de doute possible. */
		else if (entreprise != null) {
			final String message = String.format("Entreprise n°%s (%s%s) identifiée sur la base du numéro civil %d (numéro cantonal).",
			                                     FormatNumeroHelper.numeroCTBToDisplay(entreprise.getNumero()),
			                                     raisonSocialeCivile,
			                                     noIdeCivil != null ? ", IDE: " + NO_IDE_RENDERER.toString(noIdeCivil) : StringUtils.EMPTY,
			                                     organisation.getNumeroOrganisation());
			Audit.info(event.getNoEvenement(), message);
			evenements.add(new MessageSuiviPreExecution(event, organisation, entreprise, context, options, message));

		}
		/* L'entreprise n'a pas été retrouvée par identifiant cantonal, on utilise le service d'identification pour tenter de la retrouver
		 si elle existe quand même sans avoir été rapprochée. */
		else {
			Tiers tiers = null;

			final CriteresEntreprise criteres = new CriteresEntreprise();
			criteres.setRaisonSociale(raisonSocialeCivile);
			criteres.setIde(noIdeCivil);
			try {
				final List<Long> found = identCtbService.identifieEntreprise(criteres);

				// L'identificatione est un succès
				if (found.size() == 1) {
					tiers = tiersDAO.get(found.get(0));
					if (tiers == null) {
						panicNotFoundAfterIdent(found.get(0), organisationDescription);
					}
				}
				// L'identificatione est un échec: selon toute vraisemblance, on connait le tiers mais on n'arrive pas à l'identifier avec certitude.
				else if (found.size() > 1) {
					Collections.sort(found);
					final String listeTrouves = StringsUtils.appendsWithDelimiter(", ", found);
					String message = String.format("Plusieurs entreprises ont été trouvées (numéros %s) pour les attributs civils [%s]. Arrêt du traitement.",
					                               listeTrouves,
					                               attributsCivilsAffichage(raisonSocialeCivile, noIdeCivil));
					Audit.info(event.getNoEvenement(), message);
					return new TraitementManuel(event, organisation, null, context, options, message);
				}
			}
			// L'identificatione est un échec: selon toute vraisemblance, on connait le tiers mais il y a beaucoup trop de résultat.
			catch (TooManyIdentificationPossibilitiesException e) {
				String message = String.format("L'identification de l'organisation a renvoyé un trop grand nombre de résultats pour les attributs civils [%s]! Arrêt du traitement.",
				                               attributsCivilsAffichage(raisonSocialeCivile, noIdeCivil));
				Audit.info(event.getNoEvenement(), message);
				return new TraitementManuel(event, organisation, null, context, options, message);
			}
			// L'identification a retourné un tiers. Reste à savoir si c'est un candidat acceptable.
			if (tiers != null) {
				if (tiers instanceof Entreprise) {
					entreprise = (Entreprise) tiers;
					final String raisonSociale = determineRaisonSocialeFiscaleEntreprise(event, entreprise);
					final String message;
					// Cas nominal, l'entreprise n'est pas déjà appariée.
					if (entreprise.getNumeroEntreprise() == null) {
						message = String.format("Entreprise n°%s (%s) identifiée sur la base de ses attributs civils [%s].",
						                        FormatNumeroHelper.numeroCTBToDisplay(entreprise.getNumero()),
						                        raisonSociale,
						                        attributsCivilsAffichage(raisonSocialeCivile, noIdeCivil));
					}
					// Le tiers entreprise trouvé est déjà apparié à une autre organisation. Causé soit par un doublon dans RCEnt, soit par une grande similitude des attributs civils.
					else {
						final Organisation organisationDejaAppariee = serviceOrganisationService.getOrganisationHistory(entreprise.getNumeroEntreprise());
						message = String.format("Entreprise n°%s (%s) identifiée sur la base de ses attributs civils [%s], mais déjà rattachée à l'organisation n°%s (%s). Potentiel doublon au civil. Traitement manuel.",
						                        FormatNumeroHelper.numeroCTBToDisplay(entreprise.getNumero()),
						                        raisonSociale,
						                        attributsCivilsAffichage(raisonSocialeCivile, noIdeCivil),
						                        entreprise.getNumeroEntreprise(),
						                        organisationDejaAppariee == null ? "<introuvable au civil>" : attributsCivilsAffichage(organisationDejaAppariee.getNom(event.getDateEvenement()), organisationDejaAppariee.getNumeroIDE(event.getDateEvenement())));
						return new TraitementManuel(event, organisation, null, context, options, message);
					}
					Audit.info(event.getNoEvenement(), message);
					evenements.add(new MessageSuiviPreExecution(event, organisation, entreprise, context, options, message));

					if (checkFormesJuridiquesCompatibles(event, organisation, entreprise)) {
						evenements.add(rattacheOrganisation(event, organisation, entreprise, context, options));
					} else {
						final List<FormeJuridiqueFiscaleEntreprise> formesJuridiquesNonAnnuleesTriees = entreprise.getFormesJuridiquesNonAnnuleesTriees();
						FormeJuridiqueFiscaleEntreprise formeJuridiqueFiscaleEntreprise = null;
						if (!formesJuridiquesNonAnnuleesTriees.isEmpty()) {
							formeJuridiqueFiscaleEntreprise = DateRangeHelper.rangeAt(entreprise.getFormesJuridiquesNonAnnuleesTriees(), event.getDateEvenement());
						}

						final FormeLegale formeLegale = organisation.getFormeLegale(event.getDateEvenement());
						return new TraitementManuel(event, organisation, entreprise, context, options,
						                            String.format("Impossible de rattacher l'organisation n°%d%s à l'entreprise n°%s (%s)%s " +
								                                          "identifiée sur la base de ses attributs civils [%s]: les formes juridiques ne correspondent pas. Arrêt du traitement.",
						                                          organisation.getNumeroOrganisation(),
						                                          formeLegale != null ? " (" + formeLegale + ")" : "",
						                                          FormatNumeroHelper.numeroCTBToDisplay(entreprise.getNumero()),
						                                          raisonSociale,
						                                          formeJuridiqueFiscaleEntreprise != null ? " (" + formeJuridiqueFiscaleEntreprise.getFormeJuridique().getLibelle() + ")" : "",
						                                          attributsCivilsAffichage(raisonSocialeCivile, noIdeCivil)
						                            )

						);
					}
				} else {
					final String message = String.format("Attention: le tiers n°%s identifié grâce aux attributs civils [%s] n'est pas une entreprise (%s) " +
							                                     "et sera ignoré. Si nécessaire, un tiers Entreprise sera créé pour l'organisation civile n°%d, en doublon du " +
							                                     "tiers n°%s (%s).\n",
					                                     FormatNumeroHelper.numeroCTBToDisplay(tiers.getNumero()),
					                                     attributsCivilsAffichage(raisonSocialeCivile, noIdeCivil),
					                                     tiers.getType().getDescription(),
					                                     organisation.getNumeroOrganisation(),
					                                     FormatNumeroHelper.numeroCTBToDisplay(tiers.getNumero()),
					                                     tiers.getType().getDescription());
							Audit.info(event.getNoEvenement(), message);
					evenements.add(new MessageWarningPreExectution(event, organisation, null, context, options, message));
				}
			}
			// L'identification n'a rien retourné. Cela veut dire qu'on ne connait pas déjà le tiers. On rapporte simplement cet état de fait.
			else {
				final String message = String.format("Aucune entreprise identifiée pour le numéro civil %d ou les attributs civils [%s].",
				                                     organisation.getNumeroOrganisation(),
				                                     attributsCivilsAffichage(raisonSocialeCivile, noIdeCivil));
				Audit.info(event.getNoEvenement(), message);
				evenements.add(new MessageSuiviPreExecution(event, organisation, null, context, options, message));
			}
		}

		/*
		    SIFISC-21128 - Sauter les annonces IDE liées à une nouvelle inscription sur VD lorsque c'est possible, afin de pouvoir traiter automatiquement
		    l'événement FOSC qui suit derrière.
		  */
		if (entreprise == null && evenementAIgnorer(event)) {
			evenements.add(
					new MessageSuiviPreExecution(event, organisation, null, context, options,
					                             String.format("L'événement pour l'organisation n°%d précède un événement FOSC d'inscription RC VD sans historique avant ce jour au civil. Ignoré.",
					                                           organisation.getNumeroOrganisation())
					)
			);
			evaluateStrategies = false;
		}


		/* Essayer chaque stratégie. Chacune est responsable de détecter l'événement dans les données. */
		final List<EvenementOrganisationInterne> resultatEvaluationStrategies = new ArrayList<>();
		if (evaluateStrategies) {
			for (EvenementOrganisationTranslationStrategy strategy : strategies) {
				final EvenementOrganisationInterne e = strategy.matchAndCreate(event, organisation, entreprise);
				if (e != null) {
					if (e instanceof MessageSuiviPreExecution || e instanceof MessageWarningPreExectution) {
						evenements.add(e);
					}
					else {
						resultatEvaluationStrategies.add(e);
					}
				}
			}
		}

		// SIFISC-19332 - SIFISC-19471 - Ne vérifier la présence antérieur d'une entreprise que si on a quelque chose à faire avec l'entité.
		/*
			Validation au début du traitement effectif.
		 */
		evenements.add(new ValideurDebutDeTraitement(event, organisation, entreprise, context, options));

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

		// [SIFISC-19214] blindage "capping" de l'état final de l'événement organisation
		final NiveauCappingEtat etatCapping = cappingLevelProvider != null ? cappingLevelProvider.getNiveauCapping() : null;
		if (etatCapping != null) {
			evenements.add(buildEvenementInterneCapping(event, organisation, entreprise, context, options, etatCapping));
		}

		return new EvenementOrganisationInterneComposite(event, organisation, evenements.get(0).getEntreprise(), context, options, evenements);
	}

	private String determineRaisonSocialeFiscaleEntreprise(EvenementOrganisation event, Entreprise entreprise) {
		String raisonSociale = "<raison sociale introuvable>";
		final List<RaisonSocialeFiscaleEntreprise> raisonsSocialesNonAnnuleesTriees = entreprise.getRaisonsSocialesNonAnnuleesTriees();
		if (!raisonsSocialesNonAnnuleesTriees.isEmpty()) {
			final RaisonSocialeFiscaleEntreprise raisonSocialeFiscaleEntreprise = DateRangeHelper.rangeAt(raisonsSocialesNonAnnuleesTriees, event.getDateEvenement());
			if (raisonSocialeFiscaleEntreprise != null) {
				raisonSociale = raisonSocialeFiscaleEntreprise.getRaisonSociale();
			}
			else {
				raisonSociale = CollectionsUtils.getLastElement(raisonsSocialesNonAnnuleesTriees).getRaisonSociale();
			}
		}
		return raisonSociale;
	}

	/**
	 * Déterminer si l'événement doit être ignoré en vertu des critères énoncés au SIFISC-21128: Sauter les annonces IDE liées à une nouvelle inscription sur VD
	 * lorsque c'est possible.
	 *
	 * NOTE: Il est requis de contrôler qu'on n'a pas d'entreprise en base associée à ce numéro d'organisation avant d'appeler cette méthode.
	 *
	 * @param event l'événement
	 */
	protected boolean evenementAIgnorer(EvenementOrganisation event) {

		final RegDate dateEvenement = event.getDateEvenement();

		final Organisation organisationHistory = serviceOrganisationService.getOrganisationHistory(event.getNoOrganisation());
		final SiteOrganisation sitePrincipal = organisationHistory.getSitePrincipal(dateEvenement).getPayload();
		final Domicile siegePrincipalAvant = organisationHistory.getSiegePrincipal(dateEvenement.getOneDayBefore());
		final Domicile siegePrincipalApres = organisationHistory.getSiegePrincipal(dateEvenement);

		/*
		    Les 5 conditions pour estimer sans risque d'ignorer un événement IDE au profit de l'événement FOSC à venir.
		    FIXME: On ne supporte pas correctement les événements IDE reçus après la FOSC pour un même jour. Perte potentielle de changement d'adresse.
		  */

		// Etre un événement IDE d'inscription ou mutation
		final boolean evtIDEIncriptionMutation = event.getType() == TypeEvenementOrganisation.IDE_NOUVELLE_INSCRIPTION || event.getType() == TypeEvenementOrganisation.IDE_MUTATION;

		// On s'attend à une nouvelle inscription au RC (Vaudois, cf. condition de lieu ci-dessous), donc il faut être inscrit et non radié.
		final boolean actifAuRC = sitePrincipal.isConnuInscritAuRC(dateEvenement) && !sitePrincipal.isRadieDuRC(dateEvenement);

		// Ne pas avoir d'historique avant aujourd'hui au civil. C'est-à-dire être une nouvelle inscription RC VD, soit par fondation, soit par arrivée. On contrôle aussi l'absence d'événements.
		final boolean estNouveauCivil = organisationHistory.getNom(dateEvenement.getOneDayBefore()) == null &&
				evenementOrganisationService.evenementsPourDateValeurEtOrganisation(dateEvenement.getOneDayBefore(), event.getNoOrganisation()).isEmpty();

		// On doit être vaudois. On ne peut s'attendre à un événement FOSC pour les cas hors canton, et on veut exclure tout risque de traiter un événement FOSC pour un établissement secondaire.
		final boolean estVaudoise = siegePrincipalApres.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD;

		// S'assurer que l'événement est vide de tout changement de siège, car dès lors qu'il est présent sur un événement IDE, il serait absent de l'événement FOSC qui suivrait.
		final boolean sansChangementDeSiege = siegePrincipalAvant == null || (siegePrincipalAvant.getTypeAutoriteFiscale() == siegePrincipalApres.getTypeAutoriteFiscale() && siegePrincipalAvant.getNumeroOfsAutoriteFiscale().equals(siegePrincipalApres.getNumeroOfsAutoriteFiscale()));

		// S'assurer qu'il y a aucun d'événement FOSC avant et au moins un FOSC_NOUVELLE_ENTREPRISE après pour le même jour (SIFISC-23174: ne pas être trop laxiste et insister pour un événement de création)
		final List<EvenementOrganisation> evenementsDuJour = evenementOrganisationService.evenementsPourDateValeurEtOrganisation(dateEvenement, event.getNoOrganisation());
		final List<EvenementOrganisation> evenementsFOSCRecusAvant = new ArrayList<>();
		final List<EvenementOrganisation> evenementsFOSCNouvelleInscriptionRecusApres = new ArrayList<>();
		for (EvenementOrganisation evt : evenementsDuJour) {
			if (evt.getType().getSource() == TypeEvenementOrganisation.Source.FOSC) {
				if (evt.getId() < event.getId()) {
					evenementsFOSCRecusAvant.add(evt);
				}
				else if (evt.getId() > event.getId() && evt.getType() == TypeEvenementOrganisation.FOSC_NOUVELLE_ENTREPRISE) {
					evenementsFOSCNouvelleInscriptionRecusApres.add(evt);
				}
			}
		}
		final boolean conditionEvtFOSC = evenementsFOSCRecusAvant.isEmpty() && evenementsFOSCNouvelleInscriptionRecusApres.size() > 0;

		return evtIDEIncriptionMutation && actifAuRC && estNouveauCivil && estVaudoise && sansChangementDeSiege && conditionEvtFOSC;
	}

	private static EvenementOrganisationInterne buildEvenementInterneCapping(EvenementOrganisation event, Organisation organisation, Entreprise entreprise,
	                                                                         EvenementOrganisationContext context, EvenementOrganisationOptions options,
	                                                                         @NotNull NiveauCappingEtat etatCapping) {
		switch (etatCapping) {
		case A_VERIFIER:
			return new CappingAVerifier(event, organisation, entreprise, context, options);
		case EN_ERREUR:
			return new CappingEnErreur(event, organisation, entreprise, context, options);
		default:
			throw new IllegalArgumentException("Valeur du niveau de capping non-supportée : " + etatCapping);
		}
	}

	private boolean checkFormesJuridiquesCompatibles(EvenementOrganisation event, Organisation organisation, Entreprise entreprise) {
		final List<FormeJuridiqueFiscaleEntreprise> formesJuridiquesNonAnnuleesTriees = entreprise.getFormesJuridiquesNonAnnuleesTriees();
		if (!formesJuridiquesNonAnnuleesTriees.isEmpty()) {
			final FormeJuridiqueFiscaleEntreprise formeJuridiqueFiscaleEntreprise = DateRangeHelper.rangeAt(formesJuridiquesNonAnnuleesTriees, event.getDateEvenement());
			if (formeJuridiqueFiscaleEntreprise == null) {
				return false;
			}
			final FormeLegale formeLegaleEntreprise = FormeLegale.fromCode(formeJuridiqueFiscaleEntreprise.getFormeJuridique().getCodeECH());
			if (formeLegaleEntreprise != null && formeLegaleEntreprise == organisation.getFormeLegale(event.getDateEvenement())) {
				return true;
			}
		} else {
			return false;
		}
		return false;
	}

	private String attributsCivilsAffichage(@NotNull String raisonSociale, @Nullable String noIde) {
		return String.format("%s%s", raisonSociale, noIde != null ? ", IDE: " + NO_IDE_RENDERER.toString(noIde) : StringUtils.EMPTY);
	}

	private static void panicNotFoundAfterIdent(Long found, String organisationDescription) throws EvenementOrganisationException {
		throw new EvenementOrganisationException(String.format("L'identifiant de tiers %s retourné par le service d'identification ne correspond à aucun tiers! Organisation recherchée: %s",
		                                                       found, organisationDescription));
	}

	// Selon SIFISC-16998 : mise en erreur des annonces sans données obligatoires
	private static void sanityCheck(EvenementOrganisation event, Organisation organisation) throws EvenementOrganisationException {
		final RegDate dateEvenement = event.getDateEvenement();

		final List<DateRanged<String>> noms = organisation.getNom();
		if (noms.size() > 0) {
			if (dateEvenement.isBefore(noms.get(0).getDateDebut())) {
				throw new EvenementOrganisationException(
						String.format("Erreur fatale: la date de l'événement %d (%s) est antérieure à la date de création (%s) de l'organisation telle que rapportée par RCEnt. No civil: %d, nom: %s.",
						              event.getNoEvenement(), RegDateHelper.dateToDisplayString(dateEvenement),
						              RegDateHelper.dateToDisplayString(noms.get(0).getDateDebut()), organisation.getNumeroOrganisation(), noms.get(0).getPayload()));
			}
			final Domicile siegePrincipal = organisation.getSiegePrincipal(dateEvenement);
			if (siegePrincipal == null) {
				throw new EvenementOrganisationException(
						String.format("Donnée RCEnt invalide: Site principal introuvable pour l'organisation no civil: %d",
						              organisation.getNumeroOrganisation()));
			}
			StringBuilder champs = new StringBuilder();
			FormeLegale formeLegale = organisation.getFormeLegale(dateEvenement);
			/*
			    SIFISC-19766 - Une forme juridique null est possible, dans le cas seulement ou l'organisation n'est pas inscrite au RC.

			    L'IDE n'étant de toute manière pas très digne de foi en terme de forme juridique, il y a toujours un contrôle derrière. On peut donc laisser passer les cas de forme juridique vide,
			    et épargner à l'ACI des événements non forçables sur les entités battardes que l'IDE inscrit et diffuse régulièrement en tant qu'entreprise.
			  */
			if (formeLegale == null && (organisation.isInscriteAuRC(dateEvenement))) {
				champs.append("[legalForm] ");
			}
			for (SiteOrganisation site : organisation.getDonneesSites()) {
				String nom = site.getNom(dateEvenement); // Le nom (obligatoire de par le xsd) nous permet de déduire si le site est existant pour la date données.
				if (nom != null && site.getDomiciles().isEmpty()) {
					champs.append(String.format("[Etablissement n°%d: seat] ", site.getNumeroSite()));
				}
			}
			if (champs.length() > 0) {
				throw new EvenementOrganisationException(String.format("Données RCEnt invalides pour l'organisation n°%d, champ(s) nécessaire(s) manquant(s): %s.", organisation.getNumeroOrganisation(), champs));
			}
		} else {
			throw new EvenementOrganisationException(
					String.format("Donnée RCEnt invalide: Champ obligatoire 'nom' pas trouvé pour l'organisation no civil: %d",
					              organisation.getNumeroOrganisation()));
		}
	}

	/**
	 * Rapprocher une organisation et une entreprise en les rattachant l'une à l'autre, ainsi que leurs établissements.
	 *
	 * @param organisation l'organisation à rattacher
	 * @param entreprise l'entreprise cible
	 * @throws EvenementOrganisationException en cas d'echec de l'opération de rattachement
	 */
	protected EvenementOrganisationInterne rattacheOrganisation(EvenementOrganisation event, Organisation organisation, Entreprise entreprise, EvenementOrganisationContext context, EvenementOrganisationOptions options) throws
			EvenementOrganisationException {
		try {
			RattachementOrganisationResult result = metierServicePM.rattacheOrganisationEntreprise(organisation, entreprise, event.getDateEvenement());
			if (result.isPartiel()) {
				String messageEtablissementsNonRattaches = null;
				if (!result.getEtablissementsNonRattaches().isEmpty()) {
				String etablissementsNonRattaches = CollectionsUtils.toString(result.getEtablissementsNonRattaches(), TIERS_NO_RENDERER, ", ");
					messageEtablissementsNonRattaches = String.format(" Cependant, certains établissements n'ont pas trouvé d'équivalent civil: %s.", etablissementsNonRattaches);
				}
				String messageSitesNonRattaches = null;
				if (!result.getSitesNonRattaches().isEmpty()) {
					String sitesNonRattaches = CollectionsUtils.toString(result.getSitesNonRattaches(), SITE_RENDERER, ", ");
					messageSitesNonRattaches = String.format(" Aussi des sites civils secondaires n'ont pas pu être rattachés et seront éventuellement créés: %s", sitesNonRattaches);
				}

				return new MessageSuiviPreExecution(event, organisation, entreprise, context, options,
				                                    String.format("Organisation civile n°%d rattachée à l'entreprise n°%s.%s%s",
						              organisation.getNumeroOrganisation(), FormatNumeroHelper.numeroCTBToDisplay(entreprise.getNumero()), messageEtablissementsNonRattaches == null ? "" : messageEtablissementsNonRattaches, messageSitesNonRattaches == null ? "" : messageSitesNonRattaches));
			} else {
				return new MessageSuiviPreExecution(event, organisation, entreprise, context, options,
				                                    String.format("Organisation civile n°%d rattachée avec succès à l'entreprise n°%s, avec tous ses établissements.",
						              organisation.getNumeroOrganisation(), FormatNumeroHelper.numeroCTBToDisplay(entreprise.getNumero())));
			}
		}
		catch (MetierServiceException e) {
			throw new EvenementOrganisationException(
					String.format("Impossible de rattacher l'organisation civile n°%d à l'entreprise n°%s: %s",
					              organisation.getNumeroOrganisation(),
					              FormatNumeroHelper.numeroCTBToDisplay(entreprise.getNumero()), e.getMessage()),
					e);
		}
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
	public void setRegimeFiscalService(RegimeFiscalService regimeFiscalService) {
		this.regimeFiscalService = regimeFiscalService;
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
	public void setEvenementOrganisationService(EvenementOrganisationService evenementOrganisationService) {
		this.evenementOrganisationService = evenementOrganisationService;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setAppariementService(AppariementService appariementService) {
		this.appariementService = appariementService;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setParametreAppService(ParametreAppService parametreAppService) {
		this.parametreAppService = parametreAppService;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setUseOrganisationsOfNotice(boolean useOrganisationsOfNotice) {
		this.useOrganisationsOfNotice = useOrganisationsOfNotice;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setCappingLevelProvider(EvenementOrganisationCappingLevelProvider cappingLevelProvider) {
		this.cappingLevelProvider = cappingLevelProvider;
	}
}
