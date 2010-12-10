package ch.vd.uniregctb.validation.tiers;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.adresse.AdresseSuisse;
import ch.vd.uniregctb.adresse.AdresseTiers;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.interfaces.model.mock.MockRue;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.ForFiscalAutreElementImposable;
import ch.vd.uniregctb.tiers.ForFiscalAutreImpot;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.ForFiscalSecondaire;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.GenreImpot;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.TypeAdresseTiers;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;
import ch.vd.uniregctb.validation.AbstractValidatorTest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@SuppressWarnings({"JavaDoc"})
public class PersonnePhysiqueValidatorTest extends AbstractValidatorTest<PersonnePhysique> {
	
	@Override
	protected String getValidatorBeanName() {
		return "personnePhysiqueValidator";
	}

	@Test
	public void testValidateNomVideNonHabitant() {

		final PersonnePhysique pp = new PersonnePhysique(false);

		pp.setNom(null);
		assertTrue(validate(pp).hasErrors());
		assertFalse(validate(pp).hasWarnings());

		pp.setNom("");
		assertTrue(validate(pp).hasErrors());
		assertFalse(validate(pp).hasWarnings());

		pp.setNom("  ");
		assertTrue(validate(pp).hasErrors());
		assertFalse(validate(pp).hasWarnings());

		pp.setNom("Bob");
		assertFalse(validate(pp).hasErrors());
		assertFalse(validate(pp).hasWarnings());
	}

	@Test
	public void testValidateNomCaracteresSpeciauxNonHabitant() {

		final PersonnePhysique pp = new PersonnePhysique(false);

		pp.setNom("1");
		assertFalse(validate(pp).hasErrors());
		assertTrue(validate(pp).hasWarnings());

		pp.setNom(".K");
		assertFalse(validate(pp).hasErrors());
		assertTrue(validate(pp).hasWarnings());

		pp.setNom(" Kulti");
		assertFalse(validate(pp).hasErrors());
		assertTrue(validate(pp).hasWarnings());

		pp.setNom("ŠǿůžŷķæœŒŭĠĥſ");
		assertFalse(validate(pp).hasErrors());
		assertTrue(validate(pp).hasWarnings());

		pp.setNom("'AaÀÁÂÃÄÅÆÇÈÉÊËÌÍÎÏÐÑÒÓÔÕÖØÙÚÛÜÝÞßàáâãäåæçèéêëìíîïðñòóôõöøùúûüýþÿŒœŠšŸŽž-O'Hara.//");
		assertFalse(validate(pp).hasErrors());
		assertFalse(validate(pp).hasWarnings());

		pp.setNom("AaÀÁÂÃÄÅÆÇÈÉÊËÌÍÎÏÐÑÒÓÔÕÖØÙÚÛÜÝÞßàáâãäåæçèéêëìíîïðñòóôõöøùúûüýþÿŒœŠšŸŽž-O'Hara.//");
		assertFalse(validate(pp).hasErrors());
		assertFalse(validate(pp).hasWarnings());

		pp.setNom("'AaÀÁÂÃÄÅÆÇÈÉÊËÌÍÎÏÐÑÒÓÔÕÖØÙÚÛÜÝÞßàáâãäåæçèéêëìíîïðñòóôõöøùúûüýþÿŒœŠšŸŽž - O'Hara.//");
		assertFalse(validate(pp).hasErrors());
		assertFalse(validate(pp).hasWarnings());

		pp.setNom("''AaÀÁÂÃÄÅÆÇÈÉÊËÌÍÎÏÐÑÒÓÔÕÖØÙÚÛÜÝÞßàáâãäåæçèéêëìíîïðñòóôõöøùúûüýþÿŒœŠšŸŽž-O'Hara.//");   // double guillemet en tête
		assertFalse(validate(pp).hasErrors());
		assertTrue(validate(pp).hasWarnings());

		pp.setNom("'AaÀÁÂÃÄÅÆÇÈÉÊËÌÍÎÏÐÑÒÓÔÕÖØÙÚÛÜÝÞßàáâãäåæçèéêëìíîïðñòóôõöøùúûüýþÿŒœŠšŸŽž -  O'Hara.//"); // double espace
		assertFalse(validate(pp).hasErrors());
		assertTrue(validate(pp).hasWarnings());

		pp.setNom("AaÀÁÂÃÄÅÆÇÈÉÊËÌÍÎÏÐÑÒÓÔÕÖØÙÚÛÜÝÞßàáâãäåæçèéêëìíîïðñòóôõöøùúûüýþÿŒœŠšŸŽž - O''Hara.// "); // espace final
		assertFalse(validate(pp).hasErrors());
		assertTrue(validate(pp).hasWarnings());
	}

