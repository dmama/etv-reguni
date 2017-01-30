package ch.vd.uniregctb.foncier.migration.ifonc;

import java.io.ByteArrayInputStream;
import java.util.Map;
import java.util.zip.ZipInputStream;

import org.apache.commons.lang.StringUtils;

import ch.vd.shared.batchtemplate.StatusManager;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.document.MigrationExoIFONCRapport;
import ch.vd.uniregctb.rapport.RapportService;
import ch.vd.uniregctb.scheduler.JobCategory;
import ch.vd.uniregctb.scheduler.JobDefinition;
import ch.vd.uniregctb.scheduler.JobParam;
import ch.vd.uniregctb.scheduler.JobParamFile;
import ch.vd.uniregctb.scheduler.JobParamInteger;
import ch.vd.uniregctb.scheduler.JobParamString;

public class MigrationExoIFONCJob extends JobDefinition {

	private static final String NAME = "MigrationExoIFONCJob";
	public static final String CSV_FILE = "csvFile";
	private static final String ENCODING = "Encoding";
	private static final String DEFAULT_ENCODING = "ISO-8859-15";
	public static final String NB_THREADS = "NB_THREADS";

	private MigrationExoIFONCImporter loader;
	private RapportService rapportService;

	public void setLoader(MigrationExoIFONCImporter loader) {
		this.loader = loader;
	}

	public void setRapportService(RapportService rapportService) {
		this.rapportService = rapportService;
	}

	public MigrationExoIFONCJob(int sortOrder, String description) {
		super(NAME, JobCategory.DB, sortOrder, description);

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
		final String encoding = StringUtils.defaultIfBlank(getStringValue(params, ENCODING), DEFAULT_ENCODING);
		final int nbThreads = getStrictlyPositiveIntegerValue(params, NB_THREADS);

		final MigrationExoIFONCImporterResults results;
		try (ByteArrayInputStream bais = new ByteArrayInputStream(zippedContent); ZipInputStream zipstream = new ZipInputStream(bais)) {
			zipstream.getNextEntry();
			results = loader.loadCSV(zipstream, encoding, nbThreads, status);
		}
		final MigrationExoIFONCRapport rapport = rapportService.generateRapport(results, status);
		setLastRunReport(rapport);
		Audit.success("Le traitement de la migration des exonérations IFONC est terminé.", rapport);
	}
}
