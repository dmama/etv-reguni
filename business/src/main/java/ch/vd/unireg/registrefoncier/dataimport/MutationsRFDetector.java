package ch.vd.unireg.registrefoncier.dataimport;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.capitastra.grundstueck.Bodenbedeckung;
import ch.vd.capitastra.grundstueck.EigentumAnteil;
import ch.vd.capitastra.grundstueck.Gebaeude;
import ch.vd.capitastra.grundstueck.Grundstueck;
import ch.vd.capitastra.grundstueck.Personstamm;
import ch.vd.capitastra.rechteregister.Dienstbarkeit;
import ch.vd.capitastra.rechteregister.LastRechtGruppe;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.tx.TxCallback;
import ch.vd.registre.base.tx.TxCallbackWithoutResult;
import ch.vd.technical.esb.store.EsbStore;
import ch.vd.unireg.common.DefaultThreadFactory;
import ch.vd.unireg.common.DefaultThreadNameGenerator;
import ch.vd.unireg.common.LengthConstants;
import ch.vd.unireg.common.LoggingStatusManager;
import ch.vd.unireg.common.ObjectNotFoundException;
import ch.vd.unireg.common.StatusManager;
import ch.vd.unireg.common.SubStatusManager;
import ch.vd.unireg.evenement.registrefoncier.EtatEvenementRF;
import ch.vd.unireg.evenement.registrefoncier.EvenementRFImport;
import ch.vd.unireg.evenement.registrefoncier.EvenementRFImportDAO;
import ch.vd.unireg.evenement.registrefoncier.EvenementRFMutationDAO;
import ch.vd.unireg.evenement.registrefoncier.TypeImportRF;
import ch.vd.unireg.registrefoncier.ImmeubleRF;
import ch.vd.unireg.registrefoncier.RegistreFoncierImportService;
import ch.vd.unireg.registrefoncier.UsufruitRF;
import ch.vd.unireg.registrefoncier.dataimport.detector.AyantDroitRFDetector;
import ch.vd.unireg.registrefoncier.dataimport.detector.BatimentRFDetector;
import ch.vd.unireg.registrefoncier.dataimport.detector.DroitRFDetector;
import ch.vd.unireg.registrefoncier.dataimport.detector.ImmeubleRFDetector;
import ch.vd.unireg.registrefoncier.dataimport.detector.ServitudeRFDetector;
import ch.vd.unireg.registrefoncier.dataimport.detector.SurfaceAuSolRFDetector;
import ch.vd.unireg.registrefoncier.dataimport.elements.servitude.DienstbarkeitExtendedElement;
import ch.vd.unireg.xml.ExceptionHelper;

/**
 * Cette classe reçoit les données extraites de l'import du registre foncier, les compare avec les données en base et génère des événements de mutation correspondants.
 */
public class MutationsRFDetector {

	private static final Logger LOGGER = LoggerFactory.getLogger(MutationsRFDetector.class);

	private final RegistreFoncierImportService serviceImportRF;
	private final FichierImmeublesRFParser fichierImmeubleParser;
	private final FichierServitudeRFParser fichierServitudeParser;
	private final EvenementRFImportDAO evenementRFImportDAO;
	private final EvenementRFMutationDAO evenementRFMutationDAO;
	private final PlatformTransactionManager transactionManager;
	private final EsbStore zipRaftStore;

	private final AyantDroitRFDetector ayantDroitRFDetector;
	private final BatimentRFDetector batimentRFDetector;
	private final DroitRFDetector droitRFDetector;
	private final ServitudeRFDetector servitudeRFDetector;
	private final ImmeubleRFDetector immeubleRFDetector;
	private final SurfaceAuSolRFDetector surfaceAuSolRFDetector;

