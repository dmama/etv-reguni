package ch.vd.unireg.evenement.organisation.engine;

import java.util.Map;

import ch.vd.unireg.common.StatusManager;
import ch.vd.unireg.scheduler.JobCategory;
import ch.vd.unireg.scheduler.JobDefinition;

public class EvenementOrganisationHandlerJob extends JobDefinition {

	public static final String NAME = "EvenementOrganisationHandlerJob";

	private EvenementOrganisationRetryProcessor processorOrganisation;

	public EvenementOrganisationHandlerJob(int sortOrder, String description) {
		super(NAME, JobCategory.EVENTS, sortOrder, description);
	}

	@Override
	protected void doExecute(Map<String, Object> params) throws Exception {
		
		final StatusManager status = getStatusManager();

		// Validation/Traitement des événements organisation
		status.setMessage("Traitement des événements organisation...");
		processorOrganisation.retraiteEvenements(status);
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setProcessorOrganisation(EvenementOrganisationRetryProcessor processorOrganisation) {
		this.processorOrganisation = processorOrganisation;
	}
}
