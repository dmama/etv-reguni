package ch.vd.unireg.foncier.migration.mandataire;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.document.MigrationMandatairesSpeciauxRapport;
import ch.vd.unireg.hibernate.HibernateTemplate;
import ch.vd.unireg.interfaces.infra.data.GenreImpotMandataire;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;
import ch.vd.unireg.rapport.RapportService;
import ch.vd.unireg.scheduler.JobCategory;
import ch.vd.unireg.scheduler.JobDefinition;
import ch.vd.unireg.scheduler.JobParam;
import ch.vd.unireg.scheduler.JobParamDynamicEnum;
import ch.vd.unireg.scheduler.JobParamFile;
import ch.vd.unireg.scheduler.JobParamRegDate;
import ch.vd.unireg.scheduler.JobParamString;

public class MigrationMandatairesSpeciauxJob extends JobDefinition {

	private static final Logger LOGGER = LoggerFactory.getLogger(MigrationMandatairesSpeciauxJob.class);

	private static final String NAME = "MigrationMandatairesSpeciauxJob";
	private static final String CSV_FILE = "FILE";
	private static final String ENCODING = "ENCODING";
	private static final String DATE_DEBUT = "DATE_DEBUT";
	private static final String GENRE_IMPOT = "GENRE_IMPOT";
	private static final String DEFAULT_ENCODING = "ISO-8859-15";

	private ServiceInfrastructureService infraService;
	private PlatformTransactionManager transactionManager;
	private HibernateTemplate hibernateTemplate;
	private RapportService rapportService;

	public void setInfraService(ServiceInfrastructureService infraService) {
		this.infraService = infraService;
	}

	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	public void setHibernateTemplate(HibernateTemplate hibernateTemplate) {
		this.hibernateTemplate = hibernateTemplate;
	}

	public void setRapportService(RapportService rapportService) {
		this.rapportService = rapportService;
	}

	public MigrationMandatairesSpeciauxJob(int sortOrder, String description) {
		super(NAME, JobCategory.DB, sortOrder, description);
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
			param.setDescription("Date de début des mandats");
			param.setName(DATE_DEBUT);
			param.setMandatory(true);
			param.setType(new JobParamRegDate(false));
			addParameterDefinition(param, RegDate.get(RegDate.get().year(), 1, 1));
		}
		{
			final JobParam param = new JobParam();
			param.setDescription("Genre d'impôt (mandats spéciaux)");
			param.setName(GENRE_IMPOT);
			param.setMandatory(true);
			param.setType(new JobParamDynamicEnum<>(GenreImpotMandataire.class, this::getGenresImpotMandataires, GenreImpotMandataire::getCode));
			addParameterDefinition(param, null);
		}
	}

	private Collection<GenreImpotMandataire> getGenresImpotMandataires() {
		return infraService.getGenresImpotMandataires();
	}

	@Override
	protected void doExecute(Map<String, Object> params) throws Exception {
		final RegDate dateDebut = getRegDateValue(params, DATE_DEBUT);
		final GenreImpotMandataire genreImpot = getValue(params, GENRE_IMPOT, GenreImpotMandataire.class);
		final byte[] data = getFileContent(params, CSV_FILE);
		final String encoding = StringUtils.defaultIfBlank(getStringValue(params, ENCODING), DEFAULT_ENCODING);
		final List<String> lignesInvalides = new LinkedList<>();
		final List<DonneesMandat> mandats = extractMandats(data, encoding, lignesInvalides::add);

		final MigrationMandatImporter importer = new MigrationMandatImporter(infraService, transactionManager, hibernateTemplate);
		final MigrationMandatImporterResults results = importer.importData(mandats, dateDebut, genreImpot, getStatusManager());
		lignesInvalides.forEach(results::addLigneInvalide);
		final MigrationMandatairesSpeciauxRapport rapport = rapportService.generateRapport(results, getStatusManager());
		setLastRunReport(rapport);
		audit.success("Le traitement de la migration des mandataires spéciaux est terminé.", rapport);
	}

	private List<DonneesMandat> extractMandats(byte[] data, String encoding, Consumer<String> ligneInvalideConsumer) throws IOException {
		final List<DonneesMandat> list = new LinkedList<>();
		try (ByteArrayInputStream bais = new ByteArrayInputStream(data);
		     InputStreamReader isr = new InputStreamReader(bais, encoding);
		     BufferedReader br = new BufferedReader(isr)) {

			String line;
			while ((line = br.readLine()) != null) {
				if (StringUtils.isNotBlank(line)) {
					try {
						final DonneesMandat mandat = DonneesMandat.valueOf(line);
						list.add(mandat);
					}
					catch (ParseException e) {
						LOGGER.warn("Ligne ignorée : '" + line + "'");
						ligneInvalideConsumer.accept(line);
					}
				}
			}
		}
		return list;
	}
}
