package ch.vd.uniregctb.database;

import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.document.DatabaseDump;
import ch.vd.uniregctb.document.DocumentService;
import ch.vd.uniregctb.scheduler.JobDefinition;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersDAO;

/**
 * Job qui dump la contenu de la base de données dans un fichier sur le disque du serveur.
 */
public class DumpDatabaseJob extends JobDefinition {

	private static final SimpleDateFormat SCREEN_DATE_FORMAT = new SimpleDateFormat();

	private static final SimpleDateFormat FILE_DATE_FORMAT = new SimpleDateFormat("yyyy_MM_dd-HH_mm_ss");

	public static final String NAME = "DumpDatabaseJob";
	private static final String CATEGORIE = "Database";

	private DatabaseService dbService;
	private DocumentService docService;
	private TiersDAO tiersDAO;

	public DumpDatabaseJob(int sortOrder, String description) {
		super(NAME, CATEGORIE, sortOrder, description);
	}

	public void setDbService(DatabaseService dbService) {
		this.dbService = dbService;
	}

	public void setDocService(DocumentService docService) {
		this.docService = docService;
	}

	public void setTiersDAO(TiersDAO tiersDAO) {
		this.tiersDAO = tiersDAO;
	}

	@Override
	protected void doExecute(HashMap<String, Object> params) throws Exception {
		StatusManager status = getStatusManager();
		status.setMessage("Export de la base en cours...");

		final Date date = new Date();
		final int count = tiersDAO.getCount(Tiers.class);
		final String name = "dbdump_" + FILE_DATE_FORMAT.format(date);
		final String description = "Export de la base généré le " + SCREEN_DATE_FORMAT.format(date) + " et contenant " + count + " tiers.";
		final String extension = "zip";

		DatabaseDump doc = docService.newDoc(DatabaseDump.class, name, description, extension,
				new DocumentService.WriteDocCallback<DatabaseDump>() {
					public void writeDoc(DatabaseDump doc, OutputStream os) throws Exception {

						// Dump la base de donnée dans un fichier zip sur le disque
						ZipOutputStream zipstream = new ZipOutputStream(os);
						try {
							ZipEntry e = new ZipEntry(name + ".xml");
							zipstream.putNextEntry(e);
							dbService.dumpToDbunitFile(zipstream);
						}
						finally {
							zipstream.close();
						}

						doc.setNbTiers(count);
					}
				});

		setLastRunReport(doc);
		Audit.success("La base de données a été exportée dans le fichier " + doc.getNom() + " (document #" + doc.getId() + ").", doc);
	}

	@Override
	public boolean isVisible() {
		return isTesting();
	}
}
