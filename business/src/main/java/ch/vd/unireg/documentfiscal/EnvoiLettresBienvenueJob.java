package ch.vd.unireg.documentfiscal;

import java.util.Map;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.StatusManager;
import ch.vd.unireg.document.EnvoiLettresBienvenueRapport;
import ch.vd.unireg.rapport.RapportService;
import ch.vd.unireg.scheduler.JobCategory;
import ch.vd.unireg.scheduler.JobDefinition;
import ch.vd.unireg.scheduler.JobParam;
import ch.vd.unireg.scheduler.JobParamInteger;
import ch.vd.unireg.scheduler.JobParamRegDate;

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
		audit.success("L'envoi des lettres de bienvenue est terminé.");
	}
}
