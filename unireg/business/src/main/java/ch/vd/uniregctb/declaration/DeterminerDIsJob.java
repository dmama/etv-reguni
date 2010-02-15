package ch.vd.uniregctb.declaration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.declaration.ordinaire.DeclarationImpotService;
import ch.vd.uniregctb.declaration.ordinaire.DeterminationDIsResults;
import ch.vd.uniregctb.document.DeterminationDIsRapport;
import ch.vd.uniregctb.rapport.RapportService;
import ch.vd.uniregctb.scheduler.JobDefinition;
import ch.vd.uniregctb.scheduler.JobParam;
import ch.vd.uniregctb.scheduler.JobParamInteger;
import ch.vd.uniregctb.scheduler.JobParamRegDate;

/**
 * Job qui détermine les déclaration d'impôts à envoyer et crée des tâches en instance.
 */
public class DeterminerDIsJob extends JobDefinition {

	private DeclarationImpotService service;
	private RapportService rapportService;

	public static final String NAME = "DetermineDIsEnMasseJob";
	private static final String CATEGORIE = "DI";

	public static final String PERIODE_FISCALE = "PERIODE";
	public static final String NB_THREADS = "NB_THREADS";

	private static final List<JobParam> params;

	private static final HashMap<String, Object> defaultParams;

	static {
		params = new ArrayList<JobParam>();
		{
			JobParam param0 = new JobParam();
			param0.setDescription("Période fiscale");
			param0.setName(PERIODE_FISCALE);
			param0.setMandatory(true);
			param0.setType(new JobParamInteger());
			params.add(param0);

			JobParam param1 = new JobParam();
			param1.setDescription("Nombre de threads");
			param1.setName(NB_THREADS);
			param1.setMandatory(true);
			param1.setType(new JobParamInteger());
			params.add(param1);

			JobParam param2 = new JobParam();
			param2.setDescription("Date de traitement");
			param2.setName(DATE_TRAITEMENT);
			param2.setMandatory(false);
			param2.setType(new JobParamRegDate());
			params.add(param2);
		}

		defaultParams = new HashMap<String, Object>();
		{
			RegDate today = RegDate.get();
			defaultParams.put(PERIODE_FISCALE, today.year() - 1);
			defaultParams.put(NB_THREADS, 4);
			//defaultParams.put(DATE_TRAITEMENT, RegDateHelper.dateToDashString(RegDate.get(today.year(), 1, 15)));
		}
	}


	public DeterminerDIsJob(int sortOrder, String description) {
		this(sortOrder, description, defaultParams);
	}

	public DeterminerDIsJob(int sortOrder, String description, HashMap<String, Object> defaultParams) {
		super(NAME, CATEGORIE, sortOrder, description, params, defaultParams);
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		super.afterPropertiesSet();
		params.get(1).setEnabled(isTesting());
	}

	@Override
	protected void doInitialize() {
		super.doInitialize();
	}

	public void setService(DeclarationImpotService service) {
		this.service = service;
	}

	public void setRapportService(RapportService rapportService) {
		this.rapportService = rapportService;
	}

	@Override
	protected void doExecute(HashMap<String, Object> params) throws Exception {

		// Récupération des paramètres
		final Integer annee = (Integer) params.get(PERIODE_FISCALE);
		if (annee == null) {
			throw new RuntimeException("La période fiscale doit être spécifiée.");
		}

		final int nbThreads = getIntegerValue(params, NB_THREADS);
		if (nbThreads <= 0) {
			throw new RuntimeException("Le nombre de threads doit être un entier positif.");
		}

		final RegDate dateTraitement = getDateTraitement(params);

		// Exécution du job dans une transaction.
		final StatusManager status = getStatusManager();
		final DeterminationDIsResults results = service.determineDIsAEmettre(annee, dateTraitement, nbThreads, status);
		final DeterminationDIsRapport rapport = rapportService.generateRapport(results, status);

		setLastRunReport(rapport);
		Audit.success("La détermination des DIs à envoyer pour l'année " + annee + " à la date du "
				+ RegDateHelper.dateToDisplayString(dateTraitement) + " est terminée.", rapport);
	}

	@Override
	protected HashMap<String, Object> createDefaultParams() {
		return defaultParams;
	}
}
