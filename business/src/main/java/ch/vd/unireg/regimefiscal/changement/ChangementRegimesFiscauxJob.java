package ch.vd.unireg.regimefiscal.changement;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.document.ChangementRegimesFiscauxRapport;
import ch.vd.unireg.rapport.RapportService;
import ch.vd.unireg.regimefiscal.rattrapage.RattrapageRegimesFiscauxJob;
import ch.vd.unireg.scheduler.JobCategory;
import ch.vd.unireg.scheduler.JobDefinition;
import ch.vd.unireg.scheduler.JobParam;
import ch.vd.unireg.scheduler.JobParamInteger;
import ch.vd.unireg.scheduler.JobParamRegDate;
import ch.vd.unireg.scheduler.JobParamString;

/**
 * Job de changement des régimes fiscaux (d'un type à l'autre) à une date donnée sur les entreprises existantes (FISCPROJ-90)
 */
public class ChangementRegimesFiscauxJob extends JobDefinition {

	public static final Logger LOGGER = LoggerFactory.getLogger(RattrapageRegimesFiscauxJob.class);

	public static final String NAME = "ChangementRegimesFiscauxJob";

	public static final String ANCIEN_CODE = "ANCIEN_CODE";
	public static final String NOUVEAU_CODE = "NOUVEAU_CODE";
	public static final String DATE_CHANGEMENT = "DATE_CHANGEMENT";
	public static final String NB_THREADS = "NB_THREADS";

	private ChangementRegimesFiscauxProcessor processor;
	private RapportService rapportService;

	public ChangementRegimesFiscauxJob(int sortOrder, String description) {
		super(NAME, JobCategory.TIERS, sortOrder, description);

		final JobParam param1 = new JobParam();
		param1.setMandatory(true);
		param1.setName(ANCIEN_CODE);
		param1.setDescription("Code de l'ancien type");
		param1.setType(new JobParamString());
		addParameterDefinition(param1, null);

		final JobParam param2 = new JobParam();
		param2.setMandatory(true);
		param2.setName(NOUVEAU_CODE);
		param2.setDescription("Code du nouveau type");
		param2.setType(new JobParamString());
		addParameterDefinition(param2, null);

		final JobParam param3 = new JobParam();
		param3.setMandatory(true);
		param3.setName(DATE_CHANGEMENT);
		param3.setDescription("Date de changement");
		param3.setType(new JobParamRegDate());
		addParameterDefinition(param3, null);

		final JobParam param4 = new JobParam();
		param4.setDescription("Nombre de threads");
		param4.setName(NB_THREADS);
		param4.setMandatory(true);
		param4.setType(new JobParamInteger());
		addParameterDefinition(param4, 8);
	}

	@Override
	protected void doExecute(Map<String, Object> params) throws Exception {

		final String ancienCode = getStringValue(params, ANCIEN_CODE);
		final String nouveauCode = getStringValue(params, NOUVEAU_CODE);
		final RegDate dateChangement = getRegDateValue(params, DATE_CHANGEMENT);
		final int nbThreads = getStrictlyPositiveIntegerValue(params, NB_THREADS);

		if (StringUtils.isBlank(ancienCode) || StringUtils.isBlank(nouveauCode) || dateChangement == null) {
			throw new IllegalArgumentException("Tous les arguments sont obligatoires");
		}

		final ChangementRegimesFiscauxJobResults results = processor.process(ancienCode, nouveauCode, dateChangement, nbThreads, getStatusManager());
		final ChangementRegimesFiscauxRapport rapport = rapportService.generateRapport(results, getStatusManager());
		setLastRunReport(rapport);
		audit.success("Le changement des régimes fiscaux est terminé.", rapport);

	}

	public void setProcessor(ChangementRegimesFiscauxProcessor processor) {
		this.processor = processor;
	}

	public void setRapportService(RapportService rapportService) {
		this.rapportService = rapportService;
	}
}
