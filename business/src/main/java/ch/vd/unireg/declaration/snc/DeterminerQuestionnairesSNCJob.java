package ch.vd.unireg.declaration.snc;

import java.util.Map;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.audit.Audit;
import ch.vd.unireg.document.DeterminationQuestionnairesSNCRapport;
import ch.vd.unireg.rapport.RapportService;
import ch.vd.unireg.scheduler.JobCategory;
import ch.vd.unireg.scheduler.JobDefinition;
import ch.vd.unireg.scheduler.JobParam;
import ch.vd.unireg.scheduler.JobParamInteger;
import ch.vd.unireg.scheduler.JobParamRegDate;

public class DeterminerQuestionnairesSNCJob extends JobDefinition {

	public static final String NAME = "DeterminerQuestionnairesSNCJob";
	public static final String PERIODE_FISCALE = "PERIODE";
	public static final String NB_THREADS = "NB_THREADS";

	private RapportService rapportService;
	private QuestionnaireSNCService questionnaireService;

	public DeterminerQuestionnairesSNCJob(int sortOrder, String description) {
		super(NAME, JobCategory.QSNC, sortOrder, description);
		{
			final JobParam param = new JobParam();
			param.setDescription("Période fiscale");
			param.setName(PERIODE_FISCALE);
			param.setMandatory(true);
			param.setType(new JobParamInteger());
			addParameterDefinition(param, RegDate.get().year() - 1);
		}
		{
			final JobParam param = new JobParam();
			param.setDescription("Nombre de threads");
			param.setName(NB_THREADS);
			param.setMandatory(true);
			param.setType(new JobParamInteger());
			addParameterDefinition(param, 4);
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

	public void setRapportService(RapportService rapportService) {
		this.rapportService = rapportService;
	}

	public void setQuestionnaireService(QuestionnaireSNCService questionnaireService) {
		this.questionnaireService = questionnaireService;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		super.afterPropertiesSet();
		getParameterDefinition(DATE_TRAITEMENT).setEnabled(isTesting());
	}

	@Override
	protected void doExecute(Map<String, Object> params) throws Exception {
		final int pf = getIntegerValue(params, PERIODE_FISCALE);
		final int nbThreads = getIntegerValue(params, NB_THREADS);
		final RegDate dateTraitement = getDateTraitement(params);

		final DeterminationQuestionnairesSNCResults results = questionnaireService.determineQuestionnairesAEmettre(pf, dateTraitement, nbThreads, getStatusManager());
		final DeterminationQuestionnairesSNCRapport rapport = rapportService.generateRapport(results, getStatusManager());
		setLastRunReport(rapport);

		Audit.success("La détermination des questionnaires SNC à envoyer pour l'année " + pf + " à la date du "
				              + RegDateHelper.dateToDisplayString(dateTraitement) + " est terminée.", rapport);
	}
}
