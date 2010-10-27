package ch.vd.uniregctb.tiers;

import org.junit.Test;
import org.junit.internal.runners.JUnit4ClassRunner;
import org.junit.runner.RunWith;

import ch.vd.common.model.EnumTypeAdresse;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.adresse.AdresseCivileAdapter;
import ch.vd.uniregctb.adresse.AdresseGenerique.Source;
import ch.vd.uniregctb.interfaces.model.Adresse;
import ch.vd.uniregctb.interfaces.model.mock.MockAdresse;
import ch.vd.uniregctb.interfaces.model.mock.MockPays;
import ch.vd.uniregctb.interfaces.model.mock.MockRue;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.interfaces.service.mock.DefaultMockServiceInfrastructureService;
import ch.vd.uniregctb.interfaces.service.mock.MockServiceCivil;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

@RunWith(JUnit4ClassRunner.class)
public class AdresseCivileAdapterTest {

	final ServiceInfrastructureService serviceInfra = new DefaultMockServiceInfrastructureService();

	@Test
	public void testAdaptation() throws Exception {

		/*
		 * Adresse sur Suisse
		 */

		Adresse lausanne = MockServiceCivil.newAdresse(EnumTypeAdresse.COURRIER, MockRue.Lausanne.AvenueDeBeaulieu, null,
				RegDate.get(1980, 1, 1), RegDate.get(1987, 12, 11));
		{
			final AdresseCivileAdapter adapter = new AdresseCivileAdapter(lausanne, false,serviceInfra);
			assertEquals("Lausanne", adapter.getLocalite());
			assertEquals(RegDate.get(1980, 1, 1), adapter.getDateDebut());
			assertEquals(RegDate.get(1987, 12, 11), adapter.getDateFin());
			assertEquals(ServiceInfrastructureService.noOfsSuisse, adapter.getNoOfsPays().intValue());
			assertEquals(Source.CIVILE,adapter.getSource());
		}

		/*
		 * Adresse sur France
		 */

		MockAdresse evian = new MockAdresse();
		evian.setTypeAdresse(EnumTypeAdresse.COURRIER);
		evian.setTitre("");
		evian.setRue("Rue de l'eau plate");
		evian.setLocalite("Evian");
		evian.setNumeroPostal(null);
		evian.setNumeroPostalComplementaire(null);
		evian.setNumeroOrdrePostal(0);
		evian.setPays(MockPays.France);
		evian.setCasePostale("");
		evian.setDateDebutValidite(RegDate.get(1980, 1, 1));
		evian.setDateFinValidite(RegDate.get(1987, 12, 11));
		{
			final AdresseCivileAdapter adapter = new AdresseCivileAdapter(evian, false,serviceInfra);
			assertEquals("Rue de l'eau plate", adapter.getRue());
			assertEquals("Evian", adapter.getLocalite());
			assertEquals(RegDate.get(1980, 1, 1), adapter.getDateDebut());
			assertEquals(RegDate.get(1987, 12, 11), adapter.getDateFin());
			assertEquals(MockPays.France.getNoOFS(), adapter.getNoOfsPays().intValue());
			assertEquals(Source.CIVILE,adapter.getSource());
		}
	}

	@Test
	public void testConstructors() throws Exception {

		final Adresse lausanne = MockServiceCivil.newAdresse(EnumTypeAdresse.COURRIER, MockRue.Lausanne.AvenueDeBeaulieu, null,
				RegDate.get(1980, 1, 1), RegDate.get(1987, 12, 11));

		// Constructor n°1
		{
			final AdresseCivileAdapter adapter = new AdresseCivileAdapter(lausanne, false,serviceInfra);
			assertEquals(RegDate.get(1980, 1, 1), adapter.getDateDebut());
			assertEquals(RegDate.get(1987, 12, 11), adapter.getDateFin());
			assertEquals("Lausanne", adapter.getLocalite());
			assertEquals(ServiceInfrastructureService.noOfsSuisse, adapter.getNoOfsPays().intValue());
			assertEquals(Source.CIVILE,adapter.getSource());
			assertFalse(adapter.isDefault());
		}

		// Constructor n°2
		{
			final AdresseCivileAdapter adapter = new AdresseCivileAdapter(lausanne, Source.FISCALE, false,serviceInfra);
			assertEquals(RegDate.get(1980, 1, 1), adapter.getDateDebut());
			assertEquals(RegDate.get(1987, 12, 11), adapter.getDateFin());
			assertEquals("Lausanne", adapter.getLocalite());
			assertEquals(MockPays.Suisse.getNoOFS(), adapter.getNoOfsPays().intValue());
			assertEquals(Source.FISCALE,adapter.getSource());
			assertFalse(adapter.isDefault());
		}

		// Constructor n°3
		{
			final AdresseCivileAdapter adapter = new AdresseCivileAdapter(lausanne, RegDate.get(1985, 4, 3), RegDate.get(1987, 6, 5), false,serviceInfra);
			assertEquals(RegDate.get(1985, 4, 3), adapter.getDateDebut());
			assertEquals(RegDate.get(1987, 6, 5), adapter.getDateFin());
			assertEquals("Lausanne", adapter.getLocalite());
			assertEquals(ServiceInfrastructureService.noOfsSuisse, adapter.getNoOfsPays().intValue());
			assertEquals(Source.CIVILE,adapter.getSource());
			assertFalse(adapter.isDefault());
		}
	}

	@Test
	public void testEquals() throws Exception {

		final Adresse lausanne = MockServiceCivil.newAdresse(EnumTypeAdresse.COURRIER, MockRue.Lausanne.AvenueDeBeaulieu, null,
				RegDate.get(1980, 1, 1), RegDate.get(1987, 12, 11));

		// Cas standards
		final AdresseCivileAdapter adapter1 = new AdresseCivileAdapter(lausanne, false,serviceInfra);
		final AdresseCivileAdapter adapter2 = new AdresseCivileAdapter(lausanne, false,serviceInfra);
		assertEquals(adapter1, adapter2);

		// Date debut validite nulles
		final AdresseCivileAdapter adapter3 = new AdresseCivileAdapter(lausanne, null, RegDate.get(2000, 1, 1), false,serviceInfra);
		final AdresseCivileAdapter adapter4 = new AdresseCivileAdapter(lausanne, null, RegDate.get(2000, 1, 1), false,serviceInfra);
		assertEquals(adapter3, adapter4);

		// Date fin validite nulles
		final AdresseCivileAdapter adapter5 = new AdresseCivileAdapter(lausanne, RegDate.get(1983, 1, 1), null, false,serviceInfra);
		final AdresseCivileAdapter adapter6 = new AdresseCivileAdapter(lausanne, RegDate.get(1983, 1, 1), null, false,serviceInfra);
		assertEquals(adapter5, adapter6);

		// Date debut validite différentes
		final AdresseCivileAdapter adapter7 = new AdresseCivileAdapter(lausanne, RegDate.get(1983, 1, 1), null, false,serviceInfra);
		final AdresseCivileAdapter adapter8 = new AdresseCivileAdapter(lausanne, RegDate.get(1983, 2, 3), null, false,serviceInfra);
		assertFalse(adapter7.equals(adapter8));

		// Date fin validite différentes
		final AdresseCivileAdapter adapter9 = new AdresseCivileAdapter(lausanne, null, RegDate.get(2000, 1, 1), false,serviceInfra);
		final AdresseCivileAdapter adapter10 = new AdresseCivileAdapter(lausanne, null, RegDate.get(2000, 2, 3), false,serviceInfra);
		assertFalse(adapter9.equals(adapter10));
	}

}
