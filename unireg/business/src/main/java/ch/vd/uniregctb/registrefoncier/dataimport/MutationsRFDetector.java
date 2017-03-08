package ch.vd.uniregctb.registrefoncier.dataimport;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Executors;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;

import ch.vd.capitastra.grundstueck.Bodenbedeckung;
import ch.vd.capitastra.grundstueck.Gebaeude;
import ch.vd.capitastra.grundstueck.Grundstueck;
import ch.vd.capitastra.grundstueck.PersonEigentumAnteil;
import ch.vd.capitastra.grundstueck.Personstamm;
import ch.vd.capitastra.rechteregister.Dienstbarkeit;
import ch.vd.capitastra.rechteregister.LastRechtGruppe;
import ch.vd.registre.base.date.RegDate;
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
import ch.vd.uniregctb.evenement.registrefoncier.TypeImportRF;
import ch.vd.uniregctb.registrefoncier.ImmeubleRF;
import ch.vd.uniregctb.registrefoncier.RegistreFoncierImportService;
import ch.vd.uniregctb.registrefoncier.UsufruitRF;
import ch.vd.uniregctb.registrefoncier.dataimport.detector.AyantDroitRFDetector;
import ch.vd.uniregctb.registrefoncier.dataimport.detector.BatimentRFDetector;
import ch.vd.uniregctb.registrefoncier.dataimport.detector.DroitRFDetector;
import ch.vd.uniregctb.registrefoncier.dataimport.detector.ImmeubleRFDetector;
import ch.vd.uniregctb.registrefoncier.dataimport.detector.SurfaceAuSolRFDetector;
import ch.vd.uniregctb.registrefoncier.dataimport.elements.servitude.DienstbarkeitExtendedElement;
import ch.vd.uniregctb.transaction.TransactionTemplate;
import ch.vd.uniregctb.xml.ExceptionHelper;

/**
 * Cette classe reçoit les données extraites de l'import du registre foncier, les compare avec les données en base et génère des événements de mutation correspondants.
 */
public class MutationsRFDetector implements InitializingBean {

	private static final Logger LOGGER = LoggerFactory.getLogger(MutationsRFDetector.class);

	private final RegistreFoncierImportService serviceImportRF;
	private final FichierImmeublesRFParser fichierImmeubleParser;
	private final FichierServitudeRFParser fichierServitudeParser;
	private final EvenementRFImportDAO evenementRFImportDAO;
	private final PlatformTransactionManager transactionManager;
	private final EsbStore zipRaftStore;

	private final AyantDroitRFDetector ayantDroitRFDetector;
	private final BatimentRFDetector batimentRFDetector;
	private final DroitRFDetector droitRFDetector;
	private final ImmeubleRFDetector immeubleRFDetector;
	private final SurfaceAuSolRFDetector surfaceAuSolRFDetector;

	public MutationsRFDetector(RegistreFoncierImportService serviceImportRF,
	                           FichierImmeublesRFParser fichierImmeubleParser,
	                           FichierServitudeRFParser fichierServitudeParser,
	                           EvenementRFImportDAO evenementRFImportDAO,
	                           PlatformTransactionManager transactionManager,
	                           EsbStore zipRaftStore,
	                           AyantDroitRFDetector ayantDroitRFDetector,
	                           BatimentRFDetector batimentRFDetector,
	                           DroitRFDetector droitRFDetector,
	                           ImmeubleRFDetector immeubleRFDetector,
	                           SurfaceAuSolRFDetector surfaceAuSolRFDetector) {
		this.serviceImportRF = serviceImportRF;
		this.fichierImmeubleParser = fichierImmeubleParser;
		this.fichierServitudeParser = fichierServitudeParser;
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
				processImportPrincipal(importId, event.getFileUrl(), importInitial, nbThreads, statusManager);
				break;
			case SERVITUDES:
				processImportServitudes(importId, event.getFileUrl(), importInitial, nbThreads, statusManager);
				break;
			default:
				throw new IllegalArgumentException("Type d'import inconnu = [" + typeImport + "].");
			}

