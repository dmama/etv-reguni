package ch.vd.uniregctb.tiers;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.adresse.AdresseSuisse;
import ch.vd.uniregctb.common.WithoutSpringTest;
import ch.vd.uniregctb.declaration.Declaration;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinairePP;
import ch.vd.uniregctb.declaration.PeriodeFiscale;
import ch.vd.uniregctb.mouvement.EnvoiDossier;
import ch.vd.uniregctb.mouvement.EnvoiDossierVersCollaborateur;
import ch.vd.uniregctb.type.GenreImpot;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.TypeAdresseTiers;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

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
				ForFiscalPrincipalPP forFiscal = new ForFiscalPrincipalPP();
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
				ForFiscalPrincipalPP forFiscal = new ForFiscalPrincipalPP();
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
				ForFiscalPrincipalPP forFiscal = new ForFiscalPrincipalPP();
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
				ForFiscalPrincipalPP forFiscal = new ForFiscalPrincipalPP();
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
				ForFiscalPrincipalPP forFiscal = new ForFiscalPrincipalPP();
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
				ForFiscalPrincipalPP forFiscal1 = new ForFiscalPrincipalPP();
				forFiscal1.setDateDebut(RegDate.get(1990, 1, 1));
				forFiscal1.setDateFin(RegDate.get(2002, 3, 21));
				tiers.addForFiscal(forFiscal1);

				ForFiscalPrincipalPP forFiscal2 = new ForFiscalPrincipalPP();
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
				ForFiscalPrincipalPP forFiscal1 = new ForFiscalPrincipalPP();
				forFiscal1.setDateDebut(RegDate.get(1990, 1, 1));
				forFiscal1.setDateFin(RegDate.get(2002, 3, 21));
				tiers.addForFiscal(forFiscal1);

				ForFiscalPrincipalPP forFiscal2 = new ForFiscalPrincipalPP();
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

		DeclarationImpotOrdinairePP d1 = new DeclarationImpotOrdinairePP();
		d1.setPeriode(periode2008);
		tiers.addDeclaration(d1);
		assertEquals(Integer.valueOf(1), d1.getNumero());

		DeclarationImpotOrdinairePP d2 = new DeclarationImpotOrdinairePP();
		d2.setPeriode(periode2008);
		tiers.addDeclaration(d2);
		assertEquals(Integer.valueOf(2), d2.getNumero());
		d2.setAnnule(true);

		DeclarationImpotOrdinairePP d3 = new DeclarationImpotOrdinairePP();
		d3.setPeriode(periode2008);
		tiers.addDeclaration(d3);
		assertEquals(Integer.valueOf(3), d3.getNumero()); // le numéro 3, même si la déclaration précédente est annulée

		// 2009

		DeclarationImpotOrdinairePP d4 = new DeclarationImpotOrdinairePP();
		d4.setPeriode(periode2009);
		tiers.addDeclaration(d4);
		assertEquals(Integer.valueOf(1), d4.getNumero()); // recommence à 1 en début d'année

		DeclarationImpotOrdinairePP d5 = new DeclarationImpotOrdinairePP();
		d5.setPeriode(periode2009);
		tiers.addDeclaration(d5);
		assertEquals(Integer.valueOf(2), d5.getNumero());
	}

	/**
	 * [SIFISC-1368] Vérifie qu'aucun code de contrôle n'est générée ou assignée lors des différents scénarios d'ajout d'une déclaration d'impôt ordinaire à un tiers pour les périodes fiscales avant 2011.
	 */
	@Test
	public void testAddDeclarationCodeControleAvant2011() {

		final PeriodeFiscale periode2008 = new PeriodeFiscale();
		periode2008.setAnnee(2008);

		final PeriodeFiscale periode2010 = new PeriodeFiscale();
		periode2010.setAnnee(2010);

		final PersonnePhysique tiers = new PersonnePhysique(true);
		assertEmpty(tiers.getDeclarations());

		// 2009

		final DeclarationImpotOrdinairePP d1 = new DeclarationImpotOrdinairePP();
		d1.setPeriode(periode2008);
		tiers.addDeclaration(d1);
		assertNull(d1.getCodeControle());

		final DeclarationImpotOrdinairePP d2 = new DeclarationImpotOrdinairePP();
		d2.setPeriode(periode2008);
		tiers.addDeclaration(d2);
		assertNull(d2.getCodeControle());
		d2.setAnnule(true);

		final DeclarationImpotOrdinairePP d3 = new DeclarationImpotOrdinairePP();
		d3.setPeriode(periode2008);
		tiers.addDeclaration(d3);
		assertNull(d3.getCodeControle());

		// 2010

		final DeclarationImpotOrdinairePP d4 = new DeclarationImpotOrdinairePP();
		d4.setPeriode(periode2010);
		tiers.addDeclaration(d4);
		assertNull(d4.getCodeControle());

		final DeclarationImpotOrdinairePP d5 = new DeclarationImpotOrdinairePP();
		d5.setPeriode(periode2010);
		tiers.addDeclaration(d5);
		assertNull(d5.getCodeControle());
	}

	/**
	 * [SIFISC-1368] Vérifie que le code de contrôle est bien générée ou assignée lors des différents scénarios d'ajout d'une déclaration d'impôt ordinaire à un tiers.
	 */
	@Test
	public void testAddDeclarationCodeControleDes2011() {

		final PeriodeFiscale periode2011 = new PeriodeFiscale();
		periode2011.setAnnee(2011);

		final PeriodeFiscale periode2012 = new PeriodeFiscale();
		periode2012.setAnnee(2012);

		final PersonnePhysique tiers = new PersonnePhysique(true);
		assertEmpty(tiers.getDeclarations());

		// 2011

		final String codeControle2011;

		// test la génération du code de contrôle
		final DeclarationImpotOrdinairePP d1 = new DeclarationImpotOrdinairePP();
		d1.setPeriode(periode2011);
		tiers.addDeclaration(d1);
		codeControle2011 = d1.getCodeControle();
		assertTrue(StringUtils.isNotBlank(codeControle2011));

		// test la réutilisation du code contrôle dans la même année
		final DeclarationImpotOrdinairePP d2 = new DeclarationImpotOrdinairePP();
		d2.setPeriode(periode2011);
		tiers.addDeclaration(d2);
		assertEquals(codeControle2011, d2.getCodeControle());
		d2.setAnnule(true);

		// test la réutilisation du code contrôle dans la même année, même en cas de DI annulée
		final DeclarationImpotOrdinairePP d3 = new DeclarationImpotOrdinairePP();
		d3.setPeriode(periode2011);
		tiers.addDeclaration(d3);
		assertEquals(codeControle2011, d3.getCodeControle()); // le même code de contrôle, même si la déclaration précédente est annulée

		// 2012

		final String codeControle2012;

		// test la génération d'un autre code de contrôle
		final DeclarationImpotOrdinairePP d4 = new DeclarationImpotOrdinairePP();
		d4.setPeriode(periode2012);
		tiers.addDeclaration(d4);
		codeControle2012 = d4.getCodeControle();
		assertTrue(StringUtils.isNotBlank(codeControle2012));

		final DeclarationImpotOrdinairePP d5 = new DeclarationImpotOrdinairePP();
		d5.setPeriode(periode2012);
		tiers.addDeclaration(d5);
		assertEquals(codeControle2012, d5.getCodeControle());
	}

	/**
	 * [SIFISC-1368] Vérifie que le code de contrôle est bien généré et assigné sur toutes les déclarations d'une année fiscale (cas des déclarations préexistantes sans code de contrôle).
	 */
	@Test
	public void testAddDeclarationCodeControleDeclarationsPreexistantesSansCode() {

		final PeriodeFiscale periode = new PeriodeFiscale();
		periode.setAnnee(2011);

		final PersonnePhysique tiers = new PersonnePhysique(true);
		final HashSet<Declaration> declarations = new HashSet<>();
		tiers.setDeclarations(declarations);

		// Déclarations préexistantes SANS code de contrôle

		final DeclarationImpotOrdinairePP d1 = new DeclarationImpotOrdinairePP();
		d1.setPeriode(periode);
		declarations.add(d1);

		// Ajout d'une seconde déclaration et test que le code de contrôle a bien été généré et assigné sur les DEUX déclarations
		final DeclarationImpotOrdinairePP d2 = new DeclarationImpotOrdinairePP();
		d2.setPeriode(periode);
		tiers.addDeclaration(d2);
		final String codeControle = d2.getCodeControle();
		assertTrue(StringUtils.isNotBlank(codeControle));
		assertEquals(codeControle, d1.getCodeControle());
	}

	private PersonnePhysique createHabitantWithFors() {

		PersonnePhysique hab = new PersonnePhysique(true);
		hab.setNumero(100011010L);
		hab.setNumeroIndividu(43L);

		Set<ForFiscal> fors = new HashSet<>();
		{
			ForFiscalAutreImpot forFiscal = new ForFiscalAutreImpot();
			forFiscal.setGenreImpot(GenreImpot.DROIT_MUTATION);
			forFiscal.setDateDebut(RegDate.get(2004, 3, 1));
			forFiscal.setDateFin(RegDate.get(2006, 2, 28));
			forFiscal.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
			forFiscal.setNumeroOfsAutoriteFiscale(1235);
			fors.add(forFiscal);
		}
		{
			ForFiscalAutreElementImposable forFiscal = new ForFiscalAutreElementImposable();
			forFiscal.setMotifRattachement(MotifRattachement.ACTIVITE_LUCRATIVE_CAS);
			forFiscal.setDateDebut(RegDate.get(2006, 6, 1));
			forFiscal.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
			forFiscal.setNumeroOfsAutoriteFiscale(1236);
			forFiscal.setMotifOuverture(MotifFor.DEBUT_EXPLOITATION);
			fors.add(forFiscal);
		}
		{
			ForFiscalSecondaire forFiscal = new ForFiscalSecondaire();
			forFiscal.setMotifRattachement(MotifRattachement.ACTIVITE_INDEPENDANTE);
			forFiscal.setDateDebut(RegDate.get(2002, 6, 1));
			forFiscal.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
			forFiscal.setNumeroOfsAutoriteFiscale(1237);
			forFiscal.setMotifOuverture(MotifFor.DEBUT_EXPLOITATION);
			fors.add(forFiscal);
		}

		// Principaux
		// 2002, 1, 1 - 2005, 8, 11
		{
			ForFiscalPrincipalPP forFiscal = new ForFiscalPrincipalPP();
			forFiscal.setDateDebut(RegDate.get(2002, 1, 1));
			forFiscal.setDateFin(RegDate.get(2005, 8, 11));
			forFiscal.setGenreImpot(GenreImpot.REVENU_FORTUNE);
			forFiscal.setModeImposition(ModeImposition.ORDINAIRE);
			forFiscal.setMotifRattachement(MotifRattachement.DOMICILE);
			forFiscal.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
			forFiscal.setNumeroOfsAutoriteFiscale(563);
			forFiscal.setMotifOuverture(MotifFor.ARRIVEE_HC);
			forFiscal.setMotifFermeture(MotifFor.DEPART_HS);

			fors.add(forFiscal);
		}
		// Annule : 2004, 6, 6 - 2005, 9, 9
		{
			ForFiscalPrincipalPP forFiscal = new ForFiscalPrincipalPP();
			forFiscal.setAnnule(true);
			forFiscal.setDateDebut(RegDate.get(2004, 6, 6));
			forFiscal.setDateFin(RegDate.get(2005, 9, 9));
			forFiscal.setGenreImpot(GenreImpot.REVENU_FORTUNE);
			forFiscal.setModeImposition(ModeImposition.ORDINAIRE);
			forFiscal.setMotifRattachement(MotifRattachement.DOMICILE);
			forFiscal.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
			forFiscal.setNumeroOfsAutoriteFiscale(1563);
			fors.add(forFiscal);
		}
		// 2005, 8, 12 - 2007, 2, 28
		{
			ForFiscalPrincipalPP forFiscal = new ForFiscalPrincipalPP();
			forFiscal.setDateDebut(RegDate.get(2005, 8, 12));
			forFiscal.setDateFin(RegDate.get(2007, 2, 28));
			forFiscal.setGenreImpot(GenreImpot.REVENU_FORTUNE);
			forFiscal.setModeImposition(ModeImposition.ORDINAIRE);
			forFiscal.setMotifRattachement(MotifRattachement.DOMICILE);
			forFiscal.setTypeAutoriteFiscale(TypeAutoriteFiscale.PAYS_HS);
			forFiscal.setNumeroOfsAutoriteFiscale(1234);
			forFiscal.setMotifOuverture(MotifFor.ARRIVEE_HS);
			fors.add(forFiscal);
		}
		// 2007, 3, 1 -> 2007, 3, 1 (1 jour)
		{
			ForFiscalPrincipalPP forFiscal = new ForFiscalPrincipalPP();
			forFiscal.setDateDebut(RegDate.get(2007, 3, 1));
			forFiscal.setDateFin(RegDate.get(2007, 3, 1));
			forFiscal.setGenreImpot(GenreImpot.REVENU_FORTUNE);
			forFiscal.setModeImposition(ModeImposition.ORDINAIRE);
			forFiscal.setMotifRattachement(MotifRattachement.DOMICILE);
			forFiscal.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_HC);
			forFiscal.setNumeroOfsAutoriteFiscale(563);
			fors.add(forFiscal);
		}
		// 2007, 3, 2 -> Ouvert
		{
			ForFiscalPrincipalPP forFiscal = new ForFiscalPrincipalPP();
			forFiscal.setDateDebut(RegDate.get(2007, 3, 2));
			forFiscal.setGenreImpot(GenreImpot.REVENU_FORTUNE);
			forFiscal.setModeImposition(ModeImposition.ORDINAIRE);
			forFiscal.setMotifRattachement(MotifRattachement.DOMICILE);
			forFiscal.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_HC);
			forFiscal.setNumeroOfsAutoriteFiscale(563);
			fors.add(forFiscal);
		}
		hab.setForsFiscaux(fors);
		return hab;
	}

	@Test
	public void testGetForsFiscauxValidAt() {
		PersonnePhysique hab = createHabitantWithFors();
		List<ForFiscal> list = hab.getForsFiscauxValidAt(RegDate.get(2005, 9, 9));
		assertNotNull(list);
		assertEquals(3, list.size());

		// liste de fors valides
		List<ForFiscal> list1903 = hab.getForsFiscauxValidAt(RegDate.get(1903, 1, 1));
		assertNotNull(list1903);
		assertEmpty(list1903);
	}
}
