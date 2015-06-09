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
		assertThat(organisation.getLegalForm().get(0).getDateDebut(), equalTo(RegDate.get(2015, 5, 1)));
		assertThat(organisation.getLegalForm().get(0).getDateFin(), nullValue());
		assertThat(organisation.getLegalForm().get(0).getPayload(), equalTo(LegalForm.N_0101_ENTREPRISE_INDIVIDUELLE));

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
		Address postalAddress_period1 = organisation.getLocationData().get(0).getUid().getPostOfficeBoxAddress().get(1).getPayload();
																												   // TODO: Voir probleme de l'ordre chronologique. Trier ou non à la création?
		assertThat(organisation.getLocationData().get(0).getUid().getPostOfficeBoxAddress().get(0).getDateDebut(), equalTo(RegDate.get(2015, 4, 29)));
		assertThat(organisation.getLocationData().get(0).getUid().getPostOfficeBoxAddress().get(0).getDateFin(), equalTo(RegDate.get(2015, 4, 30)));
		assertThat(postalAddress_period1.getAddressLine1(), nullValue());
		assertThat(postalAddress_period1.getAddressLine2(), nullValue());
		assertThat(postalAddress_period1.getStreet(), equalTo("Chemin du Mont"));
		assertThat(postalAddress_period1.getHouseNumber(), equalTo("15"));
		assertThat(postalAddress_period1.getSwissZipCode(), equalTo(1422L));
		assertThat(postalAddress_period1.getCountry().getCountryName(), equalTo("CH"));
		assertThat(postalAddress_period1.getStreet(), equalTo("Chemin du Mont"));

		// Adresse changée
		Address postalAddress_period2 = organisation.getLocationData().get(0).getUid().getPostOfficeBoxAddress().get(0).getPayload();
																												// TODO: Voir probleme de l'ordre chronologique. Trier ou non à la création?
		assertThat(organisation.getLocationData().get(0).getUid().getPostOfficeBoxAddress().get(1).getDateDebut(), equalTo(RegDate.get(2015, 5, 1)));
		assertThat(organisation.getLocationData().get(0).getUid().getPostOfficeBoxAddress().get(1).getDateFin(), nullValue());
		assertThat(postalAddress_period2.getAddressLine1(), nullValue());
		assertThat(postalAddress_period2.getAddressLine2(), nullValue());
		assertThat(postalAddress_period2.getStreet(), equalTo("Chemin du Mont"));
		assertThat(postalAddress_period2.getHouseNumber(), equalTo("15"));
		assertThat(postalAddress_period2.getTown(), equalTo("Triffouilli les oies"));
		assertThat(postalAddress_period2.getSwissZipCode(), equalTo(1101L));
		assertThat(postalAddress_period2.getCountry().getCountryName(), equalTo("CH"));
	}

	@Test
	public void testGetOrganisationSocieteAnonyme() throws Exception {
		File xml = new File("src/test/resources/samples/organisationData/CHE105879116-no-history.xml");
		JAXBElement<OrganisationData> data = (JAXBElement<OrganisationData>) unmarshaller.unmarshal(xml);

		when(client.getOrganisation(101202213, null, false)).thenReturn(data.getValue());

		Organisation organisation = service.getOrganisation(101202213, null);
		assertThat(organisation.getCantonalId(), equalTo(101202213L));
	}

}