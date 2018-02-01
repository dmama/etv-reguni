package ch.vd.unireg.metier.assujettissement;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
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
import ch.vd.unireg.scheduler.JobParamString;
import ch.vd.unireg.tiers.Contribuable;

/**
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class DumpAssujettissementsJob extends JobDefinition {

	//private final Logger LOGGER = LoggerFactory.getLogger(DumpAssujettissementsJob.class);

	public static final String NAME = "DumpAssujettissementsJob";

	public static final String FILENAME = "FILENAME";
	public static final String NB_THREADS = "NB_THREADS";

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
		addParameterDefinition(param1, 4);
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

		// Chargement des ids des contribuables à processer
		statusManager.setMessage("Chargement des ids de tous les contribuables...");
		final List<Long> ids = getCtbIds(statusManager);

		try (FileWriter file = new FileWriter(filename)) {
			processAll(ids, nbThreads, file, statusManager);
		}

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
						final StringBuilder sb = new StringBuilder();
						sb.append(id).append(';').append(process(id)).append('\n');
						line = sb.toString();
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

	private String process(Long id) throws IOException {
		final Contribuable ctb = hibernateTemplate.get(Contribuable.class, id);
		if (ctb == null) {
			return "contribuable not found";
		}

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

	@SuppressWarnings("unchecked")
	private List<Long> getCtbIds(final StatusManager statusManager) {
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);
		return template.execute(new TransactionCallback<List<Long>>() {
			@Override
			public List<Long> doInTransaction(TransactionStatus status) {
				status.setRollbackOnly();
				final List<Long> ids = hibernateTemplate.find("select cont.numero from Contribuable as cont order by cont.numero asc", null);
				statusManager.setMessage(String.format("%d contribuables trouvés", ids.size()));
				return ids;
			}
		});
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
