package ch.vd.unireg.evenement.entreprise.engine.translator;

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
import ch.vd.unireg.evenement.entreprise.EvenementEntreprise;
import ch.vd.unireg.evenement.entreprise.EvenementEntrepriseCappingLevelProvider;
import ch.vd.unireg.evenement.entreprise.EvenementEntrepriseContext;
import ch.vd.unireg.evenement.entreprise.EvenementEntrepriseException;
import ch.vd.unireg.evenement.entreprise.EvenementEntrepriseOptions;
import ch.vd.unireg.evenement.entreprise.EvenementEntrepriseService;
import ch.vd.unireg.evenement.entreprise.interne.CappingAVerifier;
import ch.vd.unireg.evenement.entreprise.interne.CappingEnErreur;
import ch.vd.unireg.evenement.entreprise.interne.EvenementEntrepriseInterne;
import ch.vd.unireg.evenement.entreprise.interne.EvenementEntrepriseInterneComposite;
import ch.vd.unireg.evenement.entreprise.interne.Indexation;
import ch.vd.unireg.evenement.entreprise.interne.IndexationPure;
import ch.vd.unireg.evenement.entreprise.interne.MessageSuiviPreExecution;
import ch.vd.unireg.evenement.entreprise.interne.MessageWarningPreExectution;
import ch.vd.unireg.evenement.entreprise.interne.TraitementManuel;
import ch.vd.unireg.evenement.entreprise.interne.ValideurDebutDeTraitement;
import ch.vd.unireg.evenement.entreprise.interne.adresse.AdresseStrategy;
import ch.vd.unireg.evenement.entreprise.interne.creation.CreateEntrepriseStrategy;
import ch.vd.unireg.evenement.entreprise.interne.decisionaci.DecisionAciStrategy;
import ch.vd.unireg.evenement.entreprise.interne.demenagement.DemenagementSiegeStrategy;
import ch.vd.unireg.evenement.entreprise.interne.donneeinvalide.FormeJuridiqueInvalideStrategy;
import ch.vd.unireg.evenement.entreprise.interne.donneeinvalide.FormeJuridiqueManquanteStrategy;
import ch.vd.unireg.evenement.entreprise.interne.doublon.DoublonEntrepriseRemplacanteStrategy;
import ch.vd.unireg.evenement.entreprise.interne.doublon.DoublonEntrepriseRemplaceeParStrategy;
import ch.vd.unireg.evenement.entreprise.interne.doublon.DoublonEtablissementStrategy;
import ch.vd.unireg.evenement.entreprise.interne.etablissement.EtablissementsSecondairesStrategy;
import ch.vd.unireg.evenement.entreprise.interne.formejuridique.ChangementFormeJuridiqueStrategy;
import ch.vd.unireg.evenement.entreprise.interne.information.FailliteConcordatStrategy;
import ch.vd.unireg.evenement.entreprise.interne.information.ModificationButsStrategy;
import ch.vd.unireg.evenement.entreprise.interne.information.ModificationCapitalStrategy;
import ch.vd.unireg.evenement.entreprise.interne.information.ModificationStatutsStrategy;
import ch.vd.unireg.evenement.entreprise.interne.inscription.InscriptionStrategy;
import ch.vd.unireg.evenement.entreprise.interne.radiation.RadiationStrategy;
import ch.vd.unireg.evenement.entreprise.interne.raisonsociale.RaisonSocialeStrategy;
import ch.vd.unireg.evenement.entreprise.interne.reinscription.ReinscriptionStrategy;
import ch.vd.unireg.evenement.entreprise.interne.retour.annonce.RetourAnnonceIDE;
import ch.vd.unireg.evenement.entreprise.interne.transformation.DissolutionStrategy;
import ch.vd.unireg.evenement.entreprise.interne.transformation.FusionScissionStrategy;
import ch.vd.unireg.evenement.entreprise.interne.transformation.LiquidationStrategy;
import ch.vd.unireg.evenement.fiscal.EvenementFiscalService;
import ch.vd.unireg.evenement.ide.ReferenceAnnonceIDE;
import ch.vd.unireg.evenement.identification.contribuable.CriteresEntreprise;
import ch.vd.unireg.identification.contribuable.IdentificationContribuableService;
import ch.vd.unireg.identification.contribuable.TooManyIdentificationPossibilitiesException;
import ch.vd.unireg.indexer.tiers.GlobalTiersIndexer;
import ch.vd.unireg.interfaces.entreprise.data.AnnonceIDEEnvoyee;
import ch.vd.unireg.interfaces.entreprise.data.DateRanged;
import ch.vd.unireg.interfaces.entreprise.data.Domicile;
import ch.vd.unireg.interfaces.entreprise.data.EntrepriseCivile;
import ch.vd.unireg.interfaces.entreprise.data.EntrepriseCivileEvent;
import ch.vd.unireg.interfaces.entreprise.data.EtablissementCivil;
import ch.vd.unireg.interfaces.entreprise.data.FormeLegale;
import ch.vd.unireg.interfaces.entreprise.rcent.RCEntAnnonceIDEHelper;
import ch.vd.unireg.interfaces.service.ServiceEntreprise;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;
import ch.vd.unireg.metier.MetierServiceException;
import ch.vd.unireg.metier.MetierServicePM;
import ch.vd.unireg.metier.RattachementEntrepriseResult;
import ch.vd.unireg.metier.assujettissement.AssujettissementService;
import ch.vd.unireg.parametrage.ParametreAppService;
import ch.vd.unireg.regimefiscal.RegimeFiscalService;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.EntrepriseNotFoundException;
import ch.vd.unireg.tiers.Etablissement;
import ch.vd.unireg.tiers.FormeJuridiqueFiscaleEntreprise;
import ch.vd.unireg.tiers.PlusieursEntreprisesAvecMemeNumeroCivilException;
import ch.vd.unireg.tiers.RaisonSocialeFiscaleEntreprise;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.tiers.TiersDAO;
import ch.vd.unireg.tiers.TiersService;
import ch.vd.unireg.tiers.rattrapage.appariement.AppariementService;
import ch.vd.unireg.type.TypeAutoriteFiscale;
import ch.vd.unireg.type.TypeEvenementEntreprise;

