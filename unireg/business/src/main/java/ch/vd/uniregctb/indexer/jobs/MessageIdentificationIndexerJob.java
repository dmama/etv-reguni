package ch.vd.uniregctb.indexer.jobs;

import java.util.Map;

import ch.vd.uniregctb.indexer.messageidentification.GlobalMessageIdentificationIndexer;
import ch.vd.uniregctb.scheduler.JobCategory;
import ch.vd.uniregctb.scheduler.JobDefinition;
import ch.vd.uniregctb.scheduler.JobParam;
import ch.vd.uniregctb.scheduler.JobParamInteger;

public class MessageIdentificationIndexerJob extends JobDefinition {

	private static final String NAME = "MessageIdentificationIndexerJob";
	private static final String NB_THREADS = "NB_THREADS";

	private GlobalMessageIdentificationIndexer globalIndexer;

	public void setGlobalIndexer(GlobalMessageIdentificationIndexer globalIndexer) {
		this.globalIndexer = globalIndexer;
	}

	public MessageIdentificationIndexerJob(int sortOrder, String description) {
		super(NAME, JobCategory.INDEXEUR, sortOrder, description);

		final JobParam param = new JobParam();
		param.setDescription("Nombre de threads");
		param.setName(NB_THREADS);
		param.setMandatory(true);
		param.setType(new JobParamInteger());
		addParameterDefinition(param, 8);
	}

	@Override
	protected void doExecute(Map<String, Object> params) throws Exception {
		final int nbThreads = getStrictlyPositiveIntegerValue(params, NB_THREADS);
		globalIndexer.indexAllDatabase(getStatusManager(), nbThreads);
	}
}
