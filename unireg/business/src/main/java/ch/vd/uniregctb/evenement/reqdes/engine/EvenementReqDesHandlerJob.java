package ch.vd.uniregctb.evenement.reqdes.engine;

import java.util.Map;

import ch.vd.uniregctb.scheduler.JobDefinition;

public class EvenementReqDesHandlerJob extends JobDefinition {

	private static final String NAME = "EvenementReqDesHandlerJob";
	private static final String CATEGORIE = "Events";

	private EvenementReqDesRetryProcessor processor;

	public EvenementReqDesHandlerJob(int sortOrder, String description) {
		super(NAME, CATEGORIE, sortOrder, description);
	}

	public void setProcessor(EvenementReqDesRetryProcessor processor) {
		this.processor = processor;
	}

	@Override
	protected void doExecute(Map<String, Object> params) throws Exception {
		processor.relancerEvenementsReqDesNonTraites(getStatusManager());
	}
}
