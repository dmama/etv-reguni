package ch.vd.uniregctb.parentes;

import java.util.Map;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.common.MultipleSwitch;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.common.Switchable;
import ch.vd.uniregctb.document.CalculParentesRapport;
import ch.vd.uniregctb.rapport.RapportService;
import ch.vd.uniregctb.scheduler.JobCategory;
import ch.vd.uniregctb.scheduler.JobDefinition;
import ch.vd.uniregctb.scheduler.JobParam;
import ch.vd.uniregctb.scheduler.JobParamEnum;
import ch.vd.uniregctb.scheduler.JobParamInteger;
import ch.vd.uniregctb.tiers.RapportEntreTiersDAO;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.TiersService;

public class CalculParentesJob extends JobDefinition {

	private static final String NAME = "CalculParentesJob";

	public static final String NB_THREADS = "NB_THREADS";
	public static final String MODE = "MODE";

	private RapportEntreTiersDAO rapportEntreTiersDAO;
	private TiersDAO tiersDAO;
	private PlatformTransactionManager transactionManager;
	private TiersService tiersService;
	private RapportService rapportService;
	private MultipleSwitch interceptorSwitch;

	public CalculParentesJob(int sortOrder, String description) {
		super(NAME, JobCategory.DB, sortOrder, description);

		{
			final JobParam param = new JobParam();
			param.setDescription("Nombre de threads");
			param.setName(NB_THREADS);
			param.setMandatory(true);
			param.setEnabled(true);
			param.setType(new JobParamInteger());
			addParameterDefinition(param, 4);
		}
		{
			final JobParam param = new JobParam();
			param.setDescription("Mode");
			param.setName(MODE);
			param.setMandatory(true);
			param.setEnabled(true);
			param.setType(new JobParamEnum(CalculParentesMode.class));
			addParameterDefinition(param, CalculParentesMode.FULL);
		}
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
		final CalculParentesMode mode = getEnumValue(params, MODE, CalculParentesMode.class);
		final CalculParentesProcessor processor = new CalculParentesProcessor(rapportEntreTiersDAO, tiersDAO, transactionManager, interceptorSwitch, tiersService);
		final CalculParentesResults results = processor.run(nbThreads, mode, statusManager);

		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		final CalculParentesRapport rapport = template.execute(new TransactionCallback<CalculParentesRapport>() {
			@Override
			public CalculParentesRapport doInTransaction(TransactionStatus status) {
				return rapportService.generateRapport(results, statusManager);
			}
		});
		setLastRunReport(rapport);
		Audit.success("La génération des données de parenté est terminée.", rapport);
	}
}
