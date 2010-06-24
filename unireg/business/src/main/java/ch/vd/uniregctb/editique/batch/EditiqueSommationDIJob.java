package ch.vd.uniregctb.editique.batch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.declaration.ordinaire.DeclarationImpotService;
import ch.vd.uniregctb.declaration.ordinaire.EnvoiSommationsDIsResults;
import ch.vd.uniregctb.document.EnvoiSommationsDIsRapport;
import ch.vd.uniregctb.rapport.RapportService;
import ch.vd.uniregctb.scheduler.JobDefinition;
import ch.vd.uniregctb.scheduler.JobParam;
import ch.vd.uniregctb.scheduler.JobParamBoolean;
import ch.vd.uniregctb.scheduler.JobParamInteger;
import ch.vd.uniregctb.scheduler.JobParamRegDate;

public class EditiqueSommationDIJob extends JobDefinition {

	public static final String NAME = "EditiqueSommationDIJob";
	private static final String CATEGORIE = "DI";

	public static final String PARAM_MISE_SS_PLI  = "PARAM_MISE_SS_PLI";
	public static final String PARAM_NB_MAX_SOMMATIONS  = "PARAM_NB_MAX_SOMMATIONS";

	private DeclarationImpotService declarationImpotService;
	private RapportService rapportService;

	private static List<JobParam> params ;

	private static final HashMap<String, Object> defaultParams;

	static {
		params = new ArrayList<JobParam>() ;

		JobParam param = new JobParam();
		param.setDescription("Mise sous pli automatique impossible");
		param.setName(PARAM_MISE_SS_PLI);
		param.setMandatory(false);
		param.setType(new JobParamBoolean());
		params.add(param);

		param = new JobParam();
		param.setDescription("Nombre maximal de sommations émises (0 = pas de limite)");
		param.setName(PARAM_NB_MAX_SOMMATIONS);
		param.setMandatory(false);
		param.setType(new JobParamInteger());
		params.add(param);

		param = new JobParam();
		param.setDescription("Date de traitement");
		param.setName(DATE_TRAITEMENT);
		param.setMandatory(false);
		param.setType(new JobParamRegDate());
		params.add(param);

		defaultParams = new HashMap<String, Object>();
		{
			//RegDate today = RegDate.get();
			//defaultParams.put(DATE_TRAITEMENT, RegDateHelper.dateToDashString(today));
			defaultParams.put(PARAM_MISE_SS_PLI, Boolean.FALSE);
			defaultParams.put(PARAM_NB_MAX_SOMMATIONS, 20000);
		}
	}

	public EditiqueSommationDIJob(int sortOrder) {
		this(sortOrder, defaultParams);
	}

	public EditiqueSommationDIJob(int sortOrder, HashMap<String, Object> defaultParams) {
		super(NAME, CATEGORIE, sortOrder, "Imprimer les sommations des déclarations d'impôt", params, defaultParams);
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		super.afterPropertiesSet();
		params.get(2).setEnabled(isTesting());
	}

	@Override
	protected void doExecute(HashMap<String, Object> params) throws Exception {
		final RegDate dateTraitment = getDateTraitement(params);
		Boolean miseSousPliAutomatiqueImpossible = ((Boolean)params.get(PARAM_MISE_SS_PLI));
		Integer nombreMax = ((Integer)params.get(PARAM_NB_MAX_SOMMATIONS));
		EnvoiSommationsDIsResults results = declarationImpotService.envoyerSommations(dateTraitment, miseSousPliAutomatiqueImpossible, nombreMax, getStatusManager());
		if (results == null) {
			Audit.error( String.format(
					"L'envoi en masse des sommations DIs  pour le %s a échoué"
					, RegDateHelper.dateToDisplayString(dateTraitment)
			));
			return;
		}
		EnvoiSommationsDIsRapport rapport = rapportService.generateRapport(results, getStatusManager());
		setLastRunReport(rapport);
		Audit.success(
				"L'envoi en masse des sommations DIs  pour le "
				+ RegDateHelper.dateToDisplayString(dateTraitment) +
				" est terminée.", rapport);
	}

	@Override
	protected HashMap<String, Object> createDefaultParams() {
		return defaultParams;
	}

	public void setDeclarationImpotService(DeclarationImpotService declarationImpotService) {
		this.declarationImpotService = declarationImpotService;
	}

	public void setRapportService(RapportService rapportService) {
		this.rapportService = rapportService;
	}

}
