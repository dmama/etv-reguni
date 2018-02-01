package ch.vd.unireg.interfaces.civil.data;

import ch.ech.ech0007.v4.CantonAbbreviation;
import ch.ech.ech0007.v4.SwissMunicipality;
import ch.ech.ech0008.v2.Country;
import ch.ech.ech0010.v4.AddressInformation;
import ch.ech.ech0010.v4.MailAddress;
import ch.ech.ech0010.v4.PersonMailAddressInfo;
import ch.ech.ech0011.v5.Destination;
import org.junit.Assert;
import org.junit.Test;

import ch.vd.evd0001.v5.Contact;
import ch.vd.unireg.interfaces.common.Adresse;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureRaw;
import ch.vd.unireg.interfaces.infra.mock.DefaultMockServiceInfrastructureService;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockLocalite;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.common.WithoutSpringTest;
import ch.vd.unireg.common.XmlUtils;
import ch.vd.unireg.type.TexteCasePostale;

public class AdresseRCPersTest extends WithoutSpringTest {

	private ServiceInfrastructureRaw infraService;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		infraService = new DefaultMockServiceInfrastructureService();
	}

	@Test
	public void testInitLocalisation() throws Exception {
		{
			final Destination destination = null;
			final Localisation localisation = AdresseRCPers.initLocalisation(null, destination, infraService);
			Assert.assertNull(localisation);
		}
		{
			final Destination destination = new Destination("unknown", null, null, null);
			final Localisation localisation = AdresseRCPers.initLocalisation(null, destination, infraService);
			Assert.assertNotNull(localisation);
			Assert.assertEquals(LocalisationType.HORS_SUISSE, localisation.getType());
			Assert.assertEquals((Integer) MockPays.PaysInconnu.getNoOFS(), localisation.getNoOfs());
		}
		{
			final Country country = new Country(MockPays.France.getNoOFS(), MockPays.France.getCodeIso2(), MockPays.France.getNomCourt());
			final Destination.ForeignCountry foreignCountry = new Destination.ForeignCountry(country, null);
			final Destination destination = new Destination(null, null, foreignCountry, null);
			final Localisation localisation = AdresseRCPers.initLocalisation(null, destination, infraService);
			Assert.assertNotNull(localisation);
			Assert.assertEquals(LocalisationType.HORS_SUISSE, localisation.getType());
			Assert.assertEquals((Integer) MockPays.France.getNoOFS(), localisation.getNoOfs());
		}
		{
			final Country country = new Country(MockPays.Gibraltar.getNoOFS(), MockPays.Gibraltar.getCodeIso2(), MockPays.Gibraltar.getNomCourt());
			final Destination.ForeignCountry foreignCountry = new Destination.ForeignCountry(country, null);
			final Destination destination = new Destination(null, null, foreignCountry, null);
			final Localisation localisation = AdresseRCPers.initLocalisation(null, destination, infraService);
			Assert.assertNotNull(localisation);
			Assert.assertEquals(LocalisationType.HORS_SUISSE, localisation.getType());
			Assert.assertEquals((Integer) MockPays.RoyaumeUni.getNoOFS(), localisation.getNoOfs());
		}
		{
			final Country country = new Country(99999, "XX", "Pays bidon...");
			final Destination.ForeignCountry foreignCountry = new Destination.ForeignCountry(country, null);
			final Destination destination = new Destination(null, null, foreignCountry, null);
			final Localisation localisation = AdresseRCPers.initLocalisation(null, destination, infraService);
			Assert.assertNotNull(localisation);
			Assert.assertEquals(LocalisationType.HORS_SUISSE, localisation.getType());
			Assert.assertEquals((Integer) MockPays.PaysInconnu.getNoOFS(), localisation.getNoOfs());
		}
		{
			final SwissMunicipality swissTown = new SwissMunicipality(MockCommune.Cossonay.getNoOFS(), MockCommune.Cossonay.getNomOfficiel(), CantonAbbreviation.VD, null);
			final Destination destination = new Destination(null, swissTown, null, null);
			final Localisation localisation = AdresseRCPers.initLocalisation(null, destination, infraService);
			Assert.assertNotNull(localisation);
			Assert.assertEquals(LocalisationType.CANTON_VD, localisation.getType());
			Assert.assertEquals((Integer) MockCommune.Cossonay.getNoOFS(), localisation.getNoOfs());
		}
		{
			final SwissMunicipality swissTown = new SwissMunicipality(MockCommune.Bale.getNoOFS(), MockCommune.Bale.getNomOfficiel(), CantonAbbreviation.BS, null);
			final Destination destination = new Destination(null, swissTown, null, null);
			final Localisation localisation = AdresseRCPers.initLocalisation(null, destination, infraService);
			Assert.assertNotNull(localisation);
			Assert.assertEquals(LocalisationType.HORS_CANTON, localisation.getType());
			Assert.assertEquals((Integer) MockCommune.Bale.getNoOFS(), localisation.getNoOfs());
		}
	}

	@Test
	public void testCasePostale() throws Exception {
		final PersonMailAddressInfo person = new PersonMailAddressInfo("1", null, "Alexandra", "Malo");
		final AddressInformation addressInfo = new AddressInformation(null, null, "Chemin des blés", "12", null, null, "Postfach 131", "Lausanne", null, 1000L, null, MockLocalite.Lausanne.getNoOrdre(), null, "Suisse");
		final MailAddress mailAddress = new MailAddress(null, person, addressInfo);
		final Contact contact = new Contact(XmlUtils.regdate2xmlcal(date(2009, 1, 1)), null, mailAddress);
		final Adresse rcpers = AdresseRCPers.get(contact, infraService);
		Assert.assertNotNull(rcpers);
		Assert.assertNotNull(rcpers.getCasePostale());
		Assert.assertEquals(TexteCasePostale.POSTFACH, rcpers.getCasePostale().getType());
		Assert.assertEquals((Integer) 131, rcpers.getCasePostale().getNumero());
	}
}
