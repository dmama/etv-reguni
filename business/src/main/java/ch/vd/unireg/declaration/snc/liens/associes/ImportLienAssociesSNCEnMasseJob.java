package ch.vd.unireg.declaration.snc.liens.associes;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.text.ParseException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.common.CsvHelper;
import ch.vd.unireg.common.StatusManager;
import ch.vd.unireg.document.LienAssociesSNCEnMasseImporterRapport;
import ch.vd.unireg.hibernate.HibernateTemplate;
import ch.vd.unireg.rapport.RapportService;
import ch.vd.unireg.scheduler.JobCategory;
import ch.vd.unireg.scheduler.JobDefinition;
import ch.vd.unireg.scheduler.JobParam;
import ch.vd.unireg.scheduler.JobParamFile;
import ch.vd.unireg.scheduler.JobParamRegDate;
import ch.vd.unireg.scheduler.JobParamString;

/**
 * Job d'import des SNC, à partir d'un fichier.
 */
public class ImportLienAssociesSNCEnMasseJob extends JobDefinition {

	private static final Logger LOGGER = LoggerFactory.getLogger(ImportLienAssociesSNCEnMasseJob.class);

	private static final String NAME = "ImportSNCJob";
	private static final String CSV_FILE = "FILE";
	private static final String ENCODING = "ENCODING";
	private static final String DEFAULT_ENCODING = "ISO-8859-15";

	private PlatformTransactionManager transactionManager;
	private HibernateTemplate hibernateTemplate;
	private LienAssociesSNCService lienAssociesSNCService;
	private RapportService rapportService;

	public ImportLienAssociesSNCEnMasseJob(int sortOrder, String description) {
		super(NAME, JobCategory.RAPPORT_ENTRE_TIERS, sortOrder, description);

		{
			final JobParam param = new JobParam();
			param.setDescription("Fichier CSV d'import");
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
			param.setDescription("Date de traitement");
			param.setName(DATE_TRAITEMENT);
			param.setMandatory(false);
			param.setType(new JobParamRegDate(false));
			addParameterDefinition(param, null);
		}

	}


	@Override
	protected void doExecute(Map<String, Object> params) throws Exception {
		final byte[] data = getFileContent(params, CSV_FILE);
		final String encoding = StringUtils.defaultIfBlank(getStringValue(params, ENCODING), DEFAULT_ENCODING);

		final RegDate dateTraitement = getDateTraitement(params);

		final List<String> lignesInvalides = new LinkedList<>();
		final List<DonneesLienAssocieEtSNC> donneesSNC = extractDonneesSNC(data, lignesInvalides::add);
		final StatusManager statusManager = getStatusManager();

		final LienAssociesSNCEnMasseImporterResults results = lienAssociesSNCService.importLienAssociesSNCEnMasse(donneesSNC, dateTraitement, statusManager);
		lignesInvalides.forEach(results::addLigneInvalide);

		final LienAssociesSNCEnMasseImporterRapport rapport = rapportService.generateRapport(results, statusManager);
		setLastRunReport(rapport);

		audit.success("L'import des liens entre associés et SNC à la date du " + RegDateHelper.dateToDisplayString(dateTraitement) + " est terminé.", rapport);

	}


	private List<DonneesLienAssocieEtSNC> extractDonneesSNC(byte[] data, Consumer<String> ligneInvalideConsumer) throws IOException {
		final List<DonneesLienAssocieEtSNC> donneeSNCAImporter = new LinkedList<>();
		final File tempFile = File.createTempFile("tempCsv", "snc");
		FileUtils.writeByteArrayToFile(tempFile, data);
		final Reader reader = new FileReader(tempFile);

		try (final CSVParser csvParser = new CSVParser(reader, CSVFormat.EXCEL
				.withHeader(DonneesLienAssocieEtSNC.HEADERS.stream().toArray(String[]::new)).withDelimiter(CsvHelper.COMMA)
				.withSkipHeaderRecord().withTrim())) {
			for (CSVRecord csvRecord : csvParser) {
				try {
					final DonneesLienAssocieEtSNC snc = DonneesLienAssocieEtSNC.valueOf(csvRecord);
					donneeSNCAImporter.add(snc);
				}
				catch (ParseException e) {
					LOGGER.warn("Ligne ignoree : '[{}]' ==> impossible de parser la valeur {},  de la colonne {}.", DonneesLienAssocieEtSNC.parseToCsvString(csvRecord), e.getMessage(),
					            DonneesLienAssocieEtSNC.HEADERS.get(e.getErrorOffset()));
					ligneInvalideConsumer.accept(DonneesLienAssocieEtSNC.parseToCsvString(csvRecord));
					tempFile.deleteOnExit();
				}
			}
		}

		tempFile.deleteOnExit();
		return donneeSNCAImporter;
	}

	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	public void setHibernateTemplate(HibernateTemplate hibernateTemplate) {
		this.hibernateTemplate = hibernateTemplate;
	}

	public void setLienAssociesSNCService(LienAssociesSNCService lienAssociesSNCService) {
		this.lienAssociesSNCService = lienAssociesSNCService;
	}

	public void setRapportService(RapportService rapportService) {
		this.rapportService = rapportService;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		super.afterPropertiesSet();
		getParameterDefinition(DATE_TRAITEMENT).setEnabled(isTesting());
	}
}
