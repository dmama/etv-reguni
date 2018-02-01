package ch.vd.unireg.declaration.snc;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.FetchMode;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.Assert;
import ch.vd.shared.batchtemplate.BatchWithResultsCallback;
import ch.vd.shared.batchtemplate.Behavior;
import ch.vd.shared.batchtemplate.SimpleProgressMonitor;
import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.common.AuthenticationInterface;
import ch.vd.unireg.common.HibernateEntity;
import ch.vd.unireg.common.LoggingStatusManager;
import ch.vd.unireg.common.ParallelBatchTransactionTemplateWithResults;
import ch.vd.unireg.common.StatusManager;
import ch.vd.unireg.declaration.DeclarationException;
import ch.vd.unireg.declaration.PeriodeFiscale;
import ch.vd.unireg.declaration.PeriodeFiscaleDAO;
import ch.vd.unireg.declaration.QuestionnaireSNC;
import ch.vd.unireg.hibernate.HibernateCallback;
import ch.vd.unireg.hibernate.HibernateTemplate;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;
import ch.vd.unireg.parametrage.ParametreAppService;
import ch.vd.unireg.tiers.CollectiviteAdministrative;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.Tache;
import ch.vd.unireg.tiers.TacheAnnulationQuestionnaireSNC;
import ch.vd.unireg.tiers.TacheCriteria;
import ch.vd.unireg.tiers.TacheDAO;
import ch.vd.unireg.tiers.TacheEnvoiQuestionnaireSNC;
import ch.vd.unireg.tiers.TiersService;
import ch.vd.unireg.type.CategorieEntreprise;
import ch.vd.unireg.type.TypeEtatTache;
import ch.vd.unireg.type.TypeTache;
import ch.vd.unireg.validation.ValidationService;

/**
 * Processeur pour l'implémentation de la génération en masse des tâches d'envoi des questionnaires SNC d'une période fiscale
 */
public class DeterminationQuestionnairesSNCAEmettreProcessor {

	private static final Logger LOGGER = LoggerFactory.getLogger(DeterminationQuestionnairesSNCAEmettreProcessor.class);

	private static final int BATCH_SIZE = 100;

	private final ParametreAppService parametres;
	private final PlatformTransactionManager transactionManager;
	private final PeriodeFiscaleDAO periodeDAO;
	private final HibernateTemplate hibernateTemplate;
	private final TiersService tiersService;
	private final AdresseService adresseService;
	private final ValidationService validationService;
	private final TacheDAO tacheDAO;
	private final QuestionnaireSNCService questionnaireSNCService;

	public DeterminationQuestionnairesSNCAEmettreProcessor(ParametreAppService parametres, PlatformTransactionManager transactionManager, PeriodeFiscaleDAO periodeDAO, HibernateTemplate hibernateTemplate, TiersService tiersService,
	                                                       AdresseService adresseService, ValidationService validationService, TacheDAO tacheDAO, QuestionnaireSNCService questionnaireSNCService) {
		this.parametres = parametres;
		this.transactionManager = transactionManager;
		this.periodeDAO = periodeDAO;
		this.hibernateTemplate = hibernateTemplate;
		this.tiersService = tiersService;
		this.adresseService = adresseService;
		this.validationService = validationService;
		this.tacheDAO = tacheDAO;
		this.questionnaireSNCService = questionnaireSNCService;
	}

