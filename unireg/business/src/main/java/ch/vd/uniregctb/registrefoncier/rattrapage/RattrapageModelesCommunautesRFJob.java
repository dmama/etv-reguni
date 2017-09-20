package ch.vd.uniregctb.registrefoncier.rattrapage;

import java.util.Map;

import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.document.RattrapageModelesCommunautesRFProcessorRapport;
import ch.vd.uniregctb.rapport.RapportService;
import ch.vd.uniregctb.scheduler.JobCategory;
import ch.vd.uniregctb.scheduler.JobDefinition;
import ch.vd.uniregctb.scheduler.JobParam;
import ch.vd.uniregctb.scheduler.JobParamInteger;

/**
 * Ce batch réapplique les règles de regroupement avec les modèles de communauté sur toutes les communautés existantes.
 */
public class RattrapageModelesCommunautesRFJob extends JobDefinition {

	public static final String NAME = "RattrapageModelesCommunautesRFJob";
	public static final String NB_THREADS = "NB_THREADS";

	private RattrapageModelesCommunautesRFProcessor processor;
	private RapportService rapportService;

	public RattrapageModelesCommunautesRFJob(int sortOrder, String description) {
		super(NAME, JobCategory.RF, sortOrder, description);

		final JobParam param1 = new JobParam();
		param1.setDescription("Nombre de threads");
		param1.setName(NB_THREADS);
		param1.setMandatory(true);
		param1.setType(new JobParamInteger());
		addParameterDefinition(param1, 8);
	}

	@Override
	protected void doExecute(Map<String, Object> params) throws Exception {
		final int nbThreads = getStrictlyPositiveIntegerValue(params, NB_THREADS);
		final StatusManager statusManager = getStatusManager();

		final RattrapageModelesCommunautesRFProcessorResults results = processor.process(nbThreads, statusManager);
		final RattrapageModelesCommunautesRFProcessorRapport rapport = rapportService.generateRapport(results, statusManager);
		setLastRunReport(rapport);
		Audit.success("Le rattrapage du regroupement des communautés RF est terminé.", rapport);
	}

	public void setProcessor(RattrapageModelesCommunautesRFProcessor processor) {
		this.processor = processor;
	}

	public void setRapportService(RapportService rapportService) {
		this.rapportService = rapportService;
	}
}
