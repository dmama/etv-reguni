package ch.vd.unireg.registrefoncier.rattrapage;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.mutable.MutableBoolean;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.registre.base.date.RegDate;
import ch.vd.shared.batchtemplate.BatchWithResultsCallback;
import ch.vd.shared.batchtemplate.Behavior;
import ch.vd.shared.batchtemplate.SimpleProgressMonitor;
import ch.vd.unireg.common.AnnulableHelper;
import ch.vd.unireg.common.AuthenticationInterface;
import ch.vd.unireg.common.LoggingStatusManager;
import ch.vd.unireg.common.ObjectNotFoundException;
import ch.vd.unireg.common.ParallelBatchTransactionTemplateWithResults;
import ch.vd.unireg.common.ProgrammingException;
import ch.vd.unireg.common.StatusManager;
import ch.vd.unireg.evenement.fiscal.EvenementFiscalService;
import ch.vd.unireg.registrefoncier.DroitProprieteRF;
import ch.vd.unireg.registrefoncier.ImmeubleRF;
import ch.vd.unireg.registrefoncier.RegistreFoncierService;
import ch.vd.unireg.registrefoncier.dao.ImmeubleRFDAO;
import ch.vd.unireg.registrefoncier.dataimport.processor.AffaireRF;
import ch.vd.unireg.registrefoncier.dataimport.processor.AffaireRFListener;
import ch.vd.unireg.registrefoncier.dataimport.processor.CommunauteRFProcessor;

public class RattraperDatesMetierDroitRFProcessor {

	private static final Logger LOGGER = LoggerFactory.getLogger(RattraperDatesMetierDroitRFProcessor.class);

	private final ImmeubleRFDAO immeubleRFDAO;
	private final PlatformTransactionManager transactionManager;
	private final RegistreFoncierService registreFoncierService;
	private final EvenementFiscalService evenementFiscalService;
	private final CommunauteRFProcessor communauteRFProcessor;

	public RattraperDatesMetierDroitRFProcessor(@NotNull ImmeubleRFDAO immeubleRFDAO,
	                                            @NotNull PlatformTransactionManager transactionManager,
	                                            @NotNull RegistreFoncierService registreFoncierService,
	                                            @NotNull EvenementFiscalService evenementFiscalService,
	                                            @NotNull CommunauteRFProcessor communauteRFProcessor) {
		this.immeubleRFDAO = immeubleRFDAO;
		this.transactionManager = transactionManager;
		this.registreFoncierService = registreFoncierService;
		this.evenementFiscalService = evenementFiscalService;
		this.communauteRFProcessor = communauteRFProcessor;
	}

	/**
	 * @param dataSelection le type d'immeuble à processer
	 * @param immeubleIds   les ids d'immeuble à processer (si dataSelection=EXPLICIT_SELECTION)
	 * @param nbThreads     le nombre de threads
	 * @param sm            un statut manager
	 * @return les résultats du processing
	 */
	public RattraperDatesMetierDroitRFProcessorResults process(@NotNull RattrapageDataSelection dataSelection,
	                                                           @Nullable List<Long> immeubleIds,
	                                                           int nbThreads,
	                                                           @Nullable StatusManager sm) {

		final StatusManager statusManager = (sm == null ? new LoggingStatusManager(LOGGER) : sm);

		// recherche des immeubles concernés
		final List<Long> ids;
		switch (dataSelection) {
		case EXPLICIT_SELECTION:
			ids = immeubleIds;
			break;
		case ALL:
			ids = getAllImmeubleIds();
			break;
		default:
			ids = findImmeubleIdsToProcess();
			break;
		}

		final RattraperDatesMetierDroitRFProcessorResults rapportFinal = new RattraperDatesMetierDroitRFProcessorResults(dataSelection, nbThreads, immeubleRFDAO, registreFoncierService);

		// on traite chaque immeuble
		final SimpleProgressMonitor monitor = new SimpleProgressMonitor();
		final ParallelBatchTransactionTemplateWithResults<Long, RattraperDatesMetierDroitRFProcessorResults> template =
				new ParallelBatchTransactionTemplateWithResults<>(ids, 100, nbThreads, Behavior.REPRISE_AUTOMATIQUE, transactionManager, statusManager, AuthenticationInterface.INSTANCE);
		template.execute(rapportFinal, new BatchWithResultsCallback<Long, RattraperDatesMetierDroitRFProcessorResults>() {

			private final ThreadLocal<Long> first = new ThreadLocal<>();

			@Override
			public boolean doInTransaction(List<Long> immeubleIds, RattraperDatesMetierDroitRFProcessorResults rapport) throws Exception {
				first.set(immeubleIds.get(0));
				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace("Processing immovables properties ids={}", Arrays.toString(immeubleIds.toArray()));
				}
				statusManager.setMessage("Traitement des immeubles...", monitor.getProgressInPercent());
				immeubleIds.forEach(id -> processImmeuble(id, rapport));
				return !statusManager.isInterrupted();
			}

			@Override
			public void afterTransactionRollback(Exception e, boolean willRetry) {
				if (!willRetry) {
					final Long mutId = first.get();
					LOGGER.warn("Erreur pendant le traitement de l'immeuble n°" + mutId, e);
				}
			}

			@Override
			public RattraperDatesMetierDroitRFProcessorResults createSubRapport() {
				return new RattraperDatesMetierDroitRFProcessorResults(dataSelection, nbThreads, immeubleRFDAO, registreFoncierService);
			}
		}, monitor);