	public DeterminationQuestionnairesSNCResults run(final int periodeFiscale, final RegDate dateTraitement, final int nbThreads, StatusManager s) throws DeclarationException {
		final StatusManager status = s != null ? s : new LoggingStatusManager(LOGGER);

		// petites vérifications préalables
		checkParams(periodeFiscale, dateTraitement);

		// 1. récupération des identifiants des entreprises concernées
		status.setMessage("Récupération des contribuables à traiter...");
		final List<Long> ids = createListeIdsContribuables();

		// 2. traitement de ces identifiants par groupes
		final DeterminationQuestionnairesSNCResults rapportFinal = new DeterminationQuestionnairesSNCResults(periodeFiscale, dateTraitement, nbThreads, tiersService, adresseService);
		final SimpleProgressMonitor progressMonitor = new SimpleProgressMonitor();
		final ParallelBatchTransactionTemplateWithResults<Long, DeterminationQuestionnairesSNCResults>
				template = new ParallelBatchTransactionTemplateWithResults<>(ids, BATCH_SIZE, nbThreads, Behavior.REPRISE_AUTOMATIQUE, transactionManager, status, AuthenticationInterface.INSTANCE);
		template.execute(rapportFinal, new BatchWithResultsCallback<Long, DeterminationQuestionnairesSNCResults>() {
			@Override
			public boolean doInTransaction(List<Long> batch, DeterminationQuestionnairesSNCResults rapport) throws Exception {
				status.setMessage("Traitement du batch [" + batch.get(0) + "; " + batch.get(batch.size() - 1) + "] ...", progressMonitor.getProgressInPercent());
				traiterBatch(batch, periodeFiscale, dateTraitement, rapport, status);
				return true;
			}

			@Override
			public DeterminationQuestionnairesSNCResults createSubRapport() {
				return new DeterminationQuestionnairesSNCResults(periodeFiscale, dateTraitement, nbThreads, tiersService, adresseService);
			}
		}, progressMonitor);

		// on range et on rentre...
		if (status.isInterrupted()) {
			status.setMessage("La création des tâches d'envoi des questionnaires SNC a été interrompue.");
			rapportFinal.setInterrupted();
		}
		else {
			status.setMessage("La création des tâches d'envoi des questionnaires SNC est terminée.");
		}

		// récapitulation et fin
		rapportFinal.end();
		return rapportFinal;
	}

	private void traiterBatch(final List<Long> ids, int periodeFiscale, RegDate dateTraitement, DeterminationQuestionnairesSNCResults rapport, StatusManager statusManager) throws DeclarationException {
		// Récupère la période fiscale
		final PeriodeFiscale periode = periodeDAO.getPeriodeFiscaleByYear(periodeFiscale);
		if (periode == null) {
			throw new DeclarationException("La période fiscale " + periodeFiscale + " n'existe pas dans la base de données.");
		}

		// On charge tous les contribuables en vrac (avec préchargement des déclarations)
		final List<Entreprise> list = hibernateTemplate.execute(new HibernateCallback<List<Entreprise>>() {
			@Override
			public List<Entreprise> doInHibernate(Session session) throws HibernateException {
				final Criteria crit = session.createCriteria(Entreprise.class);
				crit.add(Restrictions.in("numero", ids));
				crit.setFetchMode("declarations", FetchMode.JOIN); // force le préchargement
				crit.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
				//noinspection unchecked
				return crit.list();
			}
		});

		final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(ServiceInfrastructureService.noOIPM);
		Assert.notNull(oipm);

		// Traitement de tous les contribuables un par un
		for (Entreprise ctb : list) {
			// on n'est pas obligé de tout faire si une interruption a été demandée
			if (statusManager.isInterrupted()) {
				break;
			}
			traiterEntreprise(ctb, periode, dateTraitement, oipm, rapport);
		}
	}

