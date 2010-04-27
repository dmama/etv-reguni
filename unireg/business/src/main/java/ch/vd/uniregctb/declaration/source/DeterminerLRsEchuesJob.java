package ch.vd.uniregctb.declaration.source;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.document.DeterminerLRsEchuesRapport;
import ch.vd.uniregctb.document.ListesNominativesRapport;
import ch.vd.uniregctb.rapport.RapportService;
import ch.vd.uniregctb.scheduler.JobDefinition;
import ch.vd.uniregctb.scheduler.JobParam;
import ch.vd.uniregctb.scheduler.JobParamInteger;
import ch.vd.uniregctb.scheduler.JobParamRegDate;

/**
 * Job qui envoie des événements fiscaux pour tous les débiteurs de prestation imposable
 * pour lesquels au moins une LR est échue (= taxable d'office) dans une période fiscale donnée
 */
public class DeterminerLRsEchuesJob extends JobDefinition {

	public static final String NAME = "DeterminerLRsEchuesJob";
	private static final String CATEGORIE = "LR";

	private static final String PERIODE_FISCALE = "PERIODE_FISCALE";

	private static final List<JobParam> params;
	private static final HashMap<String, Object> defaultParams;

	private RapportService rapportService;
	private ListeRecapService lrService;
	private PlatformTransactionManager transactionManager;

	static {
		params = new ArrayList<JobParam>();
		{
			final JobParam param = new JobParam();
			param.setDescription("Période fiscale");
			param.setName(PERIODE_FISCALE);
			param.setMandatory(true);
			param.setType(new JobParamInteger());
			params.add(param);
		}
		{
			final JobParam param = new JobParam();
			param.setDescription("Date de traitement");
			param.setName(DATE_TRAITEMENT);
			param.setMandatory(false);
			param.setType(new JobParamRegDate());
			params.add(param);
		}

		defaultParams = new HashMap<String, Object>();
		{
			final RegDate today = RegDate.get();
			defaultParams.put(PERIODE_FISCALE, today.year() - 1);
		}
	}

	public DeterminerLRsEchuesJob(int sortOrder, String description) {
		this(sortOrder, description, defaultParams);
	}

	public DeterminerLRsEchuesJob(int sortOrder, String description, HashMap<String, Object> defaultParams) {
		super(NAME, CATEGORIE, sortOrder, description, params, defaultParams);
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
		params.get(1).setEnabled(isTesting());
	}

	@Override
	protected void doExecute(HashMap<String, Object> params) throws Exception {

		// Récupération des paramètres
		final Integer periodeFiscale = (Integer) params.get(PERIODE_FISCALE);
		if (periodeFiscale == null) {
			throw new RuntimeException("La période fiscale doit être spécifiée.");
		}

		final RegDate dateTraitement = getDateTraitement(params);
		final StatusManager statusManager = getStatusManager();
		final DeterminerLRsEchuesResults results = lrService.determineLRsEchues(periodeFiscale, dateTraitement, statusManager);

		// Produit le rapport dans une transaction read-write
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(false);
		final DeterminerLRsEchuesRapport rapport = (DeterminerLRsEchuesRapport) template.execute(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				return rapportService.generateRapport(results, statusManager);
			}
		});

		setLastRunReport(rapport);
		Audit.success(String.format("La détermination des LR échues pour la période fiscale %d à la date du %s est terminée.", periodeFiscale, RegDateHelper.dateToDisplayString(dateTraitement)), rapport);
	}

	@Override
	protected HashMap<String, Object> createDefaultParams() {
		return defaultParams;
	}
}
