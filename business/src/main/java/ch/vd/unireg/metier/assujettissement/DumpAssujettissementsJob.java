package ch.vd.unireg.metier.assujettissement;

import java.io.FileWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.registre.base.date.DateRange;
import ch.vd.shared.batchtemplate.BatchCallback;
import ch.vd.shared.batchtemplate.Behavior;
import ch.vd.shared.batchtemplate.SimpleProgressMonitor;
import ch.vd.unireg.audit.Audit;
import ch.vd.unireg.common.AuthenticationInterface;
import ch.vd.unireg.common.ParallelBatchTransactionTemplate;
import ch.vd.unireg.common.StatusManager;
import ch.vd.unireg.hibernate.HibernateTemplate;
import ch.vd.unireg.scheduler.JobCategory;
import ch.vd.unireg.scheduler.JobDefinition;
import ch.vd.unireg.scheduler.JobParam;
import ch.vd.unireg.scheduler.JobParamInteger;
import ch.vd.unireg.scheduler.JobParamMultiSelectEnum;
import ch.vd.unireg.scheduler.JobParamString;
import ch.vd.unireg.tiers.Contribuable;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.tiers.TypeTiers;

/**
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class DumpAssujettissementsJob extends JobDefinition {

	//private final Logger LOGGER = LoggerFactory.getLogger(DumpAssujettissementsJob.class);

	public static final String NAME = "DumpAssujettissementsJob";

	public static final String FILENAME = "FILENAME";
	public static final String NB_THREADS = "NB_THREADS";
	public static final String POPULATION = "POPULATION";

	private HibernateTemplate hibernateTemplate;
	private PlatformTransactionManager transactionManager;
	private AssujettissementService assujettissementService;

	public DumpAssujettissementsJob(int sortOrder, String description) {
		super(NAME, JobCategory.DEBUG, sortOrder, description);

		final JobParam param0 = new JobParam();
		param0.setDescription("Fichier de sortie (local au serveur)");
		param0.setName(FILENAME);
		param0.setMandatory(true);
		param0.setType(new JobParamString());
		addParameterDefinition(param0, null);

		final JobParam param1 = new JobParam();
		param1.setDescription("Nombre de threads");
		param1.setName(NB_THREADS);
		param1.setMandatory(true);
		param1.setType(new JobParamInteger());
		addParameterDefinition(param1, 8);

		final JobParam param2 = new JobParam();
		param2.setDescription("Population");
		param2.setName(POPULATION);
		param2.setMandatory(true);
		param2.setType(new JobParamMultiSelectEnum(TypeTiers.class));
		addParameterDefinition(param2, Arrays.asList(TypeTiers.values()));
	}

	@Override
	public boolean isVisible() {
		return isTesting();
	}

	@Override
	protected void doExecute(Map<String, Object> params) throws Exception {

		final StatusManager statusManager = getStatusManager();

		final String filename = getStringValue(params, FILENAME);
		final int nbThreads = getStrictlyPositiveIntegerValue(params, NB_THREADS);
		final List<TypeTiers> typesTiers = getMultiSelectEnumValue(params, POPULATION, TypeTiers.class);

		// Chargement des ids des contribuables à processer
		statusManager.setMessage("Chargement des ids des tiers à analyser...");
		final List<Long> ids = getTiersIds(typesTiers, statusManager);

		try (FileWriter file = new FileWriter(filename)) {
			processAll(ids, nbThreads, file, statusManager);
		}

		statusManager.setMessage("Terminé");
		Audit.success("Le batch de dump des assujettissements est terminé");
	}

	private void processAll(List<Long> ids, int nbThreads, final FileWriter file, final StatusManager statusManager) {

		final SimpleProgressMonitor progressMonitor = new SimpleProgressMonitor();
		final ParallelBatchTransactionTemplate<Long> template =
				new ParallelBatchTransactionTemplate<>(ids, 100, nbThreads, Behavior.SANS_REPRISE, transactionManager, statusManager, AuthenticationInterface.INSTANCE);
		template.setReadonly(true);
		template.execute(new BatchCallback<Long>() {
			@Override
			public boolean doInTransaction(List<Long> batch) throws Exception {
				statusManager.setMessage("Traitement du lot [" + batch.get(0) + "; " + batch.get(batch.size() - 1) + "] ...", progressMonitor.getProgressInPercent());
				for (Long id : batch) {
					String line;
					try {
						line = String.valueOf(id) + ';' + process(id) + '\n';
					}
					catch (Exception e) {
						line = "exception:" + e.getMessage() + '\n';
					}
					file.write(line);
				}
				return true;
			}
		}, progressMonitor);
	}

	private String process(Long id) {
		final Tiers tiers = hibernateTemplate.get(Tiers.class, id);
		if (tiers == null) {
			return "tiers not found";
		}
		if (!(tiers instanceof Contribuable)) {
			// par définition, seuls les contribuables sont assujettis
			return "non-assujetti";
		}
		final Contribuable ctb = (Contribuable) tiers;

		// on force l'initialisation des fors fiscaux, pour faciliter la lecture des performances avec JProfiler
		ctb.getForsFiscaux().size();

		final List<Assujettissement> list;
		try {
			list = assujettissementService.determine(ctb, (DateRange) null);
		}
		catch (Exception e) {
			return "assujettissement exception:" + e.getMessage();
		}

		if (list == null || list.isEmpty()) {
			return "non-assujetti";
		}

		final StringBuilder sb = new StringBuilder();
		for (Assujettissement a : list) {
			sb.append(a).append(';');
		}
		return sb.toString();
	}

	private List<Long> getTiersIds(@NotNull List<TypeTiers> typesTiers, final StatusManager statusManager) {

		if (typesTiers.isEmpty()) {
			throw new IllegalArgumentException("La liste des types de tiers est vide.");
		}

		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);

		final List<Long> ids = template.execute(status -> {
			//noinspection unchecked
			final Map<String, Object> params = new HashMap<>();
			params.put("classes", typesTiers.stream()
					.map(TypeTiers::getConcreteTiersClass)
					.map(Class::getSimpleName)
					.collect(Collectors.toList()));
			return hibernateTemplate.find("select t.numero from Tiers t where t.class in (:classes) order by t.numero asc", params, null);
		});

		statusManager.setMessage(String.format("%d tiers trouvés", ids.size()));
		return ids;
	}

	public void setHibernateTemplate(HibernateTemplate hibernateTemplate) {
		this.hibernateTemplate = hibernateTemplate;
	}

	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	public void setAssujettissementService(AssujettissementService assujettissementService) {
		this.assujettissementService = assujettissementService;
	}

	@Override
	protected boolean isWebStartableInProductionMode() {
		return true;
	}
}
