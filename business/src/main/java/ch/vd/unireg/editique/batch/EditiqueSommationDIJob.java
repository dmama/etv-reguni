package ch.vd.unireg.editique.batch;

import java.util.Map;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.declaration.ordinaire.DeclarationImpotService;
import ch.vd.unireg.declaration.ordinaire.pp.EnvoiSommationsDIsPPResults;
import ch.vd.unireg.document.EnvoiSommationsDIsPPRapport;
import ch.vd.unireg.rapport.RapportService;
import ch.vd.unireg.scheduler.JobCategory;
import ch.vd.unireg.scheduler.JobDefinition;
import ch.vd.unireg.scheduler.JobParam;
import ch.vd.unireg.scheduler.JobParamBoolean;
import ch.vd.unireg.scheduler.JobParamInteger;
import ch.vd.unireg.scheduler.JobParamRegDate;

public class EditiqueSommationDIJob extends JobDefinition {

	public static final String NAME = "EditiqueSommationDIJob";
	public static final String PARAM_MISE_SS_PLI  = "PARAM_MISE_SS_PLI";
	public static final String PARAM_NB_MAX_SOMMATIONS  = "PARAM_NB_MAX_SOMMATIONS";

	private DeclarationImpotService declarationImpotService;
	private RapportService rapportService;

	public EditiqueSommationDIJob(int sortOrder) {
		super(NAME, JobCategory.DI_PP, sortOrder, "Imprimer les sommations des déclarations d'impôt");

		final JobParam param0 = new JobParam();
		param0.setDescription("Mise sous pli automatique impossible");
		param0.setName(PARAM_MISE_SS_PLI);
		param0.setMandatory(true);
		param0.setType(new JobParamBoolean());
		addParameterDefinition(param0, Boolean.FALSE);

		final JobParam param1 = new JobParam();
		param1.setDescription("Nombre maximal de sommations émises (0 = pas de limite)");
		param1.setName(PARAM_NB_MAX_SOMMATIONS);
		param1.setMandatory(false);
		param1.setType(new JobParamInteger());
		addParameterDefinition(param1, 20000);

		final JobParam param2 = new JobParam();
		param2.setDescription("Date de traitement");
		param2.setName(DATE_TRAITEMENT);
		param2.setMandatory(false);
		param2.setType(new JobParamRegDate());
		addParameterDefinition(param2, null);
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		super.afterPropertiesSet();
		getParameterDefinition(DATE_TRAITEMENT).setEnabled(isTesting());
	}

	@Override
	protected void doExecute(Map<String, Object> params) throws Exception {
		final RegDate dateTraitment = getDateTraitement(params);
		final boolean miseSousPliAutomatiqueImpossible = getBooleanValue(params, PARAM_MISE_SS_PLI);
		final Integer nombreMax = getOptionalIntegerValue(params, PARAM_NB_MAX_SOMMATIONS);
		final EnvoiSommationsDIsPPResults results = declarationImpotService.envoyerSommationsPP(dateTraitment, miseSousPliAutomatiqueImpossible, nombreMax == null ? 0 : nombreMax, getStatusManager());
		if (results == null) {
			audit.error( String.format(
					"L'envoi en masse des sommations DIs  pour le %s a échoué"
					, RegDateHelper.dateToDisplayString(dateTraitment)
			));
			return;
		}

		final EnvoiSommationsDIsPPRapport rapport = rapportService.generateRapport(results, getStatusManager());
		setLastRunReport(rapport);
		audit.success(
				"L'envoi en masse des sommations DIs  pour le "
				+ RegDateHelper.dateToDisplayString(dateTraitment) +
				" est terminée.", rapport);
	}

	public void setDeclarationImpotService(DeclarationImpotService declarationImpotService) {
		this.declarationImpotService = declarationImpotService;
	}

	public void setRapportService(RapportService rapportService) {
		this.rapportService = rapportService;
	}
}