	private void traiterEntreprise(final Entreprise entreprise, PeriodeFiscale periode, RegDate dateTraitement, CollectiviteAdministrative oipm, final DeterminationQuestionnairesSNCResults rapport) {
		rapport.incrementNbOfInspectedEntreprises();

		// une entreprise ne va pas devenir valide au prétexte qu'une nouvelle tâche lui est associée
		if (validationService.validate(entreprise).hasErrors()) {
			rapport.addErrorCtbInvalide(entreprise);
			return;
		}

		// un questionnaire SNC est dû dès qu'un for vaudois de genre d'impôt "IRF" est valide au moins un jour
		// sur l'année civile de la période fiscale à traiter
		final Set<Integer> periodesACouvrir = questionnaireSNCService.getPeriodesFiscalesTheoriquementCouvertes(entreprise, true);
		final boolean hasVaudoisSurPeriode = periodesACouvrir.contains(periode.getAnnee());

		// s'il faut une tâche
		//      - s'il y a déjà un questionnaire, on ne fait pas de tâche
		//      - s'il n'y a pas de questionnaire non-annulé, mais déjà une tâche en instance, on ne fait rien
		//      - s'il n'y a ni questionnaire non-annulé ni tâche en instance, on crée une nouvelle tâche en instance
		// s'il ne faut pas de tâche
		//      - s'il y a un questionnaire non-annulé, on crée une tâche d'annulation
		//      - s'il n'y a pas de questionnaire non-annulé, mais déjà une tâche d'émission en instance, on l'annule
		//      - s'il n'y a ni questionnaire non-annulé ni tâche d'émission en instance, on ne fait rien
		final List<QuestionnaireSNC> questionnairesExistants = entreprise.getDeclarationsDansPeriode(QuestionnaireSNC.class, periode.getAnnee(), false);
		final List<Tache> tachesEnInstance = getTachesEnInstance(entreprise);
		final List<TacheEnvoiQuestionnaireSNC> tachesEnvoiExistantes = extractTachesEnvoiQuestionnaire(tachesEnInstance, periode.getAnnee());
		final List<TacheAnnulationQuestionnaireSNC> tachesAnnulationExistantes = extractTachesAnnulationQuestionnaire(tachesEnInstance, periode.getAnnee());

		if (hasVaudoisSurPeriode) {
			// il faut un questionnaire....
			final DateRange anneeCivile = new DateRangeHelper.Range(RegDate.get(periode.getAnnee(), 1, 1), RegDate.get(periode.getAnnee(), 12, 31));

			// si on a des tâches d'annulation, on les annule
			annuleTout(tachesAnnulationExistantes, new Collector<TacheAnnulationQuestionnaireSNC>() {
				@Override
				public void collect(TacheAnnulationQuestionnaireSNC tache) {
					rapport.addTraiteAnnulationTacheAnnulation(entreprise, tache);
				}
			});

			// si déjà un questionnaire, pas de nouvelle tâche
			if (questionnairesExistants.isEmpty()) {
				// si pas de tâche d'envoi existante, il faut la générer
				if (tachesEnvoiExistantes.isEmpty()) {
					final TacheEnvoiQuestionnaireSNC envoi = new TacheEnvoiQuestionnaireSNC(TypeEtatTache.EN_INSTANCE,
					                                                                        Tache.getDefaultEcheance(dateTraitement),
					                                                                        entreprise,
					                                                                        anneeCivile.getDateDebut(),
					                                                                        anneeCivile.getDateFin(),
					                                                                        CategorieEntreprise.SP,
					                                                                        oipm);
					final TacheEnvoiQuestionnaireSNC saved = (TacheEnvoiQuestionnaireSNC) tacheDAO.save(envoi);

					// nouvelle tâche générée
					rapport.addTraiteNouvelleTacheEnvoi(entreprise, saved);
				}
				else {

					// si plusieurs tâches d'envoi existantes, on les annule toutes sauf une
					final List<TacheEnvoiQuestionnaireSNC> aAnnuler = tachesEnvoiExistantes.subList(1, tachesEnvoiExistantes.size());
					annuleTout(aAnnuler, new Collector<TacheEnvoiQuestionnaireSNC>() {
						@Override
						public void collect(TacheEnvoiQuestionnaireSNC tache) {
							rapport.addTraiteAnnulationTacheEnvoi(entreprise, tache);
						}
					});
					aAnnuler.clear();

					// tâche déjà présente (éventuelle correction des dates de début/fin)
					final TacheEnvoiQuestionnaireSNC tacheExistante = tachesEnvoiExistantes.get(0);
					tacheExistante.setDateDebut(anneeCivile.getDateDebut());
					tacheExistante.setDateFin(anneeCivile.getDateFin());
					rapport.addIgnoreTacheEnvoiDejaPresente(entreprise);
				}
			}
			else if (tachesAnnulationExistantes.isEmpty()) {
				// questionnaire déjà présent... on ne touche à rien
				rapport.addIgnoreQuestionnaireDejaPresent(entreprise);
			}
		}
		else {
			// il ne faut pas de questionnaire...

			// si on a des tâches d'émission, on les annule
			annuleTout(tachesEnvoiExistantes, new Collector<TacheEnvoiQuestionnaireSNC>() {
				@Override
				public void collect(TacheEnvoiQuestionnaireSNC tache) {
					rapport.addTraiteAnnulationTacheEnvoi(entreprise, tache);
				}
			});

			// si on n'a pas de questionnaire, rien à faire...
			if (!questionnairesExistants.isEmpty()) {

				// trouvons les questionnaires sans tâche d'annulation associée et créons celles qui manquent
				final Map<Long, QuestionnaireSNC> questionnairesSansTache = new HashMap<>(questionnairesExistants.size());
				for (QuestionnaireSNC questionnaire : questionnairesExistants) {
					questionnairesSansTache.put(questionnaire.getId(), questionnaire);
				}

				// vers quoi pointent les taches d'annulation ?
				for (TacheAnnulationQuestionnaireSNC annulation : tachesAnnulationExistantes) {
					questionnairesSansTache.remove(annulation.getDeclaration().getId());
					rapport.addIgnoreTacheAnnulationDejaPresente(entreprise);
				}

				// ceux qui restent n'ont pas de tâche d'annulation, il faut donc les créer maintenant
				for (QuestionnaireSNC questionnaire : questionnairesSansTache.values()) {
					final TacheAnnulationQuestionnaireSNC annulation = new TacheAnnulationQuestionnaireSNC(TypeEtatTache.EN_INSTANCE,
					                                                                                       Tache.getDefaultEcheance(dateTraitement),
					                                                                                       entreprise,
					                                                                                       questionnaire,
					                                                                                       oipm);
					final TacheAnnulationQuestionnaireSNC saved = (TacheAnnulationQuestionnaireSNC) tacheDAO.save(annulation);

					// tâche d'annulation générée
					rapport.addTraiteNouvelleTacheAnnulation(entreprise, saved);
				}
			}
			else if (tachesEnvoiExistantes.isEmpty()) {
				// pas de questionnaire, pas de tâche nécessaire... rien à faire
				rapport.addIgnoreAucunQuestionnaireRequis(entreprise);
			}
		}
	}

