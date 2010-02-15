package ch.vd.uniregctb.database;

import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.springframework.util.StringUtils;

import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.document.DatabaseDump;
import ch.vd.uniregctb.document.DocumentService;
import ch.vd.uniregctb.scheduler.JobDefinition;
import ch.vd.uniregctb.scheduler.JobParam;
import ch.vd.uniregctb.scheduler.JobParamString;
import ch.vd.uniregctb.tiers.TiersDAO;

public class DumpTiersListJob extends JobDefinition {

	public static final String NAME = "DumpTiersListJob";
	
	private static final String CATEGORIE = "Database";
	
	public static final String PARAM_TIERS_LIST  = "PARAM_TIERS_LIST";

	private static final SimpleDateFormat SCREEN_DATE_FORMAT = new SimpleDateFormat();
	
	private static final SimpleDateFormat FILE_DATE_FORMAT = new SimpleDateFormat("yyyy_MM_dd-HH_mm_ss");
	
	private static final List<JobParam> params;
	
	static {
		params = new ArrayList<JobParam>();
		{
			final JobParam param0 = new JobParam();
			param0.setDescription("Ids des tiers (séparés par virgule)");
			param0.setName(PARAM_TIERS_LIST);
			param0.setMandatory(true);
			param0.setType(new JobParamString());
			params.add(param0);
		}
	}

	private DatabaseService dbService;
	private DocumentService docService;
	
	public DumpTiersListJob(int sortOrder, String description) {
		super(NAME, CATEGORIE, sortOrder, description, params);
	}
	
	public void setDbService(DatabaseService dbService) {
		this.dbService = dbService;
	}

	public void setDocService(DocumentService docService) {
		this.docService = docService;
	}

	public void setTiersDAO(TiersDAO tiersDAO) {
	}
	
	@Override
	protected void doExecute(HashMap<String, Object> params) throws Exception {
		final StatusManager status = getStatusManager();
		status.setMessage("Export de liste de tiers en cours...");
		
		final String idsParam = (String) params.get(PARAM_TIERS_LIST);
		if (idsParam == null || !StringUtils.hasText(idsParam)) {
			return;
		}
		
		final Date date = new Date();
		final String name = "tiersdump_" + FILE_DATE_FORMAT.format(date);
		final String description = "Export de la base généré le " + SCREEN_DATE_FORMAT.format(date) + " et contenant %d tiers.";
		final String extension = "zip";

		DatabaseDump doc = docService.newDoc(DatabaseDump.class, name, description, extension,
				new DocumentService.WriteDocCallback<DatabaseDump>() {
					public void writeDoc(DatabaseDump doc, OutputStream os) throws Exception {

						int count;
						// Dump la base de donnée dans un fichier zip sur le disque
						ZipOutputStream zipstream = new ZipOutputStream(os);
						try {
							ZipEntry e = new ZipEntry(name + ".xml");
							zipstream.putNextEntry(e);
							final List<Long> tiers = extractIds(idsParam);
							count = dbService.dumpTiersListToDbunitFile(tiers, zipstream);
						}
						finally {
							zipstream.close();
						}

						doc.setNbTiers(count);
						doc.setDescription(String.format(description, count));
					}
				});

		setLastRunReport(doc);
		Audit.success("La base de données a été exportée dans le fichier " + doc.getNom() + " (document #" + doc.getId() + ").", doc);
	}
	
	private List<Long> extractIds(String idsParam) {
		final List<Long> ids = new ArrayList<Long>();
		if (idsParam == null) {
			return ids;
		}
		
		final String[] idStrs = idsParam.split(",");
		for (final String idStr : idStrs) {
			try {
				ids.add(Long.valueOf(idStr.trim()));
			}
			catch (NumberFormatException ignored) {
				// ignorer les éléments invalides 
			}
		}
		return ids;
	}

}
