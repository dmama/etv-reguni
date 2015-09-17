package ch.vd.uniregctb.metier.assujettissement;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.uniregctb.parametrage.ParametreAppService;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.type.DayMonth;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.TypeContribuable;

/**
 * Tests des calculs des périodes d'imposition dans le contexte des personnes morales
 */
public class PeriodeImpositionPersonnesMoralesCalculatorTest extends MetierTest {

	private PeriodeImpositionPersonnesMoralesCalculator calculator;
	private AssujettissementService assujettissementService;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		final ParametreAppService parametreService = getBean(ParametreAppService.class, "parametreAppService");
		this.calculator = new PeriodeImpositionPersonnesMoralesCalculator(parametreService, tiersService);
		this.assujettissementService = getBean(AssujettissementService.class, "assujettissementService");
	}

	@NotNull
	private List<PeriodeImposition> determine(Entreprise ctb) throws AssujettissementException {
		final List<Assujettissement> a = assujettissementService.determine(ctb);
		return calculator.determine(ctb, a);
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testSansForNiBouclement() throws Exception {
		final Entreprise e = addEntrepriseInconnueAuCivil();
		Assert.assertEquals(0, determine(e).size());        // aucune période d'imposition si pas de for
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testSansForAvecBouclement() throws Exception {
		final Entreprise e = addEntrepriseInconnueAuCivil();
		addBouclement(e, date(2013, 1, 1), DayMonth.get(6, 30), 12);        // bouclements tous les 30.06 depuis 30.06.2013
		Assert.assertEquals(0, determine(e).size());        // aucune période d'imposition si pas de for
	}

	/**
	 * On ne calcule pas de période d'imposition avant la PF 2009
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testAssujettissementAvant2009ExercicesCalesSurAnneesCiviles() throws Exception {
		final Entreprise e = addEntrepriseInconnueAuCivil();
		addForPrincipal(e, date(2000, 1, 1), MotifFor.INDETERMINE, date(2015, 7, 31), MotifFor.INDETERMINE, MockCommune.Aigle);
		addBouclement(e, date(2000, 1, 1), DayMonth.get(12, 31), 12);       // bouclements tous les 31.12 depuis le 31.12.2000

		final List<PeriodeImposition> periodes = determine(e);
		Assert.assertNotNull(periodes);
		Assert.assertEquals(7, periodes.size());
		assertPeriodeImpositionPersonnesMorales(date(2009, 1, 1), date(2009, 12, 31), false, TypeContribuable.VAUDOIS_ORDINAIRE, false, false, false, periodes.get(0));
		assertPeriodeImpositionPersonnesMorales(date(2010, 1, 1), date(2010, 12, 31), false, TypeContribuable.VAUDOIS_ORDINAIRE, false, false, false, periodes.get(1));
		assertPeriodeImpositionPersonnesMorales(date(2011, 1, 1), date(2011, 12, 31), false, TypeContribuable.VAUDOIS_ORDINAIRE, false, false, false, periodes.get(2));
		assertPeriodeImpositionPersonnesMorales(date(2012, 1, 1), date(2012, 12, 31), false, TypeContribuable.VAUDOIS_ORDINAIRE, false, false, false, periodes.get(3));
		assertPeriodeImpositionPersonnesMorales(date(2013, 1, 1), date(2013, 12, 31), false, TypeContribuable.VAUDOIS_ORDINAIRE, false, false, false, periodes.get(4));
		assertPeriodeImpositionPersonnesMorales(date(2014, 1, 1), date(2014, 12, 31), false, TypeContribuable.VAUDOIS_ORDINAIRE, false, false, false, periodes.get(5));
		assertPeriodeImpositionPersonnesMorales(date(2015, 1, 1), date(2015, 7, 31), false, TypeContribuable.VAUDOIS_ORDINAIRE, false, false, false, periodes.get(6));
	}

	/**
	 * On ne calcule pas de période d'imposition avant la PF 2009
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testAssujettissementAvant2009ExercicesNonCalesSurAnneesCiviles() throws Exception {
		final Entreprise e = addEntrepriseInconnueAuCivil();
		addForPrincipal(e, date(2000, 1, 1), MotifFor.INDETERMINE, date(2015, 7, 31), MotifFor.INDETERMINE, MockCommune.Aigle);
		addBouclement(e, date(2000, 1, 1), DayMonth.get(6, 30), 12);       // bouclements tous les 30.06 depuis le 30.06.2000

		final List<PeriodeImposition> periodes = determine(e);
		Assert.assertNotNull(periodes);
		Assert.assertEquals(8, periodes.size());
		assertPeriodeImpositionPersonnesMorales(date(2008, 7, 1), date(2009, 6, 30), false, TypeContribuable.VAUDOIS_ORDINAIRE, false, false, false, periodes.get(0));
		assertPeriodeImpositionPersonnesMorales(date(2009, 7, 1), date(2010, 6, 30), false, TypeContribuable.VAUDOIS_ORDINAIRE, false, false, false, periodes.get(1));
		assertPeriodeImpositionPersonnesMorales(date(2010, 7, 1), date(2011, 6, 30), false, TypeContribuable.VAUDOIS_ORDINAIRE, false, false, false, periodes.get(2));
		assertPeriodeImpositionPersonnesMorales(date(2011, 7, 1), date(2012, 6, 30), false, TypeContribuable.VAUDOIS_ORDINAIRE, false, false, false, periodes.get(3));
		assertPeriodeImpositionPersonnesMorales(date(2012, 7, 1), date(2013, 6, 30), false, TypeContribuable.VAUDOIS_ORDINAIRE, false, false, false, periodes.get(4));
		assertPeriodeImpositionPersonnesMorales(date(2013, 7, 1), date(2014, 6, 30), false, TypeContribuable.VAUDOIS_ORDINAIRE, false, false, false, periodes.get(5));
		assertPeriodeImpositionPersonnesMorales(date(2014, 7, 1), date(2015, 6, 30), false, TypeContribuable.VAUDOIS_ORDINAIRE, false, false, false, periodes.get(6));
		assertPeriodeImpositionPersonnesMorales(date(2015, 7, 1), date(2015, 7, 31), false, TypeContribuable.VAUDOIS_ORDINAIRE, false, false, false, periodes.get(7));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testAssujettissementOuvert() throws Exception {
		final Entreprise e = addEntrepriseInconnueAuCivil();
		addForPrincipal(e, date(2000, 1, 1), MotifFor.INDETERMINE, MockCommune.Aigle);
		addBouclement(e, date(2000, 1, 1), DayMonth.get(6, 30), 12);       // bouclements tous les 30.06 depuis le 30.06.2000

		final List<PeriodeImposition> periodes = determine(e);
		Assert.assertNotNull(periodes);

		final int nbPeriodesAttendues = RegDate.get().year() - 2009 + 1 + (DayMonth.get().compareTo(DayMonth.get(6, 30)) > 0 ? 1 : 0);
		Assert.assertEquals(nbPeriodesAttendues, periodes.size());
		for (int i = 0 ; i < nbPeriodesAttendues ; ++ i) {
			assertPeriodeImpositionPersonnesMorales(date(2008 + i, 7, 1), date(2009 + i, 6, 30), false, TypeContribuable.VAUDOIS_ORDINAIRE, false, false, false, periodes.get(i));
		}
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testVaudoisExercicesEtForsCalesSurAnneeCivile() throws Exception {
		final Entreprise e = addEntrepriseInconnueAuCivil();
		addForPrincipal(e, date(2013, 1, 1), MotifFor.INDETERMINE, date(2015, 7, 31), MotifFor.INDETERMINE, MockCommune.Aigle);
		addBouclement(e, date(2013, 1, 1), DayMonth.get(12, 31), 12);       // bouclements tous les 31.12 depuis le 31.12.2013

		final List<PeriodeImposition> periodes = determine(e);
		Assert.assertNotNull(periodes);
		Assert.assertEquals(3, periodes.size());
		assertPeriodeImpositionPersonnesMorales(date(2013, 1, 1), date(2013, 12, 31), false, TypeContribuable.VAUDOIS_ORDINAIRE, false, false, false, periodes.get(0));
		assertPeriodeImpositionPersonnesMorales(date(2014, 1, 1), date(2014, 12, 31), false, TypeContribuable.VAUDOIS_ORDINAIRE, false, false, false, periodes.get(1));
		assertPeriodeImpositionPersonnesMorales(date(2015, 1, 1), date(2015, 7, 31), false, TypeContribuable.VAUDOIS_ORDINAIRE, false, false, false, periodes.get(2));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testVaudoisExercicesCalesSurAnneeCivileMaisPasPremierFor() throws Exception {
		final Entreprise e = addEntrepriseInconnueAuCivil();
		addForPrincipal(e, date(2013, 4, 21), MotifFor.INDETERMINE, date(2015, 7, 31), MotifFor.INDETERMINE, MockCommune.Aigle);
		addBouclement(e, date(2013, 1, 1), DayMonth.get(12, 31), 12);       // bouclements tous les 31.12 depuis le 31.12.2013

		final List<PeriodeImposition> periodes = determine(e);
		Assert.assertNotNull(periodes);
		Assert.assertEquals(3, periodes.size());
		assertPeriodeImpositionPersonnesMorales(date(2013, 4, 21), date(2013, 12, 31), false, TypeContribuable.VAUDOIS_ORDINAIRE, false, false, false, periodes.get(0));
		assertPeriodeImpositionPersonnesMorales(date(2014, 1, 1), date(2014, 12, 31), false, TypeContribuable.VAUDOIS_ORDINAIRE, false, false, false, periodes.get(1));
		assertPeriodeImpositionPersonnesMorales(date(2015, 1, 1), date(2015, 7, 31), false, TypeContribuable.VAUDOIS_ORDINAIRE, false, false, false, periodes.get(2));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testVaudoisExercicesNonCalesSurAnneeCivile() throws Exception {
		final Entreprise e = addEntrepriseInconnueAuCivil();
		addForPrincipal(e, date(2013, 1, 15), MotifFor.INDETERMINE, date(2015, 7, 31), MotifFor.INDETERMINE, MockCommune.Aigle);
		addBouclement(e, date(2013, 1, 1), DayMonth.get(6, 30), 12);    // bouclements tous les 30.06 depuis le 31.03.2013

		final List<PeriodeImposition> periodes = determine(e);
		Assert.assertNotNull(periodes);
		Assert.assertEquals(4, periodes.size());
		assertPeriodeImpositionPersonnesMorales(date(2013, 1, 15), date(2013, 6, 30), false, TypeContribuable.VAUDOIS_ORDINAIRE, false, false, false, periodes.get(0));
		assertPeriodeImpositionPersonnesMorales(date(2013, 7, 1), date(2014, 6, 30), false, TypeContribuable.VAUDOIS_ORDINAIRE, false, false, false, periodes.get(1));
		assertPeriodeImpositionPersonnesMorales(date(2014, 7, 1), date(2015, 6, 30), false, TypeContribuable.VAUDOIS_ORDINAIRE, false, false, false, periodes.get(2));
		assertPeriodeImpositionPersonnesMorales(date(2015, 7, 1), date(2015, 7, 31), false, TypeContribuable.VAUDOIS_ORDINAIRE, false, false, false, periodes.get(3));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testVaudoisModificationDureeExcercicesCommerciauxDebutAnneeCivile() throws Exception {
		final Entreprise e = addEntrepriseInconnueAuCivil();
		addForPrincipal(e, date(2013, 1, 15), MotifFor.INDETERMINE, date(2015, 7, 31), MotifFor.INDETERMINE, MockCommune.Aigle);
		addBouclement(e, date(2013, 1, 1), DayMonth.get(12, 31), 12);    // bouclements tous les 31.12 depuis le 31.12.2013
		addBouclement(e, date(2014, 3, 31), DayMonth.get(3, 31), 12);    // bouclements tous les 31.03 depuis le 31.03.2014
		addBouclement(e, date(2014, 4, 1), DayMonth.get(12, 31), 12);    // bouclements tous les 31.12 depuis le 31.12.2014

		final List<PeriodeImposition> periodes = determine(e);
		Assert.assertNotNull(periodes);
		Assert.assertEquals(4, periodes.size());
		assertPeriodeImpositionPersonnesMorales(date(2013, 1, 15), date(2013, 12, 31), false, TypeContribuable.VAUDOIS_ORDINAIRE, false, false, false, periodes.get(0));
		assertPeriodeImpositionPersonnesMorales(date(2014, 1, 1), date(2014, 3, 31), false, TypeContribuable.VAUDOIS_ORDINAIRE, false, false, false, periodes.get(1));
		assertPeriodeImpositionPersonnesMorales(date(2014, 4, 1), date(2014, 12, 31), false, TypeContribuable.VAUDOIS_ORDINAIRE, false, false, false, periodes.get(2));
		assertPeriodeImpositionPersonnesMorales(date(2015, 1, 1), date(2015, 7, 31), false, TypeContribuable.VAUDOIS_ORDINAIRE, false, false, false, periodes.get(3));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testVaudoisModificationDureeExcercicesCommerciauxEnCoursAnneeCivile() throws Exception {
		final Entreprise e = addEntrepriseInconnueAuCivil();
		addForPrincipal(e, date(2013, 1, 15), MotifFor.INDETERMINE, date(2015, 7, 31), MotifFor.INDETERMINE, MockCommune.Aigle);
		addBouclement(e, date(2013, 3, 31), DayMonth.get(3, 31), 12);   // bouclements tous les 31.03 depuis le 31.03.2013
		addBouclement(e, date(2014, 4, 1), DayMonth.get(6, 30), 12);    // bouclements tous les 30.06 depuis le 30.06.2014
		addBouclement(e, date(2014, 7, 1), DayMonth.get(3, 31), 12);    // bouclements tous les 31.03 depuis le 31.03.2015

		final List<PeriodeImposition> periodes = determine(e);
		Assert.assertNotNull(periodes);
		Assert.assertEquals(5, periodes.size());
		assertPeriodeImpositionPersonnesMorales(date(2013, 1, 15), date(2013, 3, 31), false, TypeContribuable.VAUDOIS_ORDINAIRE, false, false, false, periodes.get(0));
		assertPeriodeImpositionPersonnesMorales(date(2013, 4, 1), date(2014, 3, 31), false, TypeContribuable.VAUDOIS_ORDINAIRE, false, false, false, periodes.get(1));
		assertPeriodeImpositionPersonnesMorales(date(2014, 4, 1), date(2014, 6, 30), false, TypeContribuable.VAUDOIS_ORDINAIRE, false, false, false, periodes.get(2));
		assertPeriodeImpositionPersonnesMorales(date(2014, 7, 1), date(2015, 3, 31), false, TypeContribuable.VAUDOIS_ORDINAIRE, false, false, false, periodes.get(3));
		assertPeriodeImpositionPersonnesMorales(date(2015, 4, 1), date(2015, 7, 31), false, TypeContribuable.VAUDOIS_ORDINAIRE, false, false, false, periodes.get(4));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testVaudoisArriveeHorsSuisseBouclementApresDansAnnee() throws Exception {
		final Entreprise e = addEntrepriseInconnueAuCivil();
		addForPrincipal(e, date(2014, 4, 21), MotifFor.ARRIVEE_HS, date(2015, 7, 31), MotifFor.INDETERMINE, MockCommune.Echallens);
		addBouclement(e, date(2013, 12, 31), DayMonth.get(12, 31), 12);       // bouclements tous les 31.12 depuis le 31.12.2013 (= un exercice commercial est partiellement hors zone de fors)

		final List<PeriodeImposition> periodes = determine(e);
		Assert.assertNotNull(periodes);
		Assert.assertEquals(2, periodes.size());
		assertPeriodeImpositionPersonnesMorales(date(2014, 4, 21), date(2014, 12, 31), false, TypeContribuable.VAUDOIS_ORDINAIRE, false, false, false, periodes.get(0));
		assertPeriodeImpositionPersonnesMorales(date(2015, 1, 1), date(2015, 7, 31), false, TypeContribuable.VAUDOIS_ORDINAIRE, false, false, false, periodes.get(1));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testVaudoisArriveeHorsSuisseBouclementSansBouclementApresArriveeDansAnnee() throws Exception {
		final Entreprise e = addEntrepriseInconnueAuCivil();
		addForPrincipal(e, date(2014, 4, 21), MotifFor.ARRIVEE_HS, date(2015, 7, 31), MotifFor.INDETERMINE, MockCommune.Echallens);
		addBouclement(e, date(2014, 3, 31), DayMonth.get(3, 31), 12);       // bouclements tous les 31.03 depuis le 31.03.2014 (= un exercice commercial est partiellement hors zone de fors)

		final List<PeriodeImposition> periodes = determine(e);
		Assert.assertNotNull(periodes);
		Assert.assertEquals(2, periodes.size());
		assertPeriodeImpositionPersonnesMorales(date(2014, 4, 21), date(2015, 3, 31), false, TypeContribuable.VAUDOIS_ORDINAIRE, false, false, false, periodes.get(0));
		assertPeriodeImpositionPersonnesMorales(date(2015, 4, 1), date(2015, 7, 31), false, TypeContribuable.VAUDOIS_ORDINAIRE, false, false, false, periodes.get(1));
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

		final List<PeriodeImposition> periodes = determine(e);
		Assert.assertNotNull(periodes);
		Assert.assertEquals(2, periodes.size());
		assertPeriodeImpositionPersonnesMorales(dateCreation, date(2014, 3, 31), false, TypeContribuable.VAUDOIS_ORDINAIRE, false, false, false, periodes.get(0));
		assertPeriodeImpositionPersonnesMorales(date(2014, 4, 1), dateDepart, false, TypeContribuable.VAUDOIS_ORDINAIRE, false, false, false, periodes.get(1));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testVaudoisArriveeHorsCantonSansBouclementApresArriveeDansAnnee() throws Exception {
		final Entreprise e = addEntrepriseInconnueAuCivil();
		addForPrincipal(e, date(2014, 4, 21), MotifFor.ARRIVEE_HC, date(2015, 7, 31), MotifFor.INDETERMINE, MockCommune.Echallens);
		addBouclement(e, date(2014, 3, 31), DayMonth.get(3, 31), 12);       // bouclements tous les 31.03 depuis le 31.03.2014 (= un exercice commercial est partiellement hors zone de fors)

		final List<PeriodeImposition> periodes = determine(e);
		Assert.assertNotNull(periodes);
		Assert.assertEquals(2, periodes.size());
		assertPeriodeImpositionPersonnesMorales(date(2014, 4, 1), date(2015, 3, 31), false, TypeContribuable.VAUDOIS_ORDINAIRE, false, false, false, periodes.get(0));
		assertPeriodeImpositionPersonnesMorales(date(2015, 4, 1), date(2015, 7, 31), false, TypeContribuable.VAUDOIS_ORDINAIRE, false, false, false, periodes.get(1));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testVaudoisArriveeHorsCantonLendemainBouclement() throws Exception {
		final Entreprise e = addEntrepriseInconnueAuCivil();
		addForPrincipal(e, date(2014, 4, 1), MotifFor.ARRIVEE_HC, date(2015, 7, 31), MotifFor.INDETERMINE, MockCommune.Echallens);
		addBouclement(e, date(2014, 3, 31), DayMonth.get(3, 31), 12);       // bouclements tous les 31.03 depuis le 31.03.2014

		final List<PeriodeImposition> periodes = determine(e);
		Assert.assertNotNull(periodes);
		Assert.assertEquals(2, periodes.size());
		assertPeriodeImpositionPersonnesMorales(date(2014, 4, 1), date(2015, 3, 31), false, TypeContribuable.VAUDOIS_ORDINAIRE, false, false, false, periodes.get(0));
		assertPeriodeImpositionPersonnesMorales(date(2015, 4, 1), date(2015, 7, 31), false, TypeContribuable.VAUDOIS_ORDINAIRE, false, false, false, periodes.get(1));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testHorsCantonImmeubleAchatSynchroneAvecExercice() throws Exception {

		final RegDate dateAchat = date(2013, 4, 1);
		final RegDate dateVente = date(2015, 7, 31);

		final Entreprise e = addEntrepriseInconnueAuCivil();
		addForPrincipal(e, dateAchat, null, MockCommune.Bale);
		addForSecondaire(e, dateAchat, MotifFor.ACHAT_IMMOBILIER, dateVente, MotifFor.VENTE_IMMOBILIER, MockCommune.Cossonay.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
		addBouclement(e, date(2013, 3, 31), DayMonth.get(3, 31), 12);       // bouclements tous les 31.03 depuis le 31.03.2013

		final List<PeriodeImposition> periodes = determine(e);
		Assert.assertNotNull(periodes);
		Assert.assertEquals(3, periodes.size());
		assertPeriodeImpositionPersonnesMorales(date(2013, 4, 1), date(2014, 3, 31), false, TypeContribuable.HORS_CANTON, false, false, false, periodes.get(0));
		assertPeriodeImpositionPersonnesMorales(date(2014, 4, 1), date(2015, 3, 31), false, TypeContribuable.HORS_CANTON, false, false, false, periodes.get(1));
		assertPeriodeImpositionPersonnesMorales(date(2015, 4, 1), date(2016, 3, 31), false, TypeContribuable.HORS_CANTON, false, false, false, periodes.get(2));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testHorsCantonImmeubleAchatNonSynchroneAvecExercice() throws Exception {

		final RegDate dateAchat = date(2013, 4, 1);
		final RegDate dateVente = date(2015, 7, 31);

		final Entreprise e = addEntrepriseInconnueAuCivil();
		addForPrincipal(e, dateAchat, null, MockCommune.Bale);
		addForSecondaire(e, dateAchat, MotifFor.ACHAT_IMMOBILIER, dateVente, MotifFor.VENTE_IMMOBILIER, MockCommune.Cossonay.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
		addBouclement(e, date(2012, 12, 31), DayMonth.get(12, 31), 12);       // bouclements tous les 31.12 depuis le 31.12.2012

		final List<PeriodeImposition> periodes = determine(e);
		Assert.assertNotNull(periodes);
		Assert.assertEquals(3, periodes.size());
		assertPeriodeImpositionPersonnesMorales(date(2013, 1, 1), date(2013, 12, 31), false, TypeContribuable.HORS_CANTON, false, false, false, periodes.get(0));
		assertPeriodeImpositionPersonnesMorales(date(2014, 1, 1), date(2014, 12, 31), false, TypeContribuable.HORS_CANTON, false, false, false, periodes.get(1));
		assertPeriodeImpositionPersonnesMorales(date(2015, 1, 1), date(2015, 12, 31), false, TypeContribuable.HORS_CANTON, false, false, false, periodes.get(2));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testHorsCantonActiviteSynchroneAvecExercice() throws Exception {

		final RegDate dateDebutExploitation = date(2013, 4, 1);
		final RegDate dateFinExploitation = date(2015, 3, 31);

		final Entreprise e = addEntrepriseInconnueAuCivil();
		addForPrincipal(e, dateDebutExploitation, null, MockCommune.Bale);
		addForSecondaire(e, dateDebutExploitation, MotifFor.DEBUT_EXPLOITATION, dateFinExploitation, MotifFor.FIN_EXPLOITATION, MockCommune.Cossonay.getNoOFS(), MotifRattachement.ACTIVITE_INDEPENDANTE);
		addBouclement(e, date(2013, 3, 31), DayMonth.get(3, 31), 12);       // bouclements tous les 31.03 depuis le 31.03.2013

		final List<PeriodeImposition> periodes = determine(e);
		Assert.assertNotNull(periodes);
		Assert.assertEquals(2, periodes.size());
		assertPeriodeImpositionPersonnesMorales(date(2013, 4, 1), date(2014, 3, 31), false, TypeContribuable.HORS_CANTON, false, false, false, periodes.get(0));
		assertPeriodeImpositionPersonnesMorales(date(2014, 4, 1), date(2015, 3, 31), false, TypeContribuable.HORS_CANTON, false, false, false, periodes.get(1));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testHorsCantonActiviteNonSynchroneAvecExercice() throws Exception {

		final RegDate dateDebutExploitation = date(2013, 4, 1);
		final RegDate dateFinExploitation = date(2015, 7, 31);

		final Entreprise e = addEntrepriseInconnueAuCivil();
		addForPrincipal(e, dateDebutExploitation, null, MockCommune.Bale);
		addForSecondaire(e, dateDebutExploitation, MotifFor.DEBUT_EXPLOITATION, dateFinExploitation, MotifFor.FIN_EXPLOITATION, MockCommune.Cossonay.getNoOFS(), MotifRattachement.ACTIVITE_INDEPENDANTE);
		addBouclement(e, date(2012, 12, 31), DayMonth.get(12, 31), 12);       // bouclements tous les 31.12 depuis le 31.12.2012

		final List<PeriodeImposition> periodes = determine(e);
		Assert.assertNotNull(periodes);
		Assert.assertEquals(3, periodes.size());
		assertPeriodeImpositionPersonnesMorales(date(2013, 1, 1), date(2013, 12, 31), false, TypeContribuable.HORS_CANTON, false, false, false, periodes.get(0));
		assertPeriodeImpositionPersonnesMorales(date(2014, 1, 1), date(2014, 12, 31), false, TypeContribuable.HORS_CANTON, false, false, false, periodes.get(1));
		assertPeriodeImpositionPersonnesMorales(date(2015, 1, 1), date(2015, 12, 31), false, TypeContribuable.HORS_CANTON, false, false, false, periodes.get(2));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testHorsSuisseImmeubleAchatSynchroneAvecExercice() throws Exception {

		final RegDate dateAchat = date(2013, 4, 1);
		final RegDate dateVente = date(2015, 3, 31);

		final Entreprise e = addEntrepriseInconnueAuCivil();
		addForPrincipal(e, dateAchat, null, MockPays.Allemagne);
		addForSecondaire(e, dateAchat, MotifFor.ACHAT_IMMOBILIER, dateVente, MotifFor.VENTE_IMMOBILIER, MockCommune.Cossonay.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
		addBouclement(e, date(2013, 3, 31), DayMonth.get(3, 31), 12);       // bouclements tous les 31.03 depuis le 31.03.2013

		final List<PeriodeImposition> periodes = determine(e);
		Assert.assertNotNull(periodes);
		Assert.assertEquals(2, periodes.size());
		assertPeriodeImpositionPersonnesMorales(dateAchat, date(2014, 3, 31), false, TypeContribuable.HORS_SUISSE, false, false, false, periodes.get(0));
		assertPeriodeImpositionPersonnesMorales(date(2014, 4, 1), dateVente, false, TypeContribuable.HORS_SUISSE, false, false, true, periodes.get(1));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testHorsSuisseImmeubleAchatNonSynchroneAvecExercice() throws Exception {

		final RegDate dateAchat = date(2013, 4, 1);
		final RegDate dateVente = date(2015, 7, 31);

		final Entreprise e = addEntrepriseInconnueAuCivil();
		addForPrincipal(e, dateAchat, null, MockPays.Allemagne);
		addForSecondaire(e, dateAchat, MotifFor.ACHAT_IMMOBILIER, dateVente, MotifFor.VENTE_IMMOBILIER, MockCommune.Cossonay.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
		addBouclement(e, date(2012, 12, 31), DayMonth.get(12, 31), 12);       // bouclements tous les 31.12 depuis le 31.12.2012

		final List<PeriodeImposition> periodes = determine(e);
		Assert.assertNotNull(periodes);
		Assert.assertEquals(3, periodes.size());
		assertPeriodeImpositionPersonnesMorales(dateAchat, date(2013, 12, 31), false, TypeContribuable.HORS_SUISSE, false, false, false, periodes.get(0));
		assertPeriodeImpositionPersonnesMorales(date(2014, 1, 1), date(2014, 12, 31), false, TypeContribuable.HORS_SUISSE, false, false, false, periodes.get(1));
		assertPeriodeImpositionPersonnesMorales(date(2015, 1, 1), dateVente, false, TypeContribuable.HORS_SUISSE, false, false, true, periodes.get(2));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testHorsSuisseActiviteSynchroneAvecExercice() throws Exception {

		final RegDate dateDebutExploitation = date(2013, 4, 1);
		final RegDate dateFinExploitation = date(2015, 3, 31);

		final Entreprise e = addEntrepriseInconnueAuCivil();
		addForPrincipal(e, dateDebutExploitation, null, MockPays.France);
		addForSecondaire(e, dateDebutExploitation, MotifFor.DEBUT_EXPLOITATION, dateFinExploitation, MotifFor.FIN_EXPLOITATION, MockCommune.Cossonay.getNoOFS(), MotifRattachement.ACTIVITE_INDEPENDANTE);
		addBouclement(e, date(2013, 3, 31), DayMonth.get(3, 31), 12);       // bouclements tous les 31.03 depuis le 31.03.2013

		final List<PeriodeImposition> periodes = determine(e);
		Assert.assertNotNull(periodes);
		Assert.assertEquals(2, periodes.size());
		assertPeriodeImpositionPersonnesMorales(dateDebutExploitation, date(2014, 3, 31), false, TypeContribuable.HORS_SUISSE, false, false, false, periodes.get(0));
		assertPeriodeImpositionPersonnesMorales(date(2014, 4, 1), dateFinExploitation, false, TypeContribuable.HORS_SUISSE, false, false, true, periodes.get(1));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testHorsSuisseActiviteNonSynchroneAvecExercice() throws Exception {

		final RegDate dateDebutExploitation = date(2013, 4, 1);
		final RegDate dateFinExploitation = date(2015, 7, 31);

		final Entreprise e = addEntrepriseInconnueAuCivil();
		addForPrincipal(e, dateDebutExploitation, null, MockPays.RoyaumeUni);
		addForSecondaire(e, dateDebutExploitation, MotifFor.DEBUT_EXPLOITATION, dateFinExploitation, MotifFor.FIN_EXPLOITATION, MockCommune.Cossonay.getNoOFS(), MotifRattachement.ACTIVITE_INDEPENDANTE);
		addBouclement(e, date(2012, 12, 31), DayMonth.get(12, 31), 12);       // bouclements tous les 31.12 depuis le 31.12.2012

		final List<PeriodeImposition> periodes = determine(e);
		Assert.assertNotNull(periodes);
		Assert.assertEquals(3, periodes.size());
		assertPeriodeImpositionPersonnesMorales(dateDebutExploitation, date(2013, 12, 31), false, TypeContribuable.HORS_SUISSE, false, false, false, periodes.get(0));
		assertPeriodeImpositionPersonnesMorales(date(2014, 1, 1), date(2014, 12, 31), false, TypeContribuable.HORS_SUISSE, false, false, false, periodes.get(1));
		assertPeriodeImpositionPersonnesMorales(date(2015, 1, 1), dateFinExploitation, false, TypeContribuable.HORS_SUISSE, false, false, true, periodes.get(2));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testHorsCantonImmeubleTransfertSiegeVersVaud() throws Exception {

		final RegDate dateAchat = date(2013, 4, 15);
		final RegDate dateArriveeSiege = date(2014, 5, 21);
		final RegDate dateFinActivite = date(2015, 7, 28);

		final Entreprise e = addEntrepriseInconnueAuCivil();
		addForPrincipal(e, dateAchat, null, dateArriveeSiege.getOneDayBefore(), MotifFor.ARRIVEE_HC, MockCommune.Bale);
		addForPrincipal(e, dateArriveeSiege, MotifFor.ARRIVEE_HC, dateFinActivite, MotifFor.INDETERMINE, MockCommune.Morges);
		addForSecondaire(e, dateAchat, MotifFor.ACHAT_IMMOBILIER, dateFinActivite, MotifFor.VENTE_IMMOBILIER, MockCommune.Cossonay.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
		addBouclement(e, date(2012, 3, 31), DayMonth.get(3, 31), 12);       // bouclements tous les 31.03 depuis le 31.03.2012

		final List<PeriodeImposition> periodes = determine(e);
		Assert.assertNotNull(periodes);
		Assert.assertEquals(3, periodes.size());
		assertPeriodeImpositionPersonnesMorales(date(2013, 4, 1), date(2014, 3, 31), false, TypeContribuable.HORS_CANTON, false, false, false, periodes.get(0));
		assertPeriodeImpositionPersonnesMorales(date(2014, 4, 1), date(2015, 3, 31), false, TypeContribuable.VAUDOIS_ORDINAIRE, false, false, false, periodes.get(1));
		assertPeriodeImpositionPersonnesMorales(date(2015, 4, 1), date(2015, 7, 28), false, TypeContribuable.VAUDOIS_ORDINAIRE, false, false, false, periodes.get(2));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testHorsCantonActiviteTransfertSiegeVersVaud() throws Exception {

		final RegDate dateDebutExploitation = date(2013, 4, 15);
		final RegDate dateArriveeSiege = date(2014, 5, 21);
		final RegDate dateFinActivite = date(2015, 7, 28);

		final Entreprise e = addEntrepriseInconnueAuCivil();
		addForPrincipal(e, dateDebutExploitation, null, dateArriveeSiege.getOneDayBefore(), MotifFor.ARRIVEE_HC, MockCommune.Bale);
		addForPrincipal(e, dateArriveeSiege, MotifFor.ARRIVEE_HC, dateFinActivite, MotifFor.INDETERMINE, MockCommune.Morges);
		addForSecondaire(e, dateDebutExploitation, MotifFor.DEBUT_EXPLOITATION, dateFinActivite, MotifFor.FIN_EXPLOITATION, MockCommune.Cossonay.getNoOFS(), MotifRattachement.ACTIVITE_INDEPENDANTE);
		addBouclement(e, date(2012, 3, 31), DayMonth.get(3, 31), 12);       // bouclements tous les 31.03 depuis le 31.03.2012

		final List<PeriodeImposition> periodes = determine(e);
		Assert.assertNotNull(periodes);
		Assert.assertEquals(3, periodes.size());
		assertPeriodeImpositionPersonnesMorales(date(2013, 4, 1), date(2014, 3, 31), false, TypeContribuable.HORS_CANTON, false, false, false, periodes.get(0));
		assertPeriodeImpositionPersonnesMorales(date(2014, 4, 1), date(2015, 3, 31), false, TypeContribuable.VAUDOIS_ORDINAIRE, false, false, false, periodes.get(1));
		assertPeriodeImpositionPersonnesMorales(date(2015, 4, 1), date(2015, 7, 28), false, TypeContribuable.VAUDOIS_ORDINAIRE, false, false, false, periodes.get(2));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testHorsSuisseImmeubleTransfertSiegeVersVaud() throws Exception {

		final RegDate dateAchat = date(2013, 4, 15);
		final RegDate dateArriveeSiege = date(2014, 5, 21);
		final RegDate dateFinActivite = date(2015, 7, 28);

		final Entreprise e = addEntrepriseInconnueAuCivil();
		addForPrincipal(e, dateAchat, null, dateArriveeSiege.getOneDayBefore(), MotifFor.ARRIVEE_HS, MockPays.Allemagne);
		addForPrincipal(e, dateArriveeSiege, MotifFor.ARRIVEE_HS, dateFinActivite, MotifFor.INDETERMINE, MockCommune.Morges);
		addForSecondaire(e, dateAchat, MotifFor.ACHAT_IMMOBILIER, dateFinActivite, MotifFor.VENTE_IMMOBILIER, MockCommune.Cossonay.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
		addBouclement(e, date(2012, 3, 31), DayMonth.get(3, 31), 12);       // bouclements tous les 31.03 depuis le 31.03.2012

		final List<PeriodeImposition> periodes = determine(e);
		Assert.assertNotNull(periodes);
		Assert.assertEquals(4, periodes.size());
		assertPeriodeImpositionPersonnesMorales(dateAchat, date(2014, 3, 31), false, TypeContribuable.HORS_SUISSE, false, false, false, periodes.get(0));
		assertPeriodeImpositionPersonnesMorales(date(2014, 4, 1), dateArriveeSiege.getOneDayBefore(), false, TypeContribuable.HORS_SUISSE, false, false, false, periodes.get(1));
		assertPeriodeImpositionPersonnesMorales(dateArriveeSiege, date(2015, 3, 31), false, TypeContribuable.VAUDOIS_ORDINAIRE, false, false, false, periodes.get(2));
		assertPeriodeImpositionPersonnesMorales(date(2015, 4, 1), dateFinActivite, false, TypeContribuable.VAUDOIS_ORDINAIRE, false, false, false, periodes.get(3));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testHorsSuisseActiviteTransfertSiegeVersVaud() throws Exception {

		final RegDate dateDebutExploitation = date(2013, 4, 15);
		final RegDate dateArriveeSiege = date(2014, 5, 21);
		final RegDate dateFinActivite = date(2015, 7, 28);

		final Entreprise e = addEntrepriseInconnueAuCivil();
		addForPrincipal(e, dateDebutExploitation, null, dateArriveeSiege.getOneDayBefore(), MotifFor.ARRIVEE_HS, MockPays.Allemagne);
		addForPrincipal(e, dateArriveeSiege, MotifFor.ARRIVEE_HS, dateFinActivite, MotifFor.INDETERMINE, MockCommune.Morges);
		addForSecondaire(e, dateDebutExploitation, MotifFor.DEBUT_EXPLOITATION, dateFinActivite, MotifFor.FIN_EXPLOITATION, MockCommune.Cossonay.getNoOFS(), MotifRattachement.ACTIVITE_INDEPENDANTE);
		addBouclement(e, date(2012, 3, 31), DayMonth.get(3, 31), 12);       // bouclements tous les 31.03 depuis le 31.03.2012

		final List<PeriodeImposition> periodes = determine(e);
		Assert.assertNotNull(periodes);
		Assert.assertEquals(4, periodes.size());
		assertPeriodeImpositionPersonnesMorales(dateDebutExploitation, date(2014, 3, 31), false, TypeContribuable.HORS_SUISSE, false, false, false, periodes.get(0));
		assertPeriodeImpositionPersonnesMorales(date(2014, 4, 1), dateArriveeSiege.getOneDayBefore(), false, TypeContribuable.HORS_SUISSE, false, false, false, periodes.get(1));
		assertPeriodeImpositionPersonnesMorales(dateArriveeSiege, date(2015, 3, 31), false, TypeContribuable.VAUDOIS_ORDINAIRE, false, false, false, periodes.get(2));
		assertPeriodeImpositionPersonnesMorales(date(2015, 4, 1), dateFinActivite, false, TypeContribuable.VAUDOIS_ORDINAIRE, false, false, false, periodes.get(3));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testVaudoisImmeubleTransfertSiegeVersHorsCanton() throws Exception {

		final RegDate dateCreationEntreprise = date(2013, 2, 1);
		final RegDate dateAchat = date(2013, 4, 15);
		final RegDate dateDepartSiege = date(2014, 5, 21);
		final RegDate dateVente = date(2015, 7, 28);

		final Entreprise e = addEntrepriseInconnueAuCivil();
		addForPrincipal(e, dateCreationEntreprise, MotifFor.INDETERMINE, dateDepartSiege, MotifFor.DEPART_HC, MockCommune.Echallens);
		addForPrincipal(e, dateDepartSiege.getOneDayAfter(), MotifFor.DEPART_HC, MockCommune.Geneve);
		addForSecondaire(e, dateAchat, MotifFor.ACHAT_IMMOBILIER, dateVente, MotifFor.VENTE_IMMOBILIER, MockCommune.Cossonay.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
		addBouclement(e, date(2012, 3, 31), DayMonth.get(3, 31), 12);       // bouclements tous les 31.03 depuis le 31.03.2012
		addBouclement(e, date(2014, 4, 1), DayMonth.get(9, 30), 12);       // bouclements tous les 30.09 depuis le 30.09.2014

		final List<PeriodeImposition> periodes = determine(e);
		Assert.assertNotNull(periodes);
		Assert.assertEquals(4, periodes.size());
		assertPeriodeImpositionPersonnesMorales(dateCreationEntreprise, date(2013, 3, 31), false, TypeContribuable.VAUDOIS_ORDINAIRE, false, false, false, periodes.get(0));
		assertPeriodeImpositionPersonnesMorales(date(2013, 4, 1), date(2014, 3, 31), false, TypeContribuable.VAUDOIS_ORDINAIRE, false, false, false, periodes.get(1));
		assertPeriodeImpositionPersonnesMorales(date(2014, 4, 1), date(2014, 9, 30), false, TypeContribuable.VAUDOIS_ORDINAIRE, false, false, false, periodes.get(2));
		assertPeriodeImpositionPersonnesMorales(date(2014, 10, 1), date(2015, 9, 30), false, TypeContribuable.HORS_CANTON, false, false, false, periodes.get(3));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testVaudoisActiviteTransfertSiegeVersHorsCanton() throws Exception {

		final RegDate dateCreationEntreprise = date(2013, 2, 1);
		final RegDate dateDebutActivite = date(2013, 4, 15);
		final RegDate dateDepartSiege = date(2014, 5, 21);
		final RegDate dateFinActivite = date(2015, 7, 28);

		final Entreprise e = addEntrepriseInconnueAuCivil();
		addForPrincipal(e, dateCreationEntreprise, MotifFor.INDETERMINE, dateDepartSiege, MotifFor.DEPART_HC, MockCommune.Echallens);
		addForPrincipal(e, dateDepartSiege.getOneDayAfter(), MotifFor.DEPART_HC, MockCommune.Geneve);
		addForSecondaire(e, dateDebutActivite, MotifFor.DEBUT_EXPLOITATION, dateFinActivite, MotifFor.FIN_EXPLOITATION, MockCommune.Cossonay.getNoOFS(), MotifRattachement.ACTIVITE_INDEPENDANTE);
		addBouclement(e, date(2012, 3, 31), DayMonth.get(3, 31), 12);       // bouclements tous les 31.03 depuis le 31.03.2012
		addBouclement(e, date(2014, 4, 1), DayMonth.get(9, 30), 12);       // bouclements tous les 30.09 depuis le 30.09.2014

		final List<PeriodeImposition> periodes = determine(e);
		Assert.assertNotNull(periodes);
		Assert.assertEquals(4, periodes.size());
		assertPeriodeImpositionPersonnesMorales(dateCreationEntreprise, date(2013, 3, 31), false, TypeContribuable.VAUDOIS_ORDINAIRE, false, false, false, periodes.get(0));
		assertPeriodeImpositionPersonnesMorales(date(2013, 4, 1), date(2014, 3, 31), false, TypeContribuable.VAUDOIS_ORDINAIRE, false, false, false, periodes.get(1));
		assertPeriodeImpositionPersonnesMorales(date(2014, 4, 1), date(2014, 9, 30), false, TypeContribuable.VAUDOIS_ORDINAIRE, false, false, false, periodes.get(2));
		assertPeriodeImpositionPersonnesMorales(date(2014, 10, 1), date(2015, 9, 30), false, TypeContribuable.HORS_CANTON, false, false, false, periodes.get(3));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testVaudoisImmeubleTransfertSiegeVersHorsSuisse() throws Exception {

		final RegDate dateCreationEntreprise = date(2013, 2, 1);
		final RegDate dateAchat = date(2013, 4, 15);
		final RegDate dateDepartSiege = date(2014, 5, 21);
		final RegDate dateVente = date(2015, 7, 28);

		final Entreprise e = addEntrepriseInconnueAuCivil();
		addForPrincipal(e, dateCreationEntreprise, MotifFor.INDETERMINE, dateDepartSiege, MotifFor.DEPART_HS, MockCommune.Echallens);
		addForPrincipal(e, dateDepartSiege.getOneDayAfter(), MotifFor.DEPART_HS, MockPays.Allemagne);
		addForSecondaire(e, dateAchat, MotifFor.ACHAT_IMMOBILIER, dateVente, MotifFor.VENTE_IMMOBILIER, MockCommune.Cossonay.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
		addBouclement(e, date(2012, 3, 31), DayMonth.get(3, 31), 12);       // bouclements tous les 31.03 depuis le 31.03.2012
		addBouclement(e, date(2014, 4, 1), DayMonth.get(9, 30), 12);       // bouclements tous les 30.09 depuis le 30.09.2014

		final List<PeriodeImposition> periodes = determine(e);
		Assert.assertNotNull(periodes);
		Assert.assertEquals(5, periodes.size());
		assertPeriodeImpositionPersonnesMorales(dateCreationEntreprise, date(2013, 3, 31), false, TypeContribuable.VAUDOIS_ORDINAIRE, false, false, false, periodes.get(0));
		assertPeriodeImpositionPersonnesMorales(date(2013, 4, 1), date(2014, 3, 31), false, TypeContribuable.VAUDOIS_ORDINAIRE, false, false, false, periodes.get(1));
		assertPeriodeImpositionPersonnesMorales(date(2014, 4, 1), dateDepartSiege, false, TypeContribuable.VAUDOIS_ORDINAIRE, false, false, false, periodes.get(2));
		assertPeriodeImpositionPersonnesMorales(dateDepartSiege.getOneDayAfter(), date(2014, 9, 30), false, TypeContribuable.HORS_SUISSE, false, false, false, periodes.get(3));
		assertPeriodeImpositionPersonnesMorales(date(2014, 10, 1), dateVente, false, TypeContribuable.HORS_SUISSE, false, false, true, periodes.get(4));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testVaudoisActiviteTransfertSiegeVersHorsSuisse() throws Exception {

		final RegDate dateCreationEntreprise = date(2013, 2, 1);
		final RegDate dateDebutActivite = date(2013, 4, 15);
		final RegDate dateDepartSiege = date(2014, 5, 21);
		final RegDate dateFinActivite = date(2015, 7, 28);

		final Entreprise e = addEntrepriseInconnueAuCivil();
		addForPrincipal(e, dateCreationEntreprise, MotifFor.INDETERMINE, dateDepartSiege, MotifFor.DEPART_HS, MockCommune.Echallens);
		addForPrincipal(e, dateDepartSiege.getOneDayAfter(), MotifFor.DEPART_HS, MockPays.Allemagne);
		addForSecondaire(e, dateDebutActivite, MotifFor.DEBUT_EXPLOITATION, dateFinActivite, MotifFor.FIN_EXPLOITATION, MockCommune.Cossonay.getNoOFS(), MotifRattachement.ACTIVITE_INDEPENDANTE);
		addBouclement(e, date(2012, 3, 31), DayMonth.get(3, 31), 12);       // bouclements tous les 31.03 depuis le 31.03.2012
		addBouclement(e, date(2014, 4, 1), DayMonth.get(9, 30), 12);       // bouclements tous les 30.09 depuis le 30.09.2014

		final List<PeriodeImposition> periodes = determine(e);
		Assert.assertNotNull(periodes);
		Assert.assertEquals(5, periodes.size());
		assertPeriodeImpositionPersonnesMorales(dateCreationEntreprise, date(2013, 3, 31), false, TypeContribuable.VAUDOIS_ORDINAIRE, false, false, false, periodes.get(0));
		assertPeriodeImpositionPersonnesMorales(date(2013, 4, 1), date(2014, 3, 31), false, TypeContribuable.VAUDOIS_ORDINAIRE, false, false, false, periodes.get(1));
		assertPeriodeImpositionPersonnesMorales(date(2014, 4, 1), dateDepartSiege, false, TypeContribuable.VAUDOIS_ORDINAIRE, false, false, false, periodes.get(2));
		assertPeriodeImpositionPersonnesMorales(dateDepartSiege.getOneDayAfter(), date(2014, 9, 30), false, TypeContribuable.HORS_SUISSE, false, false, false, periodes.get(3));
		assertPeriodeImpositionPersonnesMorales(date(2014, 10, 1), dateFinActivite, false, TypeContribuable.HORS_SUISSE, false, false, true, periodes.get(4));
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
		final RegDate dateDissolutionEntreprise = date(2015, 7, 28);

		final Entreprise e = addEntrepriseInconnueAuCivil();
		addForPrincipal(e, dateCreationEntreprise, MotifFor.INDETERMINE, dateDepartSiege, MotifFor.DEPART_HC, MockCommune.CheseauxSurLausanne);
		addForPrincipal(e, dateDepartSiege.getOneDayAfter(), MotifFor.DEPART_HC, dateRetourSiege.getOneDayBefore(), MotifFor.ARRIVEE_HC, MockCommune.Geneve);
		addForPrincipal(e, dateRetourSiege, MotifFor.ARRIVEE_HC, dateDissolutionEntreprise, MotifFor.INDETERMINE, MockCommune.Lausanne);
		addBouclement(e, date(2014, 1, 1), DayMonth.get(3, 31), 12);        // bouclements tous les 31.03 depuis le 31.03.2014
		addBouclement(e, date(2014, 4, 1), DayMonth.get(6, 30), 3);         // bouclements tous les 3 mois depuis le 30.06.2014
		addBouclement(e, date(2014, 10, 1), DayMonth.get(9, 30), 12);       // bouclements tous les 30.09 depuis le 30.09.2015

		final List<PeriodeImposition> periodes = determine(e);
		Assert.assertNotNull(periodes);
		Assert.assertEquals(3, periodes.size());
		assertPeriodeImpositionPersonnesMorales(dateCreationEntreprise, date(2014, 3, 31), false, TypeContribuable.VAUDOIS_ORDINAIRE, false, false, false, periodes.get(0));
		assertPeriodeImpositionPersonnesMorales(date(2014, 4, 1), date(2014, 6, 30), false, TypeContribuable.VAUDOIS_ORDINAIRE, false, false, false, periodes.get(1));
		assertPeriodeImpositionPersonnesMorales(date(2014, 10, 1), dateDissolutionEntreprise, false, TypeContribuable.VAUDOIS_ORDINAIRE, false, false, false, periodes.get(2));
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
		final RegDate dateDissolutionEntreprise = date(2015, 7, 28);

		final Entreprise e = addEntrepriseInconnueAuCivil();
		addForPrincipal(e, dateCreationEntreprise, MotifFor.INDETERMINE, dateDepartSiege, MotifFor.DEPART_HC, MockCommune.CheseauxSurLausanne);
		addForPrincipal(e, dateDepartSiege.getOneDayAfter(), MotifFor.DEPART_HC, dateRetourSiege.getOneDayBefore(), MotifFor.ARRIVEE_HC, MockCommune.Geneve);
		addForPrincipal(e, dateRetourSiege, MotifFor.ARRIVEE_HC, dateDissolutionEntreprise, MotifFor.INDETERMINE, MockCommune.Lausanne);
		addBouclement(e, date(2014, 1, 1), DayMonth.get(3, 31), 12);        // bouclements tous les 31.03 depuis le 31.03.2014
		addBouclement(e, date(2014, 4, 1), DayMonth.get(6, 30), 3);         // bouclements tous les 3 mois depuis le 30.06.2014
		addBouclement(e, date(2014, 10, 1), DayMonth.get(9, 30), 12);       // bouclements tous les 30.09 depuis le 30.09.2015

		final List<PeriodeImposition> periodes = determine(e);
		Assert.assertNotNull(periodes);
		Assert.assertEquals(4, periodes.size());
		assertPeriodeImpositionPersonnesMorales(dateCreationEntreprise, date(2014, 3, 31), false, TypeContribuable.VAUDOIS_ORDINAIRE, false, false, false, periodes.get(0));
		assertPeriodeImpositionPersonnesMorales(date(2014, 4, 1), date(2014, 6, 30), false, TypeContribuable.VAUDOIS_ORDINAIRE, false, false, false, periodes.get(1));
		assertPeriodeImpositionPersonnesMorales(date(2014, 7, 1), date(2014, 9, 30), false, TypeContribuable.VAUDOIS_ORDINAIRE, false, false, false, periodes.get(2));
		assertPeriodeImpositionPersonnesMorales(date(2014, 10, 1), dateDissolutionEntreprise, false, TypeContribuable.VAUDOIS_ORDINAIRE, false, false, false, periodes.get(3));
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
		final RegDate dateDissolutionEntreprise = date(2015, 7, 28);

		final Entreprise e = addEntrepriseInconnueAuCivil();
		addForPrincipal(e, dateCreationEntreprise, MotifFor.INDETERMINE, dateDepartSiege, MotifFor.DEPART_HC, MockCommune.CheseauxSurLausanne);
		addForPrincipal(e, dateDepartSiege.getOneDayAfter(), MotifFor.DEPART_HC, dateRetourSiege.getOneDayBefore(), MotifFor.ARRIVEE_HC, MockCommune.Geneve);
		addForPrincipal(e, dateRetourSiege, MotifFor.ARRIVEE_HC, dateDissolutionEntreprise, MotifFor.INDETERMINE, MockCommune.Lausanne);
		addBouclement(e, date(2014, 1, 1), DayMonth.get(3, 31), 12);        // bouclements tous les 31.03 depuis le 31.03.2014
		addBouclement(e, date(2014, 4, 1), DayMonth.get(9, 30), 12);         // bouclements tous les 30.09 mois depuis le 30.09.2014

		final List<PeriodeImposition> periodes = determine(e);
		Assert.assertNotNull(periodes);
		Assert.assertEquals(3, periodes.size());
		assertPeriodeImpositionPersonnesMorales(dateCreationEntreprise, date(2014, 3, 31), false, TypeContribuable.VAUDOIS_ORDINAIRE, false, false, false, periodes.get(0));
		assertPeriodeImpositionPersonnesMorales(date(2014, 4, 1), date(2014, 9, 30), false, TypeContribuable.VAUDOIS_ORDINAIRE, false, false, false, periodes.get(1));
		assertPeriodeImpositionPersonnesMorales(date(2014, 10, 1), dateDissolutionEntreprise, false, TypeContribuable.VAUDOIS_ORDINAIRE, false, false, false, periodes.get(2));
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
		final RegDate dateDissolutionEntreprise = date(2015, 7, 28);

		final Entreprise e = addEntrepriseInconnueAuCivil();
		addForPrincipal(e, dateCreationEntreprise, MotifFor.INDETERMINE, dateDepartSiege, MotifFor.DEPART_HC, MockCommune.CheseauxSurLausanne);
		addForPrincipal(e, dateDepartSiege.getOneDayAfter(), MotifFor.DEPART_HC, dateRetourSiege.getOneDayBefore(), MotifFor.ARRIVEE_HC, MockCommune.Geneve);
		addForPrincipal(e, dateRetourSiege, MotifFor.ARRIVEE_HC, dateDissolutionEntreprise, MotifFor.INDETERMINE, MockCommune.Lausanne);
		addForSecondaire(e, dateDebutForSecondaire, MotifFor.ACHAT_IMMOBILIER, dateFinForSecondaire, MotifFor.VENTE_IMMOBILIER, MockCommune.Cossonay.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
		addBouclement(e, date(2014, 1, 1), DayMonth.get(3, 31), 12);        // bouclements tous les 31.03 depuis le 31.03.2014
		addBouclement(e, date(2014, 4, 1), DayMonth.get(6, 30), 3);         // bouclements tous les 3 mois depuis le 30.06.2014
		addBouclement(e, date(2014, 10, 1), DayMonth.get(9, 30), 12);       // bouclements tous les 30.09 depuis le 30.09.2015

		final List<PeriodeImposition> periodes = determine(e);
		Assert.assertNotNull(periodes);
		Assert.assertEquals(4, periodes.size());
		assertPeriodeImpositionPersonnesMorales(dateCreationEntreprise, date(2014, 3, 31), false, TypeContribuable.VAUDOIS_ORDINAIRE, false, false, false, periodes.get(0));
		assertPeriodeImpositionPersonnesMorales(date(2014, 4, 1), date(2014, 6, 30), false, TypeContribuable.VAUDOIS_ORDINAIRE, false, false, false, periodes.get(1));
		assertPeriodeImpositionPersonnesMorales(date(2014, 7, 1), date(2014, 9, 30), false, TypeContribuable.HORS_CANTON, false, false, false, periodes.get(2));
		assertPeriodeImpositionPersonnesMorales(date(2014, 10, 1), dateDissolutionEntreprise, false, TypeContribuable.VAUDOIS_ORDINAIRE, false, false, false, periodes.get(3));
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
		final RegDate dateDissolutionEntreprise = date(2015, 7, 24);

		final Entreprise e = addEntrepriseInconnueAuCivil();
		addForPrincipal(e, dateCreationEntreprise, MotifFor.INDETERMINE, dateDepartSiege, MotifFor.DEPART_HS, MockCommune.CheseauxSurLausanne);
		addForPrincipal(e, dateDepartSiege.getOneDayAfter(), MotifFor.DEPART_HS, dateRetourSiege.getOneDayBefore(), MotifFor.ARRIVEE_HS, MockPays.Allemagne);
		addForPrincipal(e, dateRetourSiege, MotifFor.ARRIVEE_HS, dateDissolutionEntreprise, MotifFor.INDETERMINE, MockCommune.Lausanne);
		addBouclement(e, date(2014, 1, 1), DayMonth.get(3, 31), 12);        // bouclements tous les 31.03 depuis le 31.03.2014
		addBouclement(e, date(2014, 4, 1), DayMonth.get(6, 30), 3);         // bouclements tous les 3 mois depuis le 30.06.2014
		addBouclement(e, date(2014, 10, 1), DayMonth.get(9, 30), 12);       // bouclements tous les 30.09 depuis le 30.09.2015

		final List<PeriodeImposition> periodes = determine(e);
		Assert.assertNotNull(periodes);
		Assert.assertEquals(3, periodes.size());
		assertPeriodeImpositionPersonnesMorales(dateCreationEntreprise, date(2014, 3, 31), false, TypeContribuable.VAUDOIS_ORDINAIRE, false, false, false, periodes.get(0));
		assertPeriodeImpositionPersonnesMorales(date(2014, 4, 1), dateDepartSiege, false, TypeContribuable.VAUDOIS_ORDINAIRE, false, false, false, periodes.get(1));
		assertPeriodeImpositionPersonnesMorales(dateRetourSiege, dateDissolutionEntreprise, false, TypeContribuable.VAUDOIS_ORDINAIRE, false, false, false, periodes.get(2));
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
		final RegDate dateDissolutionEntreprise = date(2015, 7, 21);

		final Entreprise e = addEntrepriseInconnueAuCivil();
		addForPrincipal(e, dateCreationEntreprise, MotifFor.INDETERMINE, dateDepartSiege, MotifFor.DEPART_HS, MockCommune.CheseauxSurLausanne);
		addForPrincipal(e, dateDepartSiege.getOneDayAfter(), MotifFor.DEPART_HS, dateRetourSiege.getOneDayBefore(), MotifFor.ARRIVEE_HS, MockPays.Allemagne);
		addForPrincipal(e, dateRetourSiege, MotifFor.ARRIVEE_HS, dateDissolutionEntreprise, MotifFor.INDETERMINE, MockCommune.Lausanne);
		addBouclement(e, date(2014, 1, 1), DayMonth.get(3, 31), 12);        // bouclements tous les 31.03 depuis le 31.03.2014
		addBouclement(e, date(2014, 4, 1), DayMonth.get(6, 30), 3);         // bouclements tous les 3 mois depuis le 30.06.2014
		addBouclement(e, date(2014, 10, 1), DayMonth.get(9, 30), 12);       // bouclements tous les 30.09 depuis le 30.09.2015

		final List<PeriodeImposition> periodes = determine(e);
		Assert.assertNotNull(periodes);
		Assert.assertEquals(4, periodes.size());
		assertPeriodeImpositionPersonnesMorales(dateCreationEntreprise, date(2014, 3, 31), false, TypeContribuable.VAUDOIS_ORDINAIRE, false, false, false, periodes.get(0));
		assertPeriodeImpositionPersonnesMorales(date(2014, 4, 1), dateDepartSiege, false, TypeContribuable.VAUDOIS_ORDINAIRE, false, false, false, periodes.get(1));
		assertPeriodeImpositionPersonnesMorales(dateRetourSiege, date(2014, 9, 30), false, TypeContribuable.VAUDOIS_ORDINAIRE, false, false, false, periodes.get(2));
		assertPeriodeImpositionPersonnesMorales(date(2014, 10, 1), dateDissolutionEntreprise, false, TypeContribuable.VAUDOIS_ORDINAIRE, false, false, false, periodes.get(3));
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
		final RegDate dateDissolutionEntreprise = date(2015, 7, 13);

		final Entreprise e = addEntrepriseInconnueAuCivil();
		addForPrincipal(e, dateCreationEntreprise, MotifFor.INDETERMINE, dateDepartSiege, MotifFor.DEPART_HS, MockCommune.CheseauxSurLausanne);
		addForPrincipal(e, dateDepartSiege.getOneDayAfter(), MotifFor.DEPART_HS, dateRetourSiege.getOneDayBefore(), MotifFor.ARRIVEE_HS, MockPays.Allemagne);
		addForPrincipal(e, dateRetourSiege, MotifFor.ARRIVEE_HS, dateDissolutionEntreprise, MotifFor.INDETERMINE, MockCommune.Lausanne);
		addBouclement(e, date(2014, 1, 1), DayMonth.get(3, 31), 12);        // bouclements tous les 31.03 depuis le 31.03.2014
		addBouclement(e, date(2014, 4, 1), DayMonth.get(9, 30), 12);         // bouclements tous les 30.09 mois depuis le 30.09.2014

		final List<PeriodeImposition> periodes = determine(e);
		Assert.assertNotNull(periodes);
		Assert.assertEquals(4, periodes.size());
		assertPeriodeImpositionPersonnesMorales(dateCreationEntreprise, date(2014, 3, 31), false, TypeContribuable.VAUDOIS_ORDINAIRE, false, false, false, periodes.get(0));
		assertPeriodeImpositionPersonnesMorales(date(2014, 4, 1), dateDepartSiege, false, TypeContribuable.VAUDOIS_ORDINAIRE, false, false, false, periodes.get(1));
		assertPeriodeImpositionPersonnesMorales(dateRetourSiege, date(2014, 9, 30), false, TypeContribuable.VAUDOIS_ORDINAIRE, false, false, false, periodes.get(2));
		assertPeriodeImpositionPersonnesMorales(date(2014, 10, 1), dateDissolutionEntreprise, false, TypeContribuable.VAUDOIS_ORDINAIRE, false, false, false, periodes.get(3));
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
		final RegDate dateDissolutionEntreprise = date(2015, 7, 31);

		final Entreprise e = addEntrepriseInconnueAuCivil();
		addForPrincipal(e, dateCreationEntreprise, MotifFor.INDETERMINE, dateDepartSiege, MotifFor.DEPART_HS, MockCommune.CheseauxSurLausanne);
		addForPrincipal(e, dateDepartSiege.getOneDayAfter(), MotifFor.DEPART_HS, dateRetourSiege.getOneDayBefore(), MotifFor.ARRIVEE_HS, MockPays.Allemagne);
		addForPrincipal(e, dateRetourSiege, MotifFor.ARRIVEE_HS, dateDissolutionEntreprise, MotifFor.INDETERMINE, MockCommune.Lausanne);
		addForSecondaire(e, dateDebutForSecondaire, MotifFor.ACHAT_IMMOBILIER, dateFinForSecondaire, MotifFor.VENTE_IMMOBILIER, MockCommune.Cossonay.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
		addBouclement(e, date(2014, 1, 1), DayMonth.get(3, 31), 12);        // bouclements tous les 31.03 depuis le 31.03.2014
		addBouclement(e, date(2014, 4, 1), DayMonth.get(6, 30), 3);         // bouclements tous les 3 mois depuis le 30.06.2014
		addBouclement(e, date(2014, 10, 1), DayMonth.get(9, 30), 12);       // bouclements tous les 30.09 depuis le 30.09.2015

		final List<PeriodeImposition> periodes = determine(e);
		Assert.assertNotNull(periodes);
		Assert.assertEquals(5, periodes.size());
		assertPeriodeImpositionPersonnesMorales(dateCreationEntreprise, date(2014, 3, 31), false, TypeContribuable.VAUDOIS_ORDINAIRE, false, false, false, periodes.get(0));
		assertPeriodeImpositionPersonnesMorales(date(2014, 4, 1), dateDepartSiege, false, TypeContribuable.VAUDOIS_ORDINAIRE, false, false, false, periodes.get(1));
		assertPeriodeImpositionPersonnesMorales(dateDepartSiege.getOneDayAfter(), date(2014, 6, 30), false, TypeContribuable.HORS_SUISSE, false, false, false, periodes.get(2));
		assertPeriodeImpositionPersonnesMorales(date(2014, 7, 1), dateFinForSecondaire, false, TypeContribuable.HORS_SUISSE, false, false, true, periodes.get(3));
		assertPeriodeImpositionPersonnesMorales(dateRetourSiege, dateDissolutionEntreprise, false, TypeContribuable.VAUDOIS_ORDINAIRE, false, false, false, periodes.get(4));
	}
}
