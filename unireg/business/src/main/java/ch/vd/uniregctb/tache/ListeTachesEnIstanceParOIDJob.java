package ch.vd.uniregctb.tache;

import java.util.Map;

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

	public ListeTachesEnIstanceParOIDJob(int sortOrder, String description) {
		super(NAME, CATEGORIE, sortOrder, description);
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
	protected void doExecute(Map<String, Object> params) throws Exception {


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

	public void setParamsApp(ParametreAppService paramsApp) {
		this.paramsApp = paramsApp;
	}

	@Override
	protected boolean isWebStartableInProductionMode() {
		return true;
	}
}
