package ch.vd.unireg.interfaces;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.Source;
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
import java.util.List;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.springframework.data.domain.Page;
import org.springframework.util.ResourceUtils;
import org.xml.sax.SAXException;

import ch.vd.evd0022.v3.NoticeRequestReport;
import ch.vd.evd0022.v3.OrganisationData;
import ch.vd.evd0022.v3.OrganisationLocation;
import ch.vd.evd0022.v3.TypeOfLocation;
import ch.vd.evd0023.v3.ListOfNoticeRequest;
import ch.vd.evd0023.v3.ObjectFactory;
import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.common.Adresse;
import ch.vd.unireg.interfaces.infra.mock.MockLocalite;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.interfaces.organisation.ServiceOrganisationRaw;
import ch.vd.unireg.interfaces.organisation.data.AdresseAnnonceIDERCEnt;
import ch.vd.unireg.interfaces.organisation.data.AnnonceIDEData;
import ch.vd.unireg.interfaces.organisation.data.AnnonceIDEEnvoyee;
import ch.vd.unireg.interfaces.organisation.data.BaseAnnonceIDE;
import ch.vd.unireg.interfaces.organisation.data.FormeLegale;
import ch.vd.unireg.interfaces.organisation.data.NumeroIDE;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.unireg.interfaces.organisation.data.ProtoAnnonceIDE;
import ch.vd.unireg.interfaces.organisation.data.StatutAnnonce;
import ch.vd.unireg.interfaces.organisation.data.TypeAnnonce;
import ch.vd.unireg.interfaces.organisation.data.TypeDeSite;
import ch.vd.unireg.interfaces.organisation.rcent.RCEntAnnonceIDEHelper;
import ch.vd.unireg.interfaces.organisation.rcent.RCEntSchemaHelper;
import ch.vd.unireg.wsclient.WebClientPool;
import ch.vd.unireg.wsclient.rcent.RcEntClient;
import ch.vd.unireg.wsclient.rcent.RcEntClientImpl;
import ch.vd.unireg.wsclient.rcent.RcEntNoticeQuery;
import ch.vd.unireg.xml.tools.ClasspathCatalogResolver;
import ch.vd.unireg.common.BusinessItTest;
import ch.vd.unireg.interfaces.model.AdressesCivilesHisto;
import ch.vd.unireg.interfaces.service.ServiceOrganisationService;
import ch.vd.unireg.type.TypeAdresseCivil;

import static ch.vd.unireg.interfaces.civil.data.IndividuRCPersTest.assertAdresse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@SuppressWarnings({"JavaDoc"})
public class ServiceOrganisationRCEntItTest extends BusinessItTest {

	private static final String BASE_PATH_ORGANISATION = "/organisation/CT.VD.PARTY";
	private static final String BASE_PATH_ORGANISATIONS_OF_NOTICE = "/organisationsOfNotice";
	private static final String BASE_PATH_VALIDER_ANNONCE_IDE = "/noticeRequestValidate/";
	private static final String BASE_PATH_ANNONCE_IDE = "/noticeRequestList";

	// Organisation cible sur RCEnt
	private static final long ID_BCV = 101544776L;
	private static final String NOM_BCV = "Banque Cantonale Vaudoise";

	// Evénement cible sur RCEnt
	private static final long ID_EVT = 471661;
	private static final long ID_ORGANISATION_EVT = 101773699L;
	private static final String ID_NOM_EVT = "HONESTY SA";

	// Entreprise avec succursale avec numéro IDE dans RCEnt
	private static final long ID_SUCC = 101067201L;
	private static final long ID_ORGANISATION_SUCC = 101671015L;
	private static final String IDE_SUCC = "CHE169212759";

	// Organisation de l'échantillon fichier
	private static final long ID_BOMACO = 101636326L;
	private static final String BOMACO_SÀRL_EN_LIQUIDATION = "Bomaco Sàrl en liquidation";
	private static final String FILE_SAMPLE_ORGANISATION_100983251_HISTORY = "classpath:ch/vd/unireg/interfaces/organisation-bomaco-history.xml";

