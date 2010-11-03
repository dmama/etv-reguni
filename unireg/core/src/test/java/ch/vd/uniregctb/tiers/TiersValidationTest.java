package ch.vd.uniregctb.tiers;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.adresse.AdresseSuisse;
import ch.vd.uniregctb.adresse.AdresseTiers;
import ch.vd.uniregctb.common.WithoutSpringTest;
import ch.vd.uniregctb.type.GenreImpot;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.TypeAdresseTiers;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

public class TiersValidationTest extends WithoutSpringTest {

	protected static final Logger LOGGER = Logger.getLogger(TiersValidationTest.class);

	@Test
	public void testValidateForOK() {

		PersonnePhysique hab = createHabitantWithFors();
		// debugValidationResults(hab.validate());
		assertEquals(0, hab.validate().errorsCount());
	}

	@Test
	public void testValidateForAdresseDebutApresDateFin() {

		PersonnePhysique hab = createHabitantWithFors();
		ForFiscalPrincipal ff = new ForFiscalPrincipal();
		ff.setMotifRattachement(MotifRattachement.DOMICILE);
		ff.setDateDebut(RegDate.get(2001, 5, 4));
		ff.setDateFin(RegDate.get(2001, 5, 3));
		ff.setModeImposition(ModeImposition.ORDINAIRE);
		hab.addForFiscal(ff);

		// debugValidationResults(hab.validate());
		assertEquals(1, hab.validate().errorsCount());
	}

	@Test
	public void testValidateMotifRattachement() {

		PersonnePhysique hab = createHabitantWithFors();
		ForFiscalPrincipal ff = new ForFiscalPrincipal();
		// motif de rattachement = null
		ff.setDateDebut(RegDate.get(2001, 5, 4));
		ff.setDateFin(RegDate.get(2001, 5, 14));
		ff.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
		ff.setNumeroOfsAutoriteFiscale(5586);
		ff.setMotifOuverture(MotifFor.ARRIVEE_HS);
		ff.setMotifFermeture(MotifFor.DEPART_HC);
		ff.setModeImposition(ModeImposition.ORDINAIRE);
		hab.addForFiscal(ff);

		// debugValidationResults(hab.validate());
		assertEquals(1, hab.validate().errorsCount());
	}

	// UNIREG-601: un tiers annulé doit toujours valider
	@Test
	public void testValidateTiersAnnule() {

		// habitant qui valide
		PersonnePhysique hab = new PersonnePhysique(true);
		hab.setNumeroIndividu(1233L);
		assertEquals(0, hab.validate().errorsCount());
		assertEquals(0, hab.validate().warningsCount());

		// on ajoute un for invalide (dateDebut > dateFin)
		ForFiscalPrincipal forFiscal = new ForFiscalPrincipal();
		forFiscal.setDateDebut(RegDate.get(2005, 8, 12));
		forFiscal.setDateFin(RegDate.get(1995, 2, 28));
		forFiscal.setGenreImpot(GenreImpot.REVENU_FORTUNE);
		forFiscal.setModeImposition(ModeImposition.ORDINAIRE);
		forFiscal.setMotifRattachement(MotifRattachement.DOMICILE);
		forFiscal.setTypeAutoriteFiscale(TypeAutoriteFiscale.PAYS_HS);
		forFiscal.setNumeroOfsAutoriteFiscale(1234);
		hab.addForFiscal(forFiscal);
		assertEquals(1, hab.validate().errorsCount());
		assertEquals(0, hab.validate().warningsCount());

		// on annule le tiers => il doit de nouveau être valide
		hab.setAnnule(true);
		assertEquals(0, hab.validate().errorsCount());
		assertEquals(0, hab.validate().warningsCount());
	}

