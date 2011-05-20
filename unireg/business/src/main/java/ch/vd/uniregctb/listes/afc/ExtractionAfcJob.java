package ch.vd.uniregctb.listes.afc;

import java.util.Map;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.document.ExtractionAfcRapport;
import ch.vd.uniregctb.rapport.RapportService;
import ch.vd.uniregctb.scheduler.JobDefinition;
import ch.vd.uniregctb.scheduler.JobParam;
import ch.vd.uniregctb.scheduler.JobParamEnum;
import ch.vd.uniregctb.scheduler.JobParamInteger;

/**
 * Job d'extraction des listes de contribuables pour l'AFC (administration fédérale des contributions)
 */
public class ExtractionAfcJob extends JobDefinition {

	public static final String NAME = "ExtractionAfcJob";

	private static final String CATEGORIE = "Stats";

	public static final String NB_THREADS = "NB_THREADS";
	public static final String PERIODE_FISCALE = "PERIODE";
	public static final String MODE = "MODE";

	private RapportService rapportService;

	private PlatformTransactionManager transactionManager;

	private ExtractionAfcService service;

	public ExtractionAfcJob(int order, String description) {
		super(NAME, CATEGORIE, order, description);

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
			param.setType(new JobParamEnum(TypeExtractionAfc.class));
			addParameterDefinition(param, TypeExtractionAfc.REVENU);
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

	public void setService(ExtractionAfcService service) {
		this.service = service;
	}

	@Override
	protected void doExecute(Map<String, Object> params) throws Exception {
		final RegDate dateTraitement = RegDate.get();
		final StatusManager statusManager = getStatusManager();

		// récupère les paramètres
		final int nbThreads = getStrictlyPositiveIntegerValue(params,  NB_THREADS);
		final int pf = getIntegerValue(params, PERIODE_FISCALE);
		final TypeExtractionAfc mode = getEnumValue(params, MODE, TypeExtractionAfc.class);

		// on fait le boulot !
		final ExtractionAfcResults results = service.produireExtraction(dateTraitement, pf, mode, nbThreads, statusManager);

		// on génère un rapport
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(false);
		final ExtractionAfcRapport rapport = template.execute(new TransactionCallback<ExtractionAfcRapport>() {
			public ExtractionAfcRapport doInTransaction(TransactionStatus status) {
				return rapportService.generateRapport(results, statusManager);
			}
		});

		setLastRunReport(rapport);
		Audit.success(String.format("L'extraction AFC (%s %d) en date du %s est terminée.", mode, pf, RegDateHelper.dateToDisplayString(dateTraitement)), rapport);
	}
}
