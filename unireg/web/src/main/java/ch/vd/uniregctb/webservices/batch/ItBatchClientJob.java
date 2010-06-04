package ch.vd.uniregctb.webservices.batch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;

import ch.vd.registre.base.date.RegDate;
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

	private static final List<JobParam> params ;

	private static final HashMap<String, Object> defaultParams;

	static {
		params = new ArrayList<JobParam>();
		// Date debut
		{
			JobParam param = new JobParam();
			param.setDescription("Date de début");
			param.setName(PARAM_DATE_DEBUT);
			param.setMandatory(true);
			param.setType(new JobParamRegDate());
			params.add(param);
		}
		// Count
		{
			JobParam param = new JobParam();
			param.setDescription("Nombre d'occurences");
			param.setName(PARAM_COUNT);
			param.setMandatory(false);
			param.setType(new JobParamInteger());
			params.add(param);
		}
		// Duration
		{
			JobParam param = new JobParam();
			param.setDescription("Durée minimale (s)");
			param.setName(PARAM_DURATION);
			param.setMandatory(false);
			param.setType(new JobParamInteger());
			params.add(param);
		}
		// Shutdown Duration
		{
			JobParam param = new JobParam();
			param.setDescription("Durée minimale d'arrêt après interruption (s)");
			param.setName(PARAM_SHUTDOWN_DURATION);
			param.setMandatory(false);
			param.setType(new JobParamInteger());
			params.add(param);
		}
		// Salutations
		{
			JobParam param = new JobParam();
			param.setDescription("Salutations");
			param.setName(PARAM_SALUTATIONS);
			param.setMandatory(false);
			param.setType(new JobParamEnum(Salutations.class));
			params.add(param);
		}
		// Fichier joint
		{
			JobParam param = new JobParam();
			param.setDescription("Fichier joint");
			param.setName(PARAM_ATTACHEMENT);
			param.setMandatory(false);
			param.setType(new JobParamFile());
			params.add(param);
		}

		defaultParams = new HashMap<String, Object>();
		{
			defaultParams.put(PARAM_DATE_DEBUT, RegDate.get());
		}
	}

	public ItBatchClientJob(int sortOrder, HashMap<String, Object> defaultParams) {
		super(NAME, CATEGORIE, sortOrder, "IT - BatchClient testing job", params, defaultParams);
	}

	@Override
	protected void doExecute(HashMap<String, Object> params) throws Exception {

		// DATE_DEBUT
		final RegDate dateDebut = getRegDateValue(params, PARAM_DATE_DEBUT);
        // COUNT

		Integer count = (Integer)params.get(PARAM_COUNT);
		byte[] attachement = (byte[]) params.get(PARAM_ATTACHEMENT);
		if (attachement != null) {
			LOGGER.info("Contenu du fichier joint: \n" + new String(attachement) + "\n");
		}

		final Integer duration = (Integer) params.get(PARAM_DURATION);
		final Integer shutdown = (Integer) params.get(PARAM_SHUTDOWN_DURATION);

		if (duration != null) {
			final long inc = duration * 100;
			for (int i = 0; i < 10; i++) {
				Thread.sleep(inc);
				if (isInterrupted()) {
					break;
				}
			}
		}

		if (isInterrupted() && shutdown != null) {
			Thread.sleep(shutdown * 1000);
		}

        LOGGER.info("Date début : "+dateDebut);
        LOGGER.info("Count      : "+count);
	}

	@Override
	public HashMap<String, Object> getDefaultParams() {
		return defaultParams;
	}

	@Override
	public boolean isVisible() {
		return isTesting();
	}

}
