package ch.vd.unireg.indexer.jobs;

import java.util.Map;

import ch.vd.unireg.indexer.messageidentification.GlobalMessageIdentificationIndexer;
import ch.vd.unireg.scheduler.JobCategory;
import ch.vd.unireg.scheduler.JobDefinition;
import ch.vd.unireg.scheduler.JobParam;
import ch.vd.unireg.scheduler.JobParamInteger;

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
