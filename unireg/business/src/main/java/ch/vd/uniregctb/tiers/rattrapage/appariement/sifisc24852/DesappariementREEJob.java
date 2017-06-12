package ch.vd.uniregctb.tiers.rattrapage.appariement.sifisc24852;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.transaction.PlatformTransactionManager;
import org.xml.sax.SAXException;

import ch.vd.rcent.unireg.unpairingree.ObjectFactory;
import ch.vd.rcent.unireg.unpairingree.OrganisationLocation;
import ch.vd.rcent.unireg.unpairingree.Unpairing;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.shared.batchtemplate.StatusManager;
import ch.vd.unireg.xml.tools.ClasspathCatalogResolver;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.document.DesappariementREERapport;
import ch.vd.uniregctb.hibernate.HibernateTemplate;
import ch.vd.uniregctb.rapport.RapportService;
import ch.vd.uniregctb.scheduler.JobCategory;
import ch.vd.uniregctb.scheduler.JobDefinition;
import ch.vd.uniregctb.scheduler.JobParam;
import ch.vd.uniregctb.scheduler.JobParamBoolean;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.tiers.rattrapage.appariement.AppariementService;

/**
 * @author Raphaël Marmier, 2017-06-01, <raphael.marmier@vd.ch>
 */
public class DesappariementREEJob extends JobDefinition {

	private static final Logger LOGGER = LoggerFactory.getLogger(DesappariementREEJob.class);

	private static final String NAME = "DesappariementREEJob";
	private static final String PARAM_SIMULATION = "SIMULATION";

	private static final String UNPAIRING_XSD_PATH = "unpairing.xsd";
	private static final String UNPAIRING_REE_XML_PATH = "/sifisc-24852/unpairing-ree.xml";

	private PlatformTransactionManager transactionManager;
	private HibernateTemplate hibernateTemplate;
	private AppariementService appariementService;
	private TiersService tiersService;
	private RapportService rapportService;

	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	public void setHibernateTemplate(HibernateTemplate hibernateTemplate) {
		this.hibernateTemplate = hibernateTemplate;
	}

	public void setAppariementService(AppariementService appariementService) {
		this.appariementService = appariementService;
	}

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	public void setRapportService(RapportService rapportService) {
		this.rapportService = rapportService;
	}

	public DesappariementREEJob(int sortOrder, String description) {
		super(NAME, JobCategory.DB, sortOrder, description);
		{
			final JobParam param = new JobParam();
			param.setDescription("Mode simulation");
			param.setName(PARAM_SIMULATION);
			param.setMandatory(false);
			param.setEnabled(true);
			param.setType(new JobParamBoolean());
			addParameterDefinition(param, Boolean.TRUE);
		}
	}

	@Override
	protected void doExecute(Map<String, Object> params) throws Exception {
		final boolean simulation = getBooleanValue(params, PARAM_SIMULATION);

		try {

			AuthenticationHelper.pushPrincipal(AuthenticationHelper.getCurrentPrincipal());

			// Récupération des données sources
			final Unpairing unpairing = parse(new StreamSource(this.getClass().getResourceAsStream(UNPAIRING_REE_XML_PATH)));

			final List<OrganisationLocation> organisationLocations = new ArrayList<>(unpairing.getOrganisationLocation().size());
			organisationLocations.addAll(unpairing.getOrganisationLocation());


			LOGGER.info(
					String.format("Unparing REE data, initial value date: %s, current value date: %s. total %d établissements.",
					              RegDateHelper.dateToDisplayString(unpairing.getInitialLoadDate()),
			                      RegDateHelper.dateToDisplayString(unpairing.getCurrentValueDate()),
			                      organisationLocations.size())
			);

			// Trier de manière à désapparier les établissements secondaire en premier.
			organisationLocations.sort((l1, l2) -> {
				if (l1.isMain() && !l2.isMain()) {
					return 1;
				}
				else if (!l1.isMain() && l2.isMain()) {
					return -1;
				}
				return 0;
			});

			organisationLocations
					.forEach(ol -> {
						LOGGER.info(
								String.format("Etablissement n°%d, %s, %s, %s, %s", ol.getCantonalId(), ol.isMain() ? "principal" : "secondaire", ol.getLegalForm(), ol.getMunicipality(), ol.getName())
						);
					});

			final StatusManager status = getStatusManager();
			final DesappariementREEProcessor desappariementREEProcessor = new DesappariementREEProcessor(hibernateTemplate, transactionManager, tiersService, null);
			final DesappariementREEResults results = desappariementREEProcessor.run(organisationLocations, unpairing.getInitialLoadDate(), unpairing.getCurrentValueDate(), status, simulation);

			results.getDesappariements().sort((d1, d2) -> {
				if (compareJaroWinkler(d1, d2)) {
					return 1;
				} else
				if (compareJaroWinkler(d2, d1)) {
					return -1;
				}
				return 0;
			});

			final DesappariementREERapport rapport = rapportService.generateRapport(results, status);

			setLastRunReport(rapport);
			Audit.success("Le désappariement des entités établissements REE chargés à tort est terminée.", rapport);

		} catch (Exception e) {
			final String message = "Exception lors de l'exécution du batch ";
			LOGGER.warn(message, e);
			Audit.error(message + " " + e.getMessage());
		}
		finally {
			AuthenticationHelper.popPrincipal();
		}
	}

	private boolean compareJaroWinkler(DesappariementREEResults.Desappariement d1, DesappariementREEResults.Desappariement d2) {
		return d1!= null && d2 == null || d1!= null && d1.getRaisonsSocialesJaroWinker() > d2.getRaisonsSocialesJaroWinker();
	}

	private boolean aDemenage(DesappariementREEResults.Desappariement d) {
		final Integer noOFSCommune = d.getNoOFSCommune();
		final OrganisationLocation etablissementRCEnt = d.getEtablissementRCEnt();
		return noOFSCommune == null || etablissementRCEnt == null || noOFSCommune != etablissementRCEnt.getMunicipality();
	}

	private Unpairing parse(Source xml) throws JAXBException, SAXException, IOException {
		final SchemaFactory sf = SchemaFactory.newInstance(javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI);
		sf.setResourceResolver(new ClasspathCatalogResolver());

		Schema schema = sf.newSchema(new StreamSource(new ClassPathResource(UNPAIRING_XSD_PATH).getURL().toExternalForm()));
		final JAXBContext jaxbContext = JAXBContext.newInstance(ObjectFactory.class.getPackage().getName());
		final Unmarshaller u = jaxbContext.createUnmarshaller();
		u.setSchema(schema);
		return (Unpairing) ((JAXBElement) u.unmarshal(xml)).getValue();
	}
}
