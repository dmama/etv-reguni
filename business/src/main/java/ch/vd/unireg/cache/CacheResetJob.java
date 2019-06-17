package ch.vd.unireg.cache;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;

import ch.vd.unireg.common.StatusManager;
import ch.vd.unireg.scheduler.JobCategory;
import ch.vd.unireg.scheduler.JobDefinition;
import ch.vd.unireg.scheduler.JobParam;
import ch.vd.unireg.scheduler.JobParamBoolean;

/**
 * Ce job permet de resetter manuellement les différents caches d'Unireg.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class CacheResetJob extends JobDefinition {

	// private static final Logger LOGGER = LoggerFactory.getLogger(CacheResetJob.class);

	public static final String NAME = "CacheResetJob";

	private UniregCacheManager manager;

	public void setManager(UniregCacheManager manager) {
		this.manager = manager;
	}

	public CacheResetJob(int sortOrder, String description) {
		super(NAME, JobCategory.CACHE, sortOrder, description);
	}

	@Override
	public List<JobParam> getParamDefinition() {
		refreshParamDefinition(); // nécessaire parce qu'il n'est pas possible de gérer l'initialisation des caches définis dans le web
		return super.getParamDefinition();
	}

	private void refreshParamDefinition() {

		// On construit la liste des paramètres dynamiquement en fonction des caches enregistrés dans le manager
		final List<Pair<JobParam, ?>> params = manager.getCaches().entrySet().stream()
				.sorted(Comparator.comparing(Map.Entry::getKey))
				.map(CacheResetJob::newParamDefinition)
				.collect(Collectors.toList());
		refreshParameterDefinitions(params);
	}

	@NotNull
	private static Pair<JobParam, Boolean> newParamDefinition(@NotNull Map.Entry<String, UniregCacheInterface> e) {
		final JobParam param = new JobParam();
		param.setDescription("Reset du cache " + e.getValue().getDescription());
		param.setName(e.getKey());
		param.setMandatory(true);
		param.setType(new JobParamBoolean());
		return Pair.of(param, Boolean.FALSE);
	}

	@Override
	protected void doExecute(Map<String, Object> params) throws Exception {

		final StatusManager statusManager = getStatusManager();

		String message = "Reset des caches...";
		statusManager.setMessage(message);

		int i = 0;

		final @NotNull Map<String, UniregCacheInterface> caches = manager.getCaches();
		final int size = caches.size();

		for (Map.Entry<String, UniregCacheInterface> entry : caches.entrySet()) {
			final UniregCacheInterface c = entry.getValue();
			final Boolean reset = (Boolean) params.get(entry.getKey());
			if (reset != null && reset) {
				message = "Reset du cache " + c.getDescription();
				statusManager.setMessage(message, (100 * (i + 1)) / size);
				audit.warn(message);

				c.reset();
				++i;
			}
		}

		message = String.format("Les %d cache(s) ont été resetés.", i);
		statusManager.setMessage(message);
	}

}
