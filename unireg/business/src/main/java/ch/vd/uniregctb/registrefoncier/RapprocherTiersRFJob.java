package ch.vd.uniregctb.registrefoncier;

import java.util.Map;

import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.document.RapprochementTiersRFRapport;
import ch.vd.uniregctb.rapport.RapportService;
import ch.vd.uniregctb.registrefoncier.processor.RapprochementTiersRFProcessor;
import ch.vd.uniregctb.registrefoncier.processor.RapprochementTiersRFResults;
import ch.vd.uniregctb.scheduler.JobCategory;
import ch.vd.uniregctb.scheduler.JobDefinition;

public class RapprocherTiersRFJob extends JobDefinition {

	public static final String NAME = "RapprocherTiersRFJob";

	private RapprochementTiersRFProcessor processor;
	private RapportService rapportService;

	public RapprocherTiersRFJob(int sortOrder, String description) {
		super(NAME, JobCategory.RF, sortOrder, description);
	}

	public void setProcessor(RapprochementTiersRFProcessor processor) {
		this.processor = processor;
	}

	public void setRapportService(RapportService rapportService) {
		this.rapportService = rapportService;
	}

	@Override
	protected void doExecute(Map<String, Object> params) throws Exception {
		final RapprochementTiersRFResults results = processor.run(getStatusManager());
		final RapprochementTiersRFRapport rapport = rapportService.generateRapport(results, getStatusManager());
		setLastRunReport(rapport);
		Audit.success("Le processus de rapprochement des tiers RF est terminé.", rapport);
	}
}
