package ch.vd.uniregctb.type;

import org.junit.Test;

import ch.vd.registre.base.date.RegDate;

import static org.junit.Assert.assertEquals;

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
	public void testGetDebutPeriodeUnique() {
		assertEquals(RegDate.get(2000, 1, 1), PeriodiciteDecompte.UNIQUE.getDebutPeriode(RegDate.get(2000, 1, 1)));
		assertEquals(RegDate.get(2000, 1, 1), PeriodiciteDecompte.UNIQUE.getDebutPeriode(RegDate.get(2000, 1, 15)));
		assertEquals(RegDate.get(2000, 1, 1), PeriodiciteDecompte.UNIQUE.getDebutPeriode(RegDate.get(2000, 2, 1)));
		assertEquals(RegDate.get(2000, 1, 1), PeriodiciteDecompte.UNIQUE.getDebutPeriode(RegDate.get(2000, 4, 1)));
		assertEquals(RegDate.get(2000, 1, 1), PeriodiciteDecompte.UNIQUE.getDebutPeriode(RegDate.get(2000, 5, 1)));
		assertEquals(RegDate.get(2000, 1, 1), PeriodiciteDecompte.UNIQUE.getDebutPeriode(RegDate.get(2000, 7, 1)));
		assertEquals(RegDate.get(2000, 1, 1), PeriodiciteDecompte.UNIQUE.getDebutPeriode(RegDate.get(2000, 10, 1)));
		assertEquals(RegDate.get(2000, 1, 1), PeriodiciteDecompte.UNIQUE.getDebutPeriode(RegDate.get(2000, 12, 31)));
		assertEquals(RegDate.get(2001, 1, 1), PeriodiciteDecompte.UNIQUE.getDebutPeriode(RegDate.get(2001, 1, 1)));
	}

	@Test
	public void testGetFinPeriodeUnique() {
		assertEquals(RegDate.get(2000, 12, 31), PeriodiciteDecompte.UNIQUE.getFinPeriode(RegDate.get(2000, 1, 1)));
		assertEquals(RegDate.get(2000, 12, 31), PeriodiciteDecompte.UNIQUE.getFinPeriode(RegDate.get(2000, 1, 15)));
		assertEquals(RegDate.get(2000, 12, 31), PeriodiciteDecompte.UNIQUE.getFinPeriode(RegDate.get(2000, 2, 1)));
		assertEquals(RegDate.get(2000, 12, 31), PeriodiciteDecompte.UNIQUE.getFinPeriode(RegDate.get(2000, 4, 1)));
		assertEquals(RegDate.get(2000, 12, 31), PeriodiciteDecompte.UNIQUE.getFinPeriode(RegDate.get(2000, 5, 1)));
		assertEquals(RegDate.get(2000, 12, 31), PeriodiciteDecompte.UNIQUE.getFinPeriode(RegDate.get(2000, 7, 1)));
		assertEquals(RegDate.get(2000, 12, 31), PeriodiciteDecompte.UNIQUE.getFinPeriode(RegDate.get(2000, 10, 1)));
		assertEquals(RegDate.get(2000, 12, 31), PeriodiciteDecompte.UNIQUE.getFinPeriode(RegDate.get(2000, 12, 31)));
		assertEquals(RegDate.get(2001, 12, 31), PeriodiciteDecompte.UNIQUE.getFinPeriode(RegDate.get(2001, 1, 1)));
	}

	/**
	 * Test la methode qui calcule la date de debut de periode en fonction de la date de fin de periode et de la periodicite
	 */
	@Test
	public void testGetDateDebutPeriode() throws Exception {
		assertEquals(RegDate.get(2008, 1, 1), PeriodiciteDecompte.ANNUEL.getDebutPeriode(RegDate.get(2008, 12, 31)));
		assertEquals(RegDate.get(2008, 1, 1), PeriodiciteDecompte.SEMESTRIEL.getDebutPeriode(RegDate.get(2008, 6, 30)));
		assertEquals(RegDate.get(2008, 1, 1), PeriodiciteDecompte.TRIMESTRIEL.getDebutPeriode(RegDate.get(2008, 3, 31)));
		assertEquals(RegDate.get(2008, 1, 1), PeriodiciteDecompte.MENSUEL.getDebutPeriode(RegDate.get(2008, 1, 31)));
	}

	/**
	 * Test la methode qui calcule la date de fin de periode en fonction de la date de debut de periode et de la periodicite
	 */
	@Test
	public void testGetDateFinPeriode() throws Exception {
        assertEquals(RegDate.get(2008,12,31), PeriodiciteDecompte.ANNUEL.getFinPeriode(RegDate.get(2008, 1, 1)));
		assertEquals(RegDate.get(2008,12,31), PeriodiciteDecompte.SEMESTRIEL.getFinPeriode(RegDate.get(2008, 7, 1)));
		assertEquals(RegDate.get(2008,12,31), PeriodiciteDecompte.TRIMESTRIEL.getFinPeriode(RegDate.get(2008,10, 1)));
		assertEquals(RegDate.get(2008,12,31), PeriodiciteDecompte.MENSUEL.getFinPeriode(RegDate.get(2008,12, 1)));

	}

	/**
	 * Vérifie que la date de la fin de période est toujours la veille du debut de la période suivante
	 */
	@Test
	public void testCoherenceFinPeriodeDebutPeriodeSuivante() throws Exception {
		final RegDate reference = RegDate.get(2008,10,15);
		for (PeriodiciteDecompte periodicite : PeriodiciteDecompte.values()) {
			final RegDate finPeriode = periodicite.getFinPeriode(reference);
			final RegDate debutPeriodeSuivante = periodicite.getDebutPeriodeSuivante(reference);
			assertEquals("Périodicité " + periodicite, finPeriode, debutPeriodeSuivante.addDays(-1));
		}
	}
}
