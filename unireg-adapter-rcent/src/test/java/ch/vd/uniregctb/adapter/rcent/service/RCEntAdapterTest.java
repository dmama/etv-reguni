package ch.vd.uniregctb.adapter.rcent.service;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ch.vd.evd0021.v1.Address;
import ch.vd.evd0022.v1.Authorisation;
import ch.vd.evd0022.v1.Capital;
import ch.vd.evd0022.v1.CommercialRegisterEntryStatus;
import ch.vd.evd0022.v1.KindOfLocation;
import ch.vd.evd0022.v1.LegalForm;
import ch.vd.evd0022.v1.OrganisationData;
import ch.vd.evd0022.v1.TypeOfCapital;
import ch.vd.evd0022.v1.UidRegisterStatus;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.wsclient.rcent.RcEntClient;
import ch.vd.uniregctb.adapter.rcent.historizer.OrganisationHistorizer;
import ch.vd.uniregctb.adapter.rcent.model.Organisation;
import ch.vd.uniregctb.adapter.rcent.model.OrganisationFunction;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

public class RCEntAdapterTest {

	@Mock
	private RcEntClient client;

	private OrganisationHistorizer historizer = new OrganisationHistorizer();

	private Unmarshaller unmarshaller;

	private RCEntAdapter service;

	@Before
	public void setUp() throws Exception {
		JAXBContext jc = JAXBContext.newInstance("ch.vd.evd0023.v1");
		unmarshaller = jc.createUnmarshaller();

		MockitoAnnotations.initMocks(this);

		service = new RCEntAdapter(client, historizer);
	}

	@Test
	public void testGetOrganisation() throws Exception {
		File xml = new File("src/test/resources/samples/organisationData/CHE101992624-no-history.xml");
		JAXBElement<OrganisationData> data = (JAXBElement<OrganisationData>) unmarshaller.unmarshal(xml);

		when(client.getOrganisation(101202262, null, false)).thenReturn(data.getValue());

		Organisation organisation = service.getOrganisation(101202262, null);
		assertThat(organisation.getCantonalId(), equalTo(101202262L));

		// Check basic organisation data
		{
			assertThat(organisation.getLegalForm().get(0).getDateDebut(), equalTo(RegDate.get(2015, 5, 1)));
			assertThat(organisation.getLegalForm().get(0).getDateFin(), nullValue());
			assertThat(organisation.getLegalForm().get(0).getPayload(), equalTo(LegalForm.N_0101_ENTREPRISE_INDIVIDUELLE));
		}

		assertThat(organisation.getOrganisationName().get(0).getPayload(), equalTo("Jean-François Niklaus"));

		// Locations
		assertThat(organisation.getLocations().size(), equalTo(1));
		assertThat(organisation.getLocations().values().iterator().next().get(0).getPayload(), equalTo(101072745L));

		assertThat(organisation.getLocationData().size(), equalTo(1));
		assertThat(organisation.getLocationData().get(0).getCantonalId(), equalTo(101072745L));

		Map<String, List<DateRangeHelper.Ranged<String>>> identifierMap = organisation.getLocationData().get(0).getIdentifiers();
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

		final KindOfLocation kindOfLocation = organisation.getLocationData().get(0).getKindOfLocation().get(0).getPayload();
		assertThat(kindOfLocation, equalTo(KindOfLocation.ETABLISSEMENT_PRINCIPAL));

		assertThat(organisation.getLocationData().get(0).getSeat().get(0).getPayload(), equalTo(5561));

		// Adresse
		Address effectiveAddress = organisation.getLocationData().get(0).getUid().getEffectiveAddress().get(0).getPayload();
		assertThat(effectiveAddress.getAddressLine1(), equalTo("Les Tuileries"));
		assertThat(effectiveAddress.getAddressLine2(), nullValue());
		assertThat(effectiveAddress.getStreet(), equalTo("Chemin du Mont"));
		assertThat(effectiveAddress.getHouseNumber(), equalTo("15"));
		assertThat(effectiveAddress.getSwissZipCode(), equalTo(1422L));
		assertThat(effectiveAddress.getCountry().getCountryName(), equalTo("CH"));
		assertThat(effectiveAddress.getTown(), equalTo("Grandson"));
		assertThat(effectiveAddress.getFederalBuildingId(), equalTo(872180L));

	}

