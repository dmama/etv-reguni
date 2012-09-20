package ch.vd.unireg.interfaces.infra.data;

import ch.ech.ech0007.v4.CantonAbbreviation;
import ch.ech.ech0007.v4.SwissMunicipality;
import ch.ech.ech0008.v2.Country;
import ch.ech.ech0011.v5.Destination;
import junit.framework.Assert;
import org.junit.Test;

import ch.vd.unireg.interfaces.civil.data.Localisation;
import ch.vd.unireg.interfaces.civil.data.LocalisationType;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureRaw;
import ch.vd.unireg.interfaces.infra.mock.DefaultMockServiceInfrastructureService;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.uniregctb.common.WithoutSpringTest;

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
			final Localisation localisation = AdresseRCPers.initLocalisation(destination, infraService);
			Assert.assertNull(localisation);
		}
		{
			final Destination destination = new Destination("unknown", null, null, null);
			final Localisation localisation = AdresseRCPers.initLocalisation(destination, infraService);
			Assert.assertNotNull(localisation);
			Assert.assertEquals(LocalisationType.HORS_SUISSE, localisation.getType());
			Assert.assertEquals((Integer) MockPays.PaysInconnu.getNoOFS(), localisation.getNoOfs());
		}
		{
			final Country country = new Country(MockPays.France.getNoOFS(), MockPays.France.getCodeIso2(), MockPays.France.getNomMinuscule());
			final Destination.ForeignCountry foreignCountry = new Destination.ForeignCountry(country, null);
			final Destination destination = new Destination(null, null, foreignCountry, null);
			final Localisation localisation = AdresseRCPers.initLocalisation(destination, infraService);
			Assert.assertNotNull(localisation);
			Assert.assertEquals(LocalisationType.HORS_SUISSE, localisation.getType());
			Assert.assertEquals((Integer) MockPays.France.getNoOFS(), localisation.getNoOfs());
		}
		{
			final Country country = new Country(MockPays.Gibraltar.getNoOFS(), MockPays.Gibraltar.getCodeIso2(), MockPays.Gibraltar.getNomMinuscule());
			final Destination.ForeignCountry foreignCountry = new Destination.ForeignCountry(country, null);
			final Destination destination = new Destination(null, null, foreignCountry, null);
			final Localisation localisation = AdresseRCPers.initLocalisation(destination, infraService);
			Assert.assertNotNull(localisation);
			Assert.assertEquals(LocalisationType.HORS_SUISSE, localisation.getType());
			Assert.assertEquals((Integer) MockPays.RoyaumeUni.getNoOFS(), localisation.getNoOfs());
		}
		{
			final Country country = new Country(99999, "XX", "Pays bidon...");
			final Destination.ForeignCountry foreignCountry = new Destination.ForeignCountry(country, null);
			final Destination destination = new Destination(null, null, foreignCountry, null);
			final Localisation localisation = AdresseRCPers.initLocalisation(destination, infraService);
			Assert.assertNotNull(localisation);
			Assert.assertEquals(LocalisationType.HORS_SUISSE, localisation.getType());
			Assert.assertEquals((Integer) MockPays.PaysInconnu.getNoOFS(), localisation.getNoOfs());
		}
		{
			final SwissMunicipality swissTown = new SwissMunicipality(MockCommune.Cossonay.getNoOFSEtendu(), MockCommune.Cossonay.getNomMinuscule(), CantonAbbreviation.VD, null);
			final Destination destination = new Destination(null, swissTown, null, null);
			final Localisation localisation = AdresseRCPers.initLocalisation(destination, infraService);
			Assert.assertNotNull(localisation);
			Assert.assertEquals(LocalisationType.CANTON_VD, localisation.getType());
			Assert.assertEquals((Integer) MockCommune.Cossonay.getNoOFSEtendu(), localisation.getNoOfs());
		}
		{
			final SwissMunicipality swissTown = new SwissMunicipality(MockCommune.Bale.getNoOFSEtendu(), MockCommune.Bale.getNomMinuscule(), CantonAbbreviation.BS, null);
			final Destination destination = new Destination(null, swissTown, null, null);
			final Localisation localisation = AdresseRCPers.initLocalisation(destination, infraService);
			Assert.assertNotNull(localisation);
			Assert.assertEquals(LocalisationType.HORS_CANTON, localisation.getType());
			Assert.assertEquals((Integer) MockCommune.Bale.getNoOFSEtendu(), localisation.getNoOfs());
		}
	}
}
