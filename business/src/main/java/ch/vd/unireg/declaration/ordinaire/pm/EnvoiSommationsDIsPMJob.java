package ch.vd.unireg.declaration.ordinaire.pm;

import java.util.Map;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.declaration.ordinaire.DeclarationImpotService;
import ch.vd.unireg.document.EnvoiSommationsDIsPMRapport;
import ch.vd.unireg.rapport.RapportService;
import ch.vd.unireg.scheduler.JobCategory;
import ch.vd.unireg.scheduler.JobDefinition;
import ch.vd.unireg.scheduler.JobParam;
import ch.vd.unireg.scheduler.JobParamInteger;
import ch.vd.unireg.scheduler.JobParamRegDate;

public class EnvoiSommationsDIsPMJob extends JobDefinition {

	private static final String NAME = "EnvoiSommationsDIsPMJob";
	private static final String PARAM_NB_MAX_SOMMATIONS = "NB_MAX_SOMMATIONS";

	private DeclarationImpotService service;
	private RapportService rapportService;

	public EnvoiSommationsDIsPMJob(int sortOrder, String description) {
		super(NAME, JobCategory.DI_PM, sortOrder, description);
		{
			final JobParam param = new JobParam();
			param.setDescription("Nombre maximal de sommations émises (0 ou vide = pas de limite)");
			param.setName(PARAM_NB_MAX_SOMMATIONS);
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

	public void setService(DeclarationImpotService service) {
		this.service = service;
	}

	public void setRapportService(RapportService rapportService) {
		this.rapportService = rapportService;
	}

	@Override
	protected void doExecute(Map<String, Object> params) throws Exception {
		final RegDate dateTraitement = getDateTraitement(params);
		final Integer nbEnvoisMax = getOptionalIntegerValue(params, PARAM_NB_MAX_SOMMATIONS);

		final EnvoiSommationsDIsPMResults results = service.envoyerSommationsPM(dateTraitement, nbEnvoisMax, getStatusManager());
		final EnvoiSommationsDIsPMRapport rapport = rapportService.generateRapport(results, getStatusManager());
		setLastRunReport(rapport);
		audit.success(String.format("L'envoi en masse des sommations DIs PM au %s est terminée.", RegDateHelper.dateToDisplayString(dateTraitement)), rapport);
	}
}
