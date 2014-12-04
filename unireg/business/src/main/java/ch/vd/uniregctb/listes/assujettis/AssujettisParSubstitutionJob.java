package ch.vd.uniregctb.listes.assujettis;

import java.util.Map;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.RegDate;
import ch.vd.shared.batchtemplate.StatusManager;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.document.AssujettiParSubstitutionRapport;
import ch.vd.uniregctb.hibernate.HibernateTemplate;
import ch.vd.uniregctb.metier.assujettissement.AssujettissementService;
import ch.vd.uniregctb.rapport.RapportService;
import ch.vd.uniregctb.scheduler.JobDefinition;
import ch.vd.uniregctb.scheduler.JobParam;
import ch.vd.uniregctb.scheduler.JobParamInteger;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.transaction.TransactionTemplate;

public class AssujettisParSubstitutionJob extends JobDefinition {

	private static final String NAME = "ListeAssujettisParSubstitutionJob";
	private static final String CATEGORIE = "Stats";
	private static final String NB_THREADS = "NB_THREADS";

	private HibernateTemplate hibernateTemplate;
	private TiersService tiersService;
	private PlatformTransactionManager transactionManager;
	private RapportService rapportService;
	private AssujettissementService assujettissementService;

	public AssujettisParSubstitutionJob(int sortOrder, String description) {
		super(NAME, CATEGORIE, sortOrder, description);
		{
			final JobParam param = new JobParam();
			param.setDescription("Nombre de threads");
			param.setName(NB_THREADS);
			param.setMandatory(true);
			param.setType(new JobParamInteger());
			addParameterDefinition(param, 4);
		}
	}

	@Override
	protected void doExecute(Map<String, Object> params) throws Exception {
		final int nbThreads = getIntegerValue(params, NB_THREADS);
		final RegDate dateTraitement = getDateTraitement(params);
		final StatusManager statusManager = getStatusManager();
		final AssujettisParSubstitutionProcessor processor = new AssujettisParSubstitutionProcessor(hibernateTemplate, tiersService,transactionManager,assujettissementService);
		final AssujettisParSubstitutionResults results = processor.run(dateTraitement, nbThreads, statusManager);

		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		final AssujettiParSubstitutionRapport rapport = template.execute(new TransactionCallback<AssujettiParSubstitutionRapport>() {
			@Override
			public AssujettiParSubstitutionRapport doInTransaction(TransactionStatus status) {
				return rapportService.generateRapport(results, statusManager);
			}
		});
		setLastRunReport(rapport);
		Audit.success("Le génération des données de parenté est terminée.", rapport);

	}

	public void setHibernateTemplate(HibernateTemplate hibernateTemplate) {
		this.hibernateTemplate = hibernateTemplate;
	}

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}


	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}


	public void setRapportService(RapportService rapportService) {
		this.rapportService = rapportService;
	}

	public void setAssujettissementService(AssujettissementService assujettissementService) {
		this.assujettissementService = assujettissementService;
	}
}
