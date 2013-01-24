package ch.vd.uniregctb.cache;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.scheduler.JobDefinition;
import ch.vd.uniregctb.scheduler.JobParam;
import ch.vd.uniregctb.scheduler.JobParamBoolean;

/**
 * Ce job permet de resetter manuellement les différents caches d'Unireg.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class CacheResetJob extends JobDefinition {

	// private final Logger LOGGER = Logger.getLogger(CacheResetJob.class);

	public static final String NAME = "CacheResetJob";
	private static final String CATEGORIE = "Cache";

	private UniregCacheManager manager;

	public void setManager(UniregCacheManager manager) {
		this.manager = manager;
	}

	public CacheResetJob(int sortOrder, String description) {
		super(NAME, CATEGORIE, sortOrder, description);
	}

	@Override
	public List<JobParam> getParamDefinition() {
		refreshParamDefinition(); // nécessaire parce qu'il n'est pas possible de gérer l'initialisation des caches définis dans le web
		return super.getParamDefinition();
	}

	private void refreshParamDefinition() {

		// On construit la liste des paramètres dynamiquement en fonction des caches enregistrés dans le manager

		final List<UniregCacheInterface> caches = new ArrayList<UniregCacheInterface>(manager.getCaches());
		Collections.sort(caches, new Comparator<UniregCacheInterface>() {
			@Override
			public int compare(UniregCacheInterface o1, UniregCacheInterface o2) {
				return o1.getName().compareTo(o2.getName());
			}
		});

		final List<JobParam> params = new ArrayList<JobParam>();

		for (UniregCacheInterface c : caches) {
			final JobParam param = new JobParam();
			param.setDescription("Reset du cache " + c.getDescription());
			param.setName(c.getName());
			param.setMandatory(true);
			param.setType(new JobParamBoolean());
			params.add(param);
		}

		refreshParameterDefinitions(params);
	}

	@Override
	protected void doExecute(Map<String, Object> params) throws Exception {

		final StatusManager statusManager = getStatusManager();

		String message = "Reset des caches...";
		statusManager.setMessage(message);

		int i = 0;

		final Collection<UniregCacheInterface> caches = manager.getCaches();
		final int size = caches.size();

		for (UniregCacheInterface c : caches) {
			final Boolean reset = (Boolean) params.get(c.getName());
			if (reset != null && reset) {
				message = "Reset du cache " + c.getDescription();
				statusManager.setMessage(message, (100 * (i + 1)) / size);
				Audit.warn(message);

				c.reset();
				++i;
			}
		}

		message = String.format("Les %d cache(s) ont été resetés.", i);
		statusManager.setMessage(message);
	}

}
