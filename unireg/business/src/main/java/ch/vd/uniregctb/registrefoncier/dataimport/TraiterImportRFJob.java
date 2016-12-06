package ch.vd.uniregctb.registrefoncier.dataimport;

import java.util.HashMap;
import java.util.Map;

import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.document.MutationsRFDetectorRapport;
import ch.vd.uniregctb.rapport.RapportService;
import ch.vd.uniregctb.scheduler.JobCategory;
import ch.vd.uniregctb.scheduler.JobDefinition;
import ch.vd.uniregctb.scheduler.JobParam;
import ch.vd.uniregctb.scheduler.JobParamBoolean;
import ch.vd.uniregctb.scheduler.JobParamInteger;
import ch.vd.uniregctb.scheduler.JobParamLong;

/**
 * Job de traitement d'un import des immeubles du registre foncier
 */
public class TraiterImportRFJob extends JobDefinition {

	public static final String NAME = "TraiterImportRFJob";
	public static final String ID = "eventId";
	public static final String NB_THREADS = "NB_THREADS";
	public static final String CONTINUE_WITH_MUTATIONS_JOB = "CONTINUE_WITH_MUTATIONS_JOB";

	private MutationsRFDetector mutationsDetector;
	private RapportService rapportService;

	public void setMutationsDetector(MutationsRFDetector mutationsDetector) {
		this.mutationsDetector = mutationsDetector;
	}

	public void setRapportService(RapportService rapportService) {
		this.rapportService = rapportService;
	}

	public TraiterImportRFJob(int sortOrder, String description) {
		super(NAME, JobCategory.RF, sortOrder, description);

		final JobParam param1 = new JobParam();
		param1.setDescription("Id de l'événement (EvenementRFImport)");
		param1.setName(ID);
		param1.setMandatory(true);
		param1.setType(new JobParamLong());
		addParameterDefinition(param1, null);

		final JobParam param2 = new JobParam();
		param2.setDescription("Nombre de threads");
		param2.setName(NB_THREADS);
		param2.setMandatory(true);
		param2.setType(new JobParamInteger());
		addParameterDefinition(param2, 8);

		final JobParam param3 = new JobParam();
		param3.setDescription("Continuer avec le job de traitement des mutations");
		param3.setName(CONTINUE_WITH_MUTATIONS_JOB);
		param3.setMandatory(true);
		param3.setType(new JobParamBoolean());
		addParameterDefinition(param3, true);
	}

	@Override
	protected void doExecute(Map<String, Object> params) throws Exception {

		final long importId = getLongValue(params, ID);
		final int nbThreads = getStrictlyPositiveIntegerValue(params, NB_THREADS);
		final boolean startMutationJob = getBooleanValue(params, CONTINUE_WITH_MUTATIONS_JOB);

		// démarrage de l'import
		final MutationsRFDetectorResults results = mutationsDetector.run(importId, nbThreads, getStatusManager());
		final MutationsRFDetectorRapport rapport = rapportService.generateRapport(results, getStatusManager());
		setLastRunReport(rapport);
		Audit.success("L'import des immeubles RF (détection des mutations) est terminé.", rapport);

		// si demandé, on démarre le job de traitement des mutations
		if (startMutationJob) {
			final Map<String, Object> mutParams = new HashMap<>();
			mutParams.put(TraiterMutationsRFJob.ID, importId);
			mutParams.put(TraiterMutationsRFJob.NB_THREADS, nbThreads);
			mutParams.put(TraiterMutationsRFJob.CONTINUE_WITH_IDENTIFICATION_JOB, Boolean.TRUE);
			batchScheduler.startJob(TraiterMutationsRFJob.NAME, mutParams);
		}
	}
}
