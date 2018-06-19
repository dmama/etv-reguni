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
import java.util.List;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.springframework.util.ResourceUtils;
import org.xml.sax.SAXException;

import ch.vd.evd0022.v3.OrganisationData;
import ch.vd.evd0022.v3.OrganisationLocation;
import ch.vd.evd0022.v3.TypeOfLocation;
import ch.vd.evd0023.v3.ListOfNoticeRequest;
import ch.vd.evd0023.v3.ObjectFactory;
import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.BusinessItTest;
import ch.vd.unireg.interfaces.common.Adresse;
import ch.vd.unireg.interfaces.entreprise.ServiceEntrepriseRaw;
import ch.vd.unireg.interfaces.entreprise.data.AdresseAnnonceIDERCEnt;
import ch.vd.unireg.interfaces.entreprise.data.AnnonceIDEData;
import ch.vd.unireg.interfaces.entreprise.data.AnnonceIDEEnvoyee;
import ch.vd.unireg.interfaces.entreprise.data.BaseAnnonceIDE;
import ch.vd.unireg.interfaces.entreprise.data.EntrepriseCivile;
import ch.vd.unireg.interfaces.entreprise.data.FormeLegale;
import ch.vd.unireg.interfaces.entreprise.data.NumeroIDE;
import ch.vd.unireg.interfaces.entreprise.data.ProtoAnnonceIDE;
import ch.vd.unireg.interfaces.entreprise.data.StatutAnnonce;
import ch.vd.unireg.interfaces.entreprise.data.TypeAnnonce;
import ch.vd.unireg.interfaces.entreprise.data.TypeEtablissementCivil;
import ch.vd.unireg.interfaces.entreprise.rcent.RCEntAnnonceIDEHelper;
import ch.vd.unireg.interfaces.entreprise.rcent.RCEntSchemaHelper;
import ch.vd.unireg.interfaces.infra.mock.MockLocalite;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.interfaces.model.AdressesCivilesHisto;
import ch.vd.unireg.interfaces.service.ServiceEntreprise;
import ch.vd.unireg.type.TypeAdresseCivil;
import ch.vd.unireg.xml.tools.ClasspathCatalogResolver;

import static ch.vd.unireg.interfaces.civil.data.IndividuRCPersTest.assertAdresse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@SuppressWarnings({"JavaDoc"})
public class ServiceEntrepriseRCEntItTest extends BusinessItTest {

	private static final String BASE_PATH_ORGANISATION = "/organisation/CT.VD.PARTY";
	private static final String BASE_PATH_ANNONCE_IDE = "/noticeRequestList";

	// Entreprise civile cible sur RCEnt
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

	// Entreprise civile de l'échantillon fichier
	private static final long ID_BOMACO = 101636326L;
	private static final String BOMACO_SARL_EN_LIQUIDATION = "Bomaco Sàrl en liquidation";
	private static final String FILE_SAMPLE_ORGANISATION_100983251_HISTORY = "classpath:ch/vd/unireg/interfaces/entreprise-bomaco-history.xml";

	// annonce à l'IDE sur RCEnt
	private static final String USER_ID = "unireg";
	private static final long ID_ANNONCE = 180007882L;

	private String baseUrl;

	private String username = "";
	private String password = "";

	private ServiceEntreprise service;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		username = uniregProperties.getProperty("testprop.webservice.rcent.username");
		password = uniregProperties.getProperty("testprop.webservice.rcent.password");
		baseUrl = uniregProperties.getProperty("testprop.webservice.rcent.url");

		service = getBean(ServiceEntreprise.class, "serviceEntreprise");
	}

	@Test(timeout = 30000)
	public void getEntrepriseHistory() {
		EntrepriseCivile ent = service.getEntrepriseHistory(ID_BCV);
		assertNotNull(ent);
		assertContains(NOM_BCV, ent.getNom().get(0).getPayload());
	}

