package ch.vd.uniregctb.type;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.junit.internal.runners.JUnit4ClassRunner;
import org.junit.runner.RunWith;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.NotImplementedException;

@RunWith(JUnit4ClassRunner.class)
public class PeriodiciteDecompteTest {

	@Test
	public void testGetDebutPeriodeMensuel() {
		assertEquals(RegDate.get(2000, 1, 1), PeriodiciteDecompte.MENSUEL.getDebutPeriode(RegDate.get(2000, 1, 1)));
		assertEquals(RegDate.get(2000, 1, 1), PeriodiciteDecompte.MENSUEL.getDebutPeriode(RegDate.get(2000, 1, 13)));
		assertEquals(RegDate.get(2000, 1, 1), PeriodiciteDecompte.MENSUEL.getDebutPeriode(RegDate.get(2000, 1, 31)));

		assertEquals(RegDate.get(1999, 12, 1), PeriodiciteDecompte.MENSUEL.getDebutPeriode(RegDate.get(1999, 12, 1)));
		assertEquals(RegDate.get(1999, 12, 1), PeriodiciteDecompte.MENSUEL.getDebutPeriode(RegDate.get(1999, 12, 22)));
		assertEquals(RegDate.get(1999, 12, 1), PeriodiciteDecompte.MENSUEL.getDebutPeriode(RegDate.get(1999, 12, 31)));
	}

	@Test
	public void testGetFinPeriodeMensuel() {
		assertEquals(RegDate.get(2000, 1, 31), PeriodiciteDecompte.MENSUEL.getFinPeriode(RegDate.get(2000, 1, 1)));
		assertEquals(RegDate.get(2000, 1, 31), PeriodiciteDecompte.MENSUEL.getFinPeriode(RegDate.get(2000, 1, 13)));
		assertEquals(RegDate.get(2000, 1, 31), PeriodiciteDecompte.MENSUEL.getFinPeriode(RegDate.get(2000, 1, 31)));

		assertEquals(RegDate.get(1999, 12, 31), PeriodiciteDecompte.MENSUEL.getFinPeriode(RegDate.get(1999, 12, 1)));
		assertEquals(RegDate.get(1999, 12, 31), PeriodiciteDecompte.MENSUEL.getFinPeriode(RegDate.get(1999, 12, 11)));
		assertEquals(RegDate.get(1999, 12, 31), PeriodiciteDecompte.MENSUEL.getFinPeriode(RegDate.get(1999, 12, 13)));
	}

	@Test
	public void testGetDebutPeriodeAnnuel() {
		assertEquals(RegDate.get(2000, 1, 1), PeriodiciteDecompte.ANNUEL.getDebutPeriode(RegDate.get(2000, 1, 1)));
		assertEquals(RegDate.get(2000, 1, 1), PeriodiciteDecompte.ANNUEL.getDebutPeriode(RegDate.get(2000, 5, 23)));
		assertEquals(RegDate.get(2000, 1, 1), PeriodiciteDecompte.ANNUEL.getDebutPeriode(RegDate.get(2000, 12, 31)));

		assertEquals(RegDate.get(1999, 1, 1), PeriodiciteDecompte.ANNUEL.getDebutPeriode(RegDate.get(1999, 1, 1)));
		assertEquals(RegDate.get(1999, 1, 1), PeriodiciteDecompte.ANNUEL.getDebutPeriode(RegDate.get(1999, 5, 23)));
		assertEquals(RegDate.get(1999, 1, 1), PeriodiciteDecompte.ANNUEL.getDebutPeriode(RegDate.get(1999, 12, 31)));
	}

	@Test
	public void testGetFinPeriodeAnnuel() {
		assertEquals(RegDate.get(2000, 12, 31), PeriodiciteDecompte.ANNUEL.getFinPeriode(RegDate.get(2000, 1, 1)));
		assertEquals(RegDate.get(2000, 12, 31), PeriodiciteDecompte.ANNUEL.getFinPeriode(RegDate.get(2000, 9, 12)));
		assertEquals(RegDate.get(2000, 12, 31), PeriodiciteDecompte.ANNUEL.getFinPeriode(RegDate.get(2000, 12, 31)));

		assertEquals(RegDate.get(1999, 12, 31), PeriodiciteDecompte.ANNUEL.getFinPeriode(RegDate.get(1999, 1, 1)));
		assertEquals(RegDate.get(1999, 12, 31), PeriodiciteDecompte.ANNUEL.getFinPeriode(RegDate.get(1999, 9, 12)));
		assertEquals(RegDate.get(1999, 12, 31), PeriodiciteDecompte.ANNUEL.getFinPeriode(RegDate.get(1999, 12, 31)));
	}