			// terminé
			updateEvent(importId, EtatEvenementRF.TRAITE, null);
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
		if (event.getEtat() != EtatEvenementRF.A_TRAITER && event.getEtat() != EtatEvenementRF.EN_ERREUR) {
			final IllegalArgumentException exception = new IllegalArgumentException("L'import RF avec l'id = [" + importId + "] a déjà été traité.");
			updateEvent(importId, EtatEvenementRF.EN_ERREUR, exception);
			throw exception;
		}
		final RegDate previousValueDate = findValueDateOfOldestProcessedImport(importId, event.getType());
		if (previousValueDate != null && event.getDateEvenement().isBeforeOrEqual(previousValueDate)) { // SIFISC-22393
			final IllegalArgumentException exception =
					new IllegalArgumentException(
							"L'import RF avec l'id = [" + importId + "] possède une date de valeur [" + event.getDateEvenement() + "] antérieure ou égale à la date de valeur du dernier import traité [" + previousValueDate + "].");
			updateEvent(importId, EtatEvenementRF.EN_ERREUR, exception);
			throw exception;
		}
		final EvenementRFImport nextToProcess = getNextImportToProcess(event.getType());
		if (!Objects.equals(importId, nextToProcess.getId())) {
			final IllegalArgumentException exception = new IllegalArgumentException("L'import RF avec l'id = [" + importId + "] doit être traité après l'import RF avec l'id = [" + nextToProcess.getId() + "].");
			updateEvent(importId, EtatEvenementRF.EN_ERREUR, exception);
			throw exception;
		}
		final Long unprocessedImport = findOldestImportWithUnprocessedMutations(importId, event.getType());
		if (unprocessedImport != null) {
			final IllegalArgumentException exception = new IllegalArgumentException("L'import RF avec l'id = [" + importId + "] ne peut être traité car des mutations de l'import RF avec l'id = [" + unprocessedImport + "] n'ont pas été traitées.");
			updateEvent(importId, EtatEvenementRF.EN_ERREUR, exception);
			throw exception;
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

	private void processImportPrincipal(long importId, String fileUrl, boolean importInitial, int nbThreads, @NotNull StatusManager statusManager) {
		try (InputStream is = zipRaftStore.get(fileUrl)) {

			statusManager.setMessage("Détection des mutations...");

			// Note : pour des raisons de performances, le parsing de l'import et la détection des mutations s'effectuent concurremment (en parallèle)
			final FichierImmeubleIteratorAdapter adapter = new FichierImmeubleIteratorAdapter();

			// on parse le fichier (dans un thread séparé)
			ExecutorCompletionService<Boolean> ecs = new ExecutorCompletionService<>(Executors.newFixedThreadPool(1));
			ecs.submit(() -> {
				fichierImmeubleParser.processFile(is, adapter);    // <-- émetteur des données
				return true;
			});

			// on détecte les changements et crée les mutations
			processImmeubles(importId, nbThreads, adapter.getImmeublesIterator(), new SubStatusManager(0, 20, statusManager));   // <-- consommateur des données
			processDroits(importId, nbThreads, adapter.getDroitsIterator(), importInitial, new SubStatusManager(20, 40, statusManager));
			processProprietaires(importId, nbThreads, adapter.getProprietairesIterator(), new SubStatusManager(40, 60, statusManager));
			processBatiments(importId, nbThreads, adapter.getConstructionsIterator(), new SubStatusManager(60, 80, statusManager));
			processSurfaces(importId, nbThreads, adapter.getSurfacesIterator(), new SubStatusManager(80, 100, statusManager));

			// on attend que le parsing soit terminé
			ecs.take().get();

			statusManager.setMessage("Traitement terminé.");
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private void processImportServitudes(long importId, String fileUrl, boolean importInitial, int nbThreads, @NotNull StatusManager statusManager) {
		try (InputStream is = zipRaftStore.get(fileUrl)) {

			statusManager.setMessage("Détection des mutations...");

			// TODO (msi) faut-il instancier un adapteur multi-threadé (comme pour le fichier principal) ? A voir lorsqu'on recevra l'export de taille normale

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
			processServitudes(importId, nbThreads, servitudes.values().iterator(), importInitial, statusManager);
			processBeneficiaires(importId, nbThreads, beneficiaires.iterator(), importInitial, statusManager);

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
		immeubleRFDetector.processImmeubles(importId, nbThreads, iterator, statusManager);
	}

	public void processDroits(long importId, int nbThreads, Iterator<PersonEigentumAnteil> iterator, boolean importInitial, @Nullable StatusManager statusManager) {
		droitRFDetector.processDroitsPropriete(importId, nbThreads, iterator, importInitial, statusManager);
	}

	public void processServitudes(long importId, int nbThreads, Iterator<DienstbarkeitExtendedElement> iterator, boolean importInitial, @Nullable StatusManager statusManager) {
		// Les usufruits peuvent concerner plusieurs immeubles et plusieurs bénéficiaires, ce qui ne rentre pas dans le modèle de données des droits d'Unireg.
		// On instancie donc un itérateur spécial qui va retourner autant de droit 'discrets' qu'il y a d'immeubles et de bénéficiaires.
		final DienstbarkeitDiscreteIterator discreteIterator = new DienstbarkeitDiscreteIterator(iterator);
		droitRFDetector.processServitudes(importId, nbThreads, discreteIterator, importInitial, statusManager);
	}

	private void processBeneficiaires(long importId, int nbThreads, Iterator<ch.vd.capitastra.rechteregister.Personstamm> iterator, boolean importInitial, StatusManager statusManager) {
		ayantDroitRFDetector.processAyantDroits(importId, nbThreads, iterator, statusManager);
	}

	public void processProprietaires(long importId, int nbThreads, Iterator<Personstamm> iterator, @Nullable StatusManager statusManager) {
		ayantDroitRFDetector.processAyantDroits(importId, nbThreads, iterator, statusManager);
	}

	public void processBatiments(long importId, int nbThreads, Iterator<Gebaeude> iterator, @Nullable StatusManager statusManager) {
		batimentRFDetector.processBatiments(importId, nbThreads, iterator, statusManager);
	}

	public void processSurfaces(long importId, int nbThreads, Iterator<Bodenbedeckung> iterator, @Nullable StatusManager statusManager) {
		surfaceAuSolRFDetector.processSurfaces(importId, nbThreads, iterator, statusManager);
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.execute(status -> {
			final int count = evenementRFImportDAO.fixAbnormalJVMTermination();
			if (count > 0) {
				LOGGER.warn("Corrigé l'état de " + count + " job(s) d'importation RF suite à l'arrêt anormal de la JVM.");
			}
			return null;
		});
	}
}
