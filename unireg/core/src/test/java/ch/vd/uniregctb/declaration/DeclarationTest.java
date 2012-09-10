package ch.vd.uniregctb.declaration;

import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.WithoutSpringTest;
import ch.vd.uniregctb.type.TypeEtatDeclaration;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class DeclarationTest extends WithoutSpringTest {

	@Test
	public void testIsValid() {

		final Declaration declaration = new DeclarationImpotOrdinaire();
		declaration.setDateDebut(RegDate.get(2000, 1, 1));
		declaration.setDateFin(RegDate.get(2009, 12, 31));

		declaration.setAnnule(false);
		assertTrue(declaration.isValidAt(RegDate.get(2004, 1, 1)));
		assertFalse(declaration.isValidAt(RegDate.get(1990, 1, 1)));
		assertFalse(declaration.isValidAt(RegDate.get(2060, 1, 1)));

		declaration.setAnnule(true);
		assertFalse(declaration.isValidAt(RegDate.get(2004, 1, 1)));
		assertFalse(declaration.isValidAt(RegDate.get(1990, 1, 1)));
		assertFalse(declaration.isValidAt(RegDate.get(2060, 1, 1)));
	}

	@Test
	public void testGetDernierEtatCasNormal() {

		DeclarationImpotOrdinaire declaration = new DeclarationImpotOrdinaire();
		declaration.addEtat(new EtatDeclarationEmise(date(2000, 1, 1)));
		declaration.addEtat(new EtatDeclarationEchue(date(2000, 3, 3)));
		declaration.addEtat(new EtatDeclarationRetournee(date(2000, 4, 4), "TEST"));
		declaration.addEtat(new EtatDeclarationSommee(date(2000, 2, 2),date(2000, 2, 2)));

		assertEtat(date(2000, 4, 4), TypeEtatDeclaration.RETOURNEE, declaration.getDernierEtat());
	}

	@Test
	public void testGetDernierEtatAvecEtatsSommeeEtRetourneeLeMemeJour() {

		DeclarationImpotOrdinaire declaration = new DeclarationImpotOrdinaire();
		declaration.addEtat(new EtatDeclarationEmise(date(2000, 1, 1)));
		declaration.addEtat(new EtatDeclarationRetournee(date(2000, 2, 2), "TEST"));
		declaration.addEtat(new EtatDeclarationSommee(date(2000, 2, 2),date(2000, 2, 2)));

		assertEtat(date(2000, 2, 2), TypeEtatDeclaration.RETOURNEE, declaration.getDernierEtat());
	}

	@Test
	public void testGetDernierEtatAvecEtatAnnule() {

		DeclarationImpotOrdinaire declaration = new DeclarationImpotOrdinaire();
		declaration.addEtat(new EtatDeclarationEmise(date(2000, 1, 1)));
		declaration.addEtat(new EtatDeclarationEchue(date(2000, 3, 3)));
		final EtatDeclaration etatRetourne = new EtatDeclarationRetournee(date(2000, 4, 4), "TEST");
		etatRetourne.setAnnule(true);
		declaration.addEtat(etatRetourne);
		declaration.addEtat(new EtatDeclarationSommee(date(2000, 2, 2),date(2000, 2, 2)));

		assertEtat(date(2000, 3, 3), TypeEtatDeclaration.ECHUE, declaration.getDernierEtat());
	}

	@Test
	public void testGetDernierEtatOfTypeCasNormal() {

		DeclarationImpotOrdinaire declaration = new DeclarationImpotOrdinaire();
		declaration.addEtat(new EtatDeclarationEmise(date(2000, 1, 1)));
		declaration.addEtat(new EtatDeclarationEchue(date(2000, 3, 3)));
		declaration.addEtat(new EtatDeclarationRetournee(date(2000, 4, 4), "TEST"));
		declaration.addEtat(new EtatDeclarationSommee(date(2000, 2, 2),date(2000, 2, 2)));

		assertEtat(date(2000, 1, 1), TypeEtatDeclaration.EMISE, declaration.getDernierEtatOfType(TypeEtatDeclaration.EMISE));
		assertEtat(date(2000, 2, 2), TypeEtatDeclaration.SOMMEE, declaration.getDernierEtatOfType(TypeEtatDeclaration.SOMMEE));
		assertEtat(date(2000, 3, 3), TypeEtatDeclaration.ECHUE, declaration.getDernierEtatOfType(TypeEtatDeclaration.ECHUE));
		assertEtat(date(2000, 4, 4), TypeEtatDeclaration.RETOURNEE, declaration.getDernierEtatOfType(TypeEtatDeclaration.RETOURNEE));
	}

	@Test
	public void testGetDernierEtatOfTypeAvecEtatsSommeeEtRetourneeLeMemeJour() {

		DeclarationImpotOrdinaire declaration = new DeclarationImpotOrdinaire();
		declaration.addEtat(new EtatDeclarationEmise(date(2000, 1, 1)));
		declaration.addEtat(new EtatDeclarationRetournee(date(2000, 2, 2), "TEST"));
		declaration.addEtat(new EtatDeclarationSommee(date(2000, 2, 2),date(2000, 2, 2)));

		assertEtat(date(2000, 1, 1), TypeEtatDeclaration.EMISE, declaration.getDernierEtatOfType(TypeEtatDeclaration.EMISE));
		assertEtat(date(2000, 2, 2), TypeEtatDeclaration.SOMMEE, declaration.getDernierEtatOfType(TypeEtatDeclaration.SOMMEE));
		assertEtat(date(2000, 2, 2), TypeEtatDeclaration.RETOURNEE, declaration.getDernierEtatOfType(TypeEtatDeclaration.RETOURNEE));
		assertNull(declaration.getDernierEtatOfType(TypeEtatDeclaration.ECHUE));
	}

	@Test
	public void testGetDernierEtatOfTypeAvecEtatAnnule() {

		DeclarationImpotOrdinaire declaration = new DeclarationImpotOrdinaire();
		declaration.addEtat(new EtatDeclarationEmise(date(2000, 1, 1)));
		declaration.addEtat(new EtatDeclarationEchue(date(2000, 3, 3)));
		final EtatDeclaration etatRetourne = new EtatDeclarationRetournee(date(2000, 4, 4), "TEST");
		etatRetourne.setAnnule(true);
		declaration.addEtat(etatRetourne);
		declaration.addEtat(new EtatDeclarationSommee(date(2000, 2, 2),date(2000, 2, 2)));

		assertEtat(date(2000, 1, 1), TypeEtatDeclaration.EMISE, declaration.getDernierEtatOfType(TypeEtatDeclaration.EMISE));
		assertEtat(date(2000, 2, 2), TypeEtatDeclaration.SOMMEE, declaration.getDernierEtatOfType(TypeEtatDeclaration.SOMMEE));
		assertEtat(date(2000, 3, 3), TypeEtatDeclaration.ECHUE, declaration.getDernierEtatOfType(TypeEtatDeclaration.ECHUE));
		assertNull(declaration.getDernierEtatOfType(TypeEtatDeclaration.RETOURNEE));
	}

	@Test
	public void testGetDernierEtatOfTypeAvecPlusieursMemesEtats() {

		// cas DI avec états incohérents
		DeclarationImpotOrdinaire declaration = new DeclarationImpotOrdinaire();
		declaration.addEtat(new EtatDeclarationEmise(date(2000, 1, 1)));
		declaration.addEtat(new EtatDeclarationSommee(date(2000, 2, 2),date(2000, 2, 2)));
		declaration.addEtat(new EtatDeclarationEchue(date(2000, 3, 3)));
		declaration.addEtat(new EtatDeclarationRetournee(date(2000, 4, 4), "TEST"));
		declaration.addEtat(new EtatDeclarationEmise(date(2001, 1, 1)));
		declaration.addEtat(new EtatDeclarationSommee(date(2001, 2, 2),date(2000, 2, 2)));

		assertEtat(date(2001, 1, 1), TypeEtatDeclaration.EMISE, declaration.getDernierEtatOfType(TypeEtatDeclaration.EMISE));
		assertEtat(date(2001, 2, 2), TypeEtatDeclaration.SOMMEE, declaration.getDernierEtatOfType(TypeEtatDeclaration.SOMMEE));
		assertEtat(date(2000, 3, 3), TypeEtatDeclaration.ECHUE, declaration.getDernierEtatOfType(TypeEtatDeclaration.ECHUE));
		assertEtat(date(2000, 4, 4), TypeEtatDeclaration.RETOURNEE, declaration.getDernierEtatOfType(TypeEtatDeclaration.RETOURNEE));
	}


	private static void assertEtat(RegDate date, TypeEtatDeclaration type, EtatDeclaration etat) {
		assertNotNull(etat);
		assertEquals(date, etat.getDateObtention());
		assertEquals(type, etat.getEtat());
	}
}
