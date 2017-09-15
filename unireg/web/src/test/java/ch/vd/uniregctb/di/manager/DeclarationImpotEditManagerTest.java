package ch.vd.uniregctb.di.manager;

import java.util.List;

import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper.Range;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.validation.ValidationException;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureRaw;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.uniregctb.common.WebTest;
import ch.vd.uniregctb.declaration.Declaration;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaireDAO;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinairePP;
import ch.vd.uniregctb.declaration.EtatDeclaration;
import ch.vd.uniregctb.declaration.EtatDeclarationRetournee;
import ch.vd.uniregctb.declaration.ModeleDocument;
import ch.vd.uniregctb.declaration.ModeleDocumentDAO;
import ch.vd.uniregctb.declaration.PeriodeFiscale;
import ch.vd.uniregctb.declaration.PeriodeFiscaleDAO;
import ch.vd.uniregctb.declaration.ordinaire.DeclarationImpotService;
import ch.vd.uniregctb.evenement.fiscal.MockEvenementFiscalService;
import ch.vd.uniregctb.jms.BamMessageSender;
import ch.vd.uniregctb.metier.assujettissement.PeriodeImposition;
import ch.vd.uniregctb.metier.assujettissement.PeriodeImpositionService;
import ch.vd.uniregctb.parametrage.ParametreAppService;
import ch.vd.uniregctb.tiers.CollectiviteAdministrative;
import ch.vd.uniregctb.tiers.ContribuableImpositionPersonnesPhysiques;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.TacheDAO;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypeAdresseRetour;
import ch.vd.uniregctb.type.TypeContribuable;
import ch.vd.uniregctb.type.TypeDocument;
import ch.vd.uniregctb.type.TypeEtatDocumentFiscal;
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
 * <li>[SIFISC-8094] Impossible d'émettre une DI pour un CTB mixte 2 qui part HC (DI optionnelle)</li>
 * </ul>
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class DeclarationImpotEditManagerTest extends WebTest {

	protected DeclarationImpotOrdinaireDAO diDAO;
	protected DeclarationImpotEditManagerImpl manager;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		diDAO = getBean(DeclarationImpotOrdinaireDAO.class, "diDAO");
		manager = new DeclarationImpotEditManagerImpl();
		manager.setDiDAO(diDAO);
		manager.setTiersDAO(tiersDAO);
		manager.setEvenementFiscalService(new MockEvenementFiscalService());
		manager.setParametres(getBean(ParametreAppService.class, "parametreAppService"));
		manager.setValidationService(getBean(ValidationService.class, "validationService"));
		manager.setBamMessageSender(getBean(BamMessageSender.class, "bamMessageSender"));
		manager.setTiersService(tiersService);
		manager.setPeriodeFiscaleDAO(getBean(PeriodeFiscaleDAO.class, "periodeFiscaleDAO"));
		manager.setModeleDocumentDAO(getBean(ModeleDocumentDAO.class, "modeleDocumentDAO"));
		manager.setTacheDAO(getBean(TacheDAO.class, "tacheDAO"));
		manager.setPeriodeImpositionService(getBean(PeriodeImpositionService.class, "periodeImpositionService"));
		manager.setDiService(getBean(DeclarationImpotService.class, "diService"));
		manager.afterPropertiesSet();
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
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

	public void assertValidRangeDi(ContribuableImpositionPersonnesPhysiques ctb, DateRange range) {
		manager.checkRangeDi(ctb, range);
	}

	public void assertInValidRangeDi(ContribuableImpositionPersonnesPhysiques ctb, DateRange range) {
		try {
			manager.checkRangeDi(ctb, range);
			fail();
		}
		catch (ValidationException e) {
			// ok
		}
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testCheckRangeDiContribuableAssujettiEnContinu() {

		final PeriodeFiscale periode2003 = addPeriodeFiscale(2003);
		final PeriodeFiscale periode2014 = addPeriodeFiscale(2014);
		final ModeleDocument modele2003 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode2003);
		final ModeleDocument modele2014 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode2014);

		// le contribuable est assujetti depuis 1995, il doit être possible d'ajouter une DI et une seule pour chaque année
		PersonnePhysique paul = addNonHabitant("Paul", "Duruz", date(1977, 3, 15), Sexe.MASCULIN);
		addForPrincipal(paul, date(1995, 3, 15), MotifFor.MAJORITE, MockCommune.Lausanne);

		assertInValidRangeDi(paul, fullYear(1980));

		assertValidRangeDi(paul, fullYear(2003));
		addDeclarationImpot(paul, periode2003, date(2003, 1, 1), date(2003, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2003);
		assertInValidRangeDi(paul, fullYear(2003)); // la déclaration existe maintenant !

		assertValidRangeDi(paul, fullYear(2014));
		addDeclarationImpot(paul, periode2014, date(2014, 1, 1), date(2014, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2014);
		assertInValidRangeDi(paul, fullYear(2014)); // la déclaration existe maintenant !
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testCheckRangeDiContribuableAvecFinAssujettissement() {

		final PeriodeFiscale periode2007 = addPeriodeFiscale(2007);
		final PeriodeFiscale periode2008 = addPeriodeFiscale(2008);
		final ModeleDocument modele2007 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode2007);
		final ModeleDocument modele2008 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode2008);

		// le contribuable est assujetti depuis 1995 et il part au milieu de l'année 2008 : il doit être possible d'ajouter une DI et une
		// seule pour chaque année
		PersonnePhysique paul = addNonHabitant("Paul", "Duruz", date(1977, 3, 15), Sexe.MASCULIN);
		addForPrincipal(paul, date(2004, 3, 15), MotifFor.MAJORITE, date(2008, 6, 30), MotifFor.DEPART_HS, MockCommune.Lausanne);

		// pas encore assujetti
		assertInValidRangeDi(paul, fullYear(2003));

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
	@Transactional(rollbackFor = Throwable.class)
	public void testCheckRangeDiContribuableAvecDebutAssujettissement() {

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
	@Transactional(rollbackFor = Throwable.class)
	public void testCheckRangeDiContribuableAvecDepartHSEtRetourDansLAnnee() {

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
	@Transactional(rollbackFor = Throwable.class)
	public void testCheckRangeDiContribuableDepartHSAvecImmeuble() {

		final PeriodeFiscale periode2008 = addPeriodeFiscale(2008);
		addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode2008);

		// le contribuable part hors-Suisse au début de l'année, et garde un immeuble dans le canton
		PersonnePhysique paul = addNonHabitant("Paul", "Duruz", date(1977, 3, 15), Sexe.MASCULIN);
		addForPrincipal(paul, date(1995, 3, 15), MotifFor.MAJORITE, date(2008, 2, 10), MotifFor.DEPART_HS, MockCommune.Lausanne);
		addForPrincipal(paul, date(2008, 2, 11), MotifFor.DEPART_HS, MockPays.France);
		addForSecondaire(paul, date(1998, 1, 1), MotifFor.ACHAT_IMMOBILIER, MockCommune.Aubonne.getNoOFS(),
				MotifRattachement.IMMEUBLE_PRIVE);

		// assujetti sur toute l'année 2007
		assertValidRangeDi(paul, fullYear(2007));

		// assujetti une seule fois sur l'année 2008, malgré le départ hors-Suisse
		assertValidRangeDi(paul, fullYear(2008));

		// [UNIREG-889] assujetti sur l'année 2009 et DI autorisée de manière optionnelle (malgré le forfait)
		assertValidRangeDi(paul, fullYear(2009));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testCalculateRangeProchaineDIContribuableNonAssujetti() {

		// le contribuable n'est pas assujetti, il ne doit pas être possible d'ajouter une DI
		final PersonnePhysique paul = addNonHabitant("Paul", "Duruz", date(1977, 3, 15), Sexe.MASCULIN);
		assertNull(manager.calculateRangesProchainesDIs(paul));
		assertNull(manager.calculateRangesProchainesDIs(paul));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testCalculateRangeProchaineDIContribuableAssujettiEnContinu() {

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
	@Transactional(rollbackFor = Throwable.class)
	public void testCalculateRangeProchaineDIContribuableDepartHSAvecImmeuble() {

		final PeriodeFiscale periode2008 = addPeriodeFiscale(2008);
		addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode2008);

		// le contribuable part hors-Suisse au début de l'année, et garde un immeuble dans le canton
		PersonnePhysique paul = addNonHabitant("Paul", "Duruz", date(1977, 3, 15), Sexe.MASCULIN);
		addForPrincipal(paul, date(2007, 3, 15), MotifFor.ARRIVEE_HC, date(2008, 2, 10), MotifFor.DEPART_HS, MockCommune.Lausanne);
		addForPrincipal(paul, date(2008, 2, 11), MotifFor.DEPART_HS, MockPays.France);
		addForSecondaire(paul, date(2007, 10, 1), MotifFor.ACHAT_IMMOBILIER, MockCommune.Aubonne.getNoOFS(),
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
	@Transactional(rollbackFor = Throwable.class)
	public void testCalculateRangeProchaineDIContribuableHCVenteImmeuble() {

		final PeriodeFiscale periode2008 = addPeriodeFiscale(2008);
		addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode2008);

		// le contribuable part hors-Suisse au début de l'année, et garde un immeuble dans le canton
		PersonnePhysique paul = addNonHabitant("Paul", "Duruz", date(1977, 3, 15), Sexe.MASCULIN);
		addForPrincipal(paul, date(2007, 10, 1), MotifFor.ACHAT_IMMOBILIER, MockCommune.Neuchatel);
		addForSecondaire(paul, date(2007, 10, 1), MotifFor.ACHAT_IMMOBILIER, date(2009, 1, 15), MotifFor.VENTE_IMMOBILIER, MockCommune.Lausanne.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);

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
	@Transactional(rollbackFor = Throwable.class)
	public void testCalculateRangeProchaineDIContribuableDeclarationAnnulee() {

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
	@Transactional(rollbackFor = Throwable.class)
	public void testCalculateRangeProchaineDIContribuableDansLeFutur() {

		final int anneeCourante = RegDate.get().year();

		final PeriodeFiscale periode = addPeriodeFiscale(anneeCourante);
		final ModeleDocument modele = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode);

		// le contribuable est assujetti depuis l'année courante, il ne doit pas être possible d'ajouter une DI dans le futur
		PersonnePhysique paul = addNonHabitant("Paul", "Duruz", date(1977, 3, 15), Sexe.MASCULIN);
		addForPrincipal(paul, date(anneeCourante, 1, 2), MotifFor.ARRIVEE_HC, MockCommune.Lausanne);

		// assujetti sur toute l'année courante
		assertFullYearRangesSince(anneeCourante, false, manager.calculateRangesProchainesDIs(paul));
		addDeclarationImpot(paul, periode, date(anneeCourante, 1, 1), date(anneeCourante, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE,
				modele);

		// impossible d'ajouter une DI dans le future
		assertEmpty(manager.calculateRangesProchainesDIs(paul));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testCalculateRangeProchaineDIContribuableAvecFinAssujettissement() {

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
	@Transactional(rollbackFor = Throwable.class)
	public void testCalculateRangeProchaineDIContribuableAvecDebutAssujettissement() {

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
	@Transactional(rollbackFor = Throwable.class)
	public void testCalculateRangeProchaineDIContribuableAvecDepartHSEtRetourDansLAnnee() {

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
		assertEquals(optionnel, range.isDeclarationOptionnelle());
	}

	@Test
	public void testQuittanceDI() throws Exception {

		class Ids {
			long pp;
			long di;
		}
		final Ids ids = new Ids();

		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final CollectiviteAdministrative cedi = tiersService.getCollectiviteAdministrative(ServiceInfrastructureRaw.noCEDI);
				final PersonnePhysique pp = addNonHabitant("Arnold", "Stäpäld", date(1978, 9, 23), Sexe.MASCULIN);
				final PeriodeFiscale periode = addPeriodeFiscale(2010);
				final ModeleDocument modele = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode);
				final DeclarationImpotOrdinaire di = addDeclarationImpot(pp, periode, date(2010, 1, 1), date(2010, 12, 31), cedi, TypeContribuable.VAUDOIS_ORDINAIRE, modele);

				ids.pp = pp.getId();
				ids.di = di.getId();
				return null;
			}
		});

		// Quittancement de la DI par l'interface web
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				manager.quittancerDI(ids.di, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, date(2011, 4, 12));
				return null;
			}
		});

		// On vérifie que la source du quittancement est bien "WEB"
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final DeclarationImpotOrdinaire di = hibernateTemplate.get(DeclarationImpotOrdinaire.class, ids.di);
				assertNotNull(di);

				assertEquals(date(2011, 4, 12), di.getDateRetour());

				final EtatDeclaration dernier = di.getDernierEtatDeclaration();
				assertNotNull(dernier);
				assertInstanceOf(EtatDeclarationRetournee.class, dernier);

				final EtatDeclarationRetournee retour = (EtatDeclarationRetournee) dernier;
				assertEquals(date(2011, 4, 12), retour.getDateObtention());
				assertEquals("WEB", retour.getSource());
				return null;
			}
		});
	}

	@Test
	public void testCodeSegmentOnNewDi() throws Exception {

		final int anneeCourante = RegDate.get().year();
		final int anneeDerniere = anneeCourante - 1;
		final RegDate debutAnneeDerniere = date(anneeDerniere, 1, 1);
		final RegDate debutAnneeCourante = date(anneeCourante, 1, 1);
		final RegDate finAnneeDerniere = debutAnneeCourante.getOneDayBefore();
		final RegDate finAnneeCourante = date(anneeCourante, 12, 31);

		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Arnold", "Duchemin", date(1970, 4, 12), Sexe.MASCULIN);
				addForPrincipal(pp, debutAnneeDerniere, MotifFor.ARRIVEE_HS, MockCommune.Lausanne);

				final CollectiviteAdministrative cedi = tiersService.getCollectiviteAdministrative(ServiceInfrastructureRaw.noCEDI);
				final PeriodeFiscale pfAnneeDerniere = addPeriodeFiscale(anneeDerniere);
				final PeriodeFiscale pfAnneeCourante = addPeriodeFiscale(anneeCourante);
				final ModeleDocument modeleAnneeDerniere = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, pfAnneeDerniere);
				final DeclarationImpotOrdinairePP di = addDeclarationImpot(pp, pfAnneeDerniere, debutAnneeDerniere, finAnneeDerniere, cedi, TypeContribuable.VAUDOIS_ORDINAIRE, modeleAnneeDerniere);
				di.setCodeSegment(6);

				// pour la DI que l'on créera à la main plus bas
				addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_LOCAL, pfAnneeCourante);

				return pp.getNumero();
			}
		});

		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				manager.envoieImpressionLocaleDI(ppId, debutAnneeCourante, finAnneeCourante, TypeDocument.DECLARATION_IMPOT_COMPLETE_LOCAL, TypeAdresseRetour.CEDI, RegDate.get(), null);
				return null;
			}
		});

		// le code segment doit avoir été transmis à la nouvelle DI
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				final DeclarationImpotOrdinairePP di = pp.getDeclarationActiveAt(finAnneeCourante);
				assertEquals(Integer.valueOf(6), di.getCodeSegment());
				return null;
			}
		});
	}

	/**
	 * [SIFISC-4923] Ce test vérifie qu'il est possible d'émettre une déclaration d'impôt sur un contribuable hors-canton qui a vendu son dernier immeuble dans l'année.
	 * <p/>
	 * La difficulté tient au fait que les assujettissements économiques s'étendent toujours sur toute l'année et qu'on s'intéresse donc au for de gestion au 31 décembre. Comme à cette date-là, le
	 * contribuable ne possède plus de for fiscal actif dans le canton, il n'y a plus de for de gestion actif. La solution est donc de rechercher le dernier for de gestion connu.
	 *
	 * @throws Exception
	 */
	@Test
	public void testCreerNouvelleDIHorsCantonApresVenteDansLAnnee() throws Exception {

		class Ids {
			long ppId;
			long oidCedi;
		}
		final Ids ids = new Ids();

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				final PeriodeFiscale periode2009 = addPeriodeFiscale(2009);
				addModeleDocument(TypeDocument.DECLARATION_IMPOT_HC_IMMEUBLE, periode2009);

				final PersonnePhysique pp = addNonHabitant("Annette", "Lebeurre", date(1978, 12, 3), Sexe.FEMININ);
				addForPrincipal(pp, date(2006, 12, 20), MotifFor.ACHAT_IMMOBILIER, MockCommune.Zurich);
				addForSecondaire(pp, date(2006, 12, 20), MotifFor.ACHAT_IMMOBILIER, date(2009, 12, 1), MotifFor.VENTE_IMMOBILIER, MockCommune.Cully.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
				ids.ppId = pp.getNumero();

				final CollectiviteAdministrative cedi = tiersService.getCollectiviteAdministrative(ServiceInfrastructureRaw.noCEDI);
				ids.oidCedi = cedi.getId();

				return null;
			}
		});

		final RegDate delaiAccorde = RegDate.get().addMonths(2);
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = hibernateTemplate.get(PersonnePhysique.class, ids.ppId);
				// cet appel levait une exception parce que le for de gestion au 31 décembre 2009 n'était pas connu avant la correction du cas SIFISC-4923
				manager.creerNouvelleDI(pp, date(2009, 1, 1), date(2009, 12, 31), TypeDocument.DECLARATION_IMPOT_HC_IMMEUBLE, TypeAdresseRetour.CEDI, delaiAccorde, null);
				return null;
			}
		});

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = hibernateTemplate.get(PersonnePhysique.class, ids.ppId);
				final List<Declaration> decls = pp.getDeclarationsDansPeriode(Declaration.class, 2009, false);
				assertNotNull(decls);
				assertEquals(1, decls.size());
				assertDIPP(date(2009, 1, 1), date(2009, 12, 31), TypeEtatDocumentFiscal.EMISE, TypeContribuable.HORS_CANTON, TypeDocument.DECLARATION_IMPOT_HC_IMMEUBLE, ids.oidCedi, delaiAccorde, decls.get(0));
				return null;
			}
		});
	}

	/**
	 * SIFISC-8094
	 */
	@Test
	public void testCalculateRangeDIMixte2DepartHC() throws Exception {

		final int pf = RegDate.get().year() - 1;
		final RegDate dateArrivee = date(pf, 1, 1);
		final RegDate dateDepart = date(pf, 10, 31);

		final long ppId = doInNewTransaction(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Pierre", "Ponce", null, Sexe.MASCULIN);
				addForPrincipal(pp, dateArrivee, MotifFor.ARRIVEE_HS, dateDepart, MotifFor.DEPART_HC, MockCommune.Bussigny, ModeImposition.MIXTE_137_2);
				addForPrincipal(pp, dateDepart.getOneDayAfter(), MotifFor.DEPART_HC, MockCommune.Bale, ModeImposition.SOURCE);
				return pp.getNumero();
			}
		});

		doInNewTransaction(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				final List<PeriodeImposition> pis = manager.calculateRangesProchainesDIs(pp);
				assertNotNull(pis);
				assertEquals(1, pis.size());

				final PeriodeImposition pi = pis.get(0);
				assertNotNull(pi);
				assertTrue(pi.isDeclarationRemplaceeParNote());
				assertTrue(pi.isDeclarationOptionnelle());
				return null;
			}
		});
	}
}
