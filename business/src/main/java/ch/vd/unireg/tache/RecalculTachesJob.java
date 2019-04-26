package ch.vd.unireg.tache;

import java.util.Map;

import org.springframework.transaction.PlatformTransactionManager;

import ch.vd.unireg.document.RecalculTachesRapport;
import ch.vd.unireg.hibernate.HibernateTemplate;
import ch.vd.unireg.rapport.RapportService;
import ch.vd.unireg.scheduler.JobCategory;
import ch.vd.unireg.scheduler.JobDefinition;
import ch.vd.unireg.scheduler.JobParam;
import ch.vd.unireg.scheduler.JobParamBoolean;
import ch.vd.unireg.scheduler.JobParamEnum;
import ch.vd.unireg.scheduler.JobParamInteger;

public class RecalculTachesJob extends JobDefinition {

	private static final String NAME = "RecalculTachesJob";

	private static final String CLEANUP_ONLY = "NETTOYAGE_SEUL";
	private static final String SCOPE = "SCOPE";
	private static final String NB_THREADS = "NB_THREADS";

	private PlatformTransactionManager transactionManager;
	private HibernateTemplate hibernateTemplate;
	private TacheService tacheService;
	private TacheSynchronizerInterceptor tacheSynchronizerInterceptor;
	private RapportService rapportService;

	public RecalculTachesJob(int sortOrder, String description) {
		super(NAME, JobCategory.TACHE, sortOrder, description);

		{
			final JobParam param = new JobParam();
			param.setMandatory(true);
			param.setName(CLEANUP_ONLY);
			param.setDescription("Nettoyage seul");
			param.setType(new JobParamBoolean());
			addParameterDefinition(param, false);
		}
		{
			final JobParam param = new JobParam();
			param.setMandatory(true);
			param.setName(SCOPE);
			param.setDescription("Scope");
			param.setType(new JobParamEnum(RecalculTachesProcessor.Scope.class));
			addParameterDefinition(param, RecalculTachesProcessor.Scope.PP);
		}
		{
			final JobParam param = new JobParam();
			param.setMandatory(true);
			param.setName(NB_THREADS);
			param.setDescription("Nombre de threads");
			param.setType(new JobParamInteger());
			addParameterDefinition(param, 4);
		}
	}

	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	public void setHibernateTemplate(HibernateTemplate hibernateTemplate) {
		this.hibernateTemplate = hibernateTemplate;
	}

	public void setTacheService(TacheService tacheService) {
		this.tacheService = tacheService;
	}

	public void setTacheSynchronizerInterceptor(TacheSynchronizerInterceptor tacheSynchronizerInterceptor) {
		this.tacheSynchronizerInterceptor = tacheSynchronizerInterceptor;
	}

	public void setRapportService(RapportService rapportService) {
		this.rapportService = rapportService;
	}

	@Override
	protected void doExecute(Map<String, Object> params) throws Exception {
		final boolean cleanup = getBooleanValue(params, CLEANUP_ONLY);
		final int nbThreads = getStrictlyPositiveIntegerValue(params, NB_THREADS);
		final RecalculTachesProcessor.Scope scope = getEnumValue(params, SCOPE, RecalculTachesProcessor.Scope.class);
		final RecalculTachesProcessor processor = new RecalculTachesProcessor(transactionManager, hibernateTemplate, tacheService, tacheSynchronizerInterceptor);
		final TacheSyncResults results = processor.run(cleanup, nbThreads, scope, getStatusManager());
		final RecalculTachesRapport rapport = rapportService.generateRapport(results, getStatusManager());
		setLastRunReport(rapport);
		audit.success("Recalcul des tâches d'envoi et d'annulation de déclaration d'impôt terminé.");
	}
}
