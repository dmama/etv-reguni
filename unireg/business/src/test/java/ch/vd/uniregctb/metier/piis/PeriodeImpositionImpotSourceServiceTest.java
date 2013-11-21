package ch.vd.uniregctb.metier.piis;

import java.util.List;

import org.apache.commons.lang3.mutable.MutableLong;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.tx.TxCallbackWithoutResult;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.metier.assujettissement.AssujettissementException;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.CategorieImpotSource;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.PeriodiciteDecompte;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;
import ch.vd.uniregctb.validation.fors.ForFiscalValidator;

@SuppressWarnings("deprecation")
public class PeriodeImpositionImpotSourceServiceTest extends BusinessTest {

	private PeriodeImpositionImpotSourceService service;

	@Override
	protected void runOnSetUp() throws Exception {
		super.runOnSetUp();
		service = getBean(PeriodeImpositionImpotSourceService.class, "periodeImpositionImpotSourceService");
	}

	private void assertNoPiis(PersonnePhysique pp) throws AssujettissementException {
		final List<PeriodeImpositionImpotSource> piis = service.determine(pp);
		Assert.assertNotNull(piis);
		Assert.assertEquals(0, piis.size());
	}

	@Test
	public void testSansForNiRT() throws Exception {

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Alastair", "O'Malley", date(1978, 4, 2), Sexe.MASCULIN);
				return pp.getNumero();
			}
		});

		// calcul
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				assertNoPiis(pp);
			}
		});
	}

	@Test
	public void testOrdinaireSansRT() throws Exception {
		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Alastair", "O'Malley", date(1978, 4, 2), Sexe.MASCULIN);
				addForPrincipal(pp, date(2005, 1, 3), MotifFor.ARRIVEE_HS, MockCommune.Cossonay);
				return pp.getNumero();
			}
		});

		// calcul
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				assertNoPiis(pp);
			}
		});
	}

	@Test
	public void testDecesSourcierPur() throws Exception {

		final int lastYear = RegDate.get().year() - 1;
		final RegDate dateDeces = date(lastYear, 5, 8);

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Alastair", "O'Malley", date(1978, 4, 2), Sexe.MASCULIN);
				pp.setDateDeces(dateDeces);
				addForPrincipal(pp, date(lastYear, 1, 1), MotifFor.ARRIVEE_HS, dateDeces, MotifFor.VEUVAGE_DECES, MockCommune.Aubonne, ModeImposition.SOURCE);
				return pp.getNumero();
			}
		});

		// calcul
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				final List<PeriodeImpositionImpotSource> piis = service.determine(pp);
				Assert.assertNotNull(piis);
				Assert.assertEquals(1, piis.size());

				final PeriodeImpositionImpotSource pi = piis.get(0);
				Assert.assertNotNull(pi);
				Assert.assertEquals(PeriodeImpositionImpotSource.Type.SOURCE, pi.getType());
				Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, pi.getTypeAutoriteFiscale());
				Assert.assertEquals((Integer) MockCommune.Aubonne.getNoOFS(), pi.getNoOfs());
				Assert.assertEquals(date(lastYear, 1, 1), pi.getDateDebut());
				Assert.assertEquals(dateDeces, pi.getDateFin());
				Assert.assertNotNull(pi.getContribuable());
				Assert.assertEquals((Long) ppId, pi.getContribuable().getNumero());
			}
		});
	}

	@Test
	public void testDecesOrdinaireAvecRT() throws Exception {

		final int yearBeforeLast = RegDate.get().year() - 2;
		final int lastYear = yearBeforeLast + 1;
		final RegDate dateRT = date(lastYear, 1, 15);
		final RegDate dateDeces = date(lastYear, 5, 8);

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Alastair", "O'Malley", date(1978, 4, 2), Sexe.MASCULIN);
				pp.setDateDeces(dateDeces);
				addForPrincipal(pp, date(yearBeforeLast, 1, 1), MotifFor.ARRIVEE_HS, dateDeces, MotifFor.VEUVAGE_DECES, MockCommune.Aubonne);

				final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.MENSUEL, date(2009, 1, 1));
				addRapportPrestationImposable(dpi, pp, dateRT, dateDeces, false);

				return pp.getNumero();
			}
		});

		// calcul
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				final List<PeriodeImpositionImpotSource> piis = service.determine(pp);
				Assert.assertNotNull(piis);
				Assert.assertEquals(1, piis.size());

				final PeriodeImpositionImpotSource pi = piis.get(0);
				Assert.assertNotNull(pi);
				Assert.assertEquals(PeriodeImpositionImpotSource.Type.MIXTE, pi.getType());
				Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, pi.getTypeAutoriteFiscale());
				Assert.assertEquals((Integer) MockCommune.Aubonne.getNoOFS(), pi.getNoOfs());
				Assert.assertEquals(date(lastYear, 1, 1), pi.getDateDebut());
				Assert.assertEquals(dateDeces, pi.getDateFin());
				Assert.assertNotNull(pi.getContribuable());
				Assert.assertEquals((Long) ppId, pi.getContribuable().getNumero());
			}
		});
	}

	@Test
	public void testDecesOrdinaireSansRT() throws Exception {

		final int lastYear = RegDate.get().year() - 1;
		final RegDate dateDeces = date(lastYear, 5, 8);

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Alastair", "O'Malley", date(1978, 4, 2), Sexe.MASCULIN);
				pp.setDateDeces(dateDeces);
				addForPrincipal(pp, date(lastYear, 1, 1), MotifFor.ARRIVEE_HS, dateDeces, MotifFor.VEUVAGE_DECES, MockCommune.Aubonne);
				return pp.getNumero();
			}
		});

		// calcul
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				assertNoPiis(pp);
			}
		});
	}

	/**
	 * Cas 1
	 */
	@Test
	public void testSourcePureSimple() throws Exception {

		final int currentYear = RegDate.get().year();
		final int firstYear = currentYear - 1;
		final RegDate debutFor = date(firstYear, 6, 15);

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Alastair", "O'Malley", date(1978, 5, 2), Sexe.MASCULIN);
				addForPrincipal(pp, debutFor, MotifFor.ARRIVEE_HS, MockCommune.Aigle, ModeImposition.SOURCE);
				return pp.getNumero();
			}
		});

		// calcul
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				final List<PeriodeImpositionImpotSource> piis = service.determine(pp);
				Assert.assertNotNull(piis);
				Assert.assertEquals(3, piis.size());
				{
					final PeriodeImpositionImpotSource pi = piis.get(0);
					Assert.assertNotNull(pi);
					Assert.assertEquals(PeriodeImpositionImpotSource.Type.SOURCE, pi.getType());
					Assert.assertNull(pi.getTypeAutoriteFiscale());
					Assert.assertNull(pi.getNoOfs());
					Assert.assertEquals(date(firstYear, 1, 1), pi.getDateDebut());
					Assert.assertEquals(debutFor.getOneDayBefore(), pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) ppId, pi.getContribuable().getNumero());
				}
				{
					final PeriodeImpositionImpotSource pi = piis.get(1);
					Assert.assertNotNull(pi);
					Assert.assertEquals(PeriodeImpositionImpotSource.Type.SOURCE, pi.getType());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, pi.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockCommune.Aigle.getNoOFS(), pi.getNoOfs());
					Assert.assertEquals(debutFor, pi.getDateDebut());
					Assert.assertEquals(date(firstYear, 12, 31), pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) ppId, pi.getContribuable().getNumero());
				}
				{
					final PeriodeImpositionImpotSource pi = piis.get(2);
					Assert.assertNotNull(pi);
					Assert.assertEquals(PeriodeImpositionImpotSource.Type.SOURCE, pi.getType());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, pi.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockCommune.Aigle.getNoOFS(), pi.getNoOfs());
					Assert.assertEquals(date(currentYear, 1, 1), pi.getDateDebut());
					Assert.assertEquals(date(currentYear, 12, 31), pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) ppId, pi.getContribuable().getNumero());
				}
			}
		});
	}

	/**
	 * Cas 2, 3, 5 à 8 (mixtes)
	 */
	@Test
	public void testMixteSimple() throws Exception {

		final int currentYear = RegDate.get().year();
		final int firstYear = currentYear - 1;
		final RegDate debutFor = date(firstYear, 6, 15);

		final class Ids {
			long ppMixte1;
			long ppMixte2;
		}

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final Ids ids = new Ids();
				{
					final PersonnePhysique pp = addNonHabitant("Alastair M1", "O'Malley", date(1978, 5, 2), Sexe.MASCULIN);
					addForPrincipal(pp, debutFor, MotifFor.ARRIVEE_HS, MockCommune.Aigle, ModeImposition.MIXTE_137_1);
					ids.ppMixte1 = pp.getNumero();
				}
				{
					final PersonnePhysique pp = addNonHabitant("Alastair M2", "O'Malley", date(1978, 5, 2), Sexe.MASCULIN);
					addForPrincipal(pp, debutFor, MotifFor.ARRIVEE_HS, MockCommune.Aigle, ModeImposition.MIXTE_137_2);
					ids.ppMixte2 = pp.getNumero();
				}
				return ids;
			}
		});

		final MutableLong testedId = new MutableLong();
		final Runnable test = new Runnable() {
			@Override
			public void run() {
				try {
					final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(testedId.longValue());
					final List<PeriodeImpositionImpotSource> piis = service.determine(pp);
					Assert.assertNotNull(piis);
					Assert.assertEquals(3, piis.size());
					{
						final PeriodeImpositionImpotSource pi = piis.get(0);
						Assert.assertNotNull(pi);
						Assert.assertEquals(PeriodeImpositionImpotSource.Type.SOURCE, pi.getType());
						Assert.assertNull(pi.getTypeAutoriteFiscale());
						Assert.assertNull(pi.getNoOfs());
						Assert.assertEquals(date(firstYear, 1, 1), pi.getDateDebut());
						Assert.assertEquals(debutFor.getOneDayBefore(), pi.getDateFin());
						Assert.assertNotNull(pi.getContribuable());
						Assert.assertEquals((Long) testedId.longValue(), pi.getContribuable().getNumero());
					}
					{
						final PeriodeImpositionImpotSource pi = piis.get(1);
						Assert.assertNotNull(pi);
						Assert.assertEquals(PeriodeImpositionImpotSource.Type.MIXTE, pi.getType());
						Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, pi.getTypeAutoriteFiscale());
						Assert.assertEquals((Integer) MockCommune.Aigle.getNoOFS(), pi.getNoOfs());
						Assert.assertEquals(debutFor, pi.getDateDebut());
						Assert.assertEquals(date(firstYear, 12, 31), pi.getDateFin());
						Assert.assertNotNull(pi.getContribuable());
						Assert.assertEquals((Long) testedId.longValue(), pi.getContribuable().getNumero());
					}
					{
						final PeriodeImpositionImpotSource pi = piis.get(2);
						Assert.assertNotNull(pi);
						Assert.assertEquals(PeriodeImpositionImpotSource.Type.MIXTE, pi.getType());
						Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, pi.getTypeAutoriteFiscale());
						Assert.assertEquals((Integer) MockCommune.Aigle.getNoOFS(), pi.getNoOfs());
						Assert.assertEquals(date(currentYear, 1, 1), pi.getDateDebut());
						Assert.assertEquals(date(currentYear, 12, 31), pi.getDateFin());
						Assert.assertNotNull(pi.getContribuable());
						Assert.assertEquals((Long) testedId.longValue(), pi.getContribuable().getNumero());
					}
				}
				catch (AssujettissementException e) {
					throw new RuntimeException(e);
				}
			}
		};

		// calcul
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				testedId.setValue(ids.ppMixte1);
				test.run();

				testedId.setValue(ids.ppMixte2);
				test.run();
			}
		});
	}

	/**
	 * Cas 4 à 8 (ordinaires) avec RT
	 */
	@Test
	public void testOrdinaireSimpleAvecRT() throws Exception {

		final int currentYear = RegDate.get().year();
		final int firstYear = currentYear - 1;
		final RegDate debutFor = date(firstYear, 6, 15);

		final class Ids {
			long ppOrdinaire;
			long ppDepense;
			long ppIndigent;
		}

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.MENSUEL, date(2009, 1, 1));
				final Ids ids = new Ids();
				{
					final PersonnePhysique pp = addNonHabitant("Alastair ORD", "O'Malley", date(1978, 5, 2), Sexe.MASCULIN);
					addForPrincipal(pp, debutFor, MotifFor.ARRIVEE_HS, MockCommune.Aigle, ModeImposition.ORDINAIRE);
					addRapportPrestationImposable(dpi, pp, debutFor, null, false);
					ids.ppOrdinaire = pp.getNumero();
				}
				{
					final PersonnePhysique pp = addNonHabitant("Alastair ICCD", "O'Malley", date(1978, 5, 2), Sexe.MASCULIN);
					addForPrincipal(pp, debutFor, MotifFor.ARRIVEE_HS, MockCommune.Aigle, ModeImposition.DEPENSE);
					addRapportPrestationImposable(dpi, pp, debutFor, null, false);
					ids.ppDepense = pp.getNumero();
				}
				{
					final PersonnePhysique pp = addNonHabitant("Alastair IND", "O'Malley", date(1978, 5, 2), Sexe.MASCULIN);
					addForPrincipal(pp, debutFor, MotifFor.ARRIVEE_HS, MockCommune.Aigle, ModeImposition.INDIGENT);
					addRapportPrestationImposable(dpi, pp, debutFor, null, false);
					ids.ppIndigent = pp.getNumero();
				}

				return ids;
			}
		});

		final MutableLong testedId = new MutableLong();
		final Runnable test = new Runnable() {
			@Override
			public void run() {
				try {
					final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(testedId.longValue());
					final List<PeriodeImpositionImpotSource> piis = service.determine(pp);
					Assert.assertNotNull(piis);
					Assert.assertEquals(3, piis.size());
					{
						final PeriodeImpositionImpotSource pi = piis.get(0);
						Assert.assertNotNull(pi);
						Assert.assertEquals(PeriodeImpositionImpotSource.Type.SOURCE, pi.getType());
						Assert.assertNull(pi.getTypeAutoriteFiscale());
						Assert.assertNull(pi.getNoOfs());
						Assert.assertEquals(date(firstYear, 1, 1), pi.getDateDebut());
						Assert.assertEquals(debutFor.getOneDayBefore(), pi.getDateFin());
						Assert.assertNotNull(pi.getContribuable());
						Assert.assertEquals((Long) testedId.longValue(), pi.getContribuable().getNumero());
					}
					{
						final PeriodeImpositionImpotSource pi = piis.get(1);
						Assert.assertNotNull(pi);
						Assert.assertEquals(PeriodeImpositionImpotSource.Type.MIXTE, pi.getType());
						Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, pi.getTypeAutoriteFiscale());
						Assert.assertEquals((Integer) MockCommune.Aigle.getNoOFS(), pi.getNoOfs());
						Assert.assertEquals(debutFor, pi.getDateDebut());
						Assert.assertEquals(date(firstYear, 12, 31), pi.getDateFin());
						Assert.assertNotNull(pi.getContribuable());
						Assert.assertEquals((Long) testedId.longValue(), pi.getContribuable().getNumero());
					}
					{
						final PeriodeImpositionImpotSource pi = piis.get(2);
						Assert.assertNotNull(pi);
						Assert.assertEquals(PeriodeImpositionImpotSource.Type.MIXTE, pi.getType());
						Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, pi.getTypeAutoriteFiscale());
						Assert.assertEquals((Integer) MockCommune.Aigle.getNoOFS(), pi.getNoOfs());
						Assert.assertEquals(date(currentYear, 1, 1), pi.getDateDebut());
						Assert.assertEquals(date(currentYear, 12, 31), pi.getDateFin());
						Assert.assertNotNull(pi.getContribuable());
						Assert.assertEquals((Long) testedId.longValue(), pi.getContribuable().getNumero());
					}
				}
				catch (AssujettissementException e) {
					throw new RuntimeException(e);
				}
			}
		};

		// calcul
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				testedId.setValue(ids.ppOrdinaire);
				test.run();

				testedId.setValue(ids.ppDepense);
				test.run();

				testedId.setValue(ids.ppIndigent);
				test.run();
			}
		});
	}

	/**
	 * Cas 4 à 8 (ordinaires) sans RT
	 */
	@Test
	public void testOrdinaireSimpleSansRT() throws Exception {

		final int currentYear = RegDate.get().year();
		final int firstYear = currentYear - 1;
		final RegDate debutFor = date(firstYear, 6, 15);

		final class Ids {
			long ppOrdinaire;
			long ppDepense;
			long ppIndigent;
		}

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final Ids ids = new Ids();
				{
					final PersonnePhysique pp = addNonHabitant("Alastair ORD", "O'Malley", date(1978, 5, 2), Sexe.MASCULIN);
					addForPrincipal(pp, debutFor, MotifFor.ARRIVEE_HS, MockCommune.Aigle, ModeImposition.ORDINAIRE);
					ids.ppOrdinaire = pp.getNumero();
				}
				{
					final PersonnePhysique pp = addNonHabitant("Alastair ICCD", "O'Malley", date(1978, 5, 2), Sexe.MASCULIN);
					addForPrincipal(pp, debutFor, MotifFor.ARRIVEE_HS, MockCommune.Aigle, ModeImposition.DEPENSE);
					ids.ppDepense = pp.getNumero();
				}
				{
					final PersonnePhysique pp = addNonHabitant("Alastair IND", "O'Malley", date(1978, 5, 2), Sexe.MASCULIN);
					addForPrincipal(pp, debutFor, MotifFor.ARRIVEE_HS, MockCommune.Aigle, ModeImposition.INDIGENT);
					ids.ppIndigent = pp.getNumero();
				}
				return ids;
			}
		});

		// calcul
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				{
					final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ids.ppOrdinaire);
					final List<PeriodeImpositionImpotSource> piis = service.determine(pp);
					Assert.assertNotNull(piis);
					Assert.assertEquals(0, piis.size());
				}
				{
					final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ids.ppDepense);
					final List<PeriodeImpositionImpotSource> piis = service.determine(pp);
					Assert.assertNotNull(piis);
					Assert.assertEquals(0, piis.size());
				}
				{
					final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ids.ppIndigent);
					final List<PeriodeImpositionImpotSource> piis = service.determine(pp);
					Assert.assertNotNull(piis);
					Assert.assertEquals(0, piis.size());
				}
			}
		});
	}

	/**
	 * Cas 9 à 11 (mixtes)
	 */
	@Test
	public void testArriveeVaudoiseDeHsImmeubleMixtes() throws Exception {

		final int currentYear = RegDate.get().year();
		final int firstYear = currentYear - 2;
		final RegDate achatImmeuble = date(firstYear, 3, 5);
		final RegDate arrivee = date(currentYear - 1, 6, 15);

		final class Ids {
			long ppMixte1;
			long ppMixte2;
		}

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final Ids ids = new Ids();
				{
					final PersonnePhysique pp = addNonHabitant("Alastair M1", "O'Malley", date(1978, 5, 2), Sexe.MASCULIN);
					addForPrincipal(pp, achatImmeuble, MotifFor.ACHAT_IMMOBILIER, arrivee.getOneDayBefore(), MotifFor.ARRIVEE_HS, MockPays.Allemagne);
					addForPrincipal(pp, arrivee, MotifFor.ARRIVEE_HS, MockCommune.Aigle, ModeImposition.MIXTE_137_1);
					addForSecondaire(pp, achatImmeuble, MotifFor.ACHAT_IMMOBILIER, MockCommune.Leysin.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
					ids.ppMixte1 = pp.getNumero();
				}
				{
					final PersonnePhysique pp = addNonHabitant("Alastair M2", "O'Malley", date(1978, 5, 2), Sexe.MASCULIN);
					addForPrincipal(pp, achatImmeuble, MotifFor.ACHAT_IMMOBILIER, arrivee.getOneDayBefore(), MotifFor.ARRIVEE_HS, MockPays.Allemagne);
					addForPrincipal(pp, arrivee, MotifFor.ARRIVEE_HS, MockCommune.Aigle, ModeImposition.MIXTE_137_2);
					addForSecondaire(pp, achatImmeuble, MotifFor.ACHAT_IMMOBILIER, MockCommune.Leysin.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
					ids.ppMixte2 = pp.getNumero();
				}
				return ids;
			}
		});

		final MutableLong testedId = new MutableLong();
		final Runnable test = new Runnable() {
			@Override
			public void run() {
				try {
					final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(testedId.longValue());
					final List<PeriodeImpositionImpotSource> piis = service.determine(pp);
					Assert.assertNotNull(piis);
					Assert.assertEquals(3, piis.size());
					{
						final PeriodeImpositionImpotSource pi = piis.get(0);
						Assert.assertNotNull(pi);
						Assert.assertEquals(PeriodeImpositionImpotSource.Type.SOURCE, pi.getType());
						Assert.assertEquals(TypeAutoriteFiscale.PAYS_HS, pi.getTypeAutoriteFiscale());
						Assert.assertEquals((Integer) MockPays.Allemagne.getNoOFS(), pi.getNoOfs());
						Assert.assertEquals(date(currentYear - 1, 1, 1), pi.getDateDebut());
						Assert.assertEquals(arrivee.getOneDayBefore(), pi.getDateFin());
						Assert.assertNotNull(pi.getContribuable());
						Assert.assertEquals((Long) testedId.longValue(), pi.getContribuable().getNumero());
					}
					{
						final PeriodeImpositionImpotSource pi = piis.get(1);
						Assert.assertNotNull(pi);
						Assert.assertEquals(PeriodeImpositionImpotSource.Type.MIXTE, pi.getType());
						Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, pi.getTypeAutoriteFiscale());
						Assert.assertEquals((Integer) MockCommune.Aigle.getNoOFS(), pi.getNoOfs());
						Assert.assertEquals(arrivee, pi.getDateDebut());
						Assert.assertEquals(date(currentYear - 1, 12, 31), pi.getDateFin());
						Assert.assertNotNull(pi.getContribuable());
						Assert.assertEquals((Long) testedId.longValue(), pi.getContribuable().getNumero());
					}
					{
						final PeriodeImpositionImpotSource pi = piis.get(2);
						Assert.assertNotNull(pi);
						Assert.assertEquals(PeriodeImpositionImpotSource.Type.MIXTE, pi.getType());
						Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, pi.getTypeAutoriteFiscale());
						Assert.assertEquals((Integer) MockCommune.Aigle.getNoOFS(), pi.getNoOfs());
						Assert.assertEquals(date(currentYear, 1, 1), pi.getDateDebut());
						Assert.assertEquals(date(currentYear, 12, 31), pi.getDateFin());
						Assert.assertNotNull(pi.getContribuable());
						Assert.assertEquals((Long) testedId.longValue(), pi.getContribuable().getNumero());
					}
				}
				catch (AssujettissementException e) {
					throw new RuntimeException(e);
				}
			}
		};

		// calcul
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				testedId.setValue(ids.ppMixte1);
				test.run();

				testedId.setValue(ids.ppMixte2);
				test.run();
			}
		});
	}

	/**
	 * Cas 9 à 11 (ordinaires avec RT)
	 */
	@Test
	public void testArriveeVaudoiseDeHsImmeubleOrdinairesAvecRT() throws Exception {

		final int currentYear = RegDate.get().year();
		final int firstYear = currentYear - 2;
		final RegDate achatImmeuble = date(firstYear, 3, 5);
		final RegDate arrivee = date(currentYear - 1, 6, 15);

		final class Ids {
			long ppOrdinaire;
			long ppDepense;
			long ppIndigent;
		}

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final Ids ids = new Ids();
				final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.MENSUEL, date(2009, 1, 1));
				{
					final PersonnePhysique pp = addNonHabitant("Alastair ORD", "O'Malley", date(1978, 5, 2), Sexe.MASCULIN);
					addForPrincipal(pp, achatImmeuble, MotifFor.ACHAT_IMMOBILIER, arrivee.getOneDayBefore(), MotifFor.ARRIVEE_HS, MockPays.Allemagne);
					addForPrincipal(pp, arrivee, MotifFor.ARRIVEE_HS, MockCommune.Aigle, ModeImposition.ORDINAIRE);
					addForSecondaire(pp, achatImmeuble, MotifFor.ACHAT_IMMOBILIER, MockCommune.Leysin.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
					addRapportPrestationImposable(dpi, pp, arrivee, null, false);
					ids.ppOrdinaire = pp.getNumero();
				}
				{
					final PersonnePhysique pp = addNonHabitant("Alastair ICCD", "O'Malley", date(1978, 5, 2), Sexe.MASCULIN);
					addForPrincipal(pp, achatImmeuble, MotifFor.ACHAT_IMMOBILIER, arrivee.getOneDayBefore(), MotifFor.ARRIVEE_HS, MockPays.Allemagne);
					addForPrincipal(pp, arrivee, MotifFor.ARRIVEE_HS, MockCommune.Aigle, ModeImposition.DEPENSE);
					addForSecondaire(pp, achatImmeuble, MotifFor.ACHAT_IMMOBILIER, MockCommune.Leysin.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
					addRapportPrestationImposable(dpi, pp, arrivee, null, false);
					ids.ppDepense = pp.getNumero();
				}
				{
					final PersonnePhysique pp = addNonHabitant("Alastair IND", "O'Malley", date(1978, 5, 2), Sexe.MASCULIN);
					addForPrincipal(pp, achatImmeuble, MotifFor.ACHAT_IMMOBILIER, arrivee.getOneDayBefore(), MotifFor.ARRIVEE_HS, MockPays.Allemagne);
					addForPrincipal(pp, arrivee, MotifFor.ARRIVEE_HS, MockCommune.Aigle, ModeImposition.INDIGENT);
					addForSecondaire(pp, achatImmeuble, MotifFor.ACHAT_IMMOBILIER, MockCommune.Leysin.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
					addRapportPrestationImposable(dpi, pp, arrivee, null, false);
					ids.ppIndigent = pp.getNumero();
				}
				return ids;
			}
		});

		final MutableLong testedId = new MutableLong();
		final Runnable test = new Runnable() {
			@Override
			public void run() {
				try {
					final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(testedId.longValue());
					final List<PeriodeImpositionImpotSource> piis = service.determine(pp);
					Assert.assertNotNull(piis);
					Assert.assertEquals(3, piis.size());
					{
						final PeriodeImpositionImpotSource pi = piis.get(0);
						Assert.assertNotNull(pi);
						Assert.assertEquals(PeriodeImpositionImpotSource.Type.SOURCE, pi.getType());
						Assert.assertEquals(TypeAutoriteFiscale.PAYS_HS, pi.getTypeAutoriteFiscale());
						Assert.assertEquals((Integer) MockPays.Allemagne.getNoOFS(), pi.getNoOfs());
						Assert.assertEquals(date(currentYear - 1, 1, 1), pi.getDateDebut());
						Assert.assertEquals(arrivee.getOneDayBefore(), pi.getDateFin());
						Assert.assertNotNull(pi.getContribuable());
						Assert.assertEquals((Long) testedId.longValue(), pi.getContribuable().getNumero());
					}
					{
						final PeriodeImpositionImpotSource pi = piis.get(1);
						Assert.assertNotNull(pi);
						Assert.assertEquals(PeriodeImpositionImpotSource.Type.MIXTE, pi.getType());
						Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, pi.getTypeAutoriteFiscale());
						Assert.assertEquals((Integer) MockCommune.Aigle.getNoOFS(), pi.getNoOfs());
						Assert.assertEquals(arrivee, pi.getDateDebut());
						Assert.assertEquals(date(currentYear - 1, 12, 31), pi.getDateFin());
						Assert.assertNotNull(pi.getContribuable());
						Assert.assertEquals((Long) testedId.longValue(), pi.getContribuable().getNumero());
					}
					{
						final PeriodeImpositionImpotSource pi = piis.get(2);
						Assert.assertNotNull(pi);
						Assert.assertEquals(PeriodeImpositionImpotSource.Type.MIXTE, pi.getType());
						Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, pi.getTypeAutoriteFiscale());
						Assert.assertEquals((Integer) MockCommune.Aigle.getNoOFS(), pi.getNoOfs());
						Assert.assertEquals(date(currentYear, 1, 1), pi.getDateDebut());
						Assert.assertEquals(date(currentYear, 12, 31), pi.getDateFin());
						Assert.assertNotNull(pi.getContribuable());
						Assert.assertEquals((Long) testedId.longValue(), pi.getContribuable().getNumero());
					}
				}
				catch (AssujettissementException e) {
					throw new RuntimeException(e);
				}
			}
		};

		// calcul
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				testedId.setValue(ids.ppOrdinaire);
				test.run();

				testedId.setValue(ids.ppDepense);
				test.run();

				testedId.setValue(ids.ppIndigent);
				test.run();
			}
		});
	}

	/**
	 * Cas 9 à 11 (ordinaires sans RT)
	 */
	@Test
	public void testArriveeVaudoiseDeHsImmeubleOrdinairesSansRT() throws Exception {

		final int currentYear = RegDate.get().year();
		final int firstYear = currentYear - 2;
		final RegDate achatImmeuble = date(firstYear, 3, 5);
		final RegDate arrivee = date(currentYear - 1, 6, 15);

		final class Ids {
			long ppOrdinaire;
			long ppDepense;
			long ppIndigent;
		}

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final Ids ids = new Ids();
				{
					final PersonnePhysique pp = addNonHabitant("Alastair ORD", "O'Malley", date(1978, 5, 2), Sexe.MASCULIN);
					addForPrincipal(pp, achatImmeuble, MotifFor.ACHAT_IMMOBILIER, arrivee.getOneDayBefore(), MotifFor.ARRIVEE_HS, MockPays.Allemagne);
					addForPrincipal(pp, arrivee, MotifFor.ARRIVEE_HS, MockCommune.Aigle, ModeImposition.ORDINAIRE);
					addForSecondaire(pp, achatImmeuble, MotifFor.ACHAT_IMMOBILIER, MockCommune.Leysin.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
					ids.ppOrdinaire = pp.getNumero();
				}
				{
					final PersonnePhysique pp = addNonHabitant("Alastair ICCD", "O'Malley", date(1978, 5, 2), Sexe.MASCULIN);
					addForPrincipal(pp, achatImmeuble, MotifFor.ACHAT_IMMOBILIER, arrivee.getOneDayBefore(), MotifFor.ARRIVEE_HS, MockPays.Allemagne);
					addForPrincipal(pp, arrivee, MotifFor.ARRIVEE_HS, MockCommune.Aigle, ModeImposition.DEPENSE);
					addForSecondaire(pp, achatImmeuble, MotifFor.ACHAT_IMMOBILIER, MockCommune.Leysin.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
					ids.ppDepense = pp.getNumero();
				}
				{
					final PersonnePhysique pp = addNonHabitant("Alastair IND", "O'Malley", date(1978, 5, 2), Sexe.MASCULIN);
					addForPrincipal(pp, achatImmeuble, MotifFor.ACHAT_IMMOBILIER, arrivee.getOneDayBefore(), MotifFor.ARRIVEE_HS, MockPays.Allemagne);
					addForPrincipal(pp, arrivee, MotifFor.ARRIVEE_HS, MockCommune.Aigle, ModeImposition.INDIGENT);
					addForSecondaire(pp, achatImmeuble, MotifFor.ACHAT_IMMOBILIER, MockCommune.Leysin.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
					ids.ppIndigent = pp.getNumero();
				}
				return ids;
			}
		});

		// calcul
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				{
					final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ids.ppOrdinaire);
					final List<PeriodeImpositionImpotSource> piis = service.determine(pp);
					Assert.assertNotNull(piis);
					Assert.assertEquals(0, piis.size());
				}
				{
					final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ids.ppDepense);
					final List<PeriodeImpositionImpotSource> piis = service.determine(pp);
					Assert.assertNotNull(piis);
					Assert.assertEquals(0, piis.size());
				}
				{
					final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ids.ppIndigent);
					final List<PeriodeImpositionImpotSource> piis = service.determine(pp);
					Assert.assertNotNull(piis);
					Assert.assertEquals(0, piis.size());
				}
			}
		});
	}

	/**
	 * Cas 12 & 13 (sans RT)
	 */
	@Test
	public void testDepartHsSansRattachementEconomiqueNiRT() throws Exception {

		final int currentYear = RegDate.get().year();
		final int lastYear = currentYear - 1;
		final RegDate depart = date(lastYear, 6, 15);

		final class Ids {
			long ppOrdinaire;
			long ppMixte1;
			long ppMixte2;
			long ppDepense;
			long ppIndigent;
		}

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final Ids ids = new Ids();
				{
					final PersonnePhysique pp = addNonHabitant("Alastair ORD", "O'Malley", date(1978, 5, 2), Sexe.MASCULIN);
					addForPrincipal(pp, date(lastYear, 1, 1), MotifFor.ARRIVEE_HS, depart, MotifFor.DEPART_HS, MockCommune.Aigle, ModeImposition.ORDINAIRE);
					addForPrincipal(pp, depart.getOneDayAfter(), MotifFor.DEPART_HS, null, null, MockPays.Allemagne, ModeImposition.SOURCE);
					ids.ppOrdinaire = pp.getNumero();
				}
				{
					final PersonnePhysique pp = addNonHabitant("Alastair M1", "O'Malley", date(1978, 5, 2), Sexe.MASCULIN);
					addForPrincipal(pp, date(lastYear, 1, 1), MotifFor.ARRIVEE_HS, depart, MotifFor.DEPART_HS, MockCommune.Aigle, ModeImposition.MIXTE_137_1);
					addForPrincipal(pp, depart.getOneDayAfter(), MotifFor.DEPART_HS, null, null, MockPays.Allemagne, ModeImposition.SOURCE);
					ids.ppMixte1 = pp.getNumero();
				}
				{
					final PersonnePhysique pp = addNonHabitant("Alastair M2", "O'Malley", date(1978, 5, 2), Sexe.MASCULIN);
					addForPrincipal(pp, date(lastYear, 1, 1), MotifFor.ARRIVEE_HS, depart, MotifFor.DEPART_HS, MockCommune.Aigle, ModeImposition.MIXTE_137_2);
					addForPrincipal(pp, depart.getOneDayAfter(), MotifFor.DEPART_HS, null, null, MockPays.Allemagne, ModeImposition.SOURCE);
					ids.ppMixte2 = pp.getNumero();
				}
				{
					final PersonnePhysique pp = addNonHabitant("Alastair ICCD", "O'Malley", date(1978, 5, 2), Sexe.MASCULIN);
					addForPrincipal(pp, date(lastYear, 1, 1), MotifFor.ARRIVEE_HS, depart, MotifFor.DEPART_HS, MockCommune.Aigle, ModeImposition.DEPENSE);
					addForPrincipal(pp, depart.getOneDayAfter(), MotifFor.DEPART_HS, null, null, MockPays.Allemagne, ModeImposition.SOURCE);
					ids.ppDepense = pp.getNumero();
				}
				{
					final PersonnePhysique pp = addNonHabitant("Alastair IND", "O'Malley", date(1978, 5, 2), Sexe.MASCULIN);
					addForPrincipal(pp, date(lastYear, 1, 1), MotifFor.ARRIVEE_HS, depart, MotifFor.DEPART_HS, MockCommune.Aigle, ModeImposition.INDIGENT);
					addForPrincipal(pp, depart.getOneDayAfter(), MotifFor.DEPART_HS, null, null, MockPays.Allemagne, ModeImposition.SOURCE);
					ids.ppIndigent = pp.getNumero();
				}
				return ids;
			}
		});

		final MutableLong testedId = new MutableLong();
		final Runnable test = new Runnable() {
			@Override
			public void run() {
				try {
					final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(testedId.longValue());
					final List<PeriodeImpositionImpotSource> piis = service.determine(pp);
					Assert.assertNotNull(piis);
					Assert.assertEquals(2, piis.size());
					{
						final PeriodeImpositionImpotSource pi = piis.get(0);
						Assert.assertNotNull(pi);
						Assert.assertEquals(PeriodeImpositionImpotSource.Type.MIXTE, pi.getType());
						Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, pi.getTypeAutoriteFiscale());
						Assert.assertEquals((Integer) MockCommune.Aigle.getNoOFS(), pi.getNoOfs());
						Assert.assertEquals(date(lastYear, 1, 1), pi.getDateDebut());
						Assert.assertEquals(depart, pi.getDateFin());
						Assert.assertNotNull(pi.getContribuable());
						Assert.assertEquals((Long) testedId.longValue(), pi.getContribuable().getNumero());
					}
					{
						final PeriodeImpositionImpotSource pi = piis.get(1);
						Assert.assertNotNull(pi);
						Assert.assertEquals(PeriodeImpositionImpotSource.Type.SOURCE, pi.getType());
						Assert.assertEquals(TypeAutoriteFiscale.PAYS_HS, pi.getTypeAutoriteFiscale());
						Assert.assertEquals((Integer) MockPays.Allemagne.getNoOFS(), pi.getNoOfs());
						Assert.assertEquals(depart.getOneDayAfter(), pi.getDateDebut());
						Assert.assertEquals(date(lastYear, 12, 31), pi.getDateFin());
						Assert.assertNotNull(pi.getContribuable());
						Assert.assertEquals((Long) testedId.longValue(), pi.getContribuable().getNumero());
					}
				}
				catch (AssujettissementException e) {
					throw new RuntimeException(e);
				}
			}
		};
		final Runnable testEmpty = new Runnable() {
			@Override
			public void run() {
				try {
					final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(testedId.longValue());
					assertNoPiis(pp);
				}
				catch (AssujettissementException e) {
					throw new RuntimeException(e);
				}
			}
		};

		// calcul
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				testedId.setValue(ids.ppOrdinaire);
				testEmpty.run();

				testedId.setValue(ids.ppMixte1);
				test.run();

				testedId.setValue(ids.ppMixte2);
				test.run();

				testedId.setValue(ids.ppDepense);
				testEmpty.run();

				testedId.setValue(ids.ppIndigent);
				testEmpty.run();
			}
		});
	}

	/**
	 * Cas 12 & 13 (avec RT pour les ordinaires)
	 */
	@Test
	public void testDepartHsSansRattachementEconomiqueOrdinairesAvecRT() throws Exception {

		final int currentYear = RegDate.get().year();
		final int lastYear = currentYear - 1;
		final RegDate depart = date(lastYear, 6, 15);

		final class Ids {
			long ppOrdinaire;
			long ppDepense;
			long ppIndigent;
		}

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final Ids ids = new Ids();
				final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.MENSUEL, date(2009, 1, 1));
				{
					final PersonnePhysique pp = addNonHabitant("Alastair ORD RT", "O'Malley", date(1978, 5, 2), Sexe.MASCULIN);
					addForPrincipal(pp, date(lastYear, 1, 1), MotifFor.ARRIVEE_HS, depart, MotifFor.DEPART_HS, MockCommune.Aigle, ModeImposition.ORDINAIRE);
					addForPrincipal(pp, depart.getOneDayAfter(), MotifFor.DEPART_HS, null, null, MockPays.Allemagne, ModeImposition.SOURCE);
					addRapportPrestationImposable(dpi, pp, date(lastYear, 1, 1), depart, false);
					ids.ppOrdinaire = pp.getNumero();
				}
				{
					final PersonnePhysique pp = addNonHabitant("Alastair ICCD RT", "O'Malley", date(1978, 5, 2), Sexe.MASCULIN);
					addForPrincipal(pp, date(lastYear, 1, 1), MotifFor.ARRIVEE_HS, depart, MotifFor.DEPART_HS, MockCommune.Aigle, ModeImposition.DEPENSE);
					addForPrincipal(pp, depart.getOneDayAfter(), MotifFor.DEPART_HS, null, null, MockPays.Allemagne, ModeImposition.SOURCE);
					addRapportPrestationImposable(dpi, pp, date(lastYear, 1, 1), depart, false);
					ids.ppDepense = pp.getNumero();
				}
				{
					final PersonnePhysique pp = addNonHabitant("Alastair IND RT", "O'Malley", date(1978, 5, 2), Sexe.MASCULIN);
					addForPrincipal(pp, date(lastYear, 1, 1), MotifFor.ARRIVEE_HS, depart, MotifFor.DEPART_HS, MockCommune.Aigle, ModeImposition.INDIGENT);
					addForPrincipal(pp, depart.getOneDayAfter(), MotifFor.DEPART_HS, null, null, MockPays.Allemagne, ModeImposition.SOURCE);
					addRapportPrestationImposable(dpi, pp, date(lastYear, 1, 1), depart, false);
					ids.ppIndigent = pp.getNumero();
				}
				return ids;
			}
		});

		final MutableLong testedId = new MutableLong();
		final Runnable test = new Runnable() {
			@Override
			public void run() {
				try {
					final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(testedId.longValue());
					final List<PeriodeImpositionImpotSource> piis = service.determine(pp);
					Assert.assertNotNull(piis);
					Assert.assertEquals(2, piis.size());
					{
						final PeriodeImpositionImpotSource pi = piis.get(0);
						Assert.assertNotNull(pi);
						Assert.assertEquals(PeriodeImpositionImpotSource.Type.MIXTE, pi.getType());
						Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, pi.getTypeAutoriteFiscale());
						Assert.assertEquals((Integer) MockCommune.Aigle.getNoOFS(), pi.getNoOfs());
						Assert.assertEquals(date(lastYear, 1, 1), pi.getDateDebut());
						Assert.assertEquals(depart, pi.getDateFin());
						Assert.assertNotNull(pi.getContribuable());
						Assert.assertEquals((Long) testedId.longValue(), pi.getContribuable().getNumero());
					}
					{
						final PeriodeImpositionImpotSource pi = piis.get(1);
						Assert.assertNotNull(pi);
						Assert.assertEquals(PeriodeImpositionImpotSource.Type.SOURCE, pi.getType());
						Assert.assertEquals(TypeAutoriteFiscale.PAYS_HS, pi.getTypeAutoriteFiscale());
						Assert.assertEquals((Integer) MockPays.Allemagne.getNoOFS(), pi.getNoOfs());
						Assert.assertEquals(depart.getOneDayAfter(), pi.getDateDebut());
						Assert.assertEquals(date(lastYear, 12, 31), pi.getDateFin());
						Assert.assertNotNull(pi.getContribuable());
						Assert.assertEquals((Long) testedId.longValue(), pi.getContribuable().getNumero());
					}
				}
				catch (AssujettissementException e) {
					throw new RuntimeException(e);
				}
			}
		};

		// calcul
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				testedId.setValue(ids.ppOrdinaire);
				test.run();

				testedId.setValue(ids.ppDepense);
				test.run();

				testedId.setValue(ids.ppIndigent);
				test.run();
			}
		});
	}

	/**
	 * Cas 14 à 16 (sans RT)
	 */
	@Test
	public void testDepartHsAvecRattachementEconomiqueSansRT() throws Exception {

		final int currentYear = RegDate.get().year();
		final int lastYear = currentYear - 1;
		final RegDate depart = date(lastYear, 6, 15);

		final class Ids {
			long ppOrdinaire;
			long ppMixte1;
			long ppMixte2;
			long ppDepense;
			long ppIndigent;
		}

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final Ids ids = new Ids();
				{
					final PersonnePhysique pp = addNonHabitant("Alastair ORD", "O'Malley", date(1978, 5, 2), Sexe.MASCULIN);
					addForPrincipal(pp, date(lastYear, 1, 1), MotifFor.ARRIVEE_HS, depart, MotifFor.DEPART_HS, MockCommune.Aigle, ModeImposition.ORDINAIRE);
					addForPrincipal(pp, depart.getOneDayAfter(), MotifFor.DEPART_HS, null, null, MockPays.Allemagne, ModeImposition.ORDINAIRE);
					addForSecondaire(pp, date(lastYear, 1, 1), MotifFor.ACHAT_IMMOBILIER, MockCommune.Leysin.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
					ids.ppOrdinaire = pp.getNumero();
				}
				{
					final PersonnePhysique pp = addNonHabitant("Alastair M1", "O'Malley", date(1978, 5, 2), Sexe.MASCULIN);
					addForPrincipal(pp, date(lastYear, 1, 1), MotifFor.ARRIVEE_HS, depart, MotifFor.DEPART_HS, MockCommune.Aigle, ModeImposition.MIXTE_137_1);
					addForPrincipal(pp, depart.getOneDayAfter(), MotifFor.DEPART_HS, null, null, MockPays.Allemagne, ModeImposition.ORDINAIRE);
					addForSecondaire(pp, date(lastYear, 1, 1), MotifFor.ACHAT_IMMOBILIER, MockCommune.Leysin.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
					ids.ppMixte1 = pp.getNumero();
				}
				{
					final PersonnePhysique pp = addNonHabitant("Alastair M2", "O'Malley", date(1978, 5, 2), Sexe.MASCULIN);
					addForPrincipal(pp, date(lastYear, 1, 1), MotifFor.ARRIVEE_HS, depart, MotifFor.DEPART_HS, MockCommune.Aigle, ModeImposition.MIXTE_137_2);
					addForPrincipal(pp, depart.getOneDayAfter(), MotifFor.DEPART_HS, null, null, MockPays.Allemagne, ModeImposition.ORDINAIRE);
					addForSecondaire(pp, date(lastYear, 1, 1), MotifFor.ACHAT_IMMOBILIER, MockCommune.Leysin.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
					ids.ppMixte2 = pp.getNumero();
				}
				{
					final PersonnePhysique pp = addNonHabitant("Alastair ICCD", "O'Malley", date(1978, 5, 2), Sexe.MASCULIN);
					addForPrincipal(pp, date(lastYear, 1, 1), MotifFor.ARRIVEE_HS, depart, MotifFor.DEPART_HS, MockCommune.Aigle, ModeImposition.DEPENSE);
					addForPrincipal(pp, depart.getOneDayAfter(), MotifFor.DEPART_HS, null, null, MockPays.Allemagne, ModeImposition.ORDINAIRE);
					addForSecondaire(pp, date(lastYear, 1, 1), MotifFor.ACHAT_IMMOBILIER, MockCommune.Leysin.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
					ids.ppDepense = pp.getNumero();
				}
				{
					final PersonnePhysique pp = addNonHabitant("Alastair IND", "O'Malley", date(1978, 5, 2), Sexe.MASCULIN);
					addForPrincipal(pp, date(lastYear, 1, 1), MotifFor.ARRIVEE_HS, depart, MotifFor.DEPART_HS, MockCommune.Aigle, ModeImposition.INDIGENT);
					addForPrincipal(pp, depart.getOneDayAfter(), MotifFor.DEPART_HS, null, null, MockPays.Allemagne, ModeImposition.ORDINAIRE);
					addForSecondaire(pp, date(lastYear, 1, 1), MotifFor.ACHAT_IMMOBILIER, MockCommune.Leysin.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
					ids.ppIndigent = pp.getNumero();
				}
				return ids;
			}
		});

		final MutableLong testedId = new MutableLong();
		final Runnable test = new Runnable() {
			@Override
			public void run() {
				try {
					final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(testedId.longValue());
					final List<PeriodeImpositionImpotSource> piis = service.determine(pp);
					Assert.assertNotNull(piis);
					Assert.assertEquals(2, piis.size());
					{
						final PeriodeImpositionImpotSource pi = piis.get(0);
						Assert.assertNotNull(pi);
						Assert.assertEquals(PeriodeImpositionImpotSource.Type.MIXTE, pi.getType());
						Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, pi.getTypeAutoriteFiscale());
						Assert.assertEquals((Integer) MockCommune.Aigle.getNoOFS(), pi.getNoOfs());
						Assert.assertEquals(date(lastYear, 1, 1), pi.getDateDebut());
						Assert.assertEquals(depart, pi.getDateFin());
						Assert.assertNotNull(pi.getContribuable());
						Assert.assertEquals((Long) testedId.longValue(), pi.getContribuable().getNumero());
					}
					{
						final PeriodeImpositionImpotSource pi = piis.get(1);
						Assert.assertNotNull(pi);
						Assert.assertEquals(PeriodeImpositionImpotSource.Type.SOURCE, pi.getType());
						Assert.assertEquals(TypeAutoriteFiscale.PAYS_HS, pi.getTypeAutoriteFiscale());
						Assert.assertEquals((Integer) MockPays.Allemagne.getNoOFS(), pi.getNoOfs());
						Assert.assertEquals(depart.getOneDayAfter(), pi.getDateDebut());
						Assert.assertEquals(date(lastYear, 12, 31), pi.getDateFin());
						Assert.assertNotNull(pi.getContribuable());
						Assert.assertEquals((Long) testedId.longValue(), pi.getContribuable().getNumero());
					}
				}
				catch (AssujettissementException e) {
					throw new RuntimeException(e);
				}
			}
		};
		final Runnable testEmpty = new Runnable() {
			@Override
			public void run() {
				try {
					final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(testedId.longValue());
					assertNoPiis(pp);
				}
				catch (AssujettissementException e) {
					throw new RuntimeException(e);
				}
			}
		};

		// calcul
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				testedId.setValue(ids.ppOrdinaire);
				testEmpty.run();

				testedId.setValue(ids.ppMixte1);
				test.run();

				testedId.setValue(ids.ppMixte2);
				test.run();

				testedId.setValue(ids.ppDepense);
				testEmpty.run();

				testedId.setValue(ids.ppIndigent);
				testEmpty.run();
			}
		});
	}

	/**
	 * Cas 14 à 16 (avec RT)
	 */
	@Test
	public void testDepartHsAvecRattachementEconomiqueAvecRT() throws Exception {

		final int currentYear = RegDate.get().year();
		final int lastYear = currentYear - 1;
		final RegDate depart = date(lastYear, 6, 15);

		final class Ids {
			long ppOrdinaire;
			long ppMixte1;
			long ppMixte2;
			long ppDepense;
			long ppIndigent;
		}

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.MENSUEL, date(2009, 1, 1));
				final Ids ids = new Ids();
				{
					final PersonnePhysique pp = addNonHabitant("Alastair ORD", "O'Malley", date(1978, 5, 2), Sexe.MASCULIN);
					addForPrincipal(pp, date(lastYear, 1, 1), MotifFor.ARRIVEE_HS, depart, MotifFor.DEPART_HS, MockCommune.Aigle, ModeImposition.ORDINAIRE);
					addForPrincipal(pp, depart.getOneDayAfter(), MotifFor.DEPART_HS, null, null, MockPays.Allemagne, ModeImposition.ORDINAIRE);
					addForSecondaire(pp, date(lastYear, 1, 1), MotifFor.ACHAT_IMMOBILIER, MockCommune.Leysin.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
					addRapportPrestationImposable(dpi, pp, date(lastYear, 1, 1), depart, false);
					ids.ppOrdinaire = pp.getNumero();
				}
				{
					final PersonnePhysique pp = addNonHabitant("Alastair M1", "O'Malley", date(1978, 5, 2), Sexe.MASCULIN);
					addForPrincipal(pp, date(lastYear, 1, 1), MotifFor.ARRIVEE_HS, depart, MotifFor.DEPART_HS, MockCommune.Aigle, ModeImposition.MIXTE_137_1);
					addForPrincipal(pp, depart.getOneDayAfter(), MotifFor.DEPART_HS, null, null, MockPays.Allemagne, ModeImposition.ORDINAIRE);
					addForSecondaire(pp, date(lastYear, 1, 1), MotifFor.ACHAT_IMMOBILIER, MockCommune.Leysin.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
					addRapportPrestationImposable(dpi, pp, date(lastYear, 1, 1), depart, false);
					ids.ppMixte1 = pp.getNumero();
				}
				{
					final PersonnePhysique pp = addNonHabitant("Alastair M2", "O'Malley", date(1978, 5, 2), Sexe.MASCULIN);
					addForPrincipal(pp, date(lastYear, 1, 1), MotifFor.ARRIVEE_HS, depart, MotifFor.DEPART_HS, MockCommune.Aigle, ModeImposition.MIXTE_137_2);
					addForPrincipal(pp, depart.getOneDayAfter(), MotifFor.DEPART_HS, null, null, MockPays.Allemagne, ModeImposition.ORDINAIRE);
					addForSecondaire(pp, date(lastYear, 1, 1), MotifFor.ACHAT_IMMOBILIER, MockCommune.Leysin.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
					addRapportPrestationImposable(dpi, pp, date(lastYear, 1, 1), depart, false);
					ids.ppMixte2 = pp.getNumero();
				}
				{
					final PersonnePhysique pp = addNonHabitant("Alastair ICCD", "O'Malley", date(1978, 5, 2), Sexe.MASCULIN);
					addForPrincipal(pp, date(lastYear, 1, 1), MotifFor.ARRIVEE_HS, depart, MotifFor.DEPART_HS, MockCommune.Aigle, ModeImposition.DEPENSE);
					addForPrincipal(pp, depart.getOneDayAfter(), MotifFor.DEPART_HS, null, null, MockPays.Allemagne, ModeImposition.ORDINAIRE);
					addForSecondaire(pp, date(lastYear, 1, 1), MotifFor.ACHAT_IMMOBILIER, MockCommune.Leysin.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
					addRapportPrestationImposable(dpi, pp, date(lastYear, 1, 1), depart, false);
					ids.ppDepense = pp.getNumero();
				}
				{
					final PersonnePhysique pp = addNonHabitant("Alastair IND", "O'Malley", date(1978, 5, 2), Sexe.MASCULIN);
					addForPrincipal(pp, date(lastYear, 1, 1), MotifFor.ARRIVEE_HS, depart, MotifFor.DEPART_HS, MockCommune.Aigle, ModeImposition.INDIGENT);
					addForPrincipal(pp, depart.getOneDayAfter(), MotifFor.DEPART_HS, null, null, MockPays.Allemagne, ModeImposition.ORDINAIRE);
					addForSecondaire(pp, date(lastYear, 1, 1), MotifFor.ACHAT_IMMOBILIER, MockCommune.Leysin.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
					addRapportPrestationImposable(dpi, pp, date(lastYear, 1, 1), depart, false);
					ids.ppIndigent = pp.getNumero();
				}
				return ids;
			}
		});

		final MutableLong testedId = new MutableLong();
		final Runnable test = new Runnable() {
			@Override
			public void run() {
				try {
					final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(testedId.longValue());
					final List<PeriodeImpositionImpotSource> piis = service.determine(pp);
					Assert.assertNotNull(piis);
					Assert.assertEquals(2, piis.size());
					{
						final PeriodeImpositionImpotSource pi = piis.get(0);
						Assert.assertNotNull(pi);
						Assert.assertEquals(PeriodeImpositionImpotSource.Type.MIXTE, pi.getType());
						Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, pi.getTypeAutoriteFiscale());
						Assert.assertEquals((Integer) MockCommune.Aigle.getNoOFS(), pi.getNoOfs());
						Assert.assertEquals(date(lastYear, 1, 1), pi.getDateDebut());
						Assert.assertEquals(depart, pi.getDateFin());
						Assert.assertNotNull(pi.getContribuable());
						Assert.assertEquals((Long) testedId.longValue(), pi.getContribuable().getNumero());
					}
					{
						final PeriodeImpositionImpotSource pi = piis.get(1);
						Assert.assertNotNull(pi);
						Assert.assertEquals(PeriodeImpositionImpotSource.Type.SOURCE, pi.getType());
						Assert.assertEquals(TypeAutoriteFiscale.PAYS_HS, pi.getTypeAutoriteFiscale());
						Assert.assertEquals((Integer) MockPays.Allemagne.getNoOFS(), pi.getNoOfs());
						Assert.assertEquals(depart.getOneDayAfter(), pi.getDateDebut());
						Assert.assertEquals(date(lastYear, 12, 31), pi.getDateFin());
						Assert.assertNotNull(pi.getContribuable());
						Assert.assertEquals((Long) testedId.longValue(), pi.getContribuable().getNumero());
					}
				}
				catch (AssujettissementException e) {
					throw new RuntimeException(e);
				}
			}
		};

		// calcul
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				testedId.setValue(ids.ppOrdinaire);
				test.run();

				testedId.setValue(ids.ppMixte1);
				test.run();

				testedId.setValue(ids.ppMixte2);
				test.run();

				testedId.setValue(ids.ppDepense);
				test.run();

				testedId.setValue(ids.ppIndigent);
				test.run();
			}
		});
	}

	/**
	 * Cas 17 & 18 (sans RT)
	 */
	@Test
	public void testArriveeHcSansRattachementEconomiquePrealableNiRT() throws Exception {

		final int currentYear = RegDate.get().year();
		final int lastYear = currentYear - 1;
		final RegDate arrivee = date(lastYear, 6, 15);

		final class Ids {
			long ppOrdinaire;
			long ppMixte1;
			long ppMixte2;
			long ppDepense;
			long ppIndigent;
		}

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final Ids ids = new Ids();
				{
					final PersonnePhysique pp = addNonHabitant("Alastair ORD", "O'Malley", date(1978, 5, 2), Sexe.MASCULIN);
					addForPrincipal(pp, arrivee, MotifFor.ARRIVEE_HC, MockCommune.Aigle, ModeImposition.ORDINAIRE);
					ids.ppOrdinaire = pp.getNumero();
				}
				{
					final PersonnePhysique pp = addNonHabitant("Alastair M1", "O'Malley", date(1978, 5, 2), Sexe.MASCULIN);
					addForPrincipal(pp, arrivee, MotifFor.ARRIVEE_HC, MockCommune.Aigle, ModeImposition.MIXTE_137_1);
					ids.ppMixte1 = pp.getNumero();
				}
				{
					final PersonnePhysique pp = addNonHabitant("Alastair M2", "O'Malley", date(1978, 5, 2), Sexe.MASCULIN);
					addForPrincipal(pp, arrivee, MotifFor.ARRIVEE_HC, MockCommune.Aigle, ModeImposition.MIXTE_137_2);
					ids.ppMixte2 = pp.getNumero();
				}
				{
					final PersonnePhysique pp = addNonHabitant("Alastair ICCD", "O'Malley", date(1978, 5, 2), Sexe.MASCULIN);
					addForPrincipal(pp, arrivee, MotifFor.ARRIVEE_HC, MockCommune.Aigle, ModeImposition.DEPENSE);
					ids.ppDepense = pp.getNumero();
				}
				{
					final PersonnePhysique pp = addNonHabitant("Alastair IND", "O'Malley", date(1978, 5, 2), Sexe.MASCULIN);
					addForPrincipal(pp, arrivee, MotifFor.ARRIVEE_HC, MockCommune.Aigle, ModeImposition.INDIGENT);
					ids.ppIndigent = pp.getNumero();
				}
				return ids;
			}
		});

		final MutableLong testedId = new MutableLong();
		final Runnable test = new Runnable() {
			@Override
			public void run() {
				try {
					final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(testedId.longValue());
					final List<PeriodeImpositionImpotSource> piis = service.determine(pp);
					Assert.assertNotNull(piis);
					Assert.assertEquals(3, piis.size());
					{
						final PeriodeImpositionImpotSource pi = piis.get(0);
						Assert.assertNotNull(pi);
						Assert.assertEquals(PeriodeImpositionImpotSource.Type.SOURCE, pi.getType());
						Assert.assertNull(pi.getTypeAutoriteFiscale());
						Assert.assertNull(pi.getNoOfs());
						Assert.assertEquals(date(lastYear, 1, 1), pi.getDateDebut());
						Assert.assertEquals(arrivee.getLastDayOfTheMonth(), pi.getDateFin());
						Assert.assertNotNull(pi.getContribuable());
						Assert.assertEquals((Long) testedId.longValue(), pi.getContribuable().getNumero());
					}
					{
						final PeriodeImpositionImpotSource pi = piis.get(1);
						Assert.assertNotNull(pi);
						Assert.assertEquals(PeriodeImpositionImpotSource.Type.MIXTE, pi.getType());
						Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, pi.getTypeAutoriteFiscale());
						Assert.assertEquals((Integer) MockCommune.Aigle.getNoOFS(), pi.getNoOfs());
						Assert.assertEquals(arrivee.getLastDayOfTheMonth().getOneDayAfter(), pi.getDateDebut());
						Assert.assertEquals(date(lastYear, 12, 31), pi.getDateFin());
						Assert.assertNotNull(pi.getContribuable());
						Assert.assertEquals((Long) testedId.longValue(), pi.getContribuable().getNumero());
					}
					{
						final PeriodeImpositionImpotSource pi = piis.get(2);
						Assert.assertNotNull(pi);
						Assert.assertEquals(PeriodeImpositionImpotSource.Type.MIXTE, pi.getType());
						Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, pi.getTypeAutoriteFiscale());
						Assert.assertEquals((Integer) MockCommune.Aigle.getNoOFS(), pi.getNoOfs());
						Assert.assertEquals(date(currentYear, 1, 1), pi.getDateDebut());
						Assert.assertEquals(date(currentYear, 12, 31), pi.getDateFin());
						Assert.assertNotNull(pi.getContribuable());
						Assert.assertEquals((Long) testedId.longValue(), pi.getContribuable().getNumero());
					}
				}
				catch (AssujettissementException e) {
					throw new RuntimeException(e);
				}
			}
		};
		final Runnable testEmpty = new Runnable() {
			@Override
			public void run() {
				try {
					final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(testedId.longValue());
					assertNoPiis(pp);
				}
				catch (AssujettissementException e) {
					throw new RuntimeException(e);
				}
			}
		};

		// calcul
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				testedId.setValue(ids.ppOrdinaire);
				testEmpty.run();

				testedId.setValue(ids.ppMixte1);
				test.run();

				testedId.setValue(ids.ppMixte2);
				test.run();

				testedId.setValue(ids.ppDepense);
				testEmpty.run();

				testedId.setValue(ids.ppIndigent);
				testEmpty.run();
			}
		});
	}

	/**
	 * Cas 17 & 18 (avec RT)
	 */
	@Test
	public void testArriveeHcSansRattachementEconomiquePrealableAvecRT() throws Exception {

		final int currentYear = RegDate.get().year();
		final int lastYear = currentYear - 1;
		final RegDate arrivee = date(lastYear, 6, 15);

		final class Ids {
			long ppOrdinaire;
			long ppMixte1;
			long ppMixte2;
			long ppDepense;
			long ppIndigent;
		}

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final Ids ids = new Ids();
				final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.MENSUEL, date(2009, 1, 1));
				{
					final PersonnePhysique pp = addNonHabitant("Alastair ORD", "O'Malley", date(1978, 5, 2), Sexe.MASCULIN);
					addForPrincipal(pp, arrivee, MotifFor.ARRIVEE_HC, MockCommune.Aigle, ModeImposition.ORDINAIRE);
					addRapportPrestationImposable(dpi, pp, arrivee, null, false);
					ids.ppOrdinaire = pp.getNumero();
				}
				{
					final PersonnePhysique pp = addNonHabitant("Alastair M1", "O'Malley", date(1978, 5, 2), Sexe.MASCULIN);
					addForPrincipal(pp, arrivee, MotifFor.ARRIVEE_HC, MockCommune.Aigle, ModeImposition.MIXTE_137_1);
					addRapportPrestationImposable(dpi, pp, arrivee, null, false);
					ids.ppMixte1 = pp.getNumero();
				}
				{
					final PersonnePhysique pp = addNonHabitant("Alastair M2", "O'Malley", date(1978, 5, 2), Sexe.MASCULIN);
					addForPrincipal(pp, arrivee, MotifFor.ARRIVEE_HC, MockCommune.Aigle, ModeImposition.MIXTE_137_2);
					addRapportPrestationImposable(dpi, pp, arrivee, null, false);
					ids.ppMixte2 = pp.getNumero();
				}
				{
					final PersonnePhysique pp = addNonHabitant("Alastair ICCD", "O'Malley", date(1978, 5, 2), Sexe.MASCULIN);
					addForPrincipal(pp, arrivee, MotifFor.ARRIVEE_HC, MockCommune.Aigle, ModeImposition.DEPENSE);
					addRapportPrestationImposable(dpi, pp, arrivee, null, false);
					ids.ppDepense = pp.getNumero();
				}
				{
					final PersonnePhysique pp = addNonHabitant("Alastair IND", "O'Malley", date(1978, 5, 2), Sexe.MASCULIN);
					addForPrincipal(pp, arrivee, MotifFor.ARRIVEE_HC, MockCommune.Aigle, ModeImposition.INDIGENT);
					addRapportPrestationImposable(dpi, pp, arrivee, null, false);
					ids.ppIndigent = pp.getNumero();
				}
				return ids;
			}
		});

		final MutableLong testedId = new MutableLong();
		final Runnable test = new Runnable() {
			@Override
			public void run() {
				try {
					final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(testedId.longValue());
					final List<PeriodeImpositionImpotSource> piis = service.determine(pp);
					Assert.assertNotNull(piis);
					Assert.assertEquals(3, piis.size());
					{
						final PeriodeImpositionImpotSource pi = piis.get(0);
						Assert.assertNotNull(pi);
						Assert.assertEquals(PeriodeImpositionImpotSource.Type.SOURCE, pi.getType());
						Assert.assertNull(pi.getTypeAutoriteFiscale());
						Assert.assertNull(pi.getNoOfs());
						Assert.assertEquals(date(lastYear, 1, 1), pi.getDateDebut());
						Assert.assertEquals(arrivee.getLastDayOfTheMonth(), pi.getDateFin());
						Assert.assertNotNull(pi.getContribuable());
						Assert.assertEquals((Long) testedId.longValue(), pi.getContribuable().getNumero());
					}
					{
						final PeriodeImpositionImpotSource pi = piis.get(1);
						Assert.assertNotNull(pi);
						Assert.assertEquals(PeriodeImpositionImpotSource.Type.MIXTE, pi.getType());
						Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, pi.getTypeAutoriteFiscale());
						Assert.assertEquals((Integer) MockCommune.Aigle.getNoOFS(), pi.getNoOfs());
						Assert.assertEquals(arrivee.getLastDayOfTheMonth().getOneDayAfter(), pi.getDateDebut());
						Assert.assertEquals(date(lastYear, 12, 31), pi.getDateFin());
						Assert.assertNotNull(pi.getContribuable());
						Assert.assertEquals((Long) testedId.longValue(), pi.getContribuable().getNumero());
					}
					{
						final PeriodeImpositionImpotSource pi = piis.get(2);
						Assert.assertNotNull(pi);
						Assert.assertEquals(PeriodeImpositionImpotSource.Type.MIXTE, pi.getType());
						Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, pi.getTypeAutoriteFiscale());
						Assert.assertEquals((Integer) MockCommune.Aigle.getNoOFS(), pi.getNoOfs());
						Assert.assertEquals(date(currentYear, 1, 1), pi.getDateDebut());
						Assert.assertEquals(date(currentYear, 12, 31), pi.getDateFin());
						Assert.assertNotNull(pi.getContribuable());
						Assert.assertEquals((Long) testedId.longValue(), pi.getContribuable().getNumero());
					}
				}
				catch (AssujettissementException e) {
					throw new RuntimeException(e);
				}
			}
		};

		// calcul
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				testedId.setValue(ids.ppOrdinaire);
				test.run();

				testedId.setValue(ids.ppMixte1);
				test.run();

				testedId.setValue(ids.ppMixte2);
				test.run();

				testedId.setValue(ids.ppDepense);
				test.run();

				testedId.setValue(ids.ppIndigent);
				test.run();
			}
		});
	}

	/**
	 * Cas 19 (sans RT)
	 */
	@Test
	public void testArriveeHcAvecRattachementEconomiquePrealableSansRT() throws Exception {

		final int currentYear = RegDate.get().year();
		final int lastYear = currentYear - 1;
		final RegDate achat = date(lastYear, 1, 1);
		final RegDate arrivee = date(lastYear, 6, 15);

		final class Ids {
			long ppOrdinaire;
			long ppMixte1;
			long ppMixte2;
			long ppDepense;
			long ppIndigent;
		}

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final Ids ids = new Ids();
				{
					final PersonnePhysique pp = addNonHabitant("Alastair ORD", "O'Malley", date(1978, 5, 2), Sexe.MASCULIN);
					addForPrincipal(pp, achat, MotifFor.ACHAT_IMMOBILIER, arrivee.getOneDayBefore(), MotifFor.ARRIVEE_HC, MockCommune.Bern, ModeImposition.ORDINAIRE);
					addForPrincipal(pp, arrivee, MotifFor.ARRIVEE_HC, MockCommune.Aigle, ModeImposition.ORDINAIRE);
					addForSecondaire(pp, achat, MotifFor.ACHAT_IMMOBILIER, MockCommune.Leysin.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
					ids.ppOrdinaire = pp.getNumero();
				}
				{
					final PersonnePhysique pp = addNonHabitant("Alastair M1", "O'Malley", date(1978, 5, 2), Sexe.MASCULIN);
					addForPrincipal(pp, achat, MotifFor.ACHAT_IMMOBILIER, arrivee.getOneDayBefore(), MotifFor.ARRIVEE_HC, MockCommune.Bern, ModeImposition.ORDINAIRE);
					addForPrincipal(pp, arrivee, MotifFor.ARRIVEE_HC, MockCommune.Aigle, ModeImposition.MIXTE_137_1);
					addForSecondaire(pp, achat, MotifFor.ACHAT_IMMOBILIER, MockCommune.Leysin.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
					ids.ppMixte1 = pp.getNumero();
				}
				{
					final PersonnePhysique pp = addNonHabitant("Alastair M2", "O'Malley", date(1978, 5, 2), Sexe.MASCULIN);
					addForPrincipal(pp, achat, MotifFor.ACHAT_IMMOBILIER, arrivee.getOneDayBefore(), MotifFor.ARRIVEE_HC, MockCommune.Bern, ModeImposition.ORDINAIRE);
					addForPrincipal(pp, arrivee, MotifFor.ARRIVEE_HC, MockCommune.Aigle, ModeImposition.MIXTE_137_2);
					addForSecondaire(pp, achat, MotifFor.ACHAT_IMMOBILIER, MockCommune.Leysin.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
					ids.ppMixte2 = pp.getNumero();
				}
				{
					final PersonnePhysique pp = addNonHabitant("Alastair ICCD", "O'Malley", date(1978, 5, 2), Sexe.MASCULIN);
					addForPrincipal(pp, achat, MotifFor.ACHAT_IMMOBILIER, arrivee.getOneDayBefore(), MotifFor.ARRIVEE_HC, MockCommune.Bern, ModeImposition.ORDINAIRE);
					addForPrincipal(pp, arrivee, MotifFor.ARRIVEE_HC, MockCommune.Aigle, ModeImposition.DEPENSE);
					addForSecondaire(pp, achat, MotifFor.ACHAT_IMMOBILIER, MockCommune.Leysin.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
					ids.ppDepense = pp.getNumero();
				}
				{
					final PersonnePhysique pp = addNonHabitant("Alastair IND", "O'Malley", date(1978, 5, 2), Sexe.MASCULIN);
					addForPrincipal(pp, achat, MotifFor.ACHAT_IMMOBILIER, arrivee.getOneDayBefore(), MotifFor.ARRIVEE_HC, MockCommune.Bern, ModeImposition.ORDINAIRE);
					addForPrincipal(pp, arrivee, MotifFor.ARRIVEE_HC, MockCommune.Aigle, ModeImposition.INDIGENT);
					addForSecondaire(pp, achat, MotifFor.ACHAT_IMMOBILIER, MockCommune.Leysin.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
					ids.ppIndigent = pp.getNumero();
				}
				return ids;
			}
		});

		final MutableLong testedId = new MutableLong();
		final Runnable test = new Runnable() {
			@Override
			public void run() {
				try {
					final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(testedId.longValue());
					final List<PeriodeImpositionImpotSource> piis = service.determine(pp);
					Assert.assertNotNull(piis);
					Assert.assertEquals(3, piis.size());
					{
						final PeriodeImpositionImpotSource pi = piis.get(0);
						Assert.assertNotNull(pi);
						Assert.assertEquals(PeriodeImpositionImpotSource.Type.SOURCE, pi.getType());
						Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_HC, pi.getTypeAutoriteFiscale());
						Assert.assertEquals((Integer) MockCommune.Bern.getNoOFS(), pi.getNoOfs());
						Assert.assertEquals(date(lastYear, 1, 1), pi.getDateDebut());
						Assert.assertEquals(arrivee.getLastDayOfTheMonth(), pi.getDateFin());
						Assert.assertNotNull(pi.getContribuable());
						Assert.assertEquals((Long) testedId.longValue(), pi.getContribuable().getNumero());
					}
					{
						final PeriodeImpositionImpotSource pi = piis.get(1);
						Assert.assertNotNull(pi);
						Assert.assertEquals(PeriodeImpositionImpotSource.Type.MIXTE, pi.getType());
						Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, pi.getTypeAutoriteFiscale());
						Assert.assertEquals((Integer) MockCommune.Aigle.getNoOFS(), pi.getNoOfs());
						Assert.assertEquals(arrivee.getLastDayOfTheMonth().getOneDayAfter(), pi.getDateDebut());
						Assert.assertEquals(date(lastYear, 12, 31), pi.getDateFin());
						Assert.assertNotNull(pi.getContribuable());
						Assert.assertEquals((Long) testedId.longValue(), pi.getContribuable().getNumero());
					}
					{
						final PeriodeImpositionImpotSource pi = piis.get(2);
						Assert.assertNotNull(pi);
						Assert.assertEquals(PeriodeImpositionImpotSource.Type.MIXTE, pi.getType());
						Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, pi.getTypeAutoriteFiscale());
						Assert.assertEquals((Integer) MockCommune.Aigle.getNoOFS(), pi.getNoOfs());
						Assert.assertEquals(date(currentYear, 1, 1), pi.getDateDebut());
						Assert.assertEquals(date(currentYear, 12, 31), pi.getDateFin());
						Assert.assertNotNull(pi.getContribuable());
						Assert.assertEquals((Long) testedId.longValue(), pi.getContribuable().getNumero());
					}
				}
				catch (AssujettissementException e) {
					throw new RuntimeException(e);
				}
			}
		};
		final Runnable testEmpty = new Runnable() {
			@Override
			public void run() {
				try {
					final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(testedId.longValue());
					assertNoPiis(pp);
				}
				catch (AssujettissementException e) {
					throw new RuntimeException(e);
				}
			}
		};

		// calcul
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				testedId.setValue(ids.ppOrdinaire);
				testEmpty.run();

				testedId.setValue(ids.ppMixte1);
				test.run();

				testedId.setValue(ids.ppMixte2);
				test.run();

				testedId.setValue(ids.ppDepense);
				testEmpty.run();

				testedId.setValue(ids.ppIndigent);
				testEmpty.run();
			}
		});
	}

	/**
	 * Cas 19 (avec RT)
	 */
	@Test
	public void testArriveeHcAvecRattachementEconomiquePrealableAvecRT() throws Exception {

		final int currentYear = RegDate.get().year();
		final int lastYear = currentYear - 1;
		final RegDate achat = date(lastYear, 1, 1);
		final RegDate arrivee = date(lastYear, 6, 15);

		final class Ids {
			long ppOrdinaire;
			long ppMixte1;
			long ppMixte2;
			long ppDepense;
			long ppIndigent;
		}

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final Ids ids = new Ids();
				final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.MENSUEL, date(2009, 1, 1));
				{
					final PersonnePhysique pp = addNonHabitant("Alastair ORD", "O'Malley", date(1978, 5, 2), Sexe.MASCULIN);
					addForPrincipal(pp, achat, MotifFor.ACHAT_IMMOBILIER, arrivee.getOneDayBefore(), MotifFor.ARRIVEE_HC, MockCommune.Bern, ModeImposition.ORDINAIRE);
					addForPrincipal(pp, arrivee, MotifFor.ARRIVEE_HC, MockCommune.Aigle, ModeImposition.ORDINAIRE);
					addForSecondaire(pp, achat, MotifFor.ACHAT_IMMOBILIER, MockCommune.Leysin.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
					addRapportPrestationImposable(dpi, pp, arrivee, null, false);
					ids.ppOrdinaire = pp.getNumero();
				}
				{
					final PersonnePhysique pp = addNonHabitant("Alastair M1", "O'Malley", date(1978, 5, 2), Sexe.MASCULIN);
					addForPrincipal(pp, achat, MotifFor.ACHAT_IMMOBILIER, arrivee.getOneDayBefore(), MotifFor.ARRIVEE_HC, MockCommune.Bern, ModeImposition.ORDINAIRE);
					addForPrincipal(pp, arrivee, MotifFor.ARRIVEE_HC, MockCommune.Aigle, ModeImposition.MIXTE_137_1);
					addForSecondaire(pp, achat, MotifFor.ACHAT_IMMOBILIER, MockCommune.Leysin.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
					addRapportPrestationImposable(dpi, pp, arrivee, null, false);
					ids.ppMixte1 = pp.getNumero();
				}
				{
					final PersonnePhysique pp = addNonHabitant("Alastair M2", "O'Malley", date(1978, 5, 2), Sexe.MASCULIN);
					addForPrincipal(pp, achat, MotifFor.ACHAT_IMMOBILIER, arrivee.getOneDayBefore(), MotifFor.ARRIVEE_HC, MockCommune.Bern, ModeImposition.ORDINAIRE);
					addForPrincipal(pp, arrivee, MotifFor.ARRIVEE_HC, MockCommune.Aigle, ModeImposition.MIXTE_137_2);
					addForSecondaire(pp, achat, MotifFor.ACHAT_IMMOBILIER, MockCommune.Leysin.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
					addRapportPrestationImposable(dpi, pp, arrivee, null, false);
					ids.ppMixte2 = pp.getNumero();
				}
				{
					final PersonnePhysique pp = addNonHabitant("Alastair ICCD", "O'Malley", date(1978, 5, 2), Sexe.MASCULIN);
					addForPrincipal(pp, achat, MotifFor.ACHAT_IMMOBILIER, arrivee.getOneDayBefore(), MotifFor.ARRIVEE_HC, MockCommune.Bern, ModeImposition.ORDINAIRE);
					addForPrincipal(pp, arrivee, MotifFor.ARRIVEE_HC, MockCommune.Aigle, ModeImposition.DEPENSE);
					addForSecondaire(pp, achat, MotifFor.ACHAT_IMMOBILIER, MockCommune.Leysin.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
					addRapportPrestationImposable(dpi, pp, arrivee, null, false);
					ids.ppDepense = pp.getNumero();
				}
				{
					final PersonnePhysique pp = addNonHabitant("Alastair IND", "O'Malley", date(1978, 5, 2), Sexe.MASCULIN);
					addForPrincipal(pp, achat, MotifFor.ACHAT_IMMOBILIER, arrivee.getOneDayBefore(), MotifFor.ARRIVEE_HC, MockCommune.Bern, ModeImposition.ORDINAIRE);
					addForPrincipal(pp, arrivee, MotifFor.ARRIVEE_HC, MockCommune.Aigle, ModeImposition.INDIGENT);
					addForSecondaire(pp, achat, MotifFor.ACHAT_IMMOBILIER, MockCommune.Leysin.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
					addRapportPrestationImposable(dpi, pp, arrivee, null, false);
					ids.ppIndigent = pp.getNumero();
				}
				return ids;
			}
		});

		final MutableLong testedId = new MutableLong();
		final Runnable test = new Runnable() {
			@Override
			public void run() {
				try {
					final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(testedId.longValue());
					final List<PeriodeImpositionImpotSource> piis = service.determine(pp);
					Assert.assertNotNull(piis);
					Assert.assertEquals(3, piis.size());
					{
						final PeriodeImpositionImpotSource pi = piis.get(0);
						Assert.assertNotNull(pi);
						Assert.assertEquals(PeriodeImpositionImpotSource.Type.SOURCE, pi.getType());
						Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_HC, pi.getTypeAutoriteFiscale());
						Assert.assertEquals((Integer) MockCommune.Bern.getNoOFS(), pi.getNoOfs());
						Assert.assertEquals(date(lastYear, 1, 1), pi.getDateDebut());
						Assert.assertEquals(arrivee.getLastDayOfTheMonth(), pi.getDateFin());
						Assert.assertNotNull(pi.getContribuable());
						Assert.assertEquals((Long) testedId.longValue(), pi.getContribuable().getNumero());
					}
					{
						final PeriodeImpositionImpotSource pi = piis.get(1);
						Assert.assertNotNull(pi);
						Assert.assertEquals(PeriodeImpositionImpotSource.Type.MIXTE, pi.getType());
						Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, pi.getTypeAutoriteFiscale());
						Assert.assertEquals((Integer) MockCommune.Aigle.getNoOFS(), pi.getNoOfs());
						Assert.assertEquals(arrivee.getLastDayOfTheMonth().getOneDayAfter(), pi.getDateDebut());
						Assert.assertEquals(date(lastYear, 12, 31), pi.getDateFin());
						Assert.assertNotNull(pi.getContribuable());
						Assert.assertEquals((Long) testedId.longValue(), pi.getContribuable().getNumero());
					}
					{
						final PeriodeImpositionImpotSource pi = piis.get(2);
						Assert.assertNotNull(pi);
						Assert.assertEquals(PeriodeImpositionImpotSource.Type.MIXTE, pi.getType());
						Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, pi.getTypeAutoriteFiscale());
						Assert.assertEquals((Integer) MockCommune.Aigle.getNoOFS(), pi.getNoOfs());
						Assert.assertEquals(date(currentYear, 1, 1), pi.getDateDebut());
						Assert.assertEquals(date(currentYear, 12, 31), pi.getDateFin());
						Assert.assertNotNull(pi.getContribuable());
						Assert.assertEquals((Long) testedId.longValue(), pi.getContribuable().getNumero());
					}
				}
				catch (AssujettissementException e) {
					throw new RuntimeException(e);
				}
			}
		};

		// calcul
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				testedId.setValue(ids.ppOrdinaire);
				test.run();

				testedId.setValue(ids.ppMixte1);
				test.run();

				testedId.setValue(ids.ppMixte2);
				test.run();

				testedId.setValue(ids.ppDepense);
				test.run();

				testedId.setValue(ids.ppIndigent);
				test.run();
			}
		});
	}

	/**
	 * Cas 20 et 23 (avant 2014)
	 */
	@Test
	public void testDepartHorsCantonMixte2Avant2014() throws Exception {

		final int year = 2013;
		final RegDate depart = date(year, 6, 23);

		final class Ids {
			long ppSrc;
			long ppOrd;
		}

		ForFiscalValidator.setFutureBeginDate(date(year, 12, 31));
		try {
			// mise en place fiscale
			final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
				@Override
				public Ids doInTransaction(TransactionStatus status) {
					final Ids ids = new Ids();
					{
						final PersonnePhysique pp = addNonHabitant("Iain", "Kentucky", date(1980, 5, 3), Sexe.MASCULIN);
						addForPrincipal(pp, date(year, 1, 1), MotifFor.INDETERMINE, depart, MotifFor.DEPART_HC, MockCommune.Aubonne, ModeImposition.MIXTE_137_2);
						addForPrincipal(pp, depart.getOneDayAfter(), MotifFor.DEPART_HC, date(year, 12, 31), MotifFor.INDETERMINE, MockCommune.Bern, ModeImposition.SOURCE);
						ids.ppSrc = pp.getNumero();
					}
					{
						final PersonnePhysique pp = addNonHabitant("Iain", "Kentucky", date(1980, 5, 3), Sexe.MASCULIN);
						addForPrincipal(pp, date(year, 1, 1), MotifFor.INDETERMINE, depart, MotifFor.DEPART_HC, MockCommune.Aubonne, ModeImposition.MIXTE_137_2);
						addForPrincipal(pp, depart.getOneDayAfter(), MotifFor.DEPART_HC, date(year, 12, 31), MotifFor.INDETERMINE, MockCommune.Bern, ModeImposition.ORDINAIRE);
						addForSecondaire(pp, depart.getOneDayAfter(), MotifFor.ACHAT_IMMOBILIER, date(year, 12, 31), MotifFor.INDETERMINE, MockCommune.Aigle.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
						ids.ppOrd = pp.getNumero();
					}
					return ids;
				}
			});

			final MutableLong testedId = new MutableLong();
			final Runnable test = new Runnable() {
				@Override
				public void run() {
					try {
						final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(testedId.longValue());
						final List<PeriodeImpositionImpotSource> piis = service.determine(pp);
						Assert.assertNotNull(piis);
						Assert.assertEquals(2, piis.size());
						{
							final PeriodeImpositionImpotSource pi = piis.get(0);
							Assert.assertNotNull(pi);
							Assert.assertEquals(PeriodeImpositionImpotSource.Type.MIXTE, pi.getType());
							Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, pi.getTypeAutoriteFiscale());
							Assert.assertEquals((Integer) MockCommune.Aubonne.getNoOFS(), pi.getNoOfs());
							Assert.assertEquals(date(year, 1, 1), pi.getDateDebut());
							Assert.assertEquals(depart, pi.getDateFin());
							Assert.assertNotNull(pi.getContribuable());
							Assert.assertEquals((Long) testedId.longValue(), pi.getContribuable().getNumero());
						}
						{
							final PeriodeImpositionImpotSource pi = piis.get(1);
							Assert.assertNotNull(pi);
							Assert.assertEquals(PeriodeImpositionImpotSource.Type.SOURCE, pi.getType());
							Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_HC, pi.getTypeAutoriteFiscale());
							Assert.assertEquals((Integer) MockCommune.Bern.getNoOFS(), pi.getNoOfs());
							Assert.assertEquals(depart.getOneDayAfter(), pi.getDateDebut());
							Assert.assertEquals(date(year, 12, 31), pi.getDateFin());
							Assert.assertNotNull(pi.getContribuable());
							Assert.assertEquals((Long) testedId.longValue(), pi.getContribuable().getNumero());
						}
					}
					catch (AssujettissementException e) {
						throw new RuntimeException(e);
					}
				}
			};

			// calcul
			doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
				@Override
				protected void doInTransactionWithoutResult(TransactionStatus status) {
					testedId.setValue(ids.ppSrc);
					test.run();

					testedId.setValue(ids.ppOrd);
					test.run();
				}
			});
		}
		finally {
			ForFiscalValidator.setFutureBeginDate(null);
		}
	}

	/**
	 * Cas 20 et 23 (après 2014)
	 */
	@Test
	public void testDepartHorsCantonMixte2Des2014() throws Exception {

		final int year = 2014;
		final RegDate depart = date(year, 6, 23);

		final class Ids {
			long ppSrc;
			long ppOrd;
		}

		ForFiscalValidator.setFutureBeginDate(date(year, 12, 31));
		try {
			// mise en place fiscale
			final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
				@Override
				public Ids doInTransaction(TransactionStatus status) {
					final Ids ids = new Ids();
					{
						final PersonnePhysique pp = addNonHabitant("Iain", "Kentucky", date(1980, 5, 3), Sexe.MASCULIN);
						addForPrincipal(pp, date(year, 1, 1), MotifFor.INDETERMINE, depart, MotifFor.DEPART_HC, MockCommune.Aubonne, ModeImposition.MIXTE_137_2);
						addForPrincipal(pp, depart.getOneDayAfter(), MotifFor.DEPART_HC, date(year, 12, 31), MotifFor.INDETERMINE, MockCommune.Bern, ModeImposition.SOURCE);
						ids.ppSrc = pp.getNumero();
					}
					{
						final PersonnePhysique pp = addNonHabitant("Iain", "Kentucky", date(1980, 5, 3), Sexe.MASCULIN);
						addForPrincipal(pp, date(year, 1, 1), MotifFor.INDETERMINE, depart, MotifFor.DEPART_HC, MockCommune.Aubonne, ModeImposition.MIXTE_137_2);
						addForPrincipal(pp, depart.getOneDayAfter(), MotifFor.DEPART_HC, date(year, 12, 31), MotifFor.INDETERMINE, MockCommune.Bern, ModeImposition.ORDINAIRE);
						addForSecondaire(pp, depart.getOneDayAfter(), MotifFor.ACHAT_IMMOBILIER, date(year, 12, 31), MotifFor.INDETERMINE, MockCommune.Aigle.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
						ids.ppOrd = pp.getNumero();
					}
					return ids;
				}
			});

			final MutableLong testedId = new MutableLong();
			final Runnable test = new Runnable() {
				@Override
				public void run() {
					try {
						final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(testedId.longValue());
						final List<PeriodeImpositionImpotSource> piis = service.determine(pp);
						Assert.assertNotNull(piis);
						Assert.assertEquals(2, piis.size());
						{
							final PeriodeImpositionImpotSource pi = piis.get(0);
							Assert.assertNotNull(pi);
							Assert.assertEquals(PeriodeImpositionImpotSource.Type.MIXTE, pi.getType());
							Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, pi.getTypeAutoriteFiscale());
							Assert.assertEquals((Integer) MockCommune.Aubonne.getNoOFS(), pi.getNoOfs());
							Assert.assertEquals(date(year, 1, 1), pi.getDateDebut());
							Assert.assertEquals(depart.getLastDayOfTheMonth(), pi.getDateFin());
							Assert.assertNotNull(pi.getContribuable());
							Assert.assertEquals((Long) testedId.longValue(), pi.getContribuable().getNumero());
						}
						{
							final PeriodeImpositionImpotSource pi = piis.get(1);
							Assert.assertNotNull(pi);
							Assert.assertEquals(PeriodeImpositionImpotSource.Type.SOURCE, pi.getType());
							Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_HC, pi.getTypeAutoriteFiscale());
							Assert.assertEquals((Integer) MockCommune.Bern.getNoOFS(), pi.getNoOfs());
							Assert.assertEquals(depart.getLastDayOfTheMonth().getOneDayAfter(), pi.getDateDebut());
							Assert.assertEquals(date(year, 12, 31), pi.getDateFin());
							Assert.assertNotNull(pi.getContribuable());
							Assert.assertEquals((Long) testedId.longValue(), pi.getContribuable().getNumero());
						}
					}
					catch (AssujettissementException e) {
						throw new RuntimeException(e);
					}
				}
			};
			// calcul
			doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
				@Override
				protected void doInTransactionWithoutResult(TransactionStatus status) {
					testedId.setValue(ids.ppSrc);
					test.run();

					testedId.setValue(ids.ppOrd);
					test.run();
				}
			});
		}
		finally {
			ForFiscalValidator.setFutureBeginDate(null);
		}
	}

	/**
	 * Cas 21 et 22 (avant 2014)
	 */
	@Test
	public void testDepartHorsCantonMixte1OuOrdinaireAvant2014() throws Exception {

		final int year = 2013;
		final RegDate depart = date(year, 6, 23);

		final class Ids {
			long ppSrc;
			long ppOrd;
		}

		ForFiscalValidator.setFutureBeginDate(date(year, 12, 31));
		try {
			// mise en place fiscale
			final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
				@Override
				public Ids doInTransaction(TransactionStatus status) {
					final Ids ids = new Ids();
					{
						final PersonnePhysique pp = addNonHabitant("Iain", "Kentucky", date(1980, 5, 3), Sexe.MASCULIN);
						addForPrincipal(pp, date(year, 1, 1), MotifFor.INDETERMINE, depart, MotifFor.DEPART_HC, MockCommune.Aubonne, ModeImposition.MIXTE_137_1);
						addForPrincipal(pp, depart.getOneDayAfter(), MotifFor.DEPART_HC, date(year, 12, 31), MotifFor.INDETERMINE, MockCommune.Bern, ModeImposition.SOURCE);
						ids.ppSrc = pp.getNumero();
					}
					{
						final PersonnePhysique pp = addNonHabitant("Iain", "Kentucky", date(1980, 5, 3), Sexe.MASCULIN);
						addForPrincipal(pp, date(year, 1, 1), MotifFor.INDETERMINE, depart, MotifFor.DEPART_HC, MockCommune.Aubonne, ModeImposition.MIXTE_137_1);
						addForPrincipal(pp, depart.getOneDayAfter(), MotifFor.DEPART_HC, date(year, 12, 31), MotifFor.INDETERMINE, MockCommune.Bern, ModeImposition.ORDINAIRE);
						addForSecondaire(pp, depart.getOneDayAfter(), MotifFor.ACHAT_IMMOBILIER, date(year, 12, 31), MotifFor.INDETERMINE, MockCommune.Aigle.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
						ids.ppOrd = pp.getNumero();
					}
					return ids;
				}
			});

			final MutableLong testedId = new MutableLong();
			final Runnable test = new Runnable() {
				@Override
				public void run() {
					try {
						final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(testedId.longValue());
						final List<PeriodeImpositionImpotSource> piis = service.determine(pp);
						Assert.assertNotNull(piis);
						Assert.assertEquals(2, piis.size());
						{
							final PeriodeImpositionImpotSource pi = piis.get(0);
							Assert.assertNotNull(pi);
							Assert.assertEquals(PeriodeImpositionImpotSource.Type.SOURCE, pi.getType());
							Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, pi.getTypeAutoriteFiscale());
							Assert.assertEquals((Integer) MockCommune.Aubonne.getNoOFS(), pi.getNoOfs());
							Assert.assertEquals(date(year, 1, 1), pi.getDateDebut());
							Assert.assertEquals(depart, pi.getDateFin());
							Assert.assertNotNull(pi.getContribuable());
							Assert.assertEquals((Long) testedId.longValue(), pi.getContribuable().getNumero());
						}
						{
							final PeriodeImpositionImpotSource pi = piis.get(1);
							Assert.assertNotNull(pi);
							Assert.assertEquals(PeriodeImpositionImpotSource.Type.SOURCE, pi.getType());
							Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_HC, pi.getTypeAutoriteFiscale());
							Assert.assertEquals((Integer) MockCommune.Bern.getNoOFS(), pi.getNoOfs());
							Assert.assertEquals(depart.getOneDayAfter(), pi.getDateDebut());
							Assert.assertEquals(date(year, 12, 31), pi.getDateFin());
							Assert.assertNotNull(pi.getContribuable());
							Assert.assertEquals((Long) testedId.longValue(), pi.getContribuable().getNumero());
						}
					}
					catch (AssujettissementException e) {
						throw new RuntimeException(e);
					}
				}
			};

			// calcul
			doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
				@Override
				protected void doInTransactionWithoutResult(TransactionStatus status) {
					testedId.setValue(ids.ppSrc);
					test.run();

					testedId.setValue(ids.ppOrd);
					test.run();
				}
			});
		}
		finally {
			ForFiscalValidator.setFutureBeginDate(null);
		}
	}

	/**
	 * Cas 21 et 22 (après 2014)
	 */
	@Test
	public void testDepartHorsCantonMixte1OuOrdinaireDes2014() throws Exception {

		final int year = 2014;
		final RegDate depart = date(year, 6, 23);

		final class Ids {
			long ppSrc;
			long ppOrd;
		}

		ForFiscalValidator.setFutureBeginDate(date(year, 12, 31));
		try {
			// mise en place fiscale
			final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
				@Override
				public Ids doInTransaction(TransactionStatus status) {
					final Ids ids = new Ids();
					{
						final PersonnePhysique pp = addNonHabitant("Iain", "Kentucky", date(1980, 5, 3), Sexe.MASCULIN);
						addForPrincipal(pp, date(year, 1, 1), MotifFor.INDETERMINE, depart, MotifFor.DEPART_HC, MockCommune.Aubonne, ModeImposition.MIXTE_137_1);
						addForPrincipal(pp, depart.getOneDayAfter(), MotifFor.DEPART_HC, date(year, 12, 31), MotifFor.INDETERMINE, MockCommune.Bern, ModeImposition.SOURCE);
						ids.ppSrc = pp.getNumero();
					}
					{
						final PersonnePhysique pp = addNonHabitant("Iain", "Kentucky", date(1980, 5, 3), Sexe.MASCULIN);
						addForPrincipal(pp, date(year, 1, 1), MotifFor.INDETERMINE, depart, MotifFor.DEPART_HC, MockCommune.Aubonne, ModeImposition.MIXTE_137_1);
						addForPrincipal(pp, depart.getOneDayAfter(), MotifFor.DEPART_HC, date(year, 12, 31), MotifFor.INDETERMINE, MockCommune.Bern, ModeImposition.ORDINAIRE);
						addForSecondaire(pp, depart.getOneDayAfter(), MotifFor.ACHAT_IMMOBILIER, date(year, 12, 31), MotifFor.INDETERMINE, MockCommune.Aigle.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
						ids.ppOrd = pp.getNumero();
					}
					return ids;
				}
			});

			final MutableLong testedId = new MutableLong();
			final Runnable test = new Runnable() {
				@Override
				public void run() {
					try {
						final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(testedId.longValue());
						final List<PeriodeImpositionImpotSource> piis = service.determine(pp);
						Assert.assertNotNull(piis);
						Assert.assertEquals(2, piis.size());
						{
							final PeriodeImpositionImpotSource pi = piis.get(0);
							Assert.assertNotNull(pi);
							Assert.assertEquals(PeriodeImpositionImpotSource.Type.SOURCE, pi.getType());
							Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, pi.getTypeAutoriteFiscale());
							Assert.assertEquals((Integer) MockCommune.Aubonne.getNoOFS(), pi.getNoOfs());
							Assert.assertEquals(date(year, 1, 1), pi.getDateDebut());
							Assert.assertEquals(depart.getLastDayOfTheMonth(), pi.getDateFin());
							Assert.assertNotNull(pi.getContribuable());
							Assert.assertEquals((Long) testedId.longValue(), pi.getContribuable().getNumero());
						}
						{
							final PeriodeImpositionImpotSource pi = piis.get(1);
							Assert.assertNotNull(pi);
							Assert.assertEquals(PeriodeImpositionImpotSource.Type.SOURCE, pi.getType());
							Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_HC, pi.getTypeAutoriteFiscale());
							Assert.assertEquals((Integer) MockCommune.Bern.getNoOFS(), pi.getNoOfs());
							Assert.assertEquals(depart.getLastDayOfTheMonth().getOneDayAfter(), pi.getDateDebut());
							Assert.assertEquals(date(year, 12, 31), pi.getDateFin());
							Assert.assertNotNull(pi.getContribuable());
							Assert.assertEquals((Long) testedId.longValue(), pi.getContribuable().getNumero());
						}
					}
					catch (AssujettissementException e) {
						throw new RuntimeException(e);
					}
				}
			};
			// calcul
			doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
				@Override
				protected void doInTransactionWithoutResult(TransactionStatus status) {
					testedId.setValue(ids.ppSrc);
					test.run();

					testedId.setValue(ids.ppOrd);
					test.run();
				}
			});
		}
		finally {
			ForFiscalValidator.setFutureBeginDate(null);
		}
	}

	/**
	 * Cas 24 & 25
	 */
	@Test
	public void testSourcierPurObtientPermisCOuNationalite() throws Exception {

		final int year = RegDate.get().year() - 1;
		final RegDate obtention = date(year, 5, 12);

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Iain", "McGregor", date(1987, 6, 23), Sexe.MASCULIN);
				addForPrincipal(pp, date(year, 1, 1), MotifFor.INDETERMINE, obtention.getOneDayBefore(), MotifFor.PERMIS_C_SUISSE, MockCommune.Aigle, ModeImposition.SOURCE);
				addForPrincipal(pp, obtention, MotifFor.PERMIS_C_SUISSE, MockCommune.Aigle);
				return pp.getNumero();
			}
		});

		// calcul
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				final List<PeriodeImpositionImpotSource> piis = service.determine(pp);
				Assert.assertNotNull(piis);
				Assert.assertEquals(2, piis.size());
				{
					final PeriodeImpositionImpotSource pi = piis.get(0);
					Assert.assertNotNull(pi);
					Assert.assertEquals(PeriodeImpositionImpotSource.Type.SOURCE, pi.getType());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, pi.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockCommune.Aigle.getNoOFS(), pi.getNoOfs());
					Assert.assertEquals(date(year, 1, 1), pi.getDateDebut());
					Assert.assertEquals(obtention.getLastDayOfTheMonth(), pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) ppId, pi.getContribuable().getNumero());
				}
				{
					final PeriodeImpositionImpotSource pi = piis.get(1);
					Assert.assertNotNull(pi);
					Assert.assertEquals(PeriodeImpositionImpotSource.Type.MIXTE, pi.getType());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, pi.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockCommune.Aigle.getNoOFS(), pi.getNoOfs());
					Assert.assertEquals(obtention.getLastDayOfTheMonth().getOneDayAfter(), pi.getDateDebut());
					Assert.assertEquals(date(year, 12, 31), pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) ppId, pi.getContribuable().getNumero());
				}
			}
		});
	}

	/**
	 * Cas ??
	 */
	@Test
	public void testSourcierMixteObtientPermisCOuNationalite() throws Exception {

		final int year = RegDate.get().year() - 1;
		final RegDate obtention = date(year, 5, 12);

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Iain", "McGregor", date(1987, 6, 23), Sexe.MASCULIN);
				addForPrincipal(pp, date(year, 1, 1), MotifFor.INDETERMINE, obtention.getOneDayBefore(), MotifFor.PERMIS_C_SUISSE, MockCommune.Aigle, ModeImposition.MIXTE_137_2);
				addForPrincipal(pp, obtention, MotifFor.PERMIS_C_SUISSE, MockCommune.Aigle);
				return pp.getNumero();
			}
		});

		// calcul
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				final List<PeriodeImpositionImpotSource> piis = service.determine(pp);
				Assert.assertNotNull(piis);
				Assert.assertEquals(1, piis.size());
				{
					final PeriodeImpositionImpotSource pi = piis.get(0);
					Assert.assertNotNull(pi);
					Assert.assertEquals(PeriodeImpositionImpotSource.Type.MIXTE, pi.getType());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, pi.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockCommune.Aigle.getNoOFS(), pi.getNoOfs());
					Assert.assertEquals(date(year, 1, 1), pi.getDateDebut());
					Assert.assertEquals(date(year, 12, 31), pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) ppId, pi.getContribuable().getNumero());
				}
			}
		});
	}

	/**
	 * Cas 26
	 */
	@Test
	public void testPassageMixte1PourSourcierPur() throws Exception {

		final int year = RegDate.get().year() - 1;
		final RegDate achat = date(year, 5, 12);

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Iain", "McGregor", date(1987, 6, 23), Sexe.MASCULIN);
				addForPrincipal(pp, date(year, 1, 1), MotifFor.INDETERMINE, achat.getOneDayBefore(), MotifFor.CHGT_MODE_IMPOSITION, MockCommune.Aigle, ModeImposition.SOURCE);
				addForPrincipal(pp, achat, MotifFor.CHGT_MODE_IMPOSITION, MockCommune.Aigle, ModeImposition.MIXTE_137_1);
				addForSecondaire(pp, achat, MotifFor.ACHAT_IMMOBILIER, MockCommune.Aubonne.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
				return pp.getNumero();
			}
		});

		// calcul
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				final List<PeriodeImpositionImpotSource> piis = service.determine(pp);
				Assert.assertNotNull(piis);
				Assert.assertEquals(2, piis.size());
				{
					final PeriodeImpositionImpotSource pi = piis.get(0);
					Assert.assertNotNull(pi);
					Assert.assertEquals(PeriodeImpositionImpotSource.Type.MIXTE, pi.getType());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, pi.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockCommune.Aigle.getNoOFS(), pi.getNoOfs());
					Assert.assertEquals(date(year, 1, 1), pi.getDateDebut());
					Assert.assertEquals(date(year, 12, 31), pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) ppId, pi.getContribuable().getNumero());
				}
				{
					final PeriodeImpositionImpotSource pi = piis.get(1);
					Assert.assertNotNull(pi);
					Assert.assertEquals(PeriodeImpositionImpotSource.Type.MIXTE, pi.getType());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, pi.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockCommune.Aigle.getNoOFS(), pi.getNoOfs());
					Assert.assertEquals(date(year + 1, 1, 1), pi.getDateDebut());
					Assert.assertEquals(date(year + 1, 12, 31), pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) ppId, pi.getContribuable().getNumero());
				}
			}
		});
	}

	/**
	 * Cas 27 (avec RT)
	 */
	@Test
	public void testAchatPourInconnuHorsSuisseAvecRT() throws Exception {

		final int year = RegDate.get().year() - 1;
		final RegDate achat = date(year, 5, 12);

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Iain", "McGregor", date(1987, 6, 23), Sexe.MASCULIN);
				addForPrincipal(pp, achat, MotifFor.ACHAT_IMMOBILIER, MockPays.Danemark);
				addForSecondaire(pp, achat, MotifFor.ACHAT_IMMOBILIER, MockCommune.Aubonne.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);

				final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.MENSUEL, date(2009, 1, 1));
				addRapportPrestationImposable(dpi, pp, achat, null, false);
				return pp.getNumero();
			}
		});

		// calcul
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				final List<PeriodeImpositionImpotSource> piis = service.determine(pp);
				Assert.assertNotNull(piis);
				Assert.assertEquals(3, piis.size());
				{
					final PeriodeImpositionImpotSource pi = piis.get(0);
					Assert.assertNotNull(pi);
					Assert.assertEquals(PeriodeImpositionImpotSource.Type.SOURCE, pi.getType());
					Assert.assertNull(pi.getTypeAutoriteFiscale());
					Assert.assertNull(pi.getNoOfs());
					Assert.assertEquals(date(year, 1, 1), pi.getDateDebut());
					Assert.assertEquals(achat.getOneDayBefore(), pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) ppId, pi.getContribuable().getNumero());
				}
				{
					final PeriodeImpositionImpotSource pi = piis.get(1);
					Assert.assertNotNull(pi);
					Assert.assertEquals(PeriodeImpositionImpotSource.Type.SOURCE, pi.getType());
					Assert.assertEquals(TypeAutoriteFiscale.PAYS_HS, pi.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockPays.Danemark.getNoOFS(), pi.getNoOfs());
					Assert.assertEquals(achat, pi.getDateDebut());
					Assert.assertEquals(date(year, 12, 31), pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) ppId, pi.getContribuable().getNumero());
				}
				{
					final PeriodeImpositionImpotSource pi = piis.get(2);
					Assert.assertNotNull(pi);
					Assert.assertEquals(PeriodeImpositionImpotSource.Type.SOURCE, pi.getType());
					Assert.assertEquals(TypeAutoriteFiscale.PAYS_HS, pi.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockPays.Danemark.getNoOFS(), pi.getNoOfs());
					Assert.assertEquals(date(year + 1, 1, 1), pi.getDateDebut());
					Assert.assertEquals(date(year + 1, 12, 31), pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) ppId, pi.getContribuable().getNumero());
				}
			}
		});
	}

	/**
	 * Cas 27 (sans RT)
	 */
	@Test
	public void testAchatPourInconnuHorsSuisseSansRT() throws Exception {

		final int year = RegDate.get().year() - 1;
		final RegDate achat = date(year, 5, 12);

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Iain", "McGregor", date(1987, 6, 23), Sexe.MASCULIN);
				addForPrincipal(pp, achat, MotifFor.ACHAT_IMMOBILIER, MockPays.Danemark);
				addForSecondaire(pp, achat, MotifFor.ACHAT_IMMOBILIER, MockCommune.Aubonne.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
				return pp.getNumero();
			}
		});

		// calcul
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				assertNoPiis(pp);
			}
		});
	}

	/**
	 * Cas 28 (avec RT)
	 */
	@Test
	public void testAchatPourInconnuHorsCantonAvecRT() throws Exception {

		final int year = RegDate.get().year() - 1;
		final RegDate achat = date(year, 5, 12);

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Iain", "McGregor", date(1987, 6, 23), Sexe.MASCULIN);
				addForPrincipal(pp, achat, MotifFor.ACHAT_IMMOBILIER, MockCommune.Bern);
				addForSecondaire(pp, achat, MotifFor.ACHAT_IMMOBILIER, MockCommune.Aubonne.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);

				final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.MENSUEL, date(2009, 1, 1));
				addRapportPrestationImposable(dpi, pp, achat, null, false);
				return pp.getNumero();
			}
		});

		// calcul
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				final List<PeriodeImpositionImpotSource> piis = service.determine(pp);
				Assert.assertNotNull(piis);
				Assert.assertEquals(3, piis.size());
				{
					final PeriodeImpositionImpotSource pi = piis.get(0);
					Assert.assertNotNull(pi);
					Assert.assertEquals(PeriodeImpositionImpotSource.Type.SOURCE, pi.getType());
					Assert.assertNull(pi.getTypeAutoriteFiscale());
					Assert.assertNull(pi.getNoOfs());
					Assert.assertEquals(date(year, 1, 1), pi.getDateDebut());
					Assert.assertEquals(achat.getOneDayBefore(), pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) ppId, pi.getContribuable().getNumero());
				}
				{
					final PeriodeImpositionImpotSource pi = piis.get(1);
					Assert.assertNotNull(pi);
					Assert.assertEquals(PeriodeImpositionImpotSource.Type.SOURCE, pi.getType());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_HC, pi.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockCommune.Bern.getNoOFS(), pi.getNoOfs());
					Assert.assertEquals(achat, pi.getDateDebut());
					Assert.assertEquals(date(year, 12, 31), pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) ppId, pi.getContribuable().getNumero());
				}
				{
					final PeriodeImpositionImpotSource pi = piis.get(2);
					Assert.assertNotNull(pi);
					Assert.assertEquals(PeriodeImpositionImpotSource.Type.SOURCE, pi.getType());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_HC, pi.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockCommune.Bern.getNoOFS(), pi.getNoOfs());
					Assert.assertEquals(date(year + 1, 1, 1), pi.getDateDebut());
					Assert.assertEquals(date(year + 1, 12, 31), pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) ppId, pi.getContribuable().getNumero());
				}
			}
		});
	}

	/**
	 * Cas 28 (sans RT)
	 */
	@Test
	public void testAchatPourInconnuHorsCantonSansRT() throws Exception {

		final int year = RegDate.get().year() - 1;
		final RegDate achat = date(year, 5, 12);

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Iain", "McGregor", date(1987, 6, 23), Sexe.MASCULIN);
				addForPrincipal(pp, achat, MotifFor.ACHAT_IMMOBILIER, MockCommune.Bern);
				addForSecondaire(pp, achat, MotifFor.ACHAT_IMMOBILIER, MockCommune.Aubonne.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
				return pp.getNumero();
			}
		});

		// calcul
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				assertNoPiis(pp);
			}
		});
	}

	/**
	 * Cas 29
	 */
	@Test
	public void testVenteImmeubleMixte1Vaudois() throws Exception {

		final int year = RegDate.get().year() - 1;
		final RegDate vente = date(year, 5, 14);

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Patrick", "McCallum", date(1974, 9, 13), Sexe.MASCULIN);
				addForPrincipal(pp, date(year, 1, 1), MotifFor.ARRIVEE_HS, date(year, 12, 31), MotifFor.DEPART_HS, MockCommune.Bex, ModeImposition.MIXTE_137_1);
				addForSecondaire(pp, date(year, 1, 1), MotifFor.ACHAT_IMMOBILIER, vente, MotifFor.VENTE_IMMOBILIER, MockCommune.Bex.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
				return pp.getNumero();
			}
		});

		// calcul
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				final List<PeriodeImpositionImpotSource> piis = service.determine(pp);
				Assert.assertNotNull(piis);
				Assert.assertEquals(1, piis.size());
				{
					final PeriodeImpositionImpotSource pi = piis.get(0);
					Assert.assertNotNull(pi);
					Assert.assertEquals(PeriodeImpositionImpotSource.Type.MIXTE, pi.getType());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, pi.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockCommune.Bex.getNoOFS(), pi.getNoOfs());
					Assert.assertEquals(date(year, 1, 1), pi.getDateDebut());
					Assert.assertEquals(date(year, 12, 31), pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) ppId, pi.getContribuable().getNumero());
				}
			}
		});
	}

	/**
	 * Cas 30 (avec RT)
	 */
	@Test
	public void testVenteImmeubleHorsSuisseAvecRT() throws Exception {

		final int year = RegDate.get().year() - 1;
		final RegDate vente = date(year, 5, 14);

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Patrick", "McCallum", date(1974, 9, 13), Sexe.MASCULIN);
				addForPrincipal(pp, date(year, 1, 1), MotifFor.ACHAT_IMMOBILIER, date(year, 12, 31), MotifFor.INDETERMINE, MockPays.Allemagne, ModeImposition.ORDINAIRE);
				addForSecondaire(pp, date(year, 1, 1), MotifFor.ACHAT_IMMOBILIER, vente, MotifFor.VENTE_IMMOBILIER, MockCommune.Bex.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);

				final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.MENSUEL, date(2009, 1, 1));
				addRapportPrestationImposable(dpi, pp, date(year, 1, 1), vente, false);
				return pp.getNumero();
			}
		});

		// calcul
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				final List<PeriodeImpositionImpotSource> piis = service.determine(pp);
				Assert.assertNotNull(piis);
				Assert.assertEquals(1, piis.size());
				{
					final PeriodeImpositionImpotSource pi = piis.get(0);
					Assert.assertNotNull(pi);
					Assert.assertEquals(PeriodeImpositionImpotSource.Type.SOURCE, pi.getType());
					Assert.assertEquals(TypeAutoriteFiscale.PAYS_HS, pi.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockPays.Allemagne.getNoOFS(), pi.getNoOfs());
					Assert.assertEquals(date(year, 1, 1), pi.getDateDebut());
					Assert.assertEquals(date(year, 12, 31), pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) ppId, pi.getContribuable().getNumero());
				}
			}
		});
	}

	/**
	 * Cas 30 (sans RT)
	 */
	@Test
	public void testVenteImmeubleHorsSuisseSansRT() throws Exception {

		final int year = RegDate.get().year() - 1;
		final RegDate vente = date(year, 5, 14);

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Patrick", "McCallum", date(1974, 9, 13), Sexe.MASCULIN);
				addForPrincipal(pp, date(year, 1, 1), MotifFor.ACHAT_IMMOBILIER, date(year, 12, 31), MotifFor.INDETERMINE, MockPays.Allemagne, ModeImposition.ORDINAIRE);
				addForSecondaire(pp, date(year, 1, 1), MotifFor.ACHAT_IMMOBILIER, vente, MotifFor.VENTE_IMMOBILIER, MockCommune.Bex.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
				return pp.getNumero();
			}
		});

		// calcul
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				assertNoPiis(pp);
			}
		});
	}

	/**
	 * Cas 31 (avec RT)
	 */
	@Test
	public void testVenteImmeubleHorsCantonAvecRT() throws Exception {

		final int year = RegDate.get().year() - 1;
		final RegDate vente = date(year, 5, 14);

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Patrick", "McCallum", date(1974, 9, 13), Sexe.MASCULIN);
				addForPrincipal(pp, date(year, 1, 1), MotifFor.ACHAT_IMMOBILIER, date(year, 12, 31), MotifFor.INDETERMINE, MockCommune.Geneve, ModeImposition.ORDINAIRE);
				addForSecondaire(pp, date(year, 1, 1), MotifFor.ACHAT_IMMOBILIER, vente, MotifFor.VENTE_IMMOBILIER, MockCommune.Bex.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);

				final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.MENSUEL, date(2009, 1, 1));
				addRapportPrestationImposable(dpi, pp, date(year, 1, 1), vente, false);
				return pp.getNumero();
			}
		});

		// calcul
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				final List<PeriodeImpositionImpotSource> piis = service.determine(pp);
				Assert.assertNotNull(piis);
				Assert.assertEquals(1, piis.size());
				{
					final PeriodeImpositionImpotSource pi = piis.get(0);
					Assert.assertNotNull(pi);
					Assert.assertEquals(PeriodeImpositionImpotSource.Type.SOURCE, pi.getType());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_HC, pi.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockCommune.Geneve.getNoOFS(), pi.getNoOfs());
					Assert.assertEquals(date(year, 1, 1), pi.getDateDebut());
					Assert.assertEquals(date(year, 12, 31), pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) ppId, pi.getContribuable().getNumero());
				}
			}
		});
	}

	/**
	 * Cas 31 (sans RT)
	 */
	@Test
	public void testVenteImmeubleHorsCantonSansRT() throws Exception {

		final int year = RegDate.get().year() - 1;
		final RegDate vente = date(year, 5, 14);

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Patrick", "McCallum", date(1974, 9, 13), Sexe.MASCULIN);
				addForPrincipal(pp, date(year, 1, 1), MotifFor.ACHAT_IMMOBILIER, date(year, 12, 31), MotifFor.INDETERMINE, MockCommune.Geneve, ModeImposition.ORDINAIRE);
				addForSecondaire(pp, date(year, 1, 1), MotifFor.ACHAT_IMMOBILIER, vente, MotifFor.VENTE_IMMOBILIER, MockCommune.Bex.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
				return pp.getNumero();
			}
		});

		// calcul
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				assertNoPiis(pp);
			}
		});
	}

	/**
	 * Cas 32 (avec RT sur ordinaire)
	 */
	@Test
	public void testMariageSourcierPurAvecOrdinaireAvecRT() throws Exception {

		final int year = RegDate.get().year() - 1;
		final RegDate mariage = date(year, 5, 12);

		final class Ids {
			long ppSourcier;
			long ppOrdinaire;
			long mc;
		}

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final PersonnePhysique src = addNonHabitant("Patrick", "McGregor", date(1980, 7, 15), Sexe.MASCULIN);
				final PersonnePhysique ord = addNonHabitant("Mélanie", "Pittet", date(1978, 2, 28), Sexe.FEMININ);
				final EnsembleTiersCouple couple = addEnsembleTiersCouple(src, ord, mariage, null);
				final MenageCommun mc = couple.getMenage();

				addForPrincipal(src, date(year, 1, 1), MotifFor.INDETERMINE, mariage.getOneDayBefore(), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Bex, ModeImposition.SOURCE);
				addForPrincipal(ord, date(year, 1, 1), MotifFor.INDETERMINE, mariage.getOneDayBefore(), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Leysin, ModeImposition.ORDINAIRE);
				addForPrincipal(mc, mariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Bex);

				final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.MENSUEL, date(2009, 1, 1));
				addRapportPrestationImposable(dpi, ord, date(year, 1, 1), null, false);

				final Ids ids = new Ids();
				ids.ppSourcier = src.getNumero();
				ids.ppOrdinaire = ord.getNumero();
				ids.mc = mc.getNumero();
				return ids;
			}
		});

		// calculs
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				{
					final PersonnePhysique src = (PersonnePhysique) tiersDAO.get(ids.ppSourcier);
					final List<PeriodeImpositionImpotSource> piis = service.determine(src);
					Assert.assertNotNull(piis);
					Assert.assertEquals(2, piis.size());
					{
						final PeriodeImpositionImpotSource pi = piis.get(0);
						Assert.assertNotNull(pi);
						Assert.assertEquals(PeriodeImpositionImpotSource.Type.SOURCE, pi.getType());
						Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, pi.getTypeAutoriteFiscale());
						Assert.assertEquals((Integer) MockCommune.Bex.getNoOFS(), pi.getNoOfs());
						Assert.assertEquals(date(year, 1, 1), pi.getDateDebut());
						Assert.assertEquals(mariage.getLastDayOfTheMonth(), pi.getDateFin());
						Assert.assertNotNull(pi.getContribuable());
						Assert.assertEquals((Long) ids.ppSourcier, pi.getContribuable().getNumero());
					}
					{
						final PeriodeImpositionImpotSource pi = piis.get(1);
						Assert.assertNotNull(pi);
						Assert.assertEquals(PeriodeImpositionImpotSource.Type.MIXTE, pi.getType());
						Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, pi.getTypeAutoriteFiscale());
						Assert.assertEquals((Integer) MockCommune.Bex.getNoOFS(), pi.getNoOfs());
						Assert.assertEquals(mariage.getLastDayOfTheMonth().getOneDayAfter(), pi.getDateDebut());
						Assert.assertEquals(date(year, 12, 31), pi.getDateFin());
						Assert.assertNotNull(pi.getContribuable());
						Assert.assertEquals((Long) ids.ppSourcier, pi.getContribuable().getNumero());
					}
				}
				{
					final PersonnePhysique ord = (PersonnePhysique) tiersDAO.get(ids.ppOrdinaire);
					final List<PeriodeImpositionImpotSource> piis = service.determine(ord);
					Assert.assertNotNull(piis);
					Assert.assertEquals(2, piis.size());
					{
						final PeriodeImpositionImpotSource pi = piis.get(0);
						Assert.assertNotNull(pi);
						Assert.assertEquals(PeriodeImpositionImpotSource.Type.MIXTE, pi.getType());
						Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, pi.getTypeAutoriteFiscale());
						Assert.assertEquals((Integer) MockCommune.Bex.getNoOFS(), pi.getNoOfs());
						Assert.assertEquals(date(year, 1, 1), pi.getDateDebut());
						Assert.assertEquals(date(year, 12, 31), pi.getDateFin());
						Assert.assertNotNull(pi.getContribuable());
						Assert.assertEquals((Long) ids.ppOrdinaire, pi.getContribuable().getNumero());
					}
					{
						final PeriodeImpositionImpotSource pi = piis.get(1);
						Assert.assertNotNull(pi);
						Assert.assertEquals(PeriodeImpositionImpotSource.Type.MIXTE, pi.getType());
						Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, pi.getTypeAutoriteFiscale());
						Assert.assertEquals((Integer) MockCommune.Bex.getNoOFS(), pi.getNoOfs());
						Assert.assertEquals(date(year + 1, 1, 1), pi.getDateDebut());
						Assert.assertEquals(date(year + 1, 12, 31), pi.getDateFin());
						Assert.assertNotNull(pi.getContribuable());
						Assert.assertEquals((Long) ids.ppOrdinaire, pi.getContribuable().getNumero());
					}
				}
			}
		});
	}

	/**
	 * Cas 32 (sans RT sur ordinaire)
	 */
	@Test
	public void testMariageSourcierPurAvecOrdinaireSansRT() throws Exception {

		final int year = RegDate.get().year() - 1;
		final RegDate mariage = date(year, 5, 12);

		final class Ids {
			long ppSourcier;
			long ppOrdinaire;
			long mc;
		}

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final PersonnePhysique src = addNonHabitant("Patrick", "McGregor", date(1980, 7, 15), Sexe.MASCULIN);
				final PersonnePhysique ord = addNonHabitant("Mélanie", "Pittet", date(1978, 2, 28), Sexe.FEMININ);
				final EnsembleTiersCouple couple = addEnsembleTiersCouple(src, ord, mariage, null);
				final MenageCommun mc = couple.getMenage();

				addForPrincipal(src, date(year, 1, 1), MotifFor.INDETERMINE, mariage.getOneDayBefore(), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Bex, ModeImposition.SOURCE);
				addForPrincipal(ord, date(year, 1, 1), MotifFor.INDETERMINE, mariage.getOneDayBefore(), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Leysin, ModeImposition.ORDINAIRE);
				addForPrincipal(mc, mariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Bex);

				final Ids ids = new Ids();
				ids.ppSourcier = src.getNumero();
				ids.ppOrdinaire = ord.getNumero();
				ids.mc = mc.getNumero();
				return ids;
			}
		});

		// calculs
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				{
					final PersonnePhysique src = (PersonnePhysique) tiersDAO.get(ids.ppSourcier);
					final List<PeriodeImpositionImpotSource> piis = service.determine(src);
					Assert.assertNotNull(piis);
					Assert.assertEquals(2, piis.size());
					{
						final PeriodeImpositionImpotSource pi = piis.get(0);
						Assert.assertNotNull(pi);
						Assert.assertEquals(PeriodeImpositionImpotSource.Type.SOURCE, pi.getType());
						Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, pi.getTypeAutoriteFiscale());
						Assert.assertEquals((Integer) MockCommune.Bex.getNoOFS(), pi.getNoOfs());
						Assert.assertEquals(date(year, 1, 1), pi.getDateDebut());
						Assert.assertEquals(mariage.getLastDayOfTheMonth(), pi.getDateFin());
						Assert.assertNotNull(pi.getContribuable());
						Assert.assertEquals((Long) ids.ppSourcier, pi.getContribuable().getNumero());
					}
					{
						final PeriodeImpositionImpotSource pi = piis.get(1);
						Assert.assertNotNull(pi);
						Assert.assertEquals(PeriodeImpositionImpotSource.Type.MIXTE, pi.getType());
						Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, pi.getTypeAutoriteFiscale());
						Assert.assertEquals((Integer) MockCommune.Bex.getNoOFS(), pi.getNoOfs());
						Assert.assertEquals(mariage.getLastDayOfTheMonth().getOneDayAfter(), pi.getDateDebut());
						Assert.assertEquals(date(year, 12, 31), pi.getDateFin());
						Assert.assertNotNull(pi.getContribuable());
						Assert.assertEquals((Long) ids.ppSourcier, pi.getContribuable().getNumero());
					}
				}
				{
					final PersonnePhysique ord = (PersonnePhysique) tiersDAO.get(ids.ppOrdinaire);
					assertNoPiis(ord);
				}
			}
		});
	}

	/**
	 * Cas 33
	 */
	@Test
	public void testMariageSourcierPurAvecMixte() throws Exception {

		final int year = RegDate.get().year() - 1;
		final RegDate mariage = date(year, 5, 12);

		final class Ids {
			long ppSourcier;
			long ppMixte;
			long mc;
		}

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final PersonnePhysique src = addNonHabitant("Patrick", "McGregor", date(1980, 7, 15), Sexe.MASCULIN);
				final PersonnePhysique mixte = addNonHabitant("Mélanie", "Pittet", date(1978, 2, 28), Sexe.FEMININ);
				final EnsembleTiersCouple couple = addEnsembleTiersCouple(src, mixte, mariage, null);
				final MenageCommun mc = couple.getMenage();

				addForPrincipal(src, date(year, 1, 1), MotifFor.INDETERMINE, mariage.getOneDayBefore(), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Bex, ModeImposition.SOURCE);
				addForPrincipal(mixte, date(year, 1, 1), MotifFor.INDETERMINE, mariage.getOneDayBefore(), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Leysin, ModeImposition.MIXTE_137_2);
				addForPrincipal(mc, mariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Bex, ModeImposition.MIXTE_137_2);

				final Ids ids = new Ids();
				ids.ppSourcier = src.getNumero();
				ids.ppMixte = mixte.getNumero();
				ids.mc = mc.getNumero();
				return ids;
			}
		});

		// calculs
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				{
					final PersonnePhysique src = (PersonnePhysique) tiersDAO.get(ids.ppSourcier);
					final List<PeriodeImpositionImpotSource> piis = service.determine(src);
					Assert.assertNotNull(piis);
					Assert.assertEquals(2, piis.size());
					{
						final PeriodeImpositionImpotSource pi = piis.get(0);
						Assert.assertNotNull(pi);
						Assert.assertEquals(PeriodeImpositionImpotSource.Type.MIXTE, pi.getType());
						Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, pi.getTypeAutoriteFiscale());
						Assert.assertEquals((Integer) MockCommune.Bex.getNoOFS(), pi.getNoOfs());
						Assert.assertEquals(date(year, 1, 1), pi.getDateDebut());
						Assert.assertEquals(date(year, 12, 31), pi.getDateFin());
						Assert.assertNotNull(pi.getContribuable());
						Assert.assertEquals((Long) ids.ppSourcier, pi.getContribuable().getNumero());
					}
					{
						final PeriodeImpositionImpotSource pi = piis.get(1);
						Assert.assertNotNull(pi);
						Assert.assertEquals(PeriodeImpositionImpotSource.Type.MIXTE, pi.getType());
						Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, pi.getTypeAutoriteFiscale());
						Assert.assertEquals((Integer) MockCommune.Bex.getNoOFS(), pi.getNoOfs());
						Assert.assertEquals(date(year + 1, 1, 1), pi.getDateDebut());
						Assert.assertEquals(date(year + 1, 12, 31), pi.getDateFin());
						Assert.assertNotNull(pi.getContribuable());
						Assert.assertEquals((Long) ids.ppSourcier, pi.getContribuable().getNumero());
					}
				}
				{
					final PersonnePhysique ord = (PersonnePhysique) tiersDAO.get(ids.ppMixte);
					final List<PeriodeImpositionImpotSource> piis = service.determine(ord);
					Assert.assertNotNull(piis);
					Assert.assertEquals(2, piis.size());
					{
						final PeriodeImpositionImpotSource pi = piis.get(0);
						Assert.assertNotNull(pi);
						Assert.assertEquals(PeriodeImpositionImpotSource.Type.MIXTE, pi.getType());
						Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, pi.getTypeAutoriteFiscale());
						Assert.assertEquals((Integer) MockCommune.Bex.getNoOFS(), pi.getNoOfs());
						Assert.assertEquals(date(year, 1, 1), pi.getDateDebut());
						Assert.assertEquals(date(year, 12, 31), pi.getDateFin());
						Assert.assertNotNull(pi.getContribuable());
						Assert.assertEquals((Long) ids.ppMixte, pi.getContribuable().getNumero());
					}
					{
						final PeriodeImpositionImpotSource pi = piis.get(1);
						Assert.assertNotNull(pi);
						Assert.assertEquals(PeriodeImpositionImpotSource.Type.MIXTE, pi.getType());
						Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, pi.getTypeAutoriteFiscale());
						Assert.assertEquals((Integer) MockCommune.Bex.getNoOFS(), pi.getNoOfs());
						Assert.assertEquals(date(year + 1, 1, 1), pi.getDateDebut());
						Assert.assertEquals(date(year + 1, 12, 31), pi.getDateFin());
						Assert.assertNotNull(pi.getContribuable());
						Assert.assertEquals((Long) ids.ppMixte, pi.getContribuable().getNumero());
					}
				}
			}
		});
	}

	/**
	 * Cas 34
	 */
	@Test
	public void testMariageSourcierPurAvecOrdinairePermisCJusteAvantMariage() throws Exception {

		final int year = RegDate.get().year() - 1;
		final RegDate mariage = date(year, 5, 12);
		final RegDate obtention = mariage.addMonths(-3);

		final class Ids {
			long ppSourcier;
			long ppOrdinaire;
			long mc;
		}

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final PersonnePhysique src = addNonHabitant("Patrick", "McGregor", date(1980, 7, 15), Sexe.MASCULIN);
				final PersonnePhysique permisc = addNonHabitant("Mélanie", "Pittet", date(1978, 2, 28), Sexe.FEMININ);
				final EnsembleTiersCouple couple = addEnsembleTiersCouple(src, permisc, mariage, null);
				final MenageCommun mc = couple.getMenage();

				addForPrincipal(permisc, date(year, 1, 1), MotifFor.INDETERMINE, obtention.getOneDayBefore(), MotifFor.PERMIS_C_SUISSE, MockCommune.Leysin, ModeImposition.SOURCE);
				addForPrincipal(permisc, obtention, MotifFor.PERMIS_C_SUISSE, mariage.getOneDayBefore(), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Leysin, ModeImposition.ORDINAIRE);
				addForPrincipal(src, date(year, 1, 1), MotifFor.INDETERMINE, mariage.getOneDayBefore(), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Bex, ModeImposition.SOURCE);
				addForPrincipal(mc, mariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Bex);

				final Ids ids = new Ids();
				ids.ppSourcier = src.getNumero();
				ids.ppOrdinaire = permisc.getNumero();
				ids.mc = mc.getNumero();
				return ids;
			}
		});

		// calculs
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				{
					final PersonnePhysique src = (PersonnePhysique) tiersDAO.get(ids.ppSourcier);
					final List<PeriodeImpositionImpotSource> piis = service.determine(src);
					Assert.assertNotNull(piis);
					Assert.assertEquals(2, piis.size());
					{
						final PeriodeImpositionImpotSource pi = piis.get(0);
						Assert.assertNotNull(pi);
						Assert.assertEquals(PeriodeImpositionImpotSource.Type.SOURCE, pi.getType());
						Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, pi.getTypeAutoriteFiscale());
						Assert.assertEquals((Integer) MockCommune.Bex.getNoOFS(), pi.getNoOfs());
						Assert.assertEquals(date(year, 1, 1), pi.getDateDebut());
						Assert.assertEquals(mariage.getLastDayOfTheMonth(), pi.getDateFin());
						Assert.assertNotNull(pi.getContribuable());
						Assert.assertEquals((Long) ids.ppSourcier, pi.getContribuable().getNumero());
					}
					{
						final PeriodeImpositionImpotSource pi = piis.get(1);
						Assert.assertNotNull(pi);
						Assert.assertEquals(PeriodeImpositionImpotSource.Type.MIXTE, pi.getType());
						Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, pi.getTypeAutoriteFiscale());
						Assert.assertEquals((Integer) MockCommune.Bex.getNoOFS(), pi.getNoOfs());
						Assert.assertEquals(mariage.getLastDayOfTheMonth().getOneDayAfter(), pi.getDateDebut());
						Assert.assertEquals(date(year, 12, 31), pi.getDateFin());
						Assert.assertNotNull(pi.getContribuable());
						Assert.assertEquals((Long) ids.ppSourcier, pi.getContribuable().getNumero());
					}
				}
				{
					final PersonnePhysique ord = (PersonnePhysique) tiersDAO.get(ids.ppOrdinaire);
					final List<PeriodeImpositionImpotSource> piis = service.determine(ord);
					Assert.assertNotNull(piis);
					Assert.assertEquals(2, piis.size());
					{
						final PeriodeImpositionImpotSource pi = piis.get(0);
						Assert.assertNotNull(pi);
						Assert.assertEquals(PeriodeImpositionImpotSource.Type.SOURCE, pi.getType());
						Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, pi.getTypeAutoriteFiscale());
						Assert.assertEquals((Integer) MockCommune.Leysin.getNoOFS(), pi.getNoOfs());
						Assert.assertEquals(date(year, 1, 1), pi.getDateDebut());
						Assert.assertEquals(obtention.getLastDayOfTheMonth(), pi.getDateFin());
						Assert.assertNotNull(pi.getContribuable());
						Assert.assertEquals((Long) ids.ppOrdinaire, pi.getContribuable().getNumero());
					}
					{
						final PeriodeImpositionImpotSource pi = piis.get(1);
						Assert.assertNotNull(pi);
						Assert.assertEquals(PeriodeImpositionImpotSource.Type.MIXTE, pi.getType());
						Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, pi.getTypeAutoriteFiscale());
						Assert.assertEquals((Integer) MockCommune.Bex.getNoOFS(), pi.getNoOfs());
						Assert.assertEquals(obtention.getLastDayOfTheMonth().getOneDayAfter(), pi.getDateDebut());
						Assert.assertEquals(date(year, 12, 31), pi.getDateFin());
						Assert.assertNotNull(pi.getContribuable());
						Assert.assertEquals((Long) ids.ppOrdinaire, pi.getContribuable().getNumero());
					}
				}
			}
		});
	}

	/**
	 * Cas 35
	 */
	@Test
	public void testMariageSourciersPursAvecPermisCObtenuApresMariage() throws Exception {

		final int year = RegDate.get().year() - 1;
		final RegDate mariage = date(year, 5, 12);
		final RegDate obtention = mariage.addMonths(3);

		final class Ids {
			long ppSourcier;
			long ppOrdinaire;
			long mc;
		}

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final PersonnePhysique src = addNonHabitant("Patrick", "McGregor", date(1980, 7, 15), Sexe.MASCULIN);
				final PersonnePhysique permisc = addNonHabitant("Mélanie", "Pittet", date(1978, 2, 28), Sexe.FEMININ);
				final EnsembleTiersCouple couple = addEnsembleTiersCouple(src, permisc, mariage, null);
				final MenageCommun mc = couple.getMenage();

				addForPrincipal(permisc, date(year, 1, 1), MotifFor.INDETERMINE, mariage.getOneDayBefore(), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Leysin, ModeImposition.SOURCE);
				addForPrincipal(src, date(year, 1, 1), MotifFor.INDETERMINE, mariage.getOneDayBefore(), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Bex, ModeImposition.SOURCE);
				addForPrincipal(mc, mariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, obtention.getOneDayBefore(), MotifFor.PERMIS_C_SUISSE, MockCommune.Bex, ModeImposition.SOURCE);
				addForPrincipal(mc, obtention, MotifFor.PERMIS_C_SUISSE, MockCommune.Bex);

				final Ids ids = new Ids();
				ids.ppSourcier = src.getNumero();
				ids.ppOrdinaire = permisc.getNumero();
				ids.mc = mc.getNumero();
				return ids;
			}
		});

		// calculs
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				{
					final PersonnePhysique src = (PersonnePhysique) tiersDAO.get(ids.ppSourcier);
					final List<PeriodeImpositionImpotSource> piis = service.determine(src);
					Assert.assertNotNull(piis);
					Assert.assertEquals(2, piis.size());
					{
						final PeriodeImpositionImpotSource pi = piis.get(0);
						Assert.assertNotNull(pi);
						Assert.assertEquals(PeriodeImpositionImpotSource.Type.SOURCE, pi.getType());
						Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, pi.getTypeAutoriteFiscale());
						Assert.assertEquals((Integer) MockCommune.Bex.getNoOFS(), pi.getNoOfs());
						Assert.assertEquals(date(year, 1, 1), pi.getDateDebut());
						Assert.assertEquals(obtention.getLastDayOfTheMonth(), pi.getDateFin());
						Assert.assertNotNull(pi.getContribuable());
						Assert.assertEquals((Long) ids.ppSourcier, pi.getContribuable().getNumero());
					}
					{
						final PeriodeImpositionImpotSource pi = piis.get(1);
						Assert.assertNotNull(pi);
						Assert.assertEquals(PeriodeImpositionImpotSource.Type.MIXTE, pi.getType());
						Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, pi.getTypeAutoriteFiscale());
						Assert.assertEquals((Integer) MockCommune.Bex.getNoOFS(), pi.getNoOfs());
						Assert.assertEquals(obtention.getLastDayOfTheMonth().getOneDayAfter(), pi.getDateDebut());
						Assert.assertEquals(date(year, 12, 31), pi.getDateFin());
						Assert.assertNotNull(pi.getContribuable());
						Assert.assertEquals((Long) ids.ppSourcier, pi.getContribuable().getNumero());
					}
				}
				{
					final PersonnePhysique ord = (PersonnePhysique) tiersDAO.get(ids.ppOrdinaire);
					final List<PeriodeImpositionImpotSource> piis = service.determine(ord);
					Assert.assertNotNull(piis);
					Assert.assertEquals(2, piis.size());
					{
						final PeriodeImpositionImpotSource pi = piis.get(0);
						Assert.assertNotNull(pi);
						Assert.assertEquals(PeriodeImpositionImpotSource.Type.SOURCE, pi.getType());
						Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, pi.getTypeAutoriteFiscale());
						Assert.assertEquals((Integer) MockCommune.Bex.getNoOFS(), pi.getNoOfs());
						Assert.assertEquals(date(year, 1, 1), pi.getDateDebut());
						Assert.assertEquals(obtention.getLastDayOfTheMonth(), pi.getDateFin());
						Assert.assertNotNull(pi.getContribuable());
						Assert.assertEquals((Long) ids.ppOrdinaire, pi.getContribuable().getNumero());
					}
					{
						final PeriodeImpositionImpotSource pi = piis.get(1);
						Assert.assertNotNull(pi);
						Assert.assertEquals(PeriodeImpositionImpotSource.Type.MIXTE, pi.getType());
						Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, pi.getTypeAutoriteFiscale());
						Assert.assertEquals((Integer) MockCommune.Bex.getNoOFS(), pi.getNoOfs());
						Assert.assertEquals(obtention.getLastDayOfTheMonth().getOneDayAfter(), pi.getDateDebut());
						Assert.assertEquals(date(year, 12, 31), pi.getDateFin());
						Assert.assertNotNull(pi.getContribuable());
						Assert.assertEquals((Long) ids.ppOrdinaire, pi.getContribuable().getNumero());
					}
				}
			}
		});
	}

	@Test
	public void testDemenagementVaudoisApresObtentionPermisCMemeMois() throws Exception {

		final int year = RegDate.get().year() - 1;
		final RegDate obtention = date(year, 7, 12);
		final RegDate demenagement = obtention.getLastDayOfTheMonth().addDays(-5);

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Alfredo", "Garcia", date(1976, 5, 31), Sexe.MASCULIN);
				addForPrincipal(pp, date(year, 1, 1), MotifFor.INDETERMINE, obtention.getOneDayBefore(), MotifFor.PERMIS_C_SUISSE, MockCommune.Echallens, ModeImposition.SOURCE);
				addForPrincipal(pp, obtention, MotifFor.PERMIS_C_SUISSE, demenagement.getOneDayBefore(), MotifFor.DEMENAGEMENT_VD, MockCommune.Echallens);
				addForPrincipal(pp, demenagement, MotifFor.DEMENAGEMENT_VD, MockCommune.Cossonay);
				return pp.getNumero();
			}
		});

		// calcul
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				final List<PeriodeImpositionImpotSource> piis = service.determine(pp);
				Assert.assertEquals(2, piis.size());
				{
					final PeriodeImpositionImpotSource pi = piis.get(0);
					Assert.assertNotNull(pi);
					Assert.assertEquals(PeriodeImpositionImpotSource.Type.SOURCE, pi.getType());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, pi.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockCommune.Echallens.getNoOFS(), pi.getNoOfs());
					Assert.assertEquals(date(year, 1, 1), pi.getDateDebut());
					Assert.assertEquals(obtention.getLastDayOfTheMonth(), pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) ppId, pi.getContribuable().getNumero());
				}
				{
					final PeriodeImpositionImpotSource pi = piis.get(1);
					Assert.assertNotNull(pi);
					Assert.assertEquals(PeriodeImpositionImpotSource.Type.MIXTE, pi.getType());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, pi.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockCommune.Cossonay.getNoOFS(), pi.getNoOfs());
					Assert.assertEquals(obtention.getLastDayOfTheMonth().getOneDayAfter(), pi.getDateDebut());
					Assert.assertEquals(date(year, 12, 31), pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) ppId, pi.getContribuable().getNumero());
				}
			}
		});
	}

	@Test
	public void testDemenagementVaudoisEtMariageApresObtentionPermisCMemeMois() throws Exception {

		final int year = RegDate.get().year() - 1;
		final RegDate obtention = date(year, 7, 12);
		final RegDate demenagement = obtention.getLastDayOfTheMonth().addDays(-5);
		final RegDate mariage = demenagement.addDays(2);

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Alfredo", "Garcia", date(1976, 5, 31), Sexe.MASCULIN);
				addForPrincipal(pp, date(year, 1, 1), MotifFor.INDETERMINE, obtention.getOneDayBefore(), MotifFor.PERMIS_C_SUISSE, MockCommune.Echallens, ModeImposition.SOURCE);
				addForPrincipal(pp, obtention, MotifFor.PERMIS_C_SUISSE, demenagement.getOneDayBefore(), MotifFor.DEMENAGEMENT_VD, MockCommune.Echallens);
				addForPrincipal(pp, demenagement, MotifFor.DEMENAGEMENT_VD, mariage.getOneDayBefore(), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Cossonay);

				final EnsembleTiersCouple couple = addEnsembleTiersCouple(pp, null, mariage, null);
				addForPrincipal(couple.getMenage(), mariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Cossonay);
				return pp.getNumero();
			}
		});

		// calcul
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				final List<PeriodeImpositionImpotSource> piis = service.determine(pp);
				Assert.assertEquals(2, piis.size());
				{
					final PeriodeImpositionImpotSource pi = piis.get(0);
					Assert.assertNotNull(pi);
					Assert.assertEquals(PeriodeImpositionImpotSource.Type.SOURCE, pi.getType());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, pi.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockCommune.Echallens.getNoOFS(), pi.getNoOfs());
					Assert.assertEquals(date(year, 1, 1), pi.getDateDebut());
					Assert.assertEquals(obtention.getLastDayOfTheMonth(), pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) ppId, pi.getContribuable().getNumero());
				}
				{
					final PeriodeImpositionImpotSource pi = piis.get(1);
					Assert.assertNotNull(pi);
					Assert.assertEquals(PeriodeImpositionImpotSource.Type.MIXTE, pi.getType());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, pi.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockCommune.Cossonay.getNoOFS(), pi.getNoOfs());
					Assert.assertEquals(obtention.getLastDayOfTheMonth().getOneDayAfter(), pi.getDateDebut());
					Assert.assertEquals(date(year, 12, 31), pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) ppId, pi.getContribuable().getNumero());
				}
			}
		});
	}

	@Test
	public void testVeuvageChezMixtes() throws Exception {

		final int year = RegDate.get().year() - 1;
		final RegDate dateDeces = date(year, 8, 6);
		final RegDate dateMariage = date(year - 10, 5, 3);

		final class Ids {
			long ppDecede;
			long ppSurvivant;
		}

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final PersonnePhysique decede = addNonHabitant("Gertrud", "Haenkel", date(1967, 9, 23), Sexe.FEMININ);
				decede.setDateDeces(dateDeces);
				final PersonnePhysique survivant = addNonHabitant("Alfredo", "Haenkel", date(1965, 4, 12), Sexe.MASCULIN);
				final EnsembleTiersCouple couple = addEnsembleTiersCouple(survivant, decede, dateMariage, dateDeces);
				addForPrincipal(couple.getMenage(), date(year, 1, 1), MotifFor.ARRIVEE_HS, dateDeces, MotifFor.VEUVAGE_DECES, MockCommune.Cossonay, ModeImposition.MIXTE_137_2);
				addForPrincipal(survivant, dateDeces.getOneDayAfter(), MotifFor.VEUVAGE_DECES, MockCommune.Cossonay, ModeImposition.MIXTE_137_2);

				final Ids ids = new Ids();
				ids.ppDecede = decede.getNumero();
				ids.ppSurvivant = survivant.getNumero();
				return ids;
			}
		});

		// calculs
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				// la personne physique décédée
				{
					final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ids.ppDecede);
					final List<PeriodeImpositionImpotSource> piis = service.determine(pp);
					Assert.assertEquals(1, piis.size());
					{
						final PeriodeImpositionImpotSource pi = piis.get(0);
						Assert.assertNotNull(pi);
						Assert.assertEquals(PeriodeImpositionImpotSource.Type.MIXTE, pi.getType());
						Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, pi.getTypeAutoriteFiscale());
						Assert.assertEquals((Integer) MockCommune.Cossonay.getNoOFS(), pi.getNoOfs());
						Assert.assertEquals(date(year, 1, 1), pi.getDateDebut());
						Assert.assertEquals(dateDeces, pi.getDateFin());
						Assert.assertNotNull(pi.getContribuable());
						Assert.assertEquals((Long) ids.ppDecede, pi.getContribuable().getNumero());
					}
				}

				// la personne physique survivante
				{
					final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ids.ppSurvivant);
					final List<PeriodeImpositionImpotSource> piis = service.determine(pp);
					Assert.assertEquals(3, piis.size());
					{
						final PeriodeImpositionImpotSource pi = piis.get(0);
						Assert.assertNotNull(pi);
						Assert.assertEquals(PeriodeImpositionImpotSource.Type.MIXTE, pi.getType());
						Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, pi.getTypeAutoriteFiscale());
						Assert.assertEquals((Integer) MockCommune.Cossonay.getNoOFS(), pi.getNoOfs());
						Assert.assertEquals(date(year, 1, 1), pi.getDateDebut());
						Assert.assertEquals(dateDeces, pi.getDateFin());
						Assert.assertNotNull(pi.getContribuable());
						Assert.assertEquals((Long) ids.ppSurvivant, pi.getContribuable().getNumero());
					}
					{
						final PeriodeImpositionImpotSource pi = piis.get(1);
						Assert.assertNotNull(pi);
						Assert.assertEquals(PeriodeImpositionImpotSource.Type.MIXTE, pi.getType());
						Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, pi.getTypeAutoriteFiscale());
						Assert.assertEquals((Integer) MockCommune.Cossonay.getNoOFS(), pi.getNoOfs());
						Assert.assertEquals(dateDeces.getOneDayAfter(), pi.getDateDebut());
						Assert.assertEquals(date(year, 12, 31), pi.getDateFin());
						Assert.assertNotNull(pi.getContribuable());
						Assert.assertEquals((Long) ids.ppSurvivant, pi.getContribuable().getNumero());
					}
					{
						final PeriodeImpositionImpotSource pi = piis.get(2);
						Assert.assertNotNull(pi);
						Assert.assertEquals(PeriodeImpositionImpotSource.Type.MIXTE, pi.getType());
						Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, pi.getTypeAutoriteFiscale());
						Assert.assertEquals((Integer) MockCommune.Cossonay.getNoOFS(), pi.getNoOfs());
						Assert.assertEquals(date(year + 1, 1, 1), pi.getDateDebut());
						Assert.assertEquals(date(year + 1, 12, 31), pi.getDateFin());
						Assert.assertNotNull(pi.getContribuable());
						Assert.assertEquals((Long) ids.ppSurvivant, pi.getContribuable().getNumero());
					}
				}
			}
		});
	}

	@Test
	public void testVeuvageChezSourciersPurs() throws Exception {

		final int year = RegDate.get().year() - 1;
		final RegDate dateDeces = date(year, 8, 6);
		final RegDate dateMariage = date(year - 10, 5, 3);

		final class Ids {
			long ppDecede;
			long ppSurvivant;
		}

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final PersonnePhysique decede = addNonHabitant("Gertrud", "Haenkel", date(1967, 9, 23), Sexe.FEMININ);
				decede.setDateDeces(dateDeces);
				final PersonnePhysique survivant = addNonHabitant("Alfredo", "Haenkel", date(1965, 4, 12), Sexe.MASCULIN);
				final EnsembleTiersCouple couple = addEnsembleTiersCouple(survivant, decede, dateMariage, dateDeces);
				addForPrincipal(couple.getMenage(), date(year, 1, 1), MotifFor.ARRIVEE_HS, dateDeces, MotifFor.VEUVAGE_DECES, MockCommune.Cossonay, ModeImposition.SOURCE);
				addForPrincipal(survivant, dateDeces.getOneDayAfter(), MotifFor.VEUVAGE_DECES, MockCommune.Cossonay, ModeImposition.SOURCE);

				final Ids ids = new Ids();
				ids.ppDecede = decede.getNumero();
				ids.ppSurvivant = survivant.getNumero();
				return ids;
			}
		});

		// calculs
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				// la personne physique décédée
				{
					final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ids.ppDecede);
					final List<PeriodeImpositionImpotSource> piis = service.determine(pp);
					Assert.assertEquals(1, piis.size());
					{
						final PeriodeImpositionImpotSource pi = piis.get(0);
						Assert.assertNotNull(pi);
						Assert.assertEquals(PeriodeImpositionImpotSource.Type.SOURCE, pi.getType());
						Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, pi.getTypeAutoriteFiscale());
						Assert.assertEquals((Integer) MockCommune.Cossonay.getNoOFS(), pi.getNoOfs());
						Assert.assertEquals(date(year, 1, 1), pi.getDateDebut());
						Assert.assertEquals(dateDeces, pi.getDateFin());
						Assert.assertNotNull(pi.getContribuable());
						Assert.assertEquals((Long) ids.ppDecede, pi.getContribuable().getNumero());
					}
				}

				// la personne physique survivante
				{
					final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ids.ppSurvivant);
					final List<PeriodeImpositionImpotSource> piis = service.determine(pp);
					Assert.assertEquals(3, piis.size());
					{
						final PeriodeImpositionImpotSource pi = piis.get(0);
						Assert.assertNotNull(pi);
						Assert.assertEquals(PeriodeImpositionImpotSource.Type.SOURCE, pi.getType());
						Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, pi.getTypeAutoriteFiscale());
						Assert.assertEquals((Integer) MockCommune.Cossonay.getNoOFS(), pi.getNoOfs());
						Assert.assertEquals(date(year, 1, 1), pi.getDateDebut());
						Assert.assertEquals(dateDeces, pi.getDateFin());
						Assert.assertNotNull(pi.getContribuable());
						Assert.assertEquals((Long) ids.ppSurvivant, pi.getContribuable().getNumero());
					}
					{
						final PeriodeImpositionImpotSource pi = piis.get(1);
						Assert.assertNotNull(pi);
						Assert.assertEquals(PeriodeImpositionImpotSource.Type.SOURCE, pi.getType());
						Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, pi.getTypeAutoriteFiscale());
						Assert.assertEquals((Integer) MockCommune.Cossonay.getNoOFS(), pi.getNoOfs());
						Assert.assertEquals(dateDeces.getOneDayAfter(), pi.getDateDebut());
						Assert.assertEquals(date(year, 12, 31), pi.getDateFin());
						Assert.assertNotNull(pi.getContribuable());
						Assert.assertEquals((Long) ids.ppSurvivant, pi.getContribuable().getNumero());
					}
					{
						final PeriodeImpositionImpotSource pi = piis.get(2);
						Assert.assertNotNull(pi);
						Assert.assertEquals(PeriodeImpositionImpotSource.Type.SOURCE, pi.getType());
						Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, pi.getTypeAutoriteFiscale());
						Assert.assertEquals((Integer) MockCommune.Cossonay.getNoOFS(), pi.getNoOfs());
						Assert.assertEquals(date(year + 1, 1, 1), pi.getDateDebut());
						Assert.assertEquals(date(year + 1, 12, 31), pi.getDateFin());
						Assert.assertNotNull(pi.getContribuable());
						Assert.assertEquals((Long) ids.ppSurvivant, pi.getContribuable().getNumero());
					}
				}
			}
		});
	}
}
