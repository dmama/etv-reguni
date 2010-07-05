package ch.vd.uniregctb.indexer.jobs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import ch.vd.uniregctb.indexer.tiers.GlobalTiersIndexer;
import ch.vd.uniregctb.indexer.tiers.GlobalTiersIndexer.Mode;
import ch.vd.uniregctb.scheduler.JobDefinition;
import ch.vd.uniregctb.scheduler.JobParam;
import ch.vd.uniregctb.scheduler.JobParamBoolean;
import ch.vd.uniregctb.scheduler.JobParamEnum;
import ch.vd.uniregctb.scheduler.JobParamInteger;

/**
 * Job qui réindexe tout ou partie des tiers de la base de données
 */
public class DatabaseIndexerJob extends JobDefinition {

	public static final String NAME = "DatabaseIndexerJob";
	private static final String CATEGORIE = "Indexeur";

	public static final String I_NB_THREADS = "nbThreads";
	public static final String MODE = "mode";
	public static final String PREFETCH = "prefetch";

	private GlobalTiersIndexer globalTiersIndexer;

	private static final List<JobParam> params;

	static {
		params = new ArrayList<JobParam>();

		JobParam param0 = new JobParam();
		param0.setDescription("Nombre de threads");
		param0.setName(I_NB_THREADS);
		param0.setMandatory(false);
		param0.setType(new JobParamInteger());
		params.add(param0);

		JobParam param1 = new JobParam();
		param1.setDescription("Mode d'indexation");
		param1.setName(MODE);
		param1.setMandatory(false);
		param1.setType(new JobParamEnum(Mode.class));
		params.add(param1);

		JobParam param2 = new JobParam();
		param2.setDescription("Précharge les individus");
		param2.setName(PREFETCH);
		param2.setMandatory(false);
		param2.setType(new JobParamBoolean());
		params.add(param2);
	}

	public DatabaseIndexerJob(int sortOrder, HashMap<String, Object> defParams) {
		super(NAME, CATEGORIE, sortOrder, "Réindexer tout ou partie des tiers de la base de données", params, defParams);
	}

	@Override
	public void doExecute(HashMap<String, Object> params) throws Exception {

		Integer nbThreads = 1;
		Mode mode = Mode.INCREMENTAL;
		boolean prefetch = false;

		if (params != null) {
			Integer nb = (Integer) params.get(I_NB_THREADS);
			if (nb != null) {
				nbThreads = nb;
			}
			Mode m = (Mode) params.get(MODE);
			if (m != null) {
				mode = m;
			}

			Boolean p = (Boolean) params.get(PREFETCH);
			if (p != null) {
				prefetch = p;
			}
		}

		globalTiersIndexer.indexAllDatabaseAsync(getStatusManager(), nbThreads, mode, prefetch);
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setGlobalTiersIndexer(GlobalTiersIndexer globalTiersIndexer) {
		this.globalTiersIndexer = globalTiersIndexer;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		super.afterPropertiesSet();

		final HashMap<String, Object> p = new HashMap<String, Object>();
		p.put(I_NB_THREADS, 4);
		p.put(MODE, Mode.DIRTY_ONLY);
		p.put(PREFETCH, true);

		batchScheduler.registerCron(this, p, "0 0 2 * * ?"); // schedule l'indexation des dirties tous les jours, à 2 heures du matin
	}
}
