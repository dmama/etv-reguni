package ch.vd.uniregctb.declaration;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.common.WithoutSpringTest;
import ch.vd.uniregctb.type.TypeEtatDeclaration;

public class DeclarationTest extends WithoutSpringTest {

	@Test
	public void testIsValid() {

		AuthenticationHelper.setPrincipal("[UT] "+getClass().getSimpleName());

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
		declaration.addEtat(new EtatDeclaration(date(2000, 1, 1), TypeEtatDeclaration.EMISE));
		declaration.addEtat(new EtatDeclaration(date(2000, 3, 3), TypeEtatDeclaration.ECHUE));
		declaration.addEtat(new EtatDeclaration(date(2000, 4, 4), TypeEtatDeclaration.RETOURNEE));
		declaration.addEtat(new EtatDeclaration(date(2000, 2, 2), TypeEtatDeclaration.SOMMEE));

		assertEtat(date(2000, 4, 4), TypeEtatDeclaration.RETOURNEE, declaration.getDernierEtat());
	}

	@Test
	public void testGetDernierEtatAvecEtatsSommeeEtRetourneeLeMemeJour() {

		DeclarationImpotOrdinaire declaration = new DeclarationImpotOrdinaire();
		declaration.addEtat(new EtatDeclaration(date(2000, 1, 1), TypeEtatDeclaration.EMISE));
		declaration.addEtat(new EtatDeclaration(date(2000, 2, 2), TypeEtatDeclaration.RETOURNEE));
		declaration.addEtat(new EtatDeclaration(date(2000, 2, 2), TypeEtatDeclaration.SOMMEE));

		assertEtat(date(2000, 2, 2), TypeEtatDeclaration.RETOURNEE, declaration.getDernierEtat());
	}

	@Test
	public void testGetDernierEtatAvecEtatAnnule() {

		DeclarationImpotOrdinaire declaration = new DeclarationImpotOrdinaire();
		declaration.addEtat(new EtatDeclaration(date(2000, 1, 1), TypeEtatDeclaration.EMISE));
		declaration.addEtat(new EtatDeclaration(date(2000, 3, 3), TypeEtatDeclaration.ECHUE));
		final EtatDeclaration etatRetourne = new EtatDeclaration(date(2000, 4, 4), TypeEtatDeclaration.RETOURNEE);
		etatRetourne.setAnnule(true);
		declaration.addEtat(etatRetourne);
		declaration.addEtat(new EtatDeclaration(date(2000, 2, 2), TypeEtatDeclaration.SOMMEE));

		assertEtat(date(2000, 3, 3), TypeEtatDeclaration.ECHUE, declaration.getDernierEtat());
	}

	@Test
	public void testGetEtatDeclarationActifCasNormal() {

		DeclarationImpotOrdinaire declaration = new DeclarationImpotOrdinaire();
		declaration.addEtat(new EtatDeclaration(date(2000, 1, 1), TypeEtatDeclaration.EMISE));
		declaration.addEtat(new EtatDeclaration(date(2000, 3, 3), TypeEtatDeclaration.ECHUE));
		declaration.addEtat(new EtatDeclaration(date(2000, 4, 4), TypeEtatDeclaration.RETOURNEE));
		declaration.addEtat(new EtatDeclaration(date(2000, 2, 2), TypeEtatDeclaration.SOMMEE));

		assertEtat(date(2000, 1, 1), TypeEtatDeclaration.EMISE, declaration.getEtatDeclarationActif(TypeEtatDeclaration.EMISE));
		assertEtat(date(2000, 2, 2), TypeEtatDeclaration.SOMMEE, declaration.getEtatDeclarationActif(TypeEtatDeclaration.SOMMEE));
		assertEtat(date(2000, 3, 3), TypeEtatDeclaration.ECHUE, declaration.getEtatDeclarationActif(TypeEtatDeclaration.ECHUE));
		assertEtat(date(2000, 4, 4), TypeEtatDeclaration.RETOURNEE, declaration.getEtatDeclarationActif(TypeEtatDeclaration.RETOURNEE));
	}

