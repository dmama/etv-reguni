package ch.vd.unireg.registrefoncier.rattrapage;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import ch.vd.unireg.document.RattraperDatesMetierDroitProcessorRapport;
import ch.vd.unireg.rapport.RapportService;
import ch.vd.unireg.scheduler.JobCategory;
import ch.vd.unireg.scheduler.JobDefinition;
import ch.vd.unireg.scheduler.JobParam;
import ch.vd.unireg.scheduler.JobParamEnum;
import ch.vd.unireg.scheduler.JobParamInteger;
import ch.vd.unireg.scheduler.JobParamString;

public class RattraperDatesMetierDroitRFJob extends JobDefinition {

	//private static final Logger LOGGER = LoggerFactory.getLogger(RattraperDatesMetierDroitRFJob.class);

	public static final String NAME = "RattraperDatesMetierDroitRFJob";
	public static final String DATA_SELECTION = "DATA_SELECTION";
	public static final String IMMEUBLE_IDS = "IMMEUBLE_IDS";
	public static final String NB_THREADS = "NB_THREADS";

	private RattraperDatesMetierDroitRFProcessor processor;
	private RapportService rapportService;

	public RattraperDatesMetierDroitRFJob(int sortOrder, String description) {
		super(NAME, JobCategory.RF, sortOrder, description);

		final JobParam param1 = new JobParam();
		param1.setDescription("Immeubles concernés");
		param1.setName(DATA_SELECTION);
		param1.setMandatory(true);
		param1.setType(new JobParamEnum(RattrapageDataSelection.class));
		addParameterDefinition(param1, RattrapageDataSelection.ALL);

		final JobParam param2 = new JobParam();
		param2.setDescription("Ids des immeubles (sélection explicite)");
		param2.setName(IMMEUBLE_IDS);
		param2.setMandatory(false);
		param2.setType(new JobParamString());
		addParameterDefinition(param2, null);

		final JobParam param3 = new JobParam();
		param3.setDescription("Nombre de threads");
		param3.setName(NB_THREADS);
		param3.setMandatory(true);
		param3.setType(new JobParamInteger());
		addParameterDefinition(param3, 8);
	}

	@Override
	protected void doExecute(Map<String, Object> params) throws Exception {

		final int nbThreads = getStrictlyPositiveIntegerValue(params, NB_THREADS);
		final RattrapageDataSelection dataSelection = getEnumValue(params, DATA_SELECTION, RattrapageDataSelection.class);

		final List<Long> immeubleIds;
		if (dataSelection == RattrapageDataSelection.EXPLICIT_SELECTION) {
			immeubleIds = Arrays.stream(getStringValue(params, IMMEUBLE_IDS).split("[, ]"))
					.map(Long::valueOf)
					.collect(Collectors.toList());
		}
		else {
			immeubleIds = null;
		}

		// démarrage du rattrapage
		final RattraperDatesMetierDroitRFProcessorResults results = processor.process(dataSelection, immeubleIds, nbThreads, getStatusManager());
		final RattraperDatesMetierDroitProcessorRapport rapport = rapportService.generateRapport(results, getStatusManager());
		setLastRunReport(rapport);
		audit.success("Le rattrapage des dates métier des droits RF est terminé.", rapport);
	}

	public void setProcessor(RattraperDatesMetierDroitRFProcessor processor) {
		this.processor = processor;
	}

	public void setRapportService(RapportService rapportService) {
		this.rapportService = rapportService;
	}
}
