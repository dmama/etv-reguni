package ch.vd.unireg.evenement.entreprise.engine;

import java.util.Map;

import ch.vd.unireg.common.StatusManager;
import ch.vd.unireg.scheduler.JobCategory;
import ch.vd.unireg.scheduler.JobDefinition;

public class EvenementEntrepriseHandlerJob extends JobDefinition {

	public static final String NAME = "EvenementEntrepriseHandlerJob";

	private EvenementEntrepriseRetryProcessor processorEntreprise;

	public EvenementEntrepriseHandlerJob(int sortOrder, String description) {
		super(NAME, JobCategory.EVENTS, sortOrder, description);
	}

	@Override
	protected void doExecute(Map<String, Object> params) throws Exception {
		
		final StatusManager status = getStatusManager();

		// Validation/Traitement des événements entreprise
		status.setMessage("Traitement des événements entreprise...");
		processorEntreprise.retraiteEvenements(status);
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setProcessorEntreprise(EvenementEntrepriseRetryProcessor processorEntreprise) {
		this.processorEntreprise = processorEntreprise;
	}
}
