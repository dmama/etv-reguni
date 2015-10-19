package ch.vd.uniregctb.tache;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.uniregctb.scheduler.JobCategory;
import ch.vd.uniregctb.scheduler.JobDefinition;

/**
 * Job qui met-à-jour les statistiques des tâches.
 */
public class UpdateTacheStatsJob extends JobDefinition {

	private static final Logger LOGGER = LoggerFactory.getLogger(UpdateTacheStatsJob.class);

	public static final String NAME = "UpdateTacheStatsJob";

	private TacheService tacheService;

	public UpdateTacheStatsJob(int sortOrder, String description) {
		super(NAME, JobCategory.TACHE, sortOrder, description);
	}

	@Override
	protected void doExecute(Map<String, Object> params) throws Exception {
		try {
			tacheService.updateStats();
		}
		catch (Exception e) {
			LOGGER.error("Impossible de mettre-à-jour les tâches.", e);
			throw e;
		}
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setTacheService(TacheService tacheService) {
		this.tacheService = tacheService;
	}
}
