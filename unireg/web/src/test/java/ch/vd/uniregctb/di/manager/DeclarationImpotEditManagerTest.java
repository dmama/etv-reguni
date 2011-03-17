package ch.vd.uniregctb.di.manager;

import java.util.List;

import org.junit.Test;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper.Range;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.validation.ValidationException;
import ch.vd.uniregctb.common.WebTest;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaireDAO;
import ch.vd.uniregctb.declaration.ModeleDocument;
import ch.vd.uniregctb.declaration.PeriodeFiscale;
import ch.vd.uniregctb.interfaces.model.mock.MockCollectiviteAdministrative;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.interfaces.model.mock.MockPays;
import ch.vd.uniregctb.metier.assujettissement.PeriodeImposition;
import ch.vd.uniregctb.parametrage.ParametreAppService;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypeContribuable;
import ch.vd.uniregctb.type.TypeDocument;
import ch.vd.uniregctb.validation.ValidationService;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * La classe ci-dessous permet de tester le manager web de gestion des déclaration d'impôt.
 * <p>
 * Le(s) bug(s) suivant(s) sont spécifiquement testés:
 * <ul>
 * <li>[UNIREG-832] Impossible de créer une DI on line pour un contribuable HS</li>
 * </ul>
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class DeclarationImpotEditManagerTest extends WebTest {

	protected DeclarationImpotOrdinaireDAO diDAO;
	private ParametreAppService parametres;
	protected DeclarationImpotEditManagerImpl manager;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		diDAO = getBean(DeclarationImpotOrdinaireDAO.class, "diDAO");
		parametres = getBean(ParametreAppService.class, "parametreAppService");
		manager = new DeclarationImpotEditManagerImpl();
		manager.setDiDAO(diDAO);
		manager.setParametres(parametres);
		manager.setValidationService(getBean(ValidationService.class, "validationService"));
	}

	@Test
	public void testCheckRangeDiContribuableNonAssujetti() {

		// le contribuable n'est pas assujetti, il ne doit pas être possible d'ajouter une DI
		final PersonnePhysique paul = addNonHabitant("Paul", "Duruz", date(1977, 3, 15), Sexe.MASCULIN);
		assertInValidRangeDi(paul, fullYear(1980));
		assertInValidRangeDi(paul, fullYear(2000));
		assertInValidRangeDi(paul, fullYear(2040));
	}

	private static DateRange fullYear(int year) {
		return new Range(date(year, 1, 1), date(year, 12, 31));
	}

	public void assertValidRangeDi(Contribuable ctb, DateRange range) {
		manager.checkRangeDi(ctb, range);
	}

	public void assertInValidRangeDi(Contribuable ctb, DateRange range) {
		try {
			manager.checkRangeDi(ctb, range);
			fail();
		}
		catch (ValidationException e) {
			// ok
		}
	}

	@Test
	public void testCheckRangeDiContribuableAssujettiEnContinu() {

		addCollAdm(MockCollectiviteAdministrative.CEDI);

		final PeriodeFiscale periode2000 = addPeriodeFiscale(2000);
		final PeriodeFiscale periode2040 = addPeriodeFiscale(2040);
		final ModeleDocument modele2000 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode2000);
		final ModeleDocument modele2040 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode2040);

		// le contribuable est assujetti depuis 1995, il doit être possible d'ajouter une DI et une seule pour chaque année
		PersonnePhysique paul = addNonHabitant("Paul", "Duruz", date(1977, 3, 15), Sexe.MASCULIN);
		addForPrincipal(paul, date(1995, 3, 15), MotifFor.MAJORITE, MockCommune.Lausanne);

		assertInValidRangeDi(paul, fullYear(1980));

		assertValidRangeDi(paul, fullYear(2000));
		addDeclarationImpot(paul, periode2000, date(2000, 1, 1), date(2000, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2000);
		assertInValidRangeDi(paul, fullYear(2000)); // la déclaration existe maintenant !

		assertValidRangeDi(paul, fullYear(2040));
		addDeclarationImpot(paul, periode2040, date(2040, 1, 1), date(2040, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2040);
		assertInValidRangeDi(paul, fullYear(2040)); // la déclaration existe maintenant !
	}

	@Test
	public void testCheckRangeDiContribuableAvecFinAssujettissement() {

		addCollAdm(MockCollectiviteAdministrative.CEDI);

		final PeriodeFiscale periode2007 = addPeriodeFiscale(2007);
		final PeriodeFiscale periode2008 = addPeriodeFiscale(2008);
		final ModeleDocument modele2007 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode2007);
		final ModeleDocument modele2008 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode2008);

		// le contribuable est assujetti depuis 1995 et il part au milieu de l'année 2008 : il doit être possible d'ajouter une DI et une
		// seule pour chaque année
		PersonnePhysique paul = addNonHabitant("Paul", "Duruz", date(1977, 3, 15), Sexe.MASCULIN);
		addForPrincipal(paul, date(1995, 3, 15), MotifFor.MAJORITE, date(2008, 6, 30), MotifFor.DEPART_HS, MockCommune.Lausanne);

		// pas encore assujetti
		assertInValidRangeDi(paul, fullYear(1980));

		// assujetti sur toute l'année
		assertValidRangeDi(paul, fullYear(2007));
		addDeclarationImpot(paul, periode2007, date(2007, 1, 1), date(2007, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2007);
		assertInValidRangeDi(paul, fullYear(2007)); // la déclaration existe maintenant !

		// assujetti sur la première partie de l'année
		assertValidRangeDi(paul, new Range(date(2008, 1, 1), date(2008, 6, 30)));
		addDeclarationImpot(paul, periode2008, date(2008, 1, 1), date(2008, 6, 30), TypeContribuable.VAUDOIS_ORDINAIRE, modele2008);
		assertInValidRangeDi(paul, new Range(date(2008, 1, 1), date(2008, 6, 30))); // la déclaration existe maintenant !

		// plus assujetti
		assertInValidRangeDi(paul, fullYear(2009));
	}

	@Test
	public void testCheckRangeDiContribuableAvecDebutAssujettissement() {

		addCollAdm(MockCollectiviteAdministrative.CEDI);

		final PeriodeFiscale periode2008 = addPeriodeFiscale(2008);
		final PeriodeFiscale periode2009 = addPeriodeFiscale(2009);
		final ModeleDocument modele2008 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode2008);
		final ModeleDocument modele2009 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode2009);

		// le contribuable arrive au milieu de l'année 2008
		PersonnePhysique paul = addNonHabitant("Paul", "Duruz", date(1977, 3, 15), Sexe.MASCULIN);
		addForPrincipal(paul, date(2008, 7, 1), MotifFor.ARRIVEE_HS, MockCommune.Lausanne);

		// pas encore assujetti
		assertInValidRangeDi(paul, fullYear(2007));

		// assujetti sur la seconde moitié de l'année
		assertValidRangeDi(paul, new Range(date(2008, 7, 1), date(2008, 12, 31)));
		addDeclarationImpot(paul, periode2008, date(2008, 7, 1), date(2008, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2008);
		assertInValidRangeDi(paul, new Range(date(2008, 7, 1), date(2008, 12, 31))); // la déclaration existe maintenant !

		// assujetti sur toute l'année
		assertValidRangeDi(paul, fullYear(2009));
		addDeclarationImpot(paul, periode2009, date(2009, 1, 1), date(2009, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2009);
		assertInValidRangeDi(paul, fullYear(2009)); // la déclaration existe maintenant !
	}

	@Test
	public void testCheckRangeDiContribuableAvecDepartHSEtRetourDansLAnnee() {

		addCollAdm(MockCollectiviteAdministrative.CEDI);

		final PeriodeFiscale periode2008 = addPeriodeFiscale(2008);
		final ModeleDocument modele2008 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode2008);

		// le contribuable part hors-Suisse au début de l'année, et revient dans la même année
		PersonnePhysique paul = addNonHabitant("Paul", "Duruz", date(1977, 3, 15), Sexe.MASCULIN);
		addForPrincipal(paul, date(1995, 3, 15), MotifFor.MAJORITE, date(2008, 2, 10), MotifFor.DEPART_HS, MockCommune.Lausanne);
		addForPrincipal(paul, date(2008, 9, 1), MotifFor.ARRIVEE_HS, MockCommune.Lausanne);

		// assujetti sur toute l'année 2007
		assertValidRangeDi(paul, fullYear(2007));

		// assujetti deux fois sur l'année 2008
		assertValidRangeDi(paul, new Range(date(2008, 1, 1), date(2008, 2, 10))); // première déclaration
		addDeclarationImpot(paul, periode2008, date(2008, 1, 1), date(2008, 2, 10), TypeContribuable.VAUDOIS_ORDINAIRE, modele2008);
		assertValidRangeDi(paul, new Range(date(2008, 9, 1), date(2008, 12, 31))); // deuxième déclaration
		addDeclarationImpot(paul, periode2008, date(2008, 9, 1), date(2008, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2008);
		assertInValidRangeDi(paul, fullYear(2008)); // toutes les déclarations existent maintenant !

		// assujetti sur toute l'année 2009
		assertValidRangeDi(paul, fullYear(2009));
	}

	/**
	 * [UNIREG-1118] Vérifie que l'on fusionne les périodes qui provoqueraient des déclarations identiques contiguës.
	 */
	@Test
	public void testCheckRangeDiContribuableDepartHSAvecImmeuble() {

		final PeriodeFiscale periode2008 = addPeriodeFiscale(2008);
		addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode2008);

		// le contribuable part hors-Suisse au début de l'année, et garde un immeuble dans le canton
		PersonnePhysique paul = addNonHabitant("Paul", "Duruz", date(1977, 3, 15), Sexe.MASCULIN);
		addForPrincipal(paul, date(1995, 3, 15), MotifFor.MAJORITE, date(2008, 2, 10), MotifFor.DEPART_HS, MockCommune.Lausanne);
		addForPrincipal(paul, date(2008, 2, 11), MotifFor.DEPART_HS, MockPays.France);
		addForSecondaire(paul, date(1998, 1, 1), MotifFor.ACHAT_IMMOBILIER, MockCommune.Aubonne.getNoOFSEtendu(),
				MotifRattachement.IMMEUBLE_PRIVE);

		// assujetti sur toute l'année 2007
		assertValidRangeDi(paul, fullYear(2007));

		// assujetti une seule fois sur l'année 2008, malgré le départ hors-Suisse
		assertValidRangeDi(paul, fullYear(2008));

		// [UNIREG-889] assujetti sur l'année 2009 et DI autorisée de manière optionnelle (malgré le forfait)
		assertValidRangeDi(paul, fullYear(2009));
	}

	@Test
	public void testCalculateRangeProchaineDIContribuableNonAssujetti() {

		// le contribuable n'est pas assujetti, il ne doit pas être possible d'ajouter une DI
		final PersonnePhysique paul = addNonHabitant("Paul", "Duruz", date(1977, 3, 15), Sexe.MASCULIN);
		assertNull(manager.calculateRangesProchainesDIs(paul));
		assertNull(manager.calculateRangesProchainesDIs(paul));
	}

	@Test
	public void testCalculateRangeProchaineDIContribuableAssujettiEnContinu() {

		addCollAdm(MockCollectiviteAdministrative.CEDI);

		final PeriodeFiscale periode2003 = addPeriodeFiscale(2003);
		final ModeleDocument modele2003 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode2003);

		// le contribuable est assujetti depuis 1995, il doit être possible d'ajouter une DI et une seule pour chaque année
		PersonnePhysique paul = addNonHabitant("Paul", "Duruz", date(1977, 3, 15), Sexe.MASCULIN);
		addForPrincipal(paul, date(1995, 3, 15), MotifFor.MAJORITE, MockCommune.Lausanne);

		// [UNIREG-879] assujetti sur toute l'année 1995, mais on commence à parameters.getPremierePeriodeFiscale() -> 2003
		assertFullYearRangesSince(2003, false, manager.calculateRangesProchainesDIs(paul));
		addDeclarationImpot(paul, periode2003, date(2003, 1, 1), date(2003, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2003);

		// assujetti sur toute l'année 2004
		assertFullYearRangesSince(2004, false, manager.calculateRangesProchainesDIs(paul));

		// etc....
	}

	@Test
	public void testCalculateRangeProchaineDIContribuableDepartHSAvecImmeuble() {

		final PeriodeFiscale periode2008 = addPeriodeFiscale(2008);
		addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode2008);

		// le contribuable part hors-Suisse au début de l'année, et garde un immeuble dans le canton
		PersonnePhysique paul = addNonHabitant("Paul", "Duruz", date(1977, 3, 15), Sexe.MASCULIN);
		addForPrincipal(paul, date(2007, 3, 15), MotifFor.ARRIVEE_HC, date(2008, 2, 10), MotifFor.DEPART_HS, MockCommune.Lausanne);
		addForPrincipal(paul, date(2008, 2, 11), MotifFor.DEPART_HS, MockPays.France);
		addForSecondaire(paul, date(2007, 10, 1), MotifFor.ACHAT_IMMOBILIER, MockCommune.Aubonne.getNoOFSEtendu(),
				MotifRattachement.IMMEUBLE_PRIVE);

		final List<PeriodeImposition> ranges = manager.calculateRangesProchainesDIs(paul);
		assertNotNull(ranges);

		final PeriodeImposition r2007 = ranges.get(0);
		assertNotNull(r2007);
		assertPeriodeImposition(date(2007, 1, 1), date(2007, 12, 31), false, r2007);

		// [UNIREG-1118] assujetti sur toute l'année 2008, malgré le départ hors-suisse
		final PeriodeImposition r2008 = ranges.get(1);
		assertNotNull(r2008);
		assertPeriodeImposition(date(2008, 1, 1), date(2008, 12, 31), false, r2008);

		// [UNIREG-889] il n'y a - normalement - pas de déclaration pour la période 2009 parce que les HS avec immeuble sont au forfait,
		// mais cela n'empêche pas le contribuable de pouvoir demander une déclaration s'il le désire. Elles doivent donc exister de manière
		// optionnelle.
		assertFullYearRangesExists(2009, RegDate.get().year(), true, ranges);
	}

	/**
	 * [UNIREG-2051] Cas du contribuable hors-Canton qui vend son immeuble dans l'année : la dernière déclaration est remplacée par une note à l'administration fiscale de
	 * l'autre canton et il ne doit pas être possible d'envoyer une déclaration d'impôt.
	 */
	@Test
	public void testCalculateRangeProchaineDIContribuableHCVenteImmeuble() {

		final PeriodeFiscale periode2008 = addPeriodeFiscale(2008);
		addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode2008);

		// le contribuable part hors-Suisse au début de l'année, et garde un immeuble dans le canton
		PersonnePhysique paul = addNonHabitant("Paul", "Duruz", date(1977, 3, 15), Sexe.MASCULIN);
		addForPrincipal(paul, date(2007, 10, 1), MotifFor.ACHAT_IMMOBILIER, MockCommune.Neuchatel);
		addForSecondaire(paul, date(2007, 10, 1), MotifFor.ACHAT_IMMOBILIER, date(2009, 1, 15), MotifFor.VENTE_IMMOBILIER, MockCommune.Lausanne.getNoOFSEtendu(), MotifRattachement.IMMEUBLE_PRIVE);

		final List<PeriodeImposition> ranges = manager.calculateRangesProchainesDIs(paul);
		assertNotNull(ranges);
		assertEquals(2, ranges.size());

		final PeriodeImposition r2007 = ranges.get(0);
		assertNotNull(r2007);
		assertPeriodeImposition(date(2007, 1, 1), date(2007, 12, 31), false, r2007);

		final PeriodeImposition r2008 = ranges.get(1);
		assertNotNull(r2008);
		assertPeriodeImposition(date(2008, 1, 1), date(2008, 12, 31), false, r2008);

		// pas de déclaration pour 2009
	}

	/**
	 * Vérifie que la liste spécifiée contient tous les ranges complets (= année complète) pour la période <i>startYear..année courante</i>,
	 * <b>et uniquement ceux-ci</b>.
	 */
	public static void assertFullYearRangesSince(int startYear, boolean optionnel, List<PeriodeImposition> ranges) {
		assertFullYearRanges(startYear, RegDate.get().year(), optionnel, ranges);
	}

	/**
	 * Vérifie que la liste spécifiée contient tous les ranges complets (= année complète) pour la période <i>startYear..endYear</i>, <b>et
	 * uniquement ceux-ci</b>.
	 */
	public static void assertFullYearRanges(int startYear, int endYear, boolean optionnel, List<PeriodeImposition> ranges) {
		assertTrue(startYear <= endYear);
		assertEquals(endYear - startYear + 1, ranges.size());
		for (int i = startYear; i <= endYear; ++i) {
			assertPeriodeImposition(date(i, 1, 1), date(i, 12, 31), optionnel, ranges.get(i - startYear));
		}
	}

	/**
	 * Vérifie que la liste spécifiée contient tous les ranges complets (= année complète) pour la période <i>startYear..endYear</i>.
	 * <p>
	 * <b>Note:</b> d'autres ranges peuvent exister en dehors de la plage spécifiée.
	 */
	public static void assertFullYearRangesExists(int startYear, int endYear, boolean optionnel, List<PeriodeImposition> ranges) {
		assertTrue(startYear <= endYear);
		for (PeriodeImposition r : ranges) {
			final int year = r.getDateDebut().year();
			if (startYear <= year && year <= endYear) {
				assertPeriodeImposition(date(year, 1, 1), date(year, 12, 31), optionnel, r);
			}
		}
	}

	@Test
	public void testCalculateRangeProchaineDIContribuableDeclarationAnnulee() {

		addCollAdm(MockCollectiviteAdministrative.CEDI);

		final PeriodeFiscale periode2003 = addPeriodeFiscale(2003);
		final PeriodeFiscale periode2004 = addPeriodeFiscale(2004);
		final ModeleDocument modele2003 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode2003);
		final ModeleDocument modele2004 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode2004);

		// le contribuable est assujetti depuis 1995, il doit être possible d'ajouter une DI et une seule pour chaque année
		PersonnePhysique paul = addNonHabitant("Paul", "Duruz", date(1977, 3, 15), Sexe.MASCULIN);
		addForPrincipal(paul, date(1995, 3, 15), MotifFor.MAJORITE, MockCommune.Lausanne);

		// [UNIREG-879] assujetti sur toute l'année 1995, mais on commence à parameters.getPremierePeriodeFiscale() -> 2003
		assertFullYearRangesSince(2003, false, manager.calculateRangesProchainesDIs(paul));
		addDeclarationImpot(paul, periode2003, date(2003, 1, 1), date(2003, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2003);

		// assujetti sur toute l'année 2004
		assertFullYearRangesSince(2004, false, manager.calculateRangesProchainesDIs(paul));
		DeclarationImpotOrdinaire declaration = addDeclarationImpot(paul, periode2004, date(2004, 1, 1), date(2004, 12, 31),
				TypeContribuable.VAUDOIS_ORDINAIRE, modele2004);

		// prochaine déclaration est celle de 2005
		assertFullYearRangesSince(2005, false, manager.calculateRangesProchainesDIs(paul));

		// on annule la déclaration 2004 => la prochaine déclaration est celle de nouveau celle de 2004
		declaration.setAnnule(true);
		assertFullYearRangesSince(2004, false, manager.calculateRangesProchainesDIs(paul));
	}

	@Test
	public void testCalculateRangeProchaineDIContribuableDansLeFutur() {

		addCollAdm(MockCollectiviteAdministrative.CEDI);

		final int anneeCourante = RegDate.get().year();

		final PeriodeFiscale periode = addPeriodeFiscale(anneeCourante);
		final ModeleDocument modele = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode);

		// le contribuable est assujetti depuis l'année courante, il ne doit pas être possible d'ajouter une DI dans le futur
		PersonnePhysique paul = addNonHabitant("Paul", "Duruz", date(1977, 3, 15), Sexe.MASCULIN);
		addForPrincipal(paul, date(anneeCourante, 1, 11), MotifFor.ARRIVEE_HC, MockCommune.Lausanne);

		// assujetti sur toute l'année courante
		assertFullYearRangesSince(anneeCourante, false, manager.calculateRangesProchainesDIs(paul));
		addDeclarationImpot(paul, periode, date(anneeCourante, 1, 1), date(anneeCourante, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE,
				modele);

		// impossible d'ajouter une DI dans le future
		assertEmpty(manager.calculateRangesProchainesDIs(paul));
	}

	@Test
	public void testCalculateRangeProchaineDIContribuableAvecFinAssujettissement() {

		addCollAdm(MockCollectiviteAdministrative.CEDI);

		final PeriodeFiscale periode2003 = addPeriodeFiscale(2003);
		final PeriodeFiscale periode2004 = addPeriodeFiscale(2004);
		final ModeleDocument modele2003 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode2003);
		final ModeleDocument modele2004 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode2004);

		// le contribuable est assujetti depuis 2003 et il part au milieu de l'année 2004 : il doit être possible d'ajouter une DI sur 2003
		// et une autre sur 2004.
		PersonnePhysique paul = addNonHabitant("Paul", "Duruz", date(1977, 3, 15), Sexe.MASCULIN);
		addForPrincipal(paul, date(2003, 3, 15), MotifFor.MAJORITE, date(2004, 6, 30), MotifFor.DEPART_HS, MockCommune.Lausanne);

		// assujetti sur toute l'année 2003
		List<PeriodeImposition> ranges = manager.calculateRangesProchainesDIs(paul);
		assertEquals(2, ranges.size());
		assertPeriodeImposition(date(2003, 1, 1), date(2003, 12, 31), false, ranges.get(0));
		assertPeriodeImposition(date(2004, 1, 1), date(2004, 6, 30), false, ranges.get(1));
		addDeclarationImpot(paul, periode2003, date(2003, 1, 1), date(2003, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2003);

		// assujetti sur le début de l'année 2004
		ranges = manager.calculateRangesProchainesDIs(paul);
		assertEquals(1, ranges.size());
		assertPeriodeImposition(date(2004, 1, 1), date(2004, 6, 30), false, ranges.get(0));
		addDeclarationImpot(paul, periode2004, date(2004, 1, 1), date(2004, 6, 30), TypeContribuable.VAUDOIS_ORDINAIRE, modele2004);

		// plus assujetti
		assertEmpty(manager.calculateRangesProchainesDIs(paul));
	}

	@Test
	public void testCalculateRangeProchaineDIContribuableAvecDebutAssujettissement() {

		addCollAdm(MockCollectiviteAdministrative.CEDI);

		final PeriodeFiscale periode2003 = addPeriodeFiscale(2003);
		final PeriodeFiscale periode2004 = addPeriodeFiscale(2004);
		final ModeleDocument modele2003 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode2003);
		final ModeleDocument modele2004 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode2004);

		// le contribuable arrive au milieu de l'année 2003
		PersonnePhysique paul = addNonHabitant("Paul", "Duruz", date(1977, 3, 15), Sexe.MASCULIN);
		addForPrincipal(paul, date(2003, 7, 1), MotifFor.ARRIVEE_HS, MockCommune.Lausanne);

		// assujetti sur la fin de l'année 2003
		final List<PeriodeImposition> ranges = manager.calculateRangesProchainesDIs(paul);
		assertPeriodeImposition(date(2003, 7, 1), date(2003, 12, 31), false, ranges.get(0));
		addDeclarationImpot(paul, periode2003, date(2003, 7, 1), date(2003, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2003);

		// assujetti sur toute l'année 2004
		assertFullYearRangesSince(2004, false, manager.calculateRangesProchainesDIs(paul));
		addDeclarationImpot(paul, periode2004, date(2004, 1, 1), date(2004, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2004);

		// assujetti sur toute l'année 2005
		assertFullYearRangesSince(2005, false, manager.calculateRangesProchainesDIs(paul));

		// etc...
	}

	@Test
	public void testCalculateRangeProchaineDIContribuableAvecDepartHSEtRetourDansLAnnee() {

		addCollAdm(MockCollectiviteAdministrative.CEDI);
		
		final PeriodeFiscale periode2003 = addPeriodeFiscale(2003);
		final PeriodeFiscale periode2004 = addPeriodeFiscale(2004);
		final PeriodeFiscale periode2005 = addPeriodeFiscale(2005);
		final ModeleDocument modele2003 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode2003);
		final ModeleDocument modele2004 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode2004);
		final ModeleDocument modele2005 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode2005);

		// le contribuable part hors-Suisse au début de l'année, et revient dans la même année
		PersonnePhysique paul = addNonHabitant("Paul", "Duruz", date(1977, 3, 15), Sexe.MASCULIN);
		addForPrincipal(paul, date(2003, 3, 15), MotifFor.MAJORITE, date(2004, 2, 10), MotifFor.DEPART_HS, MockCommune.Lausanne);
		addForPrincipal(paul, date(2004, 9, 1), MotifFor.ARRIVEE_HS, MockCommune.Lausanne);

		// assujetti sur toute l'année 2003
		List<PeriodeImposition> ranges = manager.calculateRangesProchainesDIs(paul);
		assertPeriodeImposition(date(2003, 1, 1), date(2003, 12, 31), false, ranges.get(0));
		addDeclarationImpot(paul, periode2003, date(2003, 7, 1), date(2003, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2003);

		// assujetti deux fois sur l'année 2004
		ranges = manager.calculateRangesProchainesDIs(paul);
		assertPeriodeImposition(date(2004, 1, 1), date(2004, 2, 10), false, ranges.get(0));
		assertPeriodeImposition(date(2004, 9, 1), date(2004, 12, 31), false, ranges.get(1));
		addDeclarationImpot(paul, periode2004, date(2004, 1, 1), date(2004, 2, 10), TypeContribuable.VAUDOIS_ORDINAIRE, modele2004);
		addDeclarationImpot(paul, periode2004, date(2004, 9, 1), date(2004, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2004);

		// assujetti sur toute l'année 2005
		assertFullYearRangesSince(2005, false, manager.calculateRangesProchainesDIs(paul));
		addDeclarationImpot(paul, periode2005, date(2005, 1, 1), date(2005, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2005);

		// assujetti sur toute l'année 2006
		assertFullYearRangesSince(2006, false, manager.calculateRangesProchainesDIs(paul));

		// etc...
	}

	private static void assertPeriodeImposition(RegDate debut, RegDate fin, boolean optionnel, PeriodeImposition range) {
		assertNotNull(range);
		assertEquals(debut, range.getDateDebut());
		assertEquals(fin, range.getDateFin());
		assertEquals(optionnel, range.isOptionnelle());
	}
}
