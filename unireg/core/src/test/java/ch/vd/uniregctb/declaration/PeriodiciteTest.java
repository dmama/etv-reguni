package ch.vd.uniregctb.declaration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.WithoutSpringTest;
import ch.vd.uniregctb.type.PeriodiciteDecompte;

import static org.junit.Assert.assertEquals;

public class PeriodiciteTest extends WithoutSpringTest {

	@Test
	public void testCreerHistoriquePeriodiciteNormal() throws Exception {

		Periodicite periodicite1 = new Periodicite(PeriodiciteDecompte.TRIMESTRIEL,null,date(2009,1,1),date(2009, 5,31));
		Periodicite periodicite2 = new Periodicite(PeriodiciteDecompte.MENSUEL,null,date(2009,6,1),date(2009,6,30));
		Periodicite periodicite3 = new Periodicite(PeriodiciteDecompte.TRIMESTRIEL,null,date(2009,7,1),null);

		List<Periodicite> listePeriodicite = new ArrayList<Periodicite>();
		listePeriodicite.add(periodicite1);
		listePeriodicite.add(periodicite2);
		listePeriodicite.add(periodicite3);

		DateRangeHelper.collate(listePeriodicite);
		Collections.sort(listePeriodicite, new DateRangeComparator<Periodicite>());

		assertEquals(3,listePeriodicite.size());
		assertEquals(date(2009,1,1),listePeriodicite.get(0).getDateDebut());
		assertEquals(date(2009,6,1),listePeriodicite.get(1).getDateDebut());
		assertEquals(date(2009,7,1),listePeriodicite.get(2).getDateDebut());
	}

	@Test
	public void testComblerVidePeriodicite() throws Exception {

		Periodicite periodicite1 = new Periodicite(PeriodiciteDecompte.TRIMESTRIEL,null,date(2009,1,1),date(2009, 5,31));
		Periodicite periodicite2 = new Periodicite(PeriodiciteDecompte.MENSUEL,null,date(2009,7,1),date(2009,7,31));
		Periodicite periodicite3 = new Periodicite(PeriodiciteDecompte.TRIMESTRIEL,null,date(2009,8,1),null);

		List<Periodicite> listePeriodicite = new ArrayList<Periodicite>();
		listePeriodicite.add(periodicite1);
		listePeriodicite.add(periodicite2);
		listePeriodicite.add(periodicite3);

		listePeriodicite = Periodicite.comblerVidesPeriodicites(listePeriodicite);
		DateRangeHelper.collate(listePeriodicite);
		Collections.sort(listePeriodicite, new DateRangeComparator<Periodicite>());

		assertEquals(3,listePeriodicite.size());
		assertEquals(date(2009,1,1),listePeriodicite.get(0).getDateDebut());
		assertEquals(date(2009,6,30),listePeriodicite.get(0).getDateFin());
		assertEquals(PeriodiciteDecompte.TRIMESTRIEL,listePeriodicite.get(0).getPeriodiciteDecompte());
		assertEquals(date(2009,7,1),listePeriodicite.get(1).getDateDebut());
		assertEquals(date(2009,8,1),listePeriodicite.get(2).getDateDebut());
	}

	private RegDate date(int i, int i1, int i2) {
		return RegDate.get(i,i1,i2);
	}
}
