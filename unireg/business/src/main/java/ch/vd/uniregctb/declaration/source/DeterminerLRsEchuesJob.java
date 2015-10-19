package ch.vd.uniregctb.declaration.source;

import java.util.Map;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.shared.batchtemplate.StatusManager;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.document.DeterminerLRsEchuesRapport;
import ch.vd.uniregctb.rapport.RapportService;
import ch.vd.uniregctb.scheduler.JobCategory;
import ch.vd.uniregctb.scheduler.JobDefinition;
import ch.vd.uniregctb.scheduler.JobParam;
import ch.vd.uniregctb.scheduler.JobParamInteger;
import ch.vd.uniregctb.scheduler.JobParamRegDate;
import ch.vd.uniregctb.transaction.TransactionTemplate;

/**
 * Job qui envoie des événements fiscaux pour tous les débiteurs de prestation imposable
 * pour lesquels au moins une LR est échue (= taxable d'office) dans une période fiscale donnée
 */
public class DeterminerLRsEchuesJob extends JobDefinition {

	public static final String NAME = "DeterminerLRsEchuesJob";

	private static final String PERIODE_FISCALE = "PERIODE_FISCALE";

	private RapportService rapportService;
	private ListeRecapService lrService;
	private PlatformTransactionManager transactionManager;

	public DeterminerLRsEchuesJob(int sortOrder, String description) {
		super(NAME, JobCategory.LR, sortOrder, description);

		{
			final JobParam param = new JobParam();
			param.setDescription("Période fiscale");
			param.setName(PERIODE_FISCALE);
			param.setMandatory(false);
			param.setType(new JobParamInteger());
			addParameterDefinition(param, null);
		}
		{
			final JobParam param = new JobParam();
			param.setDescription("Date de traitement");
			param.setName(DATE_TRAITEMENT);
			param.setMandatory(false);
			param.setType(new JobParamRegDate());
			addParameterDefinition(param, null);
		}
	}

	public void setRapportService(RapportService rapportService) {
		this.rapportService = rapportService;
	}

	public void setLrService(ListeRecapService lrService) {
		this.lrService = lrService;
	}

	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		super.afterPropertiesSet();
		getParameterDefinition(DATE_TRAITEMENT).setEnabled(isTesting());
	}

	@Override
	protected void doExecute(Map<String, Object> params) throws Exception {

		// Récupération des paramètres
		final Integer periodeFiscale = getOptionalIntegerValue(params, PERIODE_FISCALE);
		final RegDate dateTraitement = getDateTraitement(params);
		final StatusManager statusManager = getStatusManager();
		final DeterminerLRsEchuesResults results = lrService.determineLRsEchues(periodeFiscale, dateTraitement, statusManager);

		// Produit le rapport dans une transaction read-write
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(false);
		final DeterminerLRsEchuesRapport rapport = template.execute(new TransactionCallback<DeterminerLRsEchuesRapport>() {
			@Override
			public DeterminerLRsEchuesRapport doInTransaction(TransactionStatus status) {
				return rapportService.generateRapport(results, statusManager);
			}
		});

		setLastRunReport(rapport);
		Audit.success(String.format("La détermination des LR échues pour la période fiscale %d à la date du %s est terminée.", periodeFiscale, RegDateHelper.dateToDisplayString(dateTraitement)), rapport);
	}
}
