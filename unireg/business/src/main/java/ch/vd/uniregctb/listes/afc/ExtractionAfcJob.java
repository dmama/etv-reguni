package ch.vd.uniregctb.listes.afc;

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

	private static final List<JobParam> params;

	private static final HashMap<String, Object> defaultParams;

	private RapportService rapportService;

	private PlatformTransactionManager transactionManager;

	private ExtractionAfcService service;

	static {
		params = new ArrayList<JobParam>();
		{
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
				param.setDescription("Mode");
				param.setName(MODE);
				param.setMandatory(true);
				param.setType(new JobParamEnum(TypeExtractionAfc.class));
				params.add(param);
			}
			{
				final JobParam param = new JobParam();
				param.setDescription("Nombre de threads");
				param.setName(NB_THREADS);
				param.setMandatory(false);
				param.setType(new JobParamInteger());
				params.add(param);
			}
		}

		defaultParams = new HashMap<String, Object>();
		{
			final RegDate today = RegDate.get();
			defaultParams.put(PERIODE_FISCALE, today.year() - 1);
			defaultParams.put(MODE, TypeExtractionAfc.REVENU);
			defaultParams.put(NB_THREADS, 4);
		}
	}

	public ExtractionAfcJob(int order, String description) {
		this(order, description, defaultParams);
	}

	public ExtractionAfcJob(int order, String description, HashMap<String, Object> defaultParams) {
		super(NAME, CATEGORIE, order, description, params, defaultParams);
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
	protected void doExecute(HashMap<String, Object> params) throws Exception {
		final RegDate dateTraitement = RegDate.get();
		final StatusManager statusManager = getStatusManager();

		// récupère les paramètres
		final int nbThreads = (Integer) getParamOrDefault(params, NB_THREADS);
		final Integer pf = (Integer) params.get(PERIODE_FISCALE);
		if (pf == null) {
			throw new IllegalArgumentException("Le paramètre '" + PERIODE_FISCALE + "' doit être renseigné!");
		}
		final TypeExtractionAfc mode = (TypeExtractionAfc) params.get(MODE);
		if (mode == null) {
			throw new IllegalArgumentException("Le paramètre '" + MODE + "' doit être renseigné!");
		}

		// on fait le boulot !
		final ExtractionAfcResults results = service.produireExtraction(dateTraitement, pf, mode, nbThreads, statusManager);

		// on génère un rapport
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(false);
		final ExtractionAfcRapport rapport = (ExtractionAfcRapport) template.execute(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				return rapportService.generateRapport(results, statusManager);
			}
		});

		setLastRunReport(rapport);
		Audit.success(String.format("L'extraction AFC (%s %d) en date du %s est terminée.", mode, pf, RegDateHelper.dateToDisplayString(dateTraitement)), rapport);
	}

	@SuppressWarnings({"unchecked"})
	private <T> T getParamOrDefault(HashMap<String, Object> params, String paramName) {
		final T value = (T) params.get(paramName);
		if (value == null) {
			return (T) defaultParams.get(paramName);
		}
		else {
			return value;
		}
	}
}
