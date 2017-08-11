package ch.vd.uniregctb.validation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.document.ValidationJobRapport;
import ch.vd.uniregctb.hibernate.HibernateTemplate;
import ch.vd.uniregctb.metier.assujettissement.PeriodeImpositionService;
import ch.vd.uniregctb.parametrage.ParametreAppService;
import ch.vd.uniregctb.rapport.RapportService;
import ch.vd.uniregctb.scheduler.JobCategory;
import ch.vd.uniregctb.scheduler.JobDefinition;
import ch.vd.uniregctb.scheduler.JobParam;
import ch.vd.uniregctb.scheduler.JobParamBoolean;
import ch.vd.uniregctb.scheduler.JobParamInteger;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.TiersService;

/**
 * Job qui permet de tester la cohérence des données d'un point de vue Unireg.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class ValidationJob extends JobDefinition {

	private final Logger LOGGER = LoggerFactory.getLogger(ValidationJob.class);

	public static final String NAME = "ValidationJob";

	public static final String P_IMPOSITION = "P_IMPOSITION";
	public static final String ADRESSES = "ADRESSES";
	public static final String MODE_STRICT = "MODE_STRICT";
	public static final String DI = "DI";
	public static final String NB_THREADS = "NB_THREADS";

	private static final int QUEUE_BY_THREAD_SIZE = 50;

	private TiersDAO tiersDAO;
	private HibernateTemplate hibernateTemplate;
	private PlatformTransactionManager transactionManager;
	private RapportService rapportService;
	private AdresseService adresseService;
	private ParametreAppService paramService;
	private ValidationService validationService;
	private PeriodeImpositionService periodeImpositionService;
	private TiersService tiersService;

	public ValidationJob(int sortOrder, String description) {
		super(NAME, JobCategory.STATS, sortOrder, description);

		final JobParam param0 = new JobParam();
		param0.setDescription("Calcul les périodes d'imposition");
		param0.setName(P_IMPOSITION);
		param0.setMandatory(false);
		param0.setType(new JobParamBoolean());
		addParameterDefinition(param0, Boolean.FALSE);

		final JobParam param1 = new JobParam();
		param1.setDescription("Cohérence date DI / p. imposition");
		param1.setName(DI);
		param1.setMandatory(false);
		param1.setType(new JobParamBoolean());
		addParameterDefinition(param1, Boolean.FALSE);

		final JobParam param2 = new JobParam();
		param2.setDescription("Calcul les adresses");
		param2.setName(ADRESSES);
		param2.setMandatory(false);
		param2.setType(new JobParamBoolean());
		addParameterDefinition(param2, Boolean.FALSE);

		final JobParam param3 = new JobParam();
		param3.setDescription("Nombre de threads");
		param3.setName(NB_THREADS);
		param3.setMandatory(true);
		param3.setType(new JobParamInteger());
		addParameterDefinition(param3, 4);

		final JobParam param4 = new JobParam();
		param4.setDescription("Mode strict");
		param4.setName(MODE_STRICT);
		param4.setMandatory(false);
		param4.setType(new JobParamBoolean());
		addParameterDefinition(param4, Boolean.TRUE);
	}

	public void setTiersDAO(TiersDAO tiersDAO) {
		this.tiersDAO = tiersDAO;
	}

	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	public void setRapportService(RapportService rapportService) {
		this.rapportService = rapportService;
	}

	public void setAdresseService(AdresseService adresseService) {
		this.adresseService = adresseService;
	}

	public void setParamService(ParametreAppService paramService) {
		this.paramService = paramService;
	}

	public void setValidationService(ValidationService validationService) {
		this.validationService = validationService;
	}

	public void setPeriodeImpositionService(PeriodeImpositionService periodeImpositionService) {
		this.periodeImpositionService = periodeImpositionService;
	}

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	public void setHibernateTemplate(HibernateTemplate hibernateTemplate) {
		this.hibernateTemplate = hibernateTemplate;
	}

	@Override
	protected void doExecute(Map<String, Object> params) throws Exception {

		final StatusManager statusManager = getStatusManager();

		final boolean calculatePeriodesImposition = getBooleanValue(params, P_IMPOSITION);
		final boolean coherencePeriodesImpositionWrtDIs = getBooleanValue(params, DI);
		final boolean calculateAdresses = getBooleanValue(params, ADRESSES);
		final int nbThreads = getStrictlyPositiveIntegerValue(params, NB_THREADS);
		final boolean modeStrict = getBooleanValue(params, MODE_STRICT);

		// Chargement des ids des tiers à processer
		statusManager.setMessage("Chargement des ids de tous les tiers...");
		final List<Long> ids = getTiersIds(statusManager);

		// Processing des tiers
		final ValidationJobResults results = new ValidationJobResults(RegDate.get(), calculatePeriodesImposition, coherencePeriodesImpositionWrtDIs, calculateAdresses, modeStrict, tiersService,
				adresseService);
		processAll(ids, results, nbThreads, statusManager);
		results.end();

		// Génération du rapport
		final ValidationJobRapport rapport = generateRapport(results, statusManager);

		setLastRunReport(rapport);
		Audit.success("Le batch de validation des tiers est terminé", rapport);
	}

	private List<Long> getTiersIds(final StatusManager statusManager) {

		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);

		final List<Long> ids = template.execute(new TransactionCallback<List<Long>>() {
			@Override
			public List<Long> doInTransaction(TransactionStatus status) {
				//noinspection unchecked
				return hibernateTemplate.find("select t.numero from Tiers t order by t.numero asc", null);
			}
		});

		statusManager.setMessage(String.format("%d tiers trouvés", ids.size()));
		return ids;
	}

	private void processAll(final List<Long> ids, final ValidationJobResults results, int nbThreads, final StatusManager statusManager)
			throws InterruptedException {

		final ArrayBlockingQueue<Long> queue = new ArrayBlockingQueue<>(QUEUE_BY_THREAD_SIZE * nbThreads);

		// Création des threads de processing
		final List<ValidationJobThread> threads = new ArrayList<>(nbThreads);
		for (int i = 0; i < nbThreads; i++) {
			final ValidationJobThread t = new ValidationJobThread(queue, results, tiersDAO, transactionManager, adresseService, paramService, validationService, periodeImpositionService);
			threads.add(t);
			t.setName("ValidThread-" + i);
			t.start();
		}

		// variables pour le log
		int i = 0;

		// Dispatching des tiers à processer
		for (Long id : ids) {
			if (statusManager.isInterrupted()) {
				results.interrompu = true;
				queue.clear();
				break;
			}

			if (++i % 100 == 0) {
				int percent = (i * 100) / ids.size();
				String message = String.format(
						"Processing du tiers %d => invalides(%d) / p.imposition(%d) / coherence(%d) / adresses(%d) / total(%d)", id,
						results.getNbErreursValidation(), results.getNbErreursPeriodesImposition(), results.getNbErreursCoherenceDI(),
						results.getNbErreursAdresses(), results.getNbTiersTotal());
				statusManager.setMessage(message, percent);
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug(message);
				}
			}

			/*
			 * insère l'id dans la queue à processer, mais de manière à pouvoir interrompre le processus si plus personne ne prélève d'ids
			 * dans la queue (p.a. si tous les threads de processing sont morts).
			 */
			while (!queue.offer(id, 10, TimeUnit.SECONDS) && !statusManager.isInterrupted()) {
				if (LOGGER.isDebugEnabled()) {
					LOGGER.warn("La queue de validation est pleine, attente de 10 secondes...");
				}
			}
		}

		// Signale aux threads de validation que tous les ids sont postés dans la queue
		// et qu'ils peuvent s'arrêter lorsque celle-ci est vide
		for (ValidationJobThread thread : threads) {
			thread.stopIfInputQueueEmpty();
		}

		// On attend que les threads de validation se terminent d'eux-mêmes
		for (ValidationJobThread thread : threads) {
			thread.join();
		}
	}

	private ValidationJobRapport generateRapport(final ValidationJobResults results, final StatusManager statusManager) {
		statusManager.setMessage("Génération du rapport...");
		final TransactionTemplate t = new TransactionTemplate(transactionManager);
		final ValidationJobRapport rapport = t.execute(new TransactionCallback<ValidationJobRapport>() {
			@Override
			public ValidationJobRapport doInTransaction(TransactionStatus s) {
				return rapportService.generateRapport(results, statusManager);
			}
		});
		return rapport;
	}

	@Override
	protected boolean isWebStartableInProductionMode() {
		return true;
	}
}
