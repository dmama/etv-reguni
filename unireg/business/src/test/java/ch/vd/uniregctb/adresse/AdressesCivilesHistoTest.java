package ch.vd.uniregctb.adresse;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.List;

import org.junit.Test;
import org.junit.internal.runners.JUnit4ClassRunner;
import org.junit.runner.RunWith;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.interfaces.model.Adresse;
import ch.vd.uniregctb.interfaces.model.mock.MockAdresse;

@RunWith(JUnit4ClassRunner.class)
public class AdressesCivilesHistoTest {

	@Test
	public void testGetVeryFirstDateAucuneAdresse() {

		final AdressesCivilesHisto adresses = new AdressesCivilesHisto();
		assertEquals(RegDate.getLateDate(), adresses.getVeryFirstDate());
	}

	@Test
	public void testGetVeryFirstDateUneAdresse() {

		final AdressesCivilesHisto adresses = new AdressesCivilesHisto();
		add(adresses.principales, RegDate.get(1990, 1, 1), null);
		adresses.sort();
		assertEquals(RegDate.get(1990, 1, 1), adresses.getVeryFirstDate());
	}

	@Test
	public void testGetVeryFirstDateDeuxAdresses() {

		final AdressesCivilesHisto adresses = new AdressesCivilesHisto();
		add(adresses.principales, RegDate.get(1990, 1, 1), null);
		add(adresses.courriers, RegDate.get(2000, 1, 1), null);
		adresses.sort();
		assertEquals(RegDate.get(1990, 1, 1), adresses.getVeryFirstDate());
	}

	@Test
	public void testGetVeryFirstDatePlusieursAdresses() {

		final AdressesCivilesHisto adresses = new AdressesCivilesHisto();
		add(adresses.principales, RegDate.get(1990, 1, 1), null);
		add(adresses.courriers, RegDate.get(1985, 1, 1), RegDate.get(1999, 12, 31));
		add(adresses.courriers, RegDate.get(2000, 1, 1), null);
		adresses.sort();
		assertEquals(RegDate.get(1985, 1, 1), adresses.getVeryFirstDate());
	}

	@Test
	public void testGetVeryFirstDateQuatreAdresses() {

		final AdressesCivilesHisto adresses = new AdressesCivilesHisto();
		add(adresses.principales, RegDate.get(1983, 3, 2), RegDate.get(1989, 12, 31));
		add(adresses.principales, RegDate.get(1990, 1, 1), null);
		add(adresses.courriers, RegDate.get(1985, 1, 1), RegDate.get(1999, 12, 31));
		add(adresses.courriers, RegDate.get(2000, 1, 1), null);
		adresses.sort();
		assertEquals(RegDate.get(1983, 3, 2), adresses.getVeryFirstDate());
	}

	@Test
	public void testGetVeryFirstDateAdressesSecondairesIgnorees() {

		final AdressesCivilesHisto adresses = new AdressesCivilesHisto();
		add(adresses.principales, RegDate.get(1983, 3, 2), RegDate.get(1989, 12, 31));
		add(adresses.principales, RegDate.get(1990, 1, 1), null);
		add(adresses.courriers, RegDate.get(1985, 1, 1), RegDate.get(1999, 12, 31));
		add(adresses.courriers, RegDate.get(2000, 1, 1), null);

		// les adresses secondaires sont ignorées
		add(adresses.secondaires, RegDate.get(1904, 1, 1), null);
		adresses.sort();
		assertEquals(RegDate.get(1983, 3, 2), adresses.getVeryFirstDate());

	}

	@Test
	public void testGetVeryFirstDateAdresseDateDebutNulle() {

		final AdressesCivilesHisto adresses = new AdressesCivilesHisto();
		add(adresses.principales, null, RegDate.get(1989, 12, 31));
		add(adresses.principales, RegDate.get(1990, 1, 1), null);
		add(adresses.courriers, RegDate.get(1985, 1, 1), RegDate.get(1999, 12, 31));
		add(adresses.courriers, RegDate.get(2000, 1, 1), null);
		adresses.sort();
		assertNull(null, adresses.getVeryFirstDate());
	}

