package ch.vd.uniregctb.adapter.rcent.service;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.cxf.jaxrs.client.ServerWebApplicationException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.io.ClassPathResource;
import org.xml.sax.SAXException;

import ch.vd.evd0004.v3.Error;
import ch.vd.evd0004.v3.Errors;
import ch.vd.evd0022.v3.Address;
import ch.vd.evd0022.v3.Capital;
import ch.vd.evd0022.v3.CommercialRegisterDiaryEntry;
import ch.vd.evd0022.v3.CommercialRegisterStatus;
import ch.vd.evd0022.v3.LegalForm;
import ch.vd.evd0022.v3.OrganisationData;
import ch.vd.evd0022.v3.OrganisationsOfNotice;
import ch.vd.evd0022.v3.TypeOfCapital;
import ch.vd.evd0022.v3.TypeOfLocation;
import ch.vd.evd0022.v3.UidRegisterStatus;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.wsclient.rcent.RcEntClient;
import ch.vd.unireg.wsclient.rcent.RcEntClientErrorMessage;
import ch.vd.unireg.wsclient.rcent.RcEntClientException;
import ch.vd.unireg.xml.tools.ClasspathCatalogResolver;
import ch.vd.uniregctb.adapter.rcent.historizer.OrganisationHistorizer;
import ch.vd.uniregctb.adapter.rcent.model.Organisation;
import ch.vd.uniregctb.adapter.rcent.model.OrganisationEvent;
import ch.vd.uniregctb.adapter.rcent.model.OrganisationFunction;
import ch.vd.uniregctb.adapter.rcent.model.OrganisationLocation;
import ch.vd.uniregctb.adapter.rcent.model.RCRegistrationData;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

public class RCEntAdapterTest {

	/*
		Configuration des schémas applicables pour le décodage des annonces RCEnt
    */
	public static final String[] RCENT_SCHEMA = new String[]{
			"eVD-0004-3-0.xsd",
			"eVD-0022-3-3.xsd",
			"eVD-0023-3-3.xsd",
			"eVD-0024-3-4.xsd"
	};

	@Mock
	private RcEntClient client;

	private OrganisationHistorizer historizer = new OrganisationHistorizer();

	private Unmarshaller unmarshaller;

	private Unmarshaller errorunmarshaller;

	private RCEntAdapter service;

	@Before
	public void setUp() throws Exception {
		JAXBContext jc = JAXBContext.newInstance("ch.vd.evd0023.v3");
		unmarshaller = jc.createUnmarshaller();
		unmarshaller.setSchema(buildRequestSchema());

		JAXBContext jcerror = JAXBContext.newInstance("ch.vd.evd0004.v3");
		errorunmarshaller = jcerror.createUnmarshaller();
		errorunmarshaller.setSchema(buildRequestSchema());

		MockitoAnnotations.initMocks(this);

		service = new RCEntAdapter(client, historizer);
	}

	private synchronized Schema buildRequestSchema() throws SAXException, IOException {
		final SchemaFactory sf = SchemaFactory.newInstance(javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI);
		sf.setResourceResolver(new ClasspathCatalogResolver());
		final Source[] source = getClasspathSources(RCENT_SCHEMA);
		return sf.newSchema(source);
	}

	private static Source[] getClasspathSources(String... pathes) throws IOException {
		final Source[] sources = new Source[pathes.length];
		for (int i = 0, pathLength = pathes.length; i < pathLength; i++) {
			final String path = pathes[i];
			sources[i] = new StreamSource(new ClassPathResource(path).getURL().toExternalForm());
		}
		return sources;
	}

	@Test
	public void testGetOrganisation() throws Exception {
		final File xml = new File("src/test/resources/samples/organisationData/CHE101992624-no-history.xml");
		final JAXBElement<OrganisationData> data = (JAXBElement<OrganisationData>) unmarshaller.unmarshal(xml);

		when(client.getOrganisation(101202262, null, false)).thenReturn(data.getValue());

		final Organisation organisation = service.getOrganisation(101202262, null);
		assertThat(organisation.getCantonalId(), equalTo(101202262L));

		// Check basic organisation data
		{
			assertThat(organisation.getLocationData().get(0).getLegalForm().get(0).getDateDebut(), equalTo(RegDate.get(2015, 5, 1)));
			assertThat(organisation.getLocationData().get(0).getLegalForm().get(0).getDateFin(), nullValue());
			assertThat(organisation.getLocationData().get(0).getLegalForm().get(0).getPayload(), equalTo(LegalForm.N_0101_ENTREPRISE_INDIVIDUELLE));
		}

		assertThat(organisation.getLocationData().get(0).getName().get(0).getPayload(), equalTo("Jean-François Niklaus"));

		// Locations
		assertThat(organisation.getLocations().size(), equalTo(1));
		assertThat(organisation.getLocations().values().iterator().next().get(0).getPayload(), equalTo(101072745L));

		assertThat(organisation.getLocationData().size(), equalTo(1));
		assertThat(organisation.getLocationData().get(0).getCantonalId(), equalTo(101072745L));

		final Map<String, List<DateRangeHelper.Ranged<String>>> identifierMap = organisation.getLocationData().get(0).getIdentifiers();
		assertThat(identifierMap.size(), equalTo(5));

		assertThat(identifierMap.get("CH.HR").size(), equalTo(1));
		assertThat(identifierMap.get("CH.HR").get(0).getPayload(), equalTo("CH55000431371"));
		assertThat(identifierMap.get("CH.IDE").size(), equalTo(1));
		assertThat(identifierMap.get("CH.IDE").get(0).getPayload(), equalTo("CHE101992624"));
		assertThat(identifierMap.get("CH.RC").size(), equalTo(1));
		assertThat(identifierMap.get("CH.RC").get(0).getPayload(), equalTo("CH55000431371"));
		assertThat(identifierMap.get("CHE").size(), equalTo(1));
		assertThat(identifierMap.get("CHE").get(0).getPayload(), equalTo("101992624"));
		assertThat(identifierMap.get("CT.VD.PARTY").size(), equalTo(1));
		assertThat(identifierMap.get("CT.VD.PARTY").get(0).getPayload(), equalTo("101072745"));

		final TypeOfLocation kindOfLocation = organisation.getLocationData().get(0).getTypeOfLocation().get(0).getPayload();
		assertThat(kindOfLocation, equalTo(TypeOfLocation.ETABLISSEMENT_PRINCIPAL));

		assertThat(organisation.getLocationData().get(0).getSeat().get(0).getPayload(), equalTo(5561));

		// Adresse
		Address effectiveAddress = organisation.getLocationData().get(0).getUid().getEffectiveAddress().get(0).getPayload();
		assertThat(effectiveAddress.getAddressInformation().getAddressLine1(), equalTo("Les Tuileries"));
		assertThat(effectiveAddress.getAddressInformation().getAddressLine2(), nullValue());
		assertThat(effectiveAddress.getAddressInformation().getStreet(), equalTo("Chemin du Mont"));
		assertThat(effectiveAddress.getAddressInformation().getHouseNumber(), equalTo("15"));
		assertThat(effectiveAddress.getAddressInformation().getSwissZipCode(), equalTo(1422L));
		assertThat(effectiveAddress.getAddressInformation().getCountry().getCountryNameShort(), equalTo("Suisse"));
		assertThat(effectiveAddress.getAddressInformation().getTown(), equalTo("Grandson"));
		assertThat(effectiveAddress.getFederalBuildingId(), equalTo(872180L));

	}

