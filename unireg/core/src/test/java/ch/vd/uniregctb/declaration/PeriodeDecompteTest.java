package ch.vd.uniregctb.declaration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.common.WithoutSpringTest;
import ch.vd.uniregctb.type.PeriodeDecompte;
import ch.vd.uniregctb.type.PeriodiciteDecompte;

import static org.junit.Assert.assertEquals;

public class PeriodeDecompteTest extends WithoutSpringTest {

	@Test
	public void testgetPeriodeCourante() throws Exception {
		final int annee = 2009;

		final RegDate dateReference = date(annee, 8, 14);

		assertEquals(new DateRangeHelper.Range(date(annee, 1, 1), date(annee, 1, 31)), PeriodeDecompte.M01.getPeriodeCourante(dateReference));
		assertEquals(new DateRangeHelper.Range(date(annee, 2, 1), date(annee, 3, 1).addDays(-1)), PeriodeDecompte.M02.getPeriodeCourante(dateReference));
		assertEquals(new DateRangeHelper.Range(date(annee, 3, 1), date(annee, 3, 31)), PeriodeDecompte.M03.getPeriodeCourante(dateReference));
		assertEquals(new DateRangeHelper.Range(date(annee, 4, 1), date(annee, 4, 30)), PeriodeDecompte.M04.getPeriodeCourante(dateReference));
		assertEquals(new DateRangeHelper.Range(date(annee, 5, 1), date(annee, 5, 31)), PeriodeDecompte.M05.getPeriodeCourante(dateReference));
		assertEquals(new DateRangeHelper.Range(date(annee, 6, 1), date(annee, 6, 30)), PeriodeDecompte.M06.getPeriodeCourante(dateReference));
		assertEquals(new DateRangeHelper.Range(date(annee, 7, 1), date(annee, 7, 31)), PeriodeDecompte.M07.getPeriodeCourante(dateReference));
		assertEquals(new DateRangeHelper.Range(date(annee, 8, 1), date(annee, 8, 31)), PeriodeDecompte.M08.getPeriodeCourante(dateReference));
		assertEquals(new DateRangeHelper.Range(date(annee, 9, 1), date(annee, 9, 30)), PeriodeDecompte.M09.getPeriodeCourante(dateReference));
		assertEquals(new DateRangeHelper.Range(date(annee, 10, 1), date(annee, 10, 31)), PeriodeDecompte.M10.getPeriodeCourante(dateReference));
		assertEquals(new DateRangeHelper.Range(date(annee, 11, 1), date(annee, 11, 30)), PeriodeDecompte.M11.getPeriodeCourante(dateReference));
		assertEquals(new DateRangeHelper.Range(date(annee, 12, 1), date(annee, 12, 31)), PeriodeDecompte.M12.getPeriodeCourante(dateReference));
		assertEquals(new DateRangeHelper.Range(date(annee, 1, 1), date(annee, 3, 31)), PeriodeDecompte.T1.getPeriodeCourante(dateReference));
		assertEquals(new DateRangeHelper.Range(date(annee, 4, 1), date(annee, 6, 30)), PeriodeDecompte.T2.getPeriodeCourante(dateReference));
		assertEquals(new DateRangeHelper.Range(date(annee, 7, 1), date(annee, 9, 30)), PeriodeDecompte.T3.getPeriodeCourante(dateReference));
		assertEquals(new DateRangeHelper.Range(date(annee, 10, 1), date(annee, 12, 31)), PeriodeDecompte.T4.getPeriodeCourante(dateReference));
		assertEquals(new DateRangeHelper.Range(date(annee, 1, 1), date(annee, 6, 30)), PeriodeDecompte.S1.getPeriodeCourante(dateReference));
		assertEquals(new DateRangeHelper.Range(date(annee, 7, 1), date(annee, 12, 31)), PeriodeDecompte.S2.getPeriodeCourante(dateReference));
		assertEquals(new DateRangeHelper.Range(date(annee, 1, 1), date(annee, 12, 31)), PeriodeDecompte.A.getPeriodeCourante(dateReference));
	}

