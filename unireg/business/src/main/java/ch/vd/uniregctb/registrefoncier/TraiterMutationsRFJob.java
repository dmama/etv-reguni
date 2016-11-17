package ch.vd.uniregctb.registrefoncier;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.shared.batchtemplate.StatusManager;
import ch.vd.uniregctb.scheduler.JobCategory;
import ch.vd.uniregctb.scheduler.JobDefinition;
import ch.vd.uniregctb.scheduler.JobParam;
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

	private DataRFMutationsProcessor processor;

	public void setProcessor(DataRFMutationsProcessor processor) {
		this.processor = processor;
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
	}

	@Override
	protected void doExecute(Map<String, Object> params) throws Exception {

		final long importId = getLongValue(params, ID);
		final int nbThreads = getStrictlyPositiveIntegerValue(params, NB_THREADS);
		final StatusManager statusManager = getStatusManager();

		// on traite les mutations
		statusManager.setMessage("Traitement des mutations...");
		processor.processImport(importId, nbThreads, statusManager);

		statusManager.setMessage("Traitement terminé.");
	}
}
