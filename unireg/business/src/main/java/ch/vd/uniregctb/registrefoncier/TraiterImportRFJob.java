package ch.vd.uniregctb.registrefoncier;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Executors;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;

import ch.vd.registre.base.tx.TxCallback;
import ch.vd.registre.base.tx.TxCallbackWithoutResult;
import ch.vd.shared.batchtemplate.StatusManager;
import ch.vd.technical.esb.store.EsbStore;
import ch.vd.uniregctb.common.ObjectNotFoundException;
import ch.vd.uniregctb.common.SubStatusManager;
import ch.vd.uniregctb.evenement.registrefoncier.EtatEvenementRF;
import ch.vd.uniregctb.evenement.registrefoncier.EvenementRFImport;
import ch.vd.uniregctb.evenement.registrefoncier.EvenementRFImportDAO;
import ch.vd.uniregctb.scheduler.JobCategory;
import ch.vd.uniregctb.scheduler.JobDefinition;
import ch.vd.uniregctb.scheduler.JobParam;
import ch.vd.uniregctb.scheduler.JobParamBoolean;
import ch.vd.uniregctb.scheduler.JobParamInteger;
import ch.vd.uniregctb.scheduler.JobParamLong;
import ch.vd.uniregctb.transaction.TransactionTemplate;

/**
 * Job de traitement d'un import des immeubles du registre foncier
 */
public class TraiterImportRFJob extends JobDefinition {

	private static final Logger LOGGER = LoggerFactory.getLogger(TraiterImportRFJob.class);

	public static final String NAME = "TraiterImportRFJob";
	public static final String ID = "eventId";
	public static final String NB_THREADS = "NB_THREADS";
	public static final String CONTINUE_WITH_MUTATIONS_JOB = "CONTINUE_WITH_MUTATIONS_JOB";

	private RegistreFoncierService serviceRF;
	private FichierImmeublesRFParser parser;
	private EvenementRFImportDAO evenementRFImportDAO;
	private PlatformTransactionManager transactionManager;
	private EsbStore zipRaftStore;
	private DataRFMutationsDetector mutationsDetector;

	public void setServiceRF(RegistreFoncierService serviceRF) {
		this.serviceRF = serviceRF;
	}

	public void setParser(FichierImmeublesRFParser parser) {
		this.parser = parser;
	}

	public void setEvenementRFImportDAO(EvenementRFImportDAO evenementRFImportDAO) {
		this.evenementRFImportDAO = evenementRFImportDAO;
	}

	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	public void setMutationsDetector(DataRFMutationsDetector mutationsDetector) {
		this.mutationsDetector = mutationsDetector;
	}

	public void setZipRaftStore(EsbStore zipRaftStore) {
		this.zipRaftStore = zipRaftStore;
	}

	public TraiterImportRFJob(int sortOrder, String description) {
		super(NAME, JobCategory.RF, sortOrder, description);

		final JobParam param1 = new JobParam();
		param1.setDescription("Id de l'événement (EvenementRFImport)");
		param1.setName(ID);
		param1.setMandatory(true);
		param1.setType(new JobParamLong());
		addParameterDefinition(param1, null);

		final JobParam param2 = new JobParam();
		param2.setDescription("Nombre de threads");
		param2.setName(NB_THREADS);
		param2.setMandatory(true);
		param2.setType(new JobParamInteger());
		addParameterDefinition(param2, 8);

		final JobParam param3 = new JobParam();
		param3.setDescription("Continuer avec le job de traitement des mutations");
		param3.setName(CONTINUE_WITH_MUTATIONS_JOB);
		param3.setMandatory(true);
		param3.setType(new JobParamBoolean());
		addParameterDefinition(param3, true);
	}

	@Override
	protected void doExecute(Map<String, Object> params) throws Exception {

		final long importId = getLongValue(params, ID);
		final int nbThreads = getStrictlyPositiveIntegerValue(params, NB_THREADS);
		final boolean startMutationJob = getBooleanValue(params, CONTINUE_WITH_MUTATIONS_JOB);

		// vérification de cohérence
		final EvenementRFImport event = getEvent(importId);
		if (event == null) {
			throw new ObjectNotFoundException("L'événement d'import RF avec l'id = [" + importId + "] n'existe pas.");
		}
		checkPreconditions(event);

		// le job de traitement des imports ne supporte pas la reprise sur erreur (ou crash), on doit
		// donc effacer toutes les (éventuelles) mutations déjà générées lors d'un run précédent.
		deleteExistingMutations(importId);

		// on peut maintenant processer l'import
		processImport(importId, event.getFileUrl(), nbThreads, startMutationJob);
	}