	@Test
	public void testGetOrganisationHistory() throws Exception {
		final File xml = new File("src/test/resources/samples/organisationData/CHE101992624.xml");
		final JAXBElement<OrganisationData> data = (JAXBElement<OrganisationData>) unmarshaller.unmarshal(xml);

		when(client.getOrganisation(101202262, null, true)).thenReturn(data.getValue());

		final Organisation organisation = service.getOrganisationHistory(101202262);
		assertThat(organisation.getCantonalId(), equalTo(101202262L));

		// Adresse
		final DateRangeHelper.Ranged<Address> postOfficeBoxAddressRange1 = organisation.getLocationData().get(0).getUid().getPostOfficeBoxAddress().get(0);
		final Address postalAddress_period1 = postOfficeBoxAddressRange1.getPayload();
		assertThat(postOfficeBoxAddressRange1.getDateDebut(), equalTo(RegDate.get(2015, 4, 29)));
		assertThat(postOfficeBoxAddressRange1.getDateFin(), equalTo(RegDate.get(2015, 4, 30)));
		assertThat(postalAddress_period1.getAddressInformation().getAddressLine1(), nullValue());
		assertThat(postalAddress_period1.getAddressInformation().getAddressLine2(), nullValue());
		assertThat(postalAddress_period1.getAddressInformation().getStreet(), equalTo("Chemin du Mont"));
		assertThat(postalAddress_period1.getAddressInformation().getHouseNumber(), equalTo("15"));
		assertThat(postalAddress_period1.getAddressInformation().getTown(), equalTo("Triffouilli les oies"));
		assertThat(postalAddress_period1.getAddressInformation().getSwissZipCode(), equalTo(1101L));
		assertThat(postalAddress_period1.getAddressInformation().getCountry().getCountryIdISO2(), equalTo("CH"));

		// Adresse changée
		final DateRangeHelper.Ranged<Address> postOfficeBoxAddressRange2 = organisation.getLocationData().get(0).getUid().getPostOfficeBoxAddress().get(1);
		final Address postalAddress_period2 = postOfficeBoxAddressRange2.getPayload();
		assertThat(postOfficeBoxAddressRange2.getDateDebut(), equalTo(RegDate.get(2015, 5, 1)));
		assertThat(postOfficeBoxAddressRange2.getDateFin(), nullValue());
		assertThat(postalAddress_period2.getAddressInformation().getAddressLine1(), nullValue());
		assertThat(postalAddress_period2.getAddressInformation().getAddressLine2(), nullValue());
		assertThat(postalAddress_period2.getAddressInformation().getStreet(), equalTo("Chemin du Mont"));
		assertThat(postalAddress_period2.getAddressInformation().getHouseNumber(), equalTo("15"));
		assertThat(postalAddress_period2.getAddressInformation().getTown(), equalTo("Grandson"));
		assertThat(postalAddress_period2.getAddressInformation().getSwissZipCode(), equalTo(1422L));
		assertThat(postalAddress_period2.getAddressInformation().getCountry().getCountryIdISO2(), equalTo("CH"));
	}

	@Test
	public void testGetOrganisationHistory4Snapshots() throws Exception {
		final File xml = new File("src/test/resources/samples/organisationData/CHE101992624-4snaps.xml");
		final JAXBElement<OrganisationData> data = (JAXBElement<OrganisationData>) unmarshaller.unmarshal(xml);

		when(client.getOrganisation(101202262, null, true)).thenReturn(data.getValue());

		final Organisation organisation = service.getOrganisationHistory(101202262);
		assertThat(organisation.getCantonalId(), equalTo(101202262L));

		// Adresse postale
		assertThat(organisation.getLocationData().get(0).getUid().getPostOfficeBoxAddress().size(), equalTo(3));

		final DateRangeHelper.Ranged<Address> addressDateRanged1 = organisation.getLocationData().get(0).getUid().getPostOfficeBoxAddress().get(0);
		final Address postalAddress_period1 = addressDateRanged1.getPayload();
		assertThat(addressDateRanged1.getDateDebut(), equalTo(RegDate.get(2015, 4, 29)));
		assertThat(addressDateRanged1.getDateFin(), equalTo(RegDate.get(2015, 4, 30)));
		assertThat(postalAddress_period1.getAddressInformation().getAddressLine1(), nullValue());
		assertThat(postalAddress_period1.getAddressInformation().getAddressLine2(), nullValue());
		assertThat(postalAddress_period1.getAddressInformation().getStreet(), equalTo("Chemin du Mont"));
		assertThat(postalAddress_period1.getAddressInformation().getHouseNumber(), equalTo("15"));
		assertThat(postalAddress_period1.getAddressInformation().getTown(), equalTo("Triffouilli les oies"));
		assertThat(postalAddress_period1.getAddressInformation().getSwissZipCode(), equalTo(1101L));
		assertThat(postalAddress_period1.getAddressInformation().getCountry().getCountryNameShort(), equalTo("Suisse"));

		// Adresse changée
		final DateRangeHelper.Ranged<Address> addressDateRanged2 = organisation.getLocationData().get(0).getUid().getPostOfficeBoxAddress().get(1);
		final Address postalAddress_period2 = addressDateRanged2.getPayload();
		assertThat(addressDateRanged2.getDateDebut(), equalTo(RegDate.get(2015, 5, 1)));
		assertThat(addressDateRanged2.getDateFin(), equalTo(RegDate.get(2015, 5, 23)));
		assertThat(postalAddress_period2.getAddressInformation().getAddressLine1(), nullValue());
		assertThat(postalAddress_period2.getAddressInformation().getAddressLine2(), nullValue());
		assertThat(postalAddress_period2.getAddressInformation().getStreet(), equalTo("Chemin du Mont"));
		assertThat(postalAddress_period2.getAddressInformation().getHouseNumber(), equalTo("15"));
		assertThat(postalAddress_period2.getAddressInformation().getTown(), equalTo("Grandson"));
		assertThat(postalAddress_period2.getAddressInformation().getSwissZipCode(), equalTo(1422L));
		assertThat(postalAddress_period2.getAddressInformation().getCountry().getCountryNameShort(), equalTo("Suisse"));

		// Adresse enlevée puis nouvelle plus tard
		final DateRangeHelper.Ranged<Address> addressDateRanged3 = organisation.getLocationData().get(0).getUid().getPostOfficeBoxAddress().get(2);
		final Address postalAddress_period3 = addressDateRanged3.getPayload();
		assertThat(addressDateRanged3.getDateDebut(), equalTo(RegDate.get(2015, 6, 6)));
		assertThat(addressDateRanged3.getDateFin(), nullValue());
		assertThat(postalAddress_period3.getAddressInformation().getAddressLine1(), nullValue());
		assertThat(postalAddress_period3.getAddressInformation().getAddressLine2(), nullValue());
		assertThat(postalAddress_period3.getAddressInformation().getStreet(), equalTo("Chemin du lac"));
		assertThat(postalAddress_period3.getAddressInformation().getHouseNumber(), equalTo("18"));
		assertThat(postalAddress_period3.getAddressInformation().getTown(), equalTo("Hiverdon"));
		assertThat(postalAddress_period3.getAddressInformation().getSwissZipCode(), equalTo(1919L));
		assertThat(postalAddress_period3.getAddressInformation().getCountry().getCountryNameShort(), equalTo("Suisse"));
	}

