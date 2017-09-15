package ch.vd.uniregctb.declaration.source;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.declaration.Declaration;
import ch.vd.uniregctb.declaration.DeclarationImpotSource;
import ch.vd.uniregctb.declaration.PeriodeFiscale;
import ch.vd.uniregctb.declaration.Periodicite;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.type.CategorieImpotSource;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.PeriodeDecompte;
import ch.vd.uniregctb.type.PeriodiciteDecompte;
import ch.vd.uniregctb.type.TypeEtatDocumentFiscal;

public class ListeRecapServiceTest extends BusinessTest {

	private ListeRecapService lrService = null;
	private TiersDAO tiersDAO;
	private static final String DB_UNIT_FILE = "ListeRecapServiceTest.xml";

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		lrService = getBean(ListeRecapService.class, "lrService");
		tiersDAO = getBean(TiersDAO.class, "tiersDAO");
	}

	/**
	 * Teste la methode findLRsManquantes
	 *
	 * @throws Exception
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testFindLRsManquantes() throws Exception {

		loadDatabase(DB_UNIT_FILE);

		final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersDAO.get(12500001L);
		final RegDate dateFinPeriode = RegDate.get(2010, 12, 31);
		final List<DateRange> lrTrouvees = new ArrayList<>();
		final List<DateRange> lrManquantes = lrService.findLRsManquantes(dpi, dateFinPeriode, lrTrouvees);
		Assert.assertEquals(16, lrManquantes.size());
		Assert.assertEquals(1, lrTrouvees.size());
		final DateRange firstRange = lrManquantes.get(0);
		Assert.assertEquals(RegDate.get(2007, 1, 1), firstRange.getDateDebut());
	}

	@Test
	public void testFindLRsManquantesWithDifferentesPeriodicites1() throws Exception {

		final long dpiId = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				DebiteurPrestationImposable dpi = addDebiteur();
				tiersService.addPeriodicite(dpi, PeriodiciteDecompte.UNIQUE, PeriodeDecompte.M01, date(2008, 1, 1), date(2008, 12, 31));
				tiersService.addPeriodicite(dpi, PeriodiciteDecompte.TRIMESTRIEL, null, date(2009, 1, 1), date(2009, 12, 31));
				tiersService.addPeriodicite(dpi, PeriodiciteDecompte.UNIQUE, PeriodeDecompte.M01, date(2010, 1, 1), date(2010, 12, 31));
				tiersService.addPeriodicite(dpi, PeriodiciteDecompte.MENSUEL, null, date(2011, 1, 1), null);
				addForDebiteur(dpi, date(2008, 1, 1), MotifFor.INDETERMINE, null, null, MockCommune.Bex);
				return dpi.getNumero();
			}
		});

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersDAO.get(dpiId);
				final RegDate dateFinPeriode = RegDate.get(2010, 12, 31);
				final List<DateRange> lrTrouvees = new ArrayList<>();
				final List<DateRange> lrManquantes = lrService.findLRsManquantes(dpi, dateFinPeriode, lrTrouvees);
				Assert.assertEquals(6, lrManquantes.size());
				final DateRange firstRange = lrManquantes.get(0);
				Assert.assertEquals(RegDate.get(2008, 1, 1), firstRange.getDateDebut());
				return null;
			}
		});
	}

	@Test
	public void testFindLRsManquantesWithDifferentesPeriodicites2() throws Exception {

		final long dpiId2 = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				DebiteurPrestationImposable dpi = addDebiteur();
				tiersService.addPeriodicite(dpi, PeriodiciteDecompte.UNIQUE, PeriodeDecompte.M02, date(2008, 1, 1), date(2008, 12, 31));
				tiersService.addPeriodicite(dpi, PeriodiciteDecompte.TRIMESTRIEL, null, date(2009, 1, 1), date(2009, 12, 31));
				tiersService.addPeriodicite(dpi, PeriodiciteDecompte.UNIQUE, PeriodeDecompte.S1, date(2010, 1, 1), date(2010, 12, 31));
				tiersService.addPeriodicite(dpi, PeriodiciteDecompte.MENSUEL, null, date(2011, 1, 1), null);
				addForDebiteur(dpi, date(2008, 1, 1), MotifFor.INDETERMINE, null, null, MockCommune.Bex);
				return dpi.getNumero();
			}
		});

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersDAO.get(dpiId2);
				final RegDate dateFinPeriode = RegDate.get(2010, 12, 31);
				final List<DateRange> lrTrouvees = new ArrayList<>();
				final List<DateRange> lrManquantes = lrService.findLRsManquantes(dpi, dateFinPeriode, lrTrouvees);
				Assert.assertEquals(6, lrManquantes.size());
				final DateRange firstRange = lrManquantes.get(0);
				Assert.assertEquals(RegDate.get(2008, 2, 1), firstRange.getDateDebut());
				return null;
			}
		});
	}

	@Test
	public void testFindLRsManquantesWithDifferentesPeriodicites3() throws Exception {

		final long dpiId3 = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				DebiteurPrestationImposable dpi = addDebiteur();
				tiersService.addPeriodicite(dpi, PeriodiciteDecompte.UNIQUE, PeriodeDecompte.M02, date(2008, 1, 1), date(2008, 12, 31));
				tiersService.addPeriodicite(dpi, PeriodiciteDecompte.TRIMESTRIEL, null, date(2009, 1, 1), date(2009, 12, 31));
				tiersService.addPeriodicite(dpi, PeriodiciteDecompte.UNIQUE, PeriodeDecompte.S1, date(2010, 1, 1), date(2010, 12, 31));
				tiersService.addPeriodicite(dpi, PeriodiciteDecompte.MENSUEL, null, date(2011, 1, 1), null);
				addForDebiteur(dpi, date(2008, 1, 1), MotifFor.INDETERMINE, null, null, MockCommune.Bex);
				final PeriodeFiscale periodeFiscale2008 = addPeriodeFiscale(2008);
				final PeriodeFiscale periodeFiscale2009 = addPeriodeFiscale(2009);

				addLRPeriodiciteUnique(dpi, date(2008, 2, 1), date(2008, 8, 28), periodeFiscale2008, TypeEtatDocumentFiscal.RETOURNEE);
				addLR(dpi, date(2009, 1, 1), PeriodiciteDecompte.TRIMESTRIEL, periodeFiscale2009, TypeEtatDocumentFiscal.RETOURNEE);
				addLR(dpi, date(2009, 4, 1), PeriodiciteDecompte.TRIMESTRIEL, periodeFiscale2009, TypeEtatDocumentFiscal.RETOURNEE);
				return dpi.getNumero();
			}
		});

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersDAO.get(dpiId3);
				final RegDate dateFinPeriode = RegDate.get(2010, 12, 31);
				final List<DateRange> lrTrouvees = new ArrayList<>();
				final List<DateRange> lrManquantes = lrService.findLRsManquantes(dpi, dateFinPeriode, lrTrouvees);
				Assert.assertEquals(3, lrManquantes.size());
				final DateRange firstRange = lrManquantes.get(0);
				Assert.assertEquals(RegDate.get(2009, 7, 1), firstRange.getDateDebut());
				return null;
			}
		});
	}

	@Test
	public void testFindLRsManquantesWithDifferentesPeriodicites4() throws Exception {

		final long dpiId4 = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				DebiteurPrestationImposable dpi = addDebiteur();
				tiersService.addPeriodicite(dpi, PeriodiciteDecompte.UNIQUE, PeriodeDecompte.M02, date(2008, 1, 1), date(2008, 12, 31));
				tiersService.addPeriodicite(dpi, PeriodiciteDecompte.ANNUEL, PeriodeDecompte.T1, date(2009, 1, 1), date(2009, 12, 31));
				tiersService.addPeriodicite(dpi, PeriodiciteDecompte.UNIQUE, PeriodeDecompte.S1, date(2010, 1, 1), date(2010, 12, 31));
				tiersService.addPeriodicite(dpi, PeriodiciteDecompte.ANNUEL, PeriodeDecompte.A, date(2011, 1, 1), null);
				addForDebiteur(dpi, date(2008, 1, 1), MotifFor.INDETERMINE, null, null, MockCommune.Bex);
				return dpi.getNumero();
			}
		});

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersDAO.get(dpiId4);
				final RegDate dateFinPeriode = RegDate.get(2011, 11, 23);
				final List<DateRange> lrTrouvees = new ArrayList<>();
				final List<DateRange> lrManquantes = lrService.findLRsManquantes(dpi, dateFinPeriode, lrTrouvees);
				Assert.assertEquals(4, lrManquantes.size());
				final DateRange firstRange = lrManquantes.get(0);
				Assert.assertEquals(RegDate.get(2008, 2, 1), firstRange.getDateDebut());
				return null;
			}
		});
	}

	@Test
	public void testFindLRsManquantesWithDifferentesPeriodicitesLaMemeAnnee() throws Exception {
		// mise en place
		final long dpiId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final DebiteurPrestationImposable dpi = addDebiteur();
				tiersService.addPeriodicite(dpi, PeriodiciteDecompte.SEMESTRIEL, null, date(2010, 1, 1), date(2010, 3, 31));
				tiersService.addPeriodicite(dpi, PeriodiciteDecompte.TRIMESTRIEL, null, date(2010, 4, 1), date(2010, 9, 30));
				tiersService.addPeriodicite(dpi, PeriodiciteDecompte.MENSUEL, null, date(2010, 10, 1), null);
				addForDebiteur(dpi, date(2010, 1, 1), MotifFor.INDETERMINE, null, null, MockCommune.Aubonne);
				return dpi.getNumero();
			}
		});

		// on vérifie que l'on a bien ce que l'on veut
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final DebiteurPrestationImposable dpi = tiersDAO.getDebiteurPrestationImposableByNumero(dpiId);
				Assert.assertNotNull(dpi);

				final List<Periodicite> periodicites = dpi.getPeriodicitesSorted();
				Assert.assertNotNull(periodicites);
				Assert.assertEquals(3, periodicites.size());
				{
					final Periodicite periodicite = periodicites.get(0);
					Assert.assertEquals(PeriodiciteDecompte.SEMESTRIEL, periodicite.getPeriodiciteDecompte());
					Assert.assertEquals(date(2010, 1, 1), periodicite.getDateDebut());
					Assert.assertEquals(date(2010, 3, 31), periodicite.getDateFin());
					Assert.assertFalse(periodicite.isAnnule());
				}
				{
					final Periodicite periodicite = periodicites.get(1);
					Assert.assertEquals(PeriodiciteDecompte.TRIMESTRIEL, periodicite.getPeriodiciteDecompte());
					Assert.assertEquals(date(2010, 4, 1), periodicite.getDateDebut());
					Assert.assertEquals(date(2010, 9, 30), periodicite.getDateFin());
					Assert.assertFalse(periodicite.isAnnule());
				}
				{
					final Periodicite periodicite = periodicites.get(2);
					Assert.assertEquals(PeriodiciteDecompte.MENSUEL, periodicite.getPeriodiciteDecompte());
					Assert.assertEquals(date(2010, 10, 1), periodicite.getDateDebut());
					Assert.assertNull(periodicite.getDateFin());
					Assert.assertFalse(periodicite.isAnnule());
				}

				// les LR manquantes -> toute l'année, répartie entre 5 LR (une semestrielle, une trimestrielle, et trois mensuelles)
				final List<DateRange> lrTrouveesOut = new ArrayList<>();
				final List<DateRange> lrManquantes = lrService.findLRsManquantes(dpi, date(2010, 12, 31), lrTrouveesOut);
				Assert.assertNotNull(lrManquantes);
				Assert.assertEquals(0, lrTrouveesOut.size());
				Assert.assertEquals(5, lrManquantes.size());
				{
					final DateRange range = lrManquantes.get(0);
					Assert.assertEquals(date(2010, 1, 1), range.getDateDebut());
					Assert.assertEquals(date(2010, 6, 30), range.getDateFin());
				}
				{
					final DateRange range = lrManquantes.get(1);
					Assert.assertEquals(date(2010, 7, 1), range.getDateDebut());
					Assert.assertEquals(date(2010, 9, 30), range.getDateFin());
				}
				{
					final DateRange range = lrManquantes.get(2);
					Assert.assertEquals(date(2010, 10, 1), range.getDateDebut());
					Assert.assertEquals(date(2010, 10, 31), range.getDateFin());
				}
				{
					final DateRange range = lrManquantes.get(3);
					Assert.assertEquals(date(2010, 11, 1), range.getDateDebut());
					Assert.assertEquals(date(2010, 11, 30), range.getDateFin());
				}
				{
					final DateRange range = lrManquantes.get(4);
					Assert.assertEquals(date(2010, 12, 1), range.getDateDebut());
					Assert.assertEquals(date(2010, 12, 31), range.getDateFin());
				}
				return null;
			}
		});
	}

	@Test
	public void testImprimeAllLRWithDifferentesPeriodicitesLaMemeAnnee() throws Exception {
		// mise en place
		final long dpiId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final DebiteurPrestationImposable dpi = addDebiteur();
				tiersService.addPeriodicite(dpi, PeriodiciteDecompte.SEMESTRIEL, null, date(2010, 1, 1), date(2010, 3, 31));
				tiersService.addPeriodicite(dpi, PeriodiciteDecompte.TRIMESTRIEL, null, date(2010, 4, 1), date(2010, 9, 30));
				tiersService.addPeriodicite(dpi, PeriodiciteDecompte.MENSUEL, null, date(2010, 10, 1), null);
				addForDebiteur(dpi, date(2010, 1, 1), MotifFor.INDETERMINE, null, null, MockCommune.Aubonne);
				addPeriodeFiscale(2010);
				return dpi.getNumero();
			}
		});

		final EnvoiLRsResults results = lrService.imprimerAllLR(date(2010, 12, 31), null);
		Assert.assertNotNull(results);
		Assert.assertEquals(1, results.nbDPIsTotal);
		Assert.assertEquals(0, results.LREnErreur.size());
		Assert.assertEquals(0, results.nbLrAnnuellesTraitees);
		Assert.assertEquals(1, results.nbLrSemestriellesTraitees);
		Assert.assertEquals(1, results.nbLrTrimestriellesTraitees);
		Assert.assertEquals(3, results.nbLrMensuellesTraitees);
		Assert.assertEquals(5, results.LRTraitees.size());
		{
			final EnvoiLRsResults.Traite lr = results.LRTraitees.get(0);
			Assert.assertNotNull(lr);
			Assert.assertEquals(date(2010, 1, 1), lr.dateDebut);
			Assert.assertEquals(date(2010, 6, 30), lr.dateFin);
			Assert.assertEquals(PeriodiciteDecompte.SEMESTRIEL, lr.periodicite);
		}
		{
			final EnvoiLRsResults.Traite lr = results.LRTraitees.get(1);
			Assert.assertNotNull(lr);
			Assert.assertEquals(date(2010, 7, 1), lr.dateDebut);
			Assert.assertEquals(date(2010, 9, 30), lr.dateFin);
			Assert.assertEquals(PeriodiciteDecompte.TRIMESTRIEL, lr.periodicite);
		}
		{
			final EnvoiLRsResults.Traite lr = results.LRTraitees.get(2);
			Assert.assertNotNull(lr);
			Assert.assertEquals(date(2010, 10, 1), lr.dateDebut);
			Assert.assertEquals(date(2010, 10, 31), lr.dateFin);
			Assert.assertEquals(PeriodiciteDecompte.MENSUEL, lr.periodicite);
		}
		{
			final EnvoiLRsResults.Traite lr = results.LRTraitees.get(3);
			Assert.assertNotNull(lr);
			Assert.assertEquals(date(2010, 11, 1), lr.dateDebut);
			Assert.assertEquals(date(2010, 11, 30), lr.dateFin);
			Assert.assertEquals(PeriodiciteDecompte.MENSUEL, lr.periodicite);
		}
		{
			final EnvoiLRsResults.Traite lr = results.LRTraitees.get(4);
			Assert.assertNotNull(lr);
			Assert.assertEquals(date(2010, 12, 1), lr.dateDebut);
			Assert.assertEquals(date(2010, 12, 31), lr.dateFin);
			Assert.assertEquals(PeriodiciteDecompte.MENSUEL, lr.periodicite);
		}

		// on vérifie que l'on a bien ce que l'on veut en base aussi
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final DebiteurPrestationImposable dpi = tiersDAO.getDebiteurPrestationImposableByNumero(dpiId);
				Assert.assertNotNull(dpi);

				// toute l'année, répartie entre 5 LR (une semestrielle, une trimestrielle, et trois mensuelles)
				final List<Declaration> lrs = dpi.getDeclarationsTriees(Declaration.class, true);
				Assert.assertNotNull(lrs);
				Assert.assertEquals(5, lrs.size());
				{
					final Declaration decl = lrs.get(0);
					Assert.assertNotNull(decl);
					Assert.assertEquals(DeclarationImpotSource.class, decl.getClass());
					Assert.assertEquals(date(2010, 1, 1), decl.getDateDebut());
					Assert.assertEquals(date(2010, 6, 30), decl.getDateFin());
					Assert.assertEquals(PeriodiciteDecompte.SEMESTRIEL, ((DeclarationImpotSource) decl).getPeriodicite());
					Assert.assertFalse(decl.isAnnule());
				}
				{
					final Declaration decl = lrs.get(1);
					Assert.assertNotNull(decl);
					Assert.assertEquals(DeclarationImpotSource.class, decl.getClass());
					Assert.assertEquals(date(2010, 7, 1), decl.getDateDebut());
					Assert.assertEquals(date(2010, 9, 30), decl.getDateFin());
					Assert.assertEquals(PeriodiciteDecompte.TRIMESTRIEL, ((DeclarationImpotSource) decl).getPeriodicite());
					Assert.assertFalse(decl.isAnnule());
				}
				{
					final Declaration decl = lrs.get(2);
					Assert.assertNotNull(decl);
					Assert.assertEquals(DeclarationImpotSource.class, decl.getClass());
					Assert.assertEquals(date(2010, 10, 1), decl.getDateDebut());
					Assert.assertEquals(date(2010, 10, 31), decl.getDateFin());
					Assert.assertEquals(PeriodiciteDecompte.MENSUEL, ((DeclarationImpotSource) decl).getPeriodicite());
					Assert.assertFalse(decl.isAnnule());
				}
				{
					final Declaration decl = lrs.get(3);
					Assert.assertNotNull(decl);
					Assert.assertEquals(DeclarationImpotSource.class, decl.getClass());
					Assert.assertEquals(date(2010, 11, 1), decl.getDateDebut());
					Assert.assertEquals(date(2010, 11, 30), decl.getDateFin());
					Assert.assertEquals(PeriodiciteDecompte.MENSUEL, ((DeclarationImpotSource) decl).getPeriodicite());
					Assert.assertFalse(decl.isAnnule());
				}
				{
					final Declaration decl = lrs.get(4);
					Assert.assertNotNull(decl);
					Assert.assertEquals(DeclarationImpotSource.class, decl.getClass());
					Assert.assertEquals(date(2010, 12, 1), decl.getDateDebut());
					Assert.assertEquals(date(2010, 12, 31), decl.getDateFin());
					Assert.assertEquals(PeriodiciteDecompte.MENSUEL, ((DeclarationImpotSource) decl).getPeriodicite());
					Assert.assertFalse(decl.isAnnule());
				}
				return null;
			}
		});
	}

	@Test
	public void testAjusterSelonPeriodeFiscale() throws Exception {

		// cas simple sur une seule période fiscale (= pas de coupure à générer)
		{
			final List<DateRange> periodesADecouper = Collections.<DateRange>singletonList(new DateRangeHelper.Range(date(2010, 5, 14), date(2010, 9, 10)));
			final List<DateRange> periodesDecoupees = ListeRecapServiceImpl.ajusterSelonPeriodeFiscale(periodesADecouper);
			checkSameCollections(periodesADecouper, periodesDecoupees);
		}

		// cas simple sur deux périodes fiscales
		{
			final List<DateRange> periodesADecouper = Collections.<DateRange>singletonList(new DateRangeHelper.Range(date(2009, 2, 1), date(2010, 9, 10)));
			final List<DateRange> periodesDecoupees = ListeRecapServiceImpl.ajusterSelonPeriodeFiscale(periodesADecouper);

			final List<DateRange> periodeAttendues = new ArrayList<>();
			periodeAttendues.add(new DateRangeHelper.Range(date(2009, 2, 1), date(2009, 12, 31)));
			periodeAttendues.add(new DateRangeHelper.Range(date(2010, 1, 1), date(2010, 9, 10)));

			checkSameCollections(periodeAttendues, periodesDecoupees);
		}

		// cas simple sur plusieurs périodes fiscales
		{
			final List<DateRange> periodesADecouper = Collections.<DateRange>singletonList(new DateRangeHelper.Range(date(2006, 5, 14), date(2010, 9, 10)));
			final List<DateRange> periodesDecoupees = ListeRecapServiceImpl.ajusterSelonPeriodeFiscale(periodesADecouper);

			final List<DateRange> periodesAttendues = new ArrayList<>();
			periodesAttendues.add(new DateRangeHelper.Range(date(2006, 5, 14), date(2006, 12, 31)));
			periodesAttendues.add(new DateRangeHelper.Range(date(2007, 1, 1), date(2007, 12, 31)));
			periodesAttendues.add(new DateRangeHelper.Range(date(2008, 1, 1), date(2008, 12, 31)));
			periodesAttendues.add(new DateRangeHelper.Range(date(2009, 1, 1), date(2009, 12, 31)));
			periodesAttendues.add(new DateRangeHelper.Range(date(2010, 1, 1), date(2010, 9, 10)));

			checkSameCollections(periodesAttendues, periodesDecoupees);
		}

		// cas multi-source
		{
			final List<DateRange> periodesADecouper = new ArrayList<>();
			periodesADecouper.add(new DateRangeHelper.Range(date(2006, 5, 14), date(2006, 12, 30)));
			periodesADecouper.add(new DateRangeHelper.Range(date(2006, 12, 31), date(2007, 3, 1)));
			periodesADecouper.add(new DateRangeHelper.Range(date(2007, 4, 12), date(2007, 4, 12)));
			periodesADecouper.add(new DateRangeHelper.Range(date(2008, 5, 24), date(2010, 1, 1)));
			final List<DateRange> periodesDecoupees = ListeRecapServiceImpl.ajusterSelonPeriodeFiscale(periodesADecouper);

			final List<DateRange> periodesAttendues = new ArrayList<>();
			periodesAttendues.add(new DateRangeHelper.Range(date(2006, 5, 14), date(2006, 12, 30)));
			periodesAttendues.add(new DateRangeHelper.Range(date(2006, 12, 31), date(2006, 12, 31)));
			periodesAttendues.add(new DateRangeHelper.Range(date(2007, 1, 1), date(2007, 3, 1)));
			periodesAttendues.add(new DateRangeHelper.Range(date(2007, 4, 12), date(2007, 4, 12)));
			periodesAttendues.add(new DateRangeHelper.Range(date(2008, 5, 24), date(2008, 12, 31)));
			periodesAttendues.add(new DateRangeHelper.Range(date(2009, 1, 1), date(2009, 12, 31)));
			periodesAttendues.add(new DateRangeHelper.Range(date(2010, 1, 1), date(2010, 1, 1)));

			checkSameCollections(periodesAttendues, periodesDecoupees);
		}
	}

	@Test
	public void testExtrairePeriodesAvecPeriodicites() throws Exception {

		final RegDate dateDebut = date(2008, 1, 1);
		final RegDate datePremierChangement = date(2009, 1, 1);
		final RegDate dateDeuxiemeChangement = date(2010, 1, 1);

		// initialisation
		final long dpiId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final DebiteurPrestationImposable dpi = addDebiteur();
				dpi.setCategorieImpotSource(CategorieImpotSource.REGULIERS);
				dpi.addPeriodicite(new Periodicite(PeriodiciteDecompte.MENSUEL, null, dateDebut, datePremierChangement.getOneDayBefore()));
				dpi.addPeriodicite(new Periodicite(PeriodiciteDecompte.TRIMESTRIEL, null, datePremierChangement, dateDeuxiemeChangement.getOneDayBefore()));
				dpi.addPeriodicite(new Periodicite(PeriodiciteDecompte.ANNUEL, null, dateDeuxiemeChangement, null));
				addForDebiteur(dpi, dateDebut, MotifFor.INDETERMINE, null, null, MockCommune.Lausanne);
				return dpi.getNumero();
			}
		});

		// tests
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {

				final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersDAO.get(dpiId);
				final List<DateRange> periodeACompleter = Collections.<DateRange>singletonList(new DateRangeHelper.Range(dateDebut, date(2009, 12, 31)));
				final List<DateRange> lrTrouvees = ListeRecapServiceImpl.extrairePeriodesAvecPeriodicites(dpi, periodeACompleter);

				// attendues : mensuelles sur 2008, trimestrielles sur 2009
				final List<DateRange> lrAttendues = new ArrayList<>();
				for (int i = 0 ; i < 12 ; ++ i) {
					final RegDate start = date(2008, i + 1, 1);
					final RegDate lastDay = start.addMonths(1).addDays(-1);
					lrAttendues.add(new DateRangeHelper.Range(start, lastDay));
				}
				for (int i = 0 ; i < 4 ; ++ i) {
					final RegDate start = date(2009, i * 3 + 1, 1);
					final RegDate lastDay = start.addMonths(3).addDays(-1);
					lrAttendues.add(new DateRangeHelper.Range(start, lastDay));
				}

				checkSameCollections(lrAttendues, lrTrouvees);
				return null;
			}
		});
	}

	@Test
	public void testValidationDebiteurApresEmissionLR() throws Exception {

		final RegDate dateDebutFor = date(2012, 12, 1);     // pas sur un trimestre
		final RegDate dateFinPeriode = date(2012, 12, 31);  // on ne s'intéresse pour le moment qu'à la fin 2012

		final long dpiId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final DebiteurPrestationImposable dpi = addDebiteur();
				dpi.setCategorieImpotSource(CategorieImpotSource.REGULIERS);
				dpi.addPeriodicite(new Periodicite(PeriodiciteDecompte.TRIMESTRIEL, null, dateDebutFor, null));
				addForDebiteur(dpi, dateDebutFor, MotifFor.INDETERMINE, null, null, MockCommune.Lausanne);
				addPeriodeFiscale(dateDebutFor.year());
				return dpi.getNumero();
			}
		});

		final EnvoiLRsResults results = lrService.imprimerAllLR(dateFinPeriode, null);
		Assert.assertNotNull(results);
		Assert.assertNotNull(results.LRTraitees);
		Assert.assertEquals(0, results.LRTraitees.size());
		Assert.assertNotNull(results.LREnErreur);
		Assert.assertEquals(1, results.LREnErreur.size());
		final EnvoiLRsResults.Erreur erreur = results.LREnErreur.get(0);
		Assert.assertNotNull(erreur);
		Assert.assertEquals(EnvoiLRsResults.ErreurType.ROLLBACK, erreur.raison);
		final String msg = "La période qui débute le (01.10.2012) et se termine le (30.11.2012) contient des LRs alors qu'elle n'est couverte par aucun for valide";
		Assert.assertTrue("Détails reçus : " + erreur.details, erreur.details.contains(msg));

		// on vérifie bien qu'aucune LR n'a été générée...
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersDAO.get(dpiId);
				Assert.assertNotNull(dpi);
				final Set<Declaration> lrs = dpi.getDeclarations();
				Assert.assertEquals(0, lrs.size());
				return null;
			}
		});
	}

	private static void checkSameCollections(List<DateRange> expected, List<DateRange> found) {
		if (expected == null) {
			Assert.assertNull(found);
		}
		else {
			Assert.assertNotNull(found);
			Assert.assertEquals(expected.size(), found.size());
			for (int i = 0 ; i < expected.size() ; ++ i) {
				Assert.assertEquals("Index " + i, expected.get(i), found.get(i));
			}
		}
	}
}
