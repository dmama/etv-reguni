package ch.vd.unireg.tiers;

import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.adresse.AdresseCivileAdapter;
import ch.vd.unireg.adresse.AdresseGenerique;
import ch.vd.unireg.adresse.AdresseGenerique.SourceType;
import ch.vd.unireg.interfaces.common.Adresse;
import ch.vd.unireg.interfaces.infra.mock.DefaultMockInfrastructureConnector;
import ch.vd.unireg.interfaces.infra.mock.MockAdresse;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureImpl;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;
import ch.vd.unireg.type.TypeAdresseCivil;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class AdresseCivileAdapterTest {

	final ServiceInfrastructureService serviceInfra = new ServiceInfrastructureImpl(new DefaultMockInfrastructureConnector(), new MockTiersDAO());

	@Test
	public void testAdaptation() throws Exception {

		/*
		 * Adresse sur Suisse
		 */

		Adresse lausanne = new MockAdresse(TypeAdresseCivil.COURRIER, MockRue.Lausanne.AvenueDeBeaulieu, null, RegDate.get(1980, 1, 1), RegDate.get(1987, 12, 11));
		{
			final AdresseCivileAdapter adapter = new AdresseCivileAdapter(lausanne, (Tiers)null, false, serviceInfra);
			assertEquals("Lausanne", adapter.getLocalite());
			assertEquals(RegDate.get(1980, 1, 1), adapter.getDateDebut());
			assertEquals(RegDate.get(1987, 12, 11), adapter.getDateFin());
			assertEquals(ServiceInfrastructureService.noOfsSuisse, adapter.getNoOfsPays().intValue());
			assertEquals(SourceType.CIVILE_PERS,adapter.getSource().getType());
		}

		/*
		 * Adresse sur France
		 */

		MockAdresse evian = new MockAdresse();
		evian.setTypeAdresse(TypeAdresseCivil.COURRIER);
		evian.setTitre("");
		evian.setRue("Rue de l'eau plate");
		evian.setLocalite("Evian");
		evian.setNumeroPostal(null);
		evian.setNumeroPostalComplementaire(null);
		evian.setPays(MockPays.France);
		evian.setDateDebutValidite(RegDate.get(1980, 1, 1));
		evian.setDateFinValidite(RegDate.get(1987, 12, 11));
		{
			final AdresseCivileAdapter adapter = new AdresseCivileAdapter(evian, (Tiers)null, false,serviceInfra);
			assertEquals("Rue de l'eau plate", adapter.getRue());
			assertEquals("Evian", adapter.getLocalite());
			assertEquals(RegDate.get(1980, 1, 1), adapter.getDateDebut());
			assertEquals(RegDate.get(1987, 12, 11), adapter.getDateFin());
			assertEquals(MockPays.France.getNoOFS(), adapter.getNoOfsPays().intValue());
			assertEquals(SourceType.CIVILE_PERS,adapter.getSource().getType());
		}
	}

	@Test
	public void testConstructors() throws Exception {

		final Adresse lausanne = new MockAdresse(TypeAdresseCivil.COURRIER, MockRue.Lausanne.AvenueDeBeaulieu, null, RegDate.get(1980, 1, 1), RegDate.get(1987, 12, 11));

		// Constructor n°1
		{
			final AdresseCivileAdapter adapter = new AdresseCivileAdapter(lausanne, (Tiers)null, false,serviceInfra);
			assertEquals(RegDate.get(1980, 1, 1), adapter.getDateDebut());
			assertEquals(RegDate.get(1987, 12, 11), adapter.getDateFin());
			assertEquals("Lausanne", adapter.getLocalite());
			assertEquals(ServiceInfrastructureService.noOfsSuisse, adapter.getNoOfsPays().intValue());
			assertEquals(SourceType.CIVILE_PERS,adapter.getSource().getType());
			assertFalse(adapter.isDefault());
		}

		// Constructor n°2
		{
			final AdresseCivileAdapter adapter = new AdresseCivileAdapter(lausanne, new AdresseGenerique.Source(SourceType.FISCALE, null), false, serviceInfra);
			assertEquals(RegDate.get(1980, 1, 1), adapter.getDateDebut());
			assertEquals(RegDate.get(1987, 12, 11), adapter.getDateFin());
			assertEquals("Lausanne", adapter.getLocalite());
			assertEquals(MockPays.Suisse.getNoOFS(), adapter.getNoOfsPays().intValue());
			assertEquals(SourceType.FISCALE,adapter.getSource().getType());
			assertFalse(adapter.isDefault());
		}

		// Constructor n°3
		{
			final AdresseCivileAdapter adapter = new AdresseCivileAdapter(lausanne, null, RegDate.get(1985, 4, 3), RegDate.get(1987, 6, 5), false,serviceInfra);
			assertEquals(RegDate.get(1985, 4, 3), adapter.getDateDebut());
			assertEquals(RegDate.get(1987, 6, 5), adapter.getDateFin());
			assertEquals("Lausanne", adapter.getLocalite());
			assertEquals(ServiceInfrastructureService.noOfsSuisse, adapter.getNoOfsPays().intValue());
			assertEquals(SourceType.CIVILE_PERS,adapter.getSource().getType());
			assertFalse(adapter.isDefault());
		}
	}

	@Test
	public void testEquals() throws Exception {

		final Adresse lausanne = new MockAdresse(TypeAdresseCivil.COURRIER, MockRue.Lausanne.AvenueDeBeaulieu, null, RegDate.get(1980, 1, 1), RegDate.get(1987, 12, 11));

		// Cas standards
		final AdresseCivileAdapter adapter1 = new AdresseCivileAdapter(lausanne, (Tiers)null, false,serviceInfra);
		final AdresseCivileAdapter adapter2 = new AdresseCivileAdapter(lausanne, (Tiers)null, false,serviceInfra);
		assertEquals(adapter1, adapter2);

		// Date debut validite nulles
		final AdresseCivileAdapter adapter3 = new AdresseCivileAdapter(lausanne, null, null, RegDate.get(2000, 1, 1), false,serviceInfra);
		final AdresseCivileAdapter adapter4 = new AdresseCivileAdapter(lausanne, null, null, RegDate.get(2000, 1, 1), false,serviceInfra);
		assertEquals(adapter3, adapter4);

		// Date fin validite nulles
		final AdresseCivileAdapter adapter5 = new AdresseCivileAdapter(lausanne, (Tiers)null, RegDate.get(1983, 1, 1), null, false,serviceInfra);
		final AdresseCivileAdapter adapter6 = new AdresseCivileAdapter(lausanne, (Tiers)null, RegDate.get(1983, 1, 1), null, false,serviceInfra);
		assertEquals(adapter5, adapter6);

		// Date debut validite différentes
		final AdresseCivileAdapter adapter7 = new AdresseCivileAdapter(lausanne, (Tiers)null, RegDate.get(1983, 1, 1), null, false,serviceInfra);
		final AdresseCivileAdapter adapter8 = new AdresseCivileAdapter(lausanne, (Tiers)null, RegDate.get(1983, 2, 3), null, false,serviceInfra);
		assertFalse(adapter7.equals(adapter8));

		// Date fin validite différentes
		final AdresseCivileAdapter adapter9 = new AdresseCivileAdapter(lausanne, null, null, RegDate.get(2000, 1, 1), false,serviceInfra);
		final AdresseCivileAdapter adapter10 = new AdresseCivileAdapter(lausanne, null, null, RegDate.get(2000, 2, 3), false,serviceInfra);
		assertFalse(adapter9.equals(adapter10));
	}

}