	@Test
	public void testGetOrganisationHistorySocieteAnonyme() throws Exception {
		final File xml = new File("src/test/resources/samples/organisationData/CHE105879116.xml");
		final JAXBElement<OrganisationData> data = (JAXBElement<OrganisationData>) unmarshaller.unmarshal(xml);

		when(client.getOrganisation(101202213, null, false)).thenReturn(data.getValue());

		final Organisation organisation = service.getOrganisation(101202213, null);
		assertThat(organisation.getCantonalId(), equalTo(101202213L));

		// Check basic organisation data
		assertThat(organisation.getLocationData().get(0).getLegalForm().get(0).getDateDebut(), equalTo(RegDate.get(2015, 4, 29)));
		assertThat(organisation.getLocationData().get(0).getLegalForm().get(0).getDateFin(), nullValue());
		assertThat(organisation.getLocationData().get(0).getLegalForm().get(0).getPayload(), equalTo(LegalForm.N_0106_SOCIETE_ANONYME));

		{ // Nom
			assertThat(organisation.getLocationData().get(0).getName().get(0).getDateDebut(), equalTo(RegDate.get(2015, 4, 29)));
			assertThat(organisation.getLocationData().get(0).getName().get(0).getDateFin(), equalTo(RegDate.get(2015, 5, 24)));
			assertThat(organisation.getLocationData().get(0).getName().get(0).getPayload(), equalTo("Seroc S.A."));

			assertThat(organisation.getLocationData().get(0).getName().get(1).getDateDebut(), equalTo(RegDate.get(2015, 5, 25)));
			assertThat(organisation.getLocationData().get(0).getName().get(1).getDateFin(), equalTo(RegDate.get(2015, 6, 5)));
			assertThat(organisation.getLocationData().get(0).getName().get(1).getPayload(), equalTo("SeroKK S.A."));

			assertThat(organisation.getLocationData().get(0).getName().get(2).getDateDebut(), equalTo(RegDate.get(2015, 6, 6)));
			assertThat(organisation.getLocationData().get(0).getName().get(2).getDateFin(), nullValue());
			assertThat(organisation.getLocationData().get(0).getName().get(2).getPayload(), equalTo("ZeroXX S.A."));
		}

		// Locations
		assertThat(organisation.getLocations().size(), equalTo(1));
		assertThat(organisation.getLocations().values().iterator().next().get(0).getPayload(), equalTo(101072728L));

		assertThat(organisation.getLocationData().size(), equalTo(1));
		assertThat(organisation.getLocationData().get(0).getCantonalId(), equalTo(101072728L));

		{ // Nom
			assertThat(organisation.getLocationData().get(0).getName().get(0).getDateDebut(), equalTo(RegDate.get(2015, 4, 29)));
			assertThat(organisation.getLocationData().get(0).getName().get(0).getDateFin(), equalTo(RegDate.get(2015, 5, 24)));
			assertThat(organisation.getLocationData().get(0).getName().get(0).getPayload(), equalTo("Seroc S.A."));

			assertThat(organisation.getLocationData().get(0).getName().get(1).getDateDebut(), equalTo(RegDate.get(2015, 5, 25)));
			assertThat(organisation.getLocationData().get(0).getName().get(1).getDateFin(), equalTo(RegDate.get(2015, 6, 5)));
			assertThat(organisation.getLocationData().get(0).getName().get(1).getPayload(), equalTo("SeroKK S.A."));

			assertThat(organisation.getLocationData().get(0).getName().get(2).getDateDebut(), equalTo(RegDate.get(2015, 6, 6)));
			assertThat(organisation.getLocationData().get(0).getName().get(2).getDateFin(), nullValue());
			assertThat(organisation.getLocationData().get(0).getName().get(2).getPayload(), equalTo("ZeroXX S.A."));
		}
		{
			assertThat(organisation.getLocationData().get(0).getRc().getPurpose().get(0).getDateDebut(), equalTo(RegDate.get(2015, 4, 29)));
			assertThat(organisation.getLocationData().get(0).getRc().getPurpose().get(0).getDateFin(), equalTo(RegDate.get(2015, 4, 30)));
			assertThat(organisation.getLocationData().get(0).getRc().getPurpose().get(0).getPayload(), equalTo("Vendre des copieurs."));

			assertThat(organisation.getLocationData().get(0).getRc().getPurpose().get(1).getDateDebut(), equalTo(RegDate.get(2015, 5, 1)));
			assertThat(organisation.getLocationData().get(0).getRc().getPurpose().get(1).getDateFin(), nullValue());
			assertThat(organisation.getLocationData().get(0).getRc().getPurpose().get(1).getPayload(), equalTo("Vendre des copieurs super rapides."));
		}
		{
			assertThat(organisation.getLocationData().get(0).getRc().getByLawsDate().get(0).getDateDebut(), equalTo(RegDate.get(2015, 4, 29)));
			assertThat(organisation.getLocationData().get(0).getRc().getByLawsDate().get(0).getDateFin(), nullValue());
			assertThat(organisation.getLocationData().get(0).getRc().getByLawsDate().get(0).getPayload(), equalTo(RegDate.get(2015, 4, 29)));
		}


		final Map<String, List<DateRangeHelper.Ranged<String>>> identifierMap = organisation.getLocationData().get(0).getIdentifiers();
		assertThat(identifierMap.size(), equalTo(8));

		assertThat(identifierMap.get("CH.HR").size(), equalTo(1));
		assertThat(identifierMap.get("CH.HR").get(0).getPayload(), equalTo("CH55001688715"));
		assertThat(identifierMap.get("CH.IDE").size(), equalTo(1));
		assertThat(identifierMap.get("CH.IDE").get(0).getPayload(), equalTo("CHE105879116"));
		assertThat(identifierMap.get("CH.IDE.TVA").size(), equalTo(1));
		assertThat(identifierMap.get("CH.IDE.TVA").get(0).getPayload(), equalTo("CHE105879116"));
		assertThat(identifierMap.get("CH.MWST").size(), equalTo(1));
		assertThat(identifierMap.get("CH.MWST").get(0).getPayload(), equalTo("196378"));
		assertThat(identifierMap.get("CH.RC").size(), equalTo(1));
		assertThat(identifierMap.get("CH.RC").get(0).getPayload(), equalTo("CH55001688715"));
		assertThat(identifierMap.get("CH.TVA").size(), equalTo(1));
		assertThat(identifierMap.get("CH.TVA").get(0).getPayload(), equalTo("196378"));
		assertThat(identifierMap.get("CHE").size(), equalTo(1));
		assertThat(identifierMap.get("CHE").get(0).getPayload(), equalTo("105879116"));
		assertThat(identifierMap.get("CT.VD.PARTY").size(), equalTo(1));
		assertThat(identifierMap.get("CT.VD.PARTY").get(0).getPayload(), equalTo("101072728"));

		final TypeOfLocation kindOfLocation = organisation.getLocationData().get(0).getTypeOfLocation().get(0).getPayload();
		assertThat(kindOfLocation, equalTo(TypeOfLocation.ETABLISSEMENT_PRINCIPAL));

		assertThat(organisation.getLocationData().get(0).getSeat().get(0).getPayload(), equalTo(5413));


		// Adresse postale
		assertThat(organisation.getLocationData().get(0).getUid().getPostOfficeBoxAddress().size(), equalTo(2));

		final DateRangeHelper.Ranged<Address> addressDateRanged1 = organisation.getLocationData().get(0).getUid().getPostOfficeBoxAddress().get(0);
		final Address postalAddress_period1 = addressDateRanged1.getPayload();
		assertThat(addressDateRanged1.getDateDebut(), equalTo(RegDate.get(2015, 4, 29)));
		assertThat(addressDateRanged1.getDateFin(), equalTo(RegDate.get(2015, 4, 30)));
		assertThat(postalAddress_period1.getAddressInformation().getAddressLine1(), nullValue());
		assertThat(postalAddress_period1.getAddressInformation().getAddressLine2(), nullValue());
		assertThat(postalAddress_period1.getAddressInformation().getStreet(), equalTo("Zone Industrielle La Coche"));
		assertThat(postalAddress_period1.getAddressInformation().getHouseNumber(), equalTo("11"));
		assertThat(postalAddress_period1.getAddressInformation().getTown(), equalTo("Roche VD"));
		assertThat(postalAddress_period1.getAddressInformation().getSwissZipCode(), equalTo(1852L));
		assertThat(postalAddress_period1.getAddressInformation().getCountry().getCountryNameShort(), equalTo("Suisse"));

		// Adresse enlevée puis nouvelle plus tard, à l'identique
		final DateRangeHelper.Ranged<Address> addressDateRanged2 = organisation.getLocationData().get(0).getUid().getPostOfficeBoxAddress().get(1);
		final Address postalAddress_period2 = addressDateRanged2.getPayload();
		assertThat(addressDateRanged2.getDateDebut(), equalTo(RegDate.get(2015, 5, 15)));
		assertThat(addressDateRanged2.getDateFin(), nullValue());
		assertThat(postalAddress_period2.getAddressInformation().getAddressLine1(), nullValue());
		assertThat(postalAddress_period2.getAddressInformation().getAddressLine2(), nullValue());
		assertThat(postalAddress_period2.getAddressInformation().getStreet(), equalTo("Zone Industrielle La Coche"));
		assertThat(postalAddress_period2.getAddressInformation().getHouseNumber(), equalTo("11"));
		assertThat(postalAddress_period2.getAddressInformation().getTown(), equalTo("Roche VD"));
		assertThat(postalAddress_period2.getAddressInformation().getSwissZipCode(), equalTo(1852L));
		assertThat(postalAddress_period2.getAddressInformation().getCountry().getCountryNameShort(), equalTo("Suisse"));
	}