	@Test
	public void testGetDebutPeriodeTrimestriel() {
		// 1er trimestre
		assertEquals(RegDate.get(2000, 1, 1), PeriodiciteDecompte.TRIMESTRIEL.getDebutPeriode(RegDate.get(2000, 1, 1)));
		assertEquals(RegDate.get(2000, 1, 1), PeriodiciteDecompte.TRIMESTRIEL.getDebutPeriode(RegDate.get(2000, 2, 23)));
		assertEquals(RegDate.get(2000, 1, 1), PeriodiciteDecompte.TRIMESTRIEL.getDebutPeriode(RegDate.get(2000, 3, 31)));

		assertEquals(RegDate.get(1999, 1, 1), PeriodiciteDecompte.TRIMESTRIEL.getDebutPeriode(RegDate.get(1999, 1, 1)));
		assertEquals(RegDate.get(1999, 1, 1), PeriodiciteDecompte.TRIMESTRIEL.getDebutPeriode(RegDate.get(1999, 2, 23)));
		assertEquals(RegDate.get(1999, 1, 1), PeriodiciteDecompte.TRIMESTRIEL.getDebutPeriode(RegDate.get(1999, 3, 31)));

		// 2ème trimestre
		assertEquals(RegDate.get(2003, 4, 1), PeriodiciteDecompte.TRIMESTRIEL.getDebutPeriode(RegDate.get(2003, 4, 1)));
		assertEquals(RegDate.get(2003, 4, 1), PeriodiciteDecompte.TRIMESTRIEL.getDebutPeriode(RegDate.get(2003, 5, 23)));
		assertEquals(RegDate.get(2003, 4, 1), PeriodiciteDecompte.TRIMESTRIEL.getDebutPeriode(RegDate.get(2003, 6, 30)));

		// 3ème trimestre
		assertEquals(RegDate.get(2003, 7, 1), PeriodiciteDecompte.TRIMESTRIEL.getDebutPeriode(RegDate.get(2003, 7, 1)));
		assertEquals(RegDate.get(2003, 7, 1), PeriodiciteDecompte.TRIMESTRIEL.getDebutPeriode(RegDate.get(2003, 8, 12)));
		assertEquals(RegDate.get(2003, 7, 1), PeriodiciteDecompte.TRIMESTRIEL.getDebutPeriode(RegDate.get(2003, 9, 30)));

		// 4ème trimestre
		assertEquals(RegDate.get(2003, 10, 1), PeriodiciteDecompte.TRIMESTRIEL.getDebutPeriode(RegDate.get(2003, 10, 1)));
		assertEquals(RegDate.get(2003, 10, 1), PeriodiciteDecompte.TRIMESTRIEL.getDebutPeriode(RegDate.get(2003, 11, 28)));
		assertEquals(RegDate.get(2003, 10, 1), PeriodiciteDecompte.TRIMESTRIEL.getDebutPeriode(RegDate.get(2003, 12, 31)));
	}

