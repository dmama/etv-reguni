package ch.vd.uniregctb.regimefiscal.extraction;

import java.util.Map;

import org.springframework.transaction.PlatformTransactionManager;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.document.ExtractionRegimesFiscauxRapport;
import ch.vd.uniregctb.hibernate.HibernateTemplate;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.rapport.RapportService;
import ch.vd.uniregctb.scheduler.JobCategory;
import ch.vd.uniregctb.scheduler.JobDefinition;
import ch.vd.uniregctb.scheduler.JobParam;
import ch.vd.uniregctb.scheduler.JobParamBoolean;
import ch.vd.uniregctb.scheduler.JobParamInteger;
import ch.vd.uniregctb.scheduler.JobParamRegDate;
import ch.vd.uniregctb.tiers.TiersService;

public class ExtractionRegimesFiscauxJob extends JobDefinition {

	private static final String NAME = "ExtractionRegimesFiscauxJob";
	private static final String HISTORY_PARAM_NAME = "HISTORY";
	private static final String NBTHREADS_PARAM_NAME = "NB_THREADS";

	private HibernateTemplate hibernateTemplate;
	private PlatformTransactionManager transactionManager;
	private ServiceInfrastructureService infraService;
	private RapportService rapportService;
	private TiersService tiersService;

	public ExtractionRegimesFiscauxJob(int sortOrder, String description) {
		super(NAME, JobCategory.STATS, sortOrder, description);
		{
			final JobParam param = new JobParam();
			param.setName(HISTORY_PARAM_NAME);
			param.setDescription("Avec historique");
			param.setMandatory(true);
			param.setType(new JobParamBoolean());
			addParameterDefinition(param, Boolean.FALSE);
		}
		{
			final JobParam param = new JobParam();
			param.setName(NBTHREADS_PARAM_NAME);
			param.setDescription("Nombre de threads");
			param.setMandatory(true);
			param.setType(new JobParamInteger());
			addParameterDefinition(param, 4);
		}
		{
			final JobParam param = new JobParam();
			param.setName(DATE_TRAITEMENT);
			param.setDescription("Date de traitement");
			param.setMandatory(false);
			param.setType(new JobParamRegDate());
			addParameterDefinition(param, null);
		}
	}

	public void setHibernateTemplate(HibernateTemplate hibernateTemplate) {
		this.hibernateTemplate = hibernateTemplate;
	}

	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	public void setInfraService(ServiceInfrastructureService infraService) {
		this.infraService = infraService;
	}

	public void setRapportService(RapportService rapportService) {
		this.rapportService = rapportService;
	}

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		super.afterPropertiesSet();
		getParameterDefinition(DATE_TRAITEMENT).setEnabled(isTesting());
	}

	@Override
	protected void doExecute(Map<String, Object> params) throws Exception {
		final boolean avecHistorique = getBooleanValue(params, HISTORY_PARAM_NAME);
		final int nbThreads = getStrictlyPositiveIntegerValue(params, NBTHREADS_PARAM_NAME);
		final RegDate dateTraitement = getDateTraitement(params);
		final ExtractionRegimesFiscauxProcessor processor = new ExtractionRegimesFiscauxProcessor(hibernateTemplate, transactionManager, infraService, tiersService);
		final ExtractionRegimesFiscauxResults results = processor.run(avecHistorique, nbThreads, dateTraitement, getStatusManager());
		final ExtractionRegimesFiscauxRapport rapport = rapportService.generateRapport(results, getStatusManager());
		setLastRunReport(rapport);
		Audit.success("Le job d'extraction des régimes fiscaux est maintenant terminé", rapport);
	}
}