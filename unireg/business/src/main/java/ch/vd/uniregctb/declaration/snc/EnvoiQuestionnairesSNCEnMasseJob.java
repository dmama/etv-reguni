package ch.vd.uniregctb.declaration.snc;

import java.util.Map;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.document.EnvoiQuestionnairesSNCRapport;
import ch.vd.uniregctb.rapport.RapportService;
import ch.vd.uniregctb.scheduler.JobCategory;
import ch.vd.uniregctb.scheduler.JobDefinition;
import ch.vd.uniregctb.scheduler.JobParam;
import ch.vd.uniregctb.scheduler.JobParamInteger;
import ch.vd.uniregctb.scheduler.JobParamRegDate;

public class EnvoiQuestionnairesSNCEnMasseJob extends JobDefinition {

	private static final String NAME = "EnvoiQuestionnairesSNCEnMasseJob";

	private static final String PERIODE_FISCALE = "PERIODE";
	private static final String NB_MAX_ENVOIS = "NB_MAX_ENVOIS";

	private RapportService rapportService;
	private QuestionnaireSNCService questionnaireService;

	public EnvoiQuestionnairesSNCEnMasseJob(int sortOrder, String description) {
		super(NAME, JobCategory.QSNC, sortOrder, description);
		{
			final JobParam param = new JobParam();
			param.setDescription("Période fiscale concernée");
			param.setName(PERIODE_FISCALE);
			param.setMandatory(true);
			param.setType(new JobParamInteger());
			addParameterDefinition(param, RegDate.get().year() - 1);
		}
		{
			final JobParam param = new JobParam();
			param.setDescription("Nombre maximum d'envois");
			param.setName(NB_MAX_ENVOIS);
			param.setMandatory(false);
			param.setType(new JobParamInteger());
			addParameterDefinition(param, null);
		}
		{
			final JobParam param = new JobParam();
			param.setDescription("Date de traitement");
			param.setName(DATE_TRAITEMENT);
			param.setMandatory(false);
			param.setType(new JobParamRegDate());
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
		final int pf = getStrictlyPositiveIntegerValue(params, PERIODE_FISCALE);
		final Integer nbMaxEnvois = getOptionalIntegerValue(params, NB_MAX_ENVOIS);
		final RegDate dateTraitement = getDateTraitement(params);

		final StatusManager statusManager = getStatusManager();
		final EnvoiQuestionnairesSNCEnMasseResults results = questionnaireService.envoiQuestionnairesSNCEnMasse(pf, dateTraitement, nbMaxEnvois, statusManager);
		final EnvoiQuestionnairesSNCRapport rapport = rapportService.generateRapport(results, statusManager);
		setLastRunReport(rapport);

		Audit.success("L'envoi des questionnaires SNC de l'année " + pf + " à la date du " + RegDateHelper.dateToDisplayString(dateTraitement) + " est terminé.", rapport);
	}
}