	@Test
	public void testGetFinPeriodeTrimestriel() {
		// 1er trimestre
		assertEquals(RegDate.get(2000, 3, 31), PeriodiciteDecompte.TRIMESTRIEL.getFinPeriode(RegDate.get(2000, 1, 1)));
		assertEquals(RegDate.get(2000, 3, 31), PeriodiciteDecompte.TRIMESTRIEL.getFinPeriode(RegDate.get(2000, 2, 23)));
		assertEquals(RegDate.get(2000, 3, 31), PeriodiciteDecompte.TRIMESTRIEL.getFinPeriode(RegDate.get(2000, 3, 31)));

		assertEquals(RegDate.get(1999, 3, 31), PeriodiciteDecompte.TRIMESTRIEL.getFinPeriode(RegDate.get(1999, 1, 1)));
		assertEquals(RegDate.get(1999, 3, 31), PeriodiciteDecompte.TRIMESTRIEL.getFinPeriode(RegDate.get(1999, 2, 23)));
		assertEquals(RegDate.get(1999, 3, 31), PeriodiciteDecompte.TRIMESTRIEL.getFinPeriode(RegDate.get(1999, 3, 31)));

		// 2ème trimestre
		assertEquals(RegDate.get(2003, 6, 30), PeriodiciteDecompte.TRIMESTRIEL.getFinPeriode(RegDate.get(2003, 4, 1)));
		assertEquals(RegDate.get(2003, 6, 30), PeriodiciteDecompte.TRIMESTRIEL.getFinPeriode(RegDate.get(2003, 5, 23)));
		assertEquals(RegDate.get(2003, 6, 30), PeriodiciteDecompte.TRIMESTRIEL.getFinPeriode(RegDate.get(2003, 6, 30)));

		// 3ème trimestre
		assertEquals(RegDate.get(2003, 9, 30), PeriodiciteDecompte.TRIMESTRIEL.getFinPeriode(RegDate.get(2003, 7, 1)));
		assertEquals(RegDate.get(2003, 9, 30), PeriodiciteDecompte.TRIMESTRIEL.getFinPeriode(RegDate.get(2003, 8, 12)));
		assertEquals(RegDate.get(2003, 9, 30), PeriodiciteDecompte.TRIMESTRIEL.getFinPeriode(RegDate.get(2003, 9, 30)));

		// 4ème trimestre
		assertEquals(RegDate.get(2003, 12, 31), PeriodiciteDecompte.TRIMESTRIEL.getFinPeriode(RegDate.get(2003, 10, 1)));
		assertEquals(RegDate.get(2003, 12, 31), PeriodiciteDecompte.TRIMESTRIEL.getFinPeriode(RegDate.get(2003, 11, 28)));
		assertEquals(RegDate.get(2003, 12, 31), PeriodiciteDecompte.TRIMESTRIEL.getFinPeriode(RegDate.get(2003, 12, 31)));
	}

	@Test
	public void testGetDebutPeriodeSemestriel() {
		// 1er semestre
		assertEquals(RegDate.get(2000, 1, 1), PeriodiciteDecompte.SEMESTRIEL.getDebutPeriode(RegDate.get(2000, 1, 1)));
		assertEquals(RegDate.get(2000, 1, 1), PeriodiciteDecompte.SEMESTRIEL.getDebutPeriode(RegDate.get(2000, 3, 11)));
		assertEquals(RegDate.get(2000, 1, 1), PeriodiciteDecompte.SEMESTRIEL.getDebutPeriode(RegDate.get(2000, 6, 30)));

		assertEquals(RegDate.get(1999, 1, 1), PeriodiciteDecompte.SEMESTRIEL.getDebutPeriode(RegDate.get(1999, 1, 1)));
		assertEquals(RegDate.get(1999, 1, 1), PeriodiciteDecompte.SEMESTRIEL.getDebutPeriode(RegDate.get(1999, 3, 11)));
		assertEquals(RegDate.get(1999, 1, 1), PeriodiciteDecompte.SEMESTRIEL.getDebutPeriode(RegDate.get(1999, 6, 30)));

		// 2ème semestre
		assertEquals(RegDate.get(2000, 7, 1), PeriodiciteDecompte.SEMESTRIEL.getDebutPeriode(RegDate.get(2000, 7, 1)));
		assertEquals(RegDate.get(2000, 7, 1), PeriodiciteDecompte.SEMESTRIEL.getDebutPeriode(RegDate.get(2000, 10, 11)));
		assertEquals(RegDate.get(2000, 7, 1), PeriodiciteDecompte.SEMESTRIEL.getDebutPeriode(RegDate.get(2000, 12, 31)));
	}

