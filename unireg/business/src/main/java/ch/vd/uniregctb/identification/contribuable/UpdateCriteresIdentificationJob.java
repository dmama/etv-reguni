package ch.vd.uniregctb.identification.contribuable;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.uniregctb.scheduler.JobCategory;
import ch.vd.uniregctb.scheduler.JobDefinition;

public class UpdateCriteresIdentificationJob extends JobDefinition {

	private static final Logger LOGGER = LoggerFactory.getLogger(UpdateCriteresIdentificationJob.class);

	public static final String NAME = "UpdateCriteresIdentificationJob";

	private IdentificationContribuableService identCtbService;

	public UpdateCriteresIdentificationJob(int sortOrder, String description) {
		super(NAME, JobCategory.IDENTIFICATION, sortOrder, description);
	}



	@Override
	protected void doExecute(Map<String, Object> params) throws Exception {
		try {
			identCtbService.updateCriteres();
		}
		catch (Exception e) {
			LOGGER.error("Impossible de mettre-à-jour les critères de recherches d'identification de ctb.", e);
			throw e;
		}
	}

	public void setIdentCtbService(IdentificationContribuableService identCtbService) {
		this.identCtbService = identCtbService;
	}
}
