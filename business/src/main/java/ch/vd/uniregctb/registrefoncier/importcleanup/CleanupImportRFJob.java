package ch.vd.uniregctb.registrefoncier.importcleanup;

import java.util.Map;

import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.document.CleanupRFProcessorRapport;
import ch.vd.uniregctb.rapport.RapportService;
import ch.vd.uniregctb.scheduler.JobCategory;
import ch.vd.uniregctb.scheduler.JobDefinition;

/**
 * Job de cleanup des mutations du registre foncier
 */
public class CleanupImportRFJob extends JobDefinition {

	public static final String NAME = "CleanupImportRFJob";
	public static final String ID = "eventId";

	private CleanupRFProcessor processor;
	private RapportService rapportService;

	public void setProcessor(CleanupRFProcessor processor) {
		this.processor = processor;
	}

	public void setRapportService(RapportService rapportService) {
		this.rapportService = rapportService;
	}

	public CleanupImportRFJob(int sortOrder, String description) {
		super(NAME, JobCategory.RF, sortOrder, description);
	}

	@Override
	protected void doExecute(Map<String, Object> params) throws Exception {

		final StatusManager statusManager = getStatusManager();

		// on fait le cleanup
		statusManager.setMessage("Nettoyage des mutations...");
		final CleanupRFProcessorResults results = processor.cleanupImports(statusManager);
		final CleanupRFProcessorRapport rapport = rapportService.generateRapport(results, getStatusManager());
		setLastRunReport(rapport);
		Audit.success("Le nettoyage des imports RF est terminé.", rapport);

		statusManager.setMessage("Traitement terminé.");
	}
}
