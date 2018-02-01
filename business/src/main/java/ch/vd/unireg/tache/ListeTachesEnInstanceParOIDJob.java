package ch.vd.unireg.tache;

import java.util.Map;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.audit.Audit;
import ch.vd.unireg.document.Document;
import ch.vd.unireg.rapport.RapportService;
import ch.vd.unireg.scheduler.JobCategory;
import ch.vd.unireg.scheduler.JobDefinition;

public class ListeTachesEnInstanceParOIDJob extends JobDefinition {

	private TacheService service;
	private RapportService rapportService;
	private PlatformTransactionManager transactionManager;

	public static final String NAME = "ListeTachesEnInstanceParOIDJob";

	public ListeTachesEnInstanceParOIDJob(int sortOrder, String description) {
		super(NAME, JobCategory.STATS, sortOrder, description);
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
		final ListeTachesEnInstanceParOID results = template.execute(new TransactionCallback<ListeTachesEnInstanceParOID>() {
			@Override
			public ListeTachesEnInstanceParOID doInTransaction(TransactionStatus status) {
				try {
					return service.produireListeTachesEnInstanceParOID(dateTraitement, getStatusManager());
				}
				catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		});
		final Document report = template.execute(new TransactionCallback<Document>() {
			@Override
			public Document doInTransaction(TransactionStatus status) {
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
	protected boolean isWebStartableInProductionMode() {
		return true;
	}
}
