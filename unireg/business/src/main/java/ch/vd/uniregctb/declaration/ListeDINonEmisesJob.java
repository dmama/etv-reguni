package ch.vd.uniregctb.declaration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.declaration.ordinaire.DeclarationImpotService;
import ch.vd.uniregctb.declaration.ordinaire.ListeDIsNonEmises;
import ch.vd.uniregctb.document.Document;
import ch.vd.uniregctb.parametrage.ParametreAppService;
import ch.vd.uniregctb.rapport.RapportService;
import ch.vd.uniregctb.scheduler.JobDefinition;
import ch.vd.uniregctb.scheduler.JobParam;
import ch.vd.uniregctb.scheduler.JobParamInteger;

public class ListeDINonEmisesJob extends JobDefinition {

	private DeclarationImpotService service;
	private RapportService rapportService;
	private PlatformTransactionManager transactionManager;

	private ParametreAppService paramsApp;

	public static final String NAME = "ListeDINonEmisesJob";
	private static final String CATEGORIE = "Stats";

	public static final String PERIODE_FISCALE = "PERIODE";

	private static final List<JobParam> params;

	private static final HashMap<String, Object> defaultParams;

	static {
		params = new ArrayList<JobParam>();
		{
			JobParam param = new JobParam();
			param.setDescription("Période fiscale");
			param.setName(PERIODE_FISCALE);
			param.setMandatory(true);
			param.setType(new JobParamInteger());
			params.add(param);
		}

		defaultParams = new HashMap<String, Object>();
		{
			RegDate today = RegDate.get();
			defaultParams.put(PERIODE_FISCALE, today.year() - 1);
		}
	}

	public ListeDINonEmisesJob(int sortOrder, String description) {
		this(sortOrder, description, defaultParams);
	}

	public ListeDINonEmisesJob(int sortOrder, String description, HashMap<String, Object> defaultParams) {
		super(NAME, CATEGORIE, sortOrder, description, params, defaultParams);
	}

	public void setService(DeclarationImpotService service) {
		this.service = service;
	}

	public void setRapportService(RapportService rapportService) {
		this.rapportService = rapportService;
	}

	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	@Override
	protected void doExecute(HashMap<String, Object> params) throws Exception {
		// Récupération des paramètres
		final Integer annee = (Integer) params.get(PERIODE_FISCALE);
		if (annee == null) {
			throw new RuntimeException("La période fiscale doit être spécifiée.");
		}
		if (annee >= RegDate.get().year()) {
			throw new RuntimeException("La période fiscale ne peut être >= à l'année en cours.");
		}
		if (annee < paramsApp.getPremierePeriodeFiscale()) {
			throw new RuntimeException("La période fiscale ne peut être < à l'année " + paramsApp.getPremierePeriodeFiscale());
		}

		final RegDate dateTraitement = RegDate.get(); // = aujourd'hui

		// Exécution de l'envoi dans une transaction.
		TransactionTemplate template = new TransactionTemplate(transactionManager);
		final ListeDIsNonEmises results = (ListeDIsNonEmises) template.execute(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				try {
					status.setRollbackOnly();
					ListeDIsNonEmises res = service.produireListeDIsNonEmises(annee, dateTraitement, getStatusManager());
					// La creation de la liste implique la simulation de la creation
					// d'une DI et non sa creation reelle, la seule issue
					// possible est donc un rollback de la transaction.
					return res;
				}
				catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		});
		Document report = (Document)template.execute(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				try {
					return rapportService.generateRapport(results, getStatusManager());
				}
				catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		});
		setLastRunReport(report);
		Audit.success("Liste DIs non émises générée correctement", report);
	}

	@Override
	protected HashMap<String, Object> createDefaultParams() {
		return defaultParams;
	}

	public void setParamsApp(ParametreAppService paramsApp) {
		this.paramsApp = paramsApp;
	}
}
