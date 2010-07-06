package ch.vd.uniregctb.tache;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import ch.vd.uniregctb.scheduler.JobDefinition;
import ch.vd.uniregctb.scheduler.JobParam;

/**
 * Job qui met-à-jour les statistiques des tâches.
 */
public class UpdateTacheStatsJob extends JobDefinition {

	private static final Logger LOGGER = Logger.getLogger(UpdateTacheStatsJob.class);

	public static final String NAME = "UpdateTacheStatsJob";
	private static final String CATEGORIE = "Tache";

	private static final List<JobParam> params = Collections.emptyList();
	private static final Map<String, Object> defaultParams = Collections.emptyMap();

	private TacheService tacheService;

	public UpdateTacheStatsJob(int sortOrder, String description) {
		super(NAME, CATEGORIE, sortOrder, description, params, defaultParams);
	}

	@Override
	protected void doExecute(HashMap<String, Object> params) throws Exception {
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
