package ch.vd.unireg.foncier;

import java.util.Map;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.document.InitialisationIFoncRapport;
import ch.vd.unireg.hibernate.HibernateTemplate;
import ch.vd.unireg.rapport.RapportService;
import ch.vd.unireg.registrefoncier.RegistreFoncierService;
import ch.vd.unireg.scheduler.JobCategory;
import ch.vd.unireg.scheduler.JobDefinition;
import ch.vd.unireg.scheduler.JobParam;
import ch.vd.unireg.scheduler.JobParamCommune;
import ch.vd.unireg.scheduler.JobParamInteger;
import ch.vd.unireg.scheduler.JobParamRegDate;

/**
 * Job d'extraction de la population pour l'initialisation de la taxation IFONC
 */
public class InitialisationIFoncJob extends JobDefinition {

	private static final String NAME = "InitialisationIFoncJob";
	private static final String DATE_PARAM = "DATE";
	private static final String NB_THREADS_PARAM = "NB_THREADS";
	private static final String COMMUNE_PARAM = "COMMUNE";

	private PlatformTransactionManager transactionManager;
	private RapportService rapportService;
	private HibernateTemplate hibernateTemplate;
	private RegistreFoncierService registreFoncierService;

	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	public void setHibernateTemplate(HibernateTemplate hibernateTemplate) {
		this.hibernateTemplate = hibernateTemplate;
	}

	public void setRapportService(RapportService rapportService) {
		this.rapportService = rapportService;
	}

	public void setRegistreFoncierService(RegistreFoncierService registreFoncierService) {
		this.registreFoncierService = registreFoncierService;
	}

	public InitialisationIFoncJob(int sortOrder, String description) {
		super(NAME, JobCategory.STATS, sortOrder, description);

		{
			final JobParam param = new JobParam();
			param.setEnabled(true);
			param.setDescription("Date de référence");
			param.setMandatory(true);
			param.setName(DATE_PARAM);
			param.setType(new JobParamRegDate(false));
			addParameterDefinition(param, RegDate.get(RegDate.get().year(), 1, 1));     // par défaut, 1er janvier de l'année en cours
		}
		{
			final JobParam param = new JobParam();
			param.setEnabled(true);
			param.setDescription("Commune cible");
			param.setMandatory(false);
			param.setName(COMMUNE_PARAM);
			param.setType(new JobParamCommune(JobParamCommune.TypeCommune.COMMUNE_VD));
			addParameterDefinition(param, null);
		}
		{
			final JobParam param = new JobParam();
			param.setEnabled(true);
			param.setDescription("Nombre de threads");
			param.setMandatory(true);
			param.setName(NB_THREADS_PARAM);
			param.setType(new JobParamInteger());
			addParameterDefinition(param, 4);
		}
	}

	@Override
	protected void doExecute(Map<String, Object> params) throws Exception {
		final RegDate dateReference = getRegDateValue(params, DATE_PARAM);
		final int nbThreads = getStrictlyPositiveIntegerValue(params, NB_THREADS_PARAM);
		final Integer ofsCommune = getOptionalIntegerValue(params, COMMUNE_PARAM);
		final InitialisationIFoncProcessor processor = new InitialisationIFoncProcessor(transactionManager, hibernateTemplate, registreFoncierService);
		final InitialisationIFoncResults results = processor.run(dateReference, nbThreads, ofsCommune, getStatusManager());

		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		final InitialisationIFoncRapport rapport = template.execute(status -> rapportService.generateRapport(results, getStatusManager()));
		setLastRunReport(rapport);
		audit.success("La génération des données d'initialisation de la taxation IFONC est terminée.", rapport);
	}

	@Override
	protected boolean isWebStartableInProductionMode() {
		return true;
	}

}
