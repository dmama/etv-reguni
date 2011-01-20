package ch.vd.uniregctb.validation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.document.ValidationJobRapport;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.parametrage.ParametreAppService;
import ch.vd.uniregctb.rapport.RapportService;
import ch.vd.uniregctb.scheduler.JobDefinition;
import ch.vd.uniregctb.scheduler.JobParam;
import ch.vd.uniregctb.scheduler.JobParamBoolean;
import ch.vd.uniregctb.scheduler.JobParamInteger;
import ch.vd.uniregctb.tiers.TiersDAO;

/**
 * Job qui permet de tester la cohérence des données d'un point de vue Unireg.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class ValidationJob extends JobDefinition {

	private final Logger LOGGER = Logger.getLogger(ValidationJob.class);

	public static final String NAME = "ValidationJob";
	private static final String CATEGORIE = "Stats";

	public static final String ASSUJET = "ASSUJET";
	public static final String ADRESSES = "ADRESSES";
	public static final String DI = "DI";
	public static final String NB_THREADS = "NB_THREADS";
	public static final String AUTORITE_FORS = "AUTORITE_FORS";

	private static final int QUEUE_BY_THREAD_SIZE = 50;

	private TiersDAO tiersDAO;
	private PlatformTransactionManager transactionManager;
	private RapportService rapportService;
	private AdresseService adresseService;
	private ServiceInfrastructureService serviceInfra;
	private ParametreAppService paramService;
	private ValidationService validationService;

	public ValidationJob(int sortOrder, String description) {
		super(NAME, CATEGORIE, sortOrder, description);

		final JobParam param0 = new JobParam();
		param0.setDescription("Calcul les assujettissements");
		param0.setName(ASSUJET);
		param0.setMandatory(false);
		param0.setType(new JobParamBoolean());
		addParameterDefinition(param0, Boolean.FALSE);

		final JobParam param1 = new JobParam();
		param1.setDescription("Cohérence date DI / assujettissement");
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
		param3.setDescription("Cohérences autorités des fors fiscaux");
		param3.setName(AUTORITE_FORS);
		param3.setMandatory(false);
		param3.setType(new JobParamBoolean());
		addParameterDefinition(param3, Boolean.FALSE);

		final JobParam param4 = new JobParam();
		param4.setDescription("Nombre de threads");
		param4.setName(NB_THREADS);
		param4.setMandatory(true);
		param4.setType(new JobParamInteger());
		addParameterDefinition(param4, 4);
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

	public void setServiceInfra(ServiceInfrastructureService serviceInfra) {
		this.serviceInfra = serviceInfra;
	}

	public void setParamService(ParametreAppService paramService) {
		this.paramService = paramService;
	}

	public void setValidationService(ValidationService validationService) {
		this.validationService = validationService;
	}

	@Override
	protected void doExecute(Map<String, Object> params) throws Exception {

		final StatusManager statusManager = getStatusManager();

		final boolean calculateAssujettissements = getBooleanValue(params, ASSUJET);
		final boolean coherenceAssujetDi = getBooleanValue(params, DI);
		final boolean calculateAdresses = getBooleanValue(params, ADRESSES);
		final boolean coherenceAutoritesForsFiscaux = getBooleanValue(params, AUTORITE_FORS);
		final int nbThreads = getStrictlyPositiveIntegerValue(params, NB_THREADS);

		// Chargement des ids des contribuables à processer
		statusManager.setMessage("Chargement des ids de tous les contribuables...");
		final List<Long> ids = getCtbIds(statusManager);

		// Processing des contribuables
		final ValidationJobResults results = new ValidationJobResults(RegDate.get(), calculateAssujettissements, coherenceAssujetDi,
				calculateAdresses, coherenceAutoritesForsFiscaux);
		processAll(ids, results, nbThreads, statusManager);
		results.end();

		// Génération du rapport
		final ValidationJobRapport rapport = generateRapport(results, statusManager);

		setLastRunReport(rapport);
		Audit.success("Le batch de validation des tiers est terminé", rapport);
	}

	@SuppressWarnings("unchecked")
	private List<Long> getCtbIds(final StatusManager statusManager) {

		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);

		final List<Long> ids = (List<Long>) template.execute(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				return tiersDAO.getHibernateTemplate()
						.find("select cont.numero from Contribuable as cont order by cont.numero asc");
			}
		});

		statusManager.setMessage(String.format("%d contribuables trouvés", ids.size()));
		return ids;
	}

	private void processAll(final List<Long> ids, final ValidationJobResults results, int nbThreads, final StatusManager statusManager)
			throws InterruptedException {

		final ArrayBlockingQueue<Long> queue = new ArrayBlockingQueue<Long>(QUEUE_BY_THREAD_SIZE * nbThreads);

		// Création des threads de processing
		final List<ValidationJobThread> threads = new ArrayList<ValidationJobThread>(nbThreads);
		for (int i = 0; i < nbThreads; i++) {
			final ValidationJobThread t = new ValidationJobThread(queue, results, tiersDAO, transactionManager, adresseService, serviceInfra, paramService, validationService);
			threads.add(t);
			t.setName("ValidThread-" + i);
			t.start();
		}

		// variables pour le log
		int i = 0;

		// Dispatching des contribuables à processer
		for (Long id : ids) {
			if (statusManager.interrupted()) {
				results.interrompu = true;
				queue.clear();
				break;
			}

			if (++i % 100 == 0) {
				int percent = (i * 100) / ids.size();
				String message = String.format(
						"Processing du contribuable %d => invalides(%d) / assujet.(%d) / coherence(%d) / adresses(%d) / aut.fisc.(%d) / total(%d)", id,
						results.getNbErreursValidation(), results.getNbErreursAssujettissement(), results.getNbErreursCoherenceDI(),
						results.getNbErreursAdresses(), results.getNbErreursAutoritesFiscales(), results.getNbCtbsTotal());
				statusManager.setMessage(message, percent);
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug(message);
				}
			}

			/*
			 * insère l'id dans la queue à processer, mais de manière à pouvoir interrompre le processus si plus personne ne prélève d'ids
			 * dans la queue (p.a. si tous les threads de processing sont morts).
			 */
			while (!queue.offer(id, 10, TimeUnit.SECONDS) && !statusManager.interrupted()) {
				if (LOGGER.isDebugEnabled()) {
					LOGGER.warn("La queue de validation est pleine, attente de 10 secondes...");
				}
			}
		}

		// Arrêt des threads
		for (ValidationJobThread thread : threads) {
			thread.interrupt();
		}
	}

	private ValidationJobRapport generateRapport(final ValidationJobResults results, final StatusManager statusManager) {
		statusManager.setMessage("Génération du rapport...");
		final TransactionTemplate t = new TransactionTemplate(transactionManager);
		final ValidationJobRapport rapport = (ValidationJobRapport) t.execute(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus s) {
				return rapportService.generateRapport(results, statusManager);
			}
		});
		return rapport;
	}
}