	public MutationsRFDetector(RegistreFoncierImportService serviceImportRF,
	                           FichierImmeublesRFParser fichierImmeubleParser,
	                           FichierServitudeRFParser fichierServitudeParser,
	                           EvenementRFImportDAO evenementRFImportDAO,
	                           EvenementRFMutationDAO evenementRFMutationDAO, PlatformTransactionManager transactionManager,
	                           EsbStore zipRaftStore,
	                           AyantDroitRFDetector ayantDroitRFDetector,
	                           BatimentRFDetector batimentRFDetector,
	                           DroitRFDetector droitRFDetector,
	                           ServitudeRFDetector servitudeRFDetector,
	                           ImmeubleRFDetector immeubleRFDetector,
	                           SurfaceAuSolRFDetector surfaceAuSolRFDetector) {
		this.serviceImportRF = serviceImportRF;
		this.fichierImmeubleParser = fichierImmeubleParser;
		this.fichierServitudeParser = fichierServitudeParser;
		this.evenementRFImportDAO = evenementRFImportDAO;
		this.evenementRFMutationDAO = evenementRFMutationDAO;
		this.transactionManager = transactionManager;
		this.zipRaftStore = zipRaftStore;
		this.ayantDroitRFDetector = ayantDroitRFDetector;
		this.batimentRFDetector = batimentRFDetector;
		this.droitRFDetector = droitRFDetector;
		this.servitudeRFDetector = servitudeRFDetector;
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

		try {
			updateEvent(importId, EtatEvenementRF.EN_TRAITEMENT, null);

			// détection de l'import initial
			final TypeImportRF typeImport = event.getType();
			final boolean importInitial = isImportInitial(typeImport);

			final MutationsRFDetectorResults rapport = new MutationsRFDetectorResults(importId, importInitial, typeImport, event.getDateEvenement(), nbThreads);

			// le job de traitement des imports ne supporte pas la reprise sur erreur (ou crash), on doit
			// donc effacer toutes les (éventuelles) mutations déjà générées lors d'un run précédent.
			deleteExistingMutations(importId, statusManager);

			// on peut maintenant processer l'import
			switch (typeImport) {
			case PRINCIPAL:
				processImportPrincipal(importId, event.getFileUrl(), nbThreads, statusManager);
				break;
			case SERVITUDES:
				processImportServitudes(importId, event.getFileUrl(), nbThreads, rapport, statusManager);
				break;
			default:
				throw new IllegalArgumentException("Type d'import inconnu = [" + typeImport + "].");
			}

			if (statusManager.isInterrupted()) {
				rapport.setInterrompu(true);
				updateEvent(importId, EtatEvenementRF.A_TRAITER, new Exception("Traitement interrompu."));
			}
			else {
				// terminé
				updateEvent(importId, EtatEvenementRF.TRAITE, null);
			}
			rapport.end();
			return rapport;
		}
		catch (Exception e) {
			LOGGER.warn("Erreur lors du processing de l'événement d'import RF avec l'id = [" + importId + "]", e);
			updateEvent(importId, EtatEvenementRF.EN_ERREUR, e);
			throw new RuntimeException(e);
		}
	}