//	@Ignore
	@Test(timeout = 30000)
	public void testGetEntrepriseEvent() {
		EntrepriseCivile ent = service.getEntrepriseEvent(ID_EVT).get(ID_ORGANISATION_EVT).getPseudoHistory();
		assertNotNull(ent);
		assertContains(ID_NOM_EVT, ent.getNom().get(0).getPayload());
	}

	@Test(timeout = 30000)
	public void testGetEntrepriseIdsByNoIde() {
		final ServiceEntrepriseRaw.Identifiers ids = service.getEntrepriseIdsByNoIde(IDE_SUCC);
		assertNotNull(ids);
		assertEquals(ID_ORGANISATION_SUCC, ids.idCantonalEntreprise);
		assertEquals(ID_SUCC, ids.idCantonalEtablissement);
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
		assertEquals(BOMACO_SARL_EN_LIQUIDATION, data.getOrganisationSnapshot().get(0).getOrganisation().getOrganisationLocation().get(0).getName());
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
	public void testGetAnnonceIDE() {
		final AnnonceIDEEnvoyee annonceIDE = service.getAnnonceIDE(ID_ANNONCE, RCEntAnnonceIDEHelper.UNIREG_USER);

		assertNotNull(annonceIDE);
		assertEquals(ID_ANNONCE, annonceIDE.getNumero().longValue());
		assertEquals(StatutAnnonce.ACCEPTE_IDE, annonceIDE.getStatut().getStatut());
	}

	@Test(timeout = 30000)
	public void testValidateProtoAnnonceIDE() {

		final AdresseAnnonceIDERCEnt adresse = RCEntAnnonceIDEHelper
				.createAdresseAnnonceIDERCEnt("Rue du Marais", "1", null, MockLocalite.Geneve.getNPA(), null, MockLocalite.Geneve.getNoOrdre(), "Genève", MockPays.Suisse.getNoOfsEtatSouverain(), MockPays.Suisse.getCodeIso2(), MockPays.Suisse.getNomCourt(), null,
				                              null, null);
		ProtoAnnonceIDE proto = RCEntAnnonceIDEHelper.createProtoAnnonceIDE(TypeAnnonce.CREATION, DateHelper.getCurrentDate(), RCEntAnnonceIDEHelper.UNIREG_USER, null, TypeEtablissementCivil.ETABLISSEMENT_PRINCIPAL, null, null,
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
		                                                  TypeEtablissementCivil.ETABLISSEMENT_PRINCIPAL,
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
		                                                                          TypeEtablissementCivil.ETABLISSEMENT_PRINCIPAL,
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
	 * [SIFISC-24996] Ce test vérifie que les adresses <i>case postale</i> d'une entreprises sont bien retournées par la méthode <i>getAdressesEntrepriseHisto</i>.
	 */
	@Test(timeout = 30000)
	public void testGetAdressesEntrepriseHistoAvecBoitePostale() {

		// tiers 34301, numéro cantonal = 101830038
		final AdressesCivilesHisto adresses = service.getAdressesEntrepriseHisto(101830038);
		assertNotNull(adresses);
		assertEquals(0, adresses.principales.size());
		assertEquals(0, adresses.secondaires.size());
		assertEquals(0, adresses.tutelles.size());

		assertEquals(2, adresses.courriers.size());
		assertAdresse(TypeAdresseCivil.COURRIER, RegDate.get(2016, 9, 21), RegDate.get(2017, 8, 22), null, "Savigny", adresses.courriers.get(0));
		assertAdresse(TypeAdresseCivil.COURRIER, RegDate.get(2017, 8, 23), null, "Route de Vevey", "Forel (Lavaux)", adresses.courriers.get(1));

		assertEquals(1, adresses.casesPostales.size());
		assertAdresse(TypeAdresseCivil.CASE_POSTALE, RegDate.get(2016, 12, 15), null, null, "Savigny", adresses.casesPostales.get(0));
		assertEquals("Case Postale 38", adresses.casesPostales.get(0).getCasePostale().toString());
	}

	/**
	 * [SIFISC-24996] Ce test vérifie que les adresses <i>case postale</i> d'une entreprises sont bien exposées sur l'entreprises retournée par la méthode <i>getEntrepriseHistory</i>.
	 */
	@Test(timeout = 30000)
	public void testGetEntrepriseHistoryAvecBoitePostale() {

		// tiers 34301, numéro cantonal = 101830038
		final EntrepriseCivile ent = service.getEntrepriseHistory(101830038);
		assertNotNull(ent);

		final List<Adresse> adresses = ent.getAdresses();
		assertEquals(3, adresses.size());
		assertAdresse(TypeAdresseCivil.COURRIER, RegDate.get(2016, 9, 21), RegDate.get(2017, 8, 22), null, "Savigny", adresses.get(0));
		assertAdresse(TypeAdresseCivil.CASE_POSTALE, RegDate.get(2016, 12, 15), null, null, "Savigny", adresses.get(1));
		assertAdresse(TypeAdresseCivil.COURRIER, RegDate.get(2017, 8, 23), null, "Route de Vevey", "Forel (Lavaux)", adresses.get(2));
		assertEquals("Case Postale 38", adresses.get(1).getCasePostale().toString());
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
