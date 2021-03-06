package ch.vd.unireg.metier.assujettissement;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.interfaces.infra.mock.MockTypeRegimeFiscal;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.ForsParType;
import ch.vd.unireg.type.DayMonth;
import ch.vd.unireg.type.FormeJuridiqueEntreprise;
import ch.vd.unireg.type.GenreImpot;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.MotifRattachement;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@SuppressWarnings({"JavaDoc", "deprecation"})
public class AssujettissementPersonnesMoralesCalculatorTest extends MetierTest {
	
	private AssujettissementPersonnesMoralesCalculator calculator;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		calculator = new AssujettissementPersonnesMoralesCalculator(tiersService, regimeFiscalService);
	}

	@NotNull
	private static List<Assujettissement> determine(AssujettissementCalculator<? super Entreprise> calculator,
	                                                Entreprise entreprise,
	                                                @Nullable Set<Integer> noOfsCommunes) throws AssujettissementException {
		if (entreprise.isAnnule()) {
			return Collections.emptyList();
		}

		final ForsParType fpt = entreprise.getForsParType(true);
		if (fpt == null || fpt.isEmpty()) {
			return Collections.emptyList();
		}

		return calculator.determine(entreprise, fpt, noOfsCommunes);
	}

	@NotNull
	private List<Assujettissement> determine(Entreprise entreprise) throws AssujettissementException {
		return determine(calculator, entreprise, null);
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testSansForNiBouclement() throws Exception {
		final Entreprise e = addEntrepriseInconnueAuCivil();
		assertEmpty(determine(e));        // aucun assujettissement si pas de for
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testSansForAvecBouclement() throws Exception {
		final Entreprise e = addEntrepriseInconnueAuCivil();
		addBouclement(e, date(2013, 1, 1), DayMonth.get(6, 30), 12);        // bouclements tous les 30.06 depuis 30.06.2013
		assertEmpty(determine(e));        // aucun assujettissement si pas de for
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testVaudoisExercicesEtForsCalesSurAnneeCivile() throws Exception {
		final Entreprise e = addEntrepriseInconnueAuCivil();
		addRegimeFiscalVD(e, date(2013, 1, 1), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
		addRegimeFiscalCH(e, date(2013, 1, 1), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
		addForPrincipal(e, date(2013, 1, 1), MotifFor.INDETERMINE, MockCommune.Aigle);
		addBouclement(e, date(2013, 1, 1), DayMonth.get(12, 31), 12);       // bouclements tous les 31.12 depuis le 31.12.2013

		final List<Assujettissement> assujettissements = determine(e);
		assertNotNull(assujettissements);
		assertEquals(1, assujettissements.size());
		assertOrdinaire(date(2013, 1, 1), null, MotifAssujettissement.INDETERMINE, null, assujettissements.get(0));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testVaudoisExercicesCalesSurAnneeCivileMaisPasPremierFor() throws Exception {
		final Entreprise e = addEntrepriseInconnueAuCivil();
		addRegimeFiscalVD(e, date(2013, 4, 21), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
		addRegimeFiscalCH(e, date(2013, 4, 21), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
		addForPrincipal(e, date(2013, 4, 21), MotifFor.INDETERMINE, MockCommune.Aigle);
		addBouclement(e, date(2013, 1, 1), DayMonth.get(12, 31), 12);       // bouclements tous les 31.12 depuis le 31.12.2013

		final List<Assujettissement> assujettissements = determine(e);
		assertNotNull(assujettissements);
		assertEquals(1, assujettissements.size());
		assertOrdinaire(date(2013, 4, 21), null, MotifAssujettissement.INDETERMINE, null, assujettissements.get(0));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testVaudoisExercicesNonCalesSurAnneeCivile() throws Exception {
		final Entreprise e = addEntrepriseInconnueAuCivil();
		addRegimeFiscalVD(e, date(2013, 1, 15), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
		addRegimeFiscalCH(e, date(2013, 1, 15), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
		addForPrincipal(e, date(2013, 1, 15), MotifFor.INDETERMINE, MockCommune.Aigle);
		addBouclement(e, date(2013, 1, 1), DayMonth.get(6, 30), 12);    // bouclements tous les 30.06 depuis le 31.03.2013

		final List<Assujettissement> assujettissements = determine(e);
		assertNotNull(assujettissements);
		assertEquals(1, assujettissements.size());
		assertOrdinaire(date(2013, 1, 15), null, MotifAssujettissement.INDETERMINE, null, assujettissements.get(0));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testVaudoisModificationDureeExcercicesCommerciauxDebutAnneeCivile() throws Exception {
		final Entreprise e = addEntrepriseInconnueAuCivil();
		addRegimeFiscalVD(e, date(2013, 1, 15), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
		addRegimeFiscalCH(e, date(2013, 1, 15), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
		addForPrincipal(e, date(2013, 1, 15), MotifFor.INDETERMINE, MockCommune.Aigle);
		addBouclement(e, date(2013, 1, 1), DayMonth.get(12, 31), 12);    // bouclements tous les 31.12 depuis le 31.12.2013
		addBouclement(e, date(2014, 3, 31), DayMonth.get(3, 31), 12);    // bouclements tous les 31.03 depuis le 31.03.2014
		addBouclement(e, date(2014, 4, 1), DayMonth.get(12, 31), 12);    // bouclements tous les 31.12 depuis le 31.12.2014

		final List<Assujettissement> assujettissements = determine(e);
		assertNotNull(assujettissements);
		assertEquals(1, assujettissements.size());
		assertOrdinaire(date(2013, 1, 15), null, MotifAssujettissement.INDETERMINE, null, assujettissements.get(0));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testVaudoisModificationDureeExcercicesCommerciauxEnCoursAnneeCivile() throws Exception {
		final Entreprise e = addEntrepriseInconnueAuCivil();
		addRegimeFiscalVD(e, date(2013, 1, 15), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
		addRegimeFiscalCH(e, date(2013, 1, 15), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
		addForPrincipal(e, date(2013, 1, 15), MotifFor.INDETERMINE, MockCommune.Aigle);
		addBouclement(e, date(2013, 3, 31), DayMonth.get(3, 31), 12);   // bouclements tous les 31.03 depuis le 31.03.2013
		addBouclement(e, date(2014, 4, 1), DayMonth.get(6, 30), 12);    // bouclements tous les 30.06 depuis le 30.06.2014
		addBouclement(e, date(2014, 7, 1), DayMonth.get(3, 31), 12);    // bouclements tous les 31.03 depuis le 31.03.2015

		final List<Assujettissement> assujettissements = determine(e);
		assertNotNull(assujettissements);
		assertEquals(1, assujettissements.size());
		assertOrdinaire(date(2013, 1, 15), null, MotifAssujettissement.INDETERMINE, null, assujettissements.get(0));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testVaudoisArriveeHorsSuisseBouclementApresDansAnnee() throws Exception {
		final Entreprise e = addEntrepriseInconnueAuCivil();
		addRegimeFiscalVD(e, date(2014, 4, 21), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
		addRegimeFiscalCH(e, date(2014, 4, 21), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
		addForPrincipal(e, date(2014, 4, 21), MotifFor.ARRIVEE_HS, MockCommune.Echallens);
		addBouclement(e, date(2013, 12, 31), DayMonth.get(12, 31), 12);       // bouclements tous les 31.12 depuis le 31.12.2013 (= un exercice commercial est partiellement hors zone de fors)

		final List<Assujettissement> assujettissements = determine(e);
		assertNotNull(assujettissements);
		assertEquals(1, assujettissements.size());
		assertOrdinaire(date(2014, 4, 21), null, MotifAssujettissement.ARRIVEE_HS, null, assujettissements.get(0));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testVaudoisArriveeHorsSuisseBouclementSansBouclementApresArriveeDansAnnee() throws Exception {
		final Entreprise e = addEntrepriseInconnueAuCivil();
		addRegimeFiscalVD(e, date(2014, 4, 21), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
		addRegimeFiscalCH(e, date(2014, 4, 21), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
		addForPrincipal(e, date(2014, 4, 21), MotifFor.ARRIVEE_HS, MockCommune.Echallens);
		addBouclement(e, date(2014, 3, 31), DayMonth.get(3, 31), 12);       // bouclements tous les 31.03 depuis le 31.03.2014 (= un exercice commercial est partiellement hors zone de fors)

		final List<Assujettissement> assujettissements = determine(e);
		assertNotNull(assujettissements);
		assertEquals(1, assujettissements.size());
		assertOrdinaire(date(2014, 4, 21), null, MotifAssujettissement.ARRIVEE_HS, null, assujettissements.get(0));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testVaudoisDepartHorsSuisseSansBouclementSynchrone() throws Exception {

		final RegDate dateCreation = date(2013, 4, 21);
		final RegDate dateDepart = date(2014, 5, 12);

		final Entreprise e = addEntrepriseInconnueAuCivil();
		addRegimeFiscalVD(e, dateCreation, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
		addRegimeFiscalCH(e, dateCreation, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
		addForPrincipal(e, dateCreation, MotifFor.INDETERMINE, dateDepart, MotifFor.DEPART_HS, MockCommune.Echallens);
		addForPrincipal(e, dateDepart.getOneDayAfter(), MotifFor.DEPART_HS, MockPays.Albanie);
		addBouclement(e, date(2013, 3, 31), DayMonth.get(3, 31), 12);       // bouclements tous les 31.03 depuis le 31.03.2013 (= un exercice commercial est partiellement hors zone de fors)

		final List<Assujettissement> assujettissements = determine(e);
		assertNotNull(assujettissements);
		assertEquals(1, assujettissements.size());
		assertOrdinaire(dateCreation, dateDepart, MotifAssujettissement.INDETERMINE, MotifAssujettissement.DEPART_HS, assujettissements.get(0));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testVaudoisArriveeHorsCantonSansBouclementApresArriveeDansAnnee() throws Exception {
		final Entreprise e = addEntrepriseInconnueAuCivil();
		addRegimeFiscalVD(e, date(2014, 4, 21), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
		addRegimeFiscalCH(e, date(2014, 4, 21), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
		addForPrincipal(e, date(2014, 4, 21), MotifFor.ARRIVEE_HC, MockCommune.Echallens);
		addBouclement(e, date(2014, 3, 31), DayMonth.get(3, 31), 12);       // bouclements tous les 31.03 depuis le 31.03.2014 (= un exercice commercial est partiellement hors zone de fors)

		final List<Assujettissement> assujettissements = determine(e);
		assertNotNull(assujettissements);
		assertEquals(1, assujettissements.size());
		assertOrdinaire(date(2014, 4, 1), null, MotifAssujettissement.ARRIVEE_HC, null, assujettissements.get(0));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testVaudoisArriveeHorsCantonLendemainBouclement() throws Exception {
		final Entreprise e = addEntrepriseInconnueAuCivil();
		addRegimeFiscalVD(e, date(2014, 4, 1), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
		addRegimeFiscalCH(e, date(2014, 4, 1), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
		addForPrincipal(e, date(2014, 4, 1), MotifFor.ARRIVEE_HC, MockCommune.Echallens);
		addBouclement(e, date(2014, 3, 31), DayMonth.get(3, 31), 12);       // bouclements tous les 31.03 depuis le 31.03.2014

		final List<Assujettissement> assujettissements = determine(e);
		assertNotNull(assujettissements);
		assertEquals(1, assujettissements.size());
		assertOrdinaire(date(2014, 4, 1), null, MotifAssujettissement.ARRIVEE_HC, null, assujettissements.get(0));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testHorsCantonImmeubleAchatSynchroneAvecExercice() throws Exception {

		final RegDate dateAchat = date(2013, 4, 1);

		final Entreprise e = addEntrepriseInconnueAuCivil();
		addRegimeFiscalVD(e, dateAchat, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
		addRegimeFiscalCH(e, dateAchat, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
		addForPrincipal(e, dateAchat, null, MockCommune.Bale);
		addForSecondaire(e, dateAchat, MotifFor.ACHAT_IMMOBILIER, MockCommune.Cossonay, MotifRattachement.IMMEUBLE_PRIVE, GenreImpot.BENEFICE_CAPITAL);
		addBouclement(e, date(2013, 3, 31), DayMonth.get(3, 31), 12);       // bouclements tous les 31.03 depuis le 31.03.2013

		final List<Assujettissement> assujettissements = determine(e);
		assertNotNull(assujettissements);
		assertEquals(1, assujettissements.size());
		assertHorsCanton(date(2013, 4, 1), null, MotifAssujettissement.ACHAT_IMMOBILIER, null, assujettissements.get(0));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testHorsCantonImmeubleAchatNonSynchroneAvecExercice() throws Exception {

		final RegDate dateAchat = date(2013, 4, 1);

		final Entreprise e = addEntrepriseInconnueAuCivil();
		addRegimeFiscalVD(e, dateAchat, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
		addRegimeFiscalCH(e, dateAchat, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
		addForPrincipal(e, dateAchat, null, MockCommune.Bale);
		addForSecondaire(e, dateAchat, MotifFor.ACHAT_IMMOBILIER, MockCommune.Cossonay, MotifRattachement.IMMEUBLE_PRIVE, GenreImpot.BENEFICE_CAPITAL);
		addBouclement(e, date(2012, 12, 31), DayMonth.get(12, 31), 12);       // bouclements tous les 31.12 depuis le 31.12.2012

		final List<Assujettissement> assujettissements = determine(e);
		assertNotNull(assujettissements);
		assertEquals(1, assujettissements.size());
		assertHorsCanton(date(2013, 1, 1), null, MotifAssujettissement.ACHAT_IMMOBILIER, null, assujettissements.get(0));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testHorsCantonActiviteSynchroneAvecExercice() throws Exception {

		final RegDate dateDebutExploitation = date(2013, 4, 1);

		final Entreprise e = addEntrepriseInconnueAuCivil();
		addRegimeFiscalVD(e, dateDebutExploitation, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
		addRegimeFiscalCH(e, dateDebutExploitation, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
		addForPrincipal(e, dateDebutExploitation, null, MockCommune.Bale);
		addForSecondaire(e, dateDebutExploitation, MotifFor.DEBUT_EXPLOITATION, MockCommune.Cossonay, MotifRattachement.ACTIVITE_INDEPENDANTE, GenreImpot.BENEFICE_CAPITAL);
		addBouclement(e, date(2013, 3, 31), DayMonth.get(3, 31), 12);       // bouclements tous les 31.03 depuis le 31.03.2013

		final List<Assujettissement> assujettissements = determine(e);
		assertNotNull(assujettissements);
		assertEquals(1, assujettissements.size());
		assertHorsCanton(date(2013, 4, 1), null, MotifAssujettissement.DEBUT_EXPLOITATION, null, assujettissements.get(0));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testHorsCantonActiviteNonSynchroneAvecExercice() throws Exception {

		final RegDate dateDebutExploitation = date(2013, 4, 1);

		final Entreprise e = addEntrepriseInconnueAuCivil();
		addRegimeFiscalVD(e, dateDebutExploitation, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
		addRegimeFiscalCH(e, dateDebutExploitation, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
		addForPrincipal(e, dateDebutExploitation, null, MockCommune.Bale);
		addForSecondaire(e, dateDebutExploitation, MotifFor.DEBUT_EXPLOITATION, MockCommune.Cossonay, MotifRattachement.ACTIVITE_INDEPENDANTE, GenreImpot.BENEFICE_CAPITAL);
		addBouclement(e, date(2012, 12, 31), DayMonth.get(12, 31), 12);       // bouclements tous les 31.12 depuis le 31.12.2012

		final List<Assujettissement> assujettissements = determine(e);
		assertNotNull(assujettissements);
		assertEquals(1, assujettissements.size());
		assertHorsCanton(date(2013, 1, 1), null, MotifAssujettissement.DEBUT_EXPLOITATION, null, assujettissements.get(0));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testHorsSuisseImmeubleAchatSynchroneAvecExercice() throws Exception {

		final RegDate dateAchat = date(2013, 4, 1);

		final Entreprise e = addEntrepriseInconnueAuCivil();
		addRegimeFiscalVD(e, dateAchat, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
		addRegimeFiscalCH(e, dateAchat, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
		addForPrincipal(e, dateAchat, null, MockPays.Allemagne);
		addForSecondaire(e, dateAchat, MotifFor.ACHAT_IMMOBILIER, MockCommune.Cossonay, MotifRattachement.IMMEUBLE_PRIVE, GenreImpot.BENEFICE_CAPITAL);
		addBouclement(e, date(2013, 3, 31), DayMonth.get(3, 31), 12);       // bouclements tous les 31.03 depuis le 31.03.2013

		final List<Assujettissement> assujettissements = determine(e);
		assertNotNull(assujettissements);
		assertEquals(1, assujettissements.size());
		assertHorsSuisse(dateAchat, null, MotifAssujettissement.ACHAT_IMMOBILIER, null, assujettissements.get(0));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testHorsSuisseImmeubleAchatNonSynchroneAvecExercice() throws Exception {

		final RegDate dateAchat = date(2013, 4, 1);

		final Entreprise e = addEntrepriseInconnueAuCivil();
		addRegimeFiscalVD(e, dateAchat, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
		addRegimeFiscalCH(e, dateAchat, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
		addForPrincipal(e, dateAchat, null, MockPays.Allemagne);
		addForSecondaire(e, dateAchat, MotifFor.ACHAT_IMMOBILIER, MockCommune.Cossonay, MotifRattachement.IMMEUBLE_PRIVE, GenreImpot.BENEFICE_CAPITAL);
		addBouclement(e, date(2012, 12, 31), DayMonth.get(12, 31), 12);       // bouclements tous les 31.12 depuis le 31.12.2012

		final List<Assujettissement> assujettissements = determine(e);
		assertNotNull(assujettissements);
		assertEquals(1, assujettissements.size());
		assertHorsSuisse(dateAchat, null, MotifAssujettissement.ACHAT_IMMOBILIER, null, assujettissements.get(0));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testHorsSuisseActiviteSynchroneAvecExercice() throws Exception {

		final RegDate dateDebutExploitation = date(2013, 4, 1);

		final Entreprise e = addEntrepriseInconnueAuCivil();
		addRegimeFiscalVD(e, dateDebutExploitation, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
		addRegimeFiscalCH(e, dateDebutExploitation, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
		addForPrincipal(e, dateDebutExploitation, null, MockPays.France);
		addForSecondaire(e, dateDebutExploitation, MotifFor.DEBUT_EXPLOITATION, MockCommune.Cossonay, MotifRattachement.ACTIVITE_INDEPENDANTE, GenreImpot.BENEFICE_CAPITAL);
		addBouclement(e, date(2013, 3, 31), DayMonth.get(3, 31), 12);       // bouclements tous les 31.03 depuis le 31.03.2013

		final List<Assujettissement> assujettissements = determine(e);
		assertNotNull(assujettissements);
		assertEquals(1, assujettissements.size());
		assertHorsSuisse(dateDebutExploitation, null, MotifAssujettissement.DEBUT_EXPLOITATION, null, assujettissements.get(0));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testHorsSuisseActiviteNonSynchroneAvecExercice() throws Exception {

		final RegDate dateDebutExploitation = date(2013, 4, 1);

		final Entreprise e = addEntrepriseInconnueAuCivil();
		addRegimeFiscalVD(e, dateDebutExploitation, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
		addRegimeFiscalCH(e, dateDebutExploitation, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
		addForPrincipal(e, dateDebutExploitation, null, MockPays.RoyaumeUni);
		addForSecondaire(e, dateDebutExploitation, MotifFor.DEBUT_EXPLOITATION, MockCommune.Cossonay, MotifRattachement.ACTIVITE_INDEPENDANTE, GenreImpot.BENEFICE_CAPITAL);
		addBouclement(e, date(2012, 12, 31), DayMonth.get(12, 31), 12);       // bouclements tous les 31.12 depuis le 31.12.2012

		final List<Assujettissement> assujettissements = determine(e);
		assertNotNull(assujettissements);
		assertEquals(1, assujettissements.size());
		assertHorsSuisse(dateDebutExploitation, null, MotifAssujettissement.DEBUT_EXPLOITATION, null, assujettissements.get(0));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testHorsCantonImmeubleTransfertSiegeVersVaud() throws Exception {

		final RegDate dateAchat = date(2013, 4, 15);
		final RegDate dateArriveeSiege = date(2014, 5, 21);

		final Entreprise e = addEntrepriseInconnueAuCivil();
		addRegimeFiscalVD(e, dateAchat, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
		addRegimeFiscalCH(e, dateAchat, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
		addForPrincipal(e, dateAchat, null, dateArriveeSiege.getOneDayBefore(), MotifFor.ARRIVEE_HC, MockCommune.Bale);
		addForPrincipal(e, dateArriveeSiege, MotifFor.ARRIVEE_HC, MockCommune.Morges);
		addForSecondaire(e, dateAchat, MotifFor.ACHAT_IMMOBILIER, MockCommune.Cossonay, MotifRattachement.IMMEUBLE_PRIVE, GenreImpot.BENEFICE_CAPITAL);
		addBouclement(e, date(2012, 3, 31), DayMonth.get(3, 31), 12);       // bouclements tous les 31.03 depuis le 31.03.2012

		final List<Assujettissement> assujettissements = determine(e);
		assertNotNull(assujettissements);
		assertEquals(2, assujettissements.size());
		assertHorsCanton(date(2013, 4, 1), date(2014, 3, 31), MotifAssujettissement.ACHAT_IMMOBILIER, MotifAssujettissement.ARRIVEE_HC, assujettissements.get(0));
		assertOrdinaire(date(2014, 4, 1), null, MotifAssujettissement.ARRIVEE_HC, null, assujettissements.get(1));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testHorsCantonActiviteTransfertSiegeVersVaud() throws Exception {

		final RegDate dateDebutExploitation = date(2013, 4, 15);
		final RegDate dateArriveeSiege = date(2014, 5, 21);

		final Entreprise e = addEntrepriseInconnueAuCivil();
		addRegimeFiscalVD(e, dateDebutExploitation, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
		addRegimeFiscalCH(e, dateDebutExploitation, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
		addForPrincipal(e, dateDebutExploitation, null, dateArriveeSiege.getOneDayBefore(), MotifFor.ARRIVEE_HC, MockCommune.Bale);
		addForPrincipal(e, dateArriveeSiege, MotifFor.ARRIVEE_HC, MockCommune.Morges);
		addForSecondaire(e, dateDebutExploitation, MotifFor.DEBUT_EXPLOITATION, MockCommune.Cossonay, MotifRattachement.ACTIVITE_INDEPENDANTE, GenreImpot.BENEFICE_CAPITAL);
		addBouclement(e, date(2012, 3, 31), DayMonth.get(3, 31), 12);       // bouclements tous les 31.03 depuis le 31.03.2012

		final List<Assujettissement> assujettissements = determine(e);
		assertNotNull(assujettissements);
		assertEquals(2, assujettissements.size());
		assertHorsCanton(date(2013, 4, 1), date(2014, 3, 31), MotifAssujettissement.DEBUT_EXPLOITATION, MotifAssujettissement.ARRIVEE_HC, assujettissements.get(0));
		assertOrdinaire(date(2014, 4, 1), null, MotifAssujettissement.ARRIVEE_HC, null, assujettissements.get(1));
	}


	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testHorsSuisseImmeubleTransfertSiegeVersVaud() throws Exception {

		final RegDate dateAchat = date(2013, 4, 15);
		final RegDate dateArriveeSiege = date(2014, 5, 21);

		final Entreprise e = addEntrepriseInconnueAuCivil();
		addRegimeFiscalVD(e, dateAchat, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
		addRegimeFiscalCH(e, dateAchat, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
		addForPrincipal(e, dateAchat, null, dateArriveeSiege.getOneDayBefore(), MotifFor.ARRIVEE_HS, MockPays.Allemagne);
		addForPrincipal(e, dateArriveeSiege, MotifFor.ARRIVEE_HS, MockCommune.Morges);
		addForSecondaire(e, dateAchat, MotifFor.ACHAT_IMMOBILIER, MockCommune.Cossonay, MotifRattachement.IMMEUBLE_PRIVE, GenreImpot.BENEFICE_CAPITAL);
		addBouclement(e, date(2012, 3, 31), DayMonth.get(3, 31), 12);       // bouclements tous les 31.03 depuis le 31.03.2012

		final List<Assujettissement> assujettissements = determine(e);
		assertNotNull(assujettissements);
		assertEquals(2, assujettissements.size());
		assertHorsSuisse(dateAchat, dateArriveeSiege.getOneDayBefore(), MotifAssujettissement.ACHAT_IMMOBILIER, MotifAssujettissement.ARRIVEE_HS, assujettissements.get(0));
		assertOrdinaire(dateArriveeSiege, null, MotifAssujettissement.ARRIVEE_HS, null, assujettissements.get(1));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testHorsSuisseActiviteTransfertSiegeVersVaud() throws Exception {

		final RegDate dateDebutExploitation = date(2013, 4, 15);
		final RegDate dateArriveeSiege = date(2014, 5, 21);

		final Entreprise e = addEntrepriseInconnueAuCivil();
		addRegimeFiscalVD(e, dateDebutExploitation, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
		addRegimeFiscalCH(e, dateDebutExploitation, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
		addForPrincipal(e, dateDebutExploitation, null, dateArriveeSiege.getOneDayBefore(), MotifFor.ARRIVEE_HS, MockPays.Allemagne);
		addForPrincipal(e, dateArriveeSiege, MotifFor.ARRIVEE_HS, MockCommune.Morges);
		addForSecondaire(e, dateDebutExploitation, MotifFor.DEBUT_EXPLOITATION, MockCommune.Cossonay, MotifRattachement.ACTIVITE_INDEPENDANTE, GenreImpot.BENEFICE_CAPITAL);
		addBouclement(e, date(2012, 3, 31), DayMonth.get(3, 31), 12);       // bouclements tous les 31.03 depuis le 31.03.2012

		final List<Assujettissement> assujettissements = determine(e);
		assertNotNull(assujettissements);
		assertEquals(2, assujettissements.size());
		assertHorsSuisse(dateDebutExploitation, dateArriveeSiege.getOneDayBefore(), MotifAssujettissement.DEBUT_EXPLOITATION, MotifAssujettissement.ARRIVEE_HS, assujettissements.get(0));
		assertOrdinaire(dateArriveeSiege, null, MotifAssujettissement.ARRIVEE_HS, null, assujettissements.get(1));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testVaudoisImmeubleTransfertSiegeVersHorsCanton() throws Exception {

		final RegDate dateCreationEntreprise = date(2013, 2, 1);
		final RegDate dateAchat = date(2013, 4, 15);
		final RegDate dateDepartSiege = date(2014, 5, 21);

		final Entreprise e = addEntrepriseInconnueAuCivil();
		addRegimeFiscalVD(e, dateCreationEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
		addRegimeFiscalCH(e, dateCreationEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
		addForPrincipal(e, dateCreationEntreprise, MotifFor.INDETERMINE, dateDepartSiege, MotifFor.DEPART_HC, MockCommune.Echallens);
		addForPrincipal(e, dateDepartSiege.getOneDayAfter(), MotifFor.DEPART_HC, MockCommune.Geneve);
		addForSecondaire(e, dateAchat, MotifFor.ACHAT_IMMOBILIER, MockCommune.Cossonay, MotifRattachement.IMMEUBLE_PRIVE, GenreImpot.BENEFICE_CAPITAL);
		addBouclement(e, date(2012, 3, 31), DayMonth.get(3, 31), 12);       // bouclements tous les 31.03 depuis le 31.03.2012
		addBouclement(e, date(2014, 4, 1), DayMonth.get(9, 30), 12);       // bouclements tous les 30.09 depuis le 30.09.2014

		final List<Assujettissement> assujettissements = determine(e);
		assertNotNull(assujettissements);
		assertEquals(2, assujettissements.size());
		assertOrdinaire(dateCreationEntreprise, date(2014, 9, 30), MotifAssujettissement.INDETERMINE, MotifAssujettissement.DEPART_HC, assujettissements.get(0));
		assertHorsCanton(date(2014, 10, 1), null, MotifAssujettissement.DEPART_HC, null, assujettissements.get(1));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testVaudoisActiviteTransfertSiegeVersHorsCanton() throws Exception {

		final RegDate dateCreationEntreprise = date(2013, 2, 1);
		final RegDate dateDebutActivite = date(2013, 4, 15);
		final RegDate dateDepartSiege = date(2014, 5, 21);

		final Entreprise e = addEntrepriseInconnueAuCivil();
		addRegimeFiscalVD(e, dateCreationEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
		addRegimeFiscalCH(e, dateCreationEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
		addForPrincipal(e, dateCreationEntreprise, MotifFor.INDETERMINE, dateDepartSiege, MotifFor.DEPART_HC, MockCommune.Echallens);
		addForPrincipal(e, dateDepartSiege.getOneDayAfter(), MotifFor.DEPART_HC, MockCommune.Geneve);
		addForSecondaire(e, dateDebutActivite, MotifFor.DEBUT_EXPLOITATION, MockCommune.Cossonay, MotifRattachement.ACTIVITE_INDEPENDANTE, GenreImpot.BENEFICE_CAPITAL);
		addBouclement(e, date(2012, 3, 31), DayMonth.get(3, 31), 12);       // bouclements tous les 31.03 depuis le 31.03.2012
		addBouclement(e, date(2014, 4, 1), DayMonth.get(9, 30), 12);       // bouclements tous les 30.09 depuis le 30.09.2014

		final List<Assujettissement> assujettissements = determine(e);
		assertNotNull(assujettissements);
		assertEquals(2, assujettissements.size());
		assertOrdinaire(dateCreationEntreprise, date(2014, 9, 30), MotifAssujettissement.INDETERMINE, MotifAssujettissement.DEPART_HC, assujettissements.get(0));
		assertHorsCanton(date(2014, 10, 1), null, MotifAssujettissement.DEPART_HC, null, assujettissements.get(1));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testVaudoisImmeubleTransfertSiegeVersHorsSuisse() throws Exception {

		final RegDate dateCreationEntreprise = date(2013, 2, 1);
		final RegDate dateAchat = date(2013, 4, 15);
		final RegDate dateDepartSiege = date(2014, 5, 21);

		final Entreprise e = addEntrepriseInconnueAuCivil();
		addRegimeFiscalVD(e, dateCreationEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
		addRegimeFiscalCH(e, dateCreationEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
		addForPrincipal(e, dateCreationEntreprise, MotifFor.INDETERMINE, dateDepartSiege, MotifFor.DEPART_HS, MockCommune.Echallens);
		addForPrincipal(e, dateDepartSiege.getOneDayAfter(), MotifFor.DEPART_HS, MockPays.Allemagne);
		addForSecondaire(e, dateAchat, MotifFor.ACHAT_IMMOBILIER, MockCommune.Cossonay, MotifRattachement.IMMEUBLE_PRIVE, GenreImpot.BENEFICE_CAPITAL);
		addBouclement(e, date(2012, 3, 31), DayMonth.get(3, 31), 12);       // bouclements tous les 31.03 depuis le 31.03.2012
		addBouclement(e, date(2014, 4, 1), DayMonth.get(9, 30), 12);       // bouclements tous les 30.09 depuis le 30.09.2014

		final List<Assujettissement> assujettissements = determine(e);
		assertNotNull(assujettissements);
		assertEquals(2, assujettissements.size());
		assertOrdinaire(dateCreationEntreprise, dateDepartSiege, MotifAssujettissement.INDETERMINE, MotifAssujettissement.DEPART_HS, assujettissements.get(0));
		assertHorsSuisse(dateDepartSiege.getOneDayAfter(), null, MotifAssujettissement.DEPART_HS, null, assujettissements.get(1));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testVaudoisActiviteTransfertSiegeVersHorsSuisse() throws Exception {

		final RegDate dateCreationEntreprise = date(2013, 2, 1);
		final RegDate dateAchat = date(2013, 4, 15);
		final RegDate dateDepartSiege = date(2014, 5, 21);

		final Entreprise e = addEntrepriseInconnueAuCivil();
		addRegimeFiscalVD(e, dateCreationEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
		addRegimeFiscalCH(e, dateCreationEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
		addForPrincipal(e, dateCreationEntreprise, MotifFor.INDETERMINE, dateDepartSiege, MotifFor.DEPART_HS, MockCommune.Echallens);
		addForPrincipal(e, dateDepartSiege.getOneDayAfter(), MotifFor.DEPART_HS, MockPays.Allemagne);
		addForSecondaire(e, dateAchat, MotifFor.DEBUT_EXPLOITATION, MockCommune.Cossonay, MotifRattachement.ACTIVITE_INDEPENDANTE, GenreImpot.BENEFICE_CAPITAL);
		addBouclement(e, date(2012, 3, 31), DayMonth.get(3, 31), 12);       // bouclements tous les 31.03 depuis le 31.03.2012
		addBouclement(e, date(2014, 4, 1), DayMonth.get(9, 30), 12);       // bouclements tous les 30.09 depuis le 30.09.2014

		final List<Assujettissement> assujettissements = determine(e);
		assertNotNull(assujettissements);
		assertEquals(2, assujettissements.size());
		assertOrdinaire(dateCreationEntreprise, dateDepartSiege, MotifAssujettissement.INDETERMINE, MotifAssujettissement.DEPART_HS, assujettissements.get(0));
		assertHorsSuisse(dateDepartSiege.getOneDayAfter(), null, MotifAssujettissement.DEPART_HS, null, assujettissements.get(1));
	}

	/**
	 * Entreprise vaudoise qui transfère son siège temporairement hors canton (le départ et le retour ne se font pas
	 * sur le même excercice commercial, ni même dans deux excercices successifs) en absence de for secondaire
	 * -> coupure dans l'assujettissement (= le ou les exercices commerciaux intercalaires)
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testVaudoisTransfertSiegeHorsCantonEtRetourSansForSecondaireExercicesDifferentsNonSuccessifs() throws Exception {

		final RegDate dateCreationEntreprise = date(2013, 1, 1);
		final RegDate dateDepartSiege = date(2014, 4, 12);
		final RegDate dateRetourSiege = date(2014, 10, 22);

		final Entreprise e = addEntrepriseInconnueAuCivil();
		addRegimeFiscalVD(e, dateCreationEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
		addRegimeFiscalCH(e, dateCreationEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
		addForPrincipal(e, dateCreationEntreprise, MotifFor.INDETERMINE, dateDepartSiege, MotifFor.DEPART_HC, MockCommune.CheseauxSurLausanne);
		addForPrincipal(e, dateDepartSiege.getOneDayAfter(), MotifFor.DEPART_HC, dateRetourSiege.getOneDayBefore(), MotifFor.ARRIVEE_HC, MockCommune.Geneve);
		addForPrincipal(e, dateRetourSiege, MotifFor.ARRIVEE_HC, MockCommune.Lausanne);
		addBouclement(e, date(2014, 1, 1), DayMonth.get(3, 31), 12);        // bouclements tous les 31.03 depuis le 31.03.2014
		addBouclement(e, date(2014, 4, 1), DayMonth.get(6, 30), 3);         // bouclements tous les 3 mois depuis le 30.06.2014
		addBouclement(e, date(2014, 10, 1), DayMonth.get(9, 30), 12);       // bouclements tous les 30.09 depuis le 30.09.2015

		final List<Assujettissement> assujettissements = determine(e);
		assertNotNull(assujettissements);
		assertEquals(2, assujettissements.size());
		assertOrdinaire(dateCreationEntreprise, date(2014, 6, 30), MotifAssujettissement.INDETERMINE, MotifAssujettissement.DEPART_HC, assujettissements.get(0));
		assertOrdinaire(date(2014, 10, 1), null, MotifAssujettissement.ARRIVEE_HC, null, assujettissements.get(1));
	}

	/**
	 * Entreprise vaudoise qui transfère son siège temporairement hors canton (le départ et le retour ne se font pas
	 * sur le même excercice commercial, mais ceux-ci sont successifs) en absence de for secondaire
	 * -> pas de coupure dans l'assujettissement
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testVaudoisTransfertSiegeHorsCantonEtRetourSansForSecondaireExercicesDifferentsSuccessifs() throws Exception {

		final RegDate dateCreationEntreprise = date(2013, 1, 1);
		final RegDate dateDepartSiege = date(2014, 4, 12);
		final RegDate dateRetourSiege = date(2014, 7, 22);

		final Entreprise e = addEntrepriseInconnueAuCivil();
		addRegimeFiscalVD(e, dateCreationEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
		addRegimeFiscalCH(e, dateCreationEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
		addForPrincipal(e, dateCreationEntreprise, MotifFor.INDETERMINE, dateDepartSiege, MotifFor.DEPART_HC, MockCommune.CheseauxSurLausanne);
		addForPrincipal(e, dateDepartSiege.getOneDayAfter(), MotifFor.DEPART_HC, dateRetourSiege.getOneDayBefore(), MotifFor.ARRIVEE_HC, MockCommune.Geneve);
		addForPrincipal(e, dateRetourSiege, MotifFor.ARRIVEE_HC, MockCommune.Lausanne);
		addBouclement(e, date(2014, 1, 1), DayMonth.get(3, 31), 12);        // bouclements tous les 31.03 depuis le 31.03.2014
		addBouclement(e, date(2014, 4, 1), DayMonth.get(6, 30), 3);         // bouclements tous les 3 mois depuis le 30.06.2014
		addBouclement(e, date(2014, 10, 1), DayMonth.get(9, 30), 12);       // bouclements tous les 30.09 depuis le 30.09.2015

		final List<Assujettissement> assujettissements = determine(e);
		assertNotNull(assujettissements);
		assertEquals(1, assujettissements.size());
		assertOrdinaire(dateCreationEntreprise, null, MotifAssujettissement.INDETERMINE, null, assujettissements.get(0));
	}

	/**
	 * Entreprise vaudoise qui transfère son siège temporairement hors canton (le départ et le retour se font
	 * sur le même excercice commercial) en absence de for secondaire
	 * -> pas de coupure dans l'assujettissement
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testVaudoisTransfertSiegeHorsCantonEtRetourSansForSecondaireMemeExerciceCommercial() throws Exception {

		final RegDate dateCreationEntreprise = date(2013, 1, 1);
		final RegDate dateDepartSiege = date(2014, 4, 12);
		final RegDate dateRetourSiege = date(2014, 7, 22);

		final Entreprise e = addEntrepriseInconnueAuCivil();
		addRegimeFiscalVD(e, dateCreationEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
		addRegimeFiscalCH(e, dateCreationEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
		addForPrincipal(e, dateCreationEntreprise, MotifFor.INDETERMINE, dateDepartSiege, MotifFor.DEPART_HC, MockCommune.CheseauxSurLausanne);
		addForPrincipal(e, dateDepartSiege.getOneDayAfter(), MotifFor.DEPART_HC, dateRetourSiege.getOneDayBefore(), MotifFor.ARRIVEE_HC, MockCommune.Geneve);
		addForPrincipal(e, dateRetourSiege, MotifFor.ARRIVEE_HC, MockCommune.Lausanne);
		addBouclement(e, date(2014, 1, 1), DayMonth.get(3, 31), 12);        // bouclements tous les 31.03 depuis le 31.03.2014
		addBouclement(e, date(2014, 4, 1), DayMonth.get(9, 30), 12);         // bouclements tous les 30.09 mois depuis le 30.09.2014

		final List<Assujettissement> assujettissements = determine(e);
		assertNotNull(assujettissements);
		assertEquals(1, assujettissements.size());
		assertOrdinaire(dateCreationEntreprise, null, MotifAssujettissement.INDETERMINE, null, assujettissements.get(0));
	}

	/**
	 * Entreprise vaudoise qui transfère son siège temporairement hors canton (le départ et le retour ne se font pas
	 * sur le même excercice commercial, ni même dans deux excercices successifs) en présence d'un for secondaire
	 * -> pas de coupure dans l'assujettissement, car le for secondaire comble le trou
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testVaudoisTransfertSiegeHorsCantonEtRetourAvecImmeubleExercicesDifferentsNonSuccessifs() throws Exception {

		final RegDate dateCreationEntreprise = date(2013, 1, 1);
		final RegDate dateDepartSiege = date(2014, 4, 12);
		final RegDate dateRetourSiege = date(2014, 10, 22);
		final RegDate dateDebutForSecondaire = dateDepartSiege.addDays(-6);
		final RegDate dateFinForSecondaire = date(2014, 7, 29);

		final Entreprise e = addEntrepriseInconnueAuCivil();
		addRegimeFiscalVD(e, dateCreationEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
		addRegimeFiscalCH(e, dateCreationEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
		addForPrincipal(e, dateCreationEntreprise, MotifFor.INDETERMINE, dateDepartSiege, MotifFor.DEPART_HC, MockCommune.CheseauxSurLausanne);
		addForPrincipal(e, dateDepartSiege.getOneDayAfter(), MotifFor.DEPART_HC, dateRetourSiege.getOneDayBefore(), MotifFor.ARRIVEE_HC, MockCommune.Geneve);
		addForPrincipal(e, dateRetourSiege, MotifFor.ARRIVEE_HC, MockCommune.Lausanne);
		addForSecondaire(e, dateDebutForSecondaire, MotifFor.ACHAT_IMMOBILIER, dateFinForSecondaire, MotifFor.VENTE_IMMOBILIER, MockCommune.Cossonay, MotifRattachement.IMMEUBLE_PRIVE, GenreImpot.BENEFICE_CAPITAL);
		addBouclement(e, date(2014, 1, 1), DayMonth.get(3, 31), 12);        // bouclements tous les 31.03 depuis le 31.03.2014
		addBouclement(e, date(2014, 4, 1), DayMonth.get(6, 30), 3);         // bouclements tous les 3 mois depuis le 30.06.2014
		addBouclement(e, date(2014, 10, 1), DayMonth.get(9, 30), 12);       // bouclements tous les 30.09 depuis le 30.09.2015

		final List<Assujettissement> assujettissements = determine(e);
		assertNotNull(assujettissements);
		assertEquals(3, assujettissements.size());
		assertOrdinaire(dateCreationEntreprise, date(2014, 6, 30), MotifAssujettissement.INDETERMINE, MotifAssujettissement.DEPART_HC, assujettissements.get(0));
		assertHorsCanton(date(2014, 7, 1), date(2014, 9, 30), MotifAssujettissement.DEPART_HC, MotifAssujettissement.ARRIVEE_HC, assujettissements.get(1));
		assertOrdinaire(date(2014, 10, 1), null, MotifAssujettissement.ARRIVEE_HC, null, assujettissements.get(2));
	}

	/**
	 * Entreprise vaudoise qui transfère son siège temporairement hors Suisse (le départ et le retour ne se font pas
	 * sur le même excercice commercial, ni même dans deux excercices successifs) en absence de for secondaire
	 * -> coupure dans l'assujettissement (aux dates de départ/arrivée)
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testVaudoisTransfertSiegeHorsSuisseEtRetourSansForSecondaireExercicesDifferentsNonSuccessifs() throws Exception {

		final RegDate dateCreationEntreprise = date(2013, 1, 1);
		final RegDate dateDepartSiege = date(2014, 4, 12);
		final RegDate dateRetourSiege = date(2014, 10, 22);

		final Entreprise e = addEntrepriseInconnueAuCivil();
		addRegimeFiscalVD(e, dateCreationEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
		addRegimeFiscalCH(e, dateCreationEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
		addForPrincipal(e, dateCreationEntreprise, MotifFor.INDETERMINE, dateDepartSiege, MotifFor.DEPART_HS, MockCommune.CheseauxSurLausanne);
		addForPrincipal(e, dateDepartSiege.getOneDayAfter(), MotifFor.DEPART_HS, dateRetourSiege.getOneDayBefore(), MotifFor.ARRIVEE_HS, MockPays.Allemagne);
		addForPrincipal(e, dateRetourSiege, MotifFor.ARRIVEE_HS, MockCommune.Lausanne);
		addBouclement(e, date(2014, 1, 1), DayMonth.get(3, 31), 12);        // bouclements tous les 31.03 depuis le 31.03.2014
		addBouclement(e, date(2014, 4, 1), DayMonth.get(6, 30), 3);         // bouclements tous les 3 mois depuis le 30.06.2014
		addBouclement(e, date(2014, 10, 1), DayMonth.get(9, 30), 12);       // bouclements tous les 30.09 depuis le 30.09.2015

		final List<Assujettissement> assujettissements = determine(e);
		assertNotNull(assujettissements);
		assertEquals(2, assujettissements.size());
		assertOrdinaire(dateCreationEntreprise, dateDepartSiege, MotifAssujettissement.INDETERMINE, MotifAssujettissement.DEPART_HS, assujettissements.get(0));
		assertOrdinaire(dateRetourSiege, null, MotifAssujettissement.ARRIVEE_HS, null, assujettissements.get(1));
	}

	/**
	 * Entreprise vaudoise qui transfère son siège temporairement hors Suisse (le départ et le retour ne se font pas
	 * sur le même excercice commercial, mais ceux-ci sont successifs) en absence de for secondaire
	 * -> coupure dans l'assujettissement (aux dates de départ/arrivée)
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testVaudoisTransfertSiegeHorsSuisseEtRetourSansForSecondaireExercicesDifferentsSuccessifs() throws Exception {

		final RegDate dateCreationEntreprise = date(2013, 1, 1);
		final RegDate dateDepartSiege = date(2014, 4, 12);
		final RegDate dateRetourSiege = date(2014, 7, 22);

		final Entreprise e = addEntrepriseInconnueAuCivil();
		addRegimeFiscalVD(e, dateCreationEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
		addRegimeFiscalCH(e, dateCreationEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
		addForPrincipal(e, dateCreationEntreprise, MotifFor.INDETERMINE, dateDepartSiege, MotifFor.DEPART_HS, MockCommune.CheseauxSurLausanne);
		addForPrincipal(e, dateDepartSiege.getOneDayAfter(), MotifFor.DEPART_HS, dateRetourSiege.getOneDayBefore(), MotifFor.ARRIVEE_HS, MockPays.Allemagne);
		addForPrincipal(e, dateRetourSiege, MotifFor.ARRIVEE_HS, MockCommune.Lausanne);
		addBouclement(e, date(2014, 1, 1), DayMonth.get(3, 31), 12);        // bouclements tous les 31.03 depuis le 31.03.2014
		addBouclement(e, date(2014, 4, 1), DayMonth.get(6, 30), 3);         // bouclements tous les 3 mois depuis le 30.06.2014
		addBouclement(e, date(2014, 10, 1), DayMonth.get(9, 30), 12);       // bouclements tous les 30.09 depuis le 30.09.2015

		final List<Assujettissement> assujettissements = determine(e);
		assertNotNull(assujettissements);
		assertEquals(2, assujettissements.size());
		assertOrdinaire(dateCreationEntreprise, dateDepartSiege, MotifAssujettissement.INDETERMINE, MotifAssujettissement.DEPART_HS, assujettissements.get(0));
		assertOrdinaire(dateRetourSiege, null, MotifAssujettissement.ARRIVEE_HS, null, assujettissements.get(1));
	}

	/**
	 * Entreprise vaudoise qui transfère son siège temporairement hors Suisse (le départ et le retour se font
	 * sur le même excercice commercial) en absence de for secondaire
	 * -> coupure dans l'assujettissement (aux dates de départ/arrivée)
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testVaudoisTransfertSiegeHorsSuisseEtRetourSansForSecondaireMemeExerciceCommercial() throws Exception {

		final RegDate dateCreationEntreprise = date(2013, 1, 1);
		final RegDate dateDepartSiege = date(2014, 4, 12);
		final RegDate dateRetourSiege = date(2014, 7, 22);

		final Entreprise e = addEntrepriseInconnueAuCivil();
		addRegimeFiscalVD(e, dateCreationEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
		addRegimeFiscalCH(e, dateCreationEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
		addForPrincipal(e, dateCreationEntreprise, MotifFor.INDETERMINE, dateDepartSiege, MotifFor.DEPART_HS, MockCommune.CheseauxSurLausanne);
		addForPrincipal(e, dateDepartSiege.getOneDayAfter(), MotifFor.DEPART_HS, dateRetourSiege.getOneDayBefore(), MotifFor.ARRIVEE_HS, MockPays.Allemagne);
		addForPrincipal(e, dateRetourSiege, MotifFor.ARRIVEE_HS, MockCommune.Lausanne);
		addBouclement(e, date(2014, 1, 1), DayMonth.get(3, 31), 12);        // bouclements tous les 31.03 depuis le 31.03.2014
		addBouclement(e, date(2014, 4, 1), DayMonth.get(9, 30), 12);         // bouclements tous les 30.09 mois depuis le 30.09.2014

		final List<Assujettissement> assujettissements = determine(e);
		assertNotNull(assujettissements);
		assertEquals(2, assujettissements.size());
		assertOrdinaire(dateCreationEntreprise, dateDepartSiege, MotifAssujettissement.INDETERMINE, MotifAssujettissement.DEPART_HS, assujettissements.get(0));
		assertOrdinaire(dateRetourSiege, null, MotifAssujettissement.ARRIVEE_HS, null, assujettissements.get(1));
	}

	/**
	 * Entreprise vaudoise qui transfère son siège temporairement hors Suisse (le départ et le retour ne se font pas
	 * sur le même excercice commercial, ni même dans deux excercices successifs) en présence d'un for secondaire
	 * -> coupure dans l'assujettissement (aux dates de départ/arrivée associées aux débuts/fins du for secondaire)
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testVaudoisTransfertSiegeHorsSuisseEtRetourAvecImmeubleExercicesDifferentsNonSuccessifs() throws Exception {

		final RegDate dateCreationEntreprise = date(2013, 1, 1);
		final RegDate dateDepartSiege = date(2014, 4, 12);
		final RegDate dateRetourSiege = date(2014, 10, 22);
		final RegDate dateDebutForSecondaire = dateDepartSiege.addDays(-6);
		final RegDate dateFinForSecondaire = date(2014, 7, 29);

		final Entreprise e = addEntrepriseInconnueAuCivil();
		addRegimeFiscalVD(e, dateCreationEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
		addRegimeFiscalCH(e, dateCreationEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
		addForPrincipal(e, dateCreationEntreprise, MotifFor.INDETERMINE, dateDepartSiege, MotifFor.DEPART_HS, MockCommune.CheseauxSurLausanne);
		addForPrincipal(e, dateDepartSiege.getOneDayAfter(), MotifFor.DEPART_HS, dateRetourSiege.getOneDayBefore(), MotifFor.ARRIVEE_HS, MockPays.Allemagne);
		addForPrincipal(e, dateRetourSiege, MotifFor.ARRIVEE_HS, MockCommune.Lausanne);
		addForSecondaire(e, dateDebutForSecondaire, MotifFor.ACHAT_IMMOBILIER, dateFinForSecondaire, MotifFor.VENTE_IMMOBILIER, MockCommune.Cossonay, MotifRattachement.IMMEUBLE_PRIVE, GenreImpot.BENEFICE_CAPITAL);
		addBouclement(e, date(2014, 1, 1), DayMonth.get(3, 31), 12);        // bouclements tous les 31.03 depuis le 31.03.2014
		addBouclement(e, date(2014, 4, 1), DayMonth.get(6, 30), 3);         // bouclements tous les 3 mois depuis le 30.06.2014
		addBouclement(e, date(2014, 10, 1), DayMonth.get(9, 30), 12);       // bouclements tous les 30.09 depuis le 30.09.2015

		final List<Assujettissement> assujettissements = determine(e);
		assertNotNull(assujettissements);
		assertEquals(3, assujettissements.size());
		assertOrdinaire(dateCreationEntreprise, dateDepartSiege, MotifAssujettissement.INDETERMINE, MotifAssujettissement.DEPART_HS, assujettissements.get(0));
		assertHorsSuisse(dateDepartSiege.getOneDayAfter(), dateFinForSecondaire, MotifAssujettissement.DEPART_HS, MotifAssujettissement.VENTE_IMMOBILIER, assujettissements.get(1));
		assertOrdinaire(dateRetourSiege, null, MotifAssujettissement.ARRIVEE_HS, null, assujettissements.get(2));
	}

	/**
	 * Vu pendant les tests de migration : il semble que quand on a un déménagement, une exception saute qui dit que deux assujettissements entrent en collision,,,
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testAvecDemenagementVaudois() throws Exception {

		final RegDate dateCreation = date(2011, 7, 12);
		final RegDate dateDemenagement = date(2014, 4, 7);

		final Entreprise e = addEntrepriseInconnueAuCivil();
		addRegimeFiscalVD(e, dateCreation, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
		addRegimeFiscalCH(e, dateCreation, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
		addForPrincipal(e, dateCreation, MotifFor.INDETERMINE, dateDemenagement.getOneDayBefore(), MotifFor.DEMENAGEMENT_VD, MockCommune.Lausanne);
		addForPrincipal(e, dateDemenagement, MotifFor.DEMENAGEMENT_VD, MockCommune.Prilly);
		addBouclement(e, date(2012, 12, 1), DayMonth.get(12, 31), 12);      // bouclements tous les 31.12 depuis le 31.12.2012

		final List<Assujettissement> assujettissements = determine(e);
		assertNotNull(assujettissements);
		assertEquals(1, assujettissements.size());
		assertOrdinaire(dateCreation, null, MotifAssujettissement.INDETERMINE, null, assujettissements.get(0));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testAvecForsPrincipauxNonBeneficeCapital() throws Exception {

		final RegDate dateCreation = date(2011, 7, 12);

		final Entreprise e = addEntrepriseInconnueAuCivil();
		addRaisonSociale(e, dateCreation, null, "L'avenir du genre humain");
		addFormeJuridique(e, dateCreation, null, FormeJuridiqueEntreprise.SNC);
		addRegimeFiscalVD(e, dateCreation, null, MockTypeRegimeFiscal.SOCIETE_PERS);
		addForPrincipal(e, dateCreation, MotifFor.INDETERMINE, MockCommune.Lausanne, GenreImpot.REVENU_FORTUNE);
		addBouclement(e, date(2012, 12, 1), DayMonth.get(12, 31), 12);      // bouclements tous les 31.12 depuis le 31.12.2012

		final List<Assujettissement> assujettissements = determine(e);
		assertEmpty(assujettissements);
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testFinAssujettissementFaillite() throws Exception {

		final RegDate dateCreation = date(2012, 4, 12);
		final RegDate dateLiquidation = date(2014, 8, 25);

		final Entreprise e = addEntrepriseInconnueAuCivil();
		addRaisonSociale(e, dateCreation, null, "Turlutu sàrl");
		addFormeJuridique(e, dateCreation, null, FormeJuridiqueEntreprise.SARL);
		addRegimeFiscalVD(e, dateCreation, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
		addRegimeFiscalCH(e, dateCreation, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
		addForPrincipal(e, dateCreation, MotifFor.DEBUT_EXPLOITATION, dateLiquidation, MotifFor.FAILLITE, MockCommune.Lausanne);
		addBouclement(e, date(2012, 9, 1), DayMonth.get(9, 30), 12);      // bouclements tous les 30.09 depuis le 30.09.2012

		final List<Assujettissement> assujettissements = determine(e);
		assertNotNull(assujettissements);
		assertEquals(1, assujettissements.size());
		assertOrdinaire(dateCreation, date(2014, 9, 30), MotifAssujettissement.DEBUT_EXPLOITATION, MotifAssujettissement.FAILLITE, assujettissements.get(0));

	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testFinAssujettissementFusion() throws Exception {

		final RegDate dateCreation = date(2012, 4, 12);
		final RegDate dateLiquidation = date(2014, 8, 25);

		final Entreprise e = addEntrepriseInconnueAuCivil();
		addRaisonSociale(e, dateCreation, null, "Turlutu sàrl");
		addFormeJuridique(e, dateCreation, null, FormeJuridiqueEntreprise.SARL);
		addRegimeFiscalVD(e, dateCreation, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
		addRegimeFiscalCH(e, dateCreation, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
		addForPrincipal(e, dateCreation, MotifFor.DEBUT_EXPLOITATION, dateLiquidation, MotifFor.FUSION_ENTREPRISES, MockCommune.Lausanne);
		addBouclement(e, date(2012, 9, 1), DayMonth.get(9, 30), 12);      // bouclements tous les 30.09 depuis le 30.09.2012

		final List<Assujettissement> assujettissements = determine(e);
		assertNotNull(assujettissements);
		assertEquals(1, assujettissements.size());
		assertOrdinaire(dateCreation, dateLiquidation, MotifAssujettissement.DEBUT_EXPLOITATION, MotifAssujettissement.FUSION_ENTREPRISES, assujettissements.get(0));
	}

	/**
	 * Cas du contribuable 466 qui avait un assujettissement 2011 en 16R3 mais l'a perdu en 16R4 dans le cas d'un déménagement vaudois
	 * pile sur une date de bouclement...
	 * (dans les tests, les Mocks présentant une fusion de Cully en Bourg-en-Lavaux en 2010 plutôt que 2011, j'ai tout ramené d'un an aussi...)
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDemenagementVaudois() throws Exception {

		final RegDate dateCreation = date(2009, 1, 1);
		final RegDate dateDemenagement = date(2011, 1, 1);

		final Entreprise e = addEntrepriseInconnueAuCivil();
		addRaisonSociale(e, dateCreation, null, "Chauffage SA");
		addFormeJuridique(e, dateCreation, null, FormeJuridiqueEntreprise.SA);
		addRegimeFiscalCH(e, dateCreation, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
		addRegimeFiscalVD(e, dateCreation, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
		addBouclement(e, dateCreation, DayMonth.get(12, 31), 12);       // tous les ans au 31.12
		addForPrincipal(e, dateCreation, MotifFor.DEBUT_EXPLOITATION, dateDemenagement.getOneDayBefore(), MotifFor.DEMENAGEMENT_VD, MockCommune.Cully, GenreImpot.BENEFICE_CAPITAL);
		addForPrincipal(e, dateDemenagement, MotifFor.DEMENAGEMENT_VD, MockCommune.BourgEnLavaux, GenreImpot.BENEFICE_CAPITAL);

		final List<Assujettissement> assujettissements = determine(e);
		assertNotNull(assujettissements);
		assertEquals(1, assujettissements.size());
		assertOrdinaire(dateCreation, null, MotifAssujettissement.DEBUT_EXPLOITATION, null, assujettissements.get(0));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testExonerationVaudoisExercicesEtForsCalesSurAnneeCivile() throws Exception {
		final Entreprise e = addEntrepriseInconnueAuCivil();
		addRegimeFiscalVD(e, date(2013, 1, 1), null, MockTypeRegimeFiscal.EXO_90C);
		addRegimeFiscalCH(e, date(2013, 1, 1), null, MockTypeRegimeFiscal.EXO_90C);
		addForPrincipal(e, date(2013, 1, 1), MotifFor.INDETERMINE, MockCommune.Aigle);
		addBouclement(e, date(2013, 1, 1), DayMonth.get(12, 31), 12);       // bouclements tous les 31.12 depuis le 31.12.2013

		final List<Assujettissement> assujettissements = determine(e);
		assertEmpty(assujettissements);
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testExonerationVaudoisExercicesCalesSurAnneeCivileMaisPasPremierFor() throws Exception {
		final Entreprise e = addEntrepriseInconnueAuCivil();
		addRegimeFiscalVD(e, date(2013, 4, 21), null, MockTypeRegimeFiscal.EXO_90C);
		addRegimeFiscalCH(e, date(2013, 4, 21), null, MockTypeRegimeFiscal.EXO_90C);
		addForPrincipal(e, date(2013, 4, 21), MotifFor.INDETERMINE, MockCommune.Aigle);
		addBouclement(e, date(2013, 1, 1), DayMonth.get(12, 31), 12);       // bouclements tous les 31.12 depuis le 31.12.2013

		final List<Assujettissement> assujettissements = determine(e);
		assertEmpty(assujettissements);
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testExonerationVaudoisExercicesNonCalesSurAnneeCivile() throws Exception {
		final Entreprise e = addEntrepriseInconnueAuCivil();
		addRegimeFiscalVD(e, date(2013, 1, 15), null, MockTypeRegimeFiscal.EXO_90C);
		addRegimeFiscalCH(e, date(2013, 1, 15), null, MockTypeRegimeFiscal.EXO_90C);
		addForPrincipal(e, date(2013, 1, 15), MotifFor.INDETERMINE, MockCommune.Aigle);
		addBouclement(e, date(2013, 1, 1), DayMonth.get(6, 30), 12);    // bouclements tous les 30.06 depuis le 31.03.2013

		final List<Assujettissement> assujettissements = determine(e);
		assertEmpty(assujettissements);
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testExonerationVaudoisModificationDureeExcercicesCommerciauxDebutAnneeCivile() throws Exception {
		final Entreprise e = addEntrepriseInconnueAuCivil();
		addRegimeFiscalVD(e, date(2013, 1, 15), null, MockTypeRegimeFiscal.EXO_90C);
		addRegimeFiscalCH(e, date(2013, 1, 15), null, MockTypeRegimeFiscal.EXO_90C);
		addForPrincipal(e, date(2013, 1, 15), MotifFor.INDETERMINE, MockCommune.Aigle);
		addBouclement(e, date(2013, 1, 1), DayMonth.get(12, 31), 12);    // bouclements tous les 31.12 depuis le 31.12.2013
		addBouclement(e, date(2014, 3, 31), DayMonth.get(3, 31), 12);    // bouclements tous les 31.03 depuis le 31.03.2014
		addBouclement(e, date(2014, 4, 1), DayMonth.get(12, 31), 12);    // bouclements tous les 31.12 depuis le 31.12.2014

		final List<Assujettissement> assujettissements = determine(e);
		assertEmpty(assujettissements);
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testExonerationVaudoisModificationDureeExcercicesCommerciauxEnCoursAnneeCivile() throws Exception {
		final Entreprise e = addEntrepriseInconnueAuCivil();
		addRegimeFiscalVD(e, date(2013, 1, 15), null, MockTypeRegimeFiscal.EXO_90C);
		addRegimeFiscalCH(e, date(2013, 1, 15), null, MockTypeRegimeFiscal.EXO_90C);
		addForPrincipal(e, date(2013, 1, 15), MotifFor.INDETERMINE, MockCommune.Aigle);
		addBouclement(e, date(2013, 3, 31), DayMonth.get(3, 31), 12);   // bouclements tous les 31.03 depuis le 31.03.2013
		addBouclement(e, date(2014, 4, 1), DayMonth.get(6, 30), 12);    // bouclements tous les 30.06 depuis le 30.06.2014
		addBouclement(e, date(2014, 7, 1), DayMonth.get(3, 31), 12);    // bouclements tous les 31.03 depuis le 31.03.2015

		final List<Assujettissement> assujettissements = determine(e);
		assertEmpty(assujettissements);
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testExonerationHorsSuisseActiviteNonSynchroneAvecExercice() throws Exception {

		final RegDate dateDebutExploitation = date(2013, 4, 1);

		final Entreprise e = addEntrepriseInconnueAuCivil();
		addRegimeFiscalVD(e, dateDebutExploitation, null, MockTypeRegimeFiscal.EXO_90C);
		addRegimeFiscalCH(e, dateDebutExploitation, null, MockTypeRegimeFiscal.EXO_90C);
		addForPrincipal(e, dateDebutExploitation, null, MockPays.RoyaumeUni);
		addForSecondaire(e, dateDebutExploitation, MotifFor.DEBUT_EXPLOITATION, MockCommune.Cossonay, MotifRattachement.ACTIVITE_INDEPENDANTE, GenreImpot.BENEFICE_CAPITAL);
		addBouclement(e, date(2012, 12, 31), DayMonth.get(12, 31), 12);       // bouclements tous les 31.12 depuis le 31.12.2012

		final List<Assujettissement> assujettissements = determine(e);
		assertEmpty(assujettissements);
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testExonerationHorsCantonActiviteTransfertSiegeVersVaud() throws Exception {

		final RegDate dateDebutExploitation = date(2013, 4, 15);
		final RegDate dateArriveeSiege = date(2014, 5, 21);

		final Entreprise e = addEntrepriseInconnueAuCivil();
		addRegimeFiscalVD(e, dateDebutExploitation, null, MockTypeRegimeFiscal.EXO_90C);
		addRegimeFiscalCH(e, dateDebutExploitation, null, MockTypeRegimeFiscal.EXO_90C);
		addForPrincipal(e, dateDebutExploitation, null, dateArriveeSiege.getOneDayBefore(), MotifFor.ARRIVEE_HC, MockCommune.Bale);
		addForPrincipal(e, dateArriveeSiege, MotifFor.ARRIVEE_HC, MockCommune.Morges);
		addForSecondaire(e, dateDebutExploitation, MotifFor.DEBUT_EXPLOITATION, MockCommune.Cossonay, MotifRattachement.ACTIVITE_INDEPENDANTE, GenreImpot.BENEFICE_CAPITAL);
		addBouclement(e, date(2012, 3, 31), DayMonth.get(3, 31), 12);       // bouclements tous les 31.03 depuis le 31.03.2012

		final List<Assujettissement> assujettissements = determine(e);
		assertEmpty(assujettissements);
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testExonerationVaudoisActiviteTransfertSiegeVersHorsCanton() throws Exception {

		final RegDate dateCreationEntreprise = date(2013, 2, 1);
		final RegDate dateDebutActivite = date(2013, 4, 15);
		final RegDate dateDepartSiege = date(2014, 5, 21);

		final Entreprise e = addEntrepriseInconnueAuCivil();
		addRegimeFiscalVD(e, dateCreationEntreprise, null, MockTypeRegimeFiscal.EXO_90C);
		addRegimeFiscalCH(e, dateCreationEntreprise, null, MockTypeRegimeFiscal.EXO_90C);
		addForPrincipal(e, dateCreationEntreprise, MotifFor.INDETERMINE, dateDepartSiege, MotifFor.DEPART_HC, MockCommune.Echallens);
		addForPrincipal(e, dateDepartSiege.getOneDayAfter(), MotifFor.DEPART_HC, MockCommune.Geneve);
		addForSecondaire(e, dateDebutActivite, MotifFor.DEBUT_EXPLOITATION, MockCommune.Cossonay, MotifRattachement.ACTIVITE_INDEPENDANTE, GenreImpot.BENEFICE_CAPITAL);
		addBouclement(e, date(2012, 3, 31), DayMonth.get(3, 31), 12);       // bouclements tous les 31.03 depuis le 31.03.2012
		addBouclement(e, date(2014, 4, 1), DayMonth.get(9, 30), 12);       // bouclements tous les 30.09 depuis le 30.09.2014

		final List<Assujettissement> assujettissements = determine(e);
		assertEmpty(assujettissements);
	}

	/**
	 * Entreprise vaudoise qui transfère son siège temporairement hors canton (le départ et le retour se font
	 * sur le même excercice commercial) en absence de for secondaire
	 * -> pas de coupure dans l'assujettissement
	 *
	 * Pas d'exonération pour une éxonération qui n'est plus en vigueur au dernier jour de l'exercice, et qui ne l'était pas déjà à la fin du précédent.
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testExonerationNonEffectiveVaudoisTransfertSiegeHorsCantonEtRetourSansForSecondaireMemeExerciceCommercial() throws Exception {

		final RegDate dateCreationEntreprise = date(2013, 1, 1);
		final RegDate dateDepartSiege = date(2014, 4, 12);
		final RegDate dateRetourSiege = date(2014, 7, 22);

		final Entreprise e = addEntrepriseInconnueAuCivil();
		addRegimeFiscalVD(e, dateCreationEntreprise, dateDepartSiege.getOneDayBefore(), MockTypeRegimeFiscal.ORDINAIRE_PM);
		addRegimeFiscalCH(e, dateCreationEntreprise, dateDepartSiege.getOneDayBefore(), MockTypeRegimeFiscal.ORDINAIRE_PM);
		addRegimeFiscalVD(e, dateDepartSiege, dateRetourSiege.getOneDayBefore(), MockTypeRegimeFiscal.EXO_90C);
		addRegimeFiscalCH(e, dateDepartSiege, dateRetourSiege.getOneDayBefore(), MockTypeRegimeFiscal.EXO_90C);
		addRegimeFiscalVD(e, dateRetourSiege, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
		addRegimeFiscalCH(e, dateRetourSiege, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
		addForPrincipal(e, dateCreationEntreprise, MotifFor.INDETERMINE, dateDepartSiege, MotifFor.DEPART_HC, MockCommune.CheseauxSurLausanne);
		addForPrincipal(e, dateDepartSiege.getOneDayAfter(), MotifFor.DEPART_HC, dateRetourSiege.getOneDayBefore(), MotifFor.ARRIVEE_HC, MockCommune.Geneve);
		addForPrincipal(e, dateRetourSiege, MotifFor.ARRIVEE_HC, MockCommune.Lausanne);
		addBouclement(e, date(2014, 1, 1), DayMonth.get(3, 31), 12);        // bouclements tous les 31.03 depuis le 31.03.2014
		addBouclement(e, date(2014, 4, 1), DayMonth.get(9, 30), 12);         // bouclements tous les 30.09 mois depuis le 30.09.2014

		final List<Assujettissement> assujettissements = determine(e);
		assertNotNull(assujettissements);
		assertEquals(1, assujettissements.size());
		assertOrdinaire(dateCreationEntreprise, null, MotifAssujettissement.INDETERMINE, null, assujettissements.get(0));
	}

	/**
	 * Entreprise vaudoise qui transfère son siège temporairement hors canton (le départ et le retour se font
	 * sur le même excercice commercial) en absence de for secondaire
	 * -> pas de coupure dans l'assujettissement
	 *
	 * Exonération portant sur l'exercice qui va du 31.3.2014 au 30.9.2014.
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testExonerationPartielleVaudoisTransfertSiegeHorsCantonEtRetourSansForSecondaireMemeExerciceCommercial() throws Exception {

		final RegDate dateCreationEntreprise = date(2013, 1, 1);
		final RegDate dateDepartSiege = date(2014, 4, 12);
		final RegDate dateRetourSiege = date(2014, 7, 22);
		final RegDate dateFinRegime = date(2015, 3, 21);

		final Entreprise e = addEntrepriseInconnueAuCivil();
		addRegimeFiscalVD(e, dateCreationEntreprise, dateDepartSiege.getOneDayBefore(), MockTypeRegimeFiscal.ORDINAIRE_PM);
		addRegimeFiscalCH(e, dateCreationEntreprise, dateDepartSiege.getOneDayBefore(), MockTypeRegimeFiscal.ORDINAIRE_PM);
		addRegimeFiscalVD(e, dateDepartSiege, dateFinRegime, MockTypeRegimeFiscal.EXO_90C);
		addRegimeFiscalCH(e, dateDepartSiege, dateFinRegime, MockTypeRegimeFiscal.EXO_90C);
		addRegimeFiscalVD(e, dateFinRegime.getOneDayAfter(), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
		addRegimeFiscalCH(e, dateFinRegime.getOneDayAfter(), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
		addForPrincipal(e, dateCreationEntreprise, MotifFor.INDETERMINE, dateDepartSiege, MotifFor.DEPART_HC, MockCommune.CheseauxSurLausanne);
		addForPrincipal(e, dateDepartSiege.getOneDayAfter(), MotifFor.DEPART_HC, dateRetourSiege.getOneDayBefore(), MotifFor.ARRIVEE_HC, MockCommune.Geneve);
		addForPrincipal(e, dateRetourSiege, MotifFor.ARRIVEE_HC, MockCommune.Lausanne);
		addBouclement(e, date(2014, 1, 1), DayMonth.get(3, 31), 12);        // bouclements tous les 31.03 depuis le 31.03.2014
		addBouclement(e, date(2014, 4, 1), DayMonth.get(9, 30), 12);         // bouclements tous les 30.09 mois depuis le 30.09.2014

		final List<Assujettissement> assujettissements = determine(e);
		assertNotNull(assujettissements);
		assertEquals(2, assujettissements.size());
		assertOrdinaire(dateCreationEntreprise, date(2014, 3, 31), MotifAssujettissement.INDETERMINE, MotifAssujettissement.EXONERATION, assujettissements.get(0));
		assertOrdinaire(date(2014, 10, 1), null, MotifAssujettissement.EXONERATION, null, assujettissements.get(1));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testExonerationAuDebutChangementTropTotVaudois() throws Exception {
		final Entreprise e = addEntrepriseInconnueAuCivil();
		addRegimeFiscalVD(e, date(2013, 4, 21), date(2013, 12, 30), MockTypeRegimeFiscal.EXO_90C);
		addRegimeFiscalCH(e, date(2013, 4, 21), date(2013, 12, 30), MockTypeRegimeFiscal.EXO_90C);
		addRegimeFiscalVD(e, date(2013, 12, 31), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
		addRegimeFiscalCH(e, date(2013, 12, 31), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
		addForPrincipal(e, date(2013, 4, 21), MotifFor.INDETERMINE, MockCommune.Aigle);
		addBouclement(e, date(2013, 1, 1), DayMonth.get(12, 31), 12);       // bouclements tous les 31.12 depuis le 31.12.2013

		final List<Assujettissement> assujettissements = determine(e);
		assertNotNull(assujettissements);
		assertEquals(1, assujettissements.size());
		assertOrdinaire(date(2013, 4, 21), null, MotifAssujettissement.INDETERMINE, null, assujettissements.get(0));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testExonerationAuDebutChangementAuBouclementVaudois() throws Exception {
		final Entreprise e = addEntrepriseInconnueAuCivil();
		addRegimeFiscalVD(e, date(2013, 4, 21), date(2013, 12, 31), MockTypeRegimeFiscal.EXO_90C);
		addRegimeFiscalCH(e, date(2013, 4, 21), date(2013, 12, 31), MockTypeRegimeFiscal.EXO_90C);
		addRegimeFiscalVD(e, date(2014, 1, 1), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
		addRegimeFiscalCH(e, date(2014, 1, 1), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
		addForPrincipal(e, date(2013, 4, 21), MotifFor.INDETERMINE, MockCommune.Aigle);
		addBouclement(e, date(2013, 1, 1), DayMonth.get(12, 31), 12);       // bouclements tous les 31.12 depuis le 31.12.2013

		final List<Assujettissement> assujettissements = determine(e);
		assertNotNull(assujettissements);
		assertEquals(1, assujettissements.size());
		assertOrdinaire(date(2014, 1, 1), null, MotifAssujettissement.EXONERATION, null, assujettissements.get(0));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testExonerationAuDebutChangementPlusTardifVaudois() throws Exception {
		final Entreprise e = addEntrepriseInconnueAuCivil();
		addRegimeFiscalVD(e, date(2013, 4, 21), date(2014, 1, 15), MockTypeRegimeFiscal.EXO_90C);
		addRegimeFiscalCH(e, date(2013, 4, 21), date(2014, 1, 15), MockTypeRegimeFiscal.EXO_90C);
		addRegimeFiscalVD(e, date(2014, 1, 16), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
		addRegimeFiscalCH(e, date(2014, 1, 16), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
		addForPrincipal(e, date(2013, 4, 21), MotifFor.INDETERMINE, MockCommune.Aigle);
		addBouclement(e, date(2013, 1, 1), DayMonth.get(12, 31), 12);       // bouclements tous les 31.12 depuis le 31.12.2013

		final List<Assujettissement> assujettissements = determine(e);
		assertNotNull(assujettissements);
		assertEquals(1, assujettissements.size());
		assertOrdinaire(date(2014, 1, 1), null, MotifAssujettissement.EXONERATION, null, assujettissements.get(0));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testExonerationApresChangementFormeJuridiqueVaudois() throws Exception {
		final Entreprise e = addEntrepriseInconnueAuCivil();
		addRegimeFiscalVD(e, date(2013, 4, 21), date(2013, 12, 31), MockTypeRegimeFiscal.ORDINAIRE_PM);
		addRegimeFiscalCH(e, date(2013, 4, 21), date(2013, 12, 31), MockTypeRegimeFiscal.ORDINAIRE_PM);
		addRegimeFiscalVD(e, date(2014, 1, 1), null, MockTypeRegimeFiscal.EXO_90C);
		addRegimeFiscalCH(e, date(2014, 1, 1), null, MockTypeRegimeFiscal.EXO_90C);
		addForPrincipal(e, date(2013, 4, 21), MotifFor.INDETERMINE, MockCommune.Aigle);
		addBouclement(e, date(2013, 1, 1), DayMonth.get(12, 31), 12);       // bouclements tous les 31.12 depuis le 31.12.2013

		final List<Assujettissement> assujettissements = determine(e);
		assertNotNull(assujettissements);
		assertEquals(1, assujettissements.size());
		assertOrdinaire(date(2013, 4, 21), date(2013, 12, 31), MotifAssujettissement.INDETERMINE, MotifAssujettissement.EXONERATION, assujettissements.get(0));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testExonerationChangementFormeJuridiqueVaudoisIntervientTropTot() throws Exception {
		final Entreprise e = addEntrepriseInconnueAuCivil();
		addRegimeFiscalVD(e, date(2013, 4, 21), date(2013, 12, 30), MockTypeRegimeFiscal.ORDINAIRE_PM);
		addRegimeFiscalCH(e, date(2013, 4, 21), date(2013, 12, 30), MockTypeRegimeFiscal.ORDINAIRE_PM);
		addRegimeFiscalVD(e, date(2013, 12, 31), null, MockTypeRegimeFiscal.EXO_90C);
		addRegimeFiscalCH(e, date(2013, 12, 31), null, MockTypeRegimeFiscal.EXO_90C);
		addForPrincipal(e, date(2013, 4, 21), MotifFor.INDETERMINE, MockCommune.Aigle);
		addBouclement(e, date(2013, 1, 1), DayMonth.get(12, 31), 12);       // bouclements tous les 31.12 depuis le 31.12.2013

		final List<Assujettissement> assujettissements = determine(e);
		assertEmpty(assujettissements);
	}

}
