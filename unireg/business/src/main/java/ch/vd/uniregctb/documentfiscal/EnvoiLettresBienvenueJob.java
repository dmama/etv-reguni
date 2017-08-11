package ch.vd.uniregctb.documentfiscal;

import java.util.Map;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.document.EnvoiLettresBienvenueRapport;
import ch.vd.uniregctb.rapport.RapportService;
import ch.vd.uniregctb.scheduler.JobCategory;
import ch.vd.uniregctb.scheduler.JobDefinition;
import ch.vd.uniregctb.scheduler.JobParam;
import ch.vd.uniregctb.scheduler.JobParamInteger;
import ch.vd.uniregctb.scheduler.JobParamRegDate;

public class EnvoiLettresBienvenueJob extends JobDefinition {

	private static final String NAME = "EnvoiLettresBienvenueJob";

	private static final String DELAI = "DELAI_CARENCE";

	private RapportService rapportService;
	private AutreDocumentFiscalService service;

	public EnvoiLettresBienvenueJob(int sortOrder, String description) {
		super(NAME, JobCategory.AUTRES_DOCUMENTS, sortOrder, description);
		{
			final JobParam param = new JobParam();
			param.setDescription("Date de traitement");
			param.setName(DATE_TRAITEMENT);
			param.setMandatory(false);
			param.setType(new JobParamRegDate());
			addParameterDefinition(param, null);
		}
		{
			final JobParam param = new JobParam();
			param.setDescription("Délai de carence (jours)");
			param.setName(DELAI);
			param.setMandatory(true);
			param.setType(new JobParamInteger());
			addParameterDefinition(param, 20);
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
		final int delaiCarence = getPositiveIntegerValue(params, DELAI);
		final StatusManager statusManager = getStatusManager();
		final EnvoiLettresBienvenueResults results = service.envoyerLettresBienvenueEnMasse(dateTraitement, delaiCarence, statusManager);
		final EnvoiLettresBienvenueRapport rapport = rapportService.generateRapport(results, statusManager);
		setLastRunReport(rapport);
		Audit.success("L'envoi des lettres de bienvenue est terminé.");
	}
}