	private void checkPreconditions(@NotNull EvenementRFImport event) {
		final long importId = event.getId();

		// l'import ne doit pas être déjà traité
		if (event.getEtat() != EtatEvenementRF.A_TRAITER && event.getEtat() != EtatEvenementRF.EN_ERREUR) {
			final IllegalArgumentException exception = new IllegalArgumentException("L'import RF avec l'id = [" + importId + "] a déjà été traité.");
			updateEvent(importId, event.getEtat(), exception);  // on ne change pas l'état
			throw exception;
		}

		// l'import doit être dans la suite chronologique des dates
		final RegDate previousValueDate = findValueDateOfOldestProcessedImport(importId, event.getType());
		if (previousValueDate != null && event.getDateEvenement().isBeforeOrEqual(previousValueDate)) { // SIFISC-22393
			final IllegalArgumentException exception =
					new IllegalArgumentException(
							"L'import RF avec l'id = [" + importId + "] possède une date de valeur [" + event.getDateEvenement() + "] antérieure ou égale à la date de valeur du dernier import traité [" + previousValueDate + "].");
			updateEvent(importId, EtatEvenementRF.EN_ERREUR, exception);
			throw exception;
		}

		// l'import doit être le prochain à traiter dans la suite chronologique des dates
		final EvenementRFImport nextToProcess = getNextImportToProcess(event.getType());
		if (!Objects.equals(importId, nextToProcess.getId())) {
			final IllegalArgumentException exception = new IllegalArgumentException("L'import RF avec l'id = [" + importId + "] doit être traité après l'import RF avec l'id = [" + nextToProcess.getId() + "].");
			updateEvent(importId, event.getEtat(), exception);  // on ne change pas l'état pour permettre de le lancer plus tard
			throw exception;
		}

		// il ne doit pas y avoir des mutations non-traitées d'un import précédent
		final Long unprocessedImport = findOldestImportWithUnprocessedMutations(importId, event.getType());
		if (unprocessedImport != null) {
			final IllegalArgumentException exception = new IllegalArgumentException("L'import RF avec l'id = [" + importId + "] ne peut être traité car des mutations de l'import RF avec l'id = [" + unprocessedImport + "] n'ont pas été traitées.");
			updateEvent(importId, event.getEtat(), exception);  // on ne change pas l'état pour permettre de le lancer plus tard
			throw exception;
		}

		if (event.getType() == TypeImportRF.SERVITUDES) {
			try {
				checkPreconditionsServitudes(event);
			}
			catch (IllegalArgumentException e) {
				updateEvent(importId, event.getEtat(), e);  // on ne change pas l'état pour permettre de le lancer plus tard
				throw e;
			}
		}
	}

	/**
	 * [SIFISC-24647] un import de servitude ne doit passer que si l'import principal correspondant (= même date) est complétement traité.
	 */
	private void checkPreconditionsServitudes(@NotNull EvenementRFImport event) {

		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);

		final String message = template.execute(status -> {
			String m = null;
			final EvenementRFImport importPrincipal = evenementRFImportDAO.find(TypeImportRF.PRINCIPAL, event.getDateEvenement());
			if (importPrincipal == null) {
				m = "L'import des servitudes RF avec la date valeur = [" + RegDateHelper.dateToDisplayString(event.getDateEvenement()) + "] " +
						"ne peut pas être traité car il n'y a pas d'import principal RF à la même date.";
			}
			else if (!(importPrincipal.getEtat() == EtatEvenementRF.TRAITE || importPrincipal.getEtat() == EtatEvenementRF.FORCE)) {
				m = "L'import des servitudes RF avec la date valeur = [" + RegDateHelper.dateToDisplayString(event.getDateEvenement()) + "] " +
						"ne peut pas être traité car l'import principal RF à la même date n'est pas traité.";
			}
			else {
				final int count = evenementRFMutationDAO.count(importPrincipal.getId(), Arrays.asList(EtatEvenementRF.A_TRAITER, EtatEvenementRF.EN_ERREUR, EtatEvenementRF.EN_TRAITEMENT));
				if (count > 0) {
					m = "L'import des servitudes RF avec la date valeur = [" + RegDateHelper.dateToDisplayString(event.getDateEvenement()) + "] " +
							"ne peut pas être traité car il y a encore " + count + " mutations à traiter sur l'import principal RF à la même date.";
				}
			}
			return m;
		});

