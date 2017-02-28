package ch.vd.uniregctb.foncier.migration;

import java.io.ByteArrayInputStream;
import java.util.Map;
import java.util.zip.ZipInputStream;

import org.apache.commons.lang3.StringUtils;

import ch.vd.shared.batchtemplate.StatusManager;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.document.MigrationDDCsvLoaderRapport;
import ch.vd.uniregctb.foncier.migration.DonneesFusionsCommunes;
import ch.vd.uniregctb.foncier.migration.MigrationDonneesFoncieresJob;
import ch.vd.uniregctb.rapport.RapportService;
import ch.vd.uniregctb.scheduler.JobCategory;
import ch.vd.uniregctb.scheduler.JobParam;
import ch.vd.uniregctb.scheduler.JobParamFile;
import ch.vd.uniregctb.scheduler.JobParamInteger;
import ch.vd.uniregctb.scheduler.JobParamString;

/**
 * Job de migration des données de dégrèvement depuis l'export CSV de SIMPA-PM.
 */
public class MigrationDDJob extends MigrationDonneesFoncieresJob {

	private static final String NAME = "MigrationDDJob";
	private static final String CSV_FILE = "csvFile";
	private static final String ENCODING = "Encoding";
	private static final String DEFAULT_ENCODING = "ISO-8859-15";
	private static final String NB_THREADS = "NB_THREADS";
	private static final String CSV_FUSIONS = "csvFusions";

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

		{
			final JobParam param = new JobParam();
			param.setDescription("Fichier CSV d'import compressé (*.zip)");
			param.setName(CSV_FILE);
			param.setMandatory(true);
			param.setType(new JobParamFile());
			addParameterDefinition(param, null);
		}
		{
			final JobParam param = new JobParam();
			param.setDescription("Encoding du fichier");
			param.setName(ENCODING);
			param.setMandatory(false);
			param.setType(new JobParamString());
			addParameterDefinition(param, DEFAULT_ENCODING);
		}
		{
			final JobParam param = new JobParam();
			param.setDescription("Fichier CSV des fusions de communes");
			param.setName(CSV_FUSIONS);
			param.setMandatory(false);
			param.setType(new JobParamFile());
			addParameterDefinition(param, null);
		}
		{
			final JobParam param = new JobParam();
			param.setDescription("Nombre de threads");
			param.setName(NB_THREADS);
			param.setMandatory(true);
			param.setType(new JobParamInteger());
			addParameterDefinition(param, 8);
		}
	}

	@Override
	protected void doExecute(Map<String, Object> params) throws Exception {

		final StatusManager status = getStatusManager();
		final byte[] zippedContent = getFileContent(params, CSV_FILE);
		final String encoding = getEncoding(params);
		final int nbThreads = getStrictlyPositiveIntegerValue(params, NB_THREADS);

		final byte[] fusionCommunesContent = getFileContent(params, CSV_FUSIONS);
		final DonneesFusionsCommunes fusionData = getDonneesFusionsCommunes(fusionCommunesContent);

		final MigrationDDImporterResults results;

		try (ByteArrayInputStream bais = new ByteArrayInputStream(zippedContent);
		     ZipInputStream zipstream = new ZipInputStream(bais)) {
			zipstream.getNextEntry();
			results = loader.loadCSV(zipstream, encoding, fusionData, nbThreads, status);
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