/**
 * Convertisseur d'événements reçus de RCEnt en événements entreprise civile internes
 */
public class EvenementEntrepriseTranslatorImpl implements EvenementEntrepriseTranslator, InitializingBean {

	private static final Logger LOGGER = LoggerFactory.getLogger(EvenementEntrepriseTranslatorImpl.class);

	private static final StringRenderer<Tiers> TIERS_NO_RENDERER = tiers -> String.format("n°%s", FormatNumeroHelper.numeroCTBToDisplay(tiers.getNumero()));

	private static final StringRenderer<String> NO_IDE_RENDERER = FormatNumeroHelper::formatNumIDE;

	private static final StringRenderer<EtablissementCivil> ETABLISSEMENT_RENDERER = etablissement -> String.format("n°%d", etablissement.getNumeroEtablissement());

	private ServiceEntreprise serviceEntreprise;
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
	private EvenementEntrepriseService evenementEntrepriseService;
	private AppariementService appariementService;
	private ParametreAppService parametreAppService;
	private boolean useOrganisationsOfNotice;
	private EvenementEntrepriseCappingLevelProvider cappingLevelProvider;

	/*
	 * Non injecté mais créé ci-dessous dans afterPropertiesSet()
	 */
	private EvenementEntrepriseContext context;
	private List<EvenementEntrepriseTranslationStrategy> strategies;

	public EvenementEntrepriseTranslatorImpl(boolean useOrganisationsOfNotice) {
		this.useOrganisationsOfNotice = useOrganisationsOfNotice;
	}

	public EvenementEntrepriseTranslatorImpl() {
		this.useOrganisationsOfNotice = false;
	}

	private final EvenementEntrepriseOptions options = new EvenementEntrepriseOptions();

