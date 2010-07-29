package ch.vd.uniregctb.evenement.externe;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.document.IdentifierContribuableRapport;
import ch.vd.uniregctb.document.TraiterEvenementExterneRapport;
import ch.vd.uniregctb.rapport.RapportService;
import ch.vd.uniregctb.scheduler.JobDefinition;
import ch.vd.uniregctb.scheduler.JobParam;
import ch.vd.uniregctb.scheduler.JobParamInteger;

public class EvenementExterneHandlerJob extends JobDefinition {

	public static String NAME = "EvenementExterneHandlerJob";
	private static final String CATEGORIE = "Events";



	EvenementExterneProcessor evenementExterneProcessor;
		private RapportService rapportService;
		public static final String NB_THREADS = "NB_THREADS";

	private static final List<JobParam> params;

	private static final HashMap<String, Object> defaultParams;
	static {
		params = new ArrayList<JobParam>();
		{

			JobParam param1 = new JobParam();
			param1.setDescription("Nombre de threads");
			param1.setName(NB_THREADS);
			param1.setMandatory(true);
			param1.setType(new JobParamInteger());
			params.add(param1);
		}

		defaultParams = new HashMap<String, Object>();
		{
			defaultParams.put(NB_THREADS, 4);

		}
	}

		public EvenementExterneHandlerJob(int sortOrder, String description) {
		this(sortOrder, description, defaultParams);
	}

	public EvenementExterneHandlerJob(int sortOrder, String description, HashMap<String, Object> defaultParams) {
		super(NAME, CATEGORIE, sortOrder, description, params, defaultParams);
	}

	@Override
		public void afterPropertiesSet() throws Exception {
			super.afterPropertiesSet();
		}


	@Override
	protected void doExecute(HashMap<String, Object> params) throws Exception {

final int nbThreads = getIntegerValue(params, NB_THREADS);
		if (nbThreads <= 0) {
			throw new RuntimeException("Le nombre de threads doit être un entier positif.");
		}

		final RegDate dateTraitement = getDateTraitement(params);
		

		final StatusManager status = getStatusManager();

		// Validation/Traitement des événements externes
		status.setMessage("traitement des événements externes...");
		TraiterEvenementExterneResult results = evenementExterneProcessor.traiteEvenementExterne(dateTraitement, nbThreads,status);
		final TraiterEvenementExterneRapport rapport = rapportService.generateRapport(results, status);
		setLastRunReport(rapport);
		Audit.success("La relance du traitement des événements externes à la date du "
				+ RegDateHelper.dateToDisplayString(dateTraitement) + " est terminée.", rapport);
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setEvenementExterneProcessor(EvenementExterneProcessor evenementExterneProcessor) {
		this.evenementExterneProcessor = evenementExterneProcessor;
	}
		public void setRapportService(RapportService rapportService) {
		this.rapportService = rapportService;
	}
}