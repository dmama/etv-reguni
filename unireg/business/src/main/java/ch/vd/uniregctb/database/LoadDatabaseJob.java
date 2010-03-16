package ch.vd.uniregctb.database;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.zip.ZipInputStream;

import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.document.Document;
import ch.vd.uniregctb.document.DocumentService;
import ch.vd.uniregctb.indexer.tiers.GlobalTiersIndexer;
import ch.vd.uniregctb.scheduler.JobDefinition;
import ch.vd.uniregctb.scheduler.JobParam;
import ch.vd.uniregctb.scheduler.JobParamString;

/**
 * Job qui charge la contenu de la base de données à partir d'un fichier sur le disque du serveur.
 */
public class LoadDatabaseJob extends JobDefinition {

	public static final String NAME = "LoadDatabaseJob";
	private static final String CATEGORIE = "Database";

	public static final String DOC_ID = "DocId";

	private static final List<JobParam> params;

	static {
		params = new ArrayList<JobParam>();
		{
			JobParam param = new JobParam();
			param.setDescription("Numéro du document");
			param.setName(DOC_ID);
			param.setMandatory(true);
			param.setType(new JobParamString());
			params.add(param);
		}
	}

	private DatabaseService dbService;
	private DocumentService docService;
	private GlobalTiersIndexer globalIndexer;

	public LoadDatabaseJob(int sortOrder, String description, HashMap<String, Object> defaultParams) {
		super(NAME, CATEGORIE, sortOrder, description, params, defaultParams);
	}

	public void setDbService(DatabaseService dbService) {
		this.dbService = dbService;
	}

	public void setDocService(DocumentService docService) {
		this.docService = docService;
	}

	public void setGlobalIndexer(GlobalTiersIndexer globalIndexer) {
		this.globalIndexer = globalIndexer;
	}

	@Override
	protected void doExecute(HashMap<String, Object> params) throws Exception {
		final StatusManager status = getStatusManager();

		final Long docId = (Long) params.get(DOC_ID);
		if (docId == null) {
			throw new RuntimeException("Le numéro du document doit être spécifié.");
		}

		final Document doc = docService.get(docId);
		if (doc == null) {
			throw new RuntimeException("Le document n°" + docId + " n'existe pas.");
		}

		status.setMessage("Ouverture du fichier...");
		docService.readDoc(doc, new DocumentService.ReadDocCallback<Document>() {
			public void readDoc(Document doc, InputStream is) throws Exception {

				ZipInputStream zipstream = new ZipInputStream(is);
				try {
					zipstream.getNextEntry();

					status.setMessage("Effacement de la base...");
					dbService.truncateDatabase();
					status.setMessage("Import de la base en cours...");
					dbService.loadFromDbunitFile(zipstream, status);
				}
				finally {
					zipstream.close();
				}
			}
		});

		status.setMessage("Reindexation de la base en cours...");
		globalIndexer.indexAllDatabase(false, status);

		Audit.success("La base de données a été rechargée et indexée à partir du fichier " + doc.getNom() + " (document #" + doc.getId()
				+ ").");
	}

	@Override
	public boolean isVisible() {
		return isTesting();
	}
}
