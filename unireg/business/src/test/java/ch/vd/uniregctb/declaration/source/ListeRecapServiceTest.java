package ch.vd.uniregctb.declaration.source;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import junit.framework.Assert;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.declaration.PeriodeFiscale;
import ch.vd.uniregctb.declaration.Periodicite;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.type.CategorieImpotSource;
import ch.vd.uniregctb.type.PeriodeDecompte;
import ch.vd.uniregctb.type.PeriodiciteDecompte;
import ch.vd.uniregctb.type.TypeEtatDeclaration;

public class ListeRecapServiceTest extends BusinessTest {

	private ListeRecapService lrService = null;
	private TiersDAO tiersDAO;
	private final static String DB_UNIT_FILE = "ListeRecapServiceTest.xml";

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
		final List<DateRange> lrTrouvees = new ArrayList<DateRange>();
		final List<DateRange> lrManquantes = lrService.findLRsManquantes(dpi, dateFinPeriode, lrTrouvees);
		Assert.assertEquals(16, lrManquantes.size());
		Assert.assertEquals(1, lrTrouvees.size());
		final DateRange firstRange = lrManquantes.get(0);
		Assert.assertEquals(RegDate.get(2007, 1, 1), firstRange.getDateDebut());
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testFindLRsManquantesWithDifferentesPeriodicites1() throws Exception {

		final long dpiId = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				DebiteurPrestationImposable dpi = addDebiteur();
				tiersService.addPeriodicite(dpi, PeriodiciteDecompte.UNIQUE, PeriodeDecompte.M01, date(2008, 1, 1), date(2008, 12, 31));
				tiersService.addPeriodicite(dpi, PeriodiciteDecompte.TRIMESTRIEL, null, date(2009, 1, 1), date(2009, 12, 31));
				tiersService.addPeriodicite(dpi, PeriodiciteDecompte.UNIQUE, PeriodeDecompte.M01, date(2010, 1, 1), date(2010, 12, 31));
				tiersService.addPeriodicite(dpi, PeriodiciteDecompte.MENSUEL, null, date(2011, 1, 1), null);
				addForDebiteur(dpi, date(2008, 1, 1), null, MockCommune.Bex);
				return dpi.getNumero();
			}
		});

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersDAO.get(dpiId);
				final RegDate dateFinPeriode = RegDate.get(2010, 12, 31);
				final List<DateRange> lrTrouvees = new ArrayList<DateRange>();
				final List<DateRange> lrManquantes = lrService.findLRsManquantes(dpi, dateFinPeriode, lrTrouvees);
				Assert.assertEquals(6, lrManquantes.size());
				final DateRange firstRange = lrManquantes.get(0);
				Assert.assertEquals(RegDate.get(2008, 1, 1), firstRange.getDateDebut());
				return null;
			}
		});
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testFindLRsManquantesWithDifferentesPeriodicites2() throws Exception {

		final long dpiId2 = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				DebiteurPrestationImposable dpi = addDebiteur();
				tiersService.addPeriodicite(dpi, PeriodiciteDecompte.UNIQUE, PeriodeDecompte.M02, date(2008, 1, 1), date(2008, 12, 31));
				tiersService.addPeriodicite(dpi, PeriodiciteDecompte.TRIMESTRIEL, null, date(2009, 1, 1), date(2009, 12, 31));
				tiersService.addPeriodicite(dpi, PeriodiciteDecompte.UNIQUE, PeriodeDecompte.S1, date(2010, 1, 1), date(2010, 12, 31));
				tiersService.addPeriodicite(dpi, PeriodiciteDecompte.MENSUEL, null, date(2011, 1, 1), null);
				addForDebiteur(dpi, date(2008, 1, 1), null, MockCommune.Bex);
				return dpi.getNumero();
			}
		});

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersDAO.get(dpiId2);
				final RegDate dateFinPeriode = RegDate.get(2010, 12, 31);
				final List<DateRange> lrTrouvees = new ArrayList<DateRange>();
				final List<DateRange> lrManquantes = lrService.findLRsManquantes(dpi, dateFinPeriode, lrTrouvees);
				Assert.assertEquals(6, lrManquantes.size());
				final DateRange firstRange = lrManquantes.get(0);
				Assert.assertEquals(RegDate.get(2008, 2, 1), firstRange.getDateDebut());
				return null;
			}
		});
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testFindLRsManquantesWithDifferentesPeriodicites3() throws Exception {

		final long dpiId3 = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				DebiteurPrestationImposable dpi = addDebiteur();
				tiersService.addPeriodicite(dpi, PeriodiciteDecompte.UNIQUE, PeriodeDecompte.M02, date(2008, 1, 1), date(2008, 12, 31));
				tiersService.addPeriodicite(dpi, PeriodiciteDecompte.TRIMESTRIEL, null, date(2009, 1, 1), date(2009, 12, 31));
				tiersService.addPeriodicite(dpi, PeriodiciteDecompte.UNIQUE, PeriodeDecompte.S1, date(2010, 1, 1), date(2010, 12, 31));
				tiersService.addPeriodicite(dpi, PeriodiciteDecompte.MENSUEL, null, date(2011, 1, 1), null);
				addForDebiteur(dpi, date(2008, 1, 1), null, MockCommune.Bex);
				final PeriodeFiscale periodeFiscale2008 = addPeriodeFiscale(2008);
				final PeriodeFiscale periodeFiscale2009 = addPeriodeFiscale(2009);

				addLR(dpi, date(2008, 2, 1), date(2008, 8, 28), periodeFiscale2008, TypeEtatDeclaration.RETOURNEE);
				addLR(dpi, date(2009, 1, 1), date(2009, 3, 31), periodeFiscale2009, TypeEtatDeclaration.RETOURNEE);
				addLR(dpi, date(2009, 4, 1), date(2009, 6, 30), periodeFiscale2009, TypeEtatDeclaration.RETOURNEE);
				return dpi.getNumero();
			}
		});

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersDAO.get(dpiId3);
				final RegDate dateFinPeriode = RegDate.get(2010, 12, 31);
				final List<DateRange> lrTrouvees = new ArrayList<DateRange>();
				final List<DateRange> lrManquantes = lrService.findLRsManquantes(dpi, dateFinPeriode, lrTrouvees);
				Assert.assertEquals(3, lrManquantes.size());
				final DateRange firstRange = lrManquantes.get(0);
				Assert.assertEquals(RegDate.get(2009, 7, 1), firstRange.getDateDebut());
				return null;
			}
		});
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testFindLRsManquantesWithDifferentesPeriodicites4() throws Exception {

		final long dpiId4 = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				DebiteurPrestationImposable dpi = addDebiteur();
				tiersService.addPeriodicite(dpi, PeriodiciteDecompte.UNIQUE, PeriodeDecompte.M02, date(2008, 1, 1), date(2008, 12, 31));
				tiersService.addPeriodicite(dpi, PeriodiciteDecompte.ANNUEL, PeriodeDecompte.T1, date(2009, 1, 1), date(2009, 12, 31));
				tiersService.addPeriodicite(dpi, PeriodiciteDecompte.UNIQUE, PeriodeDecompte.S1, date(2010, 1, 1), date(2010, 12, 31));
				tiersService.addPeriodicite(dpi, PeriodiciteDecompte.ANNUEL, PeriodeDecompte.A, date(2011, 1, 1), null);
				addForDebiteur(dpi, date(2008, 1, 1), null, MockCommune.Bex);
				return dpi.getNumero();
			}
		});

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersDAO.get(dpiId4);
				final RegDate dateFinPeriode = RegDate.get(2011, 11, 23);
				final List<DateRange> lrTrouvees = new ArrayList<DateRange>();
				final List<DateRange> lrManquantes = lrService.findLRsManquantes(dpi, dateFinPeriode, lrTrouvees);
				Assert.assertEquals(4, lrManquantes.size());
				final DateRange firstRange = lrManquantes.get(0);
				Assert.assertEquals(RegDate.get(2008, 2, 1), firstRange.getDateDebut());
				return null;
			}
		});
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testAjusterSelonPeriodeFiscale() throws Exception {

		// cas simple sur une seule période fiscale (= pas de coupure à générer)
		{
			final List<DateRange> periodesADecouper = Arrays.<DateRange>asList(new DateRangeHelper.Range(date(2010, 5, 14), date(2010, 9, 10)));
			final List<DateRange> periodesDecoupees = ListeRecapServiceImpl.ajusterSelonPeriodeFiscale(periodesADecouper);
			checkSameCollections(periodesADecouper, periodesDecoupees);
		}

		// cas simple sur deux périodes fiscales
		{
			final List<DateRange> periodesADecouper = Arrays.<DateRange>asList(new DateRangeHelper.Range(date(2009, 2, 1), date(2010, 9, 10)));
			final List<DateRange> periodesDecoupees = ListeRecapServiceImpl.ajusterSelonPeriodeFiscale(periodesADecouper);

			final List<DateRange> periodeAttendues = new ArrayList<DateRange>();
			periodeAttendues.add(new DateRangeHelper.Range(date(2009, 2, 1), date(2009, 12, 31)));
			periodeAttendues.add(new DateRangeHelper.Range(date(2010, 1, 1), date(2010, 9, 10)));

			checkSameCollections(periodeAttendues, periodesDecoupees);
		}

		// cas simple sur plusieurs périodes fiscales
		{
			final List<DateRange> periodesADecouper = Arrays.<DateRange>asList(new DateRangeHelper.Range(date(2006, 5, 14), date(2010, 9, 10)));
			final List<DateRange> periodesDecoupees = ListeRecapServiceImpl.ajusterSelonPeriodeFiscale(periodesADecouper);

			final List<DateRange> periodesAttendues = new ArrayList<DateRange>();
			periodesAttendues.add(new DateRangeHelper.Range(date(2006, 5, 14), date(2006, 12, 31)));
			periodesAttendues.add(new DateRangeHelper.Range(date(2007, 1, 1), date(2007, 12, 31)));
			periodesAttendues.add(new DateRangeHelper.Range(date(2008, 1, 1), date(2008, 12, 31)));
			periodesAttendues.add(new DateRangeHelper.Range(date(2009, 1, 1), date(2009, 12, 31)));
			periodesAttendues.add(new DateRangeHelper.Range(date(2010, 1, 1), date(2010, 9, 10)));

			checkSameCollections(periodesAttendues, periodesDecoupees);
		}

		// cas multi-source
		{
			final List<DateRange> periodesADecouper = new ArrayList<DateRange>();
			periodesADecouper.add(new DateRangeHelper.Range(date(2006, 5, 14), date(2006, 12, 30)));
			periodesADecouper.add(new DateRangeHelper.Range(date(2006, 12, 31), date(2007, 3, 1)));
			periodesADecouper.add(new DateRangeHelper.Range(date(2007, 4, 12), date(2007, 4, 12)));
			periodesADecouper.add(new DateRangeHelper.Range(date(2008, 5, 24), date(2010, 1, 1)));
			final List<DateRange> periodesDecoupees = ListeRecapServiceImpl.ajusterSelonPeriodeFiscale(periodesADecouper);

			final List<DateRange> periodesAttendues = new ArrayList<DateRange>();
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
				addForDebiteur(dpi, dateDebut, null, MockCommune.Lausanne);
				return dpi.getNumero();
			}
		});

		// tests
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {

				final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersDAO.get(dpiId);
				final List<DateRange> periodeACompleter = Arrays.<DateRange>asList(new DateRangeHelper.Range(dateDebut, date(2009, 12, 31)));
				final List<DateRange> lrTrouvees = ListeRecapServiceImpl.extrairePeriodesAvecPeriodicites(dpi, periodeACompleter);

				// attendues : mensuelles sur 2008, trimestrielles sur 2009
				final List<DateRange> lrAttendues = new ArrayList<DateRange>();
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
