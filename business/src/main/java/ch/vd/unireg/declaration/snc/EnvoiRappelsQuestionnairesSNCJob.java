package ch.vd.unireg.declaration.snc;

import java.util.Map;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.common.StatusManager;
import ch.vd.unireg.document.EnvoiRappelsQuestionnairesSNCRapport;
import ch.vd.unireg.rapport.RapportService;
import ch.vd.unireg.scheduler.JobCategory;
import ch.vd.unireg.scheduler.JobDefinition;
import ch.vd.unireg.scheduler.JobParam;
import ch.vd.unireg.scheduler.JobParamInteger;
import ch.vd.unireg.scheduler.JobParamRegDate;

/**
 * Job d'envoi en masse des rappels des questionnaires SNC
 */
public class EnvoiRappelsQuestionnairesSNCJob extends JobDefinition {

	private static final String NAME = "EnvoiRappelsQuestionnairesSNCJob";
	private static final String PARAM_MAX_RAPPELS = "NB_MAX_RAPPELS";
	private static final String PERIODE_FISCALE = "PERIODE";

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
			param.setDescription("Période fiscale");
			param.setName(PERIODE_FISCALE);
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
		final Integer periodeFiscale = getOptionalPositiveIntegerValue(params, PERIODE_FISCALE);
		final RegDate dateTraitement = getDateTraitement(params);

		final StatusManager statusManager = getStatusManager();
		final EnvoiRappelsQuestionnairesSNCResults results = questionnaireService.envoiRappelsQuestionnairesSNCEnMasse(dateTraitement, periodeFiscale, nbMaxRappels, statusManager);

		final EnvoiRappelsQuestionnairesSNCRapport rapport = rapportService.generateRapport(results, statusManager);
		setLastRunReport(rapport);

		audit.success("L'envoi des rappels des questionnaires SNC à la date du " + RegDateHelper.dateToDisplayString(dateTraitement) + " est terminé.", rapport);
	}
}
