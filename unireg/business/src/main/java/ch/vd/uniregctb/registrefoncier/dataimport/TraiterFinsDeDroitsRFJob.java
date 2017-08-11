package ch.vd.uniregctb.registrefoncier.dataimport;

import java.util.Map;

import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.document.FinsDeDroitRFRapport;
import ch.vd.uniregctb.rapport.RapportService;
import ch.vd.uniregctb.registrefoncier.dataimport.processor.DateFinDroitsRFProcessor;
import ch.vd.uniregctb.scheduler.JobCategory;
import ch.vd.uniregctb.scheduler.JobDefinition;
import ch.vd.uniregctb.scheduler.JobParam;
import ch.vd.uniregctb.scheduler.JobParamInteger;

public class TraiterFinsDeDroitsRFJob extends JobDefinition {

	public static final String NAME = "TraiterFinsDeDroitsRFJob";
	public static final String NB_THREADS = "NB_THREADS";

	private DateFinDroitsRFProcessor processor;
	private RapportService rapportService;

	public TraiterFinsDeDroitsRFJob(int sortOrder, String description) {
		super(NAME, JobCategory.RF, sortOrder, description);

		final JobParam param = new JobParam();
		param.setDescription("Nombre de threads");
		param.setName(NB_THREADS);
		param.setMandatory(true);
		param.setType(new JobParamInteger());
		addParameterDefinition(param, 4);
	}

	public void setProcessor(DateFinDroitsRFProcessor processor) {
		this.processor = processor;
	}

	public void setRapportService(RapportService rapportService) {
		this.rapportService = rapportService;
	}

	@Override
	protected void doExecute(Map<String, Object> params) throws Exception {

		final int nbThreads = getStrictlyPositiveIntegerValue(params, NB_THREADS);

		final StatusManager statusManager = getStatusManager();
		final TraitementFinsDeDroitRFResults results = processor.process(nbThreads, getStatusManager());
		final FinsDeDroitRFRapport rapport = rapportService.generateRapport(results, statusManager);
		setLastRunReport(rapport);
		Audit.success("Le traitement des fins de droit RF est terminé.", rapport);

		statusManager.setMessage("Traitement terminé.");
	}
}
