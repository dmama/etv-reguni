package ch.vd.uniregctb.indexer.jobs;

import java.util.Map;

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

	public DatabaseIndexerJob(int sortOrder, String description) {
		super(NAME, CATEGORIE, sortOrder, description);

		final JobParam param0 = new JobParam();
		param0.setDescription("Nombre de threads");
		param0.setName(I_NB_THREADS);
		param0.setMandatory(true);
		param0.setType(new JobParamInteger());
		addParameterDefinition(param0, 4);

		final JobParam param1 = new JobParam();
		param1.setDescription("Mode d'indexation");
		param1.setName(MODE);
		param1.setMandatory(true);
		param1.setType(new JobParamEnum(Mode.class));
		addParameterDefinition(param1, Mode.INCREMENTAL);

		final JobParam param2 = new JobParam();
		param2.setDescription("Précharge les individus");
		param2.setName(PREFETCH);
		param2.setMandatory(true);
		param2.setType(new JobParamBoolean());
		addParameterDefinition(param2, true);
	}

	@Override
	public void doExecute(Map<String, Object> params) throws Exception {

		final int nbThreads = getStrictlyPositiveIntegerValue(params, I_NB_THREADS);
		final Mode mode = getEnumValue(params, MODE, Mode.class);
		final boolean prefetch = getBooleanValue(params, PREFETCH);

		globalTiersIndexer.indexAllDatabase(getStatusManager(), nbThreads, mode, prefetch);
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setGlobalTiersIndexer(GlobalTiersIndexer globalTiersIndexer) {
		this.globalTiersIndexer = globalTiersIndexer;
	}
}