	@Test
	public void testGetOrganisationHistorySample100983251() throws JAXBException {
		final File xml = new File("src/test/resources/samples/organisationData/organisation-100983251-history.xml");
		final JAXBElement<OrganisationData> data = (JAXBElement<OrganisationData>) unmarshaller.unmarshal(xml);
		when(client.getOrganisation(100983251L, null, true)).thenReturn(data.getValue());

		final Organisation organisation = service.getOrganisationHistory(100983251L);
		assertThat(organisation.getCantonalId(), equalTo(100983251L));

		final List<DateRangeHelper.Ranged<UidRegisterStatus>> ideStatus = organisation.getLocationData().get(0).getUid().getStatus();
		assertNotNull(ideStatus);
		assertEquals(UidRegisterStatus.DEFINITIF, ideStatus.get(0).getPayload());

		final List<DateRangeHelper.Ranged<Capital>> capitalRanges = organisation.getLocationData().get(0).getRc().getCapital();
		assertNotNull(capitalRanges);
		final Capital capital = capitalRanges.get(0).getPayload();
		assertEquals("CHF", capital.getCurrency());
		assertEquals(new BigDecimal(25000), capital.getCapitalAmount());
		assertEquals(new BigDecimal(23000), capital.getCashedInAmount());
		assertEquals(TypeOfCapital.CAPITAL_SOCIAL, capital.getTypeOfCapital());

		final List<DateRangeHelper.Ranged<String>> locationName = organisation.getLocationData().get(0).getName();
		assertEquals("Bomaco Sàrl en liquidation", locationName.get(0).getPayload());

		final List<DateRangeHelper.Ranged<RCRegistrationData>> locationRcRegistrationData = organisation.getLocationData().get(0).getRc().getRegistrationData();
		assertEquals(1, locationRcRegistrationData.size());
		final RCRegistrationData rcRegistration = locationRcRegistrationData.get(0).getPayload();
		assertNotNull(rcRegistration);
		assertEquals(CommercialRegisterStatus.ACTIF, rcRegistration.getRegistrationStatus());
		assertEquals(RegDate.get(2007, 4, 16), rcRegistration.getChRegistrationDate());

		// JDE 27.01.2016 : déconnecté l'interprétation des fonctions qui pêtait dès qu'une même personne avait plusieurs fonctions à un moment donné
		final Map<String, List<DateRangeHelper.Ranged<OrganisationFunction>>> locationFunctions = organisation.getLocationData().get(0).getFunction();
		assertNull(locationFunctions); // S'il y en a plus, c'est que l'Historizer ne sait pas identifier proprement les fonctions qu'on doit considérer identiques.
	}

