package ch.vd.uniregctb.database;

import java.io.InputStream;
import java.util.Map;
import java.util.zip.ZipInputStream;

import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.document.Document;
import ch.vd.uniregctb.document.DocumentService;
import ch.vd.uniregctb.indexer.tiers.GlobalTiersIndexer;
import ch.vd.uniregctb.scheduler.JobDefinition;
import ch.vd.uniregctb.scheduler.JobParam;
import ch.vd.uniregctb.scheduler.JobParamLong;

/**
 * Job qui charge la contenu de la base de données à partir d'un fichier sur le disque du serveur.
 */
public class LoadDatabaseJob extends JobDefinition {

	public static final String NAME = "LoadDatabaseJob";
	private static final String CATEGORIE = "Database";

	public static final String DOC_ID = "DocId";

	private DatabaseService dbService;
	private DocumentService docService;
	private GlobalTiersIndexer globalIndexer;

	public LoadDatabaseJob(int sortOrder, String description) {
		super(NAME, CATEGORIE, sortOrder, description);

		final JobParam param = new JobParam();
		param.setDescription("Numéro du document");
		param.setName(DOC_ID);
		param.setMandatory(true);
		param.setType(new JobParamLong());
		addParameterDefinition(param, null);
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
	protected void doExecute(Map<String, Object> params) throws Exception {
		final StatusManager status = getStatusManager();

		final long docId = getLongValue(params, DOC_ID);
		final Document doc = docService.get(docId);
		if (doc == null) {
			throw new RuntimeException("Le document n°" + docId + " n'existe pas.");
		}

		status.setMessage("Ouverture du fichier...");
		docService.readDoc(doc, new DocumentService.ReadDocCallback<Document>() {
			@Override
			public void readDoc(Document doc, InputStream is) throws Exception {

				ZipInputStream zipstream = new ZipInputStream(is);
				try {
					zipstream.getNextEntry();

					status.setMessage("Effacement de la base...");
					dbService.truncateDatabase();
					status.setMessage("Import de la base en cours...");
					dbService.loadFromDbunitFile(zipstream, status, false);
				}
				finally {
					zipstream.close();
				}
			}
		});

		status.setMessage("Reindexation de la base en cours...");
		globalIndexer.indexAllDatabase(status, 2, GlobalTiersIndexer.Mode.FULL, true, true);

		Audit.success("La base de données a été rechargée et indexée à partir du fichier " + doc.getNom() + " (document #" + doc.getId()
				+ ").");
	}

	@Override
	public boolean isVisible() {
		return isTesting();
	}
}