	private List<Tache> getTachesEnInstance(Entreprise entreprise) {
		final TacheCriteria criteria = new TacheCriteria();
		criteria.setContribuable(entreprise);
		criteria.setEtatTache(TypeEtatTache.EN_INSTANCE);
		criteria.setInclureTachesAnnulees(false);
		return tacheDAO.find(criteria);
	}

	private static List<TacheEnvoiQuestionnaireSNC> extractTachesEnvoiQuestionnaire(List<Tache> enInstance, int pf) {
		final List<TacheEnvoiQuestionnaireSNC> envois = new ArrayList<>(enInstance.size());
		for (Tache tache : enInstance) {
			if (tache.getTypeTache() == TypeTache.TacheEnvoiQuestionnaireSNC && ((TacheEnvoiQuestionnaireSNC) tache).getDateFin().year() == pf) {
				envois.add((TacheEnvoiQuestionnaireSNC) tache);
			}
		}
		return envois;
	}

	private static List<TacheAnnulationQuestionnaireSNC> extractTachesAnnulationQuestionnaire(List<Tache> enInstance, int pf) {
		final List<TacheAnnulationQuestionnaireSNC> annulations = new ArrayList<>(enInstance.size());
		for (Tache tache : enInstance) {
			if (tache.getTypeTache() == TypeTache.TacheAnnulationQuestionnaireSNC && ((TacheAnnulationQuestionnaireSNC) tache).getDeclaration().getDateFin().year() == pf) {
				annulations.add((TacheAnnulationQuestionnaireSNC) tache);
			}
		}
		return annulations;
	}

