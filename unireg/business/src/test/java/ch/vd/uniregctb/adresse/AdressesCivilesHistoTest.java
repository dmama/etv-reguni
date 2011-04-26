package ch.vd.uniregctb.adresse;

import java.util.List;

import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.interfaces.model.Adresse;
import ch.vd.uniregctb.interfaces.model.mock.MockAdresse;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class AdressesCivilesHistoTest {

	@Test
	public void testGetVeryFirstDateAucuneAdresse() throws Exception {

		final AdressesCivilesHisto adresses = new AdressesCivilesHisto();
		assertEquals(RegDate.getLateDate(), adresses.getVeryFirstDate());
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
		assertEquals(RegDate.getEarlyDate(), adresses.getVeryLastDate());
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

	public void testGetVeryLastDatePlusieursAdresses() throws Exception {

		final AdressesCivilesHisto adresses = new AdressesCivilesHisto();
		add(adresses.principales, RegDate.get(1990, 1, 1), RegDate.get(2007, 1, 1));
		add(adresses.courriers, RegDate.get(1985, 1, 1), RegDate.get(1999, 12, 31));
		add(adresses.courriers, RegDate.get(2000, 1, 1), RegDate.get(2008, 1, 1));
		adresses.finish(false);
		assertEquals(RegDate.get(2008, 1, 1), adresses.getVeryLastDate());
	}

	public void testGetVeryLastDateQuatreAdresses() throws Exception {

		final AdressesCivilesHisto adresses = new AdressesCivilesHisto();
		add(adresses.principales, RegDate.get(1983, 3, 2), RegDate.get(1989, 12, 31));
		add(adresses.principales, RegDate.get(1990, 1, 1), RegDate.get(2010, 3, 2));
		add(adresses.courriers, RegDate.get(1985, 1, 1), RegDate.get(1999, 12, 31));
		add(adresses.courriers, RegDate.get(2000, 1, 1), RegDate.get(2008, 1, 1));
		adresses.finish(false);
		assertEquals(RegDate.get(2010, 3, 2), adresses.getVeryLastDate());
	}

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

	public void testGetVeryLastDateAdresseDateFinNulle() throws Exception {

		final AdressesCivilesHisto adresses = new AdressesCivilesHisto();
		add(adresses.principales, RegDate.get(1983, 3, 2), RegDate.get(1989, 12, 31));
		add(adresses.principales, RegDate.get(1990, 1, 1), null);
		add(adresses.courriers, RegDate.get(1985, 1, 1), RegDate.get(1999, 12, 31));
		add(adresses.courriers, RegDate.get(2000, 1, 1), RegDate.get(2008, 1, 1));
		adresses.finish(false);
		assertNull(null, adresses.getVeryLastDate());
	}

	private void add(List<Adresse> adresses, RegDate dateDebut, RegDate dateFin) {
		MockAdresse a = new MockAdresse();
		a.setDateDebutValidite(dateDebut);
		a.setDateFinValidite(dateFin);
		adresses.add(a);
	}

}
