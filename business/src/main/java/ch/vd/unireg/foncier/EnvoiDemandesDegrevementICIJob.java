package ch.vd.uniregctb.foncier;

import java.util.Map;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.document.EnvoiFormulairesDemandeDegrevementICIRapport;
import ch.vd.uniregctb.documentfiscal.AutreDocumentFiscalService;
import ch.vd.uniregctb.rapport.RapportService;
import ch.vd.uniregctb.scheduler.JobCategory;
import ch.vd.uniregctb.scheduler.JobDefinition;
import ch.vd.uniregctb.scheduler.JobParam;
import ch.vd.uniregctb.scheduler.JobParamInteger;
import ch.vd.uniregctb.scheduler.JobParamRegDate;

public class EnvoiDemandesDegrevementICIJob extends JobDefinition {

	private static final String NAME = "EnvoiDemandesDegrevementICIJob";
	private static final String NB_THREADS = "NB_THREADS";
	private static final String NB_MAX_ENVOIS = "NB_MAX_ENVOIS";

	private AutreDocumentFiscalService autreDocumentFiscalService;
	private RapportService rapportService;

	public EnvoiDemandesDegrevementICIJob(int sortOrder, String description) {
		super(NAME, JobCategory.DD, sortOrder, description);
		{
			final JobParam param = new JobParam();
			param.setDescription("Nombre de threads du traitement");
			param.setName(NB_THREADS);
			param.setMandatory(true);
			param.setType(new JobParamInteger());
			addParameterDefinition(param, 2);
		}
		{
			final JobParam param = new JobParam();
			param.setDescription("Nombre maximal d'envois");
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

	public void setAutreDocumentFiscalService(AutreDocumentFiscalService autreDocumentFiscalService) {
		this.autreDocumentFiscalService = autreDocumentFiscalService;
	}

	public void setRapportService(RapportService rapportService) {
		this.rapportService = rapportService;
	}

	@Override
	protected void doExecute(Map<String, Object> params) throws Exception {
		final int nbThreads = getPositiveIntegerValue(params, NB_THREADS);
		final Integer nbMaxEnvois = getOptionalPositiveIntegerValue(params, NB_MAX_ENVOIS);
		final RegDate dateTraitement = getDateTraitement(params);

		final EnvoiFormulairesDemandeDegrevementICIResults results = autreDocumentFiscalService.envoyerFormulairesDemandeDegrevementICIEnMasse(dateTraitement, nbThreads, nbMaxEnvois, getStatusManager());
		final EnvoiFormulairesDemandeDegrevementICIRapport rapport = rapportService.generateRapport(results, getStatusManager());
		setLastRunReport(rapport);

		Audit.success("L'envoi en masse des formulaires de demande de dégrèvement ICI est terminé.", rapport);
	}
}
