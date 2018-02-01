package ch.vd.unireg.evenement.reqdes.engine;

import java.util.Map;

import ch.vd.unireg.scheduler.JobCategory;
import ch.vd.unireg.scheduler.JobDefinition;

public class EvenementReqDesHandlerJob extends JobDefinition {

	private static final String NAME = "EvenementReqDesHandlerJob";

	private EvenementReqDesRetryProcessor processor;

	public EvenementReqDesHandlerJob(int sortOrder, String description) {
		super(NAME, JobCategory.EVENTS, sortOrder, description);
	}

	public void setProcessor(EvenementReqDesRetryProcessor processor) {
		this.processor = processor;
	}

	@Override
	protected void doExecute(Map<String, Object> params) throws Exception {
		processor.relancerEvenementsReqDesNonTraites(getStatusManager());
	}
}