	@Test
	public void testGetOrgaOfNoticeNouvelleOrganisation() throws JAXBException {
		final long noticeId = 383321L;
		final File xmlBefore = new File("src/test/resources/samples/organisationsOfNotice/evt-383321-before.xml");
		final File xmlAfter = new File("src/test/resources/samples/organisationsOfNotice/evt-383321-after.xml");
		final JAXBElement<OrganisationsOfNotice> orgOfNoticeAfter = (JAXBElement<OrganisationsOfNotice>) unmarshaller.unmarshal(xmlAfter);
		when(client.getOrganisationsOfNotice(noticeId, RcEntClient.OrganisationState.AFTER)).thenReturn(orgOfNoticeAfter.getValue());

		final Errors orgOfNoticeBeforeErrors = (Errors) errorunmarshaller.unmarshal(xmlBefore);
		final Error error = orgOfNoticeBeforeErrors.getError().get(0);
		final RcEntClientException rcEntClientException = new RcEntClientException(new ServerWebApplicationException(), Collections.singletonList(new RcEntClientErrorMessage(error)));
		when(client.getOrganisationsOfNotice(noticeId, RcEntClient.OrganisationState.BEFORE)).thenThrow(rcEntClientException);

		final Long noOrganisation = 101704297L;

		final Map<Long, OrganisationEvent> historyMap = service.getOrganisationEvent(noticeId);

		final Organisation organisation = historyMap.get(noOrganisation).getPseudoHistory();
		assertThat(organisation.getCantonalId(), equalTo(noOrganisation));

		final OrganisationLocation organisationLocation = organisation.getLocationData().get(0);

		final List<DateRangeHelper.Ranged<String>> locationName = organisationLocation.getName();
		assertEquals(RegDate.get(2008, 9, 4), locationName.get(0).getDateDebut());
		assertNull(locationName.get(0).getDateFin());
		assertEquals("Agades AG", locationName.get(0).getPayload());

		final List<DateRangeHelper.Ranged<RCRegistrationData>> locationRcRegistrationData = organisation.getLocationData().get(0).getRc().getRegistrationData();
		assertEquals(1, locationRcRegistrationData.size());
		final RCRegistrationData rcRegistration = locationRcRegistrationData.get(0).getPayload();
		assertNotNull(rcRegistration);
		assertEquals(CommercialRegisterStatus.RADIE, rcRegistration.getRegistrationStatus());
	}

	@Test
	public void testGetOrgaOfNoticeNouvelleOrganisationNonRC() throws JAXBException {
		final long noticeId = 383321L;
		final File xmlBefore = new File("src/test/resources/samples/organisationsOfNotice/evt-383321-before.xml");
		final File xmlAfter = new File("src/test/resources/samples/organisationsOfNotice/evt-383321-after-no-rc.xml");
		final JAXBElement<OrganisationsOfNotice> orgOfNoticeAfter = (JAXBElement<OrganisationsOfNotice>) unmarshaller.unmarshal(xmlAfter);
		when(client.getOrganisationsOfNotice(noticeId, RcEntClient.OrganisationState.AFTER)).thenReturn(orgOfNoticeAfter.getValue());

		final Errors orgOfNoticeBeforeErrors = (Errors) errorunmarshaller.unmarshal(xmlBefore);
		final Error error = orgOfNoticeBeforeErrors.getError().get(0);
		final RcEntClientException rcEntClientException = new RcEntClientException(new ServerWebApplicationException(), Collections.singletonList(new RcEntClientErrorMessage(error)));
		when(client.getOrganisationsOfNotice(noticeId, RcEntClient.OrganisationState.BEFORE)).thenThrow(rcEntClientException);

		final Long noOrganisation = 101704297L;

		final Map<Long, OrganisationEvent> historyMap = service.getOrganisationEvent(noticeId);

		final Organisation organisation = historyMap.get(noOrganisation).getPseudoHistory();
		assertThat(organisation.getCantonalId(), equalTo(noOrganisation));

		final OrganisationLocation organisationLocation = organisation.getLocationData().get(0);

		final List<DateRangeHelper.Ranged<String>> locationName = organisationLocation.getName();
		assertEquals(RegDate.get(2008, 9, 4), locationName.get(0).getDateDebut());
		assertNull(locationName.get(0).getDateFin());
		assertEquals("Agades perso", locationName.get(0).getPayload());
	}

