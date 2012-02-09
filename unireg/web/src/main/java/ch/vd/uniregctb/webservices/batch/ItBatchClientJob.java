package ch.vd.uniregctb.webservices.batch;

import java.util.Map;

import org.apache.log4j.Logger;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.scheduler.JobDefinition;
import ch.vd.uniregctb.scheduler.JobParam;
import ch.vd.uniregctb.scheduler.JobParamEnum;
import ch.vd.uniregctb.scheduler.JobParamFile;
import ch.vd.uniregctb.scheduler.JobParamInteger;
import ch.vd.uniregctb.scheduler.JobParamRegDate;

public class ItBatchClientJob extends JobDefinition {

	private static final Logger LOGGER = Logger.getLogger(ItBatchClientJob.class);

	public static final String NAME = "IT-BatchClientJob";
	private static final String CATEGORIE = "Test";

	public static final String PARAM_DATE_DEBUT = "dateDebut";
	public static final String PARAM_COUNT = "count";
	public static final String PARAM_DURATION = "duration";
	public static final String PARAM_SHUTDOWN_DURATION = "shutdown_duration";
	public static final String PARAM_SALUTATIONS = "salutations"; // paramètre bidon pour tester les enums
	public static final String PARAM_ATTACHEMENT = "attachement"; // paramètre bidon pour tester les fichiers

	public static enum Salutations {
		HELLO,
		COUCOU,
		BONJOUR
	}

	public ItBatchClientJob(int sortOrder, String description) {
		super(NAME, CATEGORIE, sortOrder, description);

		// Date debut
		{
			final JobParam param = new JobParam();
			param.setDescription("Date de début");
			param.setName(PARAM_DATE_DEBUT);
			param.setMandatory(false);
			param.setType(new JobParamRegDate());
			addParameterDefinition(param, RegDate.get());
		}
		// Count
		{
			final JobParam param = new JobParam();
			param.setDescription("Nombre d'occurences");
			param.setName(PARAM_COUNT);
			param.setMandatory(false);
			param.setType(new JobParamInteger());
			addParameterDefinition(param, 42);
		}
		// Duration
		{
			final JobParam param = new JobParam();
			param.setDescription("Durée minimale (s)");
			param.setName(PARAM_DURATION);
			param.setMandatory(false);
			param.setType(new JobParamInteger());
			addParameterDefinition(param, null);
		}
		// Shutdown Duration
		{
			final JobParam param = new JobParam();
			param.setDescription("Durée minimale d'arrêt après interruption (s)");
			param.setName(PARAM_SHUTDOWN_DURATION);
			param.setMandatory(false);
			param.setType(new JobParamInteger());
			addParameterDefinition(param, null);
		}
		// Salutations
		{
			final JobParam param = new JobParam();
			param.setDescription("Salutations");
			param.setName(PARAM_SALUTATIONS);
			param.setMandatory(false);
			param.setType(new JobParamEnum(Salutations.class));
			addParameterDefinition(param, null);
		}
		// Fichier joint
		{
			final JobParam param = new JobParam();
			param.setDescription("Fichier joint");
			param.setName(PARAM_ATTACHEMENT);
			param.setMandatory(false);
			param.setType(new JobParamFile());
			addParameterDefinition(param, null);
		}
	}

	@Override
	protected void doExecute(Map<String, Object> params) throws Exception {

		// DATE_DEBUT
		final RegDate dateDebut = getRegDateValue(params, PARAM_DATE_DEBUT);
		// COUNT

		final Integer count = getOptionalIntegerValue(params, PARAM_COUNT);
		final byte[] attachement = getFileContent(params, PARAM_ATTACHEMENT);
		if (attachement != null) {
			LOGGER.info("Contenu du fichier joint: \n" + new String(attachement) + '\n');
		}

		final Integer duration = getOptionalIntegerValue(params, PARAM_DURATION);
		final Integer shutdown = getOptionalIntegerValue(params, PARAM_SHUTDOWN_DURATION);
		final StatusManager status = getStatusManager();

		if (duration != null) {
			final long inc = duration * 10;
			for (int i = 0; i < 100; i++) {
				status.setMessage("working...", i);
				Thread.sleep(inc);
				if (isInterrupted()) {
					break;
				}
			}
		}

		if (isInterrupted() && shutdown != null) {
			Thread.sleep(shutdown * 1000);
		}
		status.setMessage("done");

		LOGGER.info("Date début : " + dateDebut);
		LOGGER.info("Count      : " + count);
	}

	@Override
	public boolean isVisible() {
		return isTesting();
	}
}