	@Test
	public void testDetectionChevauchementForsPrincipaux() {

		PersonnePhysique hab = new PersonnePhysique(true);
		hab.setNumeroIndividu(1233L);
		// 2005, 8, 12 - 2007, 2, 28
		{
			ForFiscalPrincipal forFiscal = new ForFiscalPrincipal();
			forFiscal.setDateDebut(RegDate.get(2005, 8, 12));
			forFiscal.setDateFin(RegDate.get(2007, 2, 28));
			forFiscal.setGenreImpot(GenreImpot.REVENU_FORTUNE);
			forFiscal.setModeImposition(ModeImposition.ORDINAIRE);
			forFiscal.setMotifRattachement(MotifRattachement.DOMICILE);
			forFiscal.setTypeAutoriteFiscale(TypeAutoriteFiscale.PAYS_HS);
			forFiscal.setNumeroOfsAutoriteFiscale(1234);
			hab.addForFiscal(forFiscal);
		}
		// 2007, 3, 1 -> 2007, 3, 1 (1 jour)
		{
			ForFiscalPrincipal forFiscal = new ForFiscalPrincipal();
			forFiscal.setDateDebut(RegDate.get(2007, 3, 1));
			forFiscal.setDateFin(RegDate.get(2007, 3, 1));
			forFiscal.setGenreImpot(GenreImpot.REVENU_FORTUNE);
			forFiscal.setModeImposition(ModeImposition.ORDINAIRE);
			forFiscal.setMotifRattachement(MotifRattachement.DOMICILE);
			forFiscal.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_HC);
			forFiscal.setNumeroOfsAutoriteFiscale(563);
			hab.addForFiscal(forFiscal);
		}
		// 2007, 3, 2 -> Ouvert
		{
			ForFiscalPrincipal forFiscal = new ForFiscalPrincipal();
			forFiscal.setDateDebut(RegDate.get(2007, 3, 2));
			forFiscal.setGenreImpot(GenreImpot.REVENU_FORTUNE);
			forFiscal.setModeImposition(ModeImposition.ORDINAIRE);
			forFiscal.setMotifRattachement(MotifRattachement.DOMICILE);
			forFiscal.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_HC);
			forFiscal.setNumeroOfsAutoriteFiscale(563);
			hab.addForFiscal(forFiscal);
		}

		// Ajout d'un for qui chevauche
		{
			ForFiscalPrincipal forFiscal = new ForFiscalPrincipal();
			forFiscal.setDateDebut(RegDate.get(2007, 1, 22));
			forFiscal.setDateFin(RegDate.get(2007, 4, 1));
			forFiscal.setMotifRattachement(MotifRattachement.DOMICILE);
			forFiscal.setTypeAutoriteFiscale(TypeAutoriteFiscale.PAYS_HS);
			forFiscal.setNumeroOfsAutoriteFiscale(1234);
			forFiscal.setModeImposition(ModeImposition.ORDINAIRE);
			hab.addForFiscal(forFiscal);
		}

		// debugValidationResults(hab.validate());
		assertEquals(2, hab.validate().errorsCount());
	}

	/**
	 * Teste que le chevauchement sur un seul jour est détecté (cas limite).
	 */
	@Test
	public void testDetectionChevauchementForsPrincipauxCasLimite() {

		PersonnePhysique hab = new PersonnePhysique(true);
		hab.setNumeroIndividu(1233L);
		{
			ForFiscalPrincipal forFiscal = new ForFiscalPrincipal();
			forFiscal.setDateDebut(RegDate.get(2002, 1, 1));
			forFiscal.setDateFin(RegDate.get(2005, 8, 12));
			forFiscal.setGenreImpot(GenreImpot.REVENU_FORTUNE);
			forFiscal.setModeImposition(ModeImposition.ORDINAIRE);
			forFiscal.setMotifRattachement(MotifRattachement.DOMICILE);
			forFiscal.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
			forFiscal.setNumeroOfsAutoriteFiscale(563);
			forFiscal.setMotifOuverture(MotifFor.ARRIVEE_HC);
			forFiscal.setMotifFermeture(MotifFor.DEMENAGEMENT_VD);
			hab.addForFiscal(forFiscal);
		}
		{
			ForFiscalPrincipal forFiscal = new ForFiscalPrincipal();
			forFiscal.setDateDebut(RegDate.get(2005, 8, 12));
			forFiscal.setDateFin(RegDate.get(2007, 12, 31));
			forFiscal.setGenreImpot(GenreImpot.REVENU_FORTUNE);
			forFiscal.setModeImposition(ModeImposition.ORDINAIRE);
			forFiscal.setMotifRattachement(MotifRattachement.DOMICILE);
			forFiscal.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
			forFiscal.setNumeroOfsAutoriteFiscale(564);
			forFiscal.setMotifOuverture(MotifFor.DEMENAGEMENT_VD);
			forFiscal.setMotifFermeture(MotifFor.DEPART_HC);
			hab.addForFiscal(forFiscal);
		}

		// //debugValidationResults(hab.validate());
		assertEquals(1, hab.validate().errorsCount());
	}

	/**
	 * Cas où un for intermédiaire est ouvert (date de fin = null).
	 * <p>
	 * Cas réel du contribuable n° 010.010.860 (Matthieu Argueyrolles)
	 */
	@SuppressWarnings("deprecation")
	@Test
	public void testDetectionChevauchementForsPrincipauxForIntermediateOuvert() {

		PersonnePhysique hab = new PersonnePhysique(true);
		hab.setNumero(10010860L);
		hab.setNumeroIndividu(435364L);
		{
			ForFiscalPrincipal forFiscal = new ForFiscalPrincipal();
			forFiscal.setDateDebut(RegDate.get(2003, 12, 1));
			forFiscal.setDateFin(RegDate.get(2004, 8, 11));
			forFiscal.setGenreImpot(GenreImpot.REVENU_FORTUNE);
			forFiscal.setModeImposition(ModeImposition.ORDINAIRE);
			forFiscal.setMotifRattachement(MotifRattachement.DOMICILE);
			forFiscal.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
			forFiscal.setNumeroOfsAutoriteFiscale(5601); // Chexbres
			forFiscal.setMotifOuverture(MotifFor.INDETERMINE);
			forFiscal.setMotifFermeture(MotifFor.DEMENAGEMENT_VD);
			hab.addForFiscal(forFiscal);
		}
		{
			ForFiscalPrincipal forFiscal = new ForFiscalPrincipal();
			forFiscal.setDateDebut(RegDate.get(2004, 8, 12));
			forFiscal.setDateFin(RegDate.get(2006, 10, 1));
			forFiscal.setGenreImpot(GenreImpot.REVENU_FORTUNE);
			forFiscal.setModeImposition(ModeImposition.ORDINAIRE);
			forFiscal.setMotifRattachement(MotifRattachement.DOMICILE);
			forFiscal.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
			forFiscal.setNumeroOfsAutoriteFiscale(5890); // Vevey
			forFiscal.setMotifOuverture(MotifFor.DEMENAGEMENT_VD);
			forFiscal.setMotifFermeture(MotifFor.DEMENAGEMENT_VD);
			hab.addForFiscal(forFiscal);
		}
		{ // ce for intermédiaire est ouvert => il doit entrer en conflit avec le for suivant
			ForFiscalPrincipal forFiscal = new ForFiscalPrincipal();
			forFiscal.setDateDebut(RegDate.get(2006, 10, 2));
			forFiscal.setDateFin(null);
			forFiscal.setGenreImpot(GenreImpot.REVENU_FORTUNE);
			forFiscal.setModeImposition(ModeImposition.ORDINAIRE);
			forFiscal.setMotifRattachement(MotifRattachement.DOMICILE);
			forFiscal.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
			forFiscal.setNumeroOfsAutoriteFiscale(5889); // La Tour-de-Peilz
			forFiscal.setMotifOuverture(MotifFor.DEMENAGEMENT_VD);
			forFiscal.setMotifFermeture(null);
			hab.addForFiscal(forFiscal);
		}
		{
			ForFiscalPrincipal forFiscal = new ForFiscalPrincipal();
			forFiscal.setDateDebut(RegDate.get(2006, 10, 3));
			forFiscal.setDateFin(RegDate.get(2007, 3, 30));
			forFiscal.setGenreImpot(GenreImpot.REVENU_FORTUNE);
			forFiscal.setModeImposition(ModeImposition.ORDINAIRE);
			forFiscal.setMotifRattachement(MotifRattachement.DOMICILE);
			forFiscal.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
			forFiscal.setNumeroOfsAutoriteFiscale(5889); // La Tour-de-Peilz
			forFiscal.setMotifOuverture(MotifFor.DEMENAGEMENT_VD);
			forFiscal.setMotifFermeture(MotifFor.DEMENAGEMENT_VD);
			hab.addForFiscal(forFiscal);
		}
		{
			ForFiscalPrincipal forFiscal = new ForFiscalPrincipal();
			forFiscal.setDateDebut(RegDate.get(2007, 3, 31));
			forFiscal.setDateFin(null);
			forFiscal.setGenreImpot(GenreImpot.REVENU_FORTUNE);
			forFiscal.setModeImposition(ModeImposition.ORDINAIRE);
			forFiscal.setMotifRattachement(MotifRattachement.DOMICILE);
			forFiscal.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
			forFiscal.setNumeroOfsAutoriteFiscale(5886); // Montreux
			forFiscal.setMotifOuverture(MotifFor.DEMENAGEMENT_VD);
			forFiscal.setMotifFermeture(null);
			hab.addForFiscal(forFiscal);
		}

		assertEquals(1, hab.validate().errorsCount());
	}

	/**
	 * Vérifie qu'on valide bien qu'on for secondaire doit toujours posséder un for principal pendant sa période de validité.
	 */
	@Test
	public void testValidateForSecondaireVersusForPrincipal() {

		PersonnePhysique hab = new PersonnePhysique(true);
		hab.setNumeroIndividu(1233L);
		{
			ForFiscalPrincipal forFiscal = new ForFiscalPrincipal();
			forFiscal.setDateDebut(RegDate.get(2000, 1, 1));
			forFiscal.setDateFin(RegDate.get(2005, 12, 31));
			forFiscal.setGenreImpot(GenreImpot.REVENU_FORTUNE);
			forFiscal.setModeImposition(ModeImposition.ORDINAIRE);
			forFiscal.setMotifRattachement(MotifRattachement.DOMICILE);
			forFiscal.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
			forFiscal.setNumeroOfsAutoriteFiscale(563);
			forFiscal.setMotifOuverture(MotifFor.ARRIVEE_HC);
			forFiscal.setMotifFermeture(MotifFor.DEMENAGEMENT_VD);
			hab.addForFiscal(forFiscal);
		}
		ForFiscalSecondaire forFiscalSecondaire;
		{
			forFiscalSecondaire = new ForFiscalSecondaire();
			forFiscalSecondaire.setMotifRattachement(MotifRattachement.ACTIVITE_INDEPENDANTE);
			forFiscalSecondaire.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
			forFiscalSecondaire.setNumeroOfsAutoriteFiscale(1237);
			forFiscalSecondaire.setMotifOuverture(MotifFor.DEBUT_EXPLOITATION);
			forFiscalSecondaire.setMotifFermeture(MotifFor.FIN_EXPLOITATION);
			hab.addForFiscal(forFiscalSecondaire);
		}

		// Validation OK
		forFiscalSecondaire.setDateDebut(RegDate.get(2002, 6, 1));
		forFiscalSecondaire.setDateFin(RegDate.get(2005, 12, 31));
		assertEquals(0, hab.validate().errorsCount());

		// Validation KO
		forFiscalSecondaire.setDateDebut(RegDate.get(1990, 6, 1));
		forFiscalSecondaire.setDateFin(RegDate.get(2005, 12, 31));
		assertEquals(1, hab.validate().errorsCount());
	}

	@Test
	public void testValidationAdresses() throws Exception {

		PersonnePhysique nh = new PersonnePhysique(false);
		nh.setNom("bla");

		// Adresse courier 1
		AdresseSuisse c1 = new AdresseSuisse();
		c1.setUsage(TypeAdresseTiers.COURRIER);
		c1.setDateDebut(RegDate.get(2005, 2, 3));
		c1.setDateFin(RegDate.get(2006, 11, 23));
		// Adresse courier 2
		AdresseSuisse c2 = new AdresseSuisse();
		c2.setUsage(TypeAdresseTiers.COURRIER);
		c2.setDateDebut(RegDate.get(2006, 11, 24));
		c2.setDateFin(RegDate.get(2007, 6, 12));
		// Adresse courier 3
		AdresseSuisse c3 = new AdresseSuisse();
		c3.setUsage(TypeAdresseTiers.COURRIER);
		c3.setDateDebut(RegDate.get(2007, 7, 11));
		// Adresse courier 4
		AdresseSuisse c4 = new AdresseSuisse();
		c4.setUsage(TypeAdresseTiers.COURRIER);
		c4.setDateDebut(RegDate.get(2008, 1, 2));
		// Adresse courier 5
		AdresseSuisse c5 = new AdresseSuisse();
		c5.setUsage(TypeAdresseTiers.COURRIER);
		c5.setDateDebut(RegDate.get(2006, 12, 1));
		c5.setDateFin(RegDate.get(2006, 12, 12));
		// Adresse courier 6
		AdresseSuisse c6 = new AdresseSuisse();
		c6.setUsage(TypeAdresseTiers.COURRIER);
		c6.setDateFin(RegDate.get(2006, 12, 12));
		// Adresse domicile 1
		AdresseSuisse d1 = new AdresseSuisse();
		d1.setUsage(TypeAdresseTiers.DOMICILE);
		d1.setDateDebut(RegDate.get(2005, 12, 1));
		d1.setDateFin(RegDate.get(2006, 12, 12));
		// Adresse domicile 2
		AdresseSuisse d2 = new AdresseSuisse();
		d2.setUsage(TypeAdresseTiers.DOMICILE);
		d2.setDateDebut(RegDate.get(2006, 12, 13));

		// Teste que les adresses ne se chevauchent pas
		{
			nh.setAdressesTiers(new HashSet<AdresseTiers>());
			nh.addAdresseTiers(c1);
			nh.addAdresseTiers(c2);
			nh.addAdresseTiers(c3);
			nh.addAdresseTiers(d1);
			nh.addAdresseTiers(d2);
			// //debugValidationResults(nh.validate());
			assertEquals(0, nh.validate().errorsCount());
		}
		// Teste que les adresses se chevauchent
		{
			nh.setAdressesTiers(new HashSet<AdresseTiers>());
			nh.addAdresseTiers(c1);
			nh.addAdresseTiers(c2);
			nh.addAdresseTiers(c3);
			nh.addAdresseTiers(c4);
			nh.addAdresseTiers(d1);
			nh.addAdresseTiers(d2);
			// //debugValidationResults(nh.validate());
			assertEquals(1, nh.validate().errorsCount());
		}
		// Teste que les adresses ont toute une date de debut
		{
			nh.setAdressesTiers(new HashSet<AdresseTiers>());
			nh.addAdresseTiers(d1);
			nh.addAdresseTiers(d2);
			nh.addAdresseTiers(c6);
			// //debugValidationResults(nh.validate());
			assertEquals(1, nh.validate().errorsCount());
		}
		// Une seule adresse par type peut avoir une date de fin nulle
		{
			nh.setAdressesTiers(new HashSet<AdresseTiers>());
			nh.addAdresseTiers(c3);
			nh.addAdresseTiers(c4);
			nh.addAdresseTiers(c5);
			nh.addAdresseTiers(d1);
			nh.addAdresseTiers(d2);
			// //debugValidationResults(nh.validate());
			assertEquals(1, nh.validate().errorsCount());
		}
		// Le chevauchement est autorisé par type d'adresse
		{
			nh.setAdressesTiers(new HashSet<AdresseTiers>());
			nh.addAdresseTiers(c2);
			nh.addAdresseTiers(c3);
			nh.addAdresseTiers(d1);
			nh.addAdresseTiers(d2);
			// //debugValidationResults(nh.validate());
			assertEquals(0, nh.validate().errorsCount());
		}
		// Le chevauchement est autorisé par type d'adresse
		{
			nh.setAdressesTiers(new HashSet<AdresseTiers>());
			nh.addAdresseTiers(c1);
			nh.addAdresseTiers(c2);
			nh.addAdresseTiers(c3);
			nh.addAdresseTiers(d1);
			nh.addAdresseTiers(d2);
			assertEquals(5, nh.getAdressesTiersSorted().size());
			assertEquals(2, nh.getAdressesTiersSorted(TypeAdresseTiers.DOMICILE).size());
			assertEquals(3, nh.getAdressesTiersSorted(TypeAdresseTiers.COURRIER).size());
		}
	}

	/**
	 * [UNIREG-467]
	 */
	@Test
	public void testValidationAdressesAvecAdressesAnnulees() throws Exception {

		PersonnePhysique nh = new PersonnePhysique(false);
		nh.setNom("bla");

		// Adresse courier 1 annulée
		AdresseSuisse c1 = new AdresseSuisse();
		c1.setUsage(TypeAdresseTiers.COURRIER);
		c1.setDateDebut(RegDate.get(2005, 2, 3));
		c1.setDateFin(null);
		c1.setAnnule(true);
		nh.addAdresseTiers(c1);

		// Adresse courier 2 non-annulée
		AdresseSuisse c2 = new AdresseSuisse();
		c2.setUsage(TypeAdresseTiers.COURRIER);
		c2.setDateDebut(RegDate.get(2005, 2, 2));
		c2.setDateFin(null);
		nh.addAdresseTiers(c2);

		// Pas d'erreur de validation
		assertEquals(0, nh.validate().errorsCount());
	}

	protected static void debugValidationResults(ValidationResults results) {
		for (String e : results.getErrors()) {
			LOGGER.error("E:" + e);
		}
		for (String w : results.getWarnings()) {
			LOGGER.warn("W:" + w);
		}
	}

	private PersonnePhysique createHabitantWithFors() {

		PersonnePhysique hab = new PersonnePhysique(true);
		hab.setNumero(100011010L);
		hab.setNumeroIndividu(43L);

		Set<ForFiscal> fors = new HashSet<ForFiscal>();
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
			ForFiscalPrincipal forFiscal = new ForFiscalPrincipal();
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
			ForFiscalPrincipal forFiscal = new ForFiscalPrincipal();
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
			ForFiscalPrincipal forFiscal = new ForFiscalPrincipal();
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
			ForFiscalPrincipal forFiscal = new ForFiscalPrincipal();
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
			ForFiscalPrincipal forFiscal = new ForFiscalPrincipal();
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

	private void assertForFiscauxInSortOrder(List<?> fors) {

		RegDate lastDate = null;
		for (Object o : fors) {
			ForFiscal ff = (ForFiscal) o;
			// String debut = ff.getDateDebut() != null ? ff.getDateDebut().toString() : "<null>";
			// String fin = ff.getDateFin() != null ? ff.getDateFin().toString() : "<null>";
			// String last = lastDate != null ? lastDate.toString() : "<null>";
			assertTrue("debut=" + ff.getDateDebut() + " last=" + lastDate, lastDate == null || ff.getDateDebut() == null
					|| ff.getDateDebut().isAfter(lastDate));
			lastDate = ff.getDateFin();
		}
	}

	@Test
	public void testGetForsFiscauxPrincipaux() {
		PersonnePhysique hab = createHabitantWithFors();

		List<ForFiscalPrincipal> ffps = hab.getForsFiscauxPrincipauxActifsSorted();
		assertEquals(4, ffps.size());
		assertForFiscauxInSortOrder(ffps);
	}

	@Test
	public void testGetForFiscalPrincipalAt() {
		PersonnePhysique hab = createHabitantWithFors();

		{
			ForFiscalPrincipal ffp = hab.getForFiscalPrincipalAt(RegDate.get(2001, 8, 11));
			assertNull(ffp);
		}
		{
			ForFiscalPrincipal ffp = hab.getForFiscalPrincipalAt(RegDate.get(2006, 12, 20));
			assertNotNull(ffp);
			assertEquals(new Integer(1234), ffp.getNumeroOfsAutoriteFiscale());
		}
		{
			ForFiscalPrincipal ffp = hab.getForFiscalPrincipalAt(RegDate.get(2008, 3, 1));
			assertNotNull(ffp);
			assertEquals(new Integer(563), ffp.getNumeroOfsAutoriteFiscale());
		}
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

	@Test
	public void testGetDernierForFiscalPrincipal() {
		PersonnePhysique hab = createHabitantWithFors();
		ForFiscalPrincipal ffp = hab.getDernierForFiscalPrincipal();
		assertEquals(RegDate.get(2007, 3, 2), ffp.getDateDebut());
		assertNull(ffp.getDateFin());
	}

	/**
	 * Collection vide
	 */
	@Test
	public void testExistForPrincipalListVide() {
		List<ForFiscalPrincipal> list = new ArrayList<ForFiscalPrincipal>();
		assertFalse(Tiers.existForPrincipal(list, RegDate.get(2000, 1, 1), RegDate.get(2005, 1, 1)));
		assertFalse(Tiers.existForPrincipal(list, null, RegDate.get(2005, 1, 1)));
		assertFalse(Tiers.existForPrincipal(list, RegDate.get(2000, 1, 1), null));
		assertFalse(Tiers.existForPrincipal(list, null, null));
	}

	/**
	 * 1 for principal [2000-1-1; 2004-12-31]
	 */
	@Test
	public void testExistForPrincipalSurUnFor() {
		List<ForFiscalPrincipal> list = new ArrayList<ForFiscalPrincipal>();
		{
			final ForFiscalPrincipal f = new ForFiscalPrincipal();
			f.setDateDebut(RegDate.get(2000, 1, 1));
			f.setDateFin(RegDate.get(2004, 12, 31));
			f.setModeImposition(ModeImposition.ORDINAIRE);
			list.add(f);
		}
		assertFalse(Tiers.existForPrincipal(list, null, null));
		assertFalse(Tiers.existForPrincipal(list, null, RegDate.get(1982, 12, 31)));
		assertFalse(Tiers.existForPrincipal(list, RegDate.get(1980, 1, 1), RegDate.get(1982, 12, 31)));
		assertFalse(Tiers.existForPrincipal(list, RegDate.get(1990, 1, 1), RegDate.get(2002, 12, 31)));
		assertTrue(Tiers.existForPrincipal(list, RegDate.get(2000, 1, 1), RegDate.get(2002, 12, 31)));
		assertTrue(Tiers.existForPrincipal(list, RegDate.get(2000, 1, 1), RegDate.get(2004, 12, 31)));
		assertFalse(Tiers.existForPrincipal(list, RegDate.get(2000, 1, 1), RegDate.get(2010, 12, 31)));
		assertFalse(Tiers.existForPrincipal(list, RegDate.get(2010, 1, 1), RegDate.get(2020, 12, 31)));
		assertFalse(Tiers.existForPrincipal(list, RegDate.get(1990, 1, 1), null));
		assertFalse(Tiers.existForPrincipal(list, RegDate.get(2000, 1, 1), null));
		assertFalse(Tiers.existForPrincipal(list, RegDate.get(2010, 1, 1), null));
	}

	/**
	 * 1 for principal ouvert à gauche ]null; 2004-12-31]
	 */
	@Test
	public void testExistForPrincipalSurUnForOuvertAGauche() {
		List<ForFiscalPrincipal> list = new ArrayList<ForFiscalPrincipal>();
		{
			final ForFiscalPrincipal f = new ForFiscalPrincipal();
			f.setDateDebut(null);
			f.setDateFin(RegDate.get(2004, 12, 31));
			f.setModeImposition(ModeImposition.ORDINAIRE);
			list.add(f);
		}
		assertFalse(Tiers.existForPrincipal(list, null, null));
		assertTrue(Tiers.existForPrincipal(list, null, RegDate.get(1982, 12, 31)));
		assertTrue(Tiers.existForPrincipal(list, RegDate.get(1980, 1, 1), RegDate.get(1982, 12, 31)));
		assertTrue(Tiers.existForPrincipal(list, RegDate.get(1990, 1, 1), RegDate.get(2002, 12, 31)));
		assertTrue(Tiers.existForPrincipal(list, RegDate.get(2000, 1, 1), RegDate.get(2002, 12, 31)));
		assertTrue(Tiers.existForPrincipal(list, RegDate.get(2000, 1, 1), RegDate.get(2004, 12, 31)));
		assertFalse(Tiers.existForPrincipal(list, RegDate.get(2000, 1, 1), RegDate.get(2010, 12, 31)));
		assertFalse(Tiers.existForPrincipal(list, RegDate.get(2010, 1, 1), RegDate.get(2020, 12, 31)));
		assertFalse(Tiers.existForPrincipal(list, RegDate.get(1990, 1, 1), null));
		assertFalse(Tiers.existForPrincipal(list, RegDate.get(2000, 1, 1), null));
		assertFalse(Tiers.existForPrincipal(list, RegDate.get(2010, 1, 1), null));
	}

	/**
	 * 1 for principal ouvert à droite [2000-1-1; null[
	 */
	@Test
	public void testExistForPrincipalSurUnForOuvertADroite() {
		List<ForFiscalPrincipal> list = new ArrayList<ForFiscalPrincipal>();
		{
			final ForFiscalPrincipal f = new ForFiscalPrincipal();
			f.setDateDebut(RegDate.get(2000, 1, 1));
			f.setDateFin(null);
			f.setModeImposition(ModeImposition.ORDINAIRE);
			list.add(f);
		}
		assertFalse(Tiers.existForPrincipal(list, null, null));
		assertFalse(Tiers.existForPrincipal(list, null, RegDate.get(1982, 12, 31)));
		assertFalse(Tiers.existForPrincipal(list, RegDate.get(1980, 1, 1), RegDate.get(1982, 12, 31)));
		assertFalse(Tiers.existForPrincipal(list, RegDate.get(1990, 1, 1), RegDate.get(2002, 12, 31)));
		assertTrue(Tiers.existForPrincipal(list, RegDate.get(2000, 1, 1), RegDate.get(2002, 12, 31)));
		assertTrue(Tiers.existForPrincipal(list, RegDate.get(2000, 1, 1), RegDate.get(2004, 12, 31)));
		assertTrue(Tiers.existForPrincipal(list, RegDate.get(2000, 1, 1), RegDate.get(2010, 12, 31)));
		assertTrue(Tiers.existForPrincipal(list, RegDate.get(2010, 1, 1), RegDate.get(2020, 12, 31)));
		assertFalse(Tiers.existForPrincipal(list, RegDate.get(1990, 1, 1), null));
		assertTrue(Tiers.existForPrincipal(list, RegDate.get(2000, 1, 1), null));
		assertTrue(Tiers.existForPrincipal(list, RegDate.get(2010, 1, 1), null));
	}

	/**
	 * 1 for principal ouvert des deux côtés ]null; null[
	 */
	@Test
	public void testExistForPrincipalSurUnForOuvertDesDeuxCotes() {
		List<ForFiscalPrincipal> list = new ArrayList<ForFiscalPrincipal>();
		{
			final ForFiscalPrincipal f = new ForFiscalPrincipal();
			f.setDateDebut(null);
			f.setDateFin(null);
			f.setModeImposition(ModeImposition.ORDINAIRE);
			list.add(f);
		}
		assertTrue(Tiers.existForPrincipal(list, null, null));
		assertTrue(Tiers.existForPrincipal(list, null, RegDate.get(1982, 12, 31)));
		assertTrue(Tiers.existForPrincipal(list, RegDate.get(1980, 1, 1), RegDate.get(1982, 12, 31)));
		assertTrue(Tiers.existForPrincipal(list, RegDate.get(1990, 1, 1), RegDate.get(2002, 12, 31)));
		assertTrue(Tiers.existForPrincipal(list, RegDate.get(2000, 1, 1), RegDate.get(2002, 12, 31)));
		assertTrue(Tiers.existForPrincipal(list, RegDate.get(2000, 1, 1), RegDate.get(2004, 12, 31)));
		assertTrue(Tiers.existForPrincipal(list, RegDate.get(2000, 1, 1), RegDate.get(2010, 12, 31)));
		assertTrue(Tiers.existForPrincipal(list, RegDate.get(2010, 1, 1), RegDate.get(2020, 12, 31)));
		assertTrue(Tiers.existForPrincipal(list, RegDate.get(1990, 1, 1), null));
		assertTrue(Tiers.existForPrincipal(list, RegDate.get(2000, 1, 1), null));
		assertTrue(Tiers.existForPrincipal(list, RegDate.get(2010, 1, 1), null));
	}

	/**
	 * 2 fors principaux accolés [2000-1-1; 2002-12-31] + [2003-1-1; 2004-12-31]
	 */
	@Test
	public void testExistForPrincipalSurDeuxForsAccoles() {
		List<ForFiscalPrincipal> list = new ArrayList<ForFiscalPrincipal>();
		{
			final ForFiscalPrincipal f = new ForFiscalPrincipal();
			f.setDateDebut(RegDate.get(2000, 1, 1));
			f.setDateFin(RegDate.get(2002, 12, 31));
			f.setModeImposition(ModeImposition.ORDINAIRE);
			list.add(f);
		}
		{
			final ForFiscalPrincipal f = new ForFiscalPrincipal();
			f.setDateDebut(RegDate.get(2003, 1, 1));
			f.setDateFin(RegDate.get(2004, 12, 31));
			f.setModeImposition(ModeImposition.ORDINAIRE);
			list.add(f);
		}
		assertFalse(Tiers.existForPrincipal(list, null, null));
		assertFalse(Tiers.existForPrincipal(list, null, RegDate.get(1982, 12, 31)));
		assertFalse(Tiers.existForPrincipal(list, RegDate.get(1980, 1, 1), RegDate.get(1982, 12, 31)));
		assertFalse(Tiers.existForPrincipal(list, RegDate.get(1990, 1, 1), RegDate.get(2002, 12, 31)));
		assertTrue(Tiers.existForPrincipal(list, RegDate.get(2000, 1, 1), RegDate.get(2002, 12, 31)));
		assertTrue(Tiers.existForPrincipal(list, RegDate.get(2000, 1, 1), RegDate.get(2004, 12, 31)));
		assertFalse(Tiers.existForPrincipal(list, RegDate.get(2000, 1, 1), RegDate.get(2010, 12, 31)));
		assertFalse(Tiers.existForPrincipal(list, RegDate.get(1990, 1, 1), null));
		assertFalse(Tiers.existForPrincipal(list, RegDate.get(2000, 1, 1), null));
		assertFalse(Tiers.existForPrincipal(list, RegDate.get(2010, 1, 1), null));
	}

	/**
	 * 2 fors principaux accolés ouverts à gauche ]null; 2002-12-31] + [2003-1-1; 2004-12-31]
	 */
	@Test
	public void testExistForPrincipalSurDeuxForsAccolesOuvertsAGauche() {
		List<ForFiscalPrincipal> list = new ArrayList<ForFiscalPrincipal>();
		{
			final ForFiscalPrincipal f = new ForFiscalPrincipal();
			f.setDateDebut(null);
			f.setDateFin(RegDate.get(2002, 12, 31));
			f.setModeImposition(ModeImposition.ORDINAIRE);
			list.add(f);
		}
		{
			final ForFiscalPrincipal f = new ForFiscalPrincipal();
			f.setDateDebut(RegDate.get(2003, 1, 1));
			f.setDateFin(RegDate.get(2004, 12, 31));
			f.setModeImposition(ModeImposition.ORDINAIRE);
			list.add(f);
		}
		assertFalse(Tiers.existForPrincipal(list, null, null));
		assertTrue(Tiers.existForPrincipal(list, null, RegDate.get(1982, 12, 31)));
		assertTrue(Tiers.existForPrincipal(list, RegDate.get(1980, 1, 1), RegDate.get(1982, 12, 31)));
		assertTrue(Tiers.existForPrincipal(list, RegDate.get(1990, 1, 1), RegDate.get(2002, 12, 31)));
		assertTrue(Tiers.existForPrincipal(list, RegDate.get(2000, 1, 1), RegDate.get(2002, 12, 31)));
		assertTrue(Tiers.existForPrincipal(list, RegDate.get(2000, 1, 1), RegDate.get(2004, 12, 31)));
		assertFalse(Tiers.existForPrincipal(list, RegDate.get(2000, 1, 1), RegDate.get(2010, 12, 31)));
		assertFalse(Tiers.existForPrincipal(list, RegDate.get(1990, 1, 1), null));
		assertFalse(Tiers.existForPrincipal(list, RegDate.get(2000, 1, 1), null));
		assertFalse(Tiers.existForPrincipal(list, RegDate.get(2010, 1, 1), null));
	}

	/**
	 * 2 fors principaux accolés ouverts à droite [2000-1-1; 2002-12-31] + [2003-1-1; null[
	 */
	@Test
	public void testExistForPrincipalSurDeuxForsAccolesOuvertsADroite() {
		List<ForFiscalPrincipal> list = new ArrayList<ForFiscalPrincipal>();
		{
			final ForFiscalPrincipal f = new ForFiscalPrincipal();
			f.setDateDebut(RegDate.get(2000, 1, 1));
			f.setDateFin(RegDate.get(2002, 12, 31));
			f.setModeImposition(ModeImposition.ORDINAIRE);
			list.add(f);
		}
		{
			final ForFiscalPrincipal f = new ForFiscalPrincipal();
			f.setDateDebut(RegDate.get(2003, 1, 1));
			f.setDateFin(null);
			f.setModeImposition(ModeImposition.ORDINAIRE);
			list.add(f);
		}
		assertFalse(Tiers.existForPrincipal(list, null, null));
		assertFalse(Tiers.existForPrincipal(list, null, RegDate.get(1982, 12, 31)));
		assertFalse(Tiers.existForPrincipal(list, RegDate.get(1980, 1, 1), RegDate.get(1982, 12, 31)));
		assertFalse(Tiers.existForPrincipal(list, RegDate.get(1990, 1, 1), RegDate.get(2002, 12, 31)));
		assertTrue(Tiers.existForPrincipal(list, RegDate.get(2000, 1, 1), RegDate.get(2002, 12, 31)));
		assertTrue(Tiers.existForPrincipal(list, RegDate.get(2000, 1, 1), RegDate.get(2004, 12, 31)));
		assertTrue(Tiers.existForPrincipal(list, RegDate.get(2000, 1, 1), RegDate.get(2010, 12, 31)));
		assertFalse(Tiers.existForPrincipal(list, RegDate.get(1990, 1, 1), null));
		assertTrue(Tiers.existForPrincipal(list, RegDate.get(2000, 1, 1), null));
		assertTrue(Tiers.existForPrincipal(list, RegDate.get(2010, 1, 1), null));
	}

	/**
	 * 2 fors principaux non-accolés [2000-1-1; 2002-12-31] + [2003-1-2; 2004-12-31]
	 */
	@Test
	public void testExistForPrincipalSurDeuxForsNonAccoles() {
		List<ForFiscalPrincipal> list = new ArrayList<ForFiscalPrincipal>();
		{
			final ForFiscalPrincipal f = new ForFiscalPrincipal();
			f.setDateDebut(RegDate.get(2000, 1, 1));
			f.setDateFin(RegDate.get(2002, 12, 31));
			f.setModeImposition(ModeImposition.ORDINAIRE);
			list.add(f);
		}
		{
			final ForFiscalPrincipal f = new ForFiscalPrincipal();
			f.setDateDebut(RegDate.get(2003, 1, 2));
			f.setDateFin(RegDate.get(2004, 12, 31));
			f.setModeImposition(ModeImposition.ORDINAIRE);
			list.add(f);
		}
		assertFalse(Tiers.existForPrincipal(list, null, null));
		assertFalse(Tiers.existForPrincipal(list, null, RegDate.get(1982, 12, 31)));
		assertFalse(Tiers.existForPrincipal(list, RegDate.get(1980, 1, 1), RegDate.get(1982, 12, 31)));
		assertFalse(Tiers.existForPrincipal(list, RegDate.get(1990, 1, 1), RegDate.get(2002, 12, 31)));
		assertTrue(Tiers.existForPrincipal(list, RegDate.get(2001, 1, 1), RegDate.get(2002, 12, 31)));
		assertFalse(Tiers.existForPrincipal(list, RegDate.get(2001, 1, 1), RegDate.get(2004, 12, 31)));
		assertTrue(Tiers.existForPrincipal(list, RegDate.get(2003, 1, 2), RegDate.get(2004, 12, 31)));
		assertFalse(Tiers.existForPrincipal(list, RegDate.get(2000, 1, 1), RegDate.get(2010, 12, 31)));
		assertFalse(Tiers.existForPrincipal(list, RegDate.get(1990, 1, 1), null));
		assertFalse(Tiers.existForPrincipal(list, RegDate.get(2000, 1, 1), null));
		assertFalse(Tiers.existForPrincipal(list, RegDate.get(2010, 1, 1), null));
	}

	/**
	 * 2 fors principaux non-accolés ouverts à gauche ]null; 2002-12-31] + [2003-1-2; 2004-12-31]
	 */
	@Test
	public void testExistForPrincipalSurDeuxForsNonAccolesOuvertsAGauche() {
		List<ForFiscalPrincipal> list = new ArrayList<ForFiscalPrincipal>();
		{
			final ForFiscalPrincipal f = new ForFiscalPrincipal();
			f.setDateDebut(null);
			f.setDateFin(RegDate.get(2002, 12, 31));
			f.setModeImposition(ModeImposition.ORDINAIRE);
			list.add(f);
		}
		{
			final ForFiscalPrincipal f = new ForFiscalPrincipal();
			f.setDateDebut(RegDate.get(2003, 1, 2));
			f.setDateFin(RegDate.get(2004, 12, 31));
			f.setModeImposition(ModeImposition.ORDINAIRE);
			list.add(f);
		}
		assertFalse(Tiers.existForPrincipal(list, null, null));
		assertTrue(Tiers.existForPrincipal(list, null, RegDate.get(1982, 12, 31)));
		assertTrue(Tiers.existForPrincipal(list, RegDate.get(1980, 1, 1), RegDate.get(1982, 12, 31)));
		assertTrue(Tiers.existForPrincipal(list, RegDate.get(1990, 1, 1), RegDate.get(2002, 12, 31)));
		assertTrue(Tiers.existForPrincipal(list, RegDate.get(2001, 1, 1), RegDate.get(2002, 12, 31)));
		assertFalse(Tiers.existForPrincipal(list, RegDate.get(2001, 1, 1), RegDate.get(2004, 12, 31)));
		assertTrue(Tiers.existForPrincipal(list, RegDate.get(2003, 1, 2), RegDate.get(2004, 12, 31)));
		assertFalse(Tiers.existForPrincipal(list, RegDate.get(2000, 1, 1), RegDate.get(2010, 12, 31)));
		assertFalse(Tiers.existForPrincipal(list, RegDate.get(1990, 1, 1), null));
		assertFalse(Tiers.existForPrincipal(list, RegDate.get(2000, 1, 1), null));
		assertFalse(Tiers.existForPrincipal(list, RegDate.get(2010, 1, 1), null));
	}

	/**
	 * 2 fors principaux non-accolés ouverts à droite [2000-1-1; 2002-12-31] + [2003-1-2; null[
	 */
	@Test
	public void testExistForPrincipalSurDeuxForsNonAccolesOvertsADroite() {
		List<ForFiscalPrincipal> list = new ArrayList<ForFiscalPrincipal>();
		{
			final ForFiscalPrincipal f = new ForFiscalPrincipal();
			f.setDateDebut(RegDate.get(2000, 1, 1));
			f.setDateFin(RegDate.get(2002, 12, 31));
			f.setModeImposition(ModeImposition.ORDINAIRE);
			list.add(f);
		}
		{
			final ForFiscalPrincipal f = new ForFiscalPrincipal();
			f.setDateDebut(RegDate.get(2003, 1, 2));
			f.setDateFin(null);
			f.setModeImposition(ModeImposition.ORDINAIRE);
			list.add(f);
		}
		assertFalse(Tiers.existForPrincipal(list, null, null));
		assertFalse(Tiers.existForPrincipal(list, null, RegDate.get(1982, 12, 31)));
		assertFalse(Tiers.existForPrincipal(list, RegDate.get(1980, 1, 1), RegDate.get(1982, 12, 31)));
		assertFalse(Tiers.existForPrincipal(list, RegDate.get(1990, 1, 1), RegDate.get(2002, 12, 31)));
		assertTrue(Tiers.existForPrincipal(list, RegDate.get(2001, 1, 1), RegDate.get(2002, 12, 31)));
		assertFalse(Tiers.existForPrincipal(list, RegDate.get(2001, 1, 1), RegDate.get(2004, 12, 31)));
		assertTrue(Tiers.existForPrincipal(list, RegDate.get(2003, 1, 2), RegDate.get(2004, 12, 31)));
		assertTrue(Tiers.existForPrincipal(list, RegDate.get(2003, 1, 2), RegDate.get(2010, 12, 31)));
		assertFalse(Tiers.existForPrincipal(list, RegDate.get(1990, 1, 1), null));
		assertTrue(Tiers.existForPrincipal(list, RegDate.get(2003, 1, 2), null));
		assertTrue(Tiers.existForPrincipal(list, RegDate.get(2010, 1, 1), null));
	}

	@Test
	public void testValidateForsMenageCommun() {

		/*
		 * Cas valides
		 */
		{
			// un ménage commun sans aucun for ni période de validité
			MenageCommun mc1 = new MenageCommun();
			assertFalse(mc1.validateFors().hasErrors());

			// un ménage commun sans aucun for mais avec une période de validité
			MenageCommun mc2 = new MenageCommun();
			mc2.addRapportObjet(new AppartenanceMenage(date(2000, 1, 1), date(2004, 12, 31),
					null, mc2));
			assertFalse(mc2.validateFors().hasErrors());

			// un ménage commun avec une période de validité et un for égal à cette période
			MenageCommun mc3 = new MenageCommun();
			mc3.addRapportObjet(new AppartenanceMenage(date(2000, 1, 1), date(2004, 12, 31),
					null, mc3));
			ForFiscalPrincipal f3 = new ForFiscalPrincipal(date(2000, 1, 1), date(2004, 12, 31), 1234,
					TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE);
			f3.setMotifOuverture(MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION);
			f3.setMotifFermeture(MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT);
			mc3.addForFiscal(f3);
			assertFalse(mc3.validateFors().hasErrors());

			// un ménage commun avec une période de validité et un for compris dans cette période
			MenageCommun mc4 = new MenageCommun();
			mc4.addRapportObjet(new AppartenanceMenage(date(2000, 1, 1), date(2004, 12, 31),
					null, mc4));
			ForFiscalPrincipal f4 = new ForFiscalPrincipal(date(2001, 1, 1), date(2003, 12, 31), 1234,
					TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE);
			f4.setMotifOuverture(MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION);
			f4.setMotifFermeture(MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT);
			mc4.addForFiscal(f4);
			assertFalse(mc4.validateFors().hasErrors());

			// un ménage commun avec deux périodes de validité adjacentes et un for compris dans ces deux périodes
			MenageCommun mc5 = new MenageCommun();
			mc5.addRapportObjet(new AppartenanceMenage(date(2000, 1, 1), date(2002, 12, 31),
					null, mc5));
			mc5.addRapportObjet(new AppartenanceMenage(date(2003, 1, 1), date(2004, 12, 31),
					null, mc5));
			ForFiscalPrincipal f5 = new ForFiscalPrincipal(date(2000, 1, 1), date(2004, 12, 31), 1234,
					TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE);
			f5.setMotifOuverture(MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION);
			f5.setMotifFermeture(MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT);
			mc5.addForFiscal(f5);
			assertFalse(mc5.validateFors().hasErrors());
		}

		/*
		 * Cas invalides
		 */
		{
			// un ménage commun sans période de validité mais avec un for
			MenageCommun mc1 = new MenageCommun();
			ForFiscalPrincipal f1 = new ForFiscalPrincipal(date(2000, 1, 1), date(2004, 12, 31), 1234,
					TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE);
			mc1.addForFiscal(f1);
			assertTrue(mc1.validateFors().hasErrors());

			// un ménage commun avec une période de validité et un for complétement en dehors de cette période
			MenageCommun mc2 = new MenageCommun();
			mc2.addRapportObjet(new AppartenanceMenage(date(1990, 1, 1), date(1994, 12, 31),
					null, mc2));
			ForFiscalPrincipal f2 = new ForFiscalPrincipal(date(2000, 1, 1), date(2004, 12, 31), 1234,
					TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE);
			mc2.addForFiscal(f2);
			assertTrue(mc2.validateFors().hasErrors());

			// un ménage commun avec une période de validité et un for dépassant de la période
			MenageCommun mc3 = new MenageCommun();
			mc3.addRapportObjet(new AppartenanceMenage(date(2000, 1, 1), date(2004, 12, 31),
					null, mc3));
			ForFiscalPrincipal f3 = new ForFiscalPrincipal(date(2003, 1, 1), date(2007, 12, 31), 1234,
					TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE);
			mc3.addForFiscal(f3);
			assertTrue(mc3.validateFors().hasErrors());

			// un ménage commun avec deux périodes de validité non-adjacentes et un for compris dans ces deux périodes
			MenageCommun mc4 = new MenageCommun();
			mc4.addRapportObjet(new AppartenanceMenage(date(2000, 1, 1), date(2001, 12, 31),
					null, mc4));
			mc4.addRapportObjet(new AppartenanceMenage(date(2003, 1, 1), date(2004, 12, 31),
					null, mc4));
			ForFiscalPrincipal f4 = new ForFiscalPrincipal(date(2001, 1, 1), date(2003, 12, 31), 1234,
					TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE);
			mc4.addForFiscal(f4);
			assertTrue(mc4.validateFors().hasErrors());
		}
	}

	/**
	 * Teste le cas ou un ménage précédemment fermé (fors et rapports entre tiers) est rouvert
	 * comme résultat, par exemple, d'une action d'annulation.
	 */
	@Test
	public void testReovertureForsEtRapports() {

		{
			MenageCommun mc = new MenageCommun();

			// Rapports entre tiers
			// Premier rapport entre tiers: annulé
			RapportEntreTiers ret = new AppartenanceMenage(date(1982, 12, 4), date(2008, 1, 1),
					null, mc);
			ret.setAnnule(true);
			mc.addRapportObjet(ret);
			// Deuxieme rapport: ouvert
			ret = new AppartenanceMenage(date(1982, 12, 4), null,
					null, mc);
			mc.addRapportObjet(ret);

			// Fors
			// Premier for: annulé
			ForFiscalPrincipal ffp = new ForFiscalPrincipal(date(1982, 12, 4), date(2008, 1, 1), 261,
					TypeAutoriteFiscale.COMMUNE_HC, MotifRattachement.DOMICILE, ModeImposition.SOURCE);
			ffp.setMotifOuverture(MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION);
			ffp.setMotifFermeture(MotifFor.VEUVAGE_DECES);
			ffp.setAnnule(true);
			mc.addForFiscal(ffp);
			// Deuxieme for: ouvert
			ffp = new ForFiscalPrincipal(date(1982, 12, 4), null, 261,
					TypeAutoriteFiscale.COMMUNE_HC, MotifRattachement.DOMICILE, ModeImposition.SOURCE);
			ffp.setMotifOuverture(MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION);
			ffp.setMotifFermeture(MotifFor.VEUVAGE_DECES);
			mc.addForFiscal(ffp);

			// validations
			assertFalse(mc.validateFors().hasErrors());
		}
	}

	/**
	 * Vérifie que le validator détecte bien les ménages à trois.
	 */
	@Test
	public void testDetecteMenageATroisSimultanes() {

		MenageCommun mc = new MenageCommun();
		PersonnePhysique pp1 = new PersonnePhysique();
		pp1.setNumero(1L);
		PersonnePhysique pp2 = new PersonnePhysique();
		pp2.setNumero(2L);
		PersonnePhysique pp3 = new PersonnePhysique();
		pp3.setNumero(3L);

		mc.addRapportObjet(new AppartenanceMenage(date(2000, 1, 1), null, pp1, mc));
		mc.addRapportObjet(new AppartenanceMenage(date(2000, 1, 1), null, pp2, mc));
		mc.addRapportObjet(new AppartenanceMenage(date(2000, 1, 1), null, pp3, mc));

		final ValidationResults results = mc.validate();
		assertTrue(results.hasErrors());
		assertEquals(1, results.getErrors().size());

		final String error = results.getErrors().get(0);
		assertEquals("Le ménage commun est lié avec plus de 2 personnes physiques distinctes [n°={1,2,3}]", error);
	}

	/**
	 * Vérifie que le validator détecte bien les ménages à trois.
	 */
	@Test
	public void testDetecteMenageATroisSequentiel() {

		MenageCommun mc = new MenageCommun();
		PersonnePhysique pp1 = new PersonnePhysique();
		pp1.setNumero(1L);
		PersonnePhysique pp2 = new PersonnePhysique();
		pp2.setNumero(2L);
		PersonnePhysique pp3 = new PersonnePhysique();
		pp3.setNumero(3L);

		mc.addRapportObjet(new AppartenanceMenage(date(2000, 1, 1), date(2000, 12, 31), pp1, mc));
		mc.addRapportObjet(new AppartenanceMenage(date(2002, 1, 1), date(2002, 12, 31), pp2, mc));
		mc.addRapportObjet(new AppartenanceMenage(date(2004, 1, 1), date(2004, 12, 31), pp3, mc));

		final ValidationResults results = mc.validate();
		assertTrue(results.hasErrors());
		assertEquals(1, results.getErrors().size());

		final String error = results.getErrors().get(0);
		assertEquals("Le ménage commun est lié avec plus de 2 personnes physiques distinctes [n°={1,2,3}]", error);
	}

	private static RegDate date(int year, int month, int day) {
		return RegDate.get(year, month, day);
	}
}
