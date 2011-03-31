package ch.vd.uniregctb.database;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.document.DatabaseDump;
import ch.vd.uniregctb.document.DocumentService;
import ch.vd.uniregctb.scheduler.*;
import org.apache.commons.lang.StringUtils;

import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class DumpTiersListJob extends JobDefinition {

	public static final String NAME = "DumpTiersListJob";
	
	private static final String CATEGORIE = "Database";
	
	public static final String PARAM_TIERS_LIST  = "PARAM_TIERS_LIST";
	public static final String FILE_TIERS_LIST  = "FILE_TIERS_LIST";
	public static final String INCLUDE_RET  = "INCLUDE_RET";
	public static final String INCLUDE_DECLARATION  = "INCLUDE_DECLARATION";
	public static final String INCLUDE_SIT_FAM  = "INCLUDE_SIT_FAM";

	private static final SimpleDateFormat SCREEN_DATE_FORMAT = new SimpleDateFormat();
	
	private static final SimpleDateFormat FILE_DATE_FORMAT = new SimpleDateFormat("yyyy_MM_dd-HH_mm_ss");
	
	private DatabaseService dbService;
	private DocumentService docService;
	
	public DumpTiersListJob(int sortOrder, String description) {
		super(NAME, CATEGORIE, sortOrder, description);

		{
			final JobParam param = new JobParam();
			param.setDescription("Ids des tiers (séparés par virgule)");
			param.setName(PARAM_TIERS_LIST);
			param.setMandatory(false);
			param.setType(new JobParamString());
			addParameterDefinition(param, null);
		}
		{
			final JobParam param = new JobParam();
			param.setDescription("Ids des tiers (fichier CSV)");
			param.setName(FILE_TIERS_LIST);
			param.setMandatory(false);
			param.setType(new JobParamFile());
			addParameterDefinition(param, null);
		}
		{
			final JobParam param = new JobParam();
			param.setDescription("Inclure les rapports-entre-tiers");
			param.setName(INCLUDE_RET);
			param.setMandatory(true);
			param.setType(new JobParamBoolean());
			addParameterDefinition(param, Boolean.FALSE);
		}
		{
			final JobParam param = new JobParam();
			param.setDescription("Inclure les déclarations");
			param.setName(INCLUDE_DECLARATION);
			param.setMandatory(true);
			param.setType(new JobParamBoolean());
			addParameterDefinition(param, Boolean.FALSE);
		}
		{
			final JobParam param = new JobParam();
			param.setDescription("Inclure les situations de famille");
			param.setName(INCLUDE_SIT_FAM);
			param.setMandatory(true);
			param.setType(new JobParamBoolean());
			addParameterDefinition(param, Boolean.FALSE);
		}
	}
	
	@SuppressWarnings({"UnusedDeclaration"})
	public void setDbService(DatabaseService dbService) {
		this.dbService = dbService;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setDocService(DocumentService docService) {
		this.docService = docService;
	}

	@Override
	protected void doExecute(Map<String, Object> params) throws Exception {
		final StatusManager status = getStatusManager();
		status.setMessage("Export de liste de tiers en cours...");
		
		final String idsParam = getStringValue(params, PARAM_TIERS_LIST);
		final byte[] idsFile = getFileContent(params, FILE_TIERS_LIST);

		final boolean inclusSitFamille = getBooleanValue(params, INCLUDE_SIT_FAM);
		final boolean inclusDeclarations = getBooleanValue(params, INCLUDE_DECLARATION);
		final boolean inclusRapportsEntreTiers = getBooleanValue(params, INCLUDE_RET);
		final DatabaseService.DumpParts parts = new DatabaseService.DumpParts(inclusSitFamille, inclusDeclarations, inclusRapportsEntreTiers);

		if (StringUtils.isEmpty(idsParam) && idsFile == null) {
			throw new RuntimeException("Les ids des tiers doivent être spécifiés.");
		}

		final List<Long> ids = new ArrayList<Long>();
		ids.addAll(extractIds(idsParam));
		ids.addAll(extractIdsFromCSV(idsFile));

		final Date date = DateHelper.getCurrentDate();
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
							count = dbService.dumpTiersListToDbunitFile(ids, parts, zipstream, status);
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

	@Override
	protected boolean isWebStartableInProductionMode() {
		return true;
	}
}
