package ch.vd.uniregctb.documentfiscal;

import java.util.Map;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.document.RappelLettresBienvenueRapport;
import ch.vd.uniregctb.rapport.RapportService;
import ch.vd.uniregctb.scheduler.JobCategory;
import ch.vd.uniregctb.scheduler.JobDefinition;
import ch.vd.uniregctb.scheduler.JobParam;
import ch.vd.uniregctb.scheduler.JobParamRegDate;

public class RappelLettresBienvenueJob extends JobDefinition {

	private static final String NAME = "RappelLettresBienvenueJob";

	private RapportService rapportService;
	private AutreDocumentFiscalService service;

	public RappelLettresBienvenueJob(int sortOrder, String description) {
		super(NAME, JobCategory.AUTRES_DOCUMENTS, sortOrder, description);
		{
			final JobParam param = new JobParam();
			param.setDescription("Date de traitement");
			param.setName(DATE_TRAITEMENT);
			param.setMandatory(false);
			param.setType(new JobParamRegDate());
			addParameterDefinition(param, null);
		}
	}

	public void setRapportService(RapportService rapportService) {
		this.rapportService = rapportService;
	}

	public void setService(AutreDocumentFiscalService service) {
		this.service = service;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		super.afterPropertiesSet();
		getParameterDefinition(DATE_TRAITEMENT).setEnabled(isTesting());
	}

	@Override
	protected void doExecute(Map<String, Object> params) throws Exception {
		final RegDate dateTraitement = getDateTraitement(params);
		final StatusManager statusManager = getStatusManager();
		final RappelLettresBienvenueResults results = service.envoyerRappelsLettresBienvenueEnMasse(dateTraitement, statusManager);
		final RappelLettresBienvenueRapport rapport = rapportService.generateRapport(results, statusManager);
		setLastRunReport(rapport);
		Audit.success("L'envoi des rappels des lettres de bienvenue est terminé.");
	}
}