	private interface Collector<T> {
		void collect(T element);
	}

	private static <T extends HibernateEntity> void annuleTout(Collection<T> aAnnuler, @Nullable Collector<? super T> collector) {
		for (T entity : aAnnuler) {
			entity.setAnnule(true);
			if (collector != null) {
				collector.collect(entity);
			}
		}
	}

	/**
	 * Récupération des identifiants des contribuables concernés par un éventuel questionnaire SNC
	 * @return une liste d'identifiants de contribuables entreprises
	 */
	protected List<Long> createListeIdsContribuables() {
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);
		return template.execute(new TransactionCallback<List<Long>>() {
			@Override
			public List<Long> doInTransaction(TransactionStatus status) {
				return hibernateTemplate.execute(new HibernateCallback<List<Long>>() {
					@Override
					public List<Long> doInHibernate(Session session) throws HibernateException, SQLException {
						final String hql = "select distinct ff.tiers.id from ForFiscalRevenuFortune as ff where ff.tiers.class in (Entreprise) and ff.annulationDate is null and ff.typeAutoriteFiscale = 'COMMUNE_OU_FRACTION_VD' and ff.genreImpot = 'REVENU_FORTUNE' order by ff.tiers.id";
						final Query query = session.createQuery(hql);
						//noinspection unchecked
						return query.list();
					}
				});
			}
		});
	}

	/**
	 * Vérification des paramètres du job
	 *
	 * @param anneePeriodeFiscale l'année de la période fiscale choisie
	 * @param dateTraitement      la date de traitement
	 * @throws DeclarationException en cas d'incohérence ou de valeur invalide
	 */
	private void checkParams(final int anneePeriodeFiscale, RegDate dateTraitement) throws DeclarationException {

		// laissons l'histoire aux vieilles applications
		final int premierePFautorisee = parametres.getPremierePeriodeFiscaleDeclarationsPersonnesMorales();
		if (anneePeriodeFiscale < premierePFautorisee) {
			throw new DeclarationException(String.format("La période fiscale %d est antérieure à la première période fiscale d'envoi de documents PM par Unireg [%d].",
			                                             anneePeriodeFiscale, premierePFautorisee));
		}

		// on ne peut lancer ce job qu'une fois l'année civile de la période fiscale échue
		if (anneePeriodeFiscale >= dateTraitement.year()) {
			throw new DeclarationException(String.format("La période fiscale %d n'est pas échue à la date de traitement [%s].",
			                                             anneePeriodeFiscale,
			                                             RegDateHelper.dateToDisplayString(dateTraitement)));
		}

		// et on vérifie quand-même que la période fiscale existe en base
		try {
			final TransactionTemplate template = new TransactionTemplate(transactionManager);
			template.setReadOnly(true);
			template.execute(new TransactionCallbackWithoutResult() {
				@Override
				protected void doInTransactionWithoutResult(TransactionStatus status) {
					// Récupère la période fiscale
					final PeriodeFiscale periode = periodeDAO.getPeriodeFiscaleByYear(anneePeriodeFiscale);
					if (periode == null) {
						throw new RuntimeException(String.format("La période fiscale %d n'existe pas dans la base de données.", anneePeriodeFiscale));
					}
				}
			});
		}
		catch (Exception e) {
			throw new DeclarationException(e);
		}
	}
}
