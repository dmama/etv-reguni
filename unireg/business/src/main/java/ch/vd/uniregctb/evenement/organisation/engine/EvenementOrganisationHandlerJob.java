package ch.vd.uniregctb.evenement.organisation.engine;

import java.util.Map;

import ch.vd.shared.batchtemplate.StatusManager;
import ch.vd.uniregctb.scheduler.JobDefinition;

public class EvenementOrganisationHandlerJob extends JobDefinition {

	public static final String NAME = "EvenementOrganisationHandlerJob";
	private static final String CATEGORIE = "Events";

	private EvenementOrganisationRetryProcessor processorOrganisation;

	public EvenementOrganisationHandlerJob(int sortOrder, String description) {
		super(NAME, CATEGORIE, sortOrder, description);
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
