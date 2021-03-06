package ch.vd.unireg.indexer;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.unireg.common.StatusManager;
import ch.vd.unireg.indexer.tiers.GlobalTiersSearcher;
import ch.vd.unireg.indexer.tiers.GlobalTiersSearcher.CheckCallback;
import ch.vd.unireg.scheduler.JobCategory;
import ch.vd.unireg.scheduler.JobDefinition;
import ch.vd.unireg.tiers.TiersDAO;

/**
 * Batch qui vérifie les cohérence des données de l'indexeur avec les données de la base.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class CheckCoherenceIndexerJob extends JobDefinition {

	private static final Logger LOGGER = LoggerFactory.getLogger(CheckCoherenceIndexerJob.class);

	public static final String NAME = "CheckCoherenceIndexerJob";

	private GlobalTiersSearcher searcher;
	private TiersDAO tiersDAO;
	private PlatformTransactionManager transactionManager;

	public CheckCoherenceIndexerJob(int sortOrder, String description) {
		super(NAME, JobCategory.INDEXEUR, sortOrder, description);
	}

	@Override
	protected void doExecute(Map<String, Object> params) throws Exception {

		final StatusManager statusManager = getStatusManager();
		statusManager.setMessage("Chargement des tiers de la base de données...", 25);

		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);

		// Charge les ids des tiers existants dans la base de données
		final Set<Long> existingIds = template.execute(status -> new HashSet<>(tiersDAO.getAllIds()));

		if (statusManager.isInterrupted()) {
			LOGGER.warn("Traitement interrompu.");
			return;
		}

		final class Counts {
			int errors = 0;
			int warnings = 0;
		}
		final Counts counts = new Counts();

		// Vérifie la cohérence de l'index
		searcher.checkCoherenceIndex(existingIds, statusManager, new CheckCallback() {
			@Override
			public void onError(long id, String message) {
				counts.errors++;
				LOGGER.error(message);
			}

			@Override
			public void onWarning(long id, String message) {
				counts.warnings++;
				LOGGER.warn(message);
			}
		});

		if (statusManager.isInterrupted()) {
			LOGGER.warn("Traitement interrompu.");
			return;
		}

		// Affiche le résultat
		if (counts.errors > 0 || counts.warnings > 0) {
			audit.error("Les données de l'indexer sont incohérentes : " + counts.errors + " erreur(s) et " + counts.warnings
					            + " avertissement(s) ont été trouvés. Voir le log technique pour les détails.");
		}
		else {
			audit.info("Les données de l'indexer sont cohérentes.");
		}
	}

	public void setSearcher(GlobalTiersSearcher searcher) {
		this.searcher = searcher;
	}

	public void setTiersDAO(TiersDAO tiersDAO) {
		this.tiersDAO = tiersDAO;
	}

	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}
}
