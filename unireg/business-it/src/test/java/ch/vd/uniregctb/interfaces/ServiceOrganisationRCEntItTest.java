package ch.vd.uniregctb.interfaces;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.ResourceUtils;
import org.xml.sax.SAXException;

import ch.vd.evd0022.v3.OrganisationData;
import ch.vd.evd0023.v3.ObjectFactory;
import ch.vd.unireg.interfaces.organisation.ServiceOrganisationRaw;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.unireg.wsclient.rcent.RcEntClient;
import ch.vd.unireg.wsclient.rcent.RcEntClientImpl;
import ch.vd.unireg.xml.tools.ClasspathCatalogResolver;
import ch.vd.uniregctb.common.BusinessItTest;
import ch.vd.uniregctb.interfaces.service.ServiceOrganisationService;

@SuppressWarnings({"JavaDoc"})
public class ServiceOrganisationRCEntItTest extends BusinessItTest {

	public static final String[] RCENT_SCHEMA = new String[]{
			"eVD-0004-3-0.xsd",
			"eVD-0022-3-1.xsd",
			"eVD-0023-3-1.xsd",
			"eVD-0024-3-1.xsd"
	};

	private static final String BASE_PATH_ORGANISATION = "/organisation/CT.VD.PARTY";
	private static final String BASE_PATH_ORGANISATIONS_OF_NOTICE = "/organisationsOfNotice";

	// Organisation cible sur RCEnt
	private static final long ID_BCV = 101544776L;
	private static final String NOM_BCV = "Banque Cantonale Vaudoise";

	// Evénement cible sur RCEnt
	private static final long ID_EVT = 4466328L;
	private static final long ID_ORGANISATION_EVT = 101584800L;
	private static final String ID_NOM_EVT = "Global Health Consulting Sàrl";

	// Entreprise avec succursale avec numéro IDE dans RCEnt
	private static final long ID_SUCC = 101067201L;
	private static final long ID_ORGANISATION_SUCC = 101671015L;
	private static final String IDE_SUCC = "CHE169212759";

	// Organisation de l'échantillon fichier
	private static final long ID_BOMACO = 101636326L;
	private static final String BOMACO_SÀRL_EN_LIQUIDATION = "Bomaco Sàrl en liquidation";
	private static final String FILE_SAMPLE_ORGANISATION_100983251_HISTORY = "classpath:ch/vd/uniregctb/interfaces/organisation-bomaco-history.xml";

	private String baseUrl;

	private String username = "";
	private String password = "";

	private ServiceOrganisationService service;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		username = uniregProperties.getAllProperties().get("testprop.webservice.rcent.username");
		password = uniregProperties.getAllProperties().get("testprop.webservice.rcent.password");
		baseUrl = uniregProperties.getAllProperties().get("testprop.webservice.rcent.url");

		service = getBean(ServiceOrganisationService.class, "serviceOrganisationService");
	}

	@Test
	public void testGetOrganisation() throws Exception {
		Organisation org = service.getOrganisationHistory(ID_BCV);
		Assert.assertNotNull(org);
		assertContains(NOM_BCV, org.getNom().get(0).getPayload());
	}