	@Override
	public void afterPropertiesSet() throws Exception {
		context = new EvenementEntrepriseContext(serviceEntreprise, evenementEntrepriseService, serviceInfrastructureService, regimeFiscalService, dataEventService, tiersService, indexer, metierServicePM, tiersDAO, adresseService,
		                                         evenementFiscalService, assujettissementService, appariementService, parametreAppService);

		// Construction des stratégies
		strategies = new ArrayList<>();

		/*
			L'ordre des stratégies est important.
		 */
		strategies.add(new FormeJuridiqueManquanteStrategy(context, options));
		strategies.add(new FormeJuridiqueInvalideStrategy(context, options));
		strategies.add(new DecisionAciStrategy(context, options));
		strategies.add(new CreateEntrepriseStrategy(context, options));
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
	 * Traduit un événement entreprise civile en zéro, un ou plusieurs événements externes correspondant (Dans ce cas, un événement composite est retourné.
	 * <p>
	 *     AVERTISSEMENT: Contrairement au traitement des événements Civil Ech, pour lesquel un cablage explicite des stratégies est en vigueur, le traitement des événements
	 *     entreprise civile fait l'objet d'un essai de chaque stratégie disponible, chacune détectant un cas particulier de changement et donnant lieu à un événement interne.
	 *     De fait, les nouveaux types d'événements entreprise civile sont donc silencieusement ignorés jusqu'à ce qu'une stratégie soit codée pour les reconnaître.
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
	 * @param event   un événement entreprise civile externe
	 * @return Un événement interne correspondant à l'événement passé en paramètre
	 * @throws EvenementEntrepriseException En cas d'erreur dans la création de l'événements interne, null s'il n'y a pas lieu de créer un événement
	 */
	@Override
	public EvenementEntrepriseInterne toInterne(EvenementEntreprise event) throws EvenementEntrepriseException {
		final EntrepriseCivile entrepriseCivile;
		if (useOrganisationsOfNotice) {
			final EntrepriseCivileEvent entrepriseCivileEvent = serviceEntreprise.getEntrepriseEvent(event.getNoEvenement()).get(event.getNoEntrepriseCivile());
			if (entrepriseCivileEvent == null) {
				throw new EvenementEntrepriseException(
						String.format("Fatal: l'événement %d ne contient pas de données pour l'entreprise civile %d!", event.getNoEvenement(), event.getNoEntrepriseCivile())
				);
			}
			entrepriseCivile = entrepriseCivileEvent.getPseudoHistory();
		} else {
			LOGGER.warn("Utilisation du service RCEnt WS Organisation à la place du WS OrganisationsOfNotice! Traitements à double possibles.");
			entrepriseCivile = serviceEntreprise.getEntrepriseHistory(event.getNoEntrepriseCivile());
			if (entrepriseCivile == null) {
				throw new EntrepriseNotFoundException(event.getNoEntrepriseCivile());
			}
		}

		// Sanity check. Pourquoi ici? Pour ne pas courir le risque d'ignorer des entreprises (passer à TRAITE) sur la foi d'information manquantes.
		sanityCheck(event, entrepriseCivile);

		final String entrepriseDescription = serviceEntreprise.createEntrepriseDescription(entrepriseCivile, event.getDateEvenement());
		Audit.info(event.getNoEvenement(), String.format("Entreprise civile trouvée: %s", entrepriseDescription));

		final String raisonSocialeCivile = entrepriseCivile.getNom(event.getDateEvenement());
		final String noIdeCivil = entrepriseCivile.getNumeroIDE(event.getDateEvenement());

		Entreprise entreprise;
		final List<EvenementEntrepriseInterne> evenements = new ArrayList<>();

		try {
			entreprise = context.getTiersDAO().getEntrepriseByNoEntrepriseCivile(entrepriseCivile.getNumeroEntreprise());
		}
		catch (PlusieursEntreprisesAvecMemeNumeroCivilException e) {
			return new TraitementManuel(event, entrepriseCivile, null, context, options, e.getMessage());
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
				throw new EvenementEntrepriseException(
						String.format(
								"Fatal: Impossible de traiter l'événement de retour d'annonce à l'IDE n°%d: impossible de retrouver l'entreprise pour l'annonce à l'IDE %d (établissement n°%s)! " +
										"Le rapport entre tiers a peut être changé depuis?",
								event.getNoEvenement(), noAnnonceIDE, FormatNumeroHelper.numeroCTBToDisplay(etablissement.getNumeroEtablissement())
						)
				);
			}

			final AnnonceIDEEnvoyee annonceIDE = serviceEntreprise.getAnnonceIDE(noAnnonceIDE, RCEntAnnonceIDEHelper.UNIREG_USER);
			if (annonceIDE == null) {
				throw new EvenementEntrepriseException(
						String.format(
								"Fatal: Impossible de traiter l'événement de retour d'annonce à l'IDE n°%d: impossible de retrouver l'annonce à l'IDE %d à l'origine de l'événement! " +
										"Le rapport entre tiers a peut être changé depuis?",
								event.getNoEvenement(), noAnnonceIDE
						)
				);
			}
			try {
				final EvenementEntreprise evenementEntreprise = evenementEntrepriseService.getEvenementForNoAnnonceIDE(noAnnonceIDE);
				if (event.getId() != evenementEntreprise.getId()) {
					final String message =
							String.format("Un événement RCEnt est déjà associé à l'annonce à l'IDE n°%d. Le présent événement n°%d ne peut lui aussi " +
									              "provenir de cette annonce à l'IDE. C'est un bug du registre civil. Traitement manuel.",
							              event.getNoEvenement(), noAnnonceIDE
							);
					Audit.error(event.getNoEvenement(), message);
					throw new EvenementEntrepriseException(message);
				}
			}
			catch (NonUniqueResultException e) {
				final String message =
						String.format("Un ou plusieurs précédant événements RCEnt sont déjà associés à l'annonce à l'IDE n°%d. Le présent événement n°%d ne peut lui aussi " +
								              "provenir de cette annonce à l'IDE. C'est un bug du registre civil. Traitement manuel.",
						              event.getNoEvenement(), noAnnonceIDE
						);
				Audit.error(event.getNoEvenement(), message);
				throw new EvenementEntrepriseException(message);
			}

			evenements.add(new RetourAnnonceIDE(event, entrepriseCivile, entreprise, context, options, annonceIDE));
			// Pas question de traiter normallement, on connait déjà les changements.
			evaluateStrategies = false;
		}
		/* L'entreprise est retrouvé grâce au numéro cantonal. Pas de doute possible. */
		else if (entreprise != null) {
			final String message = String.format("Entreprise n°%s (%s%s) identifiée sur la base du numéro civil %d (numéro cantonal).",
			                                     FormatNumeroHelper.numeroCTBToDisplay(entreprise.getNumero()),
			                                     raisonSocialeCivile,
			                                     noIdeCivil != null ? ", IDE: " + NO_IDE_RENDERER.toString(noIdeCivil) : StringUtils.EMPTY,
			                                     entrepriseCivile.getNumeroEntreprise());
			Audit.info(event.getNoEvenement(), message);
			evenements.add(new MessageSuiviPreExecution(event, entrepriseCivile, entreprise, context, options, message));

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
						panicNotFoundAfterIdent(found.get(0), entrepriseDescription);
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
					return new TraitementManuel(event, entrepriseCivile, null, context, options, message);
				}
			}
			// L'identificatione est un échec: selon toute vraisemblance, on connait le tiers mais il y a beaucoup trop de résultat.
			catch (TooManyIdentificationPossibilitiesException e) {
				String message = String.format("L'identification de l'entreprise civile a renvoyé un trop grand nombre de résultats pour les attributs civils [%s]! Arrêt du traitement.",
				                               attributsCivilsAffichage(raisonSocialeCivile, noIdeCivil));
				Audit.info(event.getNoEvenement(), message);
				return new TraitementManuel(event, entrepriseCivile, null, context, options, message);
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
					// Le tiers entreprise trouvé est déjà apparié à une autre entreprise civile. Causé soit par un doublon dans RCEnt, soit par une grande similitude des attributs civils.
					else {
						final EntrepriseCivile entrepriseCivileDejaAppariee = serviceEntreprise.getEntrepriseHistory(entreprise.getNumeroEntreprise());
						message = String.format("Entreprise n°%s identifiée sur la base de ses attributs civils [%s], mais déjà rattachée à l'entreprise civile n°%s (%s). Potentiel doublon au civil. Traitement manuel.",
						                        FormatNumeroHelper.numeroCTBToDisplay(entreprise.getNumero()),
						                        attributsCivilsAffichage(raisonSocialeCivile, noIdeCivil),
						                        entreprise.getNumeroEntreprise(),
						                        entrepriseCivileDejaAppariee == null ? "<introuvable au civil>" : attributsCivilsAffichage(entrepriseCivileDejaAppariee.getNom(event.getDateEvenement()), entrepriseCivileDejaAppariee.getNumeroIDE(event.getDateEvenement())));
						return new TraitementManuel(event, entrepriseCivile, null, context, options, message);
					}
					Audit.info(event.getNoEvenement(), message);
					evenements.add(new MessageSuiviPreExecution(event, entrepriseCivile, entreprise, context, options, message));

					if (checkFormesJuridiquesCompatibles(event, entrepriseCivile, entreprise)) {
						evenements.add(rattacheEntreprise(event, entrepriseCivile, entreprise, context, options));
					} else {
						final List<FormeJuridiqueFiscaleEntreprise> formesJuridiquesNonAnnuleesTriees = entreprise.getFormesJuridiquesNonAnnuleesTriees();
						FormeJuridiqueFiscaleEntreprise formeJuridiqueFiscaleEntreprise = null;
						if (!formesJuridiquesNonAnnuleesTriees.isEmpty()) {
							formeJuridiqueFiscaleEntreprise = DateRangeHelper.rangeAt(entreprise.getFormesJuridiquesNonAnnuleesTriees(), event.getDateEvenement());
						}

						final FormeLegale formeLegale = entrepriseCivile.getFormeLegale(event.getDateEvenement());
						return new TraitementManuel(event, entrepriseCivile, entreprise, context, options,
						                            String.format("Impossible de rattacher l'entreprise civile n°%d%s à l'entreprise n°%s (%s)%s " +
								                                          "identifiée sur la base de ses attributs civils [%s]: les formes juridiques ne correspondent pas. Arrêt du traitement.",
						                                          entrepriseCivile.getNumeroEntreprise(),
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
							                                     "et sera ignoré. Si nécessaire, un tiers Entreprise sera créé pour l'entreprise civile n°%d, en doublon du " +
							                                     "tiers n°%s (%s).\n",
					                                     FormatNumeroHelper.numeroCTBToDisplay(tiers.getNumero()),
					                                     attributsCivilsAffichage(raisonSocialeCivile, noIdeCivil),
					                                     tiers.getType().getDescription(),
					                                     entrepriseCivile.getNumeroEntreprise(),
					                                     FormatNumeroHelper.numeroCTBToDisplay(tiers.getNumero()),
					                                     tiers.getType().getDescription());
							Audit.info(event.getNoEvenement(), message);
					evenements.add(new MessageWarningPreExectution(event, entrepriseCivile, null, context, options, message));
				}
			}
			// L'identification n'a rien retourné. Cela veut dire qu'on ne connait pas déjà le tiers. On rapporte simplement cet état de fait.
			else {
				final String message = String.format("Aucune entreprise identifiée pour le numéro civil %d ou les attributs civils [%s].",
				                                     entrepriseCivile.getNumeroEntreprise(),
				                                     attributsCivilsAffichage(raisonSocialeCivile, noIdeCivil));
				Audit.info(event.getNoEvenement(), message);
				evenements.add(new MessageSuiviPreExecution(event, entrepriseCivile, null, context, options, message));
			}
		}

		/*
		    SIFISC-21128 - Sauter les annonces IDE liées à une nouvelle inscription sur VD lorsque c'est possible, afin de pouvoir traiter automatiquement
		    l'événement FOSC qui suit derrière.
		  */
		if (entreprise == null && evenementAIgnorer(event)) {
			evenements.add(
					new MessageSuiviPreExecution(event, entrepriseCivile, null, context, options,
					                             String.format("L'événement pour l'entreprise civile n°%d précède un événement FOSC d'inscription RC VD sans historique avant ce jour au civil. Ignoré.",
					                                           entrepriseCivile.getNumeroEntreprise())
					)
			);
			evaluateStrategies = false;
		}


		/* Essayer chaque stratégie. Chacune est responsable de détecter l'événement dans les données. */
		final List<EvenementEntrepriseInterne> resultatEvaluationStrategies = new ArrayList<>();
		if (evaluateStrategies) {
			for (EvenementEntrepriseTranslationStrategy strategy : strategies) {
				final EvenementEntrepriseInterne e = strategy.matchAndCreate(event, entrepriseCivile, entreprise);
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
		//Validation au début du traitement effectif.
		evenements.add(new ValideurDebutDeTraitement(event, entrepriseCivile, entreprise, context, options));

		if (resultatEvaluationStrategies.size() == 0) {
			// Pas de véritable traitement à exécuter. Indexation seulement. Le status sera TRAITE.
			Audit.info(event.getId(), "Pas de changement détecté. L'entité sera simplement réindexée (si connue).");
			evenements.add(new IndexationPure(event, entrepriseCivile, entreprise, context, options));

		}
		else {
			// Il y a des traitements à exécuter. Indexation obligatoire pour toute entité connue d'Unireg. Le status sera laissé inchangé.
			evenements.addAll(resultatEvaluationStrategies);
			Audit.info(event.getId(), "L'entité sera (re)indexée.");
			evenements.add(new Indexation(event, entrepriseCivile, entreprise, context, options));
		}

		// [SIFISC-19214] blindage "capping" de l'état final de l'événement entreprise civile
		final NiveauCappingEtat etatCapping = cappingLevelProvider != null ? cappingLevelProvider.getNiveauCapping() : null;
		if (etatCapping != null) {
			evenements.add(buildEvenementInterneCapping(event, entrepriseCivile, entreprise, context, options, etatCapping));
		}

		return new EvenementEntrepriseInterneComposite(event, entrepriseCivile, evenements.get(0).getEntreprise(), context, options, evenements);
	}

	private String determineRaisonSocialeFiscaleEntreprise(EvenementEntreprise event, Entreprise entreprise) {
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
	 * NOTE: Il est requis de contrôler qu'on n'a pas d'entreprise en base associée à ce numéro d'entreprise civile avant d'appeler cette méthode.
	 *
	 * @param event l'événement
	 */
	protected boolean evenementAIgnorer(EvenementEntreprise event) {

		final RegDate dateEvenement = event.getDateEvenement();

		final EntrepriseCivile entrepriseCivileHistory = serviceEntreprise.getEntrepriseHistory(event.getNoEntrepriseCivile());
		final EtablissementCivil etablissementPrincipal = entrepriseCivileHistory.getEtablissementPrincipal(dateEvenement).getPayload();
		final Domicile siegePrincipalAvant = entrepriseCivileHistory.getSiegePrincipal(dateEvenement.getOneDayBefore());
		final Domicile siegePrincipalApres = entrepriseCivileHistory.getSiegePrincipal(dateEvenement);

		/*
		    Les 5 conditions pour estimer sans risque d'ignorer un événement IDE au profit de l'événement FOSC à venir.
		    FIXME: On ne supporte pas correctement les événements IDE reçus après la FOSC pour un même jour. Perte potentielle de changement d'adresse.
		  */

		// Etre un événement IDE d'inscription ou mutation
		final boolean evtIDEIncriptionMutation = event.getType() == TypeEvenementEntreprise.IDE_NOUVELLE_INSCRIPTION || event.getType() == TypeEvenementEntreprise.IDE_MUTATION;

		// On s'attend à une nouvelle inscription au RC (Vaudois, cf. condition de lieu ci-dessous), donc il faut être inscrit et non radié.
		final boolean actifAuRC = etablissementPrincipal.isConnuInscritAuRC(dateEvenement) && !etablissementPrincipal.isRadieDuRC(dateEvenement);

		// Ne pas avoir d'historique avant aujourd'hui au civil. C'est-à-dire être une nouvelle inscription RC VD, soit par fondation, soit par arrivée. On contrôle aussi l'absence d'événements.
		final boolean estNouveauCivil = entrepriseCivileHistory.getNom(dateEvenement.getOneDayBefore()) == null &&
				evenementEntrepriseService.evenementsPourDateValeurEtEntreprise(dateEvenement.getOneDayBefore(), event.getNoEntrepriseCivile()).isEmpty();

		// On doit être vaudois. On ne peut s'attendre à un événement FOSC pour les cas hors canton, et on veut exclure tout risque de traiter un événement FOSC pour un établissement secondaire.
		final boolean estVaudoise = siegePrincipalApres.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD;

		// S'assurer que l'événement est vide de tout changement de siège, car dès lors qu'il est présent sur un événement IDE, il serait absent de l'événement FOSC qui suivrait.
		final boolean sansChangementDeSiege = siegePrincipalAvant == null || (siegePrincipalAvant.getTypeAutoriteFiscale() == siegePrincipalApres.getTypeAutoriteFiscale() && siegePrincipalAvant.getNumeroOfsAutoriteFiscale().equals(siegePrincipalApres.getNumeroOfsAutoriteFiscale()));

		// S'assurer qu'il y a aucun d'événement FOSC avant et au moins un FOSC_NOUVELLE_ENTREPRISE après pour le même jour (SIFISC-23174: ne pas être trop laxiste et insister pour un événement de création)
		final List<EvenementEntreprise> evenementsDuJour = evenementEntrepriseService.evenementsPourDateValeurEtEntreprise(dateEvenement, event.getNoEntrepriseCivile());
		final List<EvenementEntreprise> evenementsFOSCRecusAvant = new ArrayList<>();
		final List<EvenementEntreprise> evenementsFOSCNouvelleInscriptionRecusApres = new ArrayList<>();
		for (EvenementEntreprise evt : evenementsDuJour) {
			if (evt.getType().getSource() == TypeEvenementEntreprise.Source.FOSC) {
				if (evt.getId() < event.getId()) {
					evenementsFOSCRecusAvant.add(evt);
				}
				else if (evt.getId() > event.getId() && evt.getType() == TypeEvenementEntreprise.FOSC_NOUVELLE_ENTREPRISE) {
					evenementsFOSCNouvelleInscriptionRecusApres.add(evt);
				}
			}
		}
		final boolean conditionEvtFOSC = evenementsFOSCRecusAvant.isEmpty() && evenementsFOSCNouvelleInscriptionRecusApres.size() > 0;

		return evtIDEIncriptionMutation && actifAuRC && estNouveauCivil && estVaudoise && sansChangementDeSiege && conditionEvtFOSC;
	}

	private static EvenementEntrepriseInterne buildEvenementInterneCapping(EvenementEntreprise event, EntrepriseCivile entrepriseCivile, Entreprise entreprise,
	                                                                       EvenementEntrepriseContext context, EvenementEntrepriseOptions options,
	                                                                       @NotNull NiveauCappingEtat etatCapping) {
		switch (etatCapping) {
		case A_VERIFIER:
			return new CappingAVerifier(event, entrepriseCivile, entreprise, context, options);
		case EN_ERREUR:
			return new CappingEnErreur(event, entrepriseCivile, entreprise, context, options);
		default:
			throw new IllegalArgumentException("Valeur du niveau de capping non-supportée : " + etatCapping);
		}
	}

	private boolean checkFormesJuridiquesCompatibles(EvenementEntreprise event, EntrepriseCivile entrepriseCivile, Entreprise entreprise) {
		final List<FormeJuridiqueFiscaleEntreprise> formesJuridiquesNonAnnuleesTriees = entreprise.getFormesJuridiquesNonAnnuleesTriees();
		if (!formesJuridiquesNonAnnuleesTriees.isEmpty()) {
			final FormeJuridiqueFiscaleEntreprise formeJuridiqueFiscaleEntreprise = DateRangeHelper.rangeAt(formesJuridiquesNonAnnuleesTriees, event.getDateEvenement());
			if (formeJuridiqueFiscaleEntreprise == null) {
				return false;
			}
			final FormeLegale formeLegaleEntreprise = FormeLegale.fromCode(formeJuridiqueFiscaleEntreprise.getFormeJuridique().getCodeECH());
			if (formeLegaleEntreprise != null && formeLegaleEntreprise == entrepriseCivile.getFormeLegale(event.getDateEvenement())) {
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

	private static void panicNotFoundAfterIdent(Long found, String entrepriseDescription) throws EvenementEntrepriseException {
		throw new EvenementEntrepriseException(String.format("L'identifiant de tiers %s retourné par le service d'identification ne correspond à aucun tiers! entreprise civile recherchée: %s",
		                                                     found, entrepriseDescription));
	}

	// Selon SIFISC-16998 : mise en erreur des annonces sans données obligatoires
	private static void sanityCheck(EvenementEntreprise event, EntrepriseCivile entrepriseCivile) throws EvenementEntrepriseException {
		final RegDate dateEvenement = event.getDateEvenement();

		final List<DateRanged<String>> noms = entrepriseCivile.getNom();
		if (noms.size() > 0) {
			if (dateEvenement.isBefore(noms.get(0).getDateDebut())) {
				throw new EvenementEntrepriseException(
						String.format("Erreur fatale: la date de l'événement %d (%s) est antérieure à la date de création (%s) de l'entreprise civile telle que rapportée par RCEnt. No civil: %d, nom: %s.",
						              event.getNoEvenement(), RegDateHelper.dateToDisplayString(dateEvenement),
						              RegDateHelper.dateToDisplayString(noms.get(0).getDateDebut()), entrepriseCivile.getNumeroEntreprise(), noms.get(0).getPayload()));
			}
			final Domicile siegePrincipal = entrepriseCivile.getSiegePrincipal(dateEvenement);
			if (siegePrincipal == null) {
				throw new EvenementEntrepriseException(
						String.format("Donnée RCEnt invalide: Etablissement civil principal introuvable pour l'entreprise civile no civil: %d",
						              entrepriseCivile.getNumeroEntreprise()));
			}
			StringBuilder champs = new StringBuilder();
			FormeLegale formeLegale = entrepriseCivile.getFormeLegale(dateEvenement);
			/*
			    SIFISC-19766 - Une forme juridique null est possible, dans le cas seulement ou l'entreprise civile n'est pas inscrite au RC.

			    L'IDE n'étant de toute manière pas très digne de foi en terme de forme juridique, il y a toujours un contrôle derrière. On peut donc laisser passer les cas de forme juridique vide,
			    et épargner à l'ACI des événements non forçables sur les entités battardes que l'IDE inscrit et diffuse régulièrement en tant qu'entreprise.
			  */
			if (formeLegale == null && (entrepriseCivile.isInscriteAuRC(dateEvenement))) {
				champs.append("[legalForm] ");
			}
			for (EtablissementCivil etablissement : entrepriseCivile.getEtablissements()) {
				String nom = etablissement.getNom(dateEvenement); // Le nom (obligatoire de par le xsd) nous permet de déduire si l'établissement civil est existant pour la date données.
				if (nom != null && etablissement.getDomiciles().isEmpty()) {
					champs.append(String.format("[Etablissement n°%d: seat] ", etablissement.getNumeroEtablissement()));
				}
			}
			if (champs.length() > 0) {
				throw new EvenementEntrepriseException(String.format("Données RCEnt invalides pour l'entreprise civile n°%d, champ(s) nécessaire(s) manquant(s): %s.", entrepriseCivile.getNumeroEntreprise(), champs));
			}
		} else {
			throw new EvenementEntrepriseException(
					String.format("Donnée RCEnt invalide: Champ obligatoire 'nom' pas trouvé pour l'entreprise civile no civil: %d",
					              entrepriseCivile.getNumeroEntreprise()));
		}
	}

	/**
	 * Rapprocher une entreprise civile et une entreprise en les rattachant l'une à l'autre, ainsi que leurs établissements.
	 *
	 * @param entrepriseCivile l'entreprise civile à rattacher
	 * @param entreprise l'entreprise cible
	 * @throws EvenementEntrepriseException en cas d'echec de l'opération de rattachement
	 */
	protected EvenementEntrepriseInterne rattacheEntreprise(EvenementEntreprise event, EntrepriseCivile entrepriseCivile, Entreprise entreprise, EvenementEntrepriseContext context, EvenementEntrepriseOptions options) throws
			EvenementEntrepriseException {
		try {
			RattachementEntrepriseResult result = metierServicePM.rattacheEntreprisesCivileEtFiscal(entrepriseCivile, entreprise, event.getDateEvenement());
			if (result.isPartiel()) {
				String messageEtablissementsNonRattaches = null;
				if (!result.getEtablissementsNonRattaches().isEmpty()) {
				String etablissementsNonRattaches = CollectionsUtils.toString(result.getEtablissementsNonRattaches(), TIERS_NO_RENDERER, ", ");
					messageEtablissementsNonRattaches = String.format(" Cependant, certains établissements n'ont pas trouvé d'équivalent civil: %s.", etablissementsNonRattaches);
				}
				String messageEtablissementsCivilsNonRattaches = null;
				if (!result.getEtablissementsCivilsNonRattaches().isEmpty()) {
					String etablissementsNonRattaches = CollectionsUtils.toString(result.getEtablissementsCivilsNonRattaches(), ETABLISSEMENT_RENDERER, ", ");
					messageEtablissementsCivilsNonRattaches = String.format(" Aussi des établissements civils secondaires n'ont pas pu être rattachés et seront éventuellement créés: %s", etablissementsNonRattaches);
				}

				return new MessageSuiviPreExecution(event, entrepriseCivile, entreprise, context, options,
				                                    String.format("Entreprise civile n°%d rattachée à l'entreprise n°%s.%s%s",
				                                                  entrepriseCivile.getNumeroEntreprise(), FormatNumeroHelper.numeroCTBToDisplay(entreprise.getNumero()), messageEtablissementsNonRattaches == null ? "" : messageEtablissementsNonRattaches, messageEtablissementsCivilsNonRattaches == null ? "" : messageEtablissementsCivilsNonRattaches));
			} else {
				return new MessageSuiviPreExecution(event, entrepriseCivile, entreprise, context, options,
				                                    String.format("Entreprise civile n°%d rattachée avec succès à l'entreprise n°%s, avec tous ses établissements.",
				                                                  entrepriseCivile.getNumeroEntreprise(), FormatNumeroHelper.numeroCTBToDisplay(entreprise.getNumero())));
			}
		}
		catch (MetierServiceException e) {
			throw new EvenementEntrepriseException(
					String.format("Impossible de rattacher l'entreprise civile n°%d à l'entreprise n°%s: %s",
					              entrepriseCivile.getNumeroEntreprise(),
					              FormatNumeroHelper.numeroCTBToDisplay(entreprise.getNumero()), e.getMessage()),
					e);
		}
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setServiceEntreprise(ServiceEntreprise serviceEntreprise) {
		this.serviceEntreprise = serviceEntreprise;
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
	public void setEvenementEntrepriseService(EvenementEntrepriseService evenementEntrepriseService) {
		this.evenementEntrepriseService = evenementEntrepriseService;
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
	public void setCappingLevelProvider(EvenementEntrepriseCappingLevelProvider cappingLevelProvider) {
		this.cappingLevelProvider = cappingLevelProvider;
	}
}