	@Test
	public void testGetOrgaOfNoticeOrganisationExistante() throws JAXBException {
		final long eventId = 383322L;
		final File xmlBefore = new File("src/test/resources/samples/organisationsOfNotice/evt-383322-before.xml");
		final File xmlAfter = new File("src/test/resources/samples/organisationsOfNotice/evt-383322-after.xml");
		final JAXBElement<OrganisationsOfNotice> orgOfNoticeBefore = (JAXBElement<OrganisationsOfNotice>) unmarshaller.unmarshal(xmlBefore);
		final JAXBElement<OrganisationsOfNotice> orgOfNoticeAfter = (JAXBElement<OrganisationsOfNotice>) unmarshaller.unmarshal(xmlAfter);

		when(client.getOrganisationsOfNotice(eventId, RcEntClient.OrganisationState.AFTER)).thenReturn(orgOfNoticeAfter.getValue());
		when(client.getOrganisationsOfNotice(eventId, RcEntClient.OrganisationState.BEFORE)).thenReturn(orgOfNoticeBefore.getValue());

		final Long noOrganisation = 101704297L;

		final Map<Long, OrganisationEvent> historyMap = service.getOrganisationEvent(eventId);

		final Organisation organisation = historyMap.get(noOrganisation).getPseudoHistory();
		assertThat(organisation.getCantonalId(), equalTo(noOrganisation));

		final OrganisationLocation organisationLocation = organisation.getLocationData().get(0);

		final List<DateRangeHelper.Ranged<String>> locationName = organisationLocation.getName();
		assertEquals(RegDate.get(2008, 9, 3), locationName.get(0).getDateDebut());
		assertNull(locationName.get(0).getDateFin());
		assertEquals("Agades AG", locationName.get(0).getPayload());

		final List<DateRangeHelper.Ranged<LegalForm>> legalForm = organisationLocation.getLegalForm();
		{
			final DateRangeHelper.Ranged<LegalForm> legalFormRanged = legalForm.get(0);
			assertEquals(RegDate.get(2008, 9, 3), legalFormRanged.getDateDebut());
			assertEquals(RegDate.get(2008, 9, 3), legalFormRanged.getDateFin());
			assertEquals(LegalForm.N_0107_SOCIETE_A_RESPONSABILITE_LIMITE, legalFormRanged.getPayload());
		}
		{
			final DateRangeHelper.Ranged<LegalForm> legalFormRanged = legalForm.get(1);
			assertEquals(RegDate.get(2008, 9, 4), legalFormRanged.getDateDebut());
			assertNull(legalFormRanged.getDateFin());
			assertEquals(LegalForm.N_0106_SOCIETE_ANONYME, legalFormRanged.getPayload());
		}

		final List<DateRangeHelper.Ranged<RCRegistrationData>> locationRcRegistrationData = organisation.getLocationData().get(0).getRc().getRegistrationData();
		assertEquals(1, locationRcRegistrationData.size());
		final RCRegistrationData rcRegistration = locationRcRegistrationData.get(0).getPayload();
		assertNotNull(rcRegistration);
		assertEquals(CommercialRegisterStatus.ACTIF, rcRegistration.getRegistrationStatus());
	}

	@Test
	public void testGetOrgaOfNoticeMelangePlusieurs() throws JAXBException {
		final long eventId = 383323L;
		final File xmlBefore = new File("src/test/resources/samples/organisationsOfNotice/evt-383323-before.xml");
		final File xmlAfter = new File("src/test/resources/samples/organisationsOfNotice/evt-383323-after.xml");
		final JAXBElement<OrganisationsOfNotice> orgOfNoticeBefore = (JAXBElement<OrganisationsOfNotice>) unmarshaller.unmarshal(xmlBefore);
		final JAXBElement<OrganisationsOfNotice> orgOfNoticeAfter = (JAXBElement<OrganisationsOfNotice>) unmarshaller.unmarshal(xmlAfter);

		when(client.getOrganisationsOfNotice(eventId, RcEntClient.OrganisationState.AFTER)).thenReturn(orgOfNoticeAfter.getValue());
		when(client.getOrganisationsOfNotice(eventId, RcEntClient.OrganisationState.BEFORE)).thenReturn(orgOfNoticeBefore.getValue());

		final Map<Long, OrganisationEvent> historyMap = service.getOrganisationEvent(eventId);

		{
			final Organisation organisation = historyMap.get(201704297L).getPseudoHistory();
			assertThat(organisation.getCantonalId(), equalTo(201704297L));

			final OrganisationLocation organisationLocation = organisation.getLocationData().get(0);

			final List<DateRangeHelper.Ranged<String>> locationName = organisationLocation.getName();
			assertEquals(RegDate.get(2008, 9, 4), locationName.get(0).getDateDebut());
			assertNull(locationName.get(0).getDateFin());
			assertEquals("Sedaga SA", locationName.get(0).getPayload());

			final List<DateRangeHelper.Ranged<RCRegistrationData>> locationRcRegistrationData = organisation.getLocationData().get(0).getRc().getRegistrationData();
			assertEquals(1, locationRcRegistrationData.size());
			final RCRegistrationData rcRegistration = locationRcRegistrationData.get(0).getPayload();
			assertNotNull(rcRegistration);
			assertEquals(CommercialRegisterStatus.RADIE, rcRegistration.getRegistrationStatus());
		}

		{
			final Organisation organisation = historyMap.get(101704297L).getPseudoHistory();
			assertThat(organisation.getCantonalId(), equalTo(101704297L));

			final OrganisationLocation organisationLocation = organisation.getLocationData().get(0);

			final List<DateRangeHelper.Ranged<String>> locationName = organisationLocation.getName();
			assertEquals(RegDate.get(2008, 9, 3), locationName.get(0).getDateDebut());
			assertNull(locationName.get(0).getDateFin());
			assertEquals("Agades AG", locationName.get(0).getPayload());

			final List<DateRangeHelper.Ranged<LegalForm>> legalForm = organisationLocation.getLegalForm();
			{
				final DateRangeHelper.Ranged<LegalForm> legalFormRanged = legalForm.get(0);
				assertEquals(RegDate.get(2008, 9, 3), legalFormRanged.getDateDebut());
				assertEquals(RegDate.get(2008, 9, 3), legalFormRanged.getDateFin());
				assertEquals(LegalForm.N_0107_SOCIETE_A_RESPONSABILITE_LIMITE, legalFormRanged.getPayload());
			}
			{
				final DateRangeHelper.Ranged<LegalForm> legalFormRanged = legalForm.get(1);
				assertEquals(RegDate.get(2008, 9, 4), legalFormRanged.getDateDebut());
				assertNull(legalFormRanged.getDateFin());
				assertEquals(LegalForm.N_0106_SOCIETE_ANONYME, legalFormRanged.getPayload());
			}

			final List<DateRangeHelper.Ranged<RCRegistrationData>> locationRcRegistrationData = organisation.getLocationData().get(0).getRc().getRegistrationData();
			assertEquals(1, locationRcRegistrationData.size());
			final RCRegistrationData rcRegistration = locationRcRegistrationData.get(0).getPayload();
			assertNotNull(rcRegistration);
			assertEquals(CommercialRegisterStatus.ACTIF, rcRegistration.getRegistrationStatus());
		}
	}

