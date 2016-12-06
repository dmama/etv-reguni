package ch.vd.uniregctb.registrefoncier.dataimport;

import java.io.InputStream;
import java.util.Iterator;
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

import ch.vd.capitastra.grundstueck.Bodenbedeckung;
import ch.vd.capitastra.grundstueck.Gebaeude;
import ch.vd.capitastra.grundstueck.Grundstueck;
import ch.vd.capitastra.grundstueck.PersonEigentumAnteil;
import ch.vd.capitastra.grundstueck.Personstamm;
import ch.vd.registre.base.tx.TxCallback;
import ch.vd.registre.base.tx.TxCallbackWithoutResult;
import ch.vd.shared.batchtemplate.StatusManager;
import ch.vd.technical.esb.store.EsbStore;
import ch.vd.uniregctb.common.LengthConstants;
import ch.vd.uniregctb.common.LoggingStatusManager;
import ch.vd.uniregctb.common.ObjectNotFoundException;
import ch.vd.uniregctb.common.SubStatusManager;
import ch.vd.uniregctb.evenement.registrefoncier.EtatEvenementRF;
import ch.vd.uniregctb.evenement.registrefoncier.EvenementRFImport;
import ch.vd.uniregctb.evenement.registrefoncier.EvenementRFImportDAO;
import ch.vd.uniregctb.registrefoncier.RegistreFoncierService;
import ch.vd.uniregctb.registrefoncier.dataimport.detector.AyantDroitRFDetector;
import ch.vd.uniregctb.registrefoncier.dataimport.detector.BatimentRFDetector;
import ch.vd.uniregctb.registrefoncier.dataimport.detector.DroitRFDetector;
import ch.vd.uniregctb.registrefoncier.dataimport.detector.ImmeubleRFDetector;
import ch.vd.uniregctb.registrefoncier.dataimport.detector.SurfaceAuSolRFDetector;
import ch.vd.uniregctb.transaction.TransactionTemplate;
import ch.vd.uniregctb.xml.ExceptionHelper;

/**
 * Cette classe reçoit les données extraites de l'import du registre foncier, les compare avec les données en base et génère des événements de mutation correspondants.
 */
public class MutationsRFDetector {

	private static final Logger LOGGER = LoggerFactory.getLogger(MutationsRFDetector.class);

	private final RegistreFoncierService serviceRF;
	private final FichierImmeublesRFParser parser;
	private final EvenementRFImportDAO evenementRFImportDAO;
	private final PlatformTransactionManager transactionManager;
	private final EsbStore zipRaftStore;

	private final AyantDroitRFDetector ayantDroitRFDetector;
	private final BatimentRFDetector batimentRFDetector;
	private final DroitRFDetector droitRFDetector;
	private final ImmeubleRFDetector immeubleRFDetector;
	private final SurfaceAuSolRFDetector surfaceAuSolRFDetector;

	public MutationsRFDetector(RegistreFoncierService serviceRF,
	                           FichierImmeublesRFParser parser,
	                           EvenementRFImportDAO evenementRFImportDAO,
	                           PlatformTransactionManager transactionManager,
	                           EsbStore zipRaftStore,
	                           AyantDroitRFDetector ayantDroitRFDetector,
	                           BatimentRFDetector batimentRFDetector,
	                           DroitRFDetector droitRFDetector,
	                           ImmeubleRFDetector immeubleRFDetector,
	                           SurfaceAuSolRFDetector surfaceAuSolRFDetector) {
		this.serviceRF = serviceRF;
		this.parser = parser;
		this.evenementRFImportDAO = evenementRFImportDAO;
		this.transactionManager = transactionManager;
		this.zipRaftStore = zipRaftStore;
		this.ayantDroitRFDetector = ayantDroitRFDetector;
		this.batimentRFDetector = batimentRFDetector;
		this.droitRFDetector = droitRFDetector;
		this.immeubleRFDetector = immeubleRFDetector;
		this.surfaceAuSolRFDetector = surfaceAuSolRFDetector;
	}

	public MutationsRFDetectorResults run(long importId, int nbThreads, @Nullable StatusManager statusManager) {

		if (statusManager == null) {
			statusManager = new LoggingStatusManager(LOGGER);
		}

		// vérification de cohérence
		final EvenementRFImport event = getEvent(importId);
		if (event == null) {
			throw new ObjectNotFoundException("L'événement d'import RF avec l'id = [" + importId + "] n'existe pas.");
		}
		checkPreconditions(event);

		final MutationsRFDetectorResults rapport = new MutationsRFDetectorResults(importId, event.getDateEvenement(), nbThreads);

		// le job de traitement des imports ne supporte pas la reprise sur erreur (ou crash), on doit
		// donc effacer toutes les (éventuelles) mutations déjà générées lors d'un run précédent.
		deleteExistingMutations(importId, statusManager);

		// on peut maintenant processer l'import
		processImport(importId, event.getFileUrl(), nbThreads, statusManager);

		rapport.end();
		return rapport;
	}