		if (message != null) {
			throw new IllegalArgumentException(message);
		}
	}

	/**
	 * @param type de quel type d'import il s'agit
	 * @return <i>vrai</i> s'il s'agit de l'import initial; <i>faux</i> autrement.
	 */
	boolean isImportInitial(@NotNull TypeImportRF type) {
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);
		return template.execute(s -> {
			switch (type) {
			case PRINCIPAL:
				return evenementRFImportDAO.getCount(ImmeubleRF.class) == 0;
			case SERVITUDES:
				return evenementRFImportDAO.getCount(UsufruitRF.class) == 0;
			default:
				throw new IllegalArgumentException("Type d'import inconnu = [" + type + "]");
			}
		});
	}

	private void processImportPrincipal(long importId, String fileUrl, int nbThreads, @NotNull StatusManager statusManager) {
		try (InputStream is = zipRaftStore.get(fileUrl)) {

			statusManager.setMessage("Détection des mutations...");

			// Note : pour des raisons de performances, le parsing de l'import et la détection des mutations s'effectuent concurremment (en parallèle)
			final FichierImmeubleIteratorAdapter adapter = new FichierImmeubleIteratorAdapter();

			// on parse le fichier (dans un thread séparé)
			final Future<?> future;
			final ExecutorService executor = Executors.newFixedThreadPool(1,
			                                                              new DefaultThreadFactory(new DefaultThreadNameGenerator(String.format("%s-feed", Thread.currentThread().getName()))));
			try {
				future = executor.submit(() -> {
					fichierImmeubleParser.processFile(is, adapter);    // <-- émetteur des données
					return null;
				});
			}
			finally {
				// une fois la tâche terminée, l'exécuteur s'éteindra proprement
				executor.shutdown();
			}

			// on détecte les changements et crée les mutations
			processImmeubles(importId, nbThreads, adapter.getImmeublesIterator(), new SubStatusManager(0, 20, statusManager));   // <-- consommateur des données
			processDroits(importId, nbThreads, adapter.getDroitsIterator(), new SubStatusManager(20, 40, statusManager));
			processProprietaires(importId, nbThreads, adapter.getProprietairesIterator(), new SubStatusManager(40, 60, statusManager));
			processBatiments(importId, nbThreads, adapter.getConstructionsIterator(), new SubStatusManager(60, 80, statusManager));
			processSurfaces(importId, nbThreads, adapter.getSurfacesIterator(), new SubStatusManager(80, 100, statusManager));

			// on attend que le parsing soit terminé
			boolean finished = false;
			while (!finished) {
				try {
					future.get(1, TimeUnit.SECONDS);
					finished = true;
				}
				catch (TimeoutException e) {
					// on ignore l'exception mais on teste le statut du manager
					if (statusManager.isInterrupted()) {
						finished = true;
					}
				}
			}

			statusManager.setMessage("Traitement terminé.");
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private void processImportServitudes(long importId, String fileUrl, int nbThreads, @NotNull MutationsRFDetectorResults rapport, @NotNull StatusManager statusManager) {
		try (InputStream is = zipRaftStore.get(fileUrl)) {

			statusManager.setMessage("Détection des mutations...");

			final Map<String, DienstbarkeitExtendedElement> servitudes = new HashMap<>();
			final List<ch.vd.capitastra.rechteregister.Personstamm> beneficiaires = new ArrayList<>();

			fichierServitudeParser.processFile(is, new FichierServitudeRFParser.Callback() {
				@Override
				public void onServitude(@NotNull Dienstbarkeit servitude) {
					// les servitudes et les bénéficiaires de servitudes sont stockées dans deux listes disjointes dans le fichier, on les regroupes dans DienstbarkeitExtendedElement
					servitudes.put(servitude.getStandardRechtID(), new DienstbarkeitExtendedElement(servitude));
				}

				@Override
				public void onGroupeBeneficiaires(@NotNull LastRechtGruppe beneficiaires) {
					final DienstbarkeitExtendedElement servex = servitudes.get(beneficiaires.getStandardRechtIDREF());
					if (servex == null) {
						// on reçoit des bénéficiaires qui ne correspondent pas à des servitudes (certainement
						// à cause d'un filtre dans l'export de Capitastra) -> on les ignore
						return;
					}
					servex.addLastRechtGruppe(beneficiaires);
				}

				@Override
				public void onBeneficiaire(@NotNull ch.vd.capitastra.rechteregister.Personstamm beneficiaire) {
					beneficiaires.add(beneficiaire);
				}

				@Override
				public void done() {
				}
			});

			// on détecte les changements et crée les mutations (en utilisant le parallèle batch transaction template)
			processServitudes(importId, nbThreads, servitudes.values().iterator(), rapport, statusManager);
			processBeneficiaires(importId, nbThreads, beneficiaires.iterator(), statusManager);

			statusManager.setMessage("Traitement terminé.");
		}
		catch (Exception e) {
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
	 * @param type le type d'import considéré
	 * @return le prochain import qui doit être processé en respectant les états et les dates chronologiques d'import.
	 */
	@NotNull
	private EvenementRFImport getNextImportToProcess(TypeImportRF type) {
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);
		return template.execute(new TxCallback<EvenementRFImport>() {
			@Override
			public EvenementRFImport execute(TransactionStatus status) throws Exception {
				final EvenementRFImport next = evenementRFImportDAO.findNextImportToProcess(type);
				if (next == null) {
					throw new IllegalArgumentException("Il n'y a pas de prochain rapport à processer.");
				}
				return next;
			}
		});
	}

	/**
	 * @param importId l'id de l'import courant
	 * @param type     le type d'import considéré
	 * @return la date de valeur de l'import le plus ancien ayant été traité (complétement ou partiellement) sans tenir compte de l'import spécifié.
	 */
	@Nullable
	private RegDate findValueDateOfOldestProcessedImport(long importId, TypeImportRF type) {
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);
		return template.execute(s -> evenementRFImportDAO.findValueDateOfOldestProcessedImport(importId, type));
	}

	/**
	 * @param importId l'id de l'import courant
	 * @param type     le type d'import considéré
	 * @return retourne l'id de l'import le plus anciens qui possède encore des mutations à traiter (A_TRAITER ou EN_ERREUR)
	 */
	@Nullable
	private Long findOldestImportWithUnprocessedMutations(long importId, TypeImportRF type) {
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);
		return template.execute(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final EvenementRFImport previous = evenementRFImportDAO.findOldestImportWithUnprocessedMutations(importId, type);
				return previous == null ? null : previous.getId();
			}
		});
	}

	private void deleteExistingMutations(long importId, @NotNull StatusManager statusManager) {

		statusManager.setMessage("Effacement des mutations préexistantes...");

		serviceImportRF.deleteAllMutations(importId, statusManager);
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
		if (statusManager != null && statusManager.isInterrupted()) {
			return;
		}
		immeubleRFDetector.processImmeubles(importId, nbThreads, iterator, statusManager);
	}

	public void processDroits(long importId, int nbThreads, Iterator<EigentumAnteil> iterator, @Nullable StatusManager statusManager) {
		if (statusManager != null && statusManager.isInterrupted()) {
			return;
		}
		droitRFDetector.processDroitsPropriete(importId, nbThreads, iterator, statusManager);
	}

	public void processServitudes(long importId, int nbThreads, Iterator<DienstbarkeitExtendedElement> iterator, @NotNull MutationsRFDetectorResults rapport, @Nullable StatusManager statusManager) {
		if (statusManager != null && statusManager.isInterrupted()) {
			return;
		}
		servitudeRFDetector.processServitudes(importId, nbThreads, iterator, rapport, statusManager);
	}

	private void processBeneficiaires(long importId, int nbThreads, Iterator<ch.vd.capitastra.rechteregister.Personstamm> iterator, StatusManager statusManager) {
		if (statusManager != null && statusManager.isInterrupted()) {
			return;
		}
		ayantDroitRFDetector.processAyantDroits(importId, nbThreads, iterator, statusManager);
	}

	public void processProprietaires(long importId, int nbThreads, Iterator<Personstamm> iterator, @Nullable StatusManager statusManager) {
		if (statusManager != null && statusManager.isInterrupted()) {
			return;
		}
		ayantDroitRFDetector.processAyantDroits(importId, nbThreads, iterator, statusManager);
	}

	public void processBatiments(long importId, int nbThreads, Iterator<Gebaeude> iterator, @Nullable StatusManager statusManager) {
		if (statusManager != null && statusManager.isInterrupted()) {
			return;
		}
		batimentRFDetector.processBatiments(importId, nbThreads, iterator, statusManager);
	}

	public void processSurfaces(long importId, int nbThreads, Iterator<Bodenbedeckung> iterator, @Nullable StatusManager statusManager) {
		if (statusManager != null && statusManager.isInterrupted()) {
			return;
		}
		surfaceAuSolRFDetector.processSurfaces(importId, nbThreads, iterator, statusManager);
	}
}
