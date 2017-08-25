package ch.vd.uniregctb.declaration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.WithoutSpringTest;
import ch.vd.uniregctb.type.EtatDelaiDocumentFiscal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class DelaiDeclarationTest extends WithoutSpringTest {

	@Test
	public void testComparatorListeOrdonnee() throws Exception {
		List<DelaiDeclaration> list = new ArrayList<>();
		list.add(newDelaiDeclaration(date(2000, 1, 1)));
		list.add(newDelaiDeclaration(date(2003, 1, 1)));
		list.add(newDelaiDeclaration(date(2003, 1, 3)));
		Collections.sort(list, new DelaiDeclaration.Comparator());

		assertDelai(date(2000, 1, 1), list.get(0));
		assertDelai(date(2003, 1, 1), list.get(1));
		assertDelai(date(2003, 1, 3), list.get(2));
	}

	@Test
	public void testComparatorListeDesordonnee() throws Exception {
		List<DelaiDeclaration> list = new ArrayList<>();
		list.add(newDelaiDeclaration(date(2003, 1, 1)));
		list.add(newDelaiDeclaration(date(2003, 1, 3)));
		list.add(newDelaiDeclaration(date(2000, 1, 1)));
		Collections.sort(list, new DelaiDeclaration.Comparator());

		assertDelai(date(2000, 1, 1), list.get(0));
		assertDelai(date(2003, 1, 1), list.get(1));
		assertDelai(date(2003, 1, 3), list.get(2));
	}

	@Test
	public void testComparatorListeAvecDelaisAnnules() throws Exception {
		List<DelaiDeclaration> list = new ArrayList<>();
		list.add(newDelaiDeclaration(date(2000, 1, 1)));
		list.add(newDelaiDeclaration(date(2003, 1, 1)));
		final DelaiDeclaration d = newDelaiDeclaration(date(2003, 1, 1));
		d.setAnnule(true);
		list.add(d);
		list.add(newDelaiDeclaration(date(2003, 1, 3)));
		Collections.sort(list, new DelaiDeclaration.Comparator());

		assertDelai(date(2000, 1, 1), list.get(0));
		final DelaiDeclaration d1 = list.get(1);
		assertTrue(d1.isAnnule()); // le délai annulé avant
		assertDelai(date(2003, 1, 1), d1);
		final DelaiDeclaration d2 = list.get(2);
		assertFalse(d2.isAnnule());
		assertDelai(date(2003, 1, 1), d2);
		assertDelai(date(2003, 1, 3), list.get(3));
	}

	private static void assertDelai(RegDate date, DelaiDeclaration delaiDeclaration) {
		assertNotNull(delaiDeclaration);
		assertEquals(date, delaiDeclaration.getDelaiAccordeAu());
	}

	private static DelaiDeclaration newDelaiDeclaration(RegDate delai) {
		DelaiDeclaration d = new DelaiDeclaration();
		d.setDelaiAccordeAu(delai);
		d.setEtat(EtatDelaiDocumentFiscal.ACCORDE);
		return d;
	}
}