	private void checkPreconditions(@NotNull EvenementRFImport event) {
		final long importId = event.getId();
		if (event.getEtat() != EtatEvenementRF.A_TRAITER && event.getEtat() != EtatEvenementRF.EN_ERREUR) {
			final IllegalArgumentException exception = new IllegalArgumentException("L'import RF avec l'id = [" + importId + "] a déjà été traité.");
			updateEvent(importId, EtatEvenementRF.EN_ERREUR, exception);
			throw exception;
		}
		final EvenementRFImport nextToProcess = getNextImportToProcess();
		if (!Objects.equals(importId, nextToProcess.getId())) {
			final IllegalArgumentException exception = new IllegalArgumentException("L'import RF avec l'id = [" + importId + "] doit être traité après l'import RF avec l'id = [" + nextToProcess.getId() + "].");
			updateEvent(importId, EtatEvenementRF.EN_ERREUR, exception);
			throw exception;
		}
		final Long unprocessedImport = findOldestImportWithUnprocessedMutations(importId);
		if (unprocessedImport != null) {
			final IllegalArgumentException exception = new IllegalArgumentException("L'import RF avec l'id = [" + importId + "] ne peut être traité car des mutations de l'import RF avec l'id = [" + unprocessedImport + "] n'ont pas été traitées.");
			updateEvent(importId, EtatEvenementRF.EN_ERREUR, exception);
			throw exception;
		}
	}

	private void processImport(long importId, String fileUrl, int nbThreads, @NotNull StatusManager statusManager) {
		try (InputStream is = zipRaftStore.get(fileUrl)) {

			statusManager.setMessage("Détection des mutations...");

			// Note : pour des raisons de performances, le parsing de l'import et la détection des mutations s'effectuent concurremment (en parallèle)
			final FichierImmeubleIteratorAdapter adapter = new FichierImmeubleIteratorAdapter();

			// on parse le fichier (dans un thread séparé)
			ExecutorCompletionService<Boolean> ecs = new ExecutorCompletionService<>(Executors.newFixedThreadPool(1));
			ecs.submit(() -> {
				parser.processFile(is, adapter);    // <-- émetteur des données
				return true;
			});

			// on détecte les changements et crée les mutations (en utilisant le parallèle batch transaction template)
			processImmeubles(importId, nbThreads, adapter.getImmeublesIterator(), new SubStatusManager(0, 20, statusManager));   // <-- consommateur des données
			processDroits(importId, nbThreads, adapter.getDroitsIterator(), new SubStatusManager(20, 40, statusManager));
			processProprietaires(importId, nbThreads, adapter.getProprietairesIterator(), new SubStatusManager(40, 60, statusManager));
			processBatiments(importId, nbThreads, adapter.getConstructionsIterator(), new SubStatusManager(60, 80, statusManager));
			processSurfaces(importId, nbThreads, adapter.getSurfacesIterator(), new SubStatusManager(80, 100, statusManager));

			// on attend que le parsing soit terminé
			ecs.take().get();

			statusManager.setMessage("Traitement terminé.");
			updateEvent(importId, EtatEvenementRF.TRAITE, null);
		}
		catch (Exception e) {
			LOGGER.warn("Erreur lors du processing de l'événement d'import RF avec l'id = [" + importId + "]", e);
			updateEvent(importId, EtatEvenementRF.EN_ERREUR, e);
			throw new RuntimeException(e);
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

	private void deleteExistingMutations(long importId, @NotNull StatusManager statusManager) {

		statusManager.setMessage("Effacement des mutations préexistantes...");

		serviceRF.deleteExistingMutations(importId);
	}

	private void updateEvent(final long eventId, @NotNull EtatEvenementRF etat, @Nullable Exception exception) {
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.execute(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final EvenementRFImport event = evenementRFImportDAO.get(eventId);
				if (event == null) {
					throw new ObjectNotFoundException("L'événement d'import RF avec l'id = [" + eventId + "] n'existe pas.");
				}

				event.setEtat(etat);
				if (exception == null) {
					event.setErrorMessage(null);
					event.setCallstack(null);
				}
				else {
					event.setErrorMessage(LengthConstants.streamlineField(ExceptionHelper.getMessage(exception), 1000, true));
					event.setCallstack(ExceptionUtils.getStackTrace(exception));
				}
			}
		});
	}

	public void processImmeubles(long importId, final int nbThreads, @NotNull Iterator<Grundstueck> iterator, @Nullable StatusManager statusManager) {
		immeubleRFDetector.processImmeubles(importId, nbThreads, iterator, statusManager);
	}

	public void processDroits(long importId, int nbThreads, Iterator<PersonEigentumAnteil> iterator, @Nullable StatusManager statusManager) {
		droitRFDetector.processDroits(importId, nbThreads, iterator, statusManager);
	}

	public void processProprietaires(long importId, int nbThreads, Iterator<Personstamm> iterator, @Nullable StatusManager statusManager) {
		ayantDroitRFDetector.processProprietaires(importId, nbThreads, iterator, statusManager);
	}

	public void processBatiments(long importId, int nbThreads, Iterator<Gebaeude> iterator, @Nullable StatusManager statusManager) {
		batimentRFDetector.processBatiments(importId, nbThreads, iterator, statusManager);
	}

	public void processSurfaces(long importId, int nbThreads, Iterator<Bodenbedeckung> iterator, @Nullable StatusManager statusManager) {
		surfaceAuSolRFDetector.processSurfaces(importId, nbThreads, iterator, statusManager);
	}
}