	@Test
	public void testValidatePrenomCaracteresSpeciauxNonHabitant() {

		final PersonnePhysique pp = new PersonnePhysique(false);
		pp.setNom("Bob");

		pp.setPrenom("1");
		assertFalse(validate(pp).hasErrors());
		assertTrue(validate(pp).hasWarnings());

		pp.setPrenom(".K");
		assertFalse(validate(pp).hasErrors());
		assertTrue(validate(pp).hasWarnings());

		pp.setPrenom(" Kulti");
		assertFalse(validate(pp).hasErrors());
		assertTrue(validate(pp).hasWarnings());

		pp.setPrenom("ŠǿůžŷķæœŒŭĠĥſ");
		assertFalse(validate(pp).hasErrors());
		assertTrue(validate(pp).hasWarnings());

		pp.setPrenom("AaÀÁÂÃÄÅÆÇÈÉÊËÌÍÎÏÐÑÒÓÔÕÖØÙÚÛÜÝÞßàáâãäåæçèéêëìíîïðñòóôõöøùúûüýþÿŒœŠšŸŽž-O'Hara.//");
		assertFalse(validate(pp).hasErrors());
		assertFalse(validate(pp).hasWarnings());

		pp.setPrenom("'AaÀÁÂÃÄÅÆÇÈÉÊËÌÍÎÏÐÑÒÓÔÕÖØÙÚÛÜÝÞßàáâãäåæçèéêëìíîïðñòóôõöøùúûüýþÿŒœŠšŸŽž-O'Hara.//");
		assertFalse(validate(pp).hasErrors());
		assertFalse(validate(pp).hasWarnings());

		pp.setPrenom("'AaÀÁÂÃÄÅÆÇÈÉÊËÌÍÎÏÐÑÒÓÔÕÖØÙÚÛÜÝÞßàáâãäåæçèéêëìíîïðñòóôõöøùúûüýþÿŒœŠšŸŽž - O'Hara.//");
		assertFalse(validate(pp).hasErrors());
		assertFalse(validate(pp).hasWarnings());

		pp.setPrenom("''AaÀÁÂÃÄÅÆÇÈÉÊËÌÍÎÏÐÑÒÓÔÕÖØÙÚÛÜÝÞßàáâãäåæçèéêëìíîïðñòóôõöøùúûüýþÿŒœŠšŸŽž - O'Hara.//");  // double guillemet en tête
		assertFalse(validate(pp).hasErrors());
		assertTrue(validate(pp).hasWarnings());

		pp.setPrenom("'AaÀÁÂÃÄÅÆÇÈÉÊËÌÍÎÏÐÑÒÓÔÕÖØÙÚÛÜÝÞßàáâãäåæçèéêëìíîïðñòóôõöøùúûüýþÿŒœŠšŸŽž -  O'Hara.//");  // double espace
		assertFalse(validate(pp).hasErrors());
		assertTrue(validate(pp).hasWarnings());

		pp.setPrenom("AaÀÁÂÃÄÅÆÇÈÉÊËÌÍÎÏÐÑÒÓÔÕÖØÙÚÛÜÝÞßàáâãäåæçèéêëìíîïðñòóôõöøùúûüýþÿŒœŠšŸŽž - O''Hara.// ");  // espace final
		assertFalse(validate(pp).hasErrors());
		assertTrue(validate(pp).hasWarnings());

		pp.setPrenom(null);
		assertFalse(validate(pp).hasErrors());
		assertFalse(validate(pp).hasWarnings());
	}

