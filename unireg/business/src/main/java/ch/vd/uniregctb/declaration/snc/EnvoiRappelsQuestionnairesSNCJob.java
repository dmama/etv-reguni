package ch.vd.uniregctb.declaration.snc;

import java.util.Map;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.shared.batchtemplate.StatusManager;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.document.EnvoiRappelsQuestionnairesSNCRapport;
import ch.vd.uniregctb.rapport.RapportService;
import ch.vd.uniregctb.scheduler.JobCategory;
import ch.vd.uniregctb.scheduler.JobDefinition;
import ch.vd.uniregctb.scheduler.JobParam;
import ch.vd.uniregctb.scheduler.JobParamInteger;
import ch.vd.uniregctb.scheduler.JobParamRegDate;

/**
 * Job d'envoi en masse des rappels des questionnaires SNC
 */
public class EnvoiRappelsQuestionnairesSNCJob extends JobDefinition {

	private static final String NAME = "EnvoiRappelsQuestionnairesSNCJob";

	private static final String PARAM_MAX_RAPPELS = "NB_MAX_RAPPELS";

	private RapportService rapportService;
	private QuestionnaireSNCService questionnaireService;

	public EnvoiRappelsQuestionnairesSNCJob(int sortOrder, String description) {
		super(NAME, JobCategory.QSNC, sortOrder, description);
		{
			final JobParam param = new JobParam();
			param.setDescription("Nombre maximal de rappels émis (0 ou vide = pas de limite)");
			param.setName(PARAM_MAX_RAPPELS);
			param.setMandatory(false);
			param.setType(new JobParamInteger());
			addParameterDefinition(param, null);
		}
		{
			final JobParam param = new JobParam();
			param.setDescription("Date de traitement");
			param.setName(DATE_TRAITEMENT);
			param.setMandatory(false);
			param.setType(new JobParamRegDate(false));
			addParameterDefinition(param, null);
		}
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		super.afterPropertiesSet();
		getParameterDefinition(DATE_TRAITEMENT).setEnabled(isTesting());
	}

	public void setRapportService(RapportService rapportService) {
		this.rapportService = rapportService;
	}

	public void setQuestionnaireService(QuestionnaireSNCService questionnaireService) {
		this.questionnaireService = questionnaireService;
	}

	@Override
	protected void doExecute(Map<String, Object> params) throws Exception {
		final Integer nbMaxRappels = getOptionalIntegerValue(params, PARAM_MAX_RAPPELS);
		final RegDate dateTraitement = getDateTraitement(params);

		final StatusManager statusManager = getStatusManager();
		final EnvoiRappelsQuestionnairesSNCResults results = questionnaireService.envoiRappelsQuestionnairesSNCEnMasse(dateTraitement, nbMaxRappels, statusManager);

		final EnvoiRappelsQuestionnairesSNCRapport rapport = rapportService.generateRapport(results, statusManager);
		setLastRunReport(rapport);

		Audit.success("L'envoi des rappels des questionnaires SNC à la date du " + RegDateHelper.dateToDisplayString(dateTraitement) + " est terminé.", rapport);
	}
}
