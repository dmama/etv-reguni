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
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.Base64;

import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.xml.sax.SAXException;

import ch.vd.evd0022.v1.OrganisationData;
import ch.vd.evd0023.v1.ObjectFactory;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.unireg.wsclient.rcent.RcEntClient;
import ch.vd.unireg.wsclient.rcent.RcEntClientImpl;
import ch.vd.unireg.xml.tools.ClasspathCatalogResolver;
import ch.vd.uniregctb.common.BusinessItTest;
import ch.vd.uniregctb.interfaces.service.ServiceOrganisationService;

import static org.junit.Assert.assertNotNull;

@SuppressWarnings({"JavaDoc"})
public class ServiceOrganisationServiceTest extends BusinessItTest {

	public static final String[] RCENT_SCHEMA = new String[]{
			"eVD-0004-3-0.xsd",
			"eVD-0021-1-0.xsd",
			"eVD-0022-1-0.xsd",
			"eVD-0023-1-0.xsd",
			"eVD-0024-1-0.xsd"
	};

	private static final String BASE_PATH_ORGANISATION = "/v1/organisation/CT.VD.PARTY";

	// Organisation cible pour les tests. Une seule suffit.
	private static final long NO100983251 = 100983251L;
	private static final String BOMACO_SÀRL_EN_LIQUIDATION = "Bomaco Sàrl en liquidation";
	private static final String BASE_PATH_ORGANISATIONS_OF_NOTICE = "/v1/organisationsOfNotice";

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

	// FIXME: Remove @Ignore as soon as RCEnt is complient with itself!
	@Ignore
	@Test
	public void testGetOrganisation() throws Exception {
		Organisation org = service.getOrganisationHistory(NO100983251);
		assertNotNull(org);
		assertContains(BOMACO_SÀRL_EN_LIQUIDATION, org.getNom().get(0).getPayload());
	}

	@Test
	public void testRCEntClientGetOrganisationWithoutValidation() throws Exception {
		final RcEntClient client = createRCEntClient(false);
		OrganisationData data = client.getOrganisation(NO100983251, null, true);
		Assert.assertNotNull(data);
		Assert.assertEquals(NO100983251, data.getOrganisationSnapshot().get(0).getOrganisation().getCantonalId().longValue());
		Assert.assertEquals(BOMACO_SÀRL_EN_LIQUIDATION, data.getOrganisationSnapshot().get(0).getOrganisation().getOrganisationName());
	}

	// FIXME: Remove @Ignore as soon as RCEnt is complient with itself!
	@Ignore
	@Test
	public void testRCEntClientGetOrganisationWithValidation() throws Exception {
		final RcEntClient client = createRCEntClient(true);
		OrganisationData data = client.getOrganisation(NO100983251, null, true);
		Assert.assertNotNull(data);
		Assert.assertEquals(NO100983251, data.getOrganisationSnapshot().get(0).getOrganisation().getCantonalId().longValue());
		Assert.assertEquals(BOMACO_SÀRL_EN_LIQUIDATION, data.getOrganisationSnapshot().get(0).getOrganisation().getOrganisationName());
	}

	@Test
	public void testDirectGetOrganisationWithoutValidation() throws Exception {
		String url = baseUrl + BASE_PATH_ORGANISATION + "/" + NO100983251;
		String xml = getUrlContent(url);
		OrganisationData data = (OrganisationData) ((JAXBElement) createMarshaller(false).unmarshal(new StringReader(xml))).getValue();
		Assert.assertNotNull(data);
		Assert.assertEquals(NO100983251, data.getOrganisationSnapshot().get(0).getOrganisation().getCantonalId().longValue());
		Assert.assertEquals(BOMACO_SÀRL_EN_LIQUIDATION, data.getOrganisationSnapshot().get(0).getOrganisation().getOrganisationName());
	}