	@Test
	public void testGetOrganisationHistory() throws Exception {
		File xml = new File("src/test/resources/samples/organisationData/CHE101992624.xml");
		JAXBElement<OrganisationData> data = (JAXBElement<OrganisationData>) unmarshaller.unmarshal(xml);

		when(client.getOrganisation(101202262, null, true)).thenReturn(data.getValue());

		Organisation organisation = service.getOrganisationHistory(101202262);
		assertThat(organisation.getCantonalId(), equalTo(101202262L));

		// Adresse
		final DateRangeHelper.Ranged<Address> postOfficeBoxAddressRange1 = organisation.getLocationData().get(0).getUid().getPostOfficeBoxAddress().get(0);
		Address postalAddress_period1 = postOfficeBoxAddressRange1.getPayload();
		assertThat(postOfficeBoxAddressRange1.getDateDebut(), equalTo(RegDate.get(2015, 4, 29)));
		assertThat(postOfficeBoxAddressRange1.getDateFin(), equalTo(RegDate.get(2015, 4, 30)));
		assertThat(postalAddress_period1.getAddressLine1(), nullValue());
		assertThat(postalAddress_period1.getAddressLine2(), nullValue());
		assertThat(postalAddress_period1.getStreet(), equalTo("Chemin du Mont"));
		assertThat(postalAddress_period1.getHouseNumber(), equalTo("15"));
		assertThat(postalAddress_period1.getTown(), equalTo("Triffouilli les oies"));
		assertThat(postalAddress_period1.getSwissZipCode(), equalTo(1101L));
		assertThat(postalAddress_period1.getCountry().getCountryName(), equalTo("CH"));

		// Adresse changée
		final DateRangeHelper.Ranged<Address> postOfficeBoxAddressRange2 = organisation.getLocationData().get(0).getUid().getPostOfficeBoxAddress().get(1);
		Address postalAddress_period2 = postOfficeBoxAddressRange2.getPayload();
		assertThat(postOfficeBoxAddressRange2.getDateDebut(), equalTo(RegDate.get(2015, 5, 1)));
		assertThat(postOfficeBoxAddressRange2.getDateFin(), nullValue());
		assertThat(postalAddress_period2.getAddressLine1(), nullValue());
		assertThat(postalAddress_period2.getAddressLine2(), nullValue());
		assertThat(postalAddress_period2.getStreet(), equalTo("Chemin du Mont"));
		assertThat(postalAddress_period2.getHouseNumber(), equalTo("15"));
		assertThat(postalAddress_period2.getTown(), equalTo("Grandson"));
		assertThat(postalAddress_period2.getSwissZipCode(), equalTo(1422L));
		assertThat(postalAddress_period2.getCountry().getCountryName(), equalTo("CH"));
	}

	@Test
	public void testGetOrganisationHistory4Snapshots() throws Exception {
		File xml = new File("src/test/resources/samples/organisationData/CHE101992624-4snaps.xml");
		JAXBElement<OrganisationData> data = (JAXBElement<OrganisationData>) unmarshaller.unmarshal(xml);

		when(client.getOrganisation(101202262, null, true)).thenReturn(data.getValue());

		Organisation organisation = service.getOrganisationHistory(101202262);
		assertThat(organisation.getCantonalId(), equalTo(101202262L));

		// Adresse postale
		assertThat(organisation.getLocationData().get(0).getUid().getPostOfficeBoxAddress().size(), equalTo(3));

		final DateRangeHelper.Ranged<Address> addressDateRanged1 = organisation.getLocationData().get(0).getUid().getPostOfficeBoxAddress().get(0);
		Address postalAddress_period1 = addressDateRanged1.getPayload();
		assertThat(addressDateRanged1.getDateDebut(), equalTo(RegDate.get(2015, 4, 29)));
		assertThat(addressDateRanged1.getDateFin(), equalTo(RegDate.get(2015, 4, 30)));
		assertThat(postalAddress_period1.getAddressLine1(), nullValue());
		assertThat(postalAddress_period1.getAddressLine2(), nullValue());
		assertThat(postalAddress_period1.getStreet(), equalTo("Chemin du Mont"));
		assertThat(postalAddress_period1.getHouseNumber(), equalTo("15"));
		assertThat(postalAddress_period1.getTown(), equalTo("Triffouilli les oies"));
		assertThat(postalAddress_period1.getSwissZipCode(), equalTo(1101L));
		assertThat(postalAddress_period1.getCountry().getCountryName(), equalTo("CH"));

		// Adresse changée
		final DateRangeHelper.Ranged<Address> addressDateRanged2 = organisation.getLocationData().get(0).getUid().getPostOfficeBoxAddress().get(1);
		Address postalAddress_period2 = addressDateRanged2.getPayload();
		assertThat(addressDateRanged2.getDateDebut(), equalTo(RegDate.get(2015, 5, 1)));
		assertThat(addressDateRanged2.getDateFin(), equalTo(RegDate.get(2015, 5, 23)));
		assertThat(postalAddress_period2.getAddressLine1(), nullValue());
		assertThat(postalAddress_period2.getAddressLine2(), nullValue());
		assertThat(postalAddress_period2.getStreet(), equalTo("Chemin du Mont"));
		assertThat(postalAddress_period2.getHouseNumber(), equalTo("15"));
		assertThat(postalAddress_period2.getTown(), equalTo("Grandson"));
		assertThat(postalAddress_period2.getSwissZipCode(), equalTo(1422L));
		assertThat(postalAddress_period2.getCountry().getCountryName(), equalTo("CH"));

		// Adresse enlevée puis nouvelle plus tard
		final DateRangeHelper.Ranged<Address> addressDateRanged3 = organisation.getLocationData().get(0).getUid().getPostOfficeBoxAddress().get(2);
		Address postalAddress_period3 = addressDateRanged3.getPayload();
		assertThat(addressDateRanged3.getDateDebut(), equalTo(RegDate.get(2015, 6, 6)));
		assertThat(addressDateRanged3.getDateFin(), nullValue());
		assertThat(postalAddress_period3.getAddressLine1(), nullValue());
		assertThat(postalAddress_period3.getAddressLine2(), nullValue());
		assertThat(postalAddress_period3.getStreet(), equalTo("Chemin du lac"));
		assertThat(postalAddress_period3.getHouseNumber(), equalTo("18"));
		assertThat(postalAddress_period3.getTown(), equalTo("Hiverdon"));
		assertThat(postalAddress_period3.getSwissZipCode(), equalTo(1919L));
		assertThat(postalAddress_period3.getCountry().getCountryName(), equalTo("CH"));
	}

