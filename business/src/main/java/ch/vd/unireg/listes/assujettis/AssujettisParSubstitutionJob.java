package ch.vd.unireg.listes.assujettis;

import java.util.Map;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.StatusManager;
import ch.vd.unireg.document.AssujettiParSubstitutionRapport;
import ch.vd.unireg.hibernate.HibernateTemplate;
import ch.vd.unireg.metier.assujettissement.AssujettissementService;
import ch.vd.unireg.rapport.RapportService;
import ch.vd.unireg.scheduler.JobCategory;
import ch.vd.unireg.scheduler.JobDefinition;
import ch.vd.unireg.scheduler.JobParam;
import ch.vd.unireg.scheduler.JobParamInteger;
import ch.vd.unireg.tiers.TiersService;

public class AssujettisParSubstitutionJob extends JobDefinition {

	private static final String NAME = "ListeAssujettisParSubstitutionJob";
	private static final String NB_THREADS = "NB_THREADS";

	private HibernateTemplate hibernateTemplate;
	private TiersService tiersService;
	private PlatformTransactionManager transactionManager;
	private RapportService rapportService;
	private AssujettissementService assujettissementService;

	public AssujettisParSubstitutionJob(int sortOrder, String description) {
		super(NAME, JobCategory.STATS, sortOrder, description);
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
		final AssujettisParSubstitutionProcessor processor = new AssujettisParSubstitutionProcessor(hibernateTemplate, tiersService, transactionManager, assujettissementService);
		final AssujettisParSubstitutionResults results = processor.run(dateTraitement, nbThreads, statusManager);

		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		final AssujettiParSubstitutionRapport rapport = template.execute(status -> rapportService.generateRapport(results, statusManager));
		setLastRunReport(rapport);
		audit.success("La génération le la liste des assujettis par substitution est terminée.", rapport);

	}

	@Override
	protected boolean isWebStartableInProductionMode() {
		return true;
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
