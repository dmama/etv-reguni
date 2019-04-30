package ch.vd.unireg.database;

import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.unireg.common.StatusManager;
import ch.vd.unireg.document.DatabaseDump;
import ch.vd.unireg.document.DocumentService;
import ch.vd.unireg.scheduler.JobCategory;
import ch.vd.unireg.scheduler.JobDefinition;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.tiers.TiersDAO;

/**
 * Job qui dump la contenu de la base de données dans un fichier sur le disque du serveur.
 */
public class DumpDatabaseJob extends JobDefinition {

	private static final SimpleDateFormat SCREEN_DATE_FORMAT = new SimpleDateFormat();
	private static final SimpleDateFormat FILE_DATE_FORMAT = new SimpleDateFormat("yyyy_MM_dd-HH_mm_ss");
	public static final String NAME = "DumpDatabaseJob";

	private DatabaseService dbService;
	private DocumentService docService;
	private TiersDAO tiersDAO;
	private PlatformTransactionManager transactionManager;

	public DumpDatabaseJob(int sortOrder, String description) {
		super(NAME, JobCategory.DB, sortOrder, description);
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

	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	@Override
	protected void doExecute(Map<String, Object> params) throws Exception {
		StatusManager status = getStatusManager();
		status.setMessage("Export de la base en cours...");

		final TransactionTemplate template = new TransactionTemplate(transactionManager);

		final DatabaseDump doc = template.execute(s -> {

			final Date date = DateHelper.getCurrentDate();
			final int count = tiersDAO.getCount(Tiers.class);
			final String name = "dbdump_" + FILE_DATE_FORMAT.format(date);
			final String description = "Export de la base généré le " + SCREEN_DATE_FORMAT.format(date) + " et contenant " + count + " tiers.";
			final String extension = "zip";

			try {
				return docService.newDoc(DatabaseDump.class, name, description, extension,
						new DocumentService.WriteDocCallback<DatabaseDump>() {
							@Override
							public void writeDoc(DatabaseDump doc1, OutputStream os) throws Exception {

								// Dump la base de donnée dans un fichier zip sur le disque
								try (ZipOutputStream zipstream = new ZipOutputStream(os)) {
									final ZipEntry e = new ZipEntry(name + ".xml");
									zipstream.putNextEntry(e);
									dbService.dumpToDbunitFile(zipstream);
								}

								doc1.setNbTiers(count);
							}
						});
			}
			catch (Exception e) {
				throw new RuntimeException(e);
			}
		});

		setLastRunReport(doc);
		audit.success("La base de données a été exportée dans le fichier " + doc.getNom() + " (document #" + doc.getId() + ").", doc);
	}

	@Override
	public boolean isVisible() {
		return isTesting();
	}
}