	@Test
	public void testGetOrganisationHistorySocieteAnonyme() throws Exception {
		File xml = new File("src/test/resources/samples/organisationData/CHE105879116.xml");
		JAXBElement<OrganisationData> data = (JAXBElement<OrganisationData>) unmarshaller.unmarshal(xml);

		when(client.getOrganisation(101202213, null, false)).thenReturn(data.getValue());

		Organisation organisation = service.getOrganisation(101202213, null);
		assertThat(organisation.getCantonalId(), equalTo(101202213L));

		// Check basic organisation data
		assertThat(organisation.getLegalForm().get(0).getDateDebut(), equalTo(RegDate.get(2015, 4, 29)));
		assertThat(organisation.getLegalForm().get(0).getDateFin(), nullValue());
		assertThat(organisation.getLegalForm().get(0).getPayload(), equalTo(LegalForm.N_0106_SOCIETE_ANONYME));

		{ // Nom
			assertThat(organisation.getOrganisationName().get(0).getDateDebut(), equalTo(RegDate.get(2015, 4, 29)));
			assertThat(organisation.getOrganisationName().get(0).getDateFin(), equalTo(RegDate.get(2015, 5, 24)));
			assertThat(organisation.getOrganisationName().get(0).getPayload(), equalTo("Seroc S.A."));

			assertThat(organisation.getOrganisationName().get(1).getDateDebut(), equalTo(RegDate.get(2015, 5, 25)));
			assertThat(organisation.getOrganisationName().get(1).getDateFin(), equalTo(RegDate.get(2015, 6, 5)));
			assertThat(organisation.getOrganisationName().get(1).getPayload(), equalTo("SeroKK S.A."));

			assertThat(organisation.getOrganisationName().get(2).getDateDebut(), equalTo(RegDate.get(2015, 6, 6)));
			assertThat(organisation.getOrganisationName().get(2).getDateFin(), nullValue());
			assertThat(organisation.getOrganisationName().get(2).getPayload(), equalTo("ZeroXX S.A."));
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


		Map<String, List<DateRangeHelper.Ranged<String>>> identifierMap = organisation.getLocationData().get(0).getIdentifiers();
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

		final KindOfLocation kindOfLocation = organisation.getLocationData().get(0).getKindOfLocation().get(0).getPayload();
		assertThat(kindOfLocation, equalTo(KindOfLocation.ETABLISSEMENT_PRINCIPAL));

		assertThat(organisation.getLocationData().get(0).getSeat().get(0).getPayload(), equalTo(5413));


		// Adresse postale
		assertThat(organisation.getLocationData().get(0).getUid().getPostOfficeBoxAddress().size(), equalTo(2));

		final DateRangeHelper.Ranged<Address> addressDateRanged1 = organisation.getLocationData().get(0).getUid().getPostOfficeBoxAddress().get(0);
		Address postalAddress_period1 = addressDateRanged1.getPayload();
		assertThat(addressDateRanged1.getDateDebut(), equalTo(RegDate.get(2015, 4, 29)));
		assertThat(addressDateRanged1.getDateFin(), equalTo(RegDate.get(2015, 4, 30)));
		assertThat(postalAddress_period1.getAddressLine1(), nullValue());
		assertThat(postalAddress_period1.getAddressLine2(), nullValue());
		assertThat(postalAddress_period1.getStreet(), equalTo("Zone Industrielle La Coche"));
		assertThat(postalAddress_period1.getHouseNumber(), equalTo("11"));
		assertThat(postalAddress_period1.getTown(), equalTo("Roche VD"));
		assertThat(postalAddress_period1.getSwissZipCode(), equalTo(1852L));
		assertThat(postalAddress_period1.getCountry().getCountryName(), equalTo("CH"));

		// Adresse enlevée puis nouvelle plus tard, à l'identique
		final DateRangeHelper.Ranged<Address> addressDateRanged2 = organisation.getLocationData().get(0).getUid().getPostOfficeBoxAddress().get(1);
		Address postalAddress_period2 = addressDateRanged2.getPayload();
		assertThat(addressDateRanged2.getDateDebut(), equalTo(RegDate.get(2015, 5, 15)));
		assertThat(addressDateRanged2.getDateFin(), nullValue());
		assertThat(postalAddress_period2.getAddressLine1(), nullValue());
		assertThat(postalAddress_period2.getAddressLine2(), nullValue());
		assertThat(postalAddress_period2.getStreet(), equalTo("Zone Industrielle La Coche"));
		assertThat(postalAddress_period2.getHouseNumber(), equalTo("11"));
		assertThat(postalAddress_period2.getTown(), equalTo("Roche VD"));
		assertThat(postalAddress_period2.getSwissZipCode(), equalTo(1852L));
		assertThat(postalAddress_period2.getCountry().getCountryName(), equalTo("CH"));
	}

	@Test
	public void testGetOrganisationHistorySample100983251() throws JAXBException {
		File xml = new File("src/test/resources/samples/organisationData/organisation-100983251-history.xml");
		JAXBElement<OrganisationData> data = (JAXBElement<OrganisationData>) unmarshaller.unmarshal(xml);
		when(client.getOrganisation(100983251L, null, true)).thenReturn(data.getValue());

		Organisation organisation = service.getOrganisationHistory(100983251L);
		assertThat(organisation.getCantonalId(), equalTo(100983251L));

		List<DateRangeHelper.Ranged<UidRegisterStatus>> ideStatus = organisation.getLocationData().get(0).getUid().getStatus();
		assertNotNull(ideStatus);
		assertEquals(UidRegisterStatus.DEFINITIF, ideStatus.get(0).getPayload());

		List<DateRangeHelper.Ranged<Capital>> capitalRanges = organisation.getLocationData().get(0).getRc().getCapital();
		assertNotNull(capitalRanges);
		Capital capital = capitalRanges.get(0).getPayload();
		assertEquals("CHF", capital.getCurrency());
		assertEquals(new BigDecimal(25000), capital.getCapitalAmount());
		assertEquals(new BigDecimal(23000), capital.getCashedInAmount());
		assertEquals(TypeOfCapital.CAPITAL_SOCIAL, capital.getTypeOfCapital());

		List<DateRangeHelper.Ranged<String>> locationRcName = organisation.getLocationData().get(0).getRc().getName();
		assertEquals("Bomaco Sàrl en liquidation", locationRcName.get(0).getPayload());

		List<DateRangeHelper.Ranged<CommercialRegisterEntryStatus>> locationRcEntryStatus = organisation.getLocationData().get(0).getRc().getEntryStatus();
		assertEquals(CommercialRegisterEntryStatus.ACTIF, locationRcEntryStatus.get(0).getPayload());

		List<DateRangeHelper.Ranged<RegDate>> locationRcEntryDate = organisation.getLocationData().get(0).getRc().getEntryDate();
		assertEquals(RegDate.get(2007, 4, 16), locationRcEntryDate.get(0).getPayload());

		Map<String, List<DateRangeHelper.Ranged<OrganisationFunction>>> locationFunctions = organisation.getLocationData().get(0).getFunction();
		assertEquals(2, locationFunctions.size()); // S'il y en a plus, c'est que l'Historizer ne sait pas identifier proprement les fonctions qu'on doit considérer identiques.

		assertEquals("Harrison Ford", locationFunctions.get("Harrison Ford").get(0).getPayload().getName());
		assertEquals(RegDate.get(2015, 7, 7), locationFunctions.get("Harrison Ford").get(0).getDateDebut());
		assertEquals("Président du Conseil d'Administration", locationFunctions.get("Harrison Ford").get(0).getPayload().getFunctionText());
		assertEquals(Authorisation.SIG_INDIVIDUELLE, locationFunctions.get("Harrison Ford").get(0).getPayload().getAuthorisation());
		assertNull(locationFunctions.get("Harrison Ford").get(0).getDateFin());

		assertEquals("Harring", locationFunctions.get("Harring").get(0).getPayload().getName());
		assertEquals(RegDate.get(2015, 8, 5), locationFunctions.get("Harring").get(0).getDateDebut());
		assertNull(locationFunctions.get("Harring").get(0).getDateFin());
	}
}