package ch.vd.unireg.registrefoncier;

import java.util.Map;

import ch.vd.unireg.audit.Audit;
import ch.vd.unireg.document.RapprochementTiersRFRapport;
import ch.vd.unireg.rapport.RapportService;
import ch.vd.unireg.registrefoncier.processor.RapprochementTiersRFProcessor;
import ch.vd.unireg.registrefoncier.processor.RapprochementTiersRFResults;
import ch.vd.unireg.scheduler.JobCategory;
import ch.vd.unireg.scheduler.JobDefinition;
import ch.vd.unireg.scheduler.JobParam;
import ch.vd.unireg.scheduler.JobParamInteger;

public class RapprocherTiersRFJob extends JobDefinition {

	public static final String NAME = "RapprocherTiersRFJob";
	public static final String NB_THREADS = "NB_THREADS";

	private RapprochementTiersRFProcessor processor;
	private RapportService rapportService;

	public RapprocherTiersRFJob(int sortOrder, String description) {
		super(NAME, JobCategory.RF, sortOrder, description);
		{
			final JobParam param = new JobParam();
			param.setDescription("Nombre de threads");
			param.setEnabled(true);
			param.setMandatory(true);
			param.setType(new JobParamInteger());
			param.setName(NB_THREADS);
			addParameterDefinition(param, 4);
		}
	}

	public void setProcessor(RapprochementTiersRFProcessor processor) {
		this.processor = processor;
	}

	public void setRapportService(RapportService rapportService) {
		this.rapportService = rapportService;
	}

	@Override
	protected void doExecute(Map<String, Object> params) throws Exception {
		final int nbThreads = getStrictlyPositiveIntegerValue(params, NB_THREADS);
		final RapprochementTiersRFResults results = processor.run(nbThreads, getStatusManager());
		final RapprochementTiersRFRapport rapport = rapportService.generateRapport(results, getStatusManager());
		setLastRunReport(rapport);
		Audit.success("Le processus de rapprochement des tiers RF est terminé.", rapport);
	}
}