	@Test
	public void testGetFinPeriodeSemestriel() {
		// 1er semestre
		assertEquals(RegDate.get(2000, 6, 30), PeriodiciteDecompte.SEMESTRIEL.getFinPeriode(RegDate.get(2000, 1, 1)));
		assertEquals(RegDate.get(2000, 6, 30), PeriodiciteDecompte.SEMESTRIEL.getFinPeriode(RegDate.get(2000, 3, 11)));
		assertEquals(RegDate.get(2000, 6, 30), PeriodiciteDecompte.SEMESTRIEL.getFinPeriode(RegDate.get(2000, 6, 30)));

		assertEquals(RegDate.get(1999, 6, 30), PeriodiciteDecompte.SEMESTRIEL.getFinPeriode(RegDate.get(1999, 1, 1)));
		assertEquals(RegDate.get(1999, 6, 30), PeriodiciteDecompte.SEMESTRIEL.getFinPeriode(RegDate.get(1999, 3, 11)));
		assertEquals(RegDate.get(1999, 6, 30), PeriodiciteDecompte.SEMESTRIEL.getFinPeriode(RegDate.get(1999, 6, 30)));

		// 2ème semestre
		assertEquals(RegDate.get(2000, 12, 31), PeriodiciteDecompte.SEMESTRIEL.getFinPeriode(RegDate.get(2000, 7, 1)));
		assertEquals(RegDate.get(2000, 12, 31), PeriodiciteDecompte.SEMESTRIEL.getFinPeriode(RegDate.get(2000, 10, 11)));
		assertEquals(RegDate.get(2000, 12, 31), PeriodiciteDecompte.SEMESTRIEL.getFinPeriode(RegDate.get(2000, 12, 31)));
	}

	@Test
	public void testGetDebutPeriodePonctuel() {
		try {

			PeriodiciteDecompte.UNIQUE.getDebutPeriode(RegDate.get(2000, 1, 1));
			fail("Veuillez implémenter ce test si la périodicité PONCTUEL est implémentée");
		}
		catch (NotImplementedException e) {
			// ok pour le moment
		}
	}

	@Test
	public void testGetFinPeriodePonctuel() {
		try {

			PeriodiciteDecompte.UNIQUE.getDebutPeriode(RegDate.get(2000, 1, 1));
			fail("Veuillez implémenter ce test si la périodicité PONCTUEL est implémentée");
		}
		catch (NotImplementedException e) {
			// ok pour le moment
		}
	}

	/**
	 * Test la methode qui calcule la date de debut de periode en fonction de la date de fin de periode et de la periodicite
	 */
	public void testGetDateDebutPeriode() throws Exception {
		assertEquals(RegDate.get(2008, 01, 01), PeriodiciteDecompte.ANNUEL.getDebutPeriode(RegDate.get(2008, 12, 31)));
		assertEquals(RegDate.get(2008, 01, 01), PeriodiciteDecompte.SEMESTRIEL.getDebutPeriode(RegDate.get(2008, 06, 30)));
		assertEquals(RegDate.get(2008, 01, 01), PeriodiciteDecompte.TRIMESTRIEL.getDebutPeriode(RegDate.get(2008, 03, 31)));
		assertEquals(RegDate.get(2008, 01, 01), PeriodiciteDecompte.MENSUEL.getDebutPeriode(RegDate.get(2008, 01, 31)));
	}

	/**
	 * Test la methode qui calcule la date de fin de periode en fonction de la date de debut de periode et de la periodicite
	 */
	public void testGetDateFinPeriode() throws Exception {
        assertEquals(RegDate.get(2008,12,31), PeriodiciteDecompte.ANNUEL.getFinPeriode(RegDate.get(2008,01,01)));
		assertEquals(RegDate.get(2008,12,31), PeriodiciteDecompte.SEMESTRIEL.getFinPeriode(RegDate.get(2008,07,01)));
		assertEquals(RegDate.get(2008,12,31), PeriodiciteDecompte.TRIMESTRIEL.getFinPeriode(RegDate.get(2008,10,01)));
		assertEquals(RegDate.get(2008,12,31), PeriodiciteDecompte.MENSUEL.getFinPeriode(RegDate.get(2008,12,01)));

	}

	/**
	 * Vérifie que la date de la fin de période est toujours la veille du debut de la période suivante
	 * @throws Exception
	 */
	@Test
	public void testCoherenceFinPeriodeDebutPeriodeSuivante() throws Exception {
		final RegDate reference = RegDate.get(2008,10,15);
		for (PeriodiciteDecompte periodicite : PeriodiciteDecompte.values()) {
			if (periodicite == PeriodiciteDecompte.UNIQUE) {
				// celle-là n'est pas implémentée
			}
			else {
				final RegDate finPeriode = periodicite.getFinPeriode(reference);
				final RegDate debutPeriodeSuivante = periodicite.getDebutPeriodeSuivante(reference);
				assertEquals("Périodicité " + periodicite, finPeriode, debutPeriodeSuivante.addDays(-1));
			}
		}
	}
}
