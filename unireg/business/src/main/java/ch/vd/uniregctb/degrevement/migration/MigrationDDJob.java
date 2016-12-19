package ch.vd.uniregctb.degrevement.migration;

import java.io.ByteArrayInputStream;
import java.util.Map;
import java.util.zip.ZipInputStream;

import org.apache.commons.lang3.StringUtils;

import ch.vd.shared.batchtemplate.StatusManager;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.document.MigrationDDCsvLoaderRapport;
import ch.vd.uniregctb.rapport.RapportService;
import ch.vd.uniregctb.scheduler.JobCategory;
import ch.vd.uniregctb.scheduler.JobDefinition;
import ch.vd.uniregctb.scheduler.JobParam;
import ch.vd.uniregctb.scheduler.JobParamFile;
import ch.vd.uniregctb.scheduler.JobParamString;

/**
 * Job de migration des demandes de dégrèvement depuis l'export CSV de SIMPA-PM.
 */
public class MigrationDDJob extends JobDefinition {

	public static final String NAME = "MigrationDDJob";
	public static final String CSV_FILE = "csvFile";
	private static final String ENCODING = "Encoding";
	private static final String DEFAULT_ENCODING = "ISO-8859-15";
	public static final String CONTINUE_WITH_IMPORT_JOB = "CONTINUE_WITH_IMPORT_JOB";

	private MigrationDDImporter loader;
	private RapportService rapportService;

	public void setLoader(MigrationDDImporter loader) {
		this.loader = loader;
	}

	public void setRapportService(RapportService rapportService) {
		this.rapportService = rapportService;
	}

	public MigrationDDJob(int sortOrder, String description) {
		super(NAME, JobCategory.DD, sortOrder, description);

		final JobParam param1 = new JobParam();
		param1.setDescription("Fichier CSV d'import compressé (*.zip)");
		param1.setName(CSV_FILE);
		param1.setMandatory(true);
		param1.setType(new JobParamFile());
		addParameterDefinition(param1, null);

		final JobParam param2 = new JobParam();
		param2.setDescription("Encoding du fichier");
		param2.setName(ENCODING);
		param2.setMandatory(false);
		param2.setType(new JobParamString());
		addParameterDefinition(param2, DEFAULT_ENCODING);
	}

	@Override
	protected void doExecute(Map<String, Object> params) throws Exception {

		final StatusManager status = getStatusManager();
		final byte[] zippedContent = getFileContent(params, CSV_FILE);
		final String encoding = getEncoding(params);
//		final boolean startImportJob = getBooleanValue(params, CONTINUE_WITH_IMPORT_JOB);

		final MigrationDDImporterResults results;

		try (ByteArrayInputStream bais = new ByteArrayInputStream(zippedContent);
		     ZipInputStream zipstream = new ZipInputStream(bais)) {
			zipstream.getNextEntry();
			results = loader.loadCSV(zipstream, encoding, status);
		}
		final MigrationDDCsvLoaderRapport rapport = rapportService.generateRapport(results, status);
		setLastRunReport(rapport);
		Audit.success("Le traitement de la migration des demandes de dégrèvement est terminé.", rapport);
	}

	private String getEncoding(Map<String, Object> params) {
		String encoding = getStringValue(params, ENCODING);
		if (StringUtils.isBlank(encoding)) {
			encoding = DEFAULT_ENCODING;
		}
		return encoding;
	}
}
