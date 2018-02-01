package ch.vd.unireg.foncier;

import java.util.Map;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.audit.Audit;
import ch.vd.unireg.common.StatusManager;
import ch.vd.unireg.document.RappelFormulairesDemandeDegrevementICIRapport;
import ch.vd.unireg.documentfiscal.AutreDocumentFiscalService;
import ch.vd.unireg.rapport.RapportService;
import ch.vd.unireg.scheduler.JobCategory;
import ch.vd.unireg.scheduler.JobDefinition;
import ch.vd.unireg.scheduler.JobParam;
import ch.vd.unireg.scheduler.JobParamRegDate;

public class RappelDemandesDegrevementICIJob extends JobDefinition {

	private static final String NAME = "RappelDemandesDegrevementICIJob";

	private RapportService rapportService;
	private AutreDocumentFiscalService service;

	public RappelDemandesDegrevementICIJob(int sortOrder, String description) {
		super(NAME, JobCategory.DD, sortOrder, description);
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

	public void setAutreDocumentFiscalService(AutreDocumentFiscalService service) {
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
		final RappelFormulairesDemandeDegrevementICIResults results = service.envoyerRappelsFormulairesDemandeDegrevementICIEnMasse(dateTraitement, statusManager);
		final RappelFormulairesDemandeDegrevementICIRapport rapport = rapportService.generateRapport(results, statusManager);
		setLastRunReport(rapport);
		Audit.success("L'envoi des rappels des formulaires de demande de dégrèvement ICI est terminé.");
	}
}
