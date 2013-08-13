package ch.vd.uniregctb.parentes;

import java.util.Map;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.common.MultipleSwitch;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.common.Switchable;
import ch.vd.uniregctb.document.InitialisationParentesRapport;
import ch.vd.uniregctb.hibernate.HibernateTemplate;
import ch.vd.uniregctb.rapport.RapportService;
import ch.vd.uniregctb.scheduler.JobDefinition;
import ch.vd.uniregctb.scheduler.JobParam;
import ch.vd.uniregctb.scheduler.JobParamInteger;
import ch.vd.uniregctb.tiers.RapportEntreTiersDAO;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.transaction.TransactionTemplate;

public class InitialisationParentesJob extends JobDefinition {

	private static final String NAME = "InitialisationParentesJob";
	private static final String CATEGORIE = "Database";

	public static final String NB_THREADS = "NB_THREADS";

	private RapportEntreTiersDAO rapportEntreTiersDAO;
	private TiersDAO tiersDAO;
	private PlatformTransactionManager transactionManager;
	private HibernateTemplate hibernateTemplate;
	private TiersService tiersService;
	private RapportService rapportService;
	private MultipleSwitch interceptorSwitch;

	public InitialisationParentesJob(int sortOrder, String description) {
		super(NAME, CATEGORIE, sortOrder, description);

		final JobParam param = new JobParam();
		param.setDescription("Nombre de threads");
		param.setName(NB_THREADS);
		param.setMandatory(true);
		param.setEnabled(true);
		param.setType(new JobParamInteger());
		addParameterDefinition(param, 4);
	}

	public void setRapportEntreTiersDAO(RapportEntreTiersDAO rapportEntreTiersDAO) {
		this.rapportEntreTiersDAO = rapportEntreTiersDAO;
	}

	public void setTiersDAO(TiersDAO tiersDAO) {
		this.tiersDAO = tiersDAO;
	}

	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	public void setHibernateTemplate(HibernateTemplate hibernateTemplate) {
		this.hibernateTemplate = hibernateTemplate;
	}

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	public void setRapportService(RapportService rapportService) {
		this.rapportService = rapportService;
	}

	public void setInterceptors(Switchable[] interceptors) {
		this.interceptorSwitch = new MultipleSwitch(interceptors);
	}

	@Override
	protected void doExecute(Map<String, Object> params) throws Exception {
		final StatusManager statusManager = getStatusManager();
		final int nbThreads = getIntegerValue(params, NB_THREADS);
		final InitialisationParentesProcessor processor = new InitialisationParentesProcessor(rapportEntreTiersDAO, tiersDAO, transactionManager, hibernateTemplate, interceptorSwitch, tiersService);
		final InitialisationParentesResults results = processor.run(nbThreads, statusManager);

		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		final InitialisationParentesRapport rapport = template.execute(new TransactionCallback<InitialisationParentesRapport>() {
			@Override
			public InitialisationParentesRapport doInTransaction(TransactionStatus status) {
				return rapportService.generateRapport(results, statusManager);
			}
		});
		setLastRunReport(rapport);
		Audit.success("Le génération des données de parenté est terminée.", rapport);
	}
}
