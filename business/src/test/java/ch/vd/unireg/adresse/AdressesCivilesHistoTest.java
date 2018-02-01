package ch.vd.unireg.adresse;

import java.util.List;

import org.jetbrains.annotations.Nullable;
import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.interfaces.common.Adresse;
import ch.vd.unireg.interfaces.infra.mock.MockAdresse;
import ch.vd.unireg.interfaces.model.AdressesCivilesHisto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class AdressesCivilesHistoTest {

	@Test
	public void testGetVeryFirstDateAucuneAdresse() throws Exception {

		final AdressesCivilesHisto adresses = new AdressesCivilesHisto();
		assertEquals(RegDateHelper.getLateDate(), adresses.getVeryFirstDate());
	}

	@Test
	public void testGetVeryFirstDateUneAdresse() throws Exception {

		final AdressesCivilesHisto adresses = new AdressesCivilesHisto();
		add(adresses.principales, RegDate.get(1990, 1, 1), null);
		adresses.finish(false);
		assertEquals(RegDate.get(1990, 1, 1), adresses.getVeryFirstDate());
	}

	@Test
	public void testGetVeryFirstDateDeuxAdresses() throws Exception {

		final AdressesCivilesHisto adresses = new AdressesCivilesHisto();
		add(adresses.principales, RegDate.get(1990, 1, 1), null);
		add(adresses.courriers, RegDate.get(2000, 1, 1), null);
		adresses.finish(false);
		assertEquals(RegDate.get(1990, 1, 1), adresses.getVeryFirstDate());
	}

	@Test
	public void testGetVeryFirstDatePlusieursAdresses() throws Exception {

		final AdressesCivilesHisto adresses = new AdressesCivilesHisto();
		add(adresses.principales, RegDate.get(1990, 1, 1), null);
		add(adresses.courriers, RegDate.get(1985, 1, 1), RegDate.get(1999, 12, 31));
		add(adresses.courriers, RegDate.get(2000, 1, 1), null);
		adresses.finish(false);
		assertEquals(RegDate.get(1985, 1, 1), adresses.getVeryFirstDate());
	}

	@Test
	public void testGetVeryFirstDateQuatreAdresses() throws Exception {

		final AdressesCivilesHisto adresses = new AdressesCivilesHisto();
		add(adresses.principales, RegDate.get(1983, 3, 2), RegDate.get(1989, 12, 31));
		add(adresses.principales, RegDate.get(1990, 1, 1), null);
		add(adresses.courriers, RegDate.get(1985, 1, 1), RegDate.get(1999, 12, 31));
		add(adresses.courriers, RegDate.get(2000, 1, 1), null);
		adresses.finish(false);
		assertEquals(RegDate.get(1983, 3, 2), adresses.getVeryFirstDate());
	}

	@Test
	public void testGetVeryFirstDateAdressesSecondairesIgnorees() throws Exception {

		final AdressesCivilesHisto adresses = new AdressesCivilesHisto();
		add(adresses.principales, RegDate.get(1983, 3, 2), RegDate.get(1989, 12, 31));
		add(adresses.principales, RegDate.get(1990, 1, 1), null);
		add(adresses.courriers, RegDate.get(1985, 1, 1), RegDate.get(1999, 12, 31));
		add(adresses.courriers, RegDate.get(2000, 1, 1), null);

		// les adresses secondaires sont ignorées
		add(adresses.secondaires, RegDate.get(1904, 1, 1), null);
		adresses.finish(false);
		assertEquals(RegDate.get(1983, 3, 2), adresses.getVeryFirstDate());

	}

	@Test
	public void testGetVeryFirstDateAdresseDateDebutNulle() throws Exception {

		final AdressesCivilesHisto adresses = new AdressesCivilesHisto();
		add(adresses.principales, null, RegDate.get(1989, 12, 31));
		add(adresses.principales, RegDate.get(1990, 1, 1), null);
		add(adresses.courriers, RegDate.get(1985, 1, 1), RegDate.get(1999, 12, 31));
		add(adresses.courriers, RegDate.get(2000, 1, 1), null);
		adresses.finish(false);
		assertNull(null, adresses.getVeryFirstDate());
	}

	@Test
	public void testGetVeryLastDateAucuneAdresse() throws Exception {

		final AdressesCivilesHisto adresses = new AdressesCivilesHisto();
		assertEquals(RegDateHelper.getEarlyDate(), adresses.getVeryLastDate());
	}

	@Test
	public void testGetVeryLastDateUneAdresse() throws Exception {

		final AdressesCivilesHisto adresses = new AdressesCivilesHisto();
		add(adresses.principales, RegDate.get(1990, 1, 1), RegDate.get(2007, 1, 1));
		adresses.finish(false);
		assertEquals(RegDate.get(2007, 1, 1), adresses.getVeryLastDate());
	}

	@Test
	public void testGetVeryLastDateDeuxAdresses() throws Exception {

		final AdressesCivilesHisto adresses = new AdressesCivilesHisto();
		add(adresses.principales, RegDate.get(1990, 1, 1), RegDate.get(2007, 1, 1));
		add(adresses.courriers, RegDate.get(2000, 1, 1), RegDate.get(2008, 1, 1));
		adresses.finish(false);
		assertEquals(RegDate.get(2008, 1, 1), adresses.getVeryLastDate());
	}

	@Test
	public void testGetVeryLastDatePlusieursAdresses() throws Exception {

		final AdressesCivilesHisto adresses = new AdressesCivilesHisto();
		add(adresses.principales, RegDate.get(1990, 1, 1), RegDate.get(2007, 1, 1));
		add(adresses.courriers, RegDate.get(1985, 1, 1), RegDate.get(1999, 12, 31));
		add(adresses.courriers, RegDate.get(2000, 1, 1), RegDate.get(2008, 1, 1));
		adresses.finish(false);
		assertEquals(RegDate.get(2008, 1, 1), adresses.getVeryLastDate());
	}

	@Test
	public void testGetVeryLastDateQuatreAdresses() throws Exception {

		final AdressesCivilesHisto adresses = new AdressesCivilesHisto();
		add(adresses.principales, RegDate.get(1983, 3, 2), RegDate.get(1989, 12, 31));
		add(adresses.principales, RegDate.get(1990, 1, 1), RegDate.get(2010, 3, 2));
		add(adresses.courriers, RegDate.get(1985, 1, 1), RegDate.get(1999, 12, 31));
		add(adresses.courriers, RegDate.get(2000, 1, 1), RegDate.get(2008, 1, 1));
		adresses.finish(false);
		assertEquals(RegDate.get(2010, 3, 2), adresses.getVeryLastDate());
	}

	@Test
	public void testGetVeryLastDateAdressesSecondairesIgnorees() throws Exception {

		final AdressesCivilesHisto adresses = new AdressesCivilesHisto();
		add(adresses.principales, RegDate.get(1983, 3, 2), RegDate.get(1989, 12, 31));
		add(adresses.principales, RegDate.get(1990, 1, 1), RegDate.get(2010, 3, 2));
		add(adresses.courriers, RegDate.get(1985, 1, 1), RegDate.get(1999, 12, 31));
		add(adresses.courriers, RegDate.get(2000, 1, 1), RegDate.get(2008, 1, 1));

		// les adresses secondaires sont ignorées
		add(adresses.secondaires, RegDate.get(1904, 1, 1), RegDate.get(2015, 1, 1));
		adresses.finish(false);
		assertEquals(RegDate.get(2010, 3, 2), adresses.getVeryLastDate());

	}

	@Test
	public void testGetVeryLastDateAdresseDateFinNulle() throws Exception {

		final AdressesCivilesHisto adresses = new AdressesCivilesHisto();
		add(adresses.principales, RegDate.get(1983, 3, 2), RegDate.get(1989, 12, 31));
		add(adresses.principales, RegDate.get(1990, 1, 1), null);
		add(adresses.courriers, RegDate.get(1985, 1, 1), RegDate.get(1999, 12, 31));
		add(adresses.courriers, RegDate.get(2000, 1, 1), RegDate.get(2008, 1, 1));
		adresses.finish(false);
		assertNull(null, adresses.getVeryLastDate());
	}


	/**
	 * [SIFISC-6942] Vérifie que les adresses secondaires peuvent se chevaucher sans problème de validation.
	 */
	@Test
	public void testChevauchementAdressesSecondaires() throws Exception {
		final AdressesCivilesHisto adresses = new AdressesCivilesHisto();
		add(adresses.secondaires, RegDate.get(1985, 1, 1), null);
		add(adresses.secondaires, RegDate.get(2000, 1, 1), RegDate.get(2008, 1, 1));
		adresses.finish(true);
	}

	private void add(List<Adresse> adresses, @Nullable RegDate dateDebut, @Nullable RegDate dateFin) {
		MockAdresse a = new MockAdresse();
		a.setDateDebutValidite(dateDebut);
		a.setDateFinValidite(dateFin);
		adresses.add(a);
	}

}
