package ch.vd.unireg.declaration;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.unireg.common.WithoutSpringTest;
import ch.vd.unireg.type.PeriodiciteDecompte;

import static org.junit.Assert.assertEquals;

public class PeriodiciteTest extends WithoutSpringTest {

	@Test
	public void testCreerHistoriquePeriodiciteNormal() throws Exception {

		Periodicite periodicite1 = new Periodicite(PeriodiciteDecompte.TRIMESTRIEL,null,date(2009,1,1),date(2009, 5,31));
		Periodicite periodicite2 = new Periodicite(PeriodiciteDecompte.MENSUEL,null,date(2009,6,1),date(2009,6,30));
		Periodicite periodicite3 = new Periodicite(PeriodiciteDecompte.TRIMESTRIEL,null,date(2009,7,1),null);

		List<Periodicite> listePeriodicite = new ArrayList<>();
		listePeriodicite.add(periodicite1);
		listePeriodicite.add(periodicite2);
		listePeriodicite.add(periodicite3);

		DateRangeHelper.collate(listePeriodicite);
		listePeriodicite.sort(new DateRangeComparator<>());

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

		List<Periodicite> listePeriodicite = new ArrayList<>();
		listePeriodicite.add(periodicite1);
		listePeriodicite.add(periodicite2);
		listePeriodicite.add(periodicite3);

		listePeriodicite = Periodicite.comblerVidesPeriodicites(listePeriodicite);
		DateRangeHelper.collate(listePeriodicite);
		listePeriodicite.sort(new DateRangeComparator<>());

		assertEquals(3,listePeriodicite.size());
		assertEquals(date(2009,1,1),listePeriodicite.get(0).getDateDebut());
		assertEquals(date(2009,6,30),listePeriodicite.get(0).getDateFin());
		assertEquals(PeriodiciteDecompte.TRIMESTRIEL,listePeriodicite.get(0).getPeriodiciteDecompte());
		assertEquals(date(2009,7,1),listePeriodicite.get(1).getDateDebut());
		assertEquals(date(2009,8,1),listePeriodicite.get(2).getDateDebut());
	}
}