	@Test
	public void testValidateForOK() {

		final PersonnePhysique hab = createHabitantWithFors();
		// debugValidationResults(hab.validate());
		assertEquals(0, validate(hab).errorsCount());
	}

	@Test
	public void testValidateForAdresseDebutApresDateFin() {

		final PersonnePhysique hab = createHabitantWithFors();
		final ForFiscalPrincipal ff = new ForFiscalPrincipal();
		ff.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
		ff.setNumeroOfsAutoriteFiscale(MockCommune.Lausanne.getNoOFSEtendu());
		ff.setMotifRattachement(MotifRattachement.DOMICILE);
		ff.setMotifOuverture(MotifFor.DEMENAGEMENT_VD);
		ff.setMotifFermeture(MotifFor.DEMENAGEMENT_VD);
		ff.setDateDebut(RegDate.get(2001, 5, 4));
		ff.setDateFin(RegDate.get(2001, 5, 3));
		ff.setModeImposition(ModeImposition.ORDINAIRE);
		hab.addForFiscal(ff);

		// debugValidationResults(hab.validate());
		assertEquals(1, validate(hab).errorsCount());
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
		assertEquals(1, validate(hab).errorsCount());
	}

	// UNIREG-601: un tiers annulé doit toujours valider
	@Test
	public void testValidateTiersAnnule() {

		// habitant qui valide
		PersonnePhysique hab = new PersonnePhysique(true);
		hab.setNumeroIndividu(1233L);
		assertEquals(0, validate(hab).errorsCount());
		assertEquals(0, validate(hab).warningsCount());

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
		assertEquals(1, validate(hab).errorsCount());
		assertEquals(0, validate(hab).warningsCount());

		// on annule le tiers => il doit de nouveau être valide
		hab.setAnnule(true);
		assertEquals(0, validate(hab).errorsCount());
		assertEquals(0, validate(hab).warningsCount());
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
		assertEquals(2, validate(hab).errorsCount());
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
		assertEquals(1, validate(hab).errorsCount());
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

		assertEquals(1, validate(hab).errorsCount());
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
		assertEquals(0, validate(hab).errorsCount());

		// Validation KO
		forFiscalSecondaire.setDateDebut(RegDate.get(1990, 6, 1));
		forFiscalSecondaire.setDateFin(RegDate.get(2005, 12, 31));
		assertEquals(1, validate(hab).errorsCount());
	}