	// annonce à l'IDE sur RCEnt
	private static final String USER_ID = "unireg";
	private static final long ID_ANNONCE = 180007882L;

	private String baseUrl;

	private String username = "";
	private String password = "";

	private ServiceOrganisationService service;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		username = uniregProperties.getProperty("testprop.webservice.rcent.username");
		password = uniregProperties.getProperty("testprop.webservice.rcent.password");
		baseUrl = uniregProperties.getProperty("testprop.webservice.rcent.url");

		service = getBean(ServiceOrganisationService.class, "serviceOrganisationService");
	}

	@Test(timeout = 30000)
	public void testGetOrganisation() {
		Organisation org = service.getOrganisationHistory(ID_BCV);
		assertNotNull(org);
		assertContains(NOM_BCV, org.getNom().get(0).getPayload());
	}

//	@Ignore
	@Test(timeout = 30000)
	public void testGetPseudoOrganisationHistory() {
		Organisation org = service.getOrganisationEvent(ID_EVT).get(ID_ORGANISATION_EVT).getPseudoHistory();
		assertNotNull(org);
		assertContains(ID_NOM_EVT, org.getNom().get(0).getPayload());
	}

	@Test(timeout = 30000)
	public void testGetOrganisationByNoIde() {
		final ServiceOrganisationRaw.Identifiers ids = service.getOrganisationByNoIde(IDE_SUCC);
		assertNotNull(ids);
		assertEquals(ID_ORGANISATION_SUCC, ids.idCantonalOrganisation);
		assertEquals(ID_SUCC, ids.idCantonalSite);
	}

	@Test(timeout = 30000)
	public void testRCEntClientGetOrganisationWithoutValidation() throws Exception {
		final RcEntClient client = createRCEntClient(false);
		OrganisationData data = client.getOrganisation(ID_BCV, null, true);
		assertNotNull(data);
		assertEquals(ID_BCV, data.getOrganisationSnapshot().get(0).getOrganisation().getCantonalId().longValue());

		// la BCV possède maintenant, depuis le chargement REE, quelques établissements secondaires... il faut donc trouver l'établissement principal
		boolean foundPrincipal = false;
		for (OrganisationLocation location : data.getOrganisationSnapshot().get(0).getOrganisation().getOrganisationLocation()) {
			if (location.getTypeOfLocation() == TypeOfLocation.ETABLISSEMENT_PRINCIPAL) {
				assertFalse(foundPrincipal);     // on ne doit le trouver qu'une seule fois !
				foundPrincipal = true;
				assertEquals(NOM_BCV, location.getName());
			}
		}
		assertTrue(foundPrincipal);
	}

	@Test(timeout = 30000)
	public void testRCEntClientGetOrganisationWithValidation() throws Exception {
		final RcEntClient client = createRCEntClient(true);
		OrganisationData data = client.getOrganisation(ID_BCV, null, true);
		assertNotNull(data);
		assertEquals(ID_BCV, data.getOrganisationSnapshot().get(0).getOrganisation().getCantonalId().longValue());

		// la BCV possède maintenant, depuis le chargement REE, quelques établissements secondaires... il faut donc trouver l'établissement principal
		boolean foundPrincipal = false;
		for (OrganisationLocation location : data.getOrganisationSnapshot().get(0).getOrganisation().getOrganisationLocation()) {
			if (location.getTypeOfLocation() == TypeOfLocation.ETABLISSEMENT_PRINCIPAL) {
				assertFalse(foundPrincipal);     // on ne doit le trouver qu'une seule fois !
				foundPrincipal = true;
				assertEquals(NOM_BCV, location.getName());
			}
		}
		assertTrue(foundPrincipal);
	}

	@Test(timeout = 30000)
	public void testDirectGetOrganisationWithoutValidation() throws Exception {
		String url = baseUrl + BASE_PATH_ORGANISATION + "/" + ID_BCV;
		String xml = getUrlContent(url);
		OrganisationData data = (OrganisationData) ((JAXBElement) createMarshaller(false).unmarshal(new StringReader(xml))).getValue();
		assertNotNull(data);
		assertEquals(ID_BCV, data.getOrganisationSnapshot().get(0).getOrganisation().getCantonalId().longValue());

		// la BCV possède maintenant, depuis le chargement REE, quelques établissements secondaires... il faut donc trouver l'établissement principal
		boolean foundPrincipal = false;
		for (OrganisationLocation location : data.getOrganisationSnapshot().get(0).getOrganisation().getOrganisationLocation()) {
			if (location.getTypeOfLocation() == TypeOfLocation.ETABLISSEMENT_PRINCIPAL) {
				assertFalse(foundPrincipal);     // on ne doit le trouver qu'une seule fois !
				foundPrincipal = true;
				assertEquals(NOM_BCV, location.getName());
			}
		}
		assertTrue(foundPrincipal);
	}

	@Test(timeout = 30000)
	public void testDirectGetOrganisationWithValidation() throws Exception {
		String url = baseUrl + BASE_PATH_ORGANISATION + "/" + ID_BCV;
		String xml = getUrlContent(url);
		OrganisationData data = (OrganisationData) ((JAXBElement) createMarshaller(true).unmarshal(new StringReader(xml))).getValue();
		assertNotNull(data);
		assertEquals(ID_BCV, data.getOrganisationSnapshot().get(0).getOrganisation().getCantonalId().longValue());

		// la BCV possède maintenant, depuis le chargement REE, quelques établissements secondaires... il faut donc trouver l'établissement principal
		boolean foundPrincipal = false;
		for (OrganisationLocation location : data.getOrganisationSnapshot().get(0).getOrganisation().getOrganisationLocation()) {
			if (location.getTypeOfLocation() == TypeOfLocation.ETABLISSEMENT_PRINCIPAL) {
				assertFalse(foundPrincipal);     // on ne doit le trouver qu'une seule fois !
				foundPrincipal = true;
				assertEquals(NOM_BCV, location.getName());
			}
		}
		assertTrue(foundPrincipal);
	}

	@Test(timeout = 30000)
	public void testSampleOrganisationWithValidation() throws Exception {
		OrganisationData data = (OrganisationData) ((JAXBElement) createMarshaller(true).unmarshal(new StringReader(loadFile(FILE_SAMPLE_ORGANISATION_100983251_HISTORY)))).getValue();
		assertNotNull(data);
		assertEquals(ID_BOMACO, data.getOrganisationSnapshot().get(0).getOrganisation().getCantonalId().longValue());
		assertEquals(BOMACO_SÀRL_EN_LIQUIDATION, data.getOrganisationSnapshot().get(0).getOrganisation().getOrganisationLocation().get(0).getName());
	}

	@Test(timeout = 30000)
	public void testDirectGetAnnonceIDE() throws Exception {
		String url = baseUrl + BASE_PATH_ANNONCE_IDE + "?userId=" + USER_ID + "&noticeRequestId=" + ID_ANNONCE;
		String xml = getUrlContent(url);
		final ListOfNoticeRequest listOfNoticeRequest = (ListOfNoticeRequest) ((JAXBElement) createMarshaller(true).unmarshal(new StringReader(xml))).getValue();

		assertNotNull(listOfNoticeRequest);
		assertEquals(1, listOfNoticeRequest.getNumberOfResults());

	}

	@Test(timeout = 30000)
	public void testRCEntClientGetAnnonceIDE() throws Exception {
		final RcEntClient client = createRCEntClient(true);
		final RcEntNoticeQuery rcEntNoticeQuery = new RcEntNoticeQuery();
		rcEntNoticeQuery.setUserId(USER_ID);
		rcEntNoticeQuery.setNoticeId(ID_ANNONCE);
		final Page<NoticeRequestReport> pages = client.findNotices(rcEntNoticeQuery, null, 1, 10);
		assertNotNull(pages);
		assertEquals(1, pages.getTotalElements());
		final List<NoticeRequestReport> listOfNoticeRequest = pages.getContent();
		assertEquals(1, listOfNoticeRequest.size());
		assertEquals(Long.toString(ID_ANNONCE), listOfNoticeRequest.get(0).getNoticeRequest().getNoticeRequestHeader().getNoticeRequestIdentification().getNoticeRequestId());
	}

	@Test(timeout = 30000)
	public void testGetAnnonceIDE() {
		final AnnonceIDEEnvoyee annonceIDE = service.getAnnonceIDE(ID_ANNONCE, null);

		assertNotNull(annonceIDE);
		assertEquals(ID_ANNONCE, annonceIDE.getNumero().longValue());
		assertEquals(StatutAnnonce.ACCEPTE_IDE, annonceIDE.getStatut().getStatut());
	}

	@Test(timeout = 30000)
	public void testValidateProtoAnnonceIDE() {

		final AdresseAnnonceIDERCEnt adresse = RCEntAnnonceIDEHelper
				.createAdresseAnnonceIDERCEnt("Rue du Marais", "1", null, MockLocalite.Geneve.getNPA(), null, MockLocalite.Geneve.getNoOrdre(), "Genève", MockPays.Suisse.getNoOfsEtatSouverain(), MockPays.Suisse.getCodeIso2(), MockPays.Suisse.getNomCourt(), null,
				                              null, null);
		ProtoAnnonceIDE proto = RCEntAnnonceIDEHelper.createProtoAnnonceIDE(TypeAnnonce.CREATION, DateHelper.getCurrentDate(), RCEntAnnonceIDEHelper.UNIREG_USER, null, TypeDeSite.ETABLISSEMENT_PRINCIPAL, null, null,
		                                                                    null, null, null, null, null, null, "Syntruc Asso", null, FormeLegale.N_0109_ASSOCIATION, "Fabrication d'objet synthétiques",
		                                                                    adresse, null, RCEntAnnonceIDEHelper.SERVICE_IDE_UNIREG);
		final BaseAnnonceIDE.Statut statut = service.validerAnnonceIDE(proto);

		assertNotNull("La validation de l'annonce n'a pas renvoyé de statut.", statut);
		assertEquals(StatutAnnonce.VALIDATION_SANS_ERREUR, statut.getStatut());
		assertNull(statut.getErreurs());
	}

	@Test(timeout = 30000)
	public void testValidateAnnonceIDEPourrie() {
		final ProtoAnnonceIDE proto = new ProtoAnnonceIDE(TypeAnnonce.CREATION,
		                                                  DateHelper.getCurrentDate(),
		                                                  new AnnonceIDEData.UtilisateurImpl(RCEntAnnonceIDEHelper.UNIREG_USER, null),
		                                                  TypeDeSite.ETABLISSEMENT_PRINCIPAL,
		                                                  null,
		                                                  new AnnonceIDEData.InfoServiceIDEObligEtenduesImpl(RCEntAnnonceIDEHelper.NO_IDE_ADMINISTRATION_CANTONALE_DES_IMPOTS,
		                                                                                                     RCEntAnnonceIDEHelper.NO_APPLICATION_UNIREG,
		                                                                                                     RCEntAnnonceIDEHelper.NOM_APPLICATION_UNIREG));
		final BaseAnnonceIDE.Statut statut = service.validerAnnonceIDE(proto);

		assertNotNull("La validation de l'annonce n'a pas renvoyé de statut.", statut);
		// TODO: check le contenu
	}

	@Test(timeout = 30000)
	public void testValidateAnnonceIDEUnPeuMoinsPourrie() {
		final ProtoAnnonceIDE proto = RCEntAnnonceIDEHelper.createProtoAnnonceIDE(TypeAnnonce.MUTATION,
		                                                                          DateHelper.getCurrentDate(),
		                                                                          RCEntAnnonceIDEHelper.UNIREG_USER,
		                                                                          null,
		                                                                          TypeDeSite.ETABLISSEMENT_PRINCIPAL,
		                                                                          null,
		                                                                          null,
		                                                                          new NumeroIDE("CHE999999998"),
		                                                                          null,
		                                                                          null,
		                                                                          null,
		                                                                          null,
		                                                                          null,
		                                                                          "Syntruc Asso",
		                                                                          null,
		                                                                          FormeLegale.N_0109_ASSOCIATION,
		                                                                          "Fabrication d'objet synthétiques",
		                                                                          null,
		                                                                          null,
		                                                                          RCEntAnnonceIDEHelper.SERVICE_IDE_UNIREG);
		final BaseAnnonceIDE.Statut statut = service.validerAnnonceIDE(proto);

		assertNotNull("La validation de l'annonce n'a pas renvoyé de statut.", statut);
		// TODO: Meilleurs contrôle du contenu quand les messages retournés auront été réparés
	}

	/**
	 * [SIFISC-24996] Ce test vérifie que les adresses <i>case postale</i> d'une entreprises sont bien retournées par la méthode <i>getAdressesOrganisationHisto</i>.
	 */
	@Test(timeout = 30000)
	public void testGetAdressesOrganisationHistoAvecBoitePostale() {

		// tiers 34301, numéro cantonal = 101830038
		final AdressesCivilesHisto adresses = service.getAdressesOrganisationHisto(101830038);
		assertNotNull(adresses);
		assertEquals(0, adresses.principales.size());
		assertEquals(0, adresses.secondaires.size());
		assertEquals(0, adresses.tutelles.size());

		assertEquals(1, adresses.courriers.size());
//		assertAdresse(TypeAdresseCivil.COURRIER, RegDate.get(2016, 9, 21), RegDate.get(2017, 8, 22), null, "Savigny", adresses.courriers.get(0));
		assertAdresse(TypeAdresseCivil.COURRIER, RegDate.get(2017, 8, 23), null, "Route de Vevey", "Forel (Lavaux)", adresses.courriers.get(0));

		assertEquals(1, adresses.casesPostales.size());
		assertAdresse(TypeAdresseCivil.CASE_POSTALE, RegDate.get(2017, 8, 23), null, null, "Savigny", adresses.casesPostales.get(0));
		assertEquals("Case Postale 38", adresses.casesPostales.get(0).getCasePostale().toString());
	}

	/**
	 * [SIFISC-24996] Ce test vérifie que les adresses <i>case postale</i> d'une entreprises sont bien exposées sur l'entreprises retournée par la méthode <i>getOrganisationHistory</i>.
	 */
	@Test(timeout = 30000)
	public void testGetOrganisationHistoryAvecBoitePostale() {

		// tiers 34301, numéro cantonal = 101830038
		final Organisation org = service.getOrganisationHistory(101830038);
		assertNotNull(org);

		final List<Adresse> adresses = org.getAdresses();
		assertEquals(2, adresses.size());
//		assertAdresse(TypeAdresseCivil.COURRIER, RegDate.get(2016, 9, 21), RegDate.get(2017, 8, 22), null, "Savigny", adresses.get(0));
		assertAdresse(TypeAdresseCivil.COURRIER, RegDate.get(2017, 8, 23), null, "Route de Vevey", "Forel (Lavaux)", adresses.get(0));
		assertAdresse(TypeAdresseCivil.CASE_POSTALE, RegDate.get(2017, 8, 23), null, null, "Savigny", adresses.get(1));
		assertEquals("Case Postale 38", adresses.get(1).getCasePostale().toString());
	}

	private RcEntClient createRCEntClient(boolean validating) throws Exception {
		WebClientPool wcPool = new WebClientPool();
		wcPool.setUsername(username);
		wcPool.setPassword(password);
		wcPool.setBaseUrl(baseUrl);
		RcEntClientImpl client = new RcEntClientImpl();
		client.setWcPool(wcPool);
		client.setOrganisationPath(BASE_PATH_ORGANISATION);
		client.setOrganisationsOfNoticePath(BASE_PATH_ORGANISATIONS_OF_NOTICE);
		client.setNoticeRequestValidatePath(BASE_PATH_VALIDER_ANNONCE_IDE);
		client.setNoticeRequestListPath(BASE_PATH_ANNONCE_IDE);
		if (validating) {
			client.setSchemasLocations(Arrays.asList(RCEntSchemaHelper.RCENT_SCHEMA));
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
			final Source[] source = RCEntSchemaHelper.getRCEntClasspathSources();
			Schema schema = sf.newSchema(source);
			unmarshaller.setSchema(schema);
		}
		return unmarshaller;
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
