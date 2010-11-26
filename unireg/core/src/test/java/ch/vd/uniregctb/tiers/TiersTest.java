package ch.vd.uniregctb.tiers;

import org.junit.Test;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.NotImplementedException;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.adresse.AdresseSuisse;
import ch.vd.uniregctb.common.WithoutSpringTest;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.declaration.PeriodeFiscale;
import ch.vd.uniregctb.mouvement.EnvoiDossier;
import ch.vd.uniregctb.mouvement.EnvoiDossierVersCollaborateur;
import ch.vd.uniregctb.type.TypeAdresseTiers;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertSame;

public class TiersTest extends WithoutSpringTest {

	@Test
	public void testGetDateDebutEtFinActivite() {

		// Tiers sans for
		{
			PersonnePhysique tiers = new PersonnePhysique(true);
			assertNull(tiers.getDateDebutActivite());
			assertNull(tiers.getDateFinActivite());
		}

		// Tiers avec un for annulé
		{
			PersonnePhysique tiers = new PersonnePhysique(true);
			{
				ForFiscalPrincipal forFiscal = new ForFiscalPrincipal();
				forFiscal.setDateDebut(RegDate.get(1990, 1, 1));
				forFiscal.setDateFin(null);
				forFiscal.setAnnulationDate(DateHelper.getDate(1993, 1, 1));
				tiers.addForFiscal(forFiscal);
			}
			assertNull(tiers.getDateDebutActivite());
			assertNull(tiers.getDateFinActivite());
		}

		// Tiers avec un for ouvert
		{
			PersonnePhysique tiers = new PersonnePhysique(true);
			{
				ForFiscalPrincipal forFiscal = new ForFiscalPrincipal();
				forFiscal.setDateDebut(RegDate.get(1990, 1, 1));
				forFiscal.setDateFin(null);
				tiers.addForFiscal(forFiscal);
			}
			assertEquals(RegDate.get(1990, 1, 1), tiers.getDateDebutActivite());
			assertNull(tiers.getDateFinActivite());
		}

		// Tiers avec un for fermé
		{
			PersonnePhysique tiers = new PersonnePhysique(true);
			{
				ForFiscalPrincipal forFiscal = new ForFiscalPrincipal();
				forFiscal.setDateDebut(RegDate.get(1990, 1, 1));
				forFiscal.setDateFin(RegDate.get(2002, 3, 21));
				tiers.addForFiscal(forFiscal);
			}
			assertEquals(RegDate.get(1990, 1, 1), tiers.getDateDebutActivite());
			assertEquals(RegDate.get(2002, 3, 21), tiers.getDateFinActivite());
		}

		// Tiers avec un for ouvert au deux bouts
		{
			PersonnePhysique tiers = new PersonnePhysique(true);
			{
				ForFiscalPrincipal forFiscal = new ForFiscalPrincipal();
				forFiscal.setDateDebut(null);
				forFiscal.setDateFin(null);
				tiers.addForFiscal(forFiscal);
			}
			assertNull(tiers.getDateDebutActivite());
			assertNull(tiers.getDateFinActivite());
		}

		// Tiers avec un for ouvert au début et fermé à la fin
		{
			PersonnePhysique tiers = new PersonnePhysique(true);
			{
				ForFiscalPrincipal forFiscal = new ForFiscalPrincipal();
				forFiscal.setDateDebut(null);
				forFiscal.setDateFin(RegDate.get(2002, 3, 21));
				tiers.addForFiscal(forFiscal);
			}
			assertNull(tiers.getDateDebutActivite());
			assertEquals(RegDate.get(2002, 3, 21), tiers.getDateFinActivite());
		}

		// Tiers avec deux fors: un fermé et un ouvert
		{
			PersonnePhysique tiers = new PersonnePhysique(true);
			{
				ForFiscalPrincipal forFiscal1 = new ForFiscalPrincipal();
				forFiscal1.setDateDebut(RegDate.get(1990, 1, 1));
				forFiscal1.setDateFin(RegDate.get(2002, 3, 21));
				tiers.addForFiscal(forFiscal1);

				ForFiscalPrincipal forFiscal2 = new ForFiscalPrincipal();
				forFiscal2.setDateDebut(RegDate.get(2002, 3, 22));
				forFiscal2.setDateFin(null);
				tiers.addForFiscal(forFiscal2);
			}
			assertEquals(RegDate.get(1990, 1, 1), tiers.getDateDebutActivite());
			assertNull(tiers.getDateFinActivite());
		}

		// Tiers avec deux fors fermés
		{
			PersonnePhysique tiers = new PersonnePhysique(true);
			{
				ForFiscalPrincipal forFiscal1 = new ForFiscalPrincipal();
				forFiscal1.setDateDebut(RegDate.get(1990, 1, 1));
				forFiscal1.setDateFin(RegDate.get(2002, 3, 21));
				tiers.addForFiscal(forFiscal1);

				ForFiscalPrincipal forFiscal2 = new ForFiscalPrincipal();
				forFiscal2.setDateDebut(RegDate.get(2002, 3, 22));
				forFiscal2.setDateFin(RegDate.get(2005, 12, 31));
				tiers.addForFiscal(forFiscal2);
			}
			assertEquals(RegDate.get(1990, 1, 1), tiers.getDateDebutActivite());
			assertEquals(RegDate.get(2005, 12, 31), tiers.getDateFinActivite());
		}
	}

