package ch.vd.unireg.indexer.jobs;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ch.vd.unireg.common.StatusManager;
import ch.vd.unireg.document.DatabaseIndexationRapport;
import ch.vd.unireg.indexer.tiers.GlobalTiersIndexer.Mode;
import ch.vd.unireg.rapport.RapportService;
import ch.vd.unireg.scheduler.JobCategory;
import ch.vd.unireg.scheduler.JobDefinition;
import ch.vd.unireg.scheduler.JobParam;
import ch.vd.unireg.scheduler.JobParamEnum;
import ch.vd.unireg.scheduler.JobParamInteger;
import ch.vd.unireg.scheduler.JobParamMultiSelectEnum;
import ch.vd.unireg.tiers.TypeTiers;

/**
 * Job qui réindexe tout ou partie des tiers de la base de données
 */
public class DatabaseIndexerJob extends JobDefinition {

	public static final String NAME = "DatabaseIndexerJob";

	public static final String I_NB_THREADS = "nbThreads";
	public static final String MODE = "mode";
	public static final String POPULATION = "population";

	private DatabaseIndexationProcessor processor;
	private RapportService rapportService;

	public DatabaseIndexerJob(int sortOrder, String description) {
		super(NAME, JobCategory.INDEXEUR, sortOrder, description);

		final JobParam param0 = new JobParam();
		param0.setDescription("Nombre de threads");
		param0.setName(I_NB_THREADS);
		param0.setMandatory(true);
		param0.setType(new JobParamInteger());
		addParameterDefinition(param0, 8);

		final JobParam param1 = new JobParam();
		param1.setDescription("Mode d'indexation");
		param1.setName(MODE);
		param1.setMandatory(true);
		param1.setType(new JobParamEnum(Mode.class));
		addParameterDefinition(param1, Mode.MISSING_ONLY);

		final JobParam param2 = new JobParam();
		param2.setDescription("Population");
		param2.setName(POPULATION);
		param2.setMandatory(false);
		param2.setType(new JobParamMultiSelectEnum(TypeTiers.class));
		addParameterDefinition(param2, Arrays.asList(TypeTiers.values()));
	}

	@Override
	public void doExecute(Map<String, Object> params) throws Exception {
		final int nbThreads = getStrictlyPositiveIntegerValue(params, I_NB_THREADS);
		final Mode mode = getEnumValue(params, MODE, Mode.class);
		final List<TypeTiers> population = getMultiSelectEnumValue(params, POPULATION, TypeTiers.class);
		final Set<TypeTiers> typesTiers = population.isEmpty() ? EnumSet.allOf(TypeTiers.class) : EnumSet.copyOf(population);    // [SIFISC-29781] pas de population spécifique renseignée -> on prends toute la population
		final StatusManager status = getStatusManager();

		audit.info("Indexation de la base de données (mode = " + mode + ", typesTiers = " + typesTiers + ")");

		final DatabaseIndexationResults results = processor.run(mode, typesTiers, nbThreads, status);
		final DatabaseIndexationRapport rapport = rapportService.generateRapport(results, status);

		setLastRunReport(rapport);

		audit.success("L'indexation de la base de données est terminée", rapport);
	}

	public void setProcessor(DatabaseIndexationProcessor processor) {
		this.processor = processor;
	}

	public void setRapportService(RapportService rapportService) {
		this.rapportService = rapportService;
	}
}
