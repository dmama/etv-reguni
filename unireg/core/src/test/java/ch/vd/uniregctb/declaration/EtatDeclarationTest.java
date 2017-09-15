package ch.vd.uniregctb.declaration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.WithoutSpringTest;
import ch.vd.uniregctb.type.TypeEtatDocumentFiscal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class EtatDeclarationTest extends WithoutSpringTest {

	@Test
	public void testComparatorCasNormal() {

		List<EtatDeclaration> list = new ArrayList<>();
		list.add(new EtatDeclarationEmise(date(2000, 1, 1)));
		list.add(new EtatDeclarationEchue(date(2000, 3, 3)));
		list.add(new EtatDeclarationRetournee(date(2000, 4, 4), "TEST"));
		list.add(new EtatDeclarationSommee(date(2000, 2, 2),date(2000, 2, 2), null));

		Collections.sort(list, new EtatDeclaration.Comparator());

		assertEquals(4, list.size());
		assertEtat(date(2000, 1, 1), TypeEtatDocumentFiscal.EMISE, list.get(0));
		assertEtat(date(2000, 2, 2), TypeEtatDocumentFiscal.SOMMEE, list.get(1));
		assertEtat(date(2000, 3, 3), TypeEtatDocumentFiscal.ECHUE, list.get(2));
		assertEtat(date(2000, 4, 4), TypeEtatDocumentFiscal.RETOURNEE, list.get(3));
	}

	@Test
	public void testComparatorAvecEtatsSommeeEtRetourneeLeMemeJour() {

		List<EtatDeclaration> list = new ArrayList<>();
		list.add(new EtatDeclarationEmise(date(2000, 1, 1)));
		list.add(new EtatDeclarationRetournee(date(2000, 2, 2), "TEST"));
		list.add(new EtatDeclarationSommee(date(2000, 2, 2),date(2000, 2, 2), null));

		Collections.sort(list, new EtatDeclaration.Comparator());

		assertEquals(3, list.size());
		assertEtat(date(2000, 1, 1), TypeEtatDocumentFiscal.EMISE, list.get(0));
		assertEtat(date(2000, 2, 2), TypeEtatDocumentFiscal.SOMMEE, list.get(1));
		assertEtat(date(2000, 2, 2), TypeEtatDocumentFiscal.RETOURNEE, list.get(2));
	}

	private static void assertEtat(RegDate date, TypeEtatDocumentFiscal type, EtatDeclaration etat) {
		assertNotNull(etat);
		assertEquals(date, etat.getDateObtention());
		assertEquals(type, etat.getEtat());
	}
}