	private void checkPreconditions(@NotNull EvenementRFImport event) {
		final long importId = event.getId();
		if (event.getEtat() != EtatEvenementRF.A_TRAITER && event.getEtat() != EtatEvenementRF.EN_ERREUR) {
			final IllegalArgumentException exception = new IllegalArgumentException("L'import RF avec l'id = [" + importId + "] a déjà été traité.");
			updateEvent(importId, EtatEvenementRF.EN_ERREUR, ExceptionUtils.getStackTrace(exception));
			throw exception;
		}
		final EvenementRFImport nextToProcess = getNextImportToProcess();
		if (!Objects.equals(importId, nextToProcess.getId())) {
			final IllegalArgumentException exception = new IllegalArgumentException("L'import RF avec l'id = [" + importId + "] doit être traité après l'import RF avec l'id = [" + nextToProcess.getId() + "].");
			updateEvent(importId, EtatEvenementRF.EN_ERREUR, ExceptionUtils.getStackTrace(exception));
			throw exception;
		}
		final Long unprocessedImport = findOldestImportWithUnprocessedMutations(importId);
		if (unprocessedImport != null) {
			final IllegalArgumentException exception = new IllegalArgumentException("L'import RF avec l'id = [" + importId + "] ne peut être traité car des mutations de l'import RF avec l'id = [" + unprocessedImport + "] n'ont pas été traitées.");
			updateEvent(importId, EtatEvenementRF.EN_ERREUR, ExceptionUtils.getStackTrace(exception));
			throw exception;
		}
	}

	private void processImport(long importId, String fileUrl, int nbThreads, boolean startMutationJob) {
		try (InputStream is = zipRaftStore.get(fileUrl)) {

			final StatusManager statusManager = getStatusManager();
			statusManager.setMessage("Détection des mutations...");

			// Note : pour des raisons de performances, le parsing de l'import et la détection des mutations s'effectuent concurremment (en parallèle)
			final DataRFCallbackAdapter dataAdapter = new DataRFCallbackAdapter();

			// on parse le fichier (dans un thread séparé)
			ExecutorCompletionService<Boolean> ecs = new ExecutorCompletionService<>(Executors.newFixedThreadPool(1));
			ecs.submit(() -> {
				parser.processFile(is, dataAdapter);    // <-- émetteur des données
				return true;
			});

			// on détecte les changements et crée les mutations (en utilisant le parallèle batch transaction template)
			mutationsDetector.processImmeubles(importId, nbThreads, dataAdapter.getImmeublesIterator(), new SubStatusManager(0, 20, statusManager));   // <-- consommateur des données
			mutationsDetector.processDroits(importId, nbThreads, dataAdapter.getDroitsIterator(), new SubStatusManager(20, 40, statusManager));
			mutationsDetector.processProprietaires(importId, nbThreads, dataAdapter.getProprietairesIterator(), new SubStatusManager(40, 60, statusManager));
			mutationsDetector.processConstructions(importId, dataAdapter.getConstructionsIterator(), new SubStatusManager(60, 80, statusManager));
			mutationsDetector.processSurfaces(importId, nbThreads, dataAdapter.getSurfacesIterator(), new SubStatusManager(80, 100, statusManager));

			// on attend que le parsing soit terminé
			ecs.take().get();

			statusManager.setMessage("Traitement terminé.");
			updateEvent(importId, EtatEvenementRF.TRAITE, null);

			// si demandé, on démarre le job de traitement des mutations
			if (startMutationJob) {
				final Map<String, Object> mutParams = new HashMap<>();
				mutParams.put(TraiterMutationsRFJob.ID, importId);
				mutParams.put(TraiterMutationsRFJob.NB_THREADS, nbThreads);
				batchScheduler.startJob(TraiterMutationsRFJob.NAME, mutParams);
			}
		}
		catch (Exception e) {
			LOGGER.warn("Erreur lors du processing de l'événement d'import RF avec l'id = [" + importId + "]", e);
			updateEvent(importId, EtatEvenementRF.EN_ERREUR, ExceptionUtils.getStackTrace(e));
		}
	}

	@Nullable
	private EvenementRFImport getEvent(final long eventId) {
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);
		return template.execute(new TxCallback<EvenementRFImport>() {
			@Override
			public EvenementRFImport execute(TransactionStatus status) throws Exception {
				return evenementRFImportDAO.get(eventId);
			}
		});
	}

	/**
	 * @return le prochain import qui doit être processé en respectant les états et les dates chronologiques d'import.
	 */
	@NotNull
	private EvenementRFImport getNextImportToProcess() {
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);
		return template.execute(new TxCallback<EvenementRFImport>() {
			@Override
			public EvenementRFImport execute(TransactionStatus status) throws Exception {
				final EvenementRFImport next = evenementRFImportDAO.findNextImportToProcess();
				if (next == null) {
					throw new IllegalArgumentException("Il n'y a pas de prochain rapport à processer.");
				}
				return next;
			}
		});
	}

	/**
	 * @param importId l'id de l'import courant
	 * @return retourne l'id de l'import le plus anciens qui possède encore des mutations à traiter (A_TRAITER ou EN_ERREUR)
	 */
	@Nullable
	private Long findOldestImportWithUnprocessedMutations(long importId) {
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);
		return template.execute(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final EvenementRFImport previous = evenementRFImportDAO.findOldestImportWithUnprocessedMutations(importId);
				return previous == null ? null : previous.getId();
			}
		});
	}

	private void deleteExistingMutations(long importId) {

		final StatusManager statusManager = getStatusManager();
		statusManager.setMessage("Effacement des mutations préexistantes...");

		serviceRF.deleteExistingMutations(importId);
	}

	private void updateEvent(final long eventId, @NotNull EtatEvenementRF etat, @Nullable String errorMessage) {
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.execute(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final EvenementRFImport event = evenementRFImportDAO.get(eventId);
				if (event == null) {
					throw new ObjectNotFoundException("L'événement d'import RF avec l'id = [" + eventId + "] n'existe pas.");
				}

				event.setEtat(etat);
				event.setErrorMessage(errorMessage);
			}
		});
	}
}