	@Test
	public void testEtatDesactive() throws Exception {

		// sans aucun for -> jamais désactivé
		{
			final PersonnePhysique pp = new PersonnePhysique(true);
			for (int i = -50 ; i < 50 ; ++ i) {
				final RegDate testDate = RegDate.get().addMonths(i);
				assertFalse("Date : " + testDate, pp.isDesactive(testDate));
			}
		}

		// tiers annulé -> toujours désactivé
		{
			final PersonnePhysique pp = new PersonnePhysique(true);
			pp.setAnnule(true);
			for (int i = -50 ; i < 50 ; ++ i) {
				final RegDate testDate = RegDate.get().addMonths(i);
				assertTrue("Date : " + testDate, pp.isDesactive(testDate));
			}
		}
	}

	@Test
	public void testAddMouvement() {

		{
			PersonnePhysique tiers = new PersonnePhysique(true);
			{
				EnvoiDossier envoi = new EnvoiDossierVersCollaborateur();
				tiers.addMouvementDossier(envoi);
			}
			assertEquals(1, tiers.getMouvementsDossier().size());
		}
	}

	@Test
	public void testGetAdresseTiersAt() {

		// Tiers sans adresse
		PersonnePhysique tiers = new PersonnePhysique(true);
		assertNull(tiers.getAdresseTiersAt(-1, TypeAdresseTiers.COURRIER));
		assertNull(tiers.getAdresseTiersAt(0, TypeAdresseTiers.COURRIER));
		assertNull(tiers.getAdresseTiersAt(1, TypeAdresseTiers.COURRIER));

		// Tiers avec une adresse
		final AdresseSuisse adresse0 = new AdresseSuisse();
		adresse0.setUsage(TypeAdresseTiers.COURRIER);
		adresse0.setDateDebut(RegDate.get(1990, 1, 1));
		tiers.addAdresseTiers(adresse0);
		assertNull(tiers.getAdresseTiersAt(-2, TypeAdresseTiers.COURRIER));
		assertSame(adresse0, tiers.getAdresseTiersAt(-1, TypeAdresseTiers.COURRIER));
		assertSame(adresse0, tiers.getAdresseTiersAt(0, TypeAdresseTiers.COURRIER));
		assertNull(tiers.getAdresseTiersAt(1, TypeAdresseTiers.COURRIER));

		// Tiers avec deux adresses
		adresse0.setDateFin(RegDate.get(1999, 12, 31));
		final AdresseSuisse adresse1 = new AdresseSuisse();
		adresse1.setUsage(TypeAdresseTiers.COURRIER);
		adresse1.setDateDebut(RegDate.get(2000, 1, 1));
		tiers.addAdresseTiers(adresse1);
		assertNull(tiers.getAdresseTiersAt(-3, TypeAdresseTiers.COURRIER));
		assertSame(adresse0, tiers.getAdresseTiersAt(-2, TypeAdresseTiers.COURRIER));
		assertSame(adresse1, tiers.getAdresseTiersAt(-1, TypeAdresseTiers.COURRIER));
		assertSame(adresse0, tiers.getAdresseTiersAt(0, TypeAdresseTiers.COURRIER));
		assertSame(adresse1, tiers.getAdresseTiersAt(1, TypeAdresseTiers.COURRIER));
		assertNull(tiers.getAdresseTiersAt(2, TypeAdresseTiers.COURRIER));

		// Tiers avec trois adresses dont une annulée
		final AdresseSuisse adresse2 = new AdresseSuisse();
		adresse2.setUsage(TypeAdresseTiers.COURRIER);
		adresse2.setDateDebut(RegDate.get(2010, 1, 1));
		adresse2.setAnnule(true);
		tiers.addAdresseTiers(adresse2);
		assertNull(tiers.getAdresseTiersAt(-3, TypeAdresseTiers.COURRIER));
		assertSame(adresse0, tiers.getAdresseTiersAt(-2, TypeAdresseTiers.COURRIER));
		assertSame(adresse1, tiers.getAdresseTiersAt(-1, TypeAdresseTiers.COURRIER));
		assertSame(adresse0, tiers.getAdresseTiersAt(0, TypeAdresseTiers.COURRIER));
		assertSame(adresse1, tiers.getAdresseTiersAt(1, TypeAdresseTiers.COURRIER));
		assertNull(tiers.getAdresseTiersAt(2, TypeAdresseTiers.COURRIER));
	}

