package ch.vd.uniregctb.registrefoncier.dataimport;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.document.MutationsRFProcessorRapport;
import ch.vd.uniregctb.rapport.RapportService;
import ch.vd.uniregctb.registrefoncier.RapprocherTiersRFJob;
import ch.vd.uniregctb.scheduler.JobCategory;
import ch.vd.uniregctb.scheduler.JobDefinition;
import ch.vd.uniregctb.scheduler.JobParam;
import ch.vd.uniregctb.scheduler.JobParamBoolean;
import ch.vd.uniregctb.scheduler.JobParamInteger;
import ch.vd.uniregctb.scheduler.JobParamLong;

/**
 * Job de traitement des mutations du registre foncier
 */
public class TraiterMutationsRFJob extends JobDefinition {

	private static final Logger LOGGER = LoggerFactory.getLogger(TraiterMutationsRFJob.class);

	public static final String NAME = "TraiterMutationsRFJob";
	public static final String ID = "eventId";
	public static final String NB_THREADS = "NB_THREADS";
	public static final String CONTINUE_WITH_DATES_FIN_JOB = "CONTINUE_WITH_DATES_FIN_JOB";
	public static final String CONTINUE_WITH_IDENTIFICATION_JOB = "CONTINUE_WITH_IDENTIFICATION_JOB";

	private MutationsRFProcessor processor;
	private RapportService rapportService;

	public void setProcessor(MutationsRFProcessor processor) {
		this.processor = processor;
	}

	public void setRapportService(RapportService rapportService) {
		this.rapportService = rapportService;
	}

	public TraiterMutationsRFJob(int sortOrder, String description) {
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
		param3.setDescription("Continuer avec le job de calcul des dates de fin des droits");
		param3.setName(CONTINUE_WITH_DATES_FIN_JOB);
		param3.setMandatory(true);
		param3.setType(new JobParamBoolean());
		addParameterDefinition(param3, true);

		final JobParam param4 = new JobParam();
		param4.setDescription("Continuer avec le job de rapprochement des propriétaires");
		param4.setName(CONTINUE_WITH_IDENTIFICATION_JOB);
		param4.setMandatory(true);
		param4.setType(new JobParamBoolean());
		addParameterDefinition(param4, true);
	}

	@Override
	protected void doExecute(Map<String, Object> params) throws Exception {

		final long importId = getLongValue(params, ID);
		final int nbThreads = getStrictlyPositiveIntegerValue(params, NB_THREADS);
		final boolean startDatesFinJob = getBooleanValue(params, CONTINUE_WITH_DATES_FIN_JOB);
		final boolean startRapprochementJob = getBooleanValue(params, CONTINUE_WITH_IDENTIFICATION_JOB);

		final StatusManager statusManager = getStatusManager();

		// on traite les mutations
		statusManager.setMessage("Traitement des mutations...");
		final MutationsRFProcessorResults results = processor.processImport(importId, nbThreads, statusManager);
		final MutationsRFProcessorRapport rapport = rapportService.generateRapport(results, getStatusManager());
		setLastRunReport(rapport);
		Audit.success("Le traitement de l'import RF (traitement des mutations) est terminé.", rapport);

		// si demandé, on démarre le job de calcul des dates de fin
		if (startDatesFinJob) {
			final Map<String, Object> datesFinParams = new HashMap<>();
			datesFinParams.put(TraiterFinsDeDroitsRFJob.NB_THREADS, nbThreads);
			batchScheduler.startJob(TraiterFinsDeDroitsRFJob.NAME, datesFinParams);
		}

		// si demandé, on démarre le job de rapprochement des propriétaires
		if (startRapprochementJob) {
			final Map<String, Object> rapprochementParams = new HashMap<>();
			rapprochementParams.put(RapprocherTiersRFJob.NB_THREADS, nbThreads);
			batchScheduler.startJob(RapprocherTiersRFJob.NAME, rapprochementParams);
		}

		statusManager.setMessage("Traitement terminé.");
	}
}