	@Test
	public void testValidationAdresses() throws Exception {

		PersonnePhysique nh = new PersonnePhysique(false);
		nh.setNom("bla");

		// Adresse courier 1
		final AdresseSuisse c1 = new AdresseSuisse();
		c1.setNumeroRue(MockRue.Bex.RouteDuBoet.getNoRue());
		c1.setUsage(TypeAdresseTiers.COURRIER);
		c1.setDateDebut(RegDate.get(2005, 2, 3));
		c1.setDateFin(RegDate.get(2006, 11, 23));
		// Adresse courier 2
		final AdresseSuisse c2 = new AdresseSuisse();
		c2.setNumeroRue(MockRue.Bex.RouteDuBoet.getNoRue());
		c2.setUsage(TypeAdresseTiers.COURRIER);
		c2.setDateDebut(RegDate.get(2006, 11, 24));
		c2.setDateFin(RegDate.get(2007, 6, 12));
		// Adresse courier 3
		final AdresseSuisse c3 = new AdresseSuisse();
		c3.setNumeroRue(MockRue.Bex.RouteDuBoet.getNoRue());
		c3.setUsage(TypeAdresseTiers.COURRIER);
		c3.setDateDebut(RegDate.get(2007, 7, 11));
		// Adresse courier 4
		final AdresseSuisse c4 = new AdresseSuisse();
		c4.setNumeroRue(MockRue.Bex.RouteDuBoet.getNoRue());
		c4.setUsage(TypeAdresseTiers.COURRIER);
		c4.setDateDebut(RegDate.get(2008, 1, 2));
		// Adresse courier 5
		final AdresseSuisse c5 = new AdresseSuisse();
		c5.setNumeroRue(MockRue.Bex.RouteDuBoet.getNoRue());
		c5.setUsage(TypeAdresseTiers.COURRIER);
		c5.setDateDebut(RegDate.get(2006, 12, 1));
		c5.setDateFin(RegDate.get(2006, 12, 12));
		// Adresse courier 6
		final AdresseSuisse c6 = new AdresseSuisse();
		c6.setNumeroRue(MockRue.Bex.RouteDuBoet.getNoRue());
		c6.setUsage(TypeAdresseTiers.COURRIER);
		c6.setDateFin(RegDate.get(2006, 12, 12));
		// Adresse domicile 1
		final AdresseSuisse d1 = new AdresseSuisse();
		d1.setNumeroRue(MockRue.Bex.RouteDuBoet.getNoRue());
		d1.setUsage(TypeAdresseTiers.DOMICILE);
		d1.setDateDebut(RegDate.get(2005, 12, 1));
		d1.setDateFin(RegDate.get(2006, 12, 12));
		// Adresse domicile 2
		final AdresseSuisse d2 = new AdresseSuisse();
		d2.setNumeroRue(MockRue.Bex.RouteDuBoet.getNoRue());
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
			assertEquals(0, validate(nh).errorsCount());
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
			assertEquals(1, validate(nh).errorsCount());
		}
		// Teste que les adresses ont toute une date de debut
		{
			nh.setAdressesTiers(new HashSet<AdresseTiers>());
			nh.addAdresseTiers(d1);
			nh.addAdresseTiers(d2);
			nh.addAdresseTiers(c6);
			// //debugValidationResults(nh.validate());
			assertEquals(1, validate(nh).errorsCount());
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
			assertEquals(1, validate(nh).errorsCount());
		}
		// Le chevauchement est autorisé par type d'adresse
		{
			nh.setAdressesTiers(new HashSet<AdresseTiers>());
			nh.addAdresseTiers(c2);
			nh.addAdresseTiers(c3);
			nh.addAdresseTiers(d1);
			nh.addAdresseTiers(d2);
			// //debugValidationResults(nh.validate());
			assertEquals(0, validate(nh).errorsCount());
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

		final PersonnePhysique nh = new PersonnePhysique(false);
		nh.setNom("bla");

		// Adresse courier 1 annulée
		final AdresseSuisse c1 = new AdresseSuisse();
		c1.setNumeroRue(MockRue.Bex.RouteDuBoet.getNoRue());
		c1.setUsage(TypeAdresseTiers.COURRIER);
		c1.setDateDebut(RegDate.get(2005, 2, 3));
		c1.setDateFin(null);
		c1.setAnnule(true);
		nh.addAdresseTiers(c1);

		// Adresse courier 2 non-annulée
		final AdresseSuisse c2 = new AdresseSuisse();
		c2.setNumeroRue(MockRue.Bex.RouteDuBoet.getNoRue());
		c2.setUsage(TypeAdresseTiers.COURRIER);
		c2.setDateDebut(RegDate.get(2005, 2, 2));
		c2.setDateFin(null);
		nh.addAdresseTiers(c2);

		// Pas d'erreur de validation
		assertEquals(0, validate(nh).errorsCount());
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
}
