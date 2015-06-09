package ch.vd.uniregctb.migration.pm.rcent.service;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ch.vd.evd0021.v1.Address;
import ch.vd.evd0022.v1.Identifier;
import ch.vd.evd0022.v1.KindOfLocation;
import ch.vd.evd0022.v1.LegalForm;
import ch.vd.evd0022.v1.OrganisationData;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.wsclient.rcent.RcEntClient;
import ch.vd.uniregctb.migration.pm.historizer.OrganisationHistorizer;
import ch.vd.uniregctb.migration.pm.historizer.container.DateRanged;
import ch.vd.uniregctb.migration.pm.rcent.model.Organisation;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

public class RCEntServiceTest {

	@Mock
	private RcEntClient client;

	private OrganisationHistorizer historizer = new OrganisationHistorizer();

	private Unmarshaller unmarshaller;

	private RCEntService service;

	@Before
	public void setUp() throws Exception {
		JAXBContext jc = JAXBContext.newInstance("ch.vd.evd0023.v1");
		unmarshaller = jc.createUnmarshaller();

		MockitoAnnotations.initMocks(this);

		service = new RCEntService(client, historizer);
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
		assertThat(organisation.getLocations().get(0).getPayload(), equalTo(101072745L));

		assertThat(organisation.getLocationData().size(), equalTo(1));
		assertThat(organisation.getLocationData().get(0).getCantonalId(), equalTo(101072745L));

		List<DateRanged<Identifier>> locationIdentifiers = organisation.getLocationData().get(0).getIdentifier();
		assertThat(locationIdentifiers.size(), equalTo(5));

		Map<String, String> identifierMap = locationIdentifiers.stream().map(DateRanged::getPayload).collect(
				Collectors.toMap(Identifier::getIdentifierCategory, Identifier::getIdentifierValue)
		);
		assertThat(identifierMap.get("CH.HR"), equalTo("CH55000431371"));
		assertThat(identifierMap.get("CH.IDE"), equalTo("CHE101992624"));
		assertThat(identifierMap.get("CH.RC"), equalTo("CH55000431371"));
		assertThat(identifierMap.get("CHE"), equalTo("101992624"));
		assertThat(identifierMap.get("CT.VD.PARTY"), equalTo("101072745"));

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
		final DateRanged<Address> postOfficeBoxAddressRange1 = organisation.getLocationData().get(0).getUid().getPostOfficeBoxAddress().get(0);
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
		final DateRanged<Address> postOfficeBoxAddressRange2 = organisation.getLocationData().get(0).getUid().getPostOfficeBoxAddress().get(1);
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

		final DateRanged<Address> addressDateRanged1 = organisation.getLocationData().get(0).getUid().getPostOfficeBoxAddress().get(0);
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
		final DateRanged<Address> addressDateRanged2 = organisation.getLocationData().get(0).getUid().getPostOfficeBoxAddress().get(1);
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
		final DateRanged<Address> addressDateRanged3 = organisation.getLocationData().get(0).getUid().getPostOfficeBoxAddress().get(2);
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
		assertThat(organisation.getLocations().get(0).getPayload(), equalTo(101072728L));

		assertThat(organisation.getLocationData().size(), equalTo(1));
		assertThat(organisation.getLocationData().get(0).getCantonalId(), equalTo(101072728L));

		List<DateRanged<Identifier>> locationIdentifiers = organisation.getLocationData().get(0).getIdentifier();
		assertThat(locationIdentifiers.size(), equalTo(8));

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

		Map<String, String> identifierMap = locationIdentifiers.stream().map(DateRanged::getPayload).collect(
				Collectors.toMap(Identifier::getIdentifierCategory, Identifier::getIdentifierValue)
		);
		assertThat(identifierMap.get("CH.HR"), equalTo("CH55001688715"));
		assertThat(identifierMap.get("CH.IDE"), equalTo("CHE105879116"));
		assertThat(identifierMap.get("CH.IDE.TVA"), equalTo("CHE105879116"));
		assertThat(identifierMap.get("CH.MWST"), equalTo("196378"));
		assertThat(identifierMap.get("CH.RC"), equalTo("CH55001688715"));
		assertThat(identifierMap.get("CH.TVA"), equalTo("196378"));
		assertThat(identifierMap.get("CHE"), equalTo("105879116"));
		assertThat(identifierMap.get("CT.VD.PARTY"), equalTo("101072728"));

		final KindOfLocation kindOfLocation = organisation.getLocationData().get(0).getKindOfLocation().get(0).getPayload();
		assertThat(kindOfLocation, equalTo(KindOfLocation.ETABLISSEMENT_PRINCIPAL));

		assertThat(organisation.getLocationData().get(0).getSeat().get(0).getPayload(), equalTo(5413));


		// Adresse postale
		assertThat(organisation.getLocationData().get(0).getUid().getPostOfficeBoxAddress().size(), equalTo(2));

		final DateRanged<Address> addressDateRanged1 = organisation.getLocationData().get(0).getUid().getPostOfficeBoxAddress().get(0);
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
		final DateRanged<Address> addressDateRanged2 = organisation.getLocationData().get(0).getUid().getPostOfficeBoxAddress().get(1);
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
}