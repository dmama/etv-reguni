package ch.vd.uniregctb.document;

import java.util.Collection;
import java.util.Map;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.scheduler.JobDefinition;

/**
 * Job qui scanne le repository des documents à la recherche de fichiers non-référencés et qui les référence dans la base de données.
 */
public class RamasseDocumentJob extends JobDefinition {

	private DocumentService docService;
	private PlatformTransactionManager transactionManager;

	public static final String NAME = "RamasseDocumentJob";
	private static final String CATEGORIE = "Database";

	public RamasseDocumentJob(int sortOrder, String description) {
		super(NAME, CATEGORIE, sortOrder, description);
	}

	public void setDocService(DocumentService service) {
		this.docService = service;
	}

	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void doExecute(Map<String, Object> params) throws Exception {

		// Exécution du job dans une transaction.
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		Collection<Document> docs = (Collection<Document>) template.execute(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				return docService.ramasseDocs();
			}
		});

		for (Document doc : docs) {
			Audit.info("Le fichier " + doc.getFileName() + " a été récupéré et inséré dans la base de données", doc);
		}
		Audit.success("La ramassage des documents est terminé.");
	}
}