	@Test
	public void testgetPeriodeSuivante() throws Exception {
		final int annee = 2009;
		final int anneeSuivante = 2010 ;

		 RegDate dateReference = date(annee, 8, 14);
		assertEquals(new DateRangeHelper.Range(date(anneeSuivante, 1, 1), date(anneeSuivante, 1, 31)), PeriodeDecompte.M01.getPeriodeSuivante(dateReference));

		dateReference = date(annee, 1, 14);
		assertEquals(new DateRangeHelper.Range(date(annee, 2, 1), date(annee, 3, 1).addDays(-1)), PeriodeDecompte.M02.getPeriodeSuivante(dateReference));
		dateReference = date(annee,2, 14);
		assertEquals(new DateRangeHelper.Range(date(anneeSuivante, 2, 1), date(anneeSuivante, 3, 1).addDays(-1)), PeriodeDecompte.M02.getPeriodeSuivante(dateReference));

		dateReference = date(annee,2, 14);
		assertEquals(new DateRangeHelper.Range(date(annee, 3, 1), date(annee, 3, 31)), PeriodeDecompte.M03.getPeriodeSuivante(dateReference));
		dateReference = date(annee,3, 14);
		assertEquals(new DateRangeHelper.Range(date(anneeSuivante, 3, 1), date(anneeSuivante, 3, 31)), PeriodeDecompte.M03.getPeriodeSuivante(dateReference));

		dateReference = date(annee,3, 14);
		assertEquals(new DateRangeHelper.Range(date(annee, 4, 1), date(annee, 4, 30)), PeriodeDecompte.M04.getPeriodeSuivante(dateReference));
		dateReference = date(annee,4, 14);
		assertEquals(new DateRangeHelper.Range(date(anneeSuivante, 4, 1), date(anneeSuivante, 4, 30)), PeriodeDecompte.M04.getPeriodeSuivante(dateReference));

		dateReference = date(annee,4, 14);
		assertEquals(new DateRangeHelper.Range(date(annee, 5, 1), date(annee, 5, 31)), PeriodeDecompte.M05.getPeriodeSuivante(dateReference));
		dateReference = date(annee,5, 14);
		assertEquals(new DateRangeHelper.Range(date(anneeSuivante, 5, 1), date(anneeSuivante, 5, 31)), PeriodeDecompte.M05.getPeriodeSuivante(dateReference));

		dateReference = date(annee,5, 14);
		assertEquals(new DateRangeHelper.Range(date(annee, 6, 1), date(annee, 6, 30)), PeriodeDecompte.M06.getPeriodeSuivante(dateReference));
		dateReference = date(annee,6, 14);
		assertEquals(new DateRangeHelper.Range(date(anneeSuivante, 6, 1), date(anneeSuivante, 6, 30)), PeriodeDecompte.M06.getPeriodeSuivante(dateReference));

		dateReference = date(annee,6, 14);
		assertEquals(new DateRangeHelper.Range(date(annee, 7, 1), date(annee, 7, 31)), PeriodeDecompte.M07.getPeriodeSuivante(dateReference));
		dateReference = date(annee,7, 14);
		assertEquals(new DateRangeHelper.Range(date(anneeSuivante, 7, 1), date(anneeSuivante, 7, 31)), PeriodeDecompte.M07.getPeriodeSuivante(dateReference));

		dateReference = date(annee,7, 14);
		assertEquals(new DateRangeHelper.Range(date(annee, 8, 1), date(annee, 8, 31)), PeriodeDecompte.M08.getPeriodeSuivante(dateReference));
		dateReference = date(annee,8, 14);
		assertEquals(new DateRangeHelper.Range(date(anneeSuivante, 8, 1), date(anneeSuivante, 8, 31)), PeriodeDecompte.M08.getPeriodeSuivante(dateReference));

		dateReference = date(annee,8, 14);
		assertEquals(new DateRangeHelper.Range(date(annee, 9, 1), date(annee, 9, 30)), PeriodeDecompte.M09.getPeriodeSuivante(dateReference));
		dateReference = date(annee,9, 14);
		assertEquals(new DateRangeHelper.Range(date(anneeSuivante, 9, 1), date(anneeSuivante, 9, 30)), PeriodeDecompte.M09.getPeriodeSuivante(dateReference));

		dateReference = date(annee,9, 14);
		assertEquals(new DateRangeHelper.Range(date(annee, 10, 1), date(annee, 10, 31)), PeriodeDecompte.M10.getPeriodeSuivante(dateReference));
		dateReference = date(annee,10, 14);
		assertEquals(new DateRangeHelper.Range(date(anneeSuivante, 10, 1), date(anneeSuivante, 10, 31)), PeriodeDecompte.M10.getPeriodeSuivante(dateReference));

		dateReference = date(annee,10, 14);
		assertEquals(new DateRangeHelper.Range(date(annee, 11, 1), date(annee, 11, 30)), PeriodeDecompte.M11.getPeriodeSuivante(dateReference));
		dateReference = date(annee,11, 14);
		assertEquals(new DateRangeHelper.Range(date(anneeSuivante, 11, 1), date(anneeSuivante, 11, 30)), PeriodeDecompte.M11.getPeriodeSuivante(dateReference));

		dateReference = date(annee,11, 14);
		assertEquals(new DateRangeHelper.Range(date(annee, 12, 1), date(annee, 12, 31)), PeriodeDecompte.M12.getPeriodeSuivante(dateReference));
		dateReference = date(annee,12, 14);
		assertEquals(new DateRangeHelper.Range(date(anneeSuivante, 12, 1), date(anneeSuivante, 12, 31)), PeriodeDecompte.M12.getPeriodeSuivante(dateReference));
		

		dateReference = date(annee,2, 14);
		assertEquals(new DateRangeHelper.Range(date(anneeSuivante, 1, 1), date(anneeSuivante, 3, 31)), PeriodeDecompte.T1.getPeriodeSuivante(dateReference));
		dateReference = date(annee,6, 14);
		assertEquals(new DateRangeHelper.Range(date(anneeSuivante, 1, 1), date(anneeSuivante, 3, 31)), PeriodeDecompte.T1.getPeriodeSuivante(dateReference));

		dateReference = date(annee,2, 14);
		assertEquals(new DateRangeHelper.Range(date(annee, 4, 1), date(annee, 6, 30)), PeriodeDecompte.T2.getPeriodeSuivante(dateReference));
		dateReference = date(annee,8, 14);
		assertEquals(new DateRangeHelper.Range(date(anneeSuivante, 4, 1), date(anneeSuivante, 6, 30)), PeriodeDecompte.T2.getPeriodeSuivante(dateReference));

		dateReference = date(annee,2, 14);
		assertEquals(new DateRangeHelper.Range(date(annee, 7, 1), date(annee, 9, 30)), PeriodeDecompte.T3.getPeriodeSuivante(dateReference));
		dateReference = date(annee,10, 14);
		assertEquals(new DateRangeHelper.Range(date(anneeSuivante, 7, 1), date(anneeSuivante, 9, 30)), PeriodeDecompte.T3.getPeriodeSuivante(dateReference));

		dateReference = date(annee,2, 14);
		assertEquals(new DateRangeHelper.Range(date(annee, 10, 1), date(annee, 12, 31)), PeriodeDecompte.T4.getPeriodeSuivante(dateReference));
		dateReference = date(annee,11, 14);
		assertEquals(new DateRangeHelper.Range(date(anneeSuivante, 10, 1), date(anneeSuivante, 12, 31)), PeriodeDecompte.T4.getPeriodeSuivante(dateReference));

		dateReference = date(annee,1, 14);
		assertEquals(new DateRangeHelper.Range(date(anneeSuivante, 1, 1), date(anneeSuivante, 6, 30)), PeriodeDecompte.S1.getPeriodeSuivante(dateReference));
		dateReference = date(annee,11, 14);
		assertEquals(new DateRangeHelper.Range(date(anneeSuivante, 1, 1), date(anneeSuivante, 6, 30)), PeriodeDecompte.S1.getPeriodeSuivante(dateReference));

		dateReference = date(annee,5, 14);
		assertEquals(new DateRangeHelper.Range(date(annee, 7, 1), date(annee, 12, 31)), PeriodeDecompte.S2.getPeriodeSuivante(dateReference));
		dateReference = date(annee,8, 14);
		assertEquals(new DateRangeHelper.Range(date(anneeSuivante, 7, 1), date(anneeSuivante, 12, 31)), PeriodeDecompte.S2.getPeriodeSuivante(dateReference));

		dateReference = date(annee,8, 14);
		assertEquals(new DateRangeHelper.Range(date(anneeSuivante, 1, 1), date(anneeSuivante, 12, 31)), PeriodeDecompte.A.getPeriodeSuivante(dateReference));
	}




	private RegDate date(int i, int i1, int i2) {
		return RegDate.get(i, i1, i2);
	}
}
