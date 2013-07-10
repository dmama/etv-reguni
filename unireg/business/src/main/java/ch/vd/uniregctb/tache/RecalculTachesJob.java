package ch.vd.uniregctb.tache;

import java.util.Map;

import org.springframework.transaction.PlatformTransactionManager;

import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.document.RecalculTachesRapport;
import ch.vd.uniregctb.hibernate.HibernateTemplate;
import ch.vd.uniregctb.rapport.RapportService;
import ch.vd.uniregctb.scheduler.JobDefinition;
import ch.vd.uniregctb.scheduler.JobParam;
import ch.vd.uniregctb.scheduler.JobParamBoolean;

public class RecalculTachesJob extends JobDefinition {

	private static final String NAME = "RecalculTachesJob";
	private static final String CATEGORIE = "Tache";

	private static final String CLEANUP_ONLY = "NETTOYAGE_SEUL";

	private PlatformTransactionManager transactionManager;
	private HibernateTemplate hibernateTemplate;
	private TacheService tacheService;
	private TacheSynchronizerInterceptor tacheSynchronizerInterceptor;
	private RapportService rapportService;

	public RecalculTachesJob(int sortOrder, String description) {
		super(NAME, CATEGORIE, sortOrder, description);

		final JobParam param = new JobParam();
		param.setMandatory(true);
		param.setName(CLEANUP_ONLY);
		param.setDescription("Nettoyage seul");
		param.setType(new JobParamBoolean());
		addParameterDefinition(param, false);
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
		final RecalculTachesProcessor processor = new RecalculTachesProcessor(transactionManager, hibernateTemplate, tacheService, tacheSynchronizerInterceptor);
		final TacheSyncResults results = processor.run(cleanup, getStatusManager());
		final RecalculTachesRapport rapport = rapportService.generateRapport(results, getStatusManager());
		setLastRunReport(rapport);
		Audit.success("Recalcul des tâches d'envoi et d'annulation de déclaration d'impôt terminé.");
	}
}
