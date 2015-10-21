package ch.vd.uniregctb.metier.assujettissement;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.ForsParType;
import ch.vd.uniregctb.type.DayMonth;
import ch.vd.uniregctb.type.FormeJuridiqueEntreprise;
import ch.vd.uniregctb.type.GenreImpot;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;

@SuppressWarnings({"JavaDoc", "deprecation"})
public class AssujettissementPersonnesMoralesCalculatorTest extends MetierTest {
	
	private AssujettissementPersonnesMoralesCalculator calculator;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		calculator = new AssujettissementPersonnesMoralesCalculator(tiersService);
	}

	@Nullable
	private static List<Assujettissement> determine(AssujettissementCalculator<? super Entreprise> calculator,
	                                                Entreprise entreprise,
	                                                @Nullable Set<Integer> noOfsCommunes) throws AssujettissementException {
		if (entreprise.isAnnule()) {
			return null;
		}

		final ForsParType fpt = entreprise.getForsParType(true);
		if (fpt == null || fpt.isEmpty()) {
			return null;
		}

		return calculator.determine(entreprise, fpt, noOfsCommunes);
	}

	@Nullable
	private List<Assujettissement> determinePourCommunes(Entreprise entreprise, int... noOfsCommunes) throws AssujettissementException {
		final Set<Integer> set = new HashSet<>(noOfsCommunes.length);
		for (int noOfs : noOfsCommunes) {
			set.add(noOfs);
		}
		return determine(calculator, entreprise, set);
	}

	@Nullable
	private List<Assujettissement> determine(Entreprise entreprise) throws AssujettissementException {
		return determine(calculator, entreprise, null);
	}

	@Nullable
	private List<Assujettissement> determine(Entreprise entreprise, int annee) throws AssujettissementException {
		return determine(AssujettissementHelper.yearLimiting(calculator, annee), entreprise, null);
	}
	
	@Nullable
	private List<Assujettissement> determine(Entreprise entreprise, DateRange range, boolean collate) throws AssujettissementException {
		return determine(AssujettissementHelper.rangeLimiting(calculator, range, collate), entreprise, null);
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testSansForNiBouclement() throws Exception {
		final Entreprise e = addEntrepriseInconnueAuCivil();
		Assert.assertNull(determine(e));        // aucun assujettissement si pas de for
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testSansForAvecBouclement() throws Exception {
		final Entreprise e = addEntrepriseInconnueAuCivil();
		addBouclement(e, date(2013, 1, 1), DayMonth.get(6, 30), 12);        // bouclements tous les 30.06 depuis 30.06.2013
		Assert.assertNull(determine(e));        // aucun assujettissement si pas de for
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testVaudoisExercicesEtForsCalesSurAnneeCivile() throws Exception {
		final Entreprise e = addEntrepriseInconnueAuCivil();
		addForPrincipal(e, date(2013, 1, 1), MotifFor.INDETERMINE, MockCommune.Aigle);
		addBouclement(e, date(2013, 1, 1), DayMonth.get(12, 31), 12);       // bouclements tous les 31.12 depuis le 31.12.2013

		final List<Assujettissement> assujettissements = determine(e);
		Assert.assertNotNull(assujettissements);
		Assert.assertEquals(1, assujettissements.size());
		assertOrdinaire(date(2013, 1, 1), null, MotifFor.INDETERMINE, null, assujettissements.get(0));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testVaudoisExercicesCalesSurAnneeCivileMaisPasPremierFor() throws Exception {
		final Entreprise e = addEntrepriseInconnueAuCivil();
		addForPrincipal(e, date(2013, 4, 21), MotifFor.INDETERMINE, MockCommune.Aigle);
		addBouclement(e, date(2013, 1, 1), DayMonth.get(12, 31), 12);       // bouclements tous les 31.12 depuis le 31.12.2013

		final List<Assujettissement> assujettissements = determine(e);
		Assert.assertNotNull(assujettissements);
		Assert.assertEquals(1, assujettissements.size());
		assertOrdinaire(date(2013, 4, 21), null, MotifFor.INDETERMINE, null, assujettissements.get(0));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testVaudoisExercicesNonCalesSurAnneeCivile() throws Exception {
		final Entreprise e = addEntrepriseInconnueAuCivil();
		addForPrincipal(e, date(2013, 1, 15), MotifFor.INDETERMINE, MockCommune.Aigle);
		addBouclement(e, date(2013, 1, 1), DayMonth.get(6, 30), 12);    // bouclements tous les 30.06 depuis le 31.03.2013

		final List<Assujettissement> assujettissements = determine(e);
		Assert.assertNotNull(assujettissements);
		Assert.assertEquals(1, assujettissements.size());
		assertOrdinaire(date(2013, 1, 15), null, MotifFor.INDETERMINE, null, assujettissements.get(0));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testVaudoisModificationDureeExcercicesCommerciauxDebutAnneeCivile() throws Exception {
		final Entreprise e = addEntrepriseInconnueAuCivil();
		addForPrincipal(e, date(2013, 1, 15), MotifFor.INDETERMINE, MockCommune.Aigle);
		addBouclement(e, date(2013, 1, 1), DayMonth.get(12, 31), 12);    // bouclements tous les 31.12 depuis le 31.12.2013
		addBouclement(e, date(2014, 3, 31), DayMonth.get(3, 31), 12);    // bouclements tous les 31.03 depuis le 31.03.2014
		addBouclement(e, date(2014, 4, 1), DayMonth.get(12, 31), 12);    // bouclements tous les 31.12 depuis le 31.12.2014

		final List<Assujettissement> assujettissements = determine(e);
		Assert.assertNotNull(assujettissements);
		Assert.assertEquals(1, assujettissements.size());
		assertOrdinaire(date(2013, 1, 15), null, MotifFor.INDETERMINE, null, assujettissements.get(0));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testVaudoisModificationDureeExcercicesCommerciauxEnCoursAnneeCivile() throws Exception {
		final Entreprise e = addEntrepriseInconnueAuCivil();
		addForPrincipal(e, date(2013, 1, 15), MotifFor.INDETERMINE, MockCommune.Aigle);
		addBouclement(e, date(2013, 3, 31), DayMonth.get(3, 31), 12);   // bouclements tous les 31.03 depuis le 31.03.2013
		addBouclement(e, date(2014, 4, 1), DayMonth.get(6, 30), 12);    // bouclements tous les 30.06 depuis le 30.06.2014
		addBouclement(e, date(2014, 7, 1), DayMonth.get(3, 31), 12);    // bouclements tous les 31.03 depuis le 31.03.2015

		final List<Assujettissement> assujettissements = determine(e);
		Assert.assertNotNull(assujettissements);
		Assert.assertEquals(1, assujettissements.size());
		assertOrdinaire(date(2013, 1, 15), null, MotifFor.INDETERMINE, null, assujettissements.get(0));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testVaudoisArriveeHorsSuisseBouclementApresDansAnnee() throws Exception {
		final Entreprise e = addEntrepriseInconnueAuCivil();
		addForPrincipal(e, date(2014, 4, 21), MotifFor.ARRIVEE_HS, MockCommune.Echallens);
		addBouclement(e, date(2013, 12, 31), DayMonth.get(12, 31), 12);       // bouclements tous les 31.12 depuis le 31.12.2013 (= un exercice commercial est partiellement hors zone de fors)

		final List<Assujettissement> assujettissements = determine(e);
		Assert.assertNotNull(assujettissements);
		Assert.assertEquals(1, assujettissements.size());
		assertOrdinaire(date(2014, 4, 21), null, MotifFor.ARRIVEE_HS, null, assujettissements.get(0));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testVaudoisArriveeHorsSuisseBouclementSansBouclementApresArriveeDansAnnee() throws Exception {
		final Entreprise e = addEntrepriseInconnueAuCivil();
		addForPrincipal(e, date(2014, 4, 21), MotifFor.ARRIVEE_HS, MockCommune.Echallens);
		addBouclement(e, date(2014, 3, 31), DayMonth.get(3, 31), 12);       // bouclements tous les 31.03 depuis le 31.03.2014 (= un exercice commercial est partiellement hors zone de fors)

		final List<Assujettissement> assujettissements = determine(e);
		Assert.assertNotNull(assujettissements);
		Assert.assertEquals(1, assujettissements.size());
		assertOrdinaire(date(2014, 4, 21), null, MotifFor.ARRIVEE_HS, null, assujettissements.get(0));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testVaudoisDepartHorsSuisseSansBouclementSynchrone() throws Exception {

		final RegDate dateCreation = date(2013, 4, 21);
		final RegDate dateDepart = date(2014, 5, 12);

		final Entreprise e = addEntrepriseInconnueAuCivil();
		addForPrincipal(e, dateCreation, MotifFor.INDETERMINE, dateDepart, MotifFor.DEPART_HS, MockCommune.Echallens);
		addForPrincipal(e, dateDepart.getOneDayAfter(), MotifFor.DEPART_HS, MockPays.Albanie);
		addBouclement(e, date(2013, 3, 31), DayMonth.get(3, 31), 12);       // bouclements tous les 31.03 depuis le 31.03.2013 (= un exercice commercial est partiellement hors zone de fors)

		final List<Assujettissement> assujettissements = determine(e);
		Assert.assertNotNull(assujettissements);
		Assert.assertEquals(1, assujettissements.size());
		assertOrdinaire(dateCreation, dateDepart, MotifFor.INDETERMINE, MotifFor.DEPART_HS, assujettissements.get(0));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testVaudoisArriveeHorsCantonSansBouclementApresArriveeDansAnnee() throws Exception {
		final Entreprise e = addEntrepriseInconnueAuCivil();
		addForPrincipal(e, date(2014, 4, 21), MotifFor.ARRIVEE_HC, MockCommune.Echallens);
		addBouclement(e, date(2014, 3, 31), DayMonth.get(3, 31), 12);       // bouclements tous les 31.03 depuis le 31.03.2014 (= un exercice commercial est partiellement hors zone de fors)

		final List<Assujettissement> assujettissements = determine(e);
		Assert.assertNotNull(assujettissements);
		Assert.assertEquals(1, assujettissements.size());
		assertOrdinaire(date(2014, 4, 1), null, MotifFor.ARRIVEE_HC, null, assujettissements.get(0));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testVaudoisArriveeHorsCantonLendemainBouclement() throws Exception {
		final Entreprise e = addEntrepriseInconnueAuCivil();
		addForPrincipal(e, date(2014, 4, 1), MotifFor.ARRIVEE_HC, MockCommune.Echallens);
		addBouclement(e, date(2014, 3, 31), DayMonth.get(3, 31), 12);       // bouclements tous les 31.03 depuis le 31.03.2014

		final List<Assujettissement> assujettissements = determine(e);
		Assert.assertNotNull(assujettissements);
		Assert.assertEquals(1, assujettissements.size());
		assertOrdinaire(date(2014, 4, 1), null, MotifFor.ARRIVEE_HC, null, assujettissements.get(0));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testHorsCantonImmeubleAchatSynchroneAvecExercice() throws Exception {

		final RegDate dateAchat = date(2013, 4, 1);

		final Entreprise e = addEntrepriseInconnueAuCivil();
		addForPrincipal(e, dateAchat, null, MockCommune.Bale);
		addForSecondaire(e, dateAchat, MotifFor.ACHAT_IMMOBILIER, MockCommune.Cossonay.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
		addBouclement(e, date(2013, 3, 31), DayMonth.get(3, 31), 12);       // bouclements tous les 31.03 depuis le 31.03.2013

		final List<Assujettissement> assujettissements = determine(e);
		Assert.assertNotNull(assujettissements);
		Assert.assertEquals(1, assujettissements.size());
		assertHorsCanton(date(2013, 4, 1), null, MotifFor.ACHAT_IMMOBILIER, null, assujettissements.get(0));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testHorsCantonImmeubleAchatNonSynchroneAvecExercice() throws Exception {

		final RegDate dateAchat = date(2013, 4, 1);

		final Entreprise e = addEntrepriseInconnueAuCivil();
		addForPrincipal(e, dateAchat, null, MockCommune.Bale);
		addForSecondaire(e, dateAchat, MotifFor.ACHAT_IMMOBILIER, MockCommune.Cossonay.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
		addBouclement(e, date(2012, 12, 31), DayMonth.get(12, 31), 12);       // bouclements tous les 31.12 depuis le 31.12.2012

		final List<Assujettissement> assujettissements = determine(e);
		Assert.assertNotNull(assujettissements);
		Assert.assertEquals(1, assujettissements.size());
		assertHorsCanton(date(2013, 1, 1), null, MotifFor.ACHAT_IMMOBILIER, null, assujettissements.get(0));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testHorsCantonActiviteSynchroneAvecExercice() throws Exception {

		final RegDate dateDebutExploitation = date(2013, 4, 1);

		final Entreprise e = addEntrepriseInconnueAuCivil();
		addForPrincipal(e, dateDebutExploitation, null, MockCommune.Bale);
		addForSecondaire(e, dateDebutExploitation, MotifFor.DEBUT_EXPLOITATION, MockCommune.Cossonay.getNoOFS(), MotifRattachement.ACTIVITE_INDEPENDANTE);
		addBouclement(e, date(2013, 3, 31), DayMonth.get(3, 31), 12);       // bouclements tous les 31.03 depuis le 31.03.2013

		final List<Assujettissement> assujettissements = determine(e);
		Assert.assertNotNull(assujettissements);
		Assert.assertEquals(1, assujettissements.size());
		assertHorsCanton(date(2013, 4, 1), null, MotifFor.DEBUT_EXPLOITATION, null, assujettissements.get(0));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testHorsCantonActiviteNonSynchroneAvecExercice() throws Exception {

		final RegDate dateDebutExploitation = date(2013, 4, 1);

		final Entreprise e = addEntrepriseInconnueAuCivil();
		addForPrincipal(e, dateDebutExploitation, null, MockCommune.Bale);
		addForSecondaire(e, dateDebutExploitation, MotifFor.DEBUT_EXPLOITATION, MockCommune.Cossonay.getNoOFS(), MotifRattachement.ACTIVITE_INDEPENDANTE);
		addBouclement(e, date(2012, 12, 31), DayMonth.get(12, 31), 12);       // bouclements tous les 31.12 depuis le 31.12.2012

		final List<Assujettissement> assujettissements = determine(e);
		Assert.assertNotNull(assujettissements);
		Assert.assertEquals(1, assujettissements.size());
		assertHorsCanton(date(2013, 1, 1), null, MotifFor.DEBUT_EXPLOITATION, null, assujettissements.get(0));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testHorsSuisseImmeubleAchatSynchroneAvecExercice() throws Exception {

		final RegDate dateAchat = date(2013, 4, 1);

		final Entreprise e = addEntrepriseInconnueAuCivil();
		addForPrincipal(e, dateAchat, null, MockPays.Allemagne);
		addForSecondaire(e, dateAchat, MotifFor.ACHAT_IMMOBILIER, MockCommune.Cossonay.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
		addBouclement(e, date(2013, 3, 31), DayMonth.get(3, 31), 12);       // bouclements tous les 31.03 depuis le 31.03.2013

		final List<Assujettissement> assujettissements = determine(e);
		Assert.assertNotNull(assujettissements);
		Assert.assertEquals(1, assujettissements.size());
		assertHorsSuisse(dateAchat, null, MotifFor.ACHAT_IMMOBILIER, null, assujettissements.get(0));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testHorsSuisseImmeubleAchatNonSynchroneAvecExercice() throws Exception {

		final RegDate dateAchat = date(2013, 4, 1);

		final Entreprise e = addEntrepriseInconnueAuCivil();
		addForPrincipal(e, dateAchat, null, MockPays.Allemagne);
		addForSecondaire(e, dateAchat, MotifFor.ACHAT_IMMOBILIER, MockCommune.Cossonay.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
		addBouclement(e, date(2012, 12, 31), DayMonth.get(12, 31), 12);       // bouclements tous les 31.12 depuis le 31.12.2012

		final List<Assujettissement> assujettissements = determine(e);
		Assert.assertNotNull(assujettissements);
		Assert.assertEquals(1, assujettissements.size());
		assertHorsSuisse(dateAchat, null, MotifFor.ACHAT_IMMOBILIER, null, assujettissements.get(0));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testHorsSuisseActiviteSynchroneAvecExercice() throws Exception {

		final RegDate dateDebutExploitation = date(2013, 4, 1);

		final Entreprise e = addEntrepriseInconnueAuCivil();
		addForPrincipal(e, dateDebutExploitation, null, MockPays.France);
		addForSecondaire(e, dateDebutExploitation, MotifFor.DEBUT_EXPLOITATION, MockCommune.Cossonay.getNoOFS(), MotifRattachement.ACTIVITE_INDEPENDANTE);
		addBouclement(e, date(2013, 3, 31), DayMonth.get(3, 31), 12);       // bouclements tous les 31.03 depuis le 31.03.2013

		final List<Assujettissement> assujettissements = determine(e);
		Assert.assertNotNull(assujettissements);
		Assert.assertEquals(1, assujettissements.size());
		assertHorsSuisse(dateDebutExploitation, null, MotifFor.DEBUT_EXPLOITATION, null, assujettissements.get(0));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testHorsSuisseActiviteNonSynchroneAvecExercice() throws Exception {

		final RegDate dateDebutExploitation = date(2013, 4, 1);

		final Entreprise e = addEntrepriseInconnueAuCivil();
		addForPrincipal(e, dateDebutExploitation, null, MockPays.RoyaumeUni);
		addForSecondaire(e, dateDebutExploitation, MotifFor.DEBUT_EXPLOITATION, MockCommune.Cossonay.getNoOFS(), MotifRattachement.ACTIVITE_INDEPENDANTE);
		addBouclement(e, date(2012, 12, 31), DayMonth.get(12, 31), 12);       // bouclements tous les 31.12 depuis le 31.12.2012

		final List<Assujettissement> assujettissements = determine(e);
		Assert.assertNotNull(assujettissements);
		Assert.assertEquals(1, assujettissements.size());
		assertHorsSuisse(dateDebutExploitation, null, MotifFor.DEBUT_EXPLOITATION, null, assujettissements.get(0));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testHorsCantonImmeubleTransfertSiegeVersVaud() throws Exception {

		final RegDate dateAchat = date(2013, 4, 15);
		final RegDate dateArriveeSiege = date(2014, 5, 21);

		final Entreprise e = addEntrepriseInconnueAuCivil();
		addForPrincipal(e, dateAchat, null, dateArriveeSiege.getOneDayBefore(), MotifFor.ARRIVEE_HC, MockCommune.Bale);
		addForPrincipal(e, dateArriveeSiege, MotifFor.ARRIVEE_HC, MockCommune.Morges);
		addForSecondaire(e, dateAchat, MotifFor.ACHAT_IMMOBILIER, MockCommune.Cossonay.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
		addBouclement(e, date(2012, 3, 31), DayMonth.get(3, 31), 12);       // bouclements tous les 31.03 depuis le 31.03.2012

		final List<Assujettissement> assujettissements = determine(e);
		Assert.assertNotNull(assujettissements);
		Assert.assertEquals(2, assujettissements.size());
		assertHorsCanton(date(2013, 4, 1), date(2014, 3, 31), MotifFor.ACHAT_IMMOBILIER, MotifFor.ARRIVEE_HC, assujettissements.get(0));
		assertOrdinaire(date(2014, 4, 1), null, MotifFor.ARRIVEE_HC, null, assujettissements.get(1));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testHorsCantonActiviteTransfertSiegeVersVaud() throws Exception {

		final RegDate dateDebutExploitation = date(2013, 4, 15);
		final RegDate dateArriveeSiege = date(2014, 5, 21);

		final Entreprise e = addEntrepriseInconnueAuCivil();
		addForPrincipal(e, dateDebutExploitation, null, dateArriveeSiege.getOneDayBefore(), MotifFor.ARRIVEE_HC, MockCommune.Bale);
		addForPrincipal(e, dateArriveeSiege, MotifFor.ARRIVEE_HC, MockCommune.Morges);
		addForSecondaire(e, dateDebutExploitation, MotifFor.DEBUT_EXPLOITATION, MockCommune.Cossonay.getNoOFS(), MotifRattachement.ACTIVITE_INDEPENDANTE);
		addBouclement(e, date(2012, 3, 31), DayMonth.get(3, 31), 12);       // bouclements tous les 31.03 depuis le 31.03.2012

		final List<Assujettissement> assujettissements = determine(e);
		Assert.assertNotNull(assujettissements);
		Assert.assertEquals(2, assujettissements.size());
		assertHorsCanton(date(2013, 4, 1), date(2014, 3, 31), MotifFor.DEBUT_EXPLOITATION, MotifFor.ARRIVEE_HC, assujettissements.get(0));
		assertOrdinaire(date(2014, 4, 1), null, MotifFor.ARRIVEE_HC, null, assujettissements.get(1));
	}


	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testHorsSuisseImmeubleTransfertSiegeVersVaud() throws Exception {

		final RegDate dateAchat = date(2013, 4, 15);
		final RegDate dateArriveeSiege = date(2014, 5, 21);

		final Entreprise e = addEntrepriseInconnueAuCivil();
		addForPrincipal(e, dateAchat, null, dateArriveeSiege.getOneDayBefore(), MotifFor.ARRIVEE_HS, MockPays.Allemagne);
		addForPrincipal(e, dateArriveeSiege, MotifFor.ARRIVEE_HS, MockCommune.Morges);
		addForSecondaire(e, dateAchat, MotifFor.ACHAT_IMMOBILIER, MockCommune.Cossonay.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
		addBouclement(e, date(2012, 3, 31), DayMonth.get(3, 31), 12);       // bouclements tous les 31.03 depuis le 31.03.2012

		final List<Assujettissement> assujettissements = determine(e);
		Assert.assertNotNull(assujettissements);
		Assert.assertEquals(2, assujettissements.size());
		assertHorsSuisse(dateAchat, dateArriveeSiege.getOneDayBefore(), MotifFor.ACHAT_IMMOBILIER, MotifFor.ARRIVEE_HS, assujettissements.get(0));
		assertOrdinaire(dateArriveeSiege, null, MotifFor.ARRIVEE_HS, null, assujettissements.get(1));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testHorsSuisseActiviteTransfertSiegeVersVaud() throws Exception {

		final RegDate dateDebutExploitation = date(2013, 4, 15);
		final RegDate dateArriveeSiege = date(2014, 5, 21);

		final Entreprise e = addEntrepriseInconnueAuCivil();
		addForPrincipal(e, dateDebutExploitation, null, dateArriveeSiege.getOneDayBefore(), MotifFor.ARRIVEE_HS, MockPays.Allemagne);
		addForPrincipal(e, dateArriveeSiege, MotifFor.ARRIVEE_HS, MockCommune.Morges);
		addForSecondaire(e, dateDebutExploitation, MotifFor.DEBUT_EXPLOITATION, MockCommune.Cossonay.getNoOFS(), MotifRattachement.ACTIVITE_INDEPENDANTE);
		addBouclement(e, date(2012, 3, 31), DayMonth.get(3, 31), 12);       // bouclements tous les 31.03 depuis le 31.03.2012

		final List<Assujettissement> assujettissements = determine(e);
		Assert.assertNotNull(assujettissements);
		Assert.assertEquals(2, assujettissements.size());
		assertHorsSuisse(dateDebutExploitation, dateArriveeSiege.getOneDayBefore(), MotifFor.DEBUT_EXPLOITATION, MotifFor.ARRIVEE_HS, assujettissements.get(0));
		assertOrdinaire(dateArriveeSiege, null, MotifFor.ARRIVEE_HS, null, assujettissements.get(1));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testVaudoisImmeubleTransfertSiegeVersHorsCanton() throws Exception {

		final RegDate dateCreationEntreprise = date(2013, 2, 1);
		final RegDate dateAchat = date(2013, 4, 15);
		final RegDate dateDepartSiege = date(2014, 5, 21);

		final Entreprise e = addEntrepriseInconnueAuCivil();
		addForPrincipal(e, dateCreationEntreprise, MotifFor.INDETERMINE, dateDepartSiege, MotifFor.DEPART_HC, MockCommune.Echallens);
		addForPrincipal(e, dateDepartSiege.getOneDayAfter(), MotifFor.DEPART_HC, MockCommune.Geneve);
		addForSecondaire(e, dateAchat, MotifFor.ACHAT_IMMOBILIER, MockCommune.Cossonay.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
		addBouclement(e, date(2012, 3, 31), DayMonth.get(3, 31), 12);       // bouclements tous les 31.03 depuis le 31.03.2012
		addBouclement(e, date(2014, 4, 1), DayMonth.get(9, 30), 12);       // bouclements tous les 30.09 depuis le 30.09.2014

		final List<Assujettissement> assujettissements = determine(e);
		Assert.assertNotNull(assujettissements);
		Assert.assertEquals(2, assujettissements.size());
		assertOrdinaire(dateCreationEntreprise, date(2014, 9, 30), MotifFor.INDETERMINE, MotifFor.DEPART_HC, assujettissements.get(0));
		assertHorsCanton(date(2014, 10, 1), null, MotifFor.DEPART_HC, null, assujettissements.get(1));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testVaudoisActiviteTransfertSiegeVersHorsCanton() throws Exception {

		final RegDate dateCreationEntreprise = date(2013, 2, 1);
		final RegDate dateDebutActivite = date(2013, 4, 15);
		final RegDate dateDepartSiege = date(2014, 5, 21);

		final Entreprise e = addEntrepriseInconnueAuCivil();
		addForPrincipal(e, dateCreationEntreprise, MotifFor.INDETERMINE, dateDepartSiege, MotifFor.DEPART_HC, MockCommune.Echallens);
		addForPrincipal(e, dateDepartSiege.getOneDayAfter(), MotifFor.DEPART_HC, MockCommune.Geneve);
		addForSecondaire(e, dateDebutActivite, MotifFor.DEBUT_EXPLOITATION, MockCommune.Cossonay.getNoOFS(), MotifRattachement.ACTIVITE_INDEPENDANTE);
		addBouclement(e, date(2012, 3, 31), DayMonth.get(3, 31), 12);       // bouclements tous les 31.03 depuis le 31.03.2012
		addBouclement(e, date(2014, 4, 1), DayMonth.get(9, 30), 12);       // bouclements tous les 30.09 depuis le 30.09.2014

		final List<Assujettissement> assujettissements = determine(e);
		Assert.assertNotNull(assujettissements);
		Assert.assertEquals(2, assujettissements.size());
		assertOrdinaire(dateCreationEntreprise, date(2014, 9, 30), MotifFor.INDETERMINE, MotifFor.DEPART_HC, assujettissements.get(0));
		assertHorsCanton(date(2014, 10, 1), null, MotifFor.DEPART_HC, null, assujettissements.get(1));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testVaudoisImmeubleTransfertSiegeVersHorsSuisse() throws Exception {

		final RegDate dateCreationEntreprise = date(2013, 2, 1);
		final RegDate dateAchat = date(2013, 4, 15);
		final RegDate dateDepartSiege = date(2014, 5, 21);

		final Entreprise e = addEntrepriseInconnueAuCivil();
		addForPrincipal(e, dateCreationEntreprise, MotifFor.INDETERMINE, dateDepartSiege, MotifFor.DEPART_HS, MockCommune.Echallens);
		addForPrincipal(e, dateDepartSiege.getOneDayAfter(), MotifFor.DEPART_HS, MockPays.Allemagne);
		addForSecondaire(e, dateAchat, MotifFor.ACHAT_IMMOBILIER, MockCommune.Cossonay.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
		addBouclement(e, date(2012, 3, 31), DayMonth.get(3, 31), 12);       // bouclements tous les 31.03 depuis le 31.03.2012
		addBouclement(e, date(2014, 4, 1), DayMonth.get(9, 30), 12);       // bouclements tous les 30.09 depuis le 30.09.2014

		final List<Assujettissement> assujettissements = determine(e);
		Assert.assertNotNull(assujettissements);
		Assert.assertEquals(2, assujettissements.size());
		assertOrdinaire(dateCreationEntreprise, dateDepartSiege, MotifFor.INDETERMINE, MotifFor.DEPART_HS, assujettissements.get(0));
		assertHorsSuisse(dateDepartSiege.getOneDayAfter(), null, MotifFor.DEPART_HS, null, assujettissements.get(1));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testVaudoisActiviteTransfertSiegeVersHorsSuisse() throws Exception {

		final RegDate dateCreationEntreprise = date(2013, 2, 1);
		final RegDate dateAchat = date(2013, 4, 15);
		final RegDate dateDepartSiege = date(2014, 5, 21);

		final Entreprise e = addEntrepriseInconnueAuCivil();
		addForPrincipal(e, dateCreationEntreprise, MotifFor.INDETERMINE, dateDepartSiege, MotifFor.DEPART_HS, MockCommune.Echallens);
		addForPrincipal(e, dateDepartSiege.getOneDayAfter(), MotifFor.DEPART_HS, MockPays.Allemagne);
		addForSecondaire(e, dateAchat, MotifFor.DEBUT_EXPLOITATION, MockCommune.Cossonay.getNoOFS(), MotifRattachement.ACTIVITE_INDEPENDANTE);
		addBouclement(e, date(2012, 3, 31), DayMonth.get(3, 31), 12);       // bouclements tous les 31.03 depuis le 31.03.2012
		addBouclement(e, date(2014, 4, 1), DayMonth.get(9, 30), 12);       // bouclements tous les 30.09 depuis le 30.09.2014

		final List<Assujettissement> assujettissements = determine(e);
		Assert.assertNotNull(assujettissements);
		Assert.assertEquals(2, assujettissements.size());
		assertOrdinaire(dateCreationEntreprise, dateDepartSiege, MotifFor.INDETERMINE, MotifFor.DEPART_HS, assujettissements.get(0));
		assertHorsSuisse(dateDepartSiege.getOneDayAfter(), null, MotifFor.DEPART_HS, null, assujettissements.get(1));
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
		addForPrincipal(e, dateCreationEntreprise, MotifFor.INDETERMINE, dateDepartSiege, MotifFor.DEPART_HC, MockCommune.CheseauxSurLausanne);
		addForPrincipal(e, dateDepartSiege.getOneDayAfter(), MotifFor.DEPART_HC, dateRetourSiege.getOneDayBefore(), MotifFor.ARRIVEE_HC, MockCommune.Geneve);
		addForPrincipal(e, dateRetourSiege, MotifFor.ARRIVEE_HC, MockCommune.Lausanne);
		addBouclement(e, date(2014, 1, 1), DayMonth.get(3, 31), 12);        // bouclements tous les 31.03 depuis le 31.03.2014
		addBouclement(e, date(2014, 4, 1), DayMonth.get(6, 30), 3);         // bouclements tous les 3 mois depuis le 30.06.2014
		addBouclement(e, date(2014, 10, 1), DayMonth.get(9, 30), 12);       // bouclements tous les 30.09 depuis le 30.09.2015

		final List<Assujettissement> assujettissements = determine(e);
		Assert.assertNotNull(assujettissements);
		Assert.assertEquals(2, assujettissements.size());
		assertOrdinaire(dateCreationEntreprise, date(2014, 6, 30), MotifFor.INDETERMINE, MotifFor.DEPART_HC, assujettissements.get(0));
		assertOrdinaire(date(2014, 10, 1), null, MotifFor.ARRIVEE_HC, null, assujettissements.get(1));
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
		addForPrincipal(e, dateCreationEntreprise, MotifFor.INDETERMINE, dateDepartSiege, MotifFor.DEPART_HC, MockCommune.CheseauxSurLausanne);
		addForPrincipal(e, dateDepartSiege.getOneDayAfter(), MotifFor.DEPART_HC, dateRetourSiege.getOneDayBefore(), MotifFor.ARRIVEE_HC, MockCommune.Geneve);
		addForPrincipal(e, dateRetourSiege, MotifFor.ARRIVEE_HC, MockCommune.Lausanne);
		addBouclement(e, date(2014, 1, 1), DayMonth.get(3, 31), 12);        // bouclements tous les 31.03 depuis le 31.03.2014
		addBouclement(e, date(2014, 4, 1), DayMonth.get(6, 30), 3);         // bouclements tous les 3 mois depuis le 30.06.2014
		addBouclement(e, date(2014, 10, 1), DayMonth.get(9, 30), 12);       // bouclements tous les 30.09 depuis le 30.09.2015

		final List<Assujettissement> assujettissements = determine(e);
		Assert.assertNotNull(assujettissements);
		Assert.assertEquals(1, assujettissements.size());
		assertOrdinaire(dateCreationEntreprise, null, MotifFor.INDETERMINE, null, assujettissements.get(0));
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
		addForPrincipal(e, dateCreationEntreprise, MotifFor.INDETERMINE, dateDepartSiege, MotifFor.DEPART_HC, MockCommune.CheseauxSurLausanne);
		addForPrincipal(e, dateDepartSiege.getOneDayAfter(), MotifFor.DEPART_HC, dateRetourSiege.getOneDayBefore(), MotifFor.ARRIVEE_HC, MockCommune.Geneve);
		addForPrincipal(e, dateRetourSiege, MotifFor.ARRIVEE_HC, MockCommune.Lausanne);
		addBouclement(e, date(2014, 1, 1), DayMonth.get(3, 31), 12);        // bouclements tous les 31.03 depuis le 31.03.2014
		addBouclement(e, date(2014, 4, 1), DayMonth.get(9, 30), 12);         // bouclements tous les 30.09 mois depuis le 30.09.2014

		final List<Assujettissement> assujettissements = determine(e);
		Assert.assertNotNull(assujettissements);
		Assert.assertEquals(1, assujettissements.size());
		assertOrdinaire(dateCreationEntreprise, null, MotifFor.INDETERMINE, null, assujettissements.get(0));
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
		addForPrincipal(e, dateCreationEntreprise, MotifFor.INDETERMINE, dateDepartSiege, MotifFor.DEPART_HC, MockCommune.CheseauxSurLausanne);
		addForPrincipal(e, dateDepartSiege.getOneDayAfter(), MotifFor.DEPART_HC, dateRetourSiege.getOneDayBefore(), MotifFor.ARRIVEE_HC, MockCommune.Geneve);
		addForPrincipal(e, dateRetourSiege, MotifFor.ARRIVEE_HC, MockCommune.Lausanne);
		addForSecondaire(e, dateDebutForSecondaire, MotifFor.ACHAT_IMMOBILIER, dateFinForSecondaire, MotifFor.VENTE_IMMOBILIER, MockCommune.Cossonay.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
		addBouclement(e, date(2014, 1, 1), DayMonth.get(3, 31), 12);        // bouclements tous les 31.03 depuis le 31.03.2014
		addBouclement(e, date(2014, 4, 1), DayMonth.get(6, 30), 3);         // bouclements tous les 3 mois depuis le 30.06.2014
		addBouclement(e, date(2014, 10, 1), DayMonth.get(9, 30), 12);       // bouclements tous les 30.09 depuis le 30.09.2015

		final List<Assujettissement> assujettissements = determine(e);
		Assert.assertNotNull(assujettissements);
		Assert.assertEquals(3, assujettissements.size());
		assertOrdinaire(dateCreationEntreprise, date(2014, 6, 30), MotifFor.INDETERMINE, MotifFor.DEPART_HC, assujettissements.get(0));
		assertHorsCanton(date(2014, 7, 1), date(2014, 9, 30), MotifFor.DEPART_HC, MotifFor.ARRIVEE_HC, assujettissements.get(1));
		assertOrdinaire(date(2014, 10, 1), null, MotifFor.ARRIVEE_HC, null, assujettissements.get(2));
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
		addForPrincipal(e, dateCreationEntreprise, MotifFor.INDETERMINE, dateDepartSiege, MotifFor.DEPART_HS, MockCommune.CheseauxSurLausanne);
		addForPrincipal(e, dateDepartSiege.getOneDayAfter(), MotifFor.DEPART_HS, dateRetourSiege.getOneDayBefore(), MotifFor.ARRIVEE_HS, MockPays.Allemagne);
		addForPrincipal(e, dateRetourSiege, MotifFor.ARRIVEE_HS, MockCommune.Lausanne);
		addBouclement(e, date(2014, 1, 1), DayMonth.get(3, 31), 12);        // bouclements tous les 31.03 depuis le 31.03.2014
		addBouclement(e, date(2014, 4, 1), DayMonth.get(6, 30), 3);         // bouclements tous les 3 mois depuis le 30.06.2014
		addBouclement(e, date(2014, 10, 1), DayMonth.get(9, 30), 12);       // bouclements tous les 30.09 depuis le 30.09.2015

		final List<Assujettissement> assujettissements = determine(e);
		Assert.assertNotNull(assujettissements);
		Assert.assertEquals(2, assujettissements.size());
		assertOrdinaire(dateCreationEntreprise, dateDepartSiege, MotifFor.INDETERMINE, MotifFor.DEPART_HS, assujettissements.get(0));
		assertOrdinaire(dateRetourSiege, null, MotifFor.ARRIVEE_HS, null, assujettissements.get(1));
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
		addForPrincipal(e, dateCreationEntreprise, MotifFor.INDETERMINE, dateDepartSiege, MotifFor.DEPART_HS, MockCommune.CheseauxSurLausanne);
		addForPrincipal(e, dateDepartSiege.getOneDayAfter(), MotifFor.DEPART_HS, dateRetourSiege.getOneDayBefore(), MotifFor.ARRIVEE_HS, MockPays.Allemagne);
		addForPrincipal(e, dateRetourSiege, MotifFor.ARRIVEE_HS, MockCommune.Lausanne);
		addBouclement(e, date(2014, 1, 1), DayMonth.get(3, 31), 12);        // bouclements tous les 31.03 depuis le 31.03.2014
		addBouclement(e, date(2014, 4, 1), DayMonth.get(6, 30), 3);         // bouclements tous les 3 mois depuis le 30.06.2014
		addBouclement(e, date(2014, 10, 1), DayMonth.get(9, 30), 12);       // bouclements tous les 30.09 depuis le 30.09.2015

		final List<Assujettissement> assujettissements = determine(e);
		Assert.assertNotNull(assujettissements);
		Assert.assertEquals(2, assujettissements.size());
		assertOrdinaire(dateCreationEntreprise, dateDepartSiege, MotifFor.INDETERMINE, MotifFor.DEPART_HS, assujettissements.get(0));
		assertOrdinaire(dateRetourSiege, null, MotifFor.ARRIVEE_HS, null, assujettissements.get(1));
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
		addForPrincipal(e, dateCreationEntreprise, MotifFor.INDETERMINE, dateDepartSiege, MotifFor.DEPART_HS, MockCommune.CheseauxSurLausanne);
		addForPrincipal(e, dateDepartSiege.getOneDayAfter(), MotifFor.DEPART_HS, dateRetourSiege.getOneDayBefore(), MotifFor.ARRIVEE_HS, MockPays.Allemagne);
		addForPrincipal(e, dateRetourSiege, MotifFor.ARRIVEE_HS, MockCommune.Lausanne);
		addBouclement(e, date(2014, 1, 1), DayMonth.get(3, 31), 12);        // bouclements tous les 31.03 depuis le 31.03.2014
		addBouclement(e, date(2014, 4, 1), DayMonth.get(9, 30), 12);         // bouclements tous les 30.09 mois depuis le 30.09.2014

		final List<Assujettissement> assujettissements = determine(e);
		Assert.assertNotNull(assujettissements);
		Assert.assertEquals(2, assujettissements.size());
		assertOrdinaire(dateCreationEntreprise, dateDepartSiege, MotifFor.INDETERMINE, MotifFor.DEPART_HS, assujettissements.get(0));
		assertOrdinaire(dateRetourSiege, null, MotifFor.ARRIVEE_HS, null, assujettissements.get(1));
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
		addForPrincipal(e, dateCreationEntreprise, MotifFor.INDETERMINE, dateDepartSiege, MotifFor.DEPART_HS, MockCommune.CheseauxSurLausanne);
		addForPrincipal(e, dateDepartSiege.getOneDayAfter(), MotifFor.DEPART_HS, dateRetourSiege.getOneDayBefore(), MotifFor.ARRIVEE_HS, MockPays.Allemagne);
		addForPrincipal(e, dateRetourSiege, MotifFor.ARRIVEE_HS, MockCommune.Lausanne);
		addForSecondaire(e, dateDebutForSecondaire, MotifFor.ACHAT_IMMOBILIER, dateFinForSecondaire, MotifFor.VENTE_IMMOBILIER, MockCommune.Cossonay.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
		addBouclement(e, date(2014, 1, 1), DayMonth.get(3, 31), 12);        // bouclements tous les 31.03 depuis le 31.03.2014
		addBouclement(e, date(2014, 4, 1), DayMonth.get(6, 30), 3);         // bouclements tous les 3 mois depuis le 30.06.2014
		addBouclement(e, date(2014, 10, 1), DayMonth.get(9, 30), 12);       // bouclements tous les 30.09 depuis le 30.09.2015

		final List<Assujettissement> assujettissements = determine(e);
		Assert.assertNotNull(assujettissements);
		Assert.assertEquals(3, assujettissements.size());
		assertOrdinaire(dateCreationEntreprise, dateDepartSiege, MotifFor.INDETERMINE, MotifFor.DEPART_HS, assujettissements.get(0));
		assertHorsSuisse(dateDepartSiege.getOneDayAfter(), dateFinForSecondaire, MotifFor.DEPART_HS, MotifFor.VENTE_IMMOBILIER, assujettissements.get(1));
		assertOrdinaire(dateRetourSiege, null, MotifFor.ARRIVEE_HS, null, assujettissements.get(2));
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
		addForPrincipal(e, dateCreation, MotifFor.INDETERMINE, dateDemenagement.getOneDayBefore(), MotifFor.DEMENAGEMENT_VD, MockCommune.Lausanne);
		addForPrincipal(e, dateDemenagement, MotifFor.DEMENAGEMENT_VD, MockCommune.Prilly);
		addBouclement(e, date(2012, 12, 1), DayMonth.get(12, 31), 12);      // bouclements tous les 31.12 depuis le 31.12.2012

		final List<Assujettissement> assujettissements = determine(e);
		Assert.assertNotNull(assujettissements);
		Assert.assertEquals(1, assujettissements.size());
		assertOrdinaire(dateCreation, null, MotifFor.INDETERMINE, null, assujettissements.get(0));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testAvecForsPrincipauxNonBeneficeCapital() throws Exception {

		final RegDate dateCreation = date(2011, 7, 12);

		final Entreprise e = addEntrepriseInconnueAuCivil();
		addDonneesRegistreCommerce(e, dateCreation, null, "L'avenir du genre humain", FormeJuridiqueEntreprise.SNC, null);
		addForPrincipal(e, dateCreation, MotifFor.INDETERMINE, MockCommune.Lausanne, GenreImpot.REVENU_FORTUNE);
		addBouclement(e, date(2012, 12, 1), DayMonth.get(12, 31), 12);      // bouclements tous les 31.12 depuis le 31.12.2012

		final List<Assujettissement> assujettissements = determine(e);
		Assert.assertNull(assujettissements);
	}
}