		rapportFinal.end();
		return rapportFinal;
	}

	void processImmeuble(long id, @NotNull RattraperDatesMetierDroitRFProcessorResults rapport) {
		final ImmeubleRF immeuble = immeubleRFDAO.get(id);
		if (immeuble == null) {
			throw new ObjectNotFoundException("L'immeuble avec l'id=[" + id + "] n'existe pas");
		}

		rapport.addProcessed(immeuble);

		// on va chercher les droits à analyser (on ignore les droits annulés, comme il se doit)
		final List<DroitProprieteRF> droits = immeuble.getDroitsPropriete().stream()
				.filter(AnnulableHelper::nonAnnule)
				.collect(Collectors.toList());
		final Set<DroitProprieteRF> untouched = new HashSet<>(droits);

		// on déduit les dates d'import où les droits ont été modifiés à partir de leurs dates de début/fin
		final Set<RegDate> datesImport = droits.stream()
				.map(RattraperDatesMetierDroitRFProcessor::extractDatesImport)
				.flatMap(Collection::stream)
				.collect(Collectors.toSet());

		// on analyse les droits et construit la liste des affaires pour chaque date d'import
		final List<AffaireRF> affaires = datesImport.stream()
				.map(d -> new AffaireRF(d, immeuble))
				.collect(Collectors.toList());

		// on recalcule et rattrape si nécessaire les dates de début sur les droits ouverts
		final MutableBoolean somethingChanged = new MutableBoolean(false);
		affaires.forEach(c -> c.refreshDatesMetier(new AffaireRFListener() {

			@Override
			public void onCreation(DroitProprieteRF droit) {
				throw new ProgrammingException();   // on ne devrait jamais avoir de droits nouvellement ouverts
			}

			@Override
			public void onUpdateDateDebut(@NotNull DroitProprieteRF droit, @Nullable RegDate dateDebutMetierInitiale, @Nullable String motifDebutInitial) {
				somethingChanged.setTrue();
				untouched.remove(droit);
				rapport.addDebutUpdated(droit, dateDebutMetierInitiale, motifDebutInitial);
				// on publie l'événement fiscal correspondant
				evenementFiscalService.publierModificationDroitPropriete(droit.getDateDebutMetier(), droit);

			}

			@Override
			public void onUpdateDateFin(@NotNull DroitProprieteRF droit, @Nullable RegDate dateFinMetierInitiale, @Nullable String motifFinInitial) {
				somethingChanged.setTrue();
				untouched.remove(droit);
				rapport.addFinUpdated(droit, dateFinMetierInitiale, motifFinInitial);
				// on publie l'événement fiscal correspondant
				evenementFiscalService.publierModificationDroitPropriete(droit.getDateFinMetier(), droit);
			}

			@Override
			public void onOtherUpdate(@NotNull DroitProprieteRF droit) {
				throw new ProgrammingException();   // on ne devrait jamais avoir de droits modifiés pour d'autres raisons
			}

			@Override
			public void onClosing(@NotNull DroitProprieteRF droit) {
				throw new ProgrammingException();   // on ne devrait jamais avoir de droits nouvellement fermés
			}
		}));

		// on recalcule si nécessaire ce qu'il faut sur les communautés de l'immeuble
		if (somethingChanged.isTrue()) {
			communauteRFProcessor.processAll(immeuble);
		}

		untouched.forEach(rapport::addUntouched);
	}

	/**
	 * @param droit un droit de propriété
	 * @return les dates d'import où le droit a été ouvert ou fermé.
	 */
	private static Collection<RegDate> extractDatesImport(@NotNull DroitProprieteRF droit) {
		final RegDate dateDebut = droit.getDateDebut();
		final RegDate dateFin = droit.getDateFin();

		if (dateDebut != null && dateFin != null) {
			return Arrays.asList(dateDebut, dateFin.getOneDayAfter());
		}
		else if (dateDebut != null) {
			return Collections.singletonList(dateDebut);
		}
		else if (dateFin != null) {
			// les droits sont fermés à la veille de la date d'import, il faut donc aller chercher le lendemeain pour retomber sur ses pattes
			return Collections.singletonList(dateFin.getOneDayAfter());
		}
		else {
			return Collections.emptyList();
		}
	}

	private List<Long> getAllImmeubleIds() {
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);
		return template.execute(status -> immeubleRFDAO.getAllIds());
	}

	private List<Long> findImmeubleIdsToProcess() {

		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);
		return template.execute(status -> immeubleRFDAO.findImmeubleIdsAvecDatesDeFinDroitsACalculer());
	}

}