	// FIXME: Remove @Ignore as soon as RCEnt is complient with itself!
	@Ignore
	@Test
	public void testDirectGetOrganisationWithValidation() throws Exception {
		String url = baseUrl + BASE_PATH_ORGANISATION + "/" + NO100983251;
		String xml = getUrlContent(url);
		OrganisationData data = (OrganisationData) ((JAXBElement) createMarshaller(true).unmarshal(new StringReader(xml))).getValue();
		Assert.assertNotNull(data);
		Assert.assertEquals(NO100983251, data.getOrganisationSnapshot().get(0).getOrganisation().getCantonalId().longValue());
		Assert.assertEquals(BOMACO_SÀRL_EN_LIQUIDATION, data.getOrganisationSnapshot().get(0).getOrganisation().getOrganisationName());
	}

	@Test
	public void testSampleOrganisationWithValidation() throws Exception {
		OrganisationData data = (OrganisationData) ((JAXBElement) createMarshaller(true).unmarshal(new StringReader(getSample()))).getValue();
		Assert.assertNotNull(data);
		Assert.assertEquals(NO100983251, data.getOrganisationSnapshot().get(0).getOrganisation().getCantonalId().longValue());
		Assert.assertEquals(BOMACO_SÀRL_EN_LIQUIDATION, data.getOrganisationSnapshot().get(0).getOrganisation().getOrganisationName());
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
			String basicAuth = "Basic " + new String(Base64.getEncoder().encode(userpass.getBytes()));
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

	private String getSample() {
		return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
				"<organisationData xmlns=\"http://evd.vd.ch/xmlns/eVD-0023/1\">\n" +
				"\t<eVD-0022:organisationSnapshot xmlns:eVD-0022=\"http://evd.vd.ch/xmlns/eVD-0022/1\">\n" +
				"\t\t<beginValidityDate xmlns=\"http://evd.vd.ch/xmlns/eVD-0022/1\">2015-08-05</beginValidityDate>\n" +
				"\t\t<eVD-0022:organisation>\n" +
				"\t\t\t<cantonalId xmlns=\"http://evd.vd.ch/xmlns/eVD-0022/1\">100983251</cantonalId>\n" +
				"\t\t\t<eVD-0022:organisationIdentifier>\n" +
				"\t\t\t\t<identifierCategory xmlns=\"http://evd.vd.ch/xmlns/eVD-0022/1\">CH.IDE</identifierCategory>\n" +
				"\t\t\t\t<identifierValue xmlns=\"http://evd.vd.ch/xmlns/eVD-0022/1\">CHE113570477</identifierValue>\n" +
				"\t\t\t</eVD-0022:organisationIdentifier>\n" +
				"\t\t\t<eVD-0022:organisationIdentifier>\n" +
				"\t\t\t\t<identifierCategory xmlns=\"http://evd.vd.ch/xmlns/eVD-0022/1\">CH.RC</identifierCategory>\n" +
				"\t\t\t\t<identifierValue xmlns=\"http://evd.vd.ch/xmlns/eVD-0022/1\">CH55010502284</identifierValue>\n" +
				"\t\t\t</eVD-0022:organisationIdentifier>\n" +
				"\t\t\t<organisationName xmlns=\"http://evd.vd.ch/xmlns/eVD-0022/1\">Bomaco Sàrl en liquidation</organisationName>\n" +
				"\t\t\t<legalForm xmlns=\"http://evd.vd.ch/xmlns/eVD-0022/1\">0107</legalForm>\n" +
				"\t\t\t<eVD-0022:organisationLocation>\n" +
				"\t\t\t\t<cantonalId xmlns=\"http://evd.vd.ch/xmlns/eVD-0022/1\">100983252</cantonalId>\n" +
				"\t\t\t\t<eVD-0022:identifier>\n" +
				"\t\t\t\t\t<identifierCategory xmlns=\"http://evd.vd.ch/xmlns/eVD-0022/1\">CH.IDE</identifierCategory>\n" +
				"\t\t\t\t\t<identifierValue xmlns=\"http://evd.vd.ch/xmlns/eVD-0022/1\">CHE113570477</identifierValue>\n" +
				"\t\t\t\t</eVD-0022:identifier>\n" +
				"\t\t\t\t<eVD-0022:identifier>\n" +
				"\t\t\t\t\t<identifierCategory xmlns=\"http://evd.vd.ch/xmlns/eVD-0022/1\">CH.RC</identifierCategory>\n" +
				"\t\t\t\t\t<identifierValue xmlns=\"http://evd.vd.ch/xmlns/eVD-0022/1\">CH55010502284</identifierValue>\n" +
				"\t\t\t\t</eVD-0022:identifier>\n" +
				"\t\t\t\t<name xmlns=\"http://evd.vd.ch/xmlns/eVD-0022/1\">Bomaco Sàrl en liquidation</name>\n" +
				"\t\t\t\t<kindOfLocation xmlns=\"http://evd.vd.ch/xmlns/eVD-0022/1\">1</kindOfLocation>\n" +
				"\t\t\t\t<eVD-0022:seat>\n" +
				"\t\t\t\t\t<municipalityId xmlns=\"http://evd.vd.ch/xmlns/eVD-0022/1\">5518</municipalityId>\n" +
				"\t\t\t\t\t<municipalityName xmlns=\"http://evd.vd.ch/xmlns/eVD-0022/1\">Echallens</municipalityName>\n" +
				"\t\t\t\t\t<cantonAbbreviation xmlns=\"http://evd.vd.ch/xmlns/eVD-0022/1\">VD</cantonAbbreviation>\n" +
				"\t\t\t\t\t<historyMunicipalityId xmlns=\"http://evd.vd.ch/xmlns/eVD-0022/1\">14717</historyMunicipalityId>\n" +
				"\t\t\t\t</eVD-0022:seat>\n" +
				"\t\t\t\t<commercialRegisterData xmlns=\"http://evd.vd.ch/xmlns/eVD-0022/1\">\n" +
				"\t\t\t\t\t<name>Bomaco Sàrl en liquidation</name>\n" +
				"\t\t\t\t\t<status>2</status>\n" +
				"\t\t\t\t\t<entryStatus>1</entryStatus>\n" +
				"\t\t\t\t\t<entryDate>2007-04-16</entryDate>\n" +
				"\t\t\t\t</commercialRegisterData>\n" +
				"\t\t\t\t<nogaCode xmlns=\"http://evd.vd.ch/xmlns/eVD-0022/1\">469000</nogaCode>\n" +
				"\t\t\t\t<uidRegisterData xmlns=\"http://evd.vd.ch/xmlns/eVD-0022/1\">\n" +
				"\t\t\t\t\t<status>3</status>\n" +
				"\t\t\t\t\t<typeOfOrganisation>1</typeOfOrganisation>\n" +
				"\t\t\t\t\t<eVD-0022:effectiveAddress xmlns:eVD-0021=\"http://evd.vd.ch/xmlns/eVD-0021/1\">\n" +
				"\t\t\t\t\t\t<eVD-0021:street>Chemin de l'Arzillier</eVD-0021:street>\n" +
				"\t\t\t\t\t\t<eVD-0021:houseNumber>1</eVD-0021:houseNumber>\n" +
				"\t\t\t\t\t\t<eVD-0021:town>Echallens</eVD-0021:town>\n" +
				"\t\t\t\t\t\t<eVD-0021:swissZipCode>1040</eVD-0021:swissZipCode>\n" +
				"\t\t\t\t\t\t<eVD-0021:country>\n" +
				"\t\t\t\t\t\t\t<eVD-0021:countryIdISO2>CH</eVD-0021:countryIdISO2>\n" +
				"\t\t\t\t\t\t\t<eVD-0021:countryName>CH</eVD-0021:countryName>\n" +
				"\t\t\t\t\t\t</eVD-0021:country>\n" +
				"\t\t\t\t\t\t<eVD-0021:federalBuildingId>868353</eVD-0021:federalBuildingId>\n" +
				"\t\t\t\t\t\t<eVD-0021:xCoordinate>538587</eVD-0021:xCoordinate>\n" +
				"\t\t\t\t\t\t<eVD-0021:yCoordinate>166384</eVD-0021:yCoordinate>\n" +
				"\t\t\t\t\t</eVD-0022:effectiveAddress>\n" +
				"\t\t\t\t\t<publicStatus>1</publicStatus>\n" +
				"\t\t\t\t</uidRegisterData>\n" +
				"\t\t\t</eVD-0022:organisationLocation>\n" +
				"\t\t</eVD-0022:organisation>\n" +
				"\t</eVD-0022:organisationSnapshot>\n" +
				"</organisationData>\n";
	}

}
