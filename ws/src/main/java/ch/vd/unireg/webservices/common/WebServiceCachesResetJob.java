package ch.vd.uniregctb.webservices.common;

import java.util.List;
import java.util.Map;

import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.cache.UniregCacheInterface;
import ch.vd.uniregctb.scheduler.JobCategory;
import ch.vd.uniregctb.scheduler.JobDefinition;

public class WebServiceCachesResetJob extends JobDefinition {

	public static final String NAME = "WebServiceCachesResetJob";

	private List<UniregCacheInterface> caches;

	public WebServiceCachesResetJob(int sortOrder, String description) {
		super(NAME, JobCategory.CACHE, sortOrder, description);
	}

	@Override
	protected void doExecute(Map<String, Object> params) throws Exception {
		try {
			for (UniregCacheInterface cache : caches) {
				cache.reset();
			}
			Audit.success("Les caches des web-services ont été correctement resettés.");
		}
		catch (Exception e) {
			Audit.error("Impossible de resetter les caches des web-services pour la raison suivante: " + e.getMessage());
			throw e;
		}
	}

	public void setCaches(List<UniregCacheInterface> caches) {
		this.caches = caches;
	}
}
