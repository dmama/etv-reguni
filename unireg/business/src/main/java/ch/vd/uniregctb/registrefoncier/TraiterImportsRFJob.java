package ch.vd.uniregctb.registrefoncier;

import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Executors;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.registre.base.tx.TxCallback;
import ch.vd.registre.base.tx.TxCallbackWithoutResult;
import ch.vd.shared.batchtemplate.StatusManager;
import ch.vd.technical.esb.store.EsbStore;
import ch.vd.uniregctb.common.ObjectNotFoundException;
import ch.vd.uniregctb.common.SubStatusManager;
import ch.vd.uniregctb.evenement.registrefoncier.EtatEvenementRF;
import ch.vd.uniregctb.evenement.registrefoncier.EvenementRFImport;
import ch.vd.uniregctb.evenement.registrefoncier.EvenementRFImportDAO;
import ch.vd.uniregctb.evenement.registrefoncier.EvenementRFMutationDAO;
import ch.vd.uniregctb.registrefoncier.dao.ImmeubleRFDAO;
import ch.vd.uniregctb.registrefoncier.elements.XmlHelperRF;
import ch.vd.uniregctb.registrefoncier.processor.ImmeubleRFProcessor;
import ch.vd.uniregctb.scheduler.JobCategory;
import ch.vd.uniregctb.scheduler.JobDefinition;
import ch.vd.uniregctb.scheduler.JobParam;
import ch.vd.uniregctb.scheduler.JobParamInteger;
import ch.vd.uniregctb.scheduler.JobParamLong;

/**
 * Job de traitement d'un import des immeubles du registre foncier
 */
public class TraiterImportsRFJob extends JobDefinition {

	private static final Logger LOGGER = LoggerFactory.getLogger(TraiterImportsRFJob.class);

	public static final String NAME = "TraiterImportsRFJob";
	public static final String ID = "eventId";
	public static final String NB_THREADS = "NB_THREADS";

	private XmlHelperRF xmlHelperRF;
	private FichierImmeublesRFParser parser;
	private ImmeubleRFDAO immeubleRFDAO;
	private EvenementRFImportDAO evenementRFImportDAO;
	private EvenementRFMutationDAO evenementRFMutationDAO;

	private PlatformTransactionManager transactionManager;
	private EsbStore zipRaftStore;

	public TraiterImportsRFJob(String name, JobCategory categorie, int sortOrder, String description) {
		super(name, categorie, sortOrder, description);
	}

	public void setXmlHelperRF(XmlHelperRF xmlHelperRF) {
		this.xmlHelperRF = xmlHelperRF;
	}

	public void setParser(FichierImmeublesRFParser parser) {
		this.parser = parser;
	}

	public void setImmeubleRFDAO(ImmeubleRFDAO immeubleRFDAO) {
		this.immeubleRFDAO = immeubleRFDAO;
	}

	public void setEvenementRFImportDAO(EvenementRFImportDAO evenementRFImportDAO) {
		this.evenementRFImportDAO = evenementRFImportDAO;
	}

	public void setEvenementRFMutationDAO(EvenementRFMutationDAO evenementRFMutationDAO) {
		this.evenementRFMutationDAO = evenementRFMutationDAO;
	}

	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	public void setZipRaftStore(EsbStore zipRaftStore) {
		this.zipRaftStore = zipRaftStore;
	}

	public TraiterImportsRFJob(int sortOrder, String description) {
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
	}

	@Override
	protected void doExecute(Map<String, Object> params) throws Exception {

		final long importId = getLongValue(params, ID);
		final int nbThreads = getStrictlyPositiveIntegerValue(params, NB_THREADS);
		final EvenementRFImport event = getEvent(importId);
		if (event == null) {
			throw new ObjectNotFoundException("L'événement d'import RF avec l'id = [" + importId + "] n'existe pas.");
		}

		try (InputStream is = zipRaftStore.get(event.getFileUrl())) {

			final StatusManager statusManager = getStatusManager();

			statusManager.setMessage("Détection des mutations...");
			final DataRFCallbackAdapter dataAdapter = new DataRFCallbackAdapter();

			// Note : pour des raisons de performances, le parsing de l'import et la détection des mutations s'effectuent concurremment (en parallèle)

			// on parse le fichier (dans un thread séparé)
			ExecutorCompletionService<Boolean> ecs = new ExecutorCompletionService<>(Executors.newFixedThreadPool(1));
			ecs.submit(() -> {
				parser.processFile(is, dataAdapter, new SubStatusManager(0, 50, statusManager));    // <-- émetteur des données
				return true;
			});

			// on détecte les changements et crée les mutations (en utilisant le parallèle batch transaction template)
			final DataRFMutationsDetector mutationsDetector = new DataRFMutationsDetector(xmlHelperRF, immeubleRFDAO, evenementRFImportDAO, evenementRFMutationDAO, transactionManager);
			mutationsDetector.processImmeubles(importId, nbThreads, dataAdapter.getImmeublesIterator());   // <-- consommateur des données
			mutationsDetector.processDroits(importId, dataAdapter.getDroitsIterator());
			mutationsDetector.processProprietaires(importId, dataAdapter.getProprietairesIterator());
			mutationsDetector.processConstructions(importId, dataAdapter.getConstructionsIterator());
			mutationsDetector.processSurfaces(importId, dataAdapter.getSurfacesIterator());

			// on attend que le parsing soit terminé
			ecs.take().get();

			// on traite les mutations
			statusManager.setMessage("Traitement des mutations...");
			final ImmeubleRFProcessor immeubleRFProcessor = new ImmeubleRFProcessor(immeubleRFDAO, xmlHelperRF);
			final DataRFMutationsProcessor processor = new DataRFMutationsProcessor(evenementRFMutationDAO, immeubleRFProcessor, transactionManager);
			processor.processImport(importId, nbThreads, new SubStatusManager(50, 100, statusManager));

			statusManager.setMessage("Traitement terminé.");
			updateEvent(importId, EtatEvenementRF.TRAITE, null);
		}
		catch (Exception e) {
			LOGGER.warn("Erreur lors du processing de l'événement d'import RF avec l'id = [" + importId + "]", e);
			updateEvent(importId, EtatEvenementRF.EN_ERREUR, ExceptionUtils.getStackTrace(e));
		}

	}

	@Nullable
	private EvenementRFImport getEvent(final long eventId) {
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		return template.execute(new TxCallback<EvenementRFImport>() {
			@Override
			public EvenementRFImport execute(TransactionStatus status) throws Exception {
				return evenementRFImportDAO.get(eventId);
			}
		});
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
