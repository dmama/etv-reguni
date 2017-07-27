package ch.vd.uniregctb.validation.tiers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.uniregctb.adresse.AdresseSuisse;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.declaration.ModeleDocument;
import ch.vd.uniregctb.declaration.PeriodeFiscale;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.ForFiscalAutreElementImposable;
import ch.vd.uniregctb.tiers.ForFiscalAutreImpot;
import ch.vd.uniregctb.tiers.ForFiscalPrincipalPP;
import ch.vd.uniregctb.tiers.ForFiscalSecondaire;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.GenreImpot;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypeAdresseTiers;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;
import ch.vd.uniregctb.type.TypeContribuable;
import ch.vd.uniregctb.type.TypeDocument;
import ch.vd.uniregctb.validation.AbstractValidatorTest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@SuppressWarnings({"JavaDoc"})
public class PersonnePhysiqueValidatorTest extends AbstractValidatorTest<PersonnePhysique> {

	@Override
	protected String getValidatorBeanName() {
		return "personnePhysiqueValidator";
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
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
	@Transactional(rollbackFor = Throwable.class)
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
	@Transactional(rollbackFor = Throwable.class)
	public void testValidatePrenomCaracteresSpeciauxNonHabitant() {

		final PersonnePhysique pp = new PersonnePhysique(false);
		pp.setNom("Bob");

		pp.setPrenomUsuel("1");
		assertFalse(validate(pp).hasErrors());
		assertTrue(validate(pp).hasWarnings());

		pp.setPrenomUsuel(".K");
		assertFalse(validate(pp).hasErrors());
		assertTrue(validate(pp).hasWarnings());

		pp.setPrenomUsuel(" Kulti");
		assertFalse(validate(pp).hasErrors());
		assertTrue(validate(pp).hasWarnings());

		pp.setPrenomUsuel("ŠǿůžŷķæœŒŭĠĥſ");
		assertFalse(validate(pp).hasErrors());
		assertTrue(validate(pp).hasWarnings());

		pp.setPrenomUsuel("AaÀÁÂÃÄÅÆÇÈÉÊËÌÍÎÏÐÑÒÓÔÕÖØÙÚÛÜÝÞßàáâãäåæçèéêëìíîïðñòóôõöøùúûüýþÿŒœŠšŸŽž-O'Hara.//");
		assertFalse(validate(pp).hasErrors());
		assertFalse(validate(pp).hasWarnings());

		pp.setPrenomUsuel("'AaÀÁÂÃÄÅÆÇÈÉÊËÌÍÎÏÐÑÒÓÔÕÖØÙÚÛÜÝÞßàáâãäåæçèéêëìíîïðñòóôõöøùúûüýþÿŒœŠšŸŽž-O'Hara.//");
		assertFalse(validate(pp).hasErrors());
		assertFalse(validate(pp).hasWarnings());

		pp.setPrenomUsuel("'AaÀÁÂÃÄÅÆÇÈÉÊËÌÍÎÏÐÑÒÓÔÕÖØÙÚÛÜÝÞßàáâãäåæçèéêëìíîïðñòóôõöøùúûüýþÿŒœŠšŸŽž - O'Hara.//");
		assertFalse(validate(pp).hasErrors());
		assertFalse(validate(pp).hasWarnings());

		pp.setPrenomUsuel("''AaÀÁÂÃÄÅÆÇÈÉÊËÌÍÎÏÐÑÒÓÔÕÖØÙÚÛÜÝÞßàáâãäåæçèéêëìíîïðñòóôõöøùúûüýþÿŒœŠšŸŽž - O'Hara.//");  // double guillemet en tête
		assertFalse(validate(pp).hasErrors());
		assertTrue(validate(pp).hasWarnings());

		pp.setPrenomUsuel("'AaÀÁÂÃÄÅÆÇÈÉÊËÌÍÎÏÐÑÒÓÔÕÖØÙÚÛÜÝÞßàáâãäåæçèéêëìíîïðñòóôõöøùúûüýþÿŒœŠšŸŽž -  O'Hara.//");  // double espace
		assertFalse(validate(pp).hasErrors());
		assertTrue(validate(pp).hasWarnings());

		pp.setPrenomUsuel("AaÀÁÂÃÄÅÆÇÈÉÊËÌÍÎÏÐÑÒÓÔÕÖØÙÚÛÜÝÞßàáâãäåæçèéêëìíîïðñòóôõöøùúûüýþÿŒœŠšŸŽž - O''Hara.// ");  // espace final
		assertFalse(validate(pp).hasErrors());
		assertTrue(validate(pp).hasWarnings());

		pp.setPrenomUsuel(null);
		assertFalse(validate(pp).hasErrors());
		assertFalse(validate(pp).hasWarnings());
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testValidateForOK() {

		final PersonnePhysique hab = createHabitantWithFors();
		// debugValidationResults(hab.validate());
		assertValidation(null, null, validate(hab));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testValidateForAdresseDebutApresDateFin() {

		final PersonnePhysique hab = createHabitantWithFors();
		final ForFiscalPrincipalPP ff = new ForFiscalPrincipalPP();
		ff.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
		ff.setNumeroOfsAutoriteFiscale(MockCommune.Lausanne.getNoOFS());
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
	@Transactional(rollbackFor = Throwable.class)
	public void testValidateMotifRattachement() {

		PersonnePhysique hab = createHabitantWithFors();
		ForFiscalPrincipalPP ff = new ForFiscalPrincipalPP();
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
	@Transactional(rollbackFor = Throwable.class)
	public void testValidateTiersAnnule() {

		// habitant qui valide
		PersonnePhysique hab = new PersonnePhysique(true);
		hab.setNumeroIndividu(1233L);
		assertEquals(0, validate(hab).errorsCount());
		assertEquals(0, validate(hab).warningsCount());

		// on ajoute un for invalide (dateDebut > dateFin)
		ForFiscalPrincipalPP forFiscal = new ForFiscalPrincipalPP();
		forFiscal.setDateDebut(RegDate.get(2005, 8, 12));
		forFiscal.setDateFin(RegDate.get(1995, 2, 28));
		forFiscal.setGenreImpot(GenreImpot.REVENU_FORTUNE);
		forFiscal.setModeImposition(ModeImposition.ORDINAIRE);
		forFiscal.setMotifRattachement(MotifRattachement.DOMICILE);
		forFiscal.setTypeAutoriteFiscale(TypeAutoriteFiscale.PAYS_HS);
		forFiscal.setNumeroOfsAutoriteFiscale(MockPays.Albanie.getNoOFS());
		hab.addForFiscal(forFiscal);
		assertEquals(1, validate(hab).errorsCount());
		assertEquals(0, validate(hab).warningsCount());

		// on annule le tiers => il doit de nouveau être valide
		hab.setAnnule(true);
		assertEquals(0, validate(hab).errorsCount());
		assertEquals(0, validate(hab).warningsCount());
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetectionChevauchementForsPrincipaux() {

		PersonnePhysique hab = new PersonnePhysique(true);
		hab.setNumeroIndividu(1233L);
		// 2005, 8, 12 - 2007, 2, 28
		{
			ForFiscalPrincipalPP forFiscal = new ForFiscalPrincipalPP();
			forFiscal.setDateDebut(RegDate.get(2005, 8, 12));
			forFiscal.setDateFin(RegDate.get(2007, 2, 28));
			forFiscal.setGenreImpot(GenreImpot.REVENU_FORTUNE);
			forFiscal.setModeImposition(ModeImposition.ORDINAIRE);
			forFiscal.setMotifRattachement(MotifRattachement.DOMICILE);
			forFiscal.setTypeAutoriteFiscale(TypeAutoriteFiscale.PAYS_HS);
			forFiscal.setNumeroOfsAutoriteFiscale(MockPays.Allemagne.getNoOFS());
			hab.addForFiscal(forFiscal);
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
			forFiscal.setNumeroOfsAutoriteFiscale(MockCommune.Bern.getNoOFS());
			hab.addForFiscal(forFiscal);
		}
		// 2007, 3, 2 -> Ouvert
		{
			ForFiscalPrincipalPP forFiscal = new ForFiscalPrincipalPP();
			forFiscal.setDateDebut(RegDate.get(2007, 3, 2));
			forFiscal.setGenreImpot(GenreImpot.REVENU_FORTUNE);
			forFiscal.setModeImposition(ModeImposition.ORDINAIRE);
			forFiscal.setMotifRattachement(MotifRattachement.DOMICILE);
			forFiscal.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_HC);
			forFiscal.setNumeroOfsAutoriteFiscale(MockCommune.Bern.getNoOFS());
			hab.addForFiscal(forFiscal);
		}

		// Ajout d'un for qui chevauche
		{
			ForFiscalPrincipalPP forFiscal = new ForFiscalPrincipalPP();
			forFiscal.setDateDebut(RegDate.get(2007, 1, 22));
			forFiscal.setDateFin(RegDate.get(2007, 4, 1));
			forFiscal.setMotifRattachement(MotifRattachement.DOMICILE);
			forFiscal.setTypeAutoriteFiscale(TypeAutoriteFiscale.PAYS_HS);
			forFiscal.setNumeroOfsAutoriteFiscale(MockPays.Allemagne.getNoOFS());
			forFiscal.setModeImposition(ModeImposition.ORDINAIRE);
			hab.addForFiscal(forFiscal);
		}

		// debugValidationResults(hab.validate());
		final List<String> erreurs = new ArrayList<>();
		erreurs.add("Le for principal qui commence le 22.01.2007 chevauche le for précédent");
		erreurs.add("Le for principal qui commence le 01.03.2007 chevauche le for précédent");
		assertValidation(erreurs, null, validate(hab));
	}

	/**
	 * Teste que le chevauchement sur un seul jour est détecté (cas limite).
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetectionChevauchementForsPrincipauxCasLimite() {

		PersonnePhysique hab = new PersonnePhysique(true);
		hab.setNumeroIndividu(1233L);
		{
			ForFiscalPrincipalPP forFiscal = new ForFiscalPrincipalPP();
			forFiscal.setDateDebut(RegDate.get(2002, 1, 1));
			forFiscal.setDateFin(RegDate.get(2005, 8, 12));
			forFiscal.setGenreImpot(GenreImpot.REVENU_FORTUNE);
			forFiscal.setModeImposition(ModeImposition.ORDINAIRE);
			forFiscal.setMotifRattachement(MotifRattachement.DOMICILE);
			forFiscal.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
			forFiscal.setNumeroOfsAutoriteFiscale(MockCommune.Aigle.getNoOFS());
			forFiscal.setMotifOuverture(MotifFor.ARRIVEE_HC);
			forFiscal.setMotifFermeture(MotifFor.DEMENAGEMENT_VD);
			hab.addForFiscal(forFiscal);
		}
		{
			ForFiscalPrincipalPP forFiscal = new ForFiscalPrincipalPP();
			forFiscal.setDateDebut(RegDate.get(2005, 8, 12));
			forFiscal.setDateFin(RegDate.get(2007, 12, 31));
			forFiscal.setGenreImpot(GenreImpot.REVENU_FORTUNE);
			forFiscal.setModeImposition(ModeImposition.ORDINAIRE);
			forFiscal.setMotifRattachement(MotifRattachement.DOMICILE);
			forFiscal.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
			forFiscal.setNumeroOfsAutoriteFiscale(MockCommune.Aigle.getNoOFS());
			forFiscal.setMotifOuverture(MotifFor.DEMENAGEMENT_VD);
			forFiscal.setMotifFermeture(MotifFor.DEPART_HC);
			hab.addForFiscal(forFiscal);
		}

		// //debugValidationResults(hab.validate());
		assertValidation(Collections.singletonList("Le for principal qui commence le 12.08.2005 chevauche le for précédent"), null, validate(hab));
	}

	/**
	 * Cas où un for intermédiaire est ouvert (date de fin = null).
	 * <p/>
	 * Cas réel du contribuable n° 010.010.860 (Matthieu Argueyrolles)
	 */
	@SuppressWarnings("deprecation")
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetectionChevauchementForsPrincipauxForIntermediateOuvert() {

		PersonnePhysique hab = new PersonnePhysique(true);
		hab.setNumero(10010860L);
		hab.setNumeroIndividu(435364L);
		{
			ForFiscalPrincipalPP forFiscal = new ForFiscalPrincipalPP();
			forFiscal.setDateDebut(RegDate.get(2003, 12, 1));
			forFiscal.setDateFin(RegDate.get(2004, 8, 11));
			forFiscal.setGenreImpot(GenreImpot.REVENU_FORTUNE);
			forFiscal.setModeImposition(ModeImposition.ORDINAIRE);
			forFiscal.setMotifRattachement(MotifRattachement.DOMICILE);
			forFiscal.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
			forFiscal.setNumeroOfsAutoriteFiscale(MockCommune.Croy.getNoOFS());
			forFiscal.setMotifOuverture(MotifFor.INDETERMINE);
			forFiscal.setMotifFermeture(MotifFor.DEMENAGEMENT_VD);
			hab.addForFiscal(forFiscal);
		}
		{
			ForFiscalPrincipalPP forFiscal = new ForFiscalPrincipalPP();
			forFiscal.setDateDebut(RegDate.get(2004, 8, 12));
			forFiscal.setDateFin(RegDate.get(2006, 10, 1));
			forFiscal.setGenreImpot(GenreImpot.REVENU_FORTUNE);
			forFiscal.setModeImposition(ModeImposition.ORDINAIRE);
			forFiscal.setMotifRattachement(MotifRattachement.DOMICILE);
			forFiscal.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
			forFiscal.setNumeroOfsAutoriteFiscale(MockCommune.Vevey.getNoOFS());
			forFiscal.setMotifOuverture(MotifFor.DEMENAGEMENT_VD);
			forFiscal.setMotifFermeture(MotifFor.DEMENAGEMENT_VD);
			hab.addForFiscal(forFiscal);
		}
		{ // ce for intermédiaire est ouvert => il doit entrer en conflit avec le for suivant
			ForFiscalPrincipalPP forFiscal = new ForFiscalPrincipalPP();
			forFiscal.setDateDebut(RegDate.get(2006, 10, 2));
			forFiscal.setDateFin(null);
			forFiscal.setGenreImpot(GenreImpot.REVENU_FORTUNE);
			forFiscal.setModeImposition(ModeImposition.ORDINAIRE);
			forFiscal.setMotifRattachement(MotifRattachement.DOMICILE);
			forFiscal.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
			forFiscal.setNumeroOfsAutoriteFiscale(MockCommune.Aubonne.getNoOFS());
			forFiscal.setMotifOuverture(MotifFor.DEMENAGEMENT_VD);
			forFiscal.setMotifFermeture(null);
			hab.addForFiscal(forFiscal);
		}
		{
			ForFiscalPrincipalPP forFiscal = new ForFiscalPrincipalPP();
			forFiscal.setDateDebut(RegDate.get(2006, 10, 3));
			forFiscal.setDateFin(RegDate.get(2007, 3, 30));
			forFiscal.setGenreImpot(GenreImpot.REVENU_FORTUNE);
			forFiscal.setModeImposition(ModeImposition.ORDINAIRE);
			forFiscal.setMotifRattachement(MotifRattachement.DOMICILE);
			forFiscal.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
			forFiscal.setNumeroOfsAutoriteFiscale(MockCommune.Aubonne.getNoOFS());
			forFiscal.setMotifOuverture(MotifFor.DEMENAGEMENT_VD);
			forFiscal.setMotifFermeture(MotifFor.DEMENAGEMENT_VD);
			hab.addForFiscal(forFiscal);
		}
		{
			ForFiscalPrincipalPP forFiscal = new ForFiscalPrincipalPP();
			forFiscal.setDateDebut(RegDate.get(2007, 3, 31));
			forFiscal.setDateFin(null);
			forFiscal.setGenreImpot(GenreImpot.REVENU_FORTUNE);
			forFiscal.setModeImposition(ModeImposition.ORDINAIRE);
			forFiscal.setMotifRattachement(MotifRattachement.DOMICILE);
			forFiscal.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
			forFiscal.setNumeroOfsAutoriteFiscale(MockCommune.CheseauxSurLausanne.getNoOFS());
			forFiscal.setMotifOuverture(MotifFor.DEMENAGEMENT_VD);
			forFiscal.setMotifFermeture(null);
			hab.addForFiscal(forFiscal);
		}

		assertValidation(Collections.singletonList("Le for principal qui commence le 03.10.2006 chevauche le for précédent"), null, validate(hab));
	}

	/**
	 * Vérifie qu'on valide bien qu'on for secondaire doit toujours posséder un for principal pendant sa période de validité.
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testValidateForSecondaireVersusForPrincipal() {

		PersonnePhysique hab = new PersonnePhysique(true);
		hab.setNumeroIndividu(1233L);
		{
			ForFiscalPrincipalPP forFiscal = new ForFiscalPrincipalPP();
			forFiscal.setDateDebut(RegDate.get(2000, 1, 1));
			forFiscal.setDateFin(RegDate.get(2005, 12, 31));
			forFiscal.setGenreImpot(GenreImpot.REVENU_FORTUNE);
			forFiscal.setModeImposition(ModeImposition.ORDINAIRE);
			forFiscal.setMotifRattachement(MotifRattachement.DOMICILE);
			forFiscal.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
			forFiscal.setNumeroOfsAutoriteFiscale(MockCommune.Lausanne.getNoOFS());
			forFiscal.setMotifOuverture(MotifFor.ARRIVEE_HC);
			forFiscal.setMotifFermeture(MotifFor.DEMENAGEMENT_VD);
			hab.addForFiscal(forFiscal);
		}
		ForFiscalSecondaire forFiscalSecondaire;
		{
			forFiscalSecondaire = new ForFiscalSecondaire();
			forFiscalSecondaire.setMotifRattachement(MotifRattachement.ACTIVITE_INDEPENDANTE);
			forFiscalSecondaire.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
			forFiscalSecondaire.setNumeroOfsAutoriteFiscale(MockCommune.Bussigny.getNoOFS());
			forFiscalSecondaire.setMotifOuverture(MotifFor.DEBUT_EXPLOITATION);
			forFiscalSecondaire.setMotifFermeture(MotifFor.FIN_EXPLOITATION);
			hab.addForFiscal(forFiscalSecondaire);
		}

		// Validation OK
		forFiscalSecondaire.setDateDebut(RegDate.get(2002, 6, 1));
		forFiscalSecondaire.setDateFin(RegDate.get(2005, 12, 31));
		assertValidation(null, null, validate(hab));

		// Validation KO
		forFiscalSecondaire.setDateDebut(RegDate.get(1990, 6, 1));
		forFiscalSecondaire.setDateFin(RegDate.get(2005, 12, 31));
		assertValidation(Collections.singletonList("Il n'y a pas de for principal pour accompagner le for secondaire qui commence le 01.06.1990 et se termine le 31.12.2005"), null, validate(hab));
	}

	@Test
	public void testValidateModeImposition() {

		final PersonnePhysique hab = new PersonnePhysique(true);
		hab.setNumeroIndividu(1233L);

		final ForFiscalPrincipalPP ffp = new ForFiscalPrincipalPP();
		ffp.setDateDebut(RegDate.get(2000, 1, 1));
		ffp.setDateFin(RegDate.get(2005, 12, 31));
		ffp.setGenreImpot(GenreImpot.REVENU_FORTUNE);
		ffp.setMotifRattachement(MotifRattachement.DOMICILE);
		ffp.setMotifOuverture(MotifFor.ARRIVEE_HC);
		ffp.setMotifFermeture(MotifFor.DEMENAGEMENT_VD);
		hab.addForFiscal(ffp);

		final Map<TypeAutoriteFiscale, Integer> noOfsForExemplePourTypeAutoriteFiscale = new EnumMap<>(TypeAutoriteFiscale.class);
		noOfsForExemplePourTypeAutoriteFiscale.put(TypeAutoriteFiscale.COMMUNE_HC, MockCommune.Neuchatel.getNoOFS());
		noOfsForExemplePourTypeAutoriteFiscale.put(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Echallens.getNoOFS());
		noOfsForExemplePourTypeAutoriteFiscale.put(TypeAutoriteFiscale.PAYS_HS, MockPays.Allemagne.getNoOFS());

		// Sans for secondaire
		for (TypeAutoriteFiscale taf : TypeAutoriteFiscale.values()) {

			ffp.setNumeroOfsAutoriteFiscale(noOfsForExemplePourTypeAutoriteFiscale.get(taf));
			ffp.setTypeAutoriteFiscale(taf);

			// Source sans fors secondaire : ok
			{
				ffp.setModeImposition(ModeImposition.SOURCE);
				final ValidationResults vr = validate(hab);
				assertFalse(taf.name(), vr.hasWarnings());
				assertFalse(taf.name(), vr.hasErrors());
			}
		}

		// on deuxième fors principal pour couvrir le for secondaire dans certains cas ci-dessous
		final ForFiscalPrincipalPP ffp2 = new ForFiscalPrincipalPP();
		ffp2.setDateDebut(RegDate.get(2006, 1, 1));
		ffp2.setGenreImpot(GenreImpot.REVENU_FORTUNE);
		ffp2.setMotifRattachement(MotifRattachement.DOMICILE);
		ffp2.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_HC);
		ffp2.setModeImposition(ModeImposition.SOURCE);
		ffp2.setNumeroOfsAutoriteFiscale(MockCommune.Neuchatel.getNoOFS());
		ffp2.setMotifOuverture(MotifFor.ARRIVEE_HC);
		hab.addForFiscal(ffp2);

		final ForFiscalSecondaire ffs = new ForFiscalSecondaire();
		ffs.setMotifRattachement(MotifRattachement.ACTIVITE_INDEPENDANTE);
		ffs.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
		ffs.setNumeroOfsAutoriteFiscale(MockCommune.Bussigny.getNoOFS());
		ffs.setMotifOuverture(MotifFor.DEBUT_EXPLOITATION);
		hab.addForFiscal(ffs);

		// Avec for secondaire
		for (TypeAutoriteFiscale taf : TypeAutoriteFiscale.values()) {

			ffp.setNumeroOfsAutoriteFiscale(noOfsForExemplePourTypeAutoriteFiscale.get(taf));
			ffp.setTypeAutoriteFiscale(taf);

			// Source avec fors secondaire (intersection complète) : warning
			{
				ffs.setDateDebut(RegDate.get(2000, 1, 1));
				ffs.setDateFin(RegDate.get(2005, 12, 31));
				ffs.setMotifFermeture(MotifFor.FIN_EXPLOITATION);
				ffp.setModeImposition(ModeImposition.SOURCE);

				final ValidationResults vr = validate(hab);
				assertFalse(taf.name(), vr.hasErrors());
				assertTrue(taf.name(), vr.hasWarnings());

				final List<String> warnings = vr.getWarnings();
				assertEquals(taf.name(), 1, warnings.size());
				assertEquals(taf.name(), "Le mode d'imposition \"source\" du for principal qui commence le 01.01.2000 est anormal en présence de fors secondaires", warnings.get(0));
			}

			// Source avec fors secondaire (intersection partielle) : warning
			{
				ffs.setDateDebut(RegDate.get(2000, 1, 1));
				ffs.setDateFin(RegDate.get(2002, 12, 31));
				ffp.setModeImposition(ModeImposition.SOURCE);

				final ValidationResults vr = validate(hab);
				assertFalse(taf.name(), vr.hasErrors());
				assertTrue(taf.name(), vr.hasWarnings());

				final List<String> warnings = vr.getWarnings();
				assertEquals(taf.name(), 1, warnings.size());
				assertEquals(taf.name(), "Le mode d'imposition \"source\" du for principal qui commence le 01.01.2000 est anormal en présence de fors secondaires", warnings.get(0));
			}

			// Source avec fors secondaire (pas d'intersection) : ok
			{
				ffp2.setModeImposition(ModeImposition.ORDINAIRE); // pour aller avec le for secondaire

				ffs.setDateDebut(RegDate.get(2006, 1, 1));
				ffs.setDateFin(null);
				ffs.setMotifFermeture(null);
				ffp.setModeImposition(ModeImposition.SOURCE);

				final ValidationResults vr = validate(hab);
				assertFalse(taf.name(), vr.hasWarnings());
				assertFalse(taf.name(), vr.hasErrors());

				ffp2.setModeImposition(ModeImposition.SOURCE);
			}
		}
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testValidationAdresses() throws Exception {

		PersonnePhysique nh = new PersonnePhysique(false);
		nh.setNom("bla");

		// Adresse courier 1
		final AdresseSuisse c1 = new AdresseSuisse();
		c1.setNumeroRue(MockRue.Bex.CheminDeLaForet.getNoRue());
		c1.setNumeroOrdrePoste(MockRue.Bex.CheminDeLaForet.getNoLocalite());
		c1.setUsage(TypeAdresseTiers.COURRIER);
		c1.setDateDebut(RegDate.get(2005, 2, 3));
		c1.setDateFin(RegDate.get(2006, 11, 23));
		// Adresse courier 2
		final AdresseSuisse c2 = new AdresseSuisse();
		c2.setNumeroRue(MockRue.Bex.CheminDeLaForet.getNoRue());
		c2.setNumeroOrdrePoste(MockRue.Bex.CheminDeLaForet.getNoLocalite());
		c2.setUsage(TypeAdresseTiers.COURRIER);
		c2.setDateDebut(RegDate.get(2006, 11, 24));
		c2.setDateFin(RegDate.get(2007, 6, 12));
		// Adresse courier 3
		final AdresseSuisse c3 = new AdresseSuisse();
		c3.setNumeroRue(MockRue.Bex.CheminDeLaForet.getNoRue());
		c3.setNumeroOrdrePoste(MockRue.Bex.CheminDeLaForet.getNoLocalite());
		c3.setUsage(TypeAdresseTiers.COURRIER);
		c3.setDateDebut(RegDate.get(2007, 7, 11));
		// Adresse courier 4
		final AdresseSuisse c4 = new AdresseSuisse();
		c4.setNumeroRue(MockRue.Bex.CheminDeLaForet.getNoRue());
		c4.setNumeroOrdrePoste(MockRue.Bex.CheminDeLaForet.getNoLocalite());
		c4.setUsage(TypeAdresseTiers.COURRIER);
		c4.setDateDebut(RegDate.get(2008, 1, 2));
		// Adresse courier 5
		final AdresseSuisse c5 = new AdresseSuisse();
		c5.setNumeroRue(MockRue.Bex.CheminDeLaForet.getNoRue());
		c5.setNumeroOrdrePoste(MockRue.Bex.CheminDeLaForet.getNoLocalite());
		c5.setUsage(TypeAdresseTiers.COURRIER);
		c5.setDateDebut(RegDate.get(2006, 12, 1));
		c5.setDateFin(RegDate.get(2006, 12, 12));
		// Adresse courier 6
		final AdresseSuisse c6 = new AdresseSuisse();
		c6.setNumeroRue(MockRue.Bex.CheminDeLaForet.getNoRue());
		c6.setNumeroOrdrePoste(MockRue.Bex.CheminDeLaForet.getNoLocalite());
		c6.setUsage(TypeAdresseTiers.COURRIER);
		c6.setDateFin(RegDate.get(2006, 12, 12));
		// Adresse domicile 1
		final AdresseSuisse d1 = new AdresseSuisse();
		d1.setNumeroRue(MockRue.Bex.CheminDeLaForet.getNoRue());
		d1.setNumeroOrdrePoste(MockRue.Bex.CheminDeLaForet.getNoLocalite());
		d1.setUsage(TypeAdresseTiers.DOMICILE);
		d1.setDateDebut(RegDate.get(2005, 12, 1));
		d1.setDateFin(RegDate.get(2006, 12, 12));
		// Adresse domicile 2
		final AdresseSuisse d2 = new AdresseSuisse();
		d2.setNumeroRue(MockRue.Bex.CheminDeLaForet.getNoRue());
		d2.setNumeroOrdrePoste(MockRue.Bex.CheminDeLaForet.getNoLocalite());
		d2.setUsage(TypeAdresseTiers.DOMICILE);
		d2.setDateDebut(RegDate.get(2006, 12, 13));

		// Teste que les adresses ne se chevauchent pas
		{
			nh.setAdressesTiers(new HashSet<>());
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
			nh.setAdressesTiers(new HashSet<>());
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
			nh.setAdressesTiers(new HashSet<>());
			nh.addAdresseTiers(d1);
			nh.addAdresseTiers(d2);
			nh.addAdresseTiers(c6);
			// //debugValidationResults(nh.validate());
			assertEquals(1, validate(nh).errorsCount());
		}
		// Une seule adresse par type peut avoir une date de fin nulle
		{
			nh.setAdressesTiers(new HashSet<>());
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
			nh.setAdressesTiers(new HashSet<>());
			nh.addAdresseTiers(c2);
			nh.addAdresseTiers(c3);
			nh.addAdresseTiers(d1);
			nh.addAdresseTiers(d2);
			// //debugValidationResults(nh.validate());
			assertEquals(0, validate(nh).errorsCount());
		}
		// Le chevauchement est autorisé par type d'adresse
		{
			nh.setAdressesTiers(new HashSet<>());
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
	@Transactional(rollbackFor = Throwable.class)
	public void testValidationAdressesAvecAdressesAnnulees() throws Exception {

		final PersonnePhysique nh = new PersonnePhysique(false);
		nh.setNom("bla");

		// Adresse courier 1 annulée
		final AdresseSuisse c1 = new AdresseSuisse();
		c1.setNumeroRue(MockRue.Bex.CheminDeLaForet.getNoRue());
		c1.setNumeroOrdrePoste(MockRue.Bex.CheminDeLaForet.getNoLocalite());
		c1.setUsage(TypeAdresseTiers.COURRIER);
		c1.setDateDebut(RegDate.get(2005, 2, 3));
		c1.setDateFin(null);
		c1.setAnnule(true);
		nh.addAdresseTiers(c1);

		// Adresse courier 2 non-annulée
		final AdresseSuisse c2 = new AdresseSuisse();
		c2.setNumeroRue(MockRue.Bex.CheminDeLaForet.getNoRue());
		c2.setNumeroOrdrePoste(MockRue.Bex.CheminDeLaForet.getNoLocalite());
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

		Set<ForFiscal> fors = new HashSet<>();
		{
			ForFiscalAutreImpot forFiscal = new ForFiscalAutreImpot();
			forFiscal.setGenreImpot(GenreImpot.DROIT_MUTATION);
			forFiscal.setDateDebut(RegDate.get(2004, 3, 1));
			forFiscal.setDateFin(RegDate.get(2006, 2, 28));
			forFiscal.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
			forFiscal.setNumeroOfsAutoriteFiscale(MockCommune.CheseauxSurLausanne.getNoOFS());
			fors.add(forFiscal);
		}
		{
			ForFiscalAutreElementImposable forFiscal = new ForFiscalAutreElementImposable();
			forFiscal.setMotifRattachement(MotifRattachement.ACTIVITE_LUCRATIVE_CAS);
			forFiscal.setDateDebut(RegDate.get(2006, 6, 1));
			forFiscal.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
			forFiscal.setNumeroOfsAutoriteFiscale(MockCommune.CheseauxSurLausanne.getNoOFS());
			forFiscal.setMotifOuverture(MotifFor.DEBUT_EXPLOITATION);
			fors.add(forFiscal);
		}
		{
			ForFiscalSecondaire forFiscal = new ForFiscalSecondaire();
			forFiscal.setMotifRattachement(MotifRattachement.ACTIVITE_INDEPENDANTE);
			forFiscal.setDateDebut(RegDate.get(2002, 6, 1));
			forFiscal.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
			forFiscal.setNumeroOfsAutoriteFiscale(MockCommune.CheseauxSurLausanne.getNoOFS());
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
			forFiscal.setNumeroOfsAutoriteFiscale(MockCommune.CheseauxSurLausanne.getNoOFS());
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
			forFiscal.setNumeroOfsAutoriteFiscale(MockCommune.CheseauxSurLausanne.getNoOFS());
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
			forFiscal.setNumeroOfsAutoriteFiscale(MockPays.Allemagne.getNoOFS());
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
			forFiscal.setNumeroOfsAutoriteFiscale(MockCommune.Bern.getNoOFS());
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
			forFiscal.setNumeroOfsAutoriteFiscale(MockCommune.Bale.getNoOFS());
			fors.add(forFiscal);
		}
		hab.setForsFiscaux(fors);
		return hab;
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testSansChevauchementRepresentationsLegales() throws Exception {

		final class Ids {
			long idTuteur;
			long idCurateur;
			long idPupille;
		}

		final Ids ids = new Ids();

		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique tuteur = addNonHabitant(null, "Tuteur", null, Sexe.MASCULIN);
				final PersonnePhysique curateur = addNonHabitant(null, "Curateur", null, Sexe.FEMININ);
				final PersonnePhysique pupille = addNonHabitant(null, "Pupille", null, Sexe.MASCULIN);

				ids.idTuteur = tuteur.getNumero();
				ids.idCurateur = curateur.getNumero();
				ids.idPupille = pupille.getNumero();
				return null;
			}
		});

		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {

				final PersonnePhysique tuteur = (PersonnePhysique) tiersDAO.get(ids.idTuteur);
				final PersonnePhysique curateur = (PersonnePhysique) tiersDAO.get(ids.idCurateur);
				final PersonnePhysique pupille = (PersonnePhysique) tiersDAO.get(ids.idPupille);

				addTutelle(pupille, tuteur, null, date(2000, 1, 1), date(2000, 12, 31));
				addCuratelle(pupille, curateur, date(2001, 1, 1), date(2001, 6, 30));

				final ValidationResults vr = validate(pupille);
				assertFalse(vr.hasErrors());
				return null;
			}
		});
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testAvecChevauchementSimpleRepresentationsLegales() throws Exception {

		final class Ids {
			long idTuteur;
			long idCurateur;
			long idPupille;
		}

		final Ids ids = new Ids();

		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique tuteur = addNonHabitant(null, "Tuteur", null, Sexe.MASCULIN);
				final PersonnePhysique curateur = addNonHabitant(null, "Curateur", null, Sexe.FEMININ);
				final PersonnePhysique pupille = addNonHabitant(null, "Pupille", null, Sexe.MASCULIN);

				ids.idTuteur = tuteur.getNumero();
				ids.idCurateur = curateur.getNumero();
				ids.idPupille = pupille.getNumero();
				return null;
			}
		});

		doInNewTransactionAndSessionWithoutValidation(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {

				final PersonnePhysique tuteur = (PersonnePhysique) tiersDAO.get(ids.idTuteur);
				final PersonnePhysique curateur = (PersonnePhysique) tiersDAO.get(ids.idCurateur);
				final PersonnePhysique pupille = (PersonnePhysique) tiersDAO.get(ids.idPupille);

				addTutelle(pupille, tuteur, null, date(2000, 1, 1), date(2001, 1, 31));
				addCuratelle(pupille, curateur, date(2001, 1, 1), date(2001, 6, 30));

				final ValidationResults vr = validate(pupille);
				assertTrue(vr.hasErrors());

				assertEquals(1, vr.getErrors().size());
				assertEquals("La période [01.01.2001 ; 31.01.2001] est couverte par plusieurs mesures tutélaires", vr.getErrors().get(0));

				return null;
			}
		});
	}

	@Test
	public void testAvecChevauchementRépartiRepresentationsLegales() throws Exception {

		final class Ids {
			long idTuteur;
			long idCurateur;
			long idConseiller;
			long idPupille;
		}

		final Ids ids = new Ids();

		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique tuteur = addNonHabitant(null, "Tuteur", null, Sexe.MASCULIN);
				final PersonnePhysique curateur = addNonHabitant(null, "Curateur", null, Sexe.FEMININ);
				final PersonnePhysique conseiller = addNonHabitant(null, "Conseiller", null, Sexe.FEMININ);
				final PersonnePhysique pupille = addNonHabitant(null, "Pupille", null, Sexe.MASCULIN);

				ids.idTuteur = tuteur.getNumero();
				ids.idCurateur = curateur.getNumero();
				ids.idConseiller = conseiller.getNumero();
				ids.idPupille = pupille.getNumero();
				return null;
			}
		});

		doInNewTransactionAndSessionWithoutValidation(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {

				final PersonnePhysique tuteur = (PersonnePhysique) tiersDAO.get(ids.idTuteur);
				final PersonnePhysique curateur = (PersonnePhysique) tiersDAO.get(ids.idCurateur);
				final PersonnePhysique conseiller = (PersonnePhysique) tiersDAO.get(ids.idConseiller);
				final PersonnePhysique pupille = (PersonnePhysique) tiersDAO.get(ids.idPupille);

				addTutelle(pupille, tuteur, null, date(2000, 1, 1), date(2001, 1, 31));         // |-----------------------|
				addCuratelle(pupille, curateur, date(2001, 1, 1), date(2001, 6, 30));           //                      |-----------------|
				addConseilLegal(pupille, conseiller, date(2001, 6, 1), null);                   //                                     |---------------...

				final ValidationResults vr = validate(pupille);
				assertTrue(vr.hasErrors());

				assertEquals(2, vr.getErrors().size());
				assertEquals("La période [01.01.2001 ; 31.01.2001] est couverte par plusieurs mesures tutélaires", vr.getErrors().get(0));
				assertEquals("La période [01.06.2001 ; 30.06.2001] est couverte par plusieurs mesures tutélaires", vr.getErrors().get(1));

				return null;
			}
		});
	}

	@Test
	public void testAvecChevauchementMultipleRepresentationsLegales() throws Exception {

		final class Ids {
			long idTuteur;
			long idCurateur;
			long idConseiller;
			long idPupille;
		}

		final Ids ids = new Ids();

		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique tuteur = addNonHabitant(null, "Tuteur", null, Sexe.MASCULIN);
				final PersonnePhysique curateur = addNonHabitant(null, "Curateur", null, Sexe.FEMININ);
				final PersonnePhysique conseiller = addNonHabitant(null, "Conseiller", null, Sexe.FEMININ);
				final PersonnePhysique pupille = addNonHabitant(null, "Pupille", null, Sexe.MASCULIN);

				ids.idTuteur = tuteur.getNumero();
				ids.idCurateur = curateur.getNumero();
				ids.idConseiller = conseiller.getNumero();
				ids.idPupille = pupille.getNumero();
				return null;
			}
		});

		doInNewTransactionAndSessionWithoutValidation(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {

				final PersonnePhysique tuteur = (PersonnePhysique) tiersDAO.get(ids.idTuteur);
				final PersonnePhysique curateur = (PersonnePhysique) tiersDAO.get(ids.idCurateur);
				final PersonnePhysique conseiller = (PersonnePhysique) tiersDAO.get(ids.idConseiller);
				final PersonnePhysique pupille = (PersonnePhysique) tiersDAO.get(ids.idPupille);

				addTutelle(pupille, tuteur, null, date(2000, 1, 1), date(2001, 1, 31));                 // |----------------------------|
				addCuratelle(pupille, curateur, date(2001, 1, 1), date(2001, 6, 30));                   //                          |--------------------|
				addConseilLegal(pupille, conseiller, date(2000, 6, 1), date(2001, 3, 31));              //                |---------------------|
				addConseilLegal(pupille, tuteur, date(2001, 5, 1), null);                               //                                            |-----------...

				final ValidationResults vr = validate(pupille);
				assertTrue(vr.hasErrors());

				assertEquals(2, vr.getErrors().size());
				assertEquals("La période [01.06.2000 ; 31.03.2001] est couverte par plusieurs mesures tutélaires", vr.getErrors().get(0));
				assertEquals("La période [01.05.2001 ; 30.06.2001] est couverte par plusieurs mesures tutélaires", vr.getErrors().get(1));

				return null;
			}
		});
	}

	// [SIFISC-719] on vérifie que les rapports de représentation conventionnels ne se chevauchent pas
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testRapportsRepresentationConventionnels() throws Exception {

		final PersonnePhysique representant = addNonHabitant("Lionel", "Schtroumpf", date(1954, 7, 23), Sexe.MASCULIN);

		final PersonnePhysique pp = addNonHabitant("Gérard", "Stöpötz", date(1954, 7, 23), Sexe.MASCULIN);
		assertFalse(validate(pp).hasErrors());

		// on ajoute une première représentation conventionnelle : ok
		addRepresentationConventionnelle(pp, representant, date(1990, 1, 1), date(1999, 12, 31), false);
		assertFalse(validate(pp).hasErrors());

		// on ajoute une deuxième représentation conventionnelle qui ne chevauche pas le premier : ok
		addRepresentationConventionnelle(pp, representant, date(2005, 1, 1), date(2005, 12, 31), false);
		assertFalse(validate(pp).hasErrors());

		// on ajoute une troisième représentation conventionnelle qui chevauche le seconde : ok
		addRepresentationConventionnelle(pp, representant, date(2005, 7, 1), date(2009, 12, 31), false);
		final ValidationResults results = validate(pp);
		assertTrue(results.hasErrors());

		final List<String> errors = results.getErrors();
		assertEquals(1, errors.size());
		assertEquals("La période [01.07.2005 ; 31.12.2005] est couverte par plusieurs rapports de type 'représentation conventionnelle'.", errors.get(0));
	}

	// [SIFISC-2533] Vérifie que les rapports d'appartenance ménage ne se chevauchent pas
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testRapportsAppartenanceMenage() throws Exception {

		// un marié seul
		final PersonnePhysique pp = addNonHabitant("Gérard", "Stöpötz", date(1954, 7, 23), Sexe.MASCULIN);
		assertFalse(validate(pp).hasErrors());

		addEnsembleTiersCouple(pp, null, date(2003, 3, 23), null);
		assertFalse(validate(pp).hasErrors());

		// on créé un second ménage avec la personne physique, qui chevauch le premier ménage
		addEnsembleTiersCouple(pp, null, date(2005, 7, 10), null);
		final ValidationResults results = validate(pp);
		assertTrue(results.hasErrors());

		final List<String> errors = results.getErrors();
		assertEquals(1, errors.size());
		assertEquals("La personne physique appartient à plusieurs ménages communs sur la période [10.07.2005 ; ]", errors.get(0));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testValidateUneDeclarationPourUnePeriodeImposition() throws Exception {

		// Création d'un contribuable avec une déclaration qui correspond bien avec une période d'imposition
		final PersonnePhysique pp = addNonHabitant("Jean", "Rappel", date(1954, 12, 3), Sexe.MASCULIN);
		addForPrincipal(pp, date(2006, 1, 26), MotifFor.INDETERMINE, date(2009, 7, 11), MotifFor.ARRIVEE_HS, MockPays.Danemark);
		addForPrincipal(pp, date(2009, 7, 12), MotifFor.ARRIVEE_HS, date(2009, 12, 27), MotifFor.DEPART_HC, MockCommune.Bussigny);
		addForPrincipal(pp, date(2009, 12, 28), MotifFor.DEPART_HC, MockCommune.Neuchatel);
		addForSecondaire(pp, date(2006, 1, 26), MotifFor.ACHAT_IMMOBILIER, date(2010, 12, 31), MotifFor.VENTE_IMMOBILIER, MockCommune.Aigle.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);

		final PeriodeFiscale periode2009 = addPeriodeFiscale(2009);
		final ModeleDocument modeleHC2009 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_HC_IMMEUBLE, periode2009);
		addDeclarationImpot(pp, periode2009, date(2009, 1, 1), date(2009, 12, 31), TypeContribuable.HORS_CANTON, modeleHC2009);

		final ValidationResults results = validate(pp);
		assertFalse(results.hasErrors());
		assertFalse(results.hasWarnings());
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testValidateDeuxDeclarationsPourUnePeriodeImposition() throws Exception {

		// Création d'un contribuable avec deux déclarations pour une période d'imposition
		final PersonnePhysique pp = addNonHabitant("Jean", "Rappel", date(1954, 12, 3), Sexe.MASCULIN);
		addForPrincipal(pp, date(2006, 1, 26), MotifFor.INDETERMINE, date(2009, 7, 11), MotifFor.ARRIVEE_HS, MockPays.Danemark);
		addForPrincipal(pp, date(2009, 7, 12), MotifFor.ARRIVEE_HS, date(2009, 12, 27), MotifFor.DEPART_HC, MockCommune.Bussigny);
		addForPrincipal(pp, date(2009, 12, 28), MotifFor.DEPART_HC, MockCommune.Neuchatel);
		addForSecondaire(pp, date(2006, 1, 26), MotifFor.ACHAT_IMMOBILIER, date(2010, 12, 31), MotifFor.VENTE_IMMOBILIER, MockCommune.Aigle.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);

		final PeriodeFiscale periode2009 = addPeriodeFiscale(2009);
		final ModeleDocument modeleHS2009 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode2009);
		final ModeleDocument modeleHC2009 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_HC_IMMEUBLE, periode2009);
		addDeclarationImpot(pp, periode2009, date(2009, 1, 1), date(2009, 7, 11), TypeContribuable.HORS_SUISSE, modeleHS2009);
		addDeclarationImpot(pp, periode2009, date(2009, 7, 12), date(2009, 12, 31), TypeContribuable.HORS_CANTON, modeleHC2009);

		final ValidationResults results = validate(pp);
		assertFalse(results.hasErrors());
		assertTrue(results.hasWarnings());

		final List<String> warnings = results.getWarnings();
		assertEquals(2, warnings.size());
		assertEquals("La déclaration d'impôt ordinaire complète qui va du 01.01.2009 au 11.07.2009 ne correspond pas " +
				"à la période d'imposition théorique qui va du 01.01.2009 au 31.12.2009", warnings.get(0));
		assertEquals("La déclaration d'impôt hors-canton immeuble qui va du 12.07.2009 au 31.12.2009 ne correspond pas " +
				"à la période d'imposition théorique qui va du 01.01.2009 au 31.12.2009", warnings.get(1));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testValidateUneDeclarationPourZeroPeriodeImposition() throws Exception {

		// Création d'un contribuable avec une déclaration mais pas de période d'imposition théorique
		final PersonnePhysique pp = addNonHabitant("Jean", "Rappel", date(1954, 12, 3), Sexe.MASCULIN);
		addForPrincipal(pp, date(2006, 1, 26), MotifFor.INDETERMINE, MockPays.Danemark);

		final PeriodeFiscale periode2009 = addPeriodeFiscale(2009);
		final ModeleDocument modeleHC2009 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_HC_IMMEUBLE, periode2009);
		addDeclarationImpot(pp, periode2009, date(2009, 1, 1), date(2009, 12, 31), TypeContribuable.HORS_CANTON, modeleHC2009);

		final ValidationResults results = validate(pp);
		assertFalse(results.hasErrors());
		assertTrue(results.hasWarnings());

		final List<String> warnings = results.getWarnings();
		assertEquals(1, warnings.size());
		assertEquals("La déclaration d'impôt hors-canton immeuble qui va du 01.01.2009 au 31.12.2009 ne correspond à aucune période d'imposition théorique", warnings.get(0));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testMultiplesLiensHeritageNonChevauchants() throws Exception {

		final RegDate dateDeces = date(2017, 5, 31);
		final PersonnePhysique defunt = addNonHabitant("Mordant", "Lapomme", date(1964, 8, 21), Sexe.MASCULIN);
		defunt.setDateDeces(dateDeces);
		final PersonnePhysique heritier = addNonHabitant("Croquant", "Lapomme", date(1996, 7, 31), Sexe.MASCULIN);

		addHeritage(heritier, defunt, dateDeces.getOneDayAfter(), null);
		addHeritage(heritier, defunt, date(2000, 1, 1), date(2000, 12, 31));        // donnée bidon..

		// validation côté défunt
		{
			final ValidationResults results = validate(defunt);
			assertTrue(results.hasErrors());
			assertTrue(results.hasWarnings());

			final List<String> erreurs = results.getErrors();
			assertNotNull(erreurs);
			assertEquals(1, erreurs.size());
			final String expectedErreur = String.format("La personne physique %s possède plusieurs liens d'héritage vers l'héritier %s",
			                                            FormatNumeroHelper.numeroCTBToDisplay(defunt.getNumero()),
			                                            FormatNumeroHelper.numeroCTBToDisplay(heritier.getNumero()));
			assertEquals(expectedErreur, erreurs.get(0));

			final List<String> warnings = results.getWarnings();
			assertNotNull(warnings);
			assertEquals(1, warnings.size());
			final String expectedWarning = String.format("Le rapport entre tiers de type Héritage (01.01.2000 - 31.12.2000) entre le tiers héritier %s et le tiers défunt(e) %s devrait débuter au lendemain de la date de décès du/de la défunt(e) (%s)",
			                                             FormatNumeroHelper.numeroCTBToDisplay(heritier.getNumero()),
			                                             FormatNumeroHelper.numeroCTBToDisplay(defunt.getNumero()),
			                                             RegDateHelper.dateToDisplayString(dateDeces));
			assertEquals(expectedWarning, warnings.get(0));
		}

		// validation côté héritier
		{
			final ValidationResults results = validate(heritier);
			assertTrue(results.hasErrors());
			assertTrue(results.hasWarnings());

			final List<String> erreurs = results.getErrors();
			assertNotNull(erreurs);
			assertEquals(1, erreurs.size());
			final String expectedErreur = String.format("La personne physique %s possède plusieurs liens d'héritage vers le défunt %s",
			                                            FormatNumeroHelper.numeroCTBToDisplay(heritier.getNumero()),
			                                            FormatNumeroHelper.numeroCTBToDisplay(defunt.getNumero()));
			assertEquals(expectedErreur, erreurs.get(0));

			final List<String> warnings = results.getWarnings();
			assertNotNull(warnings);
			assertEquals(1, warnings.size());
			final String expectedWarning = String.format("Le rapport entre tiers de type Héritage (01.01.2000 - 31.12.2000) entre le tiers héritier %s et le tiers défunt(e) %s devrait débuter au lendemain de la date de décès du/de la défunt(e) (%s)",
			                                             FormatNumeroHelper.numeroCTBToDisplay(heritier.getNumero()),
			                                             FormatNumeroHelper.numeroCTBToDisplay(defunt.getNumero()),
			                                             RegDateHelper.dateToDisplayString(dateDeces));
			assertEquals(expectedWarning, warnings.get(0));
		}
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testMultiplesLiensHeritageChevauchants() throws Exception {

		final RegDate dateDeces = date(2017, 5, 31);
		final PersonnePhysique defunt = addNonHabitant("Mordant", "Lapomme", date(1964, 8, 21), Sexe.MASCULIN);
		defunt.setDateDeces(dateDeces);
		final PersonnePhysique heritier = addNonHabitant("Croquant", "Lapomme", date(1996, 7, 31), Sexe.MASCULIN);

		addHeritage(heritier, defunt, dateDeces.getOneDayAfter(), null);
		addHeritage(heritier, defunt, dateDeces.getOneDayAfter(), dateDeces.addMonths(1));        // donnée bidon..

		// validation côté défunt
		{
			final ValidationResults results = validate(defunt);
			assertTrue(results.hasErrors());
			assertFalse(results.hasWarnings());

			final List<String> erreurs = results.getErrors();
			assertNotNull(erreurs);
			assertEquals(1, erreurs.size());
			final String expectedErreur = String.format("La personne physique %s possède plusieurs liens d'héritage vers l'héritier %s",
			                                            FormatNumeroHelper.numeroCTBToDisplay(defunt.getNumero()),
			                                            FormatNumeroHelper.numeroCTBToDisplay(heritier.getNumero()));
			assertEquals(expectedErreur, erreurs.get(0));
		}

		// validation côté héritier
		{
			final ValidationResults results = validate(heritier);
			assertTrue(results.hasErrors());
			assertFalse(results.hasWarnings());

			final List<String> erreurs = results.getErrors();
			assertNotNull(erreurs);
			assertEquals(1, erreurs.size());
			final String expectedErreur = String.format("La personne physique %s possède plusieurs liens d'héritage vers le défunt %s",
			                                            FormatNumeroHelper.numeroCTBToDisplay(heritier.getNumero()),
			                                            FormatNumeroHelper.numeroCTBToDisplay(defunt.getNumero()));
			assertEquals(expectedErreur, erreurs.get(0));
		}
	}
}