	@Test
	public void testGetVeryLastDateAucuneAdresse() {

		final AdressesCivilesHisto adresses = new AdressesCivilesHisto();
		assertEquals(RegDate.getEarlyDate(), adresses.getVeryLastDate());
	}

	@Test
	public void testGetVeryLastDateUneAdresse() {

		final AdressesCivilesHisto adresses = new AdressesCivilesHisto();
		add(adresses.principales, RegDate.get(1990, 1, 1), RegDate.get(2007, 1, 1));
		adresses.sort();
		assertEquals(RegDate.get(2007, 1, 1), adresses.getVeryLastDate());
	}

	@Test
	public void testGetVeryLastDateDeuxAdresses() {

		final AdressesCivilesHisto adresses = new AdressesCivilesHisto();
		add(adresses.principales, RegDate.get(1990, 1, 1), RegDate.get(2007, 1, 1));
		add(adresses.courriers, RegDate.get(2000, 1, 1), RegDate.get(2008, 1, 1));
		adresses.sort();
		assertEquals(RegDate.get(2008, 1, 1), adresses.getVeryLastDate());
	}

	public void testGetVeryLastDatePlusieursAdresses() {

		final AdressesCivilesHisto adresses = new AdressesCivilesHisto();
		add(adresses.principales, RegDate.get(1990, 1, 1), RegDate.get(2007, 1, 1));
		add(adresses.courriers, RegDate.get(1985, 1, 1), RegDate.get(1999, 12, 31));
		add(adresses.courriers, RegDate.get(2000, 1, 1), RegDate.get(2008, 1, 1));
		adresses.sort();
		assertEquals(RegDate.get(2008, 1, 1), adresses.getVeryLastDate());
	}

	public void testGetVeryLastDateQuatreAdresses() {

		final AdressesCivilesHisto adresses = new AdressesCivilesHisto();
		add(adresses.principales, RegDate.get(1983, 3, 2), RegDate.get(1989, 12, 31));
		add(adresses.principales, RegDate.get(1990, 1, 1), RegDate.get(2010, 3, 2));
		add(adresses.courriers, RegDate.get(1985, 1, 1), RegDate.get(1999, 12, 31));
		add(adresses.courriers, RegDate.get(2000, 1, 1), RegDate.get(2008, 1, 1));
		adresses.sort();
		assertEquals(RegDate.get(2010, 3, 2), adresses.getVeryLastDate());
	}

	public void testGetVeryLastDateAdressesSecondairesIgnorees() {

		final AdressesCivilesHisto adresses = new AdressesCivilesHisto();
		add(adresses.principales, RegDate.get(1983, 3, 2), RegDate.get(1989, 12, 31));
		add(adresses.principales, RegDate.get(1990, 1, 1), RegDate.get(2010, 3, 2));
		add(adresses.courriers, RegDate.get(1985, 1, 1), RegDate.get(1999, 12, 31));
		add(adresses.courriers, RegDate.get(2000, 1, 1), RegDate.get(2008, 1, 1));

		// les adresses secondaires sont ignorées
		add(adresses.secondaires, RegDate.get(1904, 1, 1), RegDate.get(2015, 1, 1));
		adresses.sort();
		assertEquals(RegDate.get(2010, 3, 2), adresses.getVeryLastDate());

	}

	public void testGetVeryLastDateAdresseDateFinNulle() {

		final AdressesCivilesHisto adresses = new AdressesCivilesHisto();
		add(adresses.principales, RegDate.get(1983, 3, 2), RegDate.get(1989, 12, 31));
		add(adresses.principales, RegDate.get(1990, 1, 1), null);
		add(adresses.courriers, RegDate.get(1985, 1, 1), RegDate.get(1999, 12, 31));
		add(adresses.courriers, RegDate.get(2000, 1, 1), RegDate.get(2008, 1, 1));
		adresses.sort();
		assertNull(null, adresses.getVeryLastDate());
	}

	private void add(List<Adresse> adresses, RegDate dateDebut, RegDate dateFin) {
		MockAdresse a = new MockAdresse();
		a.setDateDebutValidite(dateDebut);
		a.setDateFinValidite(dateFin);
		adresses.add(a);
	}

}
