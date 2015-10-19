package ch.vd.uniregctb.listes.afc;

import java.util.Map;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.shared.batchtemplate.StatusManager;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.document.ExtractionDonneesRptRapport;
import ch.vd.uniregctb.rapport.RapportService;
import ch.vd.uniregctb.scheduler.JobCategory;
import ch.vd.uniregctb.scheduler.JobDefinition;
import ch.vd.uniregctb.scheduler.JobParam;
import ch.vd.uniregctb.scheduler.JobParamEnum;
import ch.vd.uniregctb.scheduler.JobParamInteger;
import ch.vd.uniregctb.transaction.TransactionTemplate;

/**
 * Job d'extraction des listes de contribuables pour l'AFC (administration fédérale des contributions)
 */
public class ExtractionDonneesRptJob extends JobDefinition {

	public static final String NAME = "ExtractionDonneesRptJob";

	public static final String NB_THREADS = "NB_THREADS";
	public static final String PERIODE_FISCALE = "PERIODE";
	public static final String MODE = "MODE";

	private RapportService rapportService;

	private PlatformTransactionManager transactionManager;

	private ExtractionDonneesRptService service;

	public ExtractionDonneesRptJob(int order, String description) {
		super(NAME, JobCategory.STATS, order, description);

		{
			final RegDate today = RegDate.get();
			final JobParam param = new JobParam();
			param.setDescription("Période fiscale");
			param.setName(PERIODE_FISCALE);
			param.setMandatory(true);
			param.setType(new JobParamInteger());
			addParameterDefinition(param, today.year() - 1);
		}
		{
			final JobParam param = new JobParam();
			param.setDescription("Mode");
			param.setName(MODE);
			param.setMandatory(true);
			param.setType(new JobParamEnum(TypeExtractionDonneesRpt.class));
			addParameterDefinition(param, TypeExtractionDonneesRpt.REVENU_ORDINAIRE);
		}
		{
			final JobParam param = new JobParam();
			param.setDescription("Nombre de threads");
			param.setName(NB_THREADS);
			param.setMandatory(true);
			param.setType(new JobParamInteger());
			addParameterDefinition(param, 4);
		}
	}

	public void setRapportService(RapportService rapportService) {
		this.rapportService = rapportService;
	}

	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	public void setService(ExtractionDonneesRptService service) {
		this.service = service;
	}

	@Override
	protected void doExecute(Map<String, Object> params) throws Exception {
		final RegDate dateTraitement = RegDate.get();
		final StatusManager statusManager = getStatusManager();

		// récupère les paramètres
		final int nbThreads = getStrictlyPositiveIntegerValue(params,  NB_THREADS);
		final int pf = getIntegerValue(params, PERIODE_FISCALE);
		final TypeExtractionDonneesRpt mode = getEnumValue(params, MODE, TypeExtractionDonneesRpt.class);

		// on fait le boulot !
		final ExtractionDonneesRptResults results = service.produireExtraction(dateTraitement, pf, mode, nbThreads, statusManager);

		// on génère un rapport
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(false);
		final ExtractionDonneesRptRapport rapport = template.execute(new TransactionCallback<ExtractionDonneesRptRapport>() {
			@Override
			public ExtractionDonneesRptRapport doInTransaction(TransactionStatus status) {
				return rapportService.generateRapport(results, statusManager);
			}
		});

		setLastRunReport(rapport);
		Audit.success(String.format("L'extraction des données de référence RPT (%s %d) en date du %s est terminée.", mode.getDescription(), pf, RegDateHelper.dateToDisplayString(dateTraitement)), rapport);
	}
}
