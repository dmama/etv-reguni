package ch.vd.uniregctb.identification.contribuable;

import java.util.Map;

import org.apache.log4j.Logger;

import ch.vd.uniregctb.scheduler.JobDefinition;

public class UpdateCriteresIdentificationJob extends JobDefinition {

	private static final Logger LOGGER = Logger.getLogger(UpdateCriteresIdentificationJob.class);

	public static final String NAME = "updateCriteresIdentificationJob";
	private static final String CATEGORIE = "Identification";

	private IdentificationContribuableService identCtbService;

	public UpdateCriteresIdentificationJob(int sortOrder, String description) {
		super(NAME, CATEGORIE, sortOrder, description);
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