	@Test
	public void testAddDeclaration() {

		final PeriodeFiscale periode2008 = new PeriodeFiscale();
		periode2008.setAnnee(2008);

		final PeriodeFiscale periode2009 = new PeriodeFiscale();
		periode2009.setAnnee(2009);

		final PersonnePhysique tiers = new PersonnePhysique(true);
		assertEmpty(tiers.getDeclarations());

		// 2008

		DeclarationImpotOrdinaire d1 = new DeclarationImpotOrdinaire();
		d1.setPeriode(periode2008);
		tiers.addDeclaration(d1);
		assertEquals(Integer.valueOf(1), d1.getNumero());

		DeclarationImpotOrdinaire d2 = new DeclarationImpotOrdinaire();
		d2.setPeriode(periode2008);
		tiers.addDeclaration(d2);
		assertEquals(Integer.valueOf(2), d2.getNumero());
		d2.setAnnule(true);

		DeclarationImpotOrdinaire d3 = new DeclarationImpotOrdinaire();
		d3.setPeriode(periode2008);
		tiers.addDeclaration(d3);
		assertEquals(Integer.valueOf(3), d3.getNumero()); // le numéro 3, même si la déclaration précédente est annulée

		// 2009

		DeclarationImpotOrdinaire d4 = new DeclarationImpotOrdinaire();
		d4.setPeriode(periode2009);
		tiers.addDeclaration(d4);
		assertEquals(Integer.valueOf(1), d4.getNumero()); // recommence à 1 en début d'année

		DeclarationImpotOrdinaire d5 = new DeclarationImpotOrdinaire();
		d5.setPeriode(periode2009);
		tiers.addDeclaration(d5);
		assertEquals(Integer.valueOf(2), d5.getNumero());
	}

	@Test
	public void testGetDerniereDeclaration(){
		final PeriodeFiscale periode2008 = new PeriodeFiscale();
		periode2008.setAnnee(2008);

		final PeriodeFiscale periode2009 = new PeriodeFiscale();
		periode2009.setAnnee(2009);

		final PersonnePhysique tiers = new PersonnePhysique(true);
		assertEmpty(tiers.getDeclarations());

		// 2008

		DeclarationImpotOrdinaire d1 = new DeclarationImpotOrdinaire();
		d1.setPeriode(periode2008);
		tiers.addDeclaration(d1);

		 // 2009
		DeclarationImpotOrdinaire d2 = new DeclarationImpotOrdinaire();
		d2.setPeriode(periode2009);
		tiers.addDeclaration(d2);
		assertEquals(d2.getId(),tiers.getDerniereDeclaration().getId());
		DeclarationImpotOrdinaire d3 = new DeclarationImpotOrdinaire();
		d3.setPeriode(periode2009);
		tiers.addDeclaration(d3);
		d3.setAnnule(true);
		assertEquals(d2.getId(),tiers.getDerniereDeclaration().getId());


	}

	@Test
	public void testValidateTiersAnnule() {

		final Tiers tiers = new Tiers() {
			@Override
			protected ValidationResults validateTypeAdresses() {
				return new ValidationResults();
			}

			@Override
			public String getRoleLigne1() {
				throw new NotImplementedException();
			}

			@Override
			public NatureTiers getNatureTiers() {
				throw new NotImplementedException();
			}

			@Override
			public TypeTiers getType() {
				throw new NotImplementedException();
			}
		};

		// Tiers invalide (for fiscal avec date de début nulle) mais annulé => pas d'erreur
		{
			tiers.addForFiscal(new ForFiscalAutreElementImposable());
			tiers.setAnnule(true);
			assertFalse(tiers.validate().hasErrors());
		}

		// Tiers valide et annulée => pas d'erreur
		{
			tiers.getForsFiscaux().clear();
			tiers.setAnnule(true);
			assertFalse(tiers.validate().hasErrors());
		}
	}

}
