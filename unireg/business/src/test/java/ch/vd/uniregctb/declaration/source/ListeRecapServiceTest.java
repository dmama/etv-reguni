package ch.vd.uniregctb.declaration.source;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import junit.framework.Assert;
import org.junit.Test;
import org.springframework.test.annotation.NotTransactional;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.declaration.Periodicite;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.type.CategorieImpotSource;
import ch.vd.uniregctb.type.PeriodiciteDecompte;

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
	@NotTransactional
	public void testExtrairePeriodesAvecPeriodicites() throws Exception {

		final RegDate dateDebut = date(2008, 1, 1);
		final RegDate datePremierChangement = date(2009, 1, 1);
		final RegDate dateDeuxiemeChangement = date(2010, 1, 1);

		// initialisation
		final long dpiId = (Long) doInNewTransactionAndSession(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
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
		doInNewTransactionAndSession(new TransactionCallback() {
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
