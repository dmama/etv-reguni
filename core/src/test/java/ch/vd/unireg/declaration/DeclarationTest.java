package ch.vd.unireg.declaration;

import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.WithoutSpringTest;
import ch.vd.unireg.type.TypeEtatDocumentFiscal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class DeclarationTest extends WithoutSpringTest {

	@Test
	public void testIsValid() {

		final Declaration declaration = new DeclarationImpotOrdinairePP();
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

		DeclarationImpotOrdinaire declaration = new DeclarationImpotOrdinairePP();
		declaration.addEtat(new EtatDeclarationEmise(date(2000, 1, 1)));
		declaration.addEtat(new EtatDeclarationEchue(date(2000, 3, 3)));
		declaration.addEtat(new EtatDeclarationRetournee(date(2000, 4, 4), "TEST"));
		declaration.addEtat(new EtatDeclarationSommee(date(2000, 2, 2),date(2000, 2, 2), null));

		assertEtat(date(2000, 4, 4), TypeEtatDocumentFiscal.RETOURNE, declaration.getDernierEtatDeclaration());
	}

	@Test
	public void testGetDernierEtatAvecEtatSuspendue() {
		final DeclarationImpotOrdinaire declaration = new DeclarationImpotOrdinairePP();
		declaration.addEtat(new EtatDeclarationEmise(date(2000, 1, 1)));
		declaration.addEtat(new EtatDeclarationEchue(date(2000, 3, 3)));
		declaration.addEtat(new EtatDeclarationSommee(date(2000, 2, 2),date(2000, 2, 2), null));
		declaration.addEtat(new EtatDeclarationSuspendue(date(2000, 1, 6)));

		assertEtat(date(2000, 1, 6), TypeEtatDocumentFiscal.SUSPENDU, declaration.getDernierEtatDeclaration());
	}

	@Test
	public void testGetDernierEtatAvecEtatSuspendueEtRetournee() {
		final DeclarationImpotOrdinaire declaration = new DeclarationImpotOrdinairePP();
		declaration.addEtat(new EtatDeclarationEmise(date(2000, 1, 1)));
		declaration.addEtat(new EtatDeclarationEchue(date(2000, 3, 3)));
		declaration.addEtat(new EtatDeclarationSommee(date(2000, 2, 2),date(2000, 2, 2), null));
		declaration.addEtat(new EtatDeclarationSuspendue(date(2000, 1, 6)));
		declaration.addEtat(new EtatDeclarationRetournee(date(2000, 4, 4), "TEST"));

		assertEtat(date(2000, 4, 4), TypeEtatDocumentFiscal.RETOURNE, declaration.getDernierEtatDeclaration());
	}

	@Test
	public void testGetDernierEtatAvecEtatsSommeeEtRetourneeLeMemeJour() {

		DeclarationImpotOrdinaire declaration = new DeclarationImpotOrdinairePP();
		declaration.addEtat(new EtatDeclarationEmise(date(2000, 1, 1)));
		declaration.addEtat(new EtatDeclarationRetournee(date(2000, 2, 2), "TEST"));
		declaration.addEtat(new EtatDeclarationSommee(date(2000, 2, 2),date(2000, 2, 2), null));

		assertEtat(date(2000, 2, 2), TypeEtatDocumentFiscal.RETOURNE, declaration.getDernierEtatDeclaration());
	}

	@Test
	public void testGetDernierEtatAvecEtatAnnule() {

		DeclarationImpotOrdinaire declaration = new DeclarationImpotOrdinairePP();
		declaration.addEtat(new EtatDeclarationEmise(date(2000, 1, 1)));
		declaration.addEtat(new EtatDeclarationEchue(date(2000, 3, 3)));
		final EtatDeclaration etatRetourne = new EtatDeclarationRetournee(date(2000, 4, 4), "TEST");
		etatRetourne.setAnnule(true);
		declaration.addEtat(etatRetourne);
		declaration.addEtat(new EtatDeclarationSommee(date(2000, 2, 2),date(2000, 2, 2), null));

		assertEtat(date(2000, 3, 3), TypeEtatDocumentFiscal.ECHU, declaration.getDernierEtatDeclaration());
	}

	@Test
	public void testGetDernierEtatOfTypeCasNormal() {

		DeclarationImpotOrdinaire declaration = new DeclarationImpotOrdinairePP();
		declaration.addEtat(new EtatDeclarationEmise(date(2000, 1, 1)));
		declaration.addEtat(new EtatDeclarationEchue(date(2000, 3, 3)));
		declaration.addEtat(new EtatDeclarationRetournee(date(2000, 4, 4), "TEST"));
		declaration.addEtat(new EtatDeclarationSommee(date(2000, 2, 2),date(2000, 2, 2), null));

		assertEtat(date(2000, 1, 1), TypeEtatDocumentFiscal.EMIS, declaration.getDernierEtatDeclarationOfType(TypeEtatDocumentFiscal.EMIS));
		assertEtat(date(2000, 2, 2), TypeEtatDocumentFiscal.SOMME, declaration.getDernierEtatDeclarationOfType(TypeEtatDocumentFiscal.SOMME));
		assertEtat(date(2000, 3, 3), TypeEtatDocumentFiscal.ECHU, declaration.getDernierEtatDeclarationOfType(TypeEtatDocumentFiscal.ECHU));
		assertEtat(date(2000, 4, 4), TypeEtatDocumentFiscal.RETOURNE, declaration.getDernierEtatDeclarationOfType(TypeEtatDocumentFiscal.RETOURNE));
	}

	@Test
	public void testGetDernierEtatOfTypeAvecEtatsSommeeEtRetourneeLeMemeJour() {

		DeclarationImpotOrdinaire declaration = new DeclarationImpotOrdinairePP();
		declaration.addEtat(new EtatDeclarationEmise(date(2000, 1, 1)));
		declaration.addEtat(new EtatDeclarationRetournee(date(2000, 2, 2), "TEST"));
		declaration.addEtat(new EtatDeclarationSommee(date(2000, 2, 2),date(2000, 2, 2), null));

		assertEtat(date(2000, 1, 1), TypeEtatDocumentFiscal.EMIS, declaration.getDernierEtatDeclarationOfType(TypeEtatDocumentFiscal.EMIS));
		assertEtat(date(2000, 2, 2), TypeEtatDocumentFiscal.SOMME, declaration.getDernierEtatDeclarationOfType(TypeEtatDocumentFiscal.SOMME));
		assertEtat(date(2000, 2, 2), TypeEtatDocumentFiscal.RETOURNE, declaration.getDernierEtatDeclarationOfType(TypeEtatDocumentFiscal.RETOURNE));
		assertNull(declaration.getDernierEtatDeclarationOfType(TypeEtatDocumentFiscal.ECHU));
	}

	@Test
	public void testGetDernierEtatOfTypeAvecEtatAnnule() {

		DeclarationImpotOrdinaire declaration = new DeclarationImpotOrdinairePP();
		declaration.addEtat(new EtatDeclarationEmise(date(2000, 1, 1)));
		declaration.addEtat(new EtatDeclarationEchue(date(2000, 3, 3)));
		final EtatDeclaration etatRetourne = new EtatDeclarationRetournee(date(2000, 4, 4), "TEST");
		etatRetourne.setAnnule(true);
		declaration.addEtat(etatRetourne);
		declaration.addEtat(new EtatDeclarationSommee(date(2000, 2, 2),date(2000, 2, 2), null));

		assertEtat(date(2000, 1, 1), TypeEtatDocumentFiscal.EMIS, declaration.getDernierEtatDeclarationOfType(TypeEtatDocumentFiscal.EMIS));
		assertEtat(date(2000, 2, 2), TypeEtatDocumentFiscal.SOMME, declaration.getDernierEtatDeclarationOfType(TypeEtatDocumentFiscal.SOMME));
		assertEtat(date(2000, 3, 3), TypeEtatDocumentFiscal.ECHU, declaration.getDernierEtatDeclarationOfType(TypeEtatDocumentFiscal.ECHU));
		assertNull(declaration.getDernierEtatDeclarationOfType(TypeEtatDocumentFiscal.RETOURNE));
	}

	@Test
	public void testGetDernierEtatOfTypeAvecPlusieursMemesEtats() {

		// cas DI avec états incohérents
		DeclarationImpotOrdinaire declaration = new DeclarationImpotOrdinairePP();
		declaration.addEtat(new EtatDeclarationEmise(date(2000, 1, 1)));
		declaration.addEtat(new EtatDeclarationSommee(date(2000, 2, 2),date(2000, 2, 2), null));
		declaration.addEtat(new EtatDeclarationEchue(date(2000, 3, 3)));
		declaration.addEtat(new EtatDeclarationRetournee(date(2000, 4, 4), "TEST"));
		declaration.addEtat(new EtatDeclarationEmise(date(2001, 1, 1)));
		declaration.addEtat(new EtatDeclarationSommee(date(2001, 2, 2),date(2000, 2, 2), null));

		assertEtat(date(2001, 1, 1), TypeEtatDocumentFiscal.EMIS, declaration.getDernierEtatDeclarationOfType(TypeEtatDocumentFiscal.EMIS));
		assertEtat(date(2001, 2, 2), TypeEtatDocumentFiscal.SOMME, declaration.getDernierEtatDeclarationOfType(TypeEtatDocumentFiscal.SOMME));
		assertEtat(date(2000, 3, 3), TypeEtatDocumentFiscal.ECHU, declaration.getDernierEtatDeclarationOfType(TypeEtatDocumentFiscal.ECHU));
		assertEtat(date(2000, 4, 4), TypeEtatDocumentFiscal.RETOURNE, declaration.getDernierEtatDeclarationOfType(TypeEtatDocumentFiscal.RETOURNE));
	}


	private static void assertEtat(RegDate date, TypeEtatDocumentFiscal type, EtatDeclaration etat) {
		assertNotNull(etat);
		assertEquals(date, etat.getDateObtention());
		assertEquals(type, etat.getEtat());
	}
}
