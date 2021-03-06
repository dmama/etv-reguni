package ch.vd.unireg.registrefoncier.dataimport;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.exception.ExceptionUtils;
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
import ch.vd.unireg.common.AuthenticationInterface;
import ch.vd.unireg.common.LengthConstants;
import ch.vd.unireg.common.LoggingStatusManager;
import ch.vd.unireg.common.ParallelBatchTransactionTemplateWithResults;
import ch.vd.unireg.common.StatusManager;
import ch.vd.unireg.common.SubStatusManager;
import ch.vd.unireg.evenement.registrefoncier.EtatEvenementRF;
import ch.vd.unireg.evenement.registrefoncier.EvenementRFImport;
import ch.vd.unireg.evenement.registrefoncier.EvenementRFImportDAO;
import ch.vd.unireg.evenement.registrefoncier.EvenementRFMutation;
import ch.vd.unireg.evenement.registrefoncier.EvenementRFMutationDAO;
import ch.vd.unireg.evenement.registrefoncier.TypeEntiteRF;
import ch.vd.unireg.evenement.registrefoncier.TypeMutationRF;
import ch.vd.unireg.registrefoncier.ImmeubleRF;
import ch.vd.unireg.registrefoncier.dataimport.processor.AyantDroitRFProcessor;
import ch.vd.unireg.registrefoncier.dataimport.processor.BatimentRFProcessor;
import ch.vd.unireg.registrefoncier.dataimport.processor.DroitRFProcessor;
import ch.vd.unireg.registrefoncier.dataimport.processor.MutationRFProcessor;
import ch.vd.unireg.registrefoncier.dataimport.processor.ServitudeRFProcessor;
import ch.vd.unireg.registrefoncier.dataimport.processor.SurfaceAuSolRFProcessor;
import ch.vd.unireg.xml.ExceptionHelper;

/**
 * Processeur responsable de traiter les mutations du registre foncier.
 */
public class MutationsRFProcessor {

	private static final Logger LOGGER = LoggerFactory.getLogger(MutationsRFProcessor.class);

	private final EvenementRFImportDAO evenementRFImportDAO;
	private final EvenementRFMutationDAO evenementRFMutationDAO;
	private final PlatformTransactionManager transactionManager;
	private final MutationRFProcessor communeRFProcessor;
	private final MutationRFProcessor immeubleRFProcessor;
	private final AyantDroitRFProcessor ayantDroitRFProcessor;
	private final DroitRFProcessor droitRFProcessor;
	private final ServitudeRFProcessor servitudeRFProcessor;
	private final SurfaceAuSolRFProcessor surfaceAuSolRFProcessor;
	private final BatimentRFProcessor batimentRFProcessor;

	public MutationsRFProcessor(@NotNull EvenementRFImportDAO evenementRFImportDAO,
	                            @NotNull EvenementRFMutationDAO evenementRFMutationDAO,
	                            @NotNull MutationRFProcessor communeRFProcessor,
	                            @NotNull MutationRFProcessor immeubleRFProcessor,
	                            @NotNull AyantDroitRFProcessor ayantDroitRFProcessor,
	                            @NotNull DroitRFProcessor droitRFProcessor,
	                            @NotNull ServitudeRFProcessor servitudeRFProcessor,
	                            @NotNull SurfaceAuSolRFProcessor surfaceAuSolRFProcessor,
	                            @NotNull BatimentRFProcessor batimentRFProcessor,
	                            @NotNull PlatformTransactionManager transactionManager) {
		this.evenementRFImportDAO = evenementRFImportDAO;
		this.evenementRFMutationDAO = evenementRFMutationDAO;
		this.ayantDroitRFProcessor = ayantDroitRFProcessor;
		this.immeubleRFProcessor = immeubleRFProcessor;
		this.droitRFProcessor = droitRFProcessor;
		this.surfaceAuSolRFProcessor = surfaceAuSolRFProcessor;
		this.batimentRFProcessor = batimentRFProcessor;
		this.transactionManager = transactionManager;
		this.communeRFProcessor = communeRFProcessor;
		this.servitudeRFProcessor = servitudeRFProcessor;
	}

	/**
	 * Traite tous les mutations à l'état A_TRAITER de l'import spécifié
	 *
	 * @param importId      l'id d'un import du registre foncier
	 * @param nbThreads     le nombre de threads à utiliser pour le traitement
	 * @param statusManager un status manager pour suivre la progression du traitement
	 */
	@NotNull
	public MutationsRFProcessorResults processImport(long importId, int nbThreads, @Nullable StatusManager statusManager) {

		if (statusManager == null) {
			statusManager = new LoggingStatusManager(LOGGER);
		}

		checkPreconditions(importId);

		final RegDate dateValeur = getDateValeur(importId);
		final boolean importInitial = isImportInitial();

		final MutationsRFProcessorResults rapportFinal = new MutationsRFProcessorResults(importId, importInitial, dateValeur, nbThreads, evenementRFMutationDAO);

		// pour respecter les contraintes relationnelles de la DB, on traite d'abord les créations et les modifications...
		final List<TypeMutationRF> creationEtModification = Arrays.asList(TypeMutationRF.CREATION, TypeMutationRF.MODIFICATION);
		processMutations(importId, importInitial, TypeEntiteRF.COMMUNE, creationEtModification, nbThreads, dateValeur, rapportFinal, new SubStatusManager(0, 10, statusManager));
		processMutations(importId, importInitial, TypeEntiteRF.IMMEUBLE, creationEtModification, nbThreads, dateValeur, rapportFinal, new SubStatusManager(10, 20, statusManager));
		processMutations(importId, importInitial, TypeEntiteRF.AYANT_DROIT, creationEtModification, nbThreads, dateValeur, rapportFinal, new SubStatusManager(20, 30, statusManager));
		processMutations(importId, importInitial, TypeEntiteRF.DROIT, creationEtModification, nbThreads, dateValeur, rapportFinal, new SubStatusManager(30, 40, statusManager));
		processMutations(importId, importInitial, TypeEntiteRF.SURFACE_AU_SOL, creationEtModification, nbThreads, dateValeur, rapportFinal, new SubStatusManager(40, 50, statusManager));
		processMutations(importId, importInitial, TypeEntiteRF.BATIMENT, creationEtModification, nbThreads, dateValeur, rapportFinal, new SubStatusManager(50, 60, statusManager));
		processMutations(importId, importInitial, TypeEntiteRF.SERVITUDE, creationEtModification, nbThreads, dateValeur, rapportFinal, new SubStatusManager(60, 70, statusManager));

		// ... puis les suppressions (attention, l'ordre de traitement des types d'entités est important aussi)
		final List<TypeMutationRF> suppression = Collections.singletonList(TypeMutationRF.SUPPRESSION);
		processMutations(importId, importInitial, TypeEntiteRF.BATIMENT, suppression, nbThreads, dateValeur, rapportFinal, new SubStatusManager(70, 74, statusManager));
		processMutations(importId, importInitial, TypeEntiteRF.SURFACE_AU_SOL, suppression, nbThreads, dateValeur, rapportFinal, new SubStatusManager(74, 78, statusManager));
		processMutations(importId, importInitial, TypeEntiteRF.DROIT, suppression, nbThreads, dateValeur, rapportFinal, new SubStatusManager(78, 83, statusManager));
		processMutations(importId, importInitial, TypeEntiteRF.AYANT_DROIT, suppression, nbThreads, dateValeur, rapportFinal, new SubStatusManager(83, 88, statusManager));
		processMutations(importId, importInitial, TypeEntiteRF.IMMEUBLE, suppression, nbThreads, dateValeur, rapportFinal, new SubStatusManager(88, 92, statusManager));
		processMutations(importId, importInitial, TypeEntiteRF.COMMUNE, suppression, nbThreads, dateValeur, rapportFinal, new SubStatusManager(92, 94, statusManager));
		processMutations(importId, importInitial, TypeEntiteRF.SERVITUDE, suppression, nbThreads, dateValeur, rapportFinal, new SubStatusManager(94, 100, statusManager));

		rapportFinal.end();
		return rapportFinal;
	}

	@NotNull
	private RegDate getDateValeur(long importId) {
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);
		return template.execute(status -> {
			final EvenementRFImport importEvent = evenementRFImportDAO.get(importId);
			if (importEvent == null) {
				throw new IllegalArgumentException("L'import avec l'id=[" + importId + "] n'existe pas.");
			}
			return importEvent.getDateEvenement();
		});
	}

	/**
	 * @return <i>vrai</i> s'il s'agit de l'import initial; <i>faux</i> autrement.
	 */
	boolean isImportInitial() {
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);
		return template.execute(status -> evenementRFImportDAO.getCount(ImmeubleRF.class) == 0);
	}

	private void checkPreconditions(long importId) {
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);
		template.execute(status -> {
			final Long nextMutationsToProcess = evenementRFMutationDAO.findNextMutationsToProcess();
			if (nextMutationsToProcess != null && nextMutationsToProcess != importId) {
				throw new IllegalArgumentException("Les mutations de l'import RF avec l'id = [" + importId + "] ne peuvent être traitées car les mutations de l'import RF avec l'id = [" + nextMutationsToProcess + "] n'ont pas été traitées.");
			}
			return null;
		});
	}

	private void processMutations(long importId,
	                              boolean importInitial, @NotNull TypeEntiteRF typeEntite,
	                              @NotNull Collection<TypeMutationRF> typesMutations,
	                              int nbThreads,
	                              @NotNull RegDate dateValeur,
	                              @NotNull MutationsRFProcessorResults rapportFinal,
	                              @NotNull final StatusManager statusManager) {

		final List<Long> ids = findIdsMutationsATraiter(importId, typeEntite, typesMutations);
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Mutations to process = {}", Arrays.toString(ids.toArray()));
		}

		final MutationRFProcessor proc = getProcessor(typeEntite);

		final SimpleProgressMonitor monitor = new SimpleProgressMonitor();
		final ParallelBatchTransactionTemplateWithResults<Long, MutationsRFProcessorResults> template =
				new ParallelBatchTransactionTemplateWithResults<>(ids, 100, nbThreads, Behavior.REPRISE_AUTOMATIQUE, transactionManager, statusManager, AuthenticationInterface.INSTANCE);
		template.execute(rapportFinal, new BatchWithResultsCallback<Long, MutationsRFProcessorResults>() {

			private final ThreadLocal<Long> first = new ThreadLocal<>();

			@Override
			public boolean doInTransaction(List<Long> mutationsIds, MutationsRFProcessorResults rapport) throws Exception {
				first.set(mutationsIds.get(0));
				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace("Processing mutations ids={}", Arrays.toString(mutationsIds.toArray()));
				}
				statusManager.setMessage("Traitement des mutations " + typeEntite.name() + "...", monitor.getProgressInPercent());
				mutationsIds.stream()
						.map(id -> getMutation(id))
						.forEach(mut -> processMutation(mut, importInitial, proc, rapport));
				return true;
			}

			@Override
			public void afterTransactionRollback(Exception e, boolean willRetry) {
				if (!willRetry) {
					final Long mutId = first.get();
					LOGGER.warn("Erreur pendant le traitement de la mutation n°" + mutId, e);
					if (mutId != null) {
						updateMutation(mutId, e);
					}
				}
			}

			@Override
			public MutationsRFProcessorResults createSubRapport() {
				return new MutationsRFProcessorResults(importId, importInitial, dateValeur, nbThreads, evenementRFMutationDAO);
			}
		}, monitor);
	}

	private void processMutation(@NotNull EvenementRFMutation mut, boolean importInitial, @NotNull MutationRFProcessor proc, @NotNull MutationsRFProcessorResults rapport) {
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Processing mutation id=[{}]", mut.getId());
		}

		proc.process(mut, importInitial, rapport);

		// on met-à-jour le statut de la mutation
		mut.setEtat(EtatEvenementRF.TRAITE);
	}

	@NotNull
	private EvenementRFMutation getMutation(Long id) {
		final EvenementRFMutation mutation = evenementRFMutationDAO.get(id);
		if (mutation == null) {
			throw new IllegalArgumentException("La mutation RF avec l'id=[" + id + "] n'existe pas.");
		}
		return mutation;
	}

	@NotNull
	private MutationRFProcessor getProcessor(@NotNull TypeEntiteRF typeEntite) {
		switch (typeEntite) {
		case AYANT_DROIT:
			return ayantDroitRFProcessor;
		case BATIMENT:
			return batimentRFProcessor;
		case COMMUNE:
			return communeRFProcessor;
		case DROIT:
			return droitRFProcessor;
		case SERVITUDE:
			return servitudeRFProcessor;
		case IMMEUBLE:
			return immeubleRFProcessor;
		case SURFACE_AU_SOL:
			return surfaceAuSolRFProcessor;
		default:
			throw new IllegalArgumentException("Type d'entité RF inconnue = [" + typeEntite + "]");
		}
	}

	@NotNull
	private List<Long> findIdsMutationsATraiter(long importId, @NotNull TypeEntiteRF typeEntite, @NotNull Collection<TypeMutationRF> typesMutations) {
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);
		return template.execute(status -> evenementRFMutationDAO.findIds(importId,
		                                                                 typeEntite,
		                                                                 Arrays.asList(EtatEvenementRF.A_TRAITER, EtatEvenementRF.EN_ERREUR),
		                                                                 typesMutations));
	}

	private void updateMutation(final long mutId, @Nullable final Exception e) {
		final TransactionTemplate t = new TransactionTemplate(transactionManager);
		t.execute(status -> {
			final EvenementRFMutation mutation = getMutation(mutId);
			mutation.setEtat(EtatEvenementRF.EN_ERREUR);
			if (e != null) {
				mutation.setErrorMessage(LengthConstants.streamlineField(ExceptionHelper.getMessage(e), 1000, true));
				mutation.setCallstack(ExceptionUtils.getStackTrace(e));
			}
			return null;
		});
	}
}
