package ch.vd.unireg.document;

import java.util.Collection;
import java.util.Map;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.unireg.scheduler.JobCategory;
import ch.vd.unireg.scheduler.JobDefinition;

/**
 * Job qui scanne le repository des documents à la recherche de fichiers non-référencés et qui les référence dans la base de données.
 */
public class RamasseDocumentJob extends JobDefinition {

	public static final String NAME = "RamasseDocumentJob";

	private DocumentService docService;
	private PlatformTransactionManager transactionManager;

	public RamasseDocumentJob(int sortOrder, String description) {
		super(NAME, JobCategory.DB, sortOrder, description);
	}

	public void setDocService(DocumentService service) {
		this.docService = service;
	}

	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	@Override
	protected void doExecute(Map<String, Object> params) throws Exception {

		// Exécution du job dans une transaction.
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		final Collection<Document> docs = template.execute(new TransactionCallback<Collection<Document>>() {
			@Override
			public Collection<Document> doInTransaction(TransactionStatus status) {
				return docService.ramasseDocs();
			}
		});

		for (Document doc : docs) {
			audit.info("Le fichier " + doc.getFileName() + " a été récupéré et inséré dans la base de données", doc);
		}
		audit.success("La ramassage des documents est terminé.");
	}
}
