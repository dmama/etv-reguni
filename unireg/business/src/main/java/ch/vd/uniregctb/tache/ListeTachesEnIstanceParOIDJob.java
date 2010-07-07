package ch.vd.uniregctb.tache;

import java.util.HashMap;
import java.util.List;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.document.Document;
import ch.vd.uniregctb.parametrage.ParametreAppService;
import ch.vd.uniregctb.rapport.RapportService;
import ch.vd.uniregctb.scheduler.JobDefinition;
import ch.vd.uniregctb.scheduler.JobParam;

public class ListeTachesEnIstanceParOIDJob extends JobDefinition {

	private TacheService service;
	private RapportService rapportService;
	private PlatformTransactionManager transactionManager;

	private ParametreAppService paramsApp;

	public static final String NAME = "ListeTachesEnIstanceParOIDJob";
	private static final String CATEGORIE = "Stats";


	private static final List<JobParam> params;

	private static final HashMap<String, Object> defaultParams;

	static {
		params = null;

		defaultParams = null;

	}

	public ListeTachesEnIstanceParOIDJob(int sortOrder, String description) {
		this(sortOrder, description, defaultParams);
	}

	public ListeTachesEnIstanceParOIDJob(int sortOrder, String description, HashMap<String, Object> defaultParams) {
		super(NAME, CATEGORIE, sortOrder, description, params, defaultParams);
	}

	public void setService(TacheService service) {
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


		final RegDate dateTraitement = RegDate.get(); // = aujourd'hui

		// Exécution de l'envoi dans une transaction.
		TransactionTemplate template = new TransactionTemplate(transactionManager);
		final ListeTachesEnIsntanceParOID results = (ListeTachesEnIsntanceParOID) template.execute(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				try {

					ListeTachesEnIsntanceParOID res = service.produireListeTachesEnIstanceParOID(dateTraitement, getStatusManager());

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
		Audit.success("Liste des tâches en instance par OID générée correctement", report);
	}

	@Override
	protected HashMap<String, Object> createDefaultParams() {
		return defaultParams;
	}

	public void setParamsApp(ParametreAppService paramsApp) {
		this.paramsApp = paramsApp;
	}
}