	@Test
	public void testGetOrgaOfNoticeVraieErreurDansBefore() throws JAXBException {
		final long noticeId = 383324L;
		final File xmlBefore = new File("src/test/resources/samples/organisationsOfNotice/evt-383324-before.xml");
		final File xmlAfter = new File("src/test/resources/samples/organisationsOfNotice/evt-383324-after.xml");
		final JAXBElement<OrganisationsOfNotice> orgOfNoticeAfter = (JAXBElement<OrganisationsOfNotice>) unmarshaller.unmarshal(xmlAfter);
		when(client.getOrganisationsOfNotice(noticeId, RcEntClient.OrganisationState.AFTER)).thenReturn(orgOfNoticeAfter.getValue());

		final Errors orgOfNoticeBeforeErrors = (Errors) errorunmarshaller.unmarshal(xmlBefore);
		final Error error = orgOfNoticeBeforeErrors.getError().get(0);
		final RcEntClientException rcEntClientException = new RcEntClientException(new ServerWebApplicationException(), Collections.singletonList(new RcEntClientErrorMessage(error)));
		when(client.getOrganisationsOfNotice(noticeId, RcEntClient.OrganisationState.BEFORE)).thenThrow(rcEntClientException);

		try {
			final Map<Long, OrganisationEvent> historyMap = service.getOrganisationEvent(noticeId);
			fail("Une RcEntClientException aurait du être lancée.");
		}
		catch (RcEntClientException e) {
			assertEquals("Status 500 (100: Grosse erreur: 383324)", e.getMessage());
		}
	}

	@Test
	public void testGetOrgaOfNoticeErreurGraveSansMessage() throws JAXBException {
		final long noticeId = 383324L;
		final File xmlAfter = new File("src/test/resources/samples/organisationsOfNotice/evt-383324-after.xml");
		final JAXBElement<OrganisationsOfNotice> orgOfNoticeAfter = (JAXBElement<OrganisationsOfNotice>) unmarshaller.unmarshal(xmlAfter);
		when(client.getOrganisationsOfNotice(noticeId, RcEntClient.OrganisationState.AFTER)).thenReturn(orgOfNoticeAfter.getValue());

		final RcEntClientException rcEntClientException = new RcEntClientException(new ServerWebApplicationException(), null);
		when(client.getOrganisationsOfNotice(noticeId, RcEntClient.OrganisationState.BEFORE)).thenThrow(rcEntClientException);

		try {
			final Map<Long, OrganisationEvent> historyMap = service.getOrganisationEvent(noticeId);
			fail("Une RcEntClientException aurait du être lancée.");
		}
		catch (RcEntClientException e) {
			assertEquals("Status 500", e.getMessage());
		}
	}

	@Test
	public void testGetLastDiaryEntriesForEachLocation() throws JAXBException {
		final File xml = new File("src/test/resources/samples/organisationData/org-101652437-Piguet_Galland_Cie_SA.xml");
		final JAXBElement<OrganisationData> data = (JAXBElement<OrganisationData>) unmarshaller.unmarshal(xml);
		when(client.getOrganisation(101652437L, null, true)).thenReturn(data.getValue());

		final Organisation organisation = service.getOrganisationHistory(101652437L);
		assertThat(organisation.getCantonalId(), equalTo(101652437L));

		final OrganisationLocation mainLocation = (OrganisationLocation) organisation.getLocationData().stream()
				.filter(l -> l.getTypeOfLocation().get(0).getPayload() == TypeOfLocation.ETABLISSEMENT_PRINCIPAL)
				.findFirst().get();
		final List<DateRangeHelper.Ranged<UidRegisterStatus>> ideStatus = mainLocation.getUid().getStatus();
		assertNotNull(ideStatus);
		assertEquals(UidRegisterStatus.DEFINITIF, ideStatus.get(0).getPayload());

		final List<CommercialRegisterDiaryEntry> diaryEntries = mainLocation.getRc().getDiaryEntries();
		assertNotNull(diaryEntries);
		assertEquals(1, diaryEntries.size());

		final List<DateRangeHelper.Ranged<String>> locationName = mainLocation.getName();
		assertEquals("Piguet Galland & Cie SA", locationName.get(0).getPayload());
	}

	@Test
	public void testGetOrgaOfNoticePublicationForEventWithMultiplesPublicationsFosc() throws JAXBException {
		final long eventId = 505765L;
		final File xmlBefore = new File("src/test/resources/samples/organisationsOfNotice/evt-505765-before.xml");
		final File xmlAfter = new File("src/test/resources/samples/organisationsOfNotice/evt-505765-after.xml");
		final JAXBElement<OrganisationsOfNotice> orgOfNoticeBefore = (JAXBElement<OrganisationsOfNotice>) unmarshaller.unmarshal(xmlBefore);
		final JAXBElement<OrganisationsOfNotice> orgOfNoticeAfter = (JAXBElement<OrganisationsOfNotice>) unmarshaller.unmarshal(xmlAfter);

		when(client.getOrganisationsOfNotice(eventId, RcEntClient.OrganisationState.AFTER)).thenReturn(orgOfNoticeAfter.getValue());
		when(client.getOrganisationsOfNotice(eventId, RcEntClient.OrganisationState.BEFORE)).thenReturn(orgOfNoticeBefore.getValue());

		final Long noOrganisation = 101580748L;

		final Map<Long, OrganisationEvent> historyMap = service.getOrganisationEvent(eventId);

		final OrganisationEvent organisationEvent = historyMap.get(noOrganisation);

		assertThat(organisationEvent.getPseudoHistory().getCantonalId(), equalTo(noOrganisation));

		assertThat(organisationEvent.getCommercialRegisterEntryNumber(), equalTo(9342L));
		assertThat(organisationEvent.getCommercialRegisterEntryDate(), equalTo(RegDate.get(2016, 6, 8)));
		assertThat(organisationEvent.getDocumentNumberFOSC(), equalTo("2886205"));
		assertThat(organisationEvent.getPublicationDateFOSC(), equalTo(RegDate.get(2016, 6, 13)));
	}

	// Selon SIFISC-19916 On supporte qu'il y a plusieurs entrées pour le même jour, mais si c'est le cas, c'est comme s'il n'y en avait pas.
	@Test
	public void testGetOrgaOfNoticePublicationForEventWithMultiplesSameDayPublicationsFosc() throws JAXBException {
		final long eventId = 505765L;
		final File xmlBefore = new File("src/test/resources/samples/organisationsOfNotice/evt-505765-multi-before.xml");
		final File xmlAfter = new File("src/test/resources/samples/organisationsOfNotice/evt-505765-multi-after.xml");
		final JAXBElement<OrganisationsOfNotice> orgOfNoticeBefore = (JAXBElement<OrganisationsOfNotice>) unmarshaller.unmarshal(xmlBefore);
		final JAXBElement<OrganisationsOfNotice> orgOfNoticeAfter = (JAXBElement<OrganisationsOfNotice>) unmarshaller.unmarshal(xmlAfter);

		when(client.getOrganisationsOfNotice(eventId, RcEntClient.OrganisationState.AFTER)).thenReturn(orgOfNoticeAfter.getValue());
		when(client.getOrganisationsOfNotice(eventId, RcEntClient.OrganisationState.BEFORE)).thenReturn(orgOfNoticeBefore.getValue());

		final Long noOrganisation = 101580748L;

		final Map<Long, OrganisationEvent> historyMap = service.getOrganisationEvent(eventId);

		final OrganisationEvent organisationEvent = historyMap.get(noOrganisation);

		assertThat(organisationEvent.getPseudoHistory().getCantonalId(), equalTo(noOrganisation));

		assertNull(organisationEvent.getCommercialRegisterEntryNumber());
		assertNull(organisationEvent.getCommercialRegisterEntryDate());
		assertNull(organisationEvent.getDocumentNumberFOSC());
		assertNull(organisationEvent.getPublicationDateFOSC());
	}