//	@Ignore
	@Test
	public void testGetPseudoOrganisationHistory() throws Exception {
		Organisation org = service.getPseudoOrganisationHistory(ID_EVT).get(ID_ORGANISATION_EVT);
		Assert.assertNotNull(org);
		assertContains(ID_NOM_EVT, org.getNom().get(0).getPayload());
	}

	@Test
	public void testGetOrganisationByNoIde() throws Exception {
		final ServiceOrganisationRaw.Identifiers ids = service.getOrganisationByNoIde(IDE_SUCC);
		Assert.assertNotNull(ids);
		Assert.assertEquals(ID_ORGANISATION_SUCC, ids.idCantonalOrganisation);
		Assert.assertEquals(ID_SUCC, ids.idCantonalSite);
	}

	@Test
	public void testRCEntClientGetOrganisationWithoutValidation() throws Exception {
		final RcEntClient client = createRCEntClient(false);
		OrganisationData data = client.getOrganisation(ID_BCV, null, true);
		Assert.assertNotNull(data);
		Assert.assertEquals(ID_BCV, data.getOrganisationSnapshot().get(0).getOrganisation().getCantonalId().longValue());
		Assert.assertEquals(NOM_BCV, data.getOrganisationSnapshot().get(0).getOrganisation().getOrganisationLocation().get(0).getName());
	}

	@Test
	public void testRCEntClientGetOrganisationWithValidation() throws Exception {
		final RcEntClient client = createRCEntClient(true);
		OrganisationData data = client.getOrganisation(ID_BCV, null, true);
		Assert.assertNotNull(data);
		Assert.assertEquals(ID_BCV, data.getOrganisationSnapshot().get(0).getOrganisation().getCantonalId().longValue());
		Assert.assertEquals(NOM_BCV, data.getOrganisationSnapshot().get(0).getOrganisation().getOrganisationLocation().get(0).getName());
	}

	@Test
	public void testDirectGetOrganisationWithoutValidation() throws Exception {
		String url = baseUrl + BASE_PATH_ORGANISATION + "/" + ID_BCV;
		String xml = getUrlContent(url);
		OrganisationData data = (OrganisationData) ((JAXBElement) createMarshaller(false).unmarshal(new StringReader(xml))).getValue();
		Assert.assertNotNull(data);
		Assert.assertEquals(ID_BCV, data.getOrganisationSnapshot().get(0).getOrganisation().getCantonalId().longValue());
		Assert.assertEquals(NOM_BCV, data.getOrganisationSnapshot().get(0).getOrganisation().getOrganisationLocation().get(0).getName());
	}

	@Test
	public void testDirectGetOrganisationWithValidation() throws Exception {
		String url = baseUrl + BASE_PATH_ORGANISATION + "/" + ID_BCV;
		String xml = getUrlContent(url);
		OrganisationData data = (OrganisationData) ((JAXBElement) createMarshaller(true).unmarshal(new StringReader(xml))).getValue();
		Assert.assertNotNull(data);
		Assert.assertEquals(ID_BCV, data.getOrganisationSnapshot().get(0).getOrganisation().getCantonalId().longValue());
		Assert.assertEquals(NOM_BCV, data.getOrganisationSnapshot().get(0).getOrganisation().getOrganisationLocation().get(0).getName());
	}

	@Test
	public void testSampleOrganisationWithValidation() throws Exception {
		OrganisationData data = (OrganisationData) ((JAXBElement) createMarshaller(true).unmarshal(new StringReader(loadFile(FILE_SAMPLE_ORGANISATION_100983251_HISTORY)))).getValue();
		Assert.assertNotNull(data);
		Assert.assertEquals(ID_BOMACO, data.getOrganisationSnapshot().get(0).getOrganisation().getCantonalId().longValue());
		Assert.assertEquals(BOMACO_SÀRL_EN_LIQUIDATION, data.getOrganisationSnapshot().get(0).getOrganisation().getOrganisationLocation().get(0).getName());
	}

	private RcEntClient createRCEntClient(boolean validating) throws Exception {
		RcEntClientImpl client = new RcEntClientImpl();
		client.setUsername(username);
		client.setPassword(password);
		client.setBaseUrl(baseUrl);
		client.setOrganisationPath(BASE_PATH_ORGANISATION);
		client.setOrganisationsOfNoticePath(BASE_PATH_ORGANISATIONS_OF_NOTICE);
		if (validating) {
			client.setSchemasLocations(Arrays.asList(RCENT_SCHEMA));
			client.setValidationEnabled(true);
		}
		client.afterPropertiesSet();
		return client;
	}

	private Unmarshaller createMarshaller(boolean validate) throws JAXBException, IOException, SAXException {
	/*
		Initialize unmarshaller
	 */
		String pack = ObjectFactory.class.getPackage().getName();
		JAXBContext jaxbContext = JAXBContext.newInstance(pack);

		Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();

		if (validate) {
			// This will make the unmarshaller validate the input.
			final SchemaFactory sf = SchemaFactory.newInstance(javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI);
			sf.setResourceResolver(new ClasspathCatalogResolver());
			final Source[] source = getClasspathSources(RCENT_SCHEMA);
			Schema schema = sf.newSchema(source);
			unmarshaller.setSchema(schema);
		}
		return unmarshaller;
	}

	private static Source[] getClasspathSources(String... pathes) throws IOException {
		final Source[] sources = new Source[pathes.length];
		for (int i = 0, pathLength = pathes.length; i < pathLength; i++) {
			final String path = pathes[i];
			sources[i] = new StreamSource(new ClassPathResource(path).getURL().toExternalForm());
		}
		return sources;
	}

	@NotNull
	private String getUrlContent(String url) throws IOException {
		URL rcentUrl = new URL(url);

		URLConnection rcent = rcentUrl.openConnection();

		if (username != null && !"".equals(username)) {
			String userpass = username + ":" + password;
			String basicAuth = "Basic " + new String(Base64.encodeBase64(userpass.getBytes()));
			rcent.setRequestProperty("Authorization", basicAuth);
		}

		BufferedReader in = new BufferedReader(
				new InputStreamReader(rcent.getInputStream()));

		StringBuilder b = new StringBuilder();
		String inputLine;
		while ((inputLine = in.readLine()) != null) {
			b.append(inputLine);
		}
		return b.toString();
	}

	private String loadFile(String filename) throws IOException {
		final File file = ResourceUtils.getFile(filename);
		return FileUtils.readFileToString(file);
	}

}