	@Test
	public void testGetEtatDeclarationActifAvecEtatsSommeeEtRetourneeLeMemeJour() {

		DeclarationImpotOrdinaire declaration = new DeclarationImpotOrdinaire();
		declaration.addEtat(new EtatDeclaration(date(2000, 1, 1), TypeEtatDeclaration.EMISE));
		declaration.addEtat(new EtatDeclaration(date(2000, 2, 2), TypeEtatDeclaration.RETOURNEE));
		declaration.addEtat(new EtatDeclaration(date(2000, 2, 2), TypeEtatDeclaration.SOMMEE));

		assertEtat(date(2000, 1, 1), TypeEtatDeclaration.EMISE, declaration.getEtatDeclarationActif(TypeEtatDeclaration.EMISE));
		assertEtat(date(2000, 2, 2), TypeEtatDeclaration.SOMMEE, declaration.getEtatDeclarationActif(TypeEtatDeclaration.SOMMEE));
		assertEtat(date(2000, 2, 2), TypeEtatDeclaration.RETOURNEE, declaration.getEtatDeclarationActif(TypeEtatDeclaration.RETOURNEE));
		assertNull(declaration.getEtatDeclarationActif(TypeEtatDeclaration.ECHUE));
	}

	@Test
	public void testGetEtatDeclarationActifAvecEtatAnnule() {

		DeclarationImpotOrdinaire declaration = new DeclarationImpotOrdinaire();
		declaration.addEtat(new EtatDeclaration(date(2000, 1, 1), TypeEtatDeclaration.EMISE));
		declaration.addEtat(new EtatDeclaration(date(2000, 3, 3), TypeEtatDeclaration.ECHUE));
		final EtatDeclaration etatRetourne = new EtatDeclaration(date(2000, 4, 4), TypeEtatDeclaration.RETOURNEE);
		etatRetourne.setAnnule(true);
		declaration.addEtat(etatRetourne);
		declaration.addEtat(new EtatDeclaration(date(2000, 2, 2), TypeEtatDeclaration.SOMMEE));

		assertEtat(date(2000, 1, 1), TypeEtatDeclaration.EMISE, declaration.getEtatDeclarationActif(TypeEtatDeclaration.EMISE));
		assertEtat(date(2000, 2, 2), TypeEtatDeclaration.SOMMEE, declaration.getEtatDeclarationActif(TypeEtatDeclaration.SOMMEE));
		assertEtat(date(2000, 3, 3), TypeEtatDeclaration.ECHUE, declaration.getEtatDeclarationActif(TypeEtatDeclaration.ECHUE));
		assertNull(declaration.getEtatDeclarationActif(TypeEtatDeclaration.RETOURNEE));
	}

	@Test
	public void testGetEtatDeclarationActifAvecPlusieursMemesEtats() {

		// cas DI avec états incohérents
		DeclarationImpotOrdinaire declaration = new DeclarationImpotOrdinaire();
		declaration.addEtat(new EtatDeclaration(date(2000, 1, 1), TypeEtatDeclaration.EMISE));
		declaration.addEtat(new EtatDeclaration(date(2000, 2, 2), TypeEtatDeclaration.SOMMEE));
		declaration.addEtat(new EtatDeclaration(date(2000, 3, 3), TypeEtatDeclaration.ECHUE));
		declaration.addEtat(new EtatDeclaration(date(2000, 4, 4), TypeEtatDeclaration.RETOURNEE));
		declaration.addEtat(new EtatDeclaration(date(2001, 1, 1), TypeEtatDeclaration.EMISE));
		declaration.addEtat(new EtatDeclaration(date(2001, 2, 2), TypeEtatDeclaration.SOMMEE));

		assertEtat(date(2001, 1, 1), TypeEtatDeclaration.EMISE, declaration.getEtatDeclarationActif(TypeEtatDeclaration.EMISE));
		assertEtat(date(2001, 2, 2), TypeEtatDeclaration.SOMMEE, declaration.getEtatDeclarationActif(TypeEtatDeclaration.SOMMEE));
		assertEtat(date(2000, 3, 3), TypeEtatDeclaration.ECHUE, declaration.getEtatDeclarationActif(TypeEtatDeclaration.ECHUE));
		assertEtat(date(2000, 4, 4), TypeEtatDeclaration.RETOURNEE, declaration.getEtatDeclarationActif(TypeEtatDeclaration.RETOURNEE));
	}


	private static void assertEtat(RegDate date, TypeEtatDeclaration type, EtatDeclaration etat) {
		assertNotNull(etat);
		assertEquals(date, etat.getDateObtention());
		assertEquals(type, etat.getEtat());
	}

	/**
	 * Raccourci pour créer une RegDate.
	 */
	private static RegDate date(int year, int month, int day) {
		return RegDate.get(year, month, day);
	}
}
