package ch.vd.unireg.declaration.snc;

import java.util.Map;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.audit.Audit;
import ch.vd.unireg.common.StatusManager;
import ch.vd.unireg.document.EchoirQSNCRapport;
import ch.vd.unireg.rapport.RapportService;
import ch.vd.unireg.scheduler.JobCategory;
import ch.vd.unireg.scheduler.JobDefinition;
import ch.vd.unireg.scheduler.JobParam;
import ch.vd.unireg.scheduler.JobParamRegDate;

/**
 * Job qui fait passer à l'état 'échu' les questionnaires SNC sommés dont le délai de retour est dépassé
 */
public class EchoirQuestionnairesSNCJob extends JobDefinition {

	public static final String NAME = "EchoirQuestionnairesSNCJob";

	private EchoirQuestionnairesSNCProcessor processor;
	private RapportService rapportService;

	public EchoirQuestionnairesSNCJob(int sortOrder, String description) {
		super(NAME, JobCategory.QSNC, sortOrder, description);

		final JobParam param = new JobParam();
		param.setDescription("Date de traitement");
		param.setName(DATE_TRAITEMENT);
		param.setMandatory(false);
		param.setType(new JobParamRegDate());
		addParameterDefinition(param, null);
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		super.afterPropertiesSet();
		getParameterDefinition(DATE_TRAITEMENT).setEnabled(isTesting());
	}

	@Override
	protected void doExecute(Map<String, Object> params) throws Exception {

		final RegDate dateTraitement = getDateTraitement(params);
		final StatusManager status = getStatusManager();

		final EchoirQuestionnairesSNCResults results = processor.run(dateTraitement, status);
		final EchoirQSNCRapport rapport = rapportService.generateRapport(results, status);

		setLastRunReport(rapport);
		Audit.success("Le passage à l'état 'échu' des DIs PP sommées est terminé.", rapport);
	}

	public void setProcessor(EchoirQuestionnairesSNCProcessor processor) {
		this.processor = processor;
	}

	public void setRapportService(RapportService rapportService) {
		this.rapportService = rapportService;
	}
}
