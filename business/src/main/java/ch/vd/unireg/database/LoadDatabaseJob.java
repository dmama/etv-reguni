package ch.vd.unireg.database;

import java.util.Map;
import java.util.zip.ZipInputStream;

import ch.vd.unireg.common.StatusManager;
import ch.vd.unireg.document.Document;
import ch.vd.unireg.document.DocumentService;
import ch.vd.unireg.indexer.tiers.GlobalTiersIndexer;
import ch.vd.unireg.scheduler.JobCategory;
import ch.vd.unireg.scheduler.JobDefinition;
import ch.vd.unireg.scheduler.JobParam;
import ch.vd.unireg.scheduler.JobParamLong;

/**
 * Job qui charge la contenu de la base de données à partir d'un fichier sur le disque du serveur.
 */
public class LoadDatabaseJob extends JobDefinition {

	public static final String NAME = "LoadDatabaseJob";
	public static final String DOC_ID = "DocId";

	private DatabaseService dbService;
	private DocumentService docService;
	private GlobalTiersIndexer globalIndexer;

	public LoadDatabaseJob(int sortOrder, String description) {
		super(NAME, JobCategory.DB, sortOrder, description);

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
		docService.readDoc(doc, (doc1, is) -> {

			try (ZipInputStream zipstream = new ZipInputStream(is)) {
				zipstream.getNextEntry();

				status.setMessage("Effacement de la base...");
				dbService.truncateDatabase();
				status.setMessage("Import de la base en cours...");
				dbService.loadFromDbunitFile(zipstream, status, false);
			}
		});

		status.setMessage("Reindexation de la base en cours...");
		globalIndexer.indexAllDatabase(GlobalTiersIndexer.Mode.FULL, 2, status);

		audit.success("La base de données a été rechargée et indexée à partir du fichier " + doc.getNom() + " (document #" + doc.getId()
				+ ").");
	}

	@Override
	public boolean isVisible() {
		return isTesting();
	}
}