	// Aucune entrée
	@Test
	public void testGetOrgaOfNoticePublicationForEventWithNoPublicationsFosc() throws JAXBException {
		final long eventId = 505765L;
		final File xmlBefore = new File("src/test/resources/samples/organisationsOfNotice/evt-505765-none-before.xml");
		final File xmlAfter = new File("src/test/resources/samples/organisationsOfNotice/evt-505765-none-after.xml");
		final JAXBElement<OrganisationsOfNotice> orgOfNoticeBefore = (JAXBElement<OrganisationsOfNotice>) unmarshaller.unmarshal(xmlBefore);
		final JAXBElement<OrganisationsOfNotice> orgOfNoticeAfter = (JAXBElement<OrganisationsOfNotice>) unmarshaller.unmarshal(xmlAfter);

		when(client.getOrganisationsOfNotice(eventId, RcEntClient.OrganisationState.AFTER)).thenReturn(orgOfNoticeAfter.getValue());
		when(client.getOrganisationsOfNotice(eventId, RcEntClient.OrganisationState.BEFORE)).thenReturn(orgOfNoticeBefore.getValue());

		final Long noOrganisation = 101580748L;

		final Map<Long, OrganisationEvent> historyMap = service.getOrganisationEvent(eventId);

		final OrganisationEvent organisationEvent = historyMap.get(noOrganisation);

		assertThat(organisationEvent.getPseudoHistory().getCantonalId(), equalTo(noOrganisation));

		assertNull(organisationEvent.getCommercialRegisterEntryNumber());
		assertNull(organisationEvent.getCommercialRegisterEntryDate());
		assertNull(organisationEvent.getDocumentNumberFOSC());
		assertNull(organisationEvent.getPublicationDateFOSC());
	}

	// SIFISC-19922 - Déterminer l'entrée de journal en cours, par différentiel des listes avant/après.
	@Test
	public void testGetOrgaOfNoticePublicationForEventWithMultiplesPublicationsFoscUnIncrement() throws JAXBException {
		final long eventId = 505765L;
		final File xmlBefore = new File("src/test/resources/samples/organisationsOfNotice/evt-505765-multi-one-increment-before.xml");
		final File xmlAfter = new File("src/test/resources/samples/organisationsOfNotice/evt-505765-multi-one-increment-after.xml");
		final JAXBElement<OrganisationsOfNotice> orgOfNoticeBefore = (JAXBElement<OrganisationsOfNotice>) unmarshaller.unmarshal(xmlBefore);
		final JAXBElement<OrganisationsOfNotice> orgOfNoticeAfter = (JAXBElement<OrganisationsOfNotice>) unmarshaller.unmarshal(xmlAfter);

		when(client.getOrganisationsOfNotice(eventId, RcEntClient.OrganisationState.AFTER)).thenReturn(orgOfNoticeAfter.getValue());
		when(client.getOrganisationsOfNotice(eventId, RcEntClient.OrganisationState.BEFORE)).thenReturn(orgOfNoticeBefore.getValue());

		final Long noOrganisation = 101580748L;

		final Map<Long, OrganisationEvent> historyMap = service.getOrganisationEvent(eventId);

		final OrganisationEvent organisationEvent = historyMap.get(noOrganisation);

		assertThat(organisationEvent.getPseudoHistory().getCantonalId(), equalTo(noOrganisation));

		assertThat(organisationEvent.getCommercialRegisterEntryNumber(), equalTo(4444L));
		assertThat(organisationEvent.getCommercialRegisterEntryDate(), equalTo(RegDate.get(2016, 8, 11)));
		assertThat(organisationEvent.getDocumentNumberFOSC(), equalTo("3325344444"));
		assertThat(organisationEvent.getPublicationDateFOSC(), equalTo(RegDate.get(2016, 8, 14)));
	}

	// SIFISC-19922 - Déterminer la publication FOSC en cours, par différentiel des listes avant/après.
	@Test
	public void testGetOrgaOfNoticePublicationForEventWithMultiplesPublicationsBusinessUnIncrement() throws JAXBException {
		final long eventId = 505765L;
		final File xmlBefore = new File("src/test/resources/samples/organisationsOfNotice/evt-505765-bp-multi-one-increment-before.xml");
		final File xmlAfter = new File("src/test/resources/samples/organisationsOfNotice/evt-505765-bp-multi-one-increment-after.xml");
		final JAXBElement<OrganisationsOfNotice> orgOfNoticeBefore = (JAXBElement<OrganisationsOfNotice>) unmarshaller.unmarshal(xmlBefore);
		final JAXBElement<OrganisationsOfNotice> orgOfNoticeAfter = (JAXBElement<OrganisationsOfNotice>) unmarshaller.unmarshal(xmlAfter);

		when(client.getOrganisationsOfNotice(eventId, RcEntClient.OrganisationState.AFTER)).thenReturn(orgOfNoticeAfter.getValue());
		when(client.getOrganisationsOfNotice(eventId, RcEntClient.OrganisationState.BEFORE)).thenReturn(orgOfNoticeBefore.getValue());

		final Long noOrganisation = 101580748L;

		final Map<Long, OrganisationEvent> historyMap = service.getOrganisationEvent(eventId);

		final OrganisationEvent organisationEvent = historyMap.get(noOrganisation);

		assertThat(organisationEvent.getPseudoHistory().getCantonalId(), equalTo(noOrganisation));

		assertThat(organisationEvent.getCommercialRegisterEntryNumber(), nullValue());
		assertThat(organisationEvent.getCommercialRegisterEntryDate(), nullValue());
		assertThat(organisationEvent.getDocumentNumberFOSC(), equalTo("3325344444"));
		assertThat(organisationEvent.getPublicationDateFOSC(), equalTo(RegDate.get(2016, 8, 14)));
	}
}