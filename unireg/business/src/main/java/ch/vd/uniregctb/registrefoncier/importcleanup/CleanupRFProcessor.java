package ch.vd.uniregctb.registrefoncier.importcleanup;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;

import ch.vd.shared.batchtemplate.StatusManager;
import ch.vd.uniregctb.common.CollectionsUtils;
import ch.vd.uniregctb.common.LoggingStatusManager;
import ch.vd.uniregctb.evenement.registrefoncier.EtatEvenementRF;
import ch.vd.uniregctb.evenement.registrefoncier.EvenementRFImport;
import ch.vd.uniregctb.evenement.registrefoncier.EvenementRFImportDAO;
import ch.vd.uniregctb.evenement.registrefoncier.EvenementRFMutationDAO;
import ch.vd.uniregctb.evenement.registrefoncier.TypeImportRF;
import ch.vd.uniregctb.registrefoncier.RegistreFoncierImportService;
import ch.vd.uniregctb.transaction.TransactionTemplate;

/**
 * Processor qui efface les anciens imports (et leurs mutations) pour faire de la place.
 */
public class CleanupRFProcessor {

	private static final Logger LOGGER = LoggerFactory.getLogger(CleanupRFProcessor.class);

	private final EvenementRFImportDAO evenementRFImportDAO;
	private final EvenementRFMutationDAO evenementRFMutationDAO;
	private final RegistreFoncierImportService registreFoncierImportService;
	private final PlatformTransactionManager transactionManager;
	private final int retainSize;

	public CleanupRFProcessor(EvenementRFImportDAO evenementRFImportDAO,
	                          EvenementRFMutationDAO evenementRFMutationDAO,
	                          RegistreFoncierImportService registreFoncierImportService,
	                          PlatformTransactionManager transactionManager,
	                          int retainSize) {
		this.evenementRFImportDAO = evenementRFImportDAO;
		this.evenementRFMutationDAO = evenementRFMutationDAO;
		this.registreFoncierImportService = registreFoncierImportService;
		this.transactionManager = transactionManager;
		this.retainSize = retainSize;
	}

	public CleanupRFProcessorResults cleanupImports(@Nullable StatusManager statusManager) {

		if (statusManager == null) {
			statusManager = new LoggingStatusManager(LOGGER);
		}

		final CleanupRFProcessorResults results = new CleanupRFProcessorResults();

		statusManager.setMessage("Détermination des imports à nettoyer...");
		final List<Long> importPrincipalIds = determineImportsToDelete(results, TypeImportRF.PRINCIPAL);
		final List<Long> importServitudeIds = determineImportsToDelete(results, TypeImportRF.SERVITUDES);

		statusManager.setMessage("Nettoyage des imports...");
		deleteImports(CollectionsUtils.union(importPrincipalIds, importServitudeIds), results, statusManager);

		results.end();
		statusManager.setMessage("Terminé.");
		return results;
	}

	List<Long> determineImportsToDelete(@NotNull CleanupRFProcessorResults results, @NotNull TypeImportRF principal) {

		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);
		return template.execute(status -> {

			// on charge tous les imports (triés par ids croissants : du plus ancien au plus récent)
			final List<EvenementRFImport> imports = evenementRFImportDAO.find(principal);
			imports.sort(Comparator.comparing(EvenementRFImport::getId));

			// on ignore les imports les plus récents
			for (int i = 0; i < retainSize && !imports.isEmpty(); ++i) {
				final int last = imports.size() - 1;
				final EvenementRFImport imp = imports.get(last);
				results.addIgnored(imp.getId(), imp.getDateEvenement(), imp.getType(), CleanupRFProcessorResults.IgnoreReason.RETAINED);
				imports.remove(last);
			}

			// on ignore les imports à traiter, en traitement ou en erreur
			for (int i = imports.size() - 1; i >= 0; --i) {
				final EvenementRFImport imp = imports.get(i);
				final EtatEvenementRF etat = imp.getEtat();
				if (etat == EtatEvenementRF.A_TRAITER || etat == EtatEvenementRF.EN_TRAITEMENT || etat == EtatEvenementRF.EN_ERREUR) {
					results.addIgnored(imp.getId(), imp.getDateEvenement(), imp.getType(), CleanupRFProcessorResults.IgnoreReason.NOT_TREATED);
					imports.remove(i);
				}
			}

			// on ignore les imports avec des mutations à traiter, en traitement ou en erreur
			for (int i = imports.size() - 1; i >= 0; --i) {
				final EvenementRFImport imp = imports.get(i);
				final Map<EtatEvenementRF, Integer> countByState = evenementRFMutationDAO.countByState(imp.getId());
				final Integer aTraiter = countByState.getOrDefault(EtatEvenementRF.A_TRAITER, 0);
				final Integer enTraitement = countByState.getOrDefault(EtatEvenementRF.EN_TRAITEMENT, 0);
				final Integer enErreur = countByState.getOrDefault(EtatEvenementRF.EN_ERREUR, 0);
				if (aTraiter > 0 || enTraitement > 0 || enErreur > 0) {
					results.addIgnored(imp.getId(), imp.getDateEvenement(), imp.getType(), CleanupRFProcessorResults.IgnoreReason.MUTATIONS_NOT_TREATED);
					imports.remove(i);
				}
			}

			return imports.stream()
					.map(EvenementRFImport::getId)
					.collect(Collectors.toList());
		});
	}

	void deleteImports(@NotNull List<Long> importIds, @NotNull CleanupRFProcessorResults results, @NotNull StatusManager statusManager) {
		importIds.forEach(i -> deleteImport(i, results, statusManager));
	}

	private void deleteImport(long importId, @NotNull CleanupRFProcessorResults results, @NotNull StatusManager statusManager) {
		try {
			// on efface les mutations
			final int mutCount = registreFoncierImportService.deleteAllMutations(importId, statusManager);

			// on efface l'import lui-même
			final TransactionTemplate template = new TransactionTemplate(transactionManager);
			final EvenementRFImport imp = template.execute(status -> {
				final EvenementRFImport i = evenementRFImportDAO.get(importId);
				evenementRFImportDAO.remove(importId);
				return i;
			});

			results.addProcessed(importId, imp.getDateEvenement(), imp.getType(), mutCount);
		}
		catch (RuntimeException e) {
			results.addErrorException(importId, e);
		}
	}

}
