package ch.vd.uniregctb.metier.piis;

import java.util.List;

import org.apache.commons.lang3.mutable.MutableLong;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.tx.TxCallbackWithoutResult;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockServiceCivil;
import ch.vd.unireg.interfaces.infra.mock.MockCanton;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.uniregctb.common.BusinessTest;
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

	private interface TestRunnable {
		void run() throws Exception;
	}

	private void assertNoPiis(PersonnePhysique pp) throws PeriodeImpositionImpotSourceServiceException {
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
				Assert.assertEquals(Localisation.getVaud(), pi.getLocalisation());
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
				Assert.assertEquals(Localisation.getVaud(), pi.getLocalisation());
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
					Assert.assertEquals(Localisation.getInconnue(), pi.getLocalisation());
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
					Assert.assertEquals(Localisation.getVaud(), pi.getLocalisation());
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
					Assert.assertEquals(Localisation.getVaud(), pi.getLocalisation());
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
		final TestRunnable test = new TestRunnable() {
			@Override
			public void run() throws Exception {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(testedId.longValue());
				final List<PeriodeImpositionImpotSource> piis = service.determine(pp);
				Assert.assertNotNull(piis);
				Assert.assertEquals(3, piis.size());
				{
					final PeriodeImpositionImpotSource pi = piis.get(0);
					Assert.assertNotNull(pi);
					Assert.assertEquals(PeriodeImpositionImpotSource.Type.SOURCE, pi.getType());
					Assert.assertEquals(Localisation.getInconnue(), pi.getLocalisation());
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
					Assert.assertEquals(Localisation.getVaud(), pi.getLocalisation());
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
					Assert.assertEquals(Localisation.getVaud(), pi.getLocalisation());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, pi.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockCommune.Aigle.getNoOFS(), pi.getNoOfs());
					Assert.assertEquals(date(currentYear, 1, 1), pi.getDateDebut());
					Assert.assertEquals(date(currentYear, 12, 31), pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) testedId.longValue(), pi.getContribuable().getNumero());
				}
			}
		};

		// calcul
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
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
		final TestRunnable test = new TestRunnable() {
			@Override
			public void run() throws Exception {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(testedId.longValue());
				final List<PeriodeImpositionImpotSource> piis = service.determine(pp);
				Assert.assertNotNull(piis);
				Assert.assertEquals(3, piis.size());
				{
					final PeriodeImpositionImpotSource pi = piis.get(0);
					Assert.assertNotNull(pi);
					Assert.assertEquals(PeriodeImpositionImpotSource.Type.SOURCE, pi.getType());
					Assert.assertEquals(Localisation.getInconnue(), pi.getLocalisation());
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
					Assert.assertEquals(Localisation.getVaud(), pi.getLocalisation());
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
					Assert.assertEquals(Localisation.getVaud(), pi.getLocalisation());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, pi.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockCommune.Aigle.getNoOFS(), pi.getNoOfs());
					Assert.assertEquals(date(currentYear, 1, 1), pi.getDateDebut());
					Assert.assertEquals(date(currentYear, 12, 31), pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) testedId.longValue(), pi.getContribuable().getNumero());
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
		final TestRunnable test = new TestRunnable() {
			@Override
			public void run() throws Exception {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(testedId.longValue());
				final List<PeriodeImpositionImpotSource> piis = service.determine(pp);
				Assert.assertNotNull(piis);
				Assert.assertEquals(3, piis.size());
				{
					final PeriodeImpositionImpotSource pi = piis.get(0);
					Assert.assertNotNull(pi);
					Assert.assertEquals(PeriodeImpositionImpotSource.Type.SOURCE, pi.getType());
					Assert.assertEquals(Localisation.getHorsSuisse(MockPays.Allemagne.getNoOFS()), pi.getLocalisation());
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
					Assert.assertEquals(Localisation.getVaud(), pi.getLocalisation());
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
					Assert.assertEquals(Localisation.getVaud(), pi.getLocalisation());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, pi.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockCommune.Aigle.getNoOFS(), pi.getNoOfs());
					Assert.assertEquals(date(currentYear, 1, 1), pi.getDateDebut());
					Assert.assertEquals(date(currentYear, 12, 31), pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) testedId.longValue(), pi.getContribuable().getNumero());
				}
			}
		};

		// calcul
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
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
		final TestRunnable test = new TestRunnable() {
			@Override
			public void run() throws Exception {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(testedId.longValue());
				final List<PeriodeImpositionImpotSource> piis = service.determine(pp);
				Assert.assertNotNull(piis);
				Assert.assertEquals(3, piis.size());
				{
					final PeriodeImpositionImpotSource pi = piis.get(0);
					Assert.assertNotNull(pi);
					Assert.assertEquals(PeriodeImpositionImpotSource.Type.SOURCE, pi.getType());
					Assert.assertEquals(Localisation.getHorsSuisse(MockPays.Allemagne.getNoOFS()), pi.getLocalisation());
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
					Assert.assertEquals(Localisation.getVaud(), pi.getLocalisation());
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
					Assert.assertEquals(Localisation.getVaud(), pi.getLocalisation());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, pi.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockCommune.Aigle.getNoOFS(), pi.getNoOfs());
					Assert.assertEquals(date(currentYear, 1, 1), pi.getDateDebut());
					Assert.assertEquals(date(currentYear, 12, 31), pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) testedId.longValue(), pi.getContribuable().getNumero());
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
		final TestRunnable test = new TestRunnable() {
			@Override
			public void run() throws Exception {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(testedId.longValue());
				final List<PeriodeImpositionImpotSource> piis = service.determine(pp);
				Assert.assertNotNull(piis);
				Assert.assertEquals(2, piis.size());
				{
					final PeriodeImpositionImpotSource pi = piis.get(0);
					Assert.assertNotNull(pi);
					Assert.assertEquals(PeriodeImpositionImpotSource.Type.MIXTE, pi.getType());
					Assert.assertEquals(Localisation.getVaud(), pi.getLocalisation());
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
					Assert.assertEquals(Localisation.getHorsSuisse(MockPays.Allemagne.getNoOFS()), pi.getLocalisation());
					Assert.assertEquals(TypeAutoriteFiscale.PAYS_HS, pi.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockPays.Allemagne.getNoOFS(), pi.getNoOfs());
					Assert.assertEquals(depart.getOneDayAfter(), pi.getDateDebut());
					Assert.assertEquals(date(lastYear, 12, 31), pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) testedId.longValue(), pi.getContribuable().getNumero());
				}
			}
		};
		final TestRunnable testEmpty = new TestRunnable() {
			@Override
			public void run() throws Exception {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(testedId.longValue());
				assertNoPiis(pp);
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
		final TestRunnable test = new TestRunnable() {
			@Override
			public void run() throws Exception {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(testedId.longValue());
				final List<PeriodeImpositionImpotSource> piis = service.determine(pp);
				Assert.assertNotNull(piis);
				Assert.assertEquals(2, piis.size());
				{
					final PeriodeImpositionImpotSource pi = piis.get(0);
					Assert.assertNotNull(pi);
					Assert.assertEquals(PeriodeImpositionImpotSource.Type.MIXTE, pi.getType());
					Assert.assertEquals(Localisation.getVaud(), pi.getLocalisation());
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
					Assert.assertEquals(Localisation.getHorsSuisse(MockPays.Allemagne.getNoOFS()), pi.getLocalisation());
					Assert.assertEquals(TypeAutoriteFiscale.PAYS_HS, pi.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockPays.Allemagne.getNoOFS(), pi.getNoOfs());
					Assert.assertEquals(depart.getOneDayAfter(), pi.getDateDebut());
					Assert.assertEquals(date(lastYear, 12, 31), pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) testedId.longValue(), pi.getContribuable().getNumero());
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
		final TestRunnable test = new TestRunnable() {
			@Override
			public void run() throws Exception {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(testedId.longValue());
				final List<PeriodeImpositionImpotSource> piis = service.determine(pp);
				Assert.assertNotNull(piis);
				Assert.assertEquals(2, piis.size());
				{
					final PeriodeImpositionImpotSource pi = piis.get(0);
					Assert.assertNotNull(pi);
					Assert.assertEquals(PeriodeImpositionImpotSource.Type.MIXTE, pi.getType());
					Assert.assertEquals(Localisation.getVaud(), pi.getLocalisation());
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
					Assert.assertEquals(Localisation.getHorsSuisse(MockPays.Allemagne.getNoOFS()), pi.getLocalisation());
					Assert.assertEquals(TypeAutoriteFiscale.PAYS_HS, pi.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockPays.Allemagne.getNoOFS(), pi.getNoOfs());
					Assert.assertEquals(depart.getOneDayAfter(), pi.getDateDebut());
					Assert.assertEquals(date(lastYear, 12, 31), pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) testedId.longValue(), pi.getContribuable().getNumero());
				}
			}
		};
		final TestRunnable testEmpty = new TestRunnable() {
			@Override
			public void run() throws Exception {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(testedId.longValue());
				assertNoPiis(pp);
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
		final TestRunnable test = new TestRunnable() {
			@Override
			public void run() throws Exception {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(testedId.longValue());
				final List<PeriodeImpositionImpotSource> piis = service.determine(pp);
				Assert.assertNotNull(piis);
				Assert.assertEquals(2, piis.size());
				{
					final PeriodeImpositionImpotSource pi = piis.get(0);
					Assert.assertNotNull(pi);
					Assert.assertEquals(PeriodeImpositionImpotSource.Type.MIXTE, pi.getType());
					Assert.assertEquals(Localisation.getVaud(), pi.getLocalisation());
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
					Assert.assertEquals(Localisation.getHorsSuisse(MockPays.Allemagne.getNoOFS()), pi.getLocalisation());
					Assert.assertEquals(TypeAutoriteFiscale.PAYS_HS, pi.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockPays.Allemagne.getNoOFS(), pi.getNoOfs());
					Assert.assertEquals(depart.getOneDayAfter(), pi.getDateDebut());
					Assert.assertEquals(date(lastYear, 12, 31), pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) testedId.longValue(), pi.getContribuable().getNumero());
				}
			}
		};

		// calcul
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
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
	public void testArriveeHcOrdinaireMixte2SansRattachementEconomiquePrealableNiRT() throws Exception {

		final int currentYear = RegDate.get().year();
		final int lastYear = currentYear - 1;
		final RegDate arrivee = date(lastYear, 6, 15);

		final class Ids {
			long ppOrdinaire;
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
		final TestRunnable test = new TestRunnable() {
			@Override
			public void run() throws Exception {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(testedId.longValue());
				final List<PeriodeImpositionImpotSource> piis = service.determine(pp);
				Assert.assertNotNull(piis);
				Assert.assertEquals(2, piis.size());
				{
					final PeriodeImpositionImpotSource pi = piis.get(0);
					Assert.assertNotNull(pi);
					Assert.assertEquals(PeriodeImpositionImpotSource.Type.MIXTE, pi.getType());
					Assert.assertEquals(Localisation.getVaud(), pi.getLocalisation());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, pi.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockCommune.Aigle.getNoOFS(), pi.getNoOfs());
					Assert.assertEquals(date(lastYear, 1, 1), pi.getDateDebut());
					Assert.assertEquals(date(lastYear, 12, 31), pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) testedId.longValue(), pi.getContribuable().getNumero());
				}
				{
					final PeriodeImpositionImpotSource pi = piis.get(1);
					Assert.assertNotNull(pi);
					Assert.assertEquals(PeriodeImpositionImpotSource.Type.MIXTE, pi.getType());
					Assert.assertEquals(Localisation.getVaud(), pi.getLocalisation());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, pi.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockCommune.Aigle.getNoOFS(), pi.getNoOfs());
					Assert.assertEquals(date(currentYear, 1, 1), pi.getDateDebut());
					Assert.assertEquals(date(currentYear, 12, 31), pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) testedId.longValue(), pi.getContribuable().getNumero());
				}
			}
		};
		final TestRunnable testEmpty = new TestRunnable() {
			@Override
			public void run() throws Exception {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(testedId.longValue());
				assertNoPiis(pp);
			}
		};

		// calcul
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				testedId.setValue(ids.ppOrdinaire);
				testEmpty.run();

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
	 * Cas 17 & 18 (sans RT)
	 */
	@Test
	public void testArriveeHcMixte1SansRattachementEconomiquePrealableNiRT() throws Exception {

		final int currentYear = RegDate.get().year();
		final int lastYear = currentYear - 1;
		final RegDate arrivee = date(lastYear, 6, 15);

		// mise en place fiscale
		final long ppMixte1 = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Alastair M1", "O'Malley", date(1978, 5, 2), Sexe.MASCULIN);
				addForPrincipal(pp, arrivee, MotifFor.ARRIVEE_HC, MockCommune.Aigle, ModeImposition.MIXTE_137_1);
				return pp.getNumero();
			}
		});

		// calcul
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppMixte1);
				final List<PeriodeImpositionImpotSource> piis = service.determine(pp);
				Assert.assertNotNull(piis);
				Assert.assertEquals(3, piis.size());
				{
					final PeriodeImpositionImpotSource pi = piis.get(0);
					Assert.assertNotNull(pi);
					Assert.assertEquals(PeriodeImpositionImpotSource.Type.SOURCE, pi.getType());
					Assert.assertEquals(Localisation.getInconnue(), pi.getLocalisation());
					Assert.assertNull(pi.getTypeAutoriteFiscale());
					Assert.assertNull(pi.getNoOfs());
					Assert.assertEquals(date(lastYear, 1, 1), pi.getDateDebut());
					Assert.assertEquals(arrivee.getLastDayOfTheMonth(), pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) ppMixte1, pi.getContribuable().getNumero());
				}
				{
					final PeriodeImpositionImpotSource pi = piis.get(1);
					Assert.assertNotNull(pi);
					Assert.assertEquals(PeriodeImpositionImpotSource.Type.MIXTE, pi.getType());
					Assert.assertEquals(Localisation.getVaud(), pi.getLocalisation());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, pi.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockCommune.Aigle.getNoOFS(), pi.getNoOfs());
					Assert.assertEquals(arrivee.getLastDayOfTheMonth().getOneDayAfter(), pi.getDateDebut());
					Assert.assertEquals(date(lastYear, 12, 31), pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) ppMixte1, pi.getContribuable().getNumero());
				}
				{
					final PeriodeImpositionImpotSource pi = piis.get(2);
					Assert.assertNotNull(pi);
					Assert.assertEquals(PeriodeImpositionImpotSource.Type.MIXTE, pi.getType());
					Assert.assertEquals(Localisation.getVaud(), pi.getLocalisation());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, pi.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockCommune.Aigle.getNoOFS(), pi.getNoOfs());
					Assert.assertEquals(date(currentYear, 1, 1), pi.getDateDebut());
					Assert.assertEquals(date(currentYear, 12, 31), pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) ppMixte1, pi.getContribuable().getNumero());
				}
			}
		});
	}

	/**
	 * Cas 17 & 18 (avec RT)
	 */
	@Test
	public void testArriveeHcMixte1SansRattachementEconomiquePrealableAvecRT() throws Exception {

		final int currentYear = RegDate.get().year();
		final int lastYear = currentYear - 1;
		final RegDate arrivee = date(lastYear, 6, 15);

		// mise en place fiscale
		final long ppMixte1 = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.MENSUEL, date(2009, 1, 1));
				final PersonnePhysique pp = addNonHabitant("Alastair M1", "O'Malley", date(1978, 5, 2), Sexe.MASCULIN);
				addForPrincipal(pp, arrivee, MotifFor.ARRIVEE_HC, MockCommune.Aigle, ModeImposition.MIXTE_137_1);
				addRapportPrestationImposable(dpi, pp, arrivee, null, false);
				return pp.getNumero();
			}
		});

		// calcul
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppMixte1);
				final List<PeriodeImpositionImpotSource> piis = service.determine(pp);
				Assert.assertNotNull(piis);
				Assert.assertEquals(3, piis.size());
				{
					final PeriodeImpositionImpotSource pi = piis.get(0);
					Assert.assertNotNull(pi);
					Assert.assertEquals(PeriodeImpositionImpotSource.Type.SOURCE, pi.getType());
					Assert.assertEquals(Localisation.getInconnue(), pi.getLocalisation());
					Assert.assertNull(pi.getTypeAutoriteFiscale());
					Assert.assertNull(pi.getNoOfs());
					Assert.assertEquals(date(lastYear, 1, 1), pi.getDateDebut());
					Assert.assertEquals(arrivee.getLastDayOfTheMonth(), pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) ppMixte1, pi.getContribuable().getNumero());
				}
				{
					final PeriodeImpositionImpotSource pi = piis.get(1);
					Assert.assertNotNull(pi);
					Assert.assertEquals(PeriodeImpositionImpotSource.Type.MIXTE, pi.getType());
					Assert.assertEquals(Localisation.getVaud(), pi.getLocalisation());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, pi.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockCommune.Aigle.getNoOFS(), pi.getNoOfs());
					Assert.assertEquals(arrivee.getLastDayOfTheMonth().getOneDayAfter(), pi.getDateDebut());
					Assert.assertEquals(date(lastYear, 12, 31), pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) ppMixte1, pi.getContribuable().getNumero());
				}
				{
					final PeriodeImpositionImpotSource pi = piis.get(2);
					Assert.assertNotNull(pi);
					Assert.assertEquals(PeriodeImpositionImpotSource.Type.MIXTE, pi.getType());
					Assert.assertEquals(Localisation.getVaud(), pi.getLocalisation());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, pi.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockCommune.Aigle.getNoOFS(), pi.getNoOfs());
					Assert.assertEquals(date(currentYear, 1, 1), pi.getDateDebut());
					Assert.assertEquals(date(currentYear, 12, 31), pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) ppMixte1, pi.getContribuable().getNumero());
				}
			}
		});
	}

	/**
	 * Cas 17 & 18 (avec RT)
	 */
	@Test
	public void testArriveeHcOrdinaireOuMixte2SansRattachementEconomiquePrealableAvecRT() throws Exception {

		final int currentYear = RegDate.get().year();
		final int lastYear = currentYear - 1;
		final RegDate arrivee = date(lastYear, 6, 15);

		final class Ids {
			long ppOrdinaire;
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
		final TestRunnable test = new TestRunnable() {
			@Override
			public void run() throws Exception {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(testedId.longValue());
				final List<PeriodeImpositionImpotSource> piis = service.determine(pp);
				Assert.assertNotNull(piis);
				Assert.assertEquals(2, piis.size());
				{
					final PeriodeImpositionImpotSource pi = piis.get(0);
					Assert.assertNotNull(pi);
					Assert.assertEquals(PeriodeImpositionImpotSource.Type.MIXTE, pi.getType());
					Assert.assertEquals(Localisation.getVaud(), pi.getLocalisation());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, pi.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockCommune.Aigle.getNoOFS(), pi.getNoOfs());
					Assert.assertEquals(date(lastYear, 1, 1), pi.getDateDebut());
					Assert.assertEquals(date(lastYear, 12, 31), pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) testedId.longValue(), pi.getContribuable().getNumero());
				}
				{
					final PeriodeImpositionImpotSource pi = piis.get(1);
					Assert.assertNotNull(pi);
					Assert.assertEquals(PeriodeImpositionImpotSource.Type.MIXTE, pi.getType());
					Assert.assertEquals(Localisation.getVaud(), pi.getLocalisation());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, pi.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockCommune.Aigle.getNoOFS(), pi.getNoOfs());
					Assert.assertEquals(date(currentYear, 1, 1), pi.getDateDebut());
					Assert.assertEquals(date(currentYear, 12, 31), pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) testedId.longValue(), pi.getContribuable().getNumero());
				}
			}
		};

		// calcul
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				testedId.setValue(ids.ppOrdinaire);
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
	 * Cas 17 & 18 (avec RT)
	 */
	@Test
	public void testArriveeHcOrdinaireOuMixte2PasseAuRoleDansLaPFSansRattachementEconomiquePrealableAvecRT() throws Exception {

		final int currentYear = RegDate.get().year();
		final int lastYear = currentYear - 1;
		final RegDate dateRole = date(lastYear, 4, 12);
		final RegDate arrivee = date(lastYear, 6, 15);

		final class Ids {
			long ppOrdinaire;
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
					addForPrincipal(pp, date(lastYear - 1, 1, 1), MotifFor.ARRIVEE_HS, dateRole.getOneDayBefore(), MotifFor.PERMIS_C_SUISSE, MockCommune.Bern, ModeImposition.SOURCE);
					addForPrincipal(pp, dateRole, MotifFor.PERMIS_C_SUISSE, arrivee.getOneDayBefore(), MotifFor.ARRIVEE_HC, MockCommune.Bern, ModeImposition.ORDINAIRE);
					addForPrincipal(pp, arrivee, MotifFor.ARRIVEE_HC, MockCommune.Aigle, ModeImposition.ORDINAIRE);
					addRapportPrestationImposable(dpi, pp, arrivee, null, false);
					ids.ppOrdinaire = pp.getNumero();
				}
				{
					final PersonnePhysique pp = addNonHabitant("Alastair M2", "O'Malley", date(1978, 5, 2), Sexe.MASCULIN);
					addForPrincipal(pp, date(lastYear - 1, 1, 1), MotifFor.ARRIVEE_HS, dateRole.getOneDayBefore(), MotifFor.PERMIS_C_SUISSE, MockCommune.Bern, ModeImposition.SOURCE);
					addForPrincipal(pp, dateRole, MotifFor.PERMIS_C_SUISSE, arrivee.getOneDayBefore(), MotifFor.ARRIVEE_HC, MockCommune.Bern, ModeImposition.ORDINAIRE);
					addForPrincipal(pp, arrivee, MotifFor.ARRIVEE_HC, MockCommune.Aigle, ModeImposition.MIXTE_137_2);
					addRapportPrestationImposable(dpi, pp, arrivee, null, false);
					ids.ppMixte2 = pp.getNumero();
				}
				{
					final PersonnePhysique pp = addNonHabitant("Alastair ICCD", "O'Malley", date(1978, 5, 2), Sexe.MASCULIN);
					addForPrincipal(pp, date(lastYear - 1, 1, 1), MotifFor.ARRIVEE_HS, dateRole.getOneDayBefore(), MotifFor.PERMIS_C_SUISSE, MockCommune.Bern, ModeImposition.SOURCE);
					addForPrincipal(pp, dateRole, MotifFor.PERMIS_C_SUISSE, arrivee.getOneDayBefore(), MotifFor.ARRIVEE_HC, MockCommune.Bern, ModeImposition.ORDINAIRE);
					addForPrincipal(pp, arrivee, MotifFor.ARRIVEE_HC, MockCommune.Aigle, ModeImposition.DEPENSE);
					addRapportPrestationImposable(dpi, pp, arrivee, null, false);
					ids.ppDepense = pp.getNumero();
				}
				{
					final PersonnePhysique pp = addNonHabitant("Alastair IND", "O'Malley", date(1978, 5, 2), Sexe.MASCULIN);
					addForPrincipal(pp, date(lastYear - 1, 1, 1), MotifFor.ARRIVEE_HS, dateRole.getOneDayBefore(), MotifFor.PERMIS_C_SUISSE, MockCommune.Bern, ModeImposition.SOURCE);
					addForPrincipal(pp, dateRole, MotifFor.PERMIS_C_SUISSE, arrivee.getOneDayBefore(), MotifFor.ARRIVEE_HC, MockCommune.Bern, ModeImposition.ORDINAIRE);
					addForPrincipal(pp, arrivee, MotifFor.ARRIVEE_HC, MockCommune.Aigle, ModeImposition.INDIGENT);
					addRapportPrestationImposable(dpi, pp, arrivee, null, false);
					ids.ppIndigent = pp.getNumero();
				}
				return ids;
			}
		});

		final MutableLong testedId = new MutableLong();
		final TestRunnable test = new TestRunnable() {
			@Override
			public void run() throws Exception {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(testedId.longValue());
				final List<PeriodeImpositionImpotSource> piis = service.determine(pp);
				Assert.assertNotNull(piis);
				Assert.assertEquals(3, piis.size());
				{
					final PeriodeImpositionImpotSource pi = piis.get(0);
					Assert.assertNotNull(pi);
					Assert.assertEquals(PeriodeImpositionImpotSource.Type.SOURCE, pi.getType());
					Assert.assertEquals(Localisation.getHorsCanton(MockCanton.Berne.getSigleOFS()), pi.getLocalisation());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_HC, pi.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockCommune.Bern.getNoOFS(), pi.getNoOfs());
					Assert.assertEquals(date(lastYear, 1, 1), pi.getDateDebut());
					Assert.assertEquals(dateRole.getLastDayOfTheMonth(), pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) testedId.longValue(), pi.getContribuable().getNumero());
				}
				{
					final PeriodeImpositionImpotSource pi = piis.get(1);
					Assert.assertNotNull(pi);
					Assert.assertEquals(PeriodeImpositionImpotSource.Type.MIXTE, pi.getType());
					Assert.assertEquals(Localisation.getVaud(), pi.getLocalisation());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, pi.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockCommune.Aigle.getNoOFS(), pi.getNoOfs());
					Assert.assertEquals(dateRole.getLastDayOfTheMonth().getOneDayAfter(), pi.getDateDebut());
					Assert.assertEquals(date(lastYear, 12, 31), pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) testedId.longValue(), pi.getContribuable().getNumero());
				}
				{
					final PeriodeImpositionImpotSource pi = piis.get(2);
					Assert.assertNotNull(pi);
					Assert.assertEquals(PeriodeImpositionImpotSource.Type.MIXTE, pi.getType());
					Assert.assertEquals(Localisation.getVaud(), pi.getLocalisation());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, pi.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockCommune.Aigle.getNoOFS(), pi.getNoOfs());
					Assert.assertEquals(date(currentYear, 1, 1), pi.getDateDebut());
					Assert.assertEquals(date(currentYear, 12, 31), pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) testedId.longValue(), pi.getContribuable().getNumero());
				}
			}
		};

		// calcul
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				testedId.setValue(ids.ppOrdinaire);
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
	public void testArriveeHcMixte1AvecRattachementEconomiquePrealableSansRT() throws Exception {

		final int currentYear = RegDate.get().year();
		final int lastYear = currentYear - 1;
		final RegDate achat = date(lastYear, 1, 1);
		final RegDate arrivee = date(lastYear, 6, 15);

		// mise en place fiscale
		final Long ppMixte1 = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Alastair M1", "O'Malley", date(1978, 5, 2), Sexe.MASCULIN);
				addForPrincipal(pp, achat, MotifFor.ACHAT_IMMOBILIER, arrivee.getOneDayBefore(), MotifFor.ARRIVEE_HC, MockCommune.Bern, ModeImposition.ORDINAIRE);
				addForPrincipal(pp, arrivee, MotifFor.ARRIVEE_HC, MockCommune.Aigle, ModeImposition.MIXTE_137_1);
				addForSecondaire(pp, achat, MotifFor.ACHAT_IMMOBILIER, MockCommune.Leysin.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
				return pp.getNumero();
			}
		});

		// calcul
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppMixte1);
				final List<PeriodeImpositionImpotSource> piis = service.determine(pp);
				Assert.assertNotNull(piis);
				Assert.assertEquals(3, piis.size());
				{
					final PeriodeImpositionImpotSource pi = piis.get(0);
					Assert.assertNotNull(pi);
					Assert.assertEquals(PeriodeImpositionImpotSource.Type.SOURCE, pi.getType());
					Assert.assertEquals(Localisation.getHorsCanton("BE"), pi.getLocalisation());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_HC, pi.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockCommune.Bern.getNoOFS(), pi.getNoOfs());
					Assert.assertEquals(date(lastYear, 1, 1), pi.getDateDebut());
					Assert.assertEquals(arrivee.getLastDayOfTheMonth(), pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) ppMixte1, pi.getContribuable().getNumero());
				}
				{
					final PeriodeImpositionImpotSource pi = piis.get(1);
					Assert.assertNotNull(pi);
					Assert.assertEquals(PeriodeImpositionImpotSource.Type.MIXTE, pi.getType());
					Assert.assertEquals(Localisation.getVaud(), pi.getLocalisation());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, pi.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockCommune.Aigle.getNoOFS(), pi.getNoOfs());
					Assert.assertEquals(arrivee.getLastDayOfTheMonth().getOneDayAfter(), pi.getDateDebut());
					Assert.assertEquals(date(lastYear, 12, 31), pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) ppMixte1, pi.getContribuable().getNumero());
				}
				{
					final PeriodeImpositionImpotSource pi = piis.get(2);
					Assert.assertNotNull(pi);
					Assert.assertEquals(PeriodeImpositionImpotSource.Type.MIXTE, pi.getType());
					Assert.assertEquals(Localisation.getVaud(), pi.getLocalisation());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, pi.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockCommune.Aigle.getNoOFS(), pi.getNoOfs());
					Assert.assertEquals(date(currentYear, 1, 1), pi.getDateDebut());
					Assert.assertEquals(date(currentYear, 12, 31), pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) ppMixte1, pi.getContribuable().getNumero());
				}
			}
		});
	}

	/**
	 * Cas 19 / 17b (sans RT)
	 */
	@Test
	public void testArriveeHcOrdinaireOuMixte2AvecRattachementEconomiquePrealableSansRT() throws Exception {

		final int currentYear = RegDate.get().year();
		final int lastYear = currentYear - 1;
		final RegDate achat = date(lastYear, 1, 1);
		final RegDate arrivee = date(lastYear, 6, 15);

		final class Ids {
			long ppOrdinaire;
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
		final TestRunnable test = new TestRunnable() {
			@Override
			public void run() throws Exception {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(testedId.longValue());
				final List<PeriodeImpositionImpotSource> piis = service.determine(pp);
				Assert.assertNotNull(piis);
				Assert.assertEquals(2, piis.size());
				{
					final PeriodeImpositionImpotSource pi = piis.get(0);
					Assert.assertNotNull(pi);
					Assert.assertEquals(PeriodeImpositionImpotSource.Type.MIXTE, pi.getType());
					Assert.assertEquals(Localisation.getVaud(), pi.getLocalisation());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, pi.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockCommune.Aigle.getNoOFS(), pi.getNoOfs());
					Assert.assertEquals(date(lastYear, 1, 1), pi.getDateDebut());
					Assert.assertEquals(date(lastYear, 12, 31), pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) testedId.longValue(), pi.getContribuable().getNumero());
				}
				{
					final PeriodeImpositionImpotSource pi = piis.get(1);
					Assert.assertNotNull(pi);
					Assert.assertEquals(PeriodeImpositionImpotSource.Type.MIXTE, pi.getType());
					Assert.assertEquals(Localisation.getVaud(), pi.getLocalisation());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, pi.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockCommune.Aigle.getNoOFS(), pi.getNoOfs());
					Assert.assertEquals(date(currentYear, 1, 1), pi.getDateDebut());
					Assert.assertEquals(date(currentYear, 12, 31), pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) testedId.longValue(), pi.getContribuable().getNumero());
				}
			}
		};
		final TestRunnable testEmpty = new TestRunnable() {
			@Override
			public void run() throws Exception {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(testedId.longValue());
				assertNoPiis(pp);
			}
		};

		// calcul
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				testedId.setValue(ids.ppOrdinaire);
				testEmpty.run();

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
	public void testArriveeHcMixte1AvecRattachementEconomiquePrealableAvecRT() throws Exception {

		final int currentYear = RegDate.get().year();
		final int lastYear = currentYear - 1;
		final RegDate achat = date(lastYear, 1, 1);
		final RegDate arrivee = date(lastYear, 6, 15);

		// mise en place fiscale
		final long ppMixte1 = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.MENSUEL, date(2009, 1, 1));
				final PersonnePhysique pp = addNonHabitant("Alastair M1", "O'Malley", date(1978, 5, 2), Sexe.MASCULIN);
				addForPrincipal(pp, achat, MotifFor.ACHAT_IMMOBILIER, arrivee.getOneDayBefore(), MotifFor.ARRIVEE_HC, MockCommune.Bern, ModeImposition.ORDINAIRE);
				addForPrincipal(pp, arrivee, MotifFor.ARRIVEE_HC, MockCommune.Aigle, ModeImposition.MIXTE_137_1);
				addForSecondaire(pp, achat, MotifFor.ACHAT_IMMOBILIER, MockCommune.Leysin.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
				addRapportPrestationImposable(dpi, pp, arrivee, null, false);
				return pp.getNumero();
			}
		});

		// calcul
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppMixte1);
				final List<PeriodeImpositionImpotSource> piis = service.determine(pp);
				Assert.assertNotNull(piis);
				Assert.assertEquals(3, piis.size());
				{
					final PeriodeImpositionImpotSource pi = piis.get(0);
					Assert.assertNotNull(pi);
					Assert.assertEquals(PeriodeImpositionImpotSource.Type.SOURCE, pi.getType());
					Assert.assertEquals(Localisation.getHorsCanton("BE"), pi.getLocalisation());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_HC, pi.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockCommune.Bern.getNoOFS(), pi.getNoOfs());
					Assert.assertEquals(date(lastYear, 1, 1), pi.getDateDebut());
					Assert.assertEquals(arrivee.getLastDayOfTheMonth(), pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) ppMixte1, pi.getContribuable().getNumero());
				}
				{
					final PeriodeImpositionImpotSource pi = piis.get(1);
					Assert.assertNotNull(pi);
					Assert.assertEquals(PeriodeImpositionImpotSource.Type.MIXTE, pi.getType());
					Assert.assertEquals(Localisation.getVaud(), pi.getLocalisation());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, pi.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockCommune.Aigle.getNoOFS(), pi.getNoOfs());
					Assert.assertEquals(arrivee.getLastDayOfTheMonth().getOneDayAfter(), pi.getDateDebut());
					Assert.assertEquals(date(lastYear, 12, 31), pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) ppMixte1, pi.getContribuable().getNumero());
				}
				{
					final PeriodeImpositionImpotSource pi = piis.get(2);
					Assert.assertNotNull(pi);
					Assert.assertEquals(PeriodeImpositionImpotSource.Type.MIXTE, pi.getType());
					Assert.assertEquals(Localisation.getVaud(), pi.getLocalisation());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, pi.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockCommune.Aigle.getNoOFS(), pi.getNoOfs());
					Assert.assertEquals(date(currentYear, 1, 1), pi.getDateDebut());
					Assert.assertEquals(date(currentYear, 12, 31), pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) ppMixte1, pi.getContribuable().getNumero());
				}
			}
		});
	}

	/**
	 * Cas 19 / 17b (avec RT)
	 */
	@Test
	public void testArriveeHcOrdinaireOuMixte2AvecRattachementEconomiquePrealableAvecRT() throws Exception {

		final int currentYear = RegDate.get().year();
		final int lastYear = currentYear - 1;
		final RegDate achat = date(lastYear, 1, 1);
		final RegDate arrivee = date(lastYear, 6, 15);

		final class Ids {
			long ppOrdinaire;
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
		final TestRunnable test = new TestRunnable() {
			@Override
			public void run() throws Exception {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(testedId.longValue());
				final List<PeriodeImpositionImpotSource> piis = service.determine(pp);
				Assert.assertNotNull(piis);
				Assert.assertEquals(2, piis.size());
				{
					final PeriodeImpositionImpotSource pi = piis.get(0);
					Assert.assertNotNull(pi);
					Assert.assertEquals(PeriodeImpositionImpotSource.Type.MIXTE, pi.getType());
					Assert.assertEquals(Localisation.getVaud(), pi.getLocalisation());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, pi.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockCommune.Aigle.getNoOFS(), pi.getNoOfs());
					Assert.assertEquals(date(lastYear, 1, 1), pi.getDateDebut());
					Assert.assertEquals(date(lastYear, 12, 31), pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) testedId.longValue(), pi.getContribuable().getNumero());
				}
				{
					final PeriodeImpositionImpotSource pi = piis.get(1);
					Assert.assertNotNull(pi);
					Assert.assertEquals(PeriodeImpositionImpotSource.Type.MIXTE, pi.getType());
					Assert.assertEquals(Localisation.getVaud(), pi.getLocalisation());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, pi.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockCommune.Aigle.getNoOFS(), pi.getNoOfs());
					Assert.assertEquals(date(currentYear, 1, 1), pi.getDateDebut());
					Assert.assertEquals(date(currentYear, 12, 31), pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) testedId.longValue(), pi.getContribuable().getNumero());
				}
			}
		};

		// calcul
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				testedId.setValue(ids.ppOrdinaire);
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
	 * Cas 20 et 23
	 */
	@Test
	public void testDepartHorsCantonMixte2() throws Exception {

		final int year = RegDate.get().year() - 1;
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
			final TestRunnable test = new TestRunnable() {
				@Override
				public void run() throws Exception {
					final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(testedId.longValue());
					final List<PeriodeImpositionImpotSource> piis = service.determine(pp);
					Assert.assertNotNull(piis);
					Assert.assertEquals(2, piis.size());
					{
						final PeriodeImpositionImpotSource pi = piis.get(0);
						Assert.assertNotNull(pi);
						Assert.assertEquals(PeriodeImpositionImpotSource.Type.MIXTE, pi.getType());
						Assert.assertEquals(Localisation.getVaud(), pi.getLocalisation());
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
						Assert.assertEquals(Localisation.getHorsCanton("BE"), pi.getLocalisation());
						Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_HC, pi.getTypeAutoriteFiscale());
						Assert.assertEquals((Integer) MockCommune.Bern.getNoOFS(), pi.getNoOfs());
						Assert.assertEquals(depart.getLastDayOfTheMonth().getOneDayAfter(), pi.getDateDebut());
						Assert.assertEquals(date(year, 12, 31), pi.getDateFin());
						Assert.assertNotNull(pi.getContribuable());
						Assert.assertEquals((Long) testedId.longValue(), pi.getContribuable().getNumero());
					}
				}
			};
			// calcul
			doInNewTransactionAndSession(new TxCallbackWithoutResult() {
				@Override
				public void execute(TransactionStatus status) throws Exception {
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
	 * Cas 21 et 22
	 */
	@Test
	public void testDepartHorsCantonMixte1OuOrdinaire() throws Exception {

		final int year = RegDate.get().year() - 1;
		final RegDate depart = date(year, 6, 23);

		final class Ids {
			long ppSrc;
			long ppOrd;
		}

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
		final TestRunnable test = new TestRunnable() {
			@Override
			public void run() throws Exception {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(testedId.longValue());
				final List<PeriodeImpositionImpotSource> piis = service.determine(pp);
				Assert.assertNotNull(piis);
				Assert.assertEquals(2, piis.size());
				{
					final PeriodeImpositionImpotSource pi = piis.get(0);
					Assert.assertNotNull(pi);
					Assert.assertEquals(PeriodeImpositionImpotSource.Type.SOURCE, pi.getType());
					Assert.assertEquals(Localisation.getVaud(), pi.getLocalisation());
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
					Assert.assertEquals(Localisation.getHorsCanton("BE"), pi.getLocalisation());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_HC, pi.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockCommune.Bern.getNoOFS(), pi.getNoOfs());
					Assert.assertEquals(depart.getLastDayOfTheMonth().getOneDayAfter(), pi.getDateDebut());
					Assert.assertEquals(date(year, 12, 31), pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) testedId.longValue(), pi.getContribuable().getNumero());
				}
			}
		};
		// calcul
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				testedId.setValue(ids.ppSrc);
				test.run();

				testedId.setValue(ids.ppOrd);
				test.run();
			}
		});
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
					Assert.assertEquals(Localisation.getVaud(), pi.getLocalisation());
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
					Assert.assertEquals(Localisation.getVaud(), pi.getLocalisation());
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
	 * Cas 24 & 25 (avec obtention du permis un premier de mois avant 2014)
	 */
	@Test
	public void testSourcierPurObtientPermisCOuNationalitePremierJourDuMois() throws Exception {

		final int year = 2013;
		final RegDate obtention = date(year, 5, 1);

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
					Assert.assertEquals(Localisation.getVaud(), pi.getLocalisation());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, pi.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockCommune.Aigle.getNoOFS(), pi.getNoOfs());
					Assert.assertEquals(date(year, 1, 1), pi.getDateDebut());
					Assert.assertEquals(obtention.getOneDayBefore(), pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) ppId, pi.getContribuable().getNumero());
				}
				{
					final PeriodeImpositionImpotSource pi = piis.get(1);
					Assert.assertNotNull(pi);
					Assert.assertEquals(PeriodeImpositionImpotSource.Type.MIXTE, pi.getType());
					Assert.assertEquals(Localisation.getVaud(), pi.getLocalisation());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, pi.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockCommune.Aigle.getNoOFS(), pi.getNoOfs());
					Assert.assertEquals(obtention, pi.getDateDebut());
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
					Assert.assertEquals(Localisation.getVaud(), pi.getLocalisation());
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
					Assert.assertEquals(Localisation.getVaud(), pi.getLocalisation());
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
					Assert.assertEquals(Localisation.getVaud(), pi.getLocalisation());
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
					Assert.assertEquals(Localisation.getInconnue(), pi.getLocalisation());
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
					Assert.assertEquals(Localisation.getHorsSuisse(MockPays.Danemark.getNoOFS()), pi.getLocalisation());
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
					Assert.assertEquals(Localisation.getHorsSuisse(MockPays.Danemark.getNoOFS()), pi.getLocalisation());
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
					Assert.assertEquals(Localisation.getInconnue(), pi.getLocalisation());
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
					Assert.assertEquals(Localisation.getHorsCanton("BE"), pi.getLocalisation());
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
					Assert.assertEquals(Localisation.getHorsCanton("BE"), pi.getLocalisation());
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
					Assert.assertEquals(Localisation.getVaud(), pi.getLocalisation());
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
					Assert.assertEquals(Localisation.getHorsSuisse(MockPays.Allemagne.getNoOFS()), pi.getLocalisation());
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
					Assert.assertEquals(Localisation.getHorsCanton("GE"), pi.getLocalisation());
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
						Assert.assertEquals(Localisation.getVaud(), pi.getLocalisation());
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
						Assert.assertEquals(Localisation.getVaud(), pi.getLocalisation());
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
						Assert.assertEquals(Localisation.getVaud(), pi.getLocalisation());
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
						Assert.assertEquals(Localisation.getVaud(), pi.getLocalisation());
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
						Assert.assertEquals(Localisation.getVaud(), pi.getLocalisation());
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
						Assert.assertEquals(Localisation.getVaud(), pi.getLocalisation());
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
					Assert.assertEquals(3, piis.size());
					{
						final PeriodeImpositionImpotSource pi = piis.get(0);
						Assert.assertNotNull(pi);
						Assert.assertEquals(PeriodeImpositionImpotSource.Type.SOURCE, pi.getType());
						Assert.assertEquals(Localisation.getVaud(), pi.getLocalisation());
						Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, pi.getTypeAutoriteFiscale());
						Assert.assertEquals((Integer) MockCommune.Bex.getNoOFS(), pi.getNoOfs());
						Assert.assertEquals(date(year, 1, 1), pi.getDateDebut());
						Assert.assertEquals(date(year, 5, 31), pi.getDateFin());
						Assert.assertNotNull(pi.getContribuable());
						Assert.assertEquals((Long) ids.ppSourcier, pi.getContribuable().getNumero());
					}
					{
						final PeriodeImpositionImpotSource pi = piis.get(1);
						Assert.assertNotNull(pi);
						Assert.assertEquals(PeriodeImpositionImpotSource.Type.MIXTE, pi.getType());
						Assert.assertEquals(Localisation.getVaud(), pi.getLocalisation());
						Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, pi.getTypeAutoriteFiscale());
						Assert.assertEquals((Integer) MockCommune.Bex.getNoOFS(), pi.getNoOfs());
						Assert.assertEquals(date(year, 6, 1), pi.getDateDebut());
						Assert.assertEquals(date(year, 12, 31), pi.getDateFin());
						Assert.assertNotNull(pi.getContribuable());
						Assert.assertEquals((Long) ids.ppSourcier, pi.getContribuable().getNumero());
					}
					{
						final PeriodeImpositionImpotSource pi = piis.get(2);
						Assert.assertNotNull(pi);
						Assert.assertEquals(PeriodeImpositionImpotSource.Type.MIXTE, pi.getType());
						Assert.assertEquals(Localisation.getVaud(), pi.getLocalisation());
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
						Assert.assertEquals(Localisation.getVaud(), pi.getLocalisation());
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
						Assert.assertEquals(Localisation.getVaud(), pi.getLocalisation());
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
						Assert.assertEquals(Localisation.getVaud(), pi.getLocalisation());
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
						Assert.assertEquals(Localisation.getVaud(), pi.getLocalisation());
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
						Assert.assertEquals(Localisation.getVaud(), pi.getLocalisation());
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
						Assert.assertEquals(Localisation.getVaud(), pi.getLocalisation());
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
						Assert.assertEquals(Localisation.getVaud(), pi.getLocalisation());
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
						Assert.assertEquals(Localisation.getVaud(), pi.getLocalisation());
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
						Assert.assertEquals(Localisation.getVaud(), pi.getLocalisation());
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
						Assert.assertEquals(Localisation.getVaud(), pi.getLocalisation());
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
					Assert.assertEquals(Localisation.getVaud(), pi.getLocalisation());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, pi.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockCommune.Cossonay.getNoOFS(), pi.getNoOfs());
					Assert.assertEquals(date(year, 1, 1), pi.getDateDebut());
					Assert.assertEquals(obtention.getLastDayOfTheMonth(), pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) ppId, pi.getContribuable().getNumero());
				}
				{
					final PeriodeImpositionImpotSource pi = piis.get(1);
					Assert.assertNotNull(pi);
					Assert.assertEquals(PeriodeImpositionImpotSource.Type.MIXTE, pi.getType());
					Assert.assertEquals(Localisation.getVaud(), pi.getLocalisation());
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
					Assert.assertEquals(Localisation.getVaud(), pi.getLocalisation());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, pi.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockCommune.Cossonay.getNoOFS(), pi.getNoOfs());
					Assert.assertEquals(date(year, 1, 1), pi.getDateDebut());
					Assert.assertEquals(obtention.getLastDayOfTheMonth(), pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) ppId, pi.getContribuable().getNumero());
				}
				{
					final PeriodeImpositionImpotSource pi = piis.get(1);
					Assert.assertNotNull(pi);
					Assert.assertEquals(PeriodeImpositionImpotSource.Type.MIXTE, pi.getType());
					Assert.assertEquals(Localisation.getVaud(), pi.getLocalisation());
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
						Assert.assertEquals(Localisation.getVaud(), pi.getLocalisation());
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
						Assert.assertEquals(Localisation.getVaud(), pi.getLocalisation());
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
						Assert.assertEquals(Localisation.getVaud(), pi.getLocalisation());
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
						Assert.assertEquals(Localisation.getVaud(), pi.getLocalisation());
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
						Assert.assertEquals(Localisation.getVaud(), pi.getLocalisation());
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
						Assert.assertEquals(Localisation.getVaud(), pi.getLocalisation());
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
						Assert.assertEquals(Localisation.getVaud(), pi.getLocalisation());
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
						Assert.assertEquals(Localisation.getVaud(), pi.getLocalisation());
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

	/**
	 * Cas 36
	 */
	@Test
	public void testObtentionPermisCHorsCantonPuisArriveeMemeMois() throws Exception {

		final int year = RegDate.get().year() - 1;
		final RegDate obtention = date(year, 7, 12);
		final RegDate arrivee = obtention.getLastDayOfTheMonth().addDays(-5);

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Alfredo", "Garcia", date(1976, 5, 31), Sexe.MASCULIN);
				addForPrincipal(pp, date(year, 1, 1), MotifFor.INDETERMINE, obtention.getOneDayBefore(), MotifFor.PERMIS_C_SUISSE, MockCommune.Bern, ModeImposition.SOURCE);
				addForPrincipal(pp, obtention, MotifFor.PERMIS_C_SUISSE, arrivee.getOneDayBefore(), MotifFor.ARRIVEE_HC, MockCommune.Bern, ModeImposition.ORDINAIRE);
				addForPrincipal(pp, arrivee, MotifFor.ARRIVEE_HC, MockCommune.Echallens);

				final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.MENSUEL, date(2009, 1, 1));
				addRapportPrestationImposable(dpi, pp, date(year, 1, 1), obtention.getLastDayOfTheMonth().addMonths(2), false);
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
					Assert.assertEquals(Localisation.getHorsCanton("BE"), pi.getLocalisation());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_HC, pi.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockCommune.Bern.getNoOFS(), pi.getNoOfs());
					Assert.assertEquals(date(year, 1, 1), pi.getDateDebut());
					Assert.assertEquals(obtention.getLastDayOfTheMonth(), pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) ppId, pi.getContribuable().getNumero());
				}
				{
					final PeriodeImpositionImpotSource pi = piis.get(1);
					Assert.assertNotNull(pi);
					Assert.assertEquals(PeriodeImpositionImpotSource.Type.MIXTE, pi.getType());
					Assert.assertEquals(Localisation.getVaud(), pi.getLocalisation());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, pi.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockCommune.Echallens.getNoOFS(), pi.getNoOfs());
					Assert.assertEquals(obtention.getLastDayOfTheMonth().getOneDayAfter(), pi.getDateDebut());
					Assert.assertEquals(date(year, 12, 31), pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) ppId, pi.getContribuable().getNumero());
				}
			}
		});
	}

	/**
	 * C'est alors la différence des types d'autorité fiscale qui doit déclencher le décalage au mois suivant
	 */
	@Test
	public void testArriveeHorsCantonAvecMauvaisMotif() throws Exception {

		final int year = RegDate.get().year() - 1;
		final RegDate arrivee = date(year, 5, 12);

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Perceval", "Jackson", date(1987, 3, 12), Sexe.MASCULIN);
				addForPrincipal(pp, date(year, 1, 1), MotifFor.INDETERMINE, arrivee.getOneDayBefore(), MotifFor.DEMENAGEMENT_VD, MockCommune.Bern, ModeImposition.SOURCE);
				addForPrincipal(pp, arrivee, MotifFor.DEMENAGEMENT_VD, MockCommune.Lausanne, ModeImposition.MIXTE_137_1);
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
					Assert.assertEquals(Localisation.getHorsCanton("BE"), pi.getLocalisation());
					Assert.assertEquals(PeriodeImpositionImpotSource.Type.SOURCE, pi.getType());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_HC, pi.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockCommune.Bern.getNoOFS(), pi.getNoOfs());
					Assert.assertEquals(date(year, 1, 1), pi.getDateDebut());
					Assert.assertEquals(arrivee.getLastDayOfTheMonth(), pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) ppId, pi.getContribuable().getNumero());
				}
				{
					final PeriodeImpositionImpotSource pi = piis.get(1);
					Assert.assertNotNull(pi);
					Assert.assertEquals(PeriodeImpositionImpotSource.Type.MIXTE, pi.getType());
					Assert.assertEquals(Localisation.getVaud(), pi.getLocalisation());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, pi.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockCommune.Lausanne.getNoOFS(), pi.getNoOfs());
					Assert.assertEquals(arrivee.getLastDayOfTheMonth().getOneDayAfter(), pi.getDateDebut());
					Assert.assertEquals(date(year, 12, 31), pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) ppId, pi.getContribuable().getNumero());
				}
				{
					final PeriodeImpositionImpotSource pi = piis.get(2);
					Assert.assertNotNull(pi);
					Assert.assertEquals(PeriodeImpositionImpotSource.Type.MIXTE, pi.getType());
					Assert.assertEquals(Localisation.getVaud(), pi.getLocalisation());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, pi.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockCommune.Lausanne.getNoOFS(), pi.getNoOfs());
					Assert.assertEquals(date(year + 1, 1, 1), pi.getDateDebut());
					Assert.assertEquals(date(year + 1, 12, 31), pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) ppId, pi.getContribuable().getNumero());
				}
			}
		});
	}

	/**
	 * Le changement de canton doit provoquer une coupure de la période d'imposition IS et un décalage au mois suivant
	 */
	@Test
	public void testChangementDeCanton() throws Exception {

		final int year = RegDate.get().year() - 1;
		final RegDate demenagement = date(year, 5, 12);

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Perceval", "Jackson", date(1987, 3, 12), Sexe.MASCULIN);
				addForPrincipal(pp, date(year, 1, 1), MotifFor.INDETERMINE, demenagement.getOneDayBefore(), MotifFor.DEMENAGEMENT_VD, MockCommune.Bern, ModeImposition.SOURCE);
				addForPrincipal(pp, demenagement, MotifFor.DEMENAGEMENT_VD, MockCommune.Sierre, ModeImposition.SOURCE);

				final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.MENSUEL, date(2009, 1, 1));
				addRapportPrestationImposable(dpi, pp, date(year, 1, 1), date(year, 12, 31), false);
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
					Assert.assertEquals(Localisation.getHorsCanton("BE"), pi.getLocalisation());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_HC, pi.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockCommune.Bern.getNoOFS(), pi.getNoOfs());
					Assert.assertEquals(date(year, 1, 1), pi.getDateDebut());
					Assert.assertEquals(demenagement.getLastDayOfTheMonth(), pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) ppId, pi.getContribuable().getNumero());
				}
				{
					final PeriodeImpositionImpotSource pi = piis.get(1);
					Assert.assertNotNull(pi);
					Assert.assertEquals(PeriodeImpositionImpotSource.Type.SOURCE, pi.getType());
					Assert.assertEquals(Localisation.getHorsCanton("VS"), pi.getLocalisation());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_HC, pi.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockCommune.Sierre.getNoOFS(), pi.getNoOfs());
					Assert.assertEquals(demenagement.getLastDayOfTheMonth().getOneDayAfter(), pi.getDateDebut());
					Assert.assertEquals(date(year, 12, 31), pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) ppId, pi.getContribuable().getNumero());
				}
			}
		});
	}

	/**
	 * Le changement de pays doit provoquer une coupure de la période d'imposition IS sans décalage de date
	 */
	@Test
	public void testChangementDePays() throws Exception {

		final int year = RegDate.get().year() - 1;
		final RegDate demenagement = date(year, 5, 12);

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Perceval", "Jackson", date(1987, 3, 12), Sexe.MASCULIN);
				addForPrincipal(pp, date(year, 1, 1), MotifFor.INDETERMINE, demenagement.getOneDayBefore(), MotifFor.DEMENAGEMENT_VD, MockPays.Allemagne, ModeImposition.SOURCE);
				addForPrincipal(pp, demenagement, MotifFor.DEMENAGEMENT_VD, null, null, MockPays.France, ModeImposition.SOURCE);

				final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.MENSUEL, date(2009, 1, 1));
				addRapportPrestationImposable(dpi, pp, date(year, 1, 1), date(year, 12, 31), false);
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
					Assert.assertEquals(Localisation.getHorsSuisse(MockPays.Allemagne.getNoOFS()), pi.getLocalisation());
					Assert.assertEquals(TypeAutoriteFiscale.PAYS_HS, pi.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockPays.Allemagne.getNoOFS(), pi.getNoOfs());
					Assert.assertEquals(date(year, 1, 1), pi.getDateDebut());
					Assert.assertEquals(demenagement.getOneDayBefore(), pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) ppId, pi.getContribuable().getNumero());
				}
				{
					final PeriodeImpositionImpotSource pi = piis.get(1);
					Assert.assertNotNull(pi);
					Assert.assertEquals(PeriodeImpositionImpotSource.Type.SOURCE, pi.getType());
					Assert.assertEquals(Localisation.getHorsSuisse(MockPays.France.getNoOFS()), pi.getLocalisation());
					Assert.assertEquals(TypeAutoriteFiscale.PAYS_HS, pi.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockPays.France.getNoOFS(), pi.getNoOfs());
					Assert.assertEquals(demenagement, pi.getDateDebut());
					Assert.assertEquals(date(year, 12, 31), pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) ppId, pi.getContribuable().getNumero());
				}
			}
		});
	}

	/**
	 * Cas d'un contribuable sans for (= sourcier gris ?) dont les rapports de travail sont interrompus une PF complète
	 * -> il ne doit pas y avoir de PIIS sur cette période, mais seulement sur les autres autour
	 */
	@Test
	public void testSansForAvecTrouUnePeriodeDansRT() throws Exception {

		final int baseYear = RegDate.get().year() - 3;
		final RegDate finPremierRT = date(baseYear, 11, 23);
		final RegDate debutDeuxiemeRT = date(baseYear + 2, 3, 6);
		final RegDate finDeuxiemeRT = date(baseYear + 2, 7, 31);

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Mamfred", "Dogart", date(1991, 3, 1), Sexe.MASCULIN);
				final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.MENSUEL, date(2009, 1, 1));
				addRapportPrestationImposable(dpi, pp, date(baseYear, 1, 1), finPremierRT, false);
				addRapportPrestationImposable(dpi, pp, debutDeuxiemeRT, finDeuxiemeRT, false);
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
					Assert.assertEquals(Localisation.getInconnue(), pi.getLocalisation());
					Assert.assertNull(pi.getTypeAutoriteFiscale());
					Assert.assertNull(pi.getNoOfs());
					Assert.assertEquals(date(baseYear, 1, 1), pi.getDateDebut());
					Assert.assertEquals(date(baseYear, 12, 31), pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) ppId, pi.getContribuable().getNumero());
				}
				{
					final PeriodeImpositionImpotSource pi = piis.get(1);
					Assert.assertNotNull(pi);
					Assert.assertEquals(PeriodeImpositionImpotSource.Type.SOURCE, pi.getType());
					Assert.assertEquals(Localisation.getInconnue(), pi.getLocalisation());
					Assert.assertNull(pi.getTypeAutoriteFiscale());
					Assert.assertNull(pi.getNoOfs());
					Assert.assertEquals(date(baseYear + 2, 1, 1), pi.getDateDebut());
					Assert.assertEquals(date(baseYear + 2, 12, 31), pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) ppId, pi.getContribuable().getNumero());
				}
			}
		});
	}

	/**
	 * Cas d'une personne qui obtient son permis C / sa nationalité suisse puis décède avant la fin du mois
	 * -> la période d'imposition IS source ne doit se prolonger que jusqu'à la date du décès (et pas jusqu'à la fin du mois)
	 */
	@Test
	public void testObtentionPermisCPuisDecesMemeMois() throws Exception {

		final int year = RegDate.get().year() - 1;
		final RegDate obtention = date(year, 7, 4);
		final RegDate deces = obtention.getLastDayOfTheMonth().addDays(-5);

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Berthe", "BigFoot", date(1934, 2, 12), Sexe.FEMININ);
				pp.setDateDeces(deces);
				addForPrincipal(pp, date(year, 1, 1), MotifFor.INDETERMINE, obtention.getOneDayBefore(), MotifFor.PERMIS_C_SUISSE, MockCommune.Cossonay, ModeImposition.SOURCE);
				addForPrincipal(pp, obtention, MotifFor.PERMIS_C_SUISSE, deces, MotifFor.VEUVAGE_DECES, MockCommune.Cossonay);
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
					Assert.assertEquals(Localisation.getVaud(), pi.getLocalisation());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, pi.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockCommune.Cossonay.getNoOFS(), pi.getNoOfs());
					Assert.assertEquals(date(year, 1, 1), pi.getDateDebut());
					Assert.assertEquals(deces, pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) ppId, pi.getContribuable().getNumero());
				}
			}
		});
	}

	/**
	 * Cas d'une personne qui obtient son permis C / sa nationalité suisse puis part HS avant la fin du mois
	 * -> la période d'imposition IS source ne doit se prolonger que jusqu'à la date du départ (et pas jusqu'à la fin du mois)
	 */
	@Test
	public void testObtentionPermisCPuisDepartHSMemeMois() throws Exception {

		final int year = RegDate.get().year() - 1;
		final RegDate obtention = date(year, 7, 4);
		final RegDate depart = obtention.getLastDayOfTheMonth().addDays(-5);

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Berthe", "BigFoot", date(1934, 2, 12), Sexe.FEMININ);
				addForPrincipal(pp, date(year, 1, 1), MotifFor.INDETERMINE, obtention.getOneDayBefore(), MotifFor.PERMIS_C_SUISSE, MockCommune.Cossonay, ModeImposition.SOURCE);
				addForPrincipal(pp, obtention, MotifFor.PERMIS_C_SUISSE, depart, MotifFor.DEPART_HS, MockCommune.Cossonay);
				addForPrincipal(pp, depart.getOneDayAfter(), MotifFor.DEPART_HS, MockPays.Allemagne);
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
					Assert.assertEquals(Localisation.getVaud(), pi.getLocalisation());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, pi.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockCommune.Cossonay.getNoOFS(), pi.getNoOfs());
					Assert.assertEquals(date(year, 1, 1), pi.getDateDebut());
					Assert.assertEquals(depart, pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) ppId, pi.getContribuable().getNumero());
				}
				{
					final PeriodeImpositionImpotSource pi = piis.get(1);
					Assert.assertNotNull(pi);
					Assert.assertEquals(PeriodeImpositionImpotSource.Type.SOURCE, pi.getType());
					Assert.assertEquals(Localisation.getHorsSuisse(MockPays.Allemagne.getNoOFS()), pi.getLocalisation());
					Assert.assertEquals(TypeAutoriteFiscale.PAYS_HS, pi.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockPays.Allemagne.getNoOFS(), pi.getNoOfs());
					Assert.assertEquals(depart.getOneDayAfter(), pi.getDateDebut());
					Assert.assertEquals(date(year, 12, 31), pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) ppId, pi.getContribuable().getNumero());
				}
			}
		});
	}

	/**
	 * Cas d'une personne qui obtient son permis C / sa nationalité suisse HS puis débarque avant la fin du mois
	 * -> la période d'imposition IS source ne doit se prolonger que jusqu'à la date la veille de l'arrivée (et pas jusqu'à la fin du mois)
	 */
	@Test
	public void testObtentionPermisCPuisArriveeHSMemeMois() throws Exception {

		final int year = RegDate.get().year() - 1;
		final RegDate obtention = date(year, 7, 4);
		final RegDate arrivee = obtention.getLastDayOfTheMonth().addDays(-5);

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Berthe", "BigFoot", date(1934, 2, 12), Sexe.FEMININ);
				addForPrincipal(pp, date(year, 1, 1), MotifFor.INDETERMINE, obtention.getOneDayBefore(), MotifFor.PERMIS_C_SUISSE, MockPays.France, ModeImposition.SOURCE);
				addForPrincipal(pp, obtention, MotifFor.PERMIS_C_SUISSE, arrivee.getOneDayBefore(), MotifFor.ARRIVEE_HS, MockPays.France);
				addForPrincipal(pp, arrivee, MotifFor.ARRIVEE_HS, MockCommune.Aigle);

				final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.MENSUEL, date(2009, 1, 1));
				addRapportPrestationImposable(dpi, pp, date(year, 1, 1), date(year, 10, 31), false);
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
					Assert.assertEquals(Localisation.getHorsSuisse(MockPays.France.getNoOFS()), pi.getLocalisation());
					Assert.assertEquals(TypeAutoriteFiscale.PAYS_HS, pi.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockPays.France.getNoOFS(), pi.getNoOfs());
					Assert.assertEquals(date(year, 1, 1), pi.getDateDebut());
					Assert.assertEquals(arrivee.getOneDayBefore(), pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) ppId, pi.getContribuable().getNumero());
				}
				{
					final PeriodeImpositionImpotSource pi = piis.get(1);
					Assert.assertNotNull(pi);
					Assert.assertEquals(PeriodeImpositionImpotSource.Type.MIXTE, pi.getType());
					Assert.assertEquals(Localisation.getVaud(), pi.getLocalisation());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, pi.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockCommune.Aigle.getNoOFS(), pi.getNoOfs());
					Assert.assertEquals(arrivee, pi.getDateDebut());
					Assert.assertEquals(date(year, 12, 31), pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) ppId, pi.getContribuable().getNumero());
				}
			}
		});
	}

	/**
	 * Cas d'une personne qui obtient son permis C / sa nationalité suisse HS puis change de pays avant la fin du mois
	 * -> la période d'imposition IS source ne doit se prolonger que jusqu'à la date la veille du déménagement (et pas jusqu'à la fin du mois)
	 */
	@Test
	public void testObtentionPermisCPuisChangementPaysHSMemeMois() throws Exception {

		final int year = RegDate.get().year() - 1;
		final RegDate obtention = date(year, 7, 4);
		final RegDate demenagement = obtention.getLastDayOfTheMonth().addDays(-5);

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Berthe", "BigFoot", date(1934, 2, 12), Sexe.FEMININ);
				addForPrincipal(pp, date(year, 1, 1), MotifFor.INDETERMINE, obtention.getOneDayBefore(), MotifFor.PERMIS_C_SUISSE, MockPays.France, ModeImposition.SOURCE);
				addForPrincipal(pp, obtention, MotifFor.PERMIS_C_SUISSE, demenagement.getOneDayBefore(), MotifFor.DEMENAGEMENT_VD, MockPays.France);
				addForPrincipal(pp, demenagement, MotifFor.DEMENAGEMENT_VD, MockPays.Allemagne);

				final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.MENSUEL, date(2009, 1, 1));
				addRapportPrestationImposable(dpi, pp, date(year, 1, 1), date(year, 10, 31), false);
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
					Assert.assertEquals(Localisation.getHorsSuisse(MockPays.France.getNoOFS()), pi.getLocalisation());
					Assert.assertEquals(TypeAutoriteFiscale.PAYS_HS, pi.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockPays.France.getNoOFS(), pi.getNoOfs());
					Assert.assertEquals(date(year, 1, 1), pi.getDateDebut());
					Assert.assertEquals(demenagement.getOneDayBefore(), pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) ppId, pi.getContribuable().getNumero());
				}
				{
					final PeriodeImpositionImpotSource pi = piis.get(1);
					Assert.assertNotNull(pi);
					Assert.assertEquals(PeriodeImpositionImpotSource.Type.SOURCE, pi.getType());
					Assert.assertEquals(Localisation.getHorsSuisse(MockPays.Allemagne.getNoOFS()), pi.getLocalisation());
					Assert.assertEquals(TypeAutoriteFiscale.PAYS_HS, pi.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockPays.Allemagne.getNoOFS(), pi.getNoOfs());
					Assert.assertEquals(demenagement, pi.getDateDebut());
					Assert.assertEquals(date(year, 12, 31), pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) ppId, pi.getContribuable().getNumero());
				}
			}
		});
	}

	/**
	 * Cas où un même ménage commun est présent plusieurs fois dans les rapports d'appartenance ménage d'un contribuable
	 * -> il ne faut pas que les fors se dédoublent...
	 */
	@Test
	public void testReconciliation() throws Exception {

		final RegDate dateMariage = date(2010, 6, 12);
		final RegDate dateSeparation = date(2011, 3, 24);
		final RegDate dateReconciliation = date(2012, 5, 15);
		final RegDate dateDeces = date(2012, 6, 2);

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Alfred", "Dugenou", date(1978, 5, 21), Sexe.MASCULIN);
				pp.setDateDeces(dateDeces);
				final EnsembleTiersCouple couple = addEnsembleTiersCouple(pp, null, dateMariage, dateSeparation.getOneDayBefore());
				final MenageCommun mc = couple.getMenage();
				addAppartenanceMenage(mc, pp, dateReconciliation, null, false);

				addForPrincipal(pp, date(2010, 1, 1), MotifFor.INDETERMINE, dateMariage.getOneDayBefore(), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Lausanne, ModeImposition.SOURCE);
				addForPrincipal(mc, dateMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, dateSeparation.getOneDayBefore(), MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, MockCommune.Lausanne, ModeImposition.SOURCE);
				addForPrincipal(pp, dateSeparation, MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, dateReconciliation.getOneDayBefore(), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Lausanne, ModeImposition.SOURCE);
				addForPrincipal(mc, dateReconciliation, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, dateDeces, MotifFor.VEUVAGE_DECES, MockCommune.Lausanne, ModeImposition.SOURCE);
				return pp.getNumero();
			}
		});

		// calculs
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
					Assert.assertEquals(Localisation.getVaud(), pi.getLocalisation());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, pi.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockCommune.Lausanne.getNoOFS(), pi.getNoOfs());
					Assert.assertEquals(date(2010, 1, 1), pi.getDateDebut());
					Assert.assertEquals(date(2010, 12, 31), pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) ppId, pi.getContribuable().getNumero());
				}
				{
					final PeriodeImpositionImpotSource pi = piis.get(1);
					Assert.assertNotNull(pi);
					Assert.assertEquals(PeriodeImpositionImpotSource.Type.SOURCE, pi.getType());
					Assert.assertEquals(Localisation.getVaud(), pi.getLocalisation());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, pi.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockCommune.Lausanne.getNoOFS(), pi.getNoOfs());
					Assert.assertEquals(date(2011, 1, 1), pi.getDateDebut());
					Assert.assertEquals(date(2011, 12, 31), pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) ppId, pi.getContribuable().getNumero());
				}
				{
					final PeriodeImpositionImpotSource pi = piis.get(2);
					Assert.assertNotNull(pi);
					Assert.assertEquals(PeriodeImpositionImpotSource.Type.SOURCE, pi.getType());
					Assert.assertEquals(Localisation.getVaud(), pi.getLocalisation());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, pi.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockCommune.Lausanne.getNoOFS(), pi.getNoOfs());
					Assert.assertEquals(date(2012, 1, 1), pi.getDateDebut());
					Assert.assertEquals(dateDeces, pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) ppId, pi.getContribuable().getNumero());
				}
			}
		});
	}

	@Test
	public void testMotifDebutNullSurPremierForSuiviParArriveeHC() throws Exception {

		final int year = RegDate.get().year() - 1;
		final RegDate arrivee = date(year, 6, 6);

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Gudule", "Massepain", date(1978, 7, 23), Sexe.FEMININ);
				addForPrincipal(pp, date(year, 1, 1), null, arrivee.getOneDayBefore(), MotifFor.ARRIVEE_HC, MockCommune.Bern, ModeImposition.SOURCE);
				addForPrincipal(pp, arrivee, MotifFor.ARRIVEE_HC, MockCommune.Bex, ModeImposition.SOURCE);
				return pp.getNumero();
			}
		});

		// calculs
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
					Assert.assertEquals(Localisation.getHorsCanton("BE"), pi.getLocalisation());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_HC, pi.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockCommune.Bern.getNoOFS(), pi.getNoOfs());
					Assert.assertEquals(date(year, 1, 1), pi.getDateDebut());
					Assert.assertEquals(arrivee.getLastDayOfTheMonth(), pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) ppId, pi.getContribuable().getNumero());
				}
				{
					final PeriodeImpositionImpotSource pi = piis.get(1);
					Assert.assertNotNull(pi);
					Assert.assertEquals(PeriodeImpositionImpotSource.Type.SOURCE, pi.getType());
					Assert.assertEquals(Localisation.getVaud(), pi.getLocalisation());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, pi.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockCommune.Bex.getNoOFS(), pi.getNoOfs());
					Assert.assertEquals(arrivee.getLastDayOfTheMonth().getOneDayAfter(), pi.getDateDebut());
					Assert.assertEquals(date(year, 12, 31), pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) ppId, pi.getContribuable().getNumero());
				}
				{
					final PeriodeImpositionImpotSource pi = piis.get(2);
					Assert.assertNotNull(pi);
					Assert.assertEquals(PeriodeImpositionImpotSource.Type.SOURCE, pi.getType());
					Assert.assertEquals(Localisation.getVaud(), pi.getLocalisation());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, pi.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockCommune.Bex.getNoOFS(), pi.getNoOfs());
					Assert.assertEquals(date(year + 1, 1, 1), pi.getDateDebut());
					Assert.assertEquals(date(year + 1, 12, 31), pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) ppId, pi.getContribuable().getNumero());
				}
			}
		});
	}

	/**
	 * Ce qui est important ici, c'est la localisation de la période d'imposition IS (la commune devrait être celle d'après le déménagement,
	 * puisque normalement l'obtention d'un permis C pour un mixte ne doit pas pousser au mois suivant)
	 */
	@Test
	public void testMixtePermisCPuisDemenagementMemeMoisFinAnnee() throws Exception {

		final int year = RegDate.get().year() - 1;
		final RegDate permis = date(year, 12, 3);
		final RegDate demenagement = date(year, 12, 15);

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Rusticule", "de Saint-André", date(1967, 7, 26), Sexe.FEMININ);
				addForPrincipal(pp, date(year, 1, 1), MotifFor.ARRIVEE_HS, permis.getOneDayBefore(), MotifFor.PERMIS_C_SUISSE, MockCommune.Aubonne, ModeImposition.MIXTE_137_1);
				addForPrincipal(pp, permis, MotifFor.PERMIS_C_SUISSE, demenagement.getOneDayBefore(), MotifFor.DEMENAGEMENT_VD, MockCommune.Aubonne);
				addForPrincipal(pp, demenagement, MotifFor.DEMENAGEMENT_VD, MockCommune.Bussigny);
				return pp.getNumero();
			}
		});

		// calcul des piis
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
					Assert.assertEquals(Localisation.getVaud(), pi.getLocalisation());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, pi.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockCommune.Bussigny.getNoOFS(), pi.getNoOfs());
					Assert.assertEquals(date(year, 1, 1), pi.getDateDebut());
					Assert.assertEquals(date(year, 12, 31), pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) ppId, pi.getContribuable().getNumero());
				}
			}
		});
	}

	/**
	 * Ce qui est important ici, c'est la localisation de la période d'imposition IS (la commune devrait être celle d'après le déménagement,
	 * même si normalement l'obtention d'un permis C pour un sourcier pur pousse au mois suivant car on doit prendre le dernier for vaudois)
	 */
	@Test
	public void testSourcierPurPermisCPuisDemenagementMemeMoisFinAnnee() throws Exception {

		final int year = RegDate.get().year() - 1;
		final RegDate permis = date(year, 12, 3);
		final RegDate demenagement = date(year, 12, 15);

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Rusticule", "de Saint-André", date(1967, 7, 26), Sexe.FEMININ);
				addForPrincipal(pp, date(year, 1, 1), MotifFor.ARRIVEE_HS, permis.getOneDayBefore(), MotifFor.PERMIS_C_SUISSE, MockCommune.Aubonne, ModeImposition.SOURCE);
				addForPrincipal(pp, permis, MotifFor.PERMIS_C_SUISSE, demenagement.getOneDayBefore(), MotifFor.DEMENAGEMENT_VD, MockCommune.Aubonne);
				addForPrincipal(pp, demenagement, MotifFor.DEMENAGEMENT_VD, MockCommune.Bussigny);
				return pp.getNumero();
			}
		});

		// calcul des piis
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
					Assert.assertEquals(Localisation.getVaud(), pi.getLocalisation());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, pi.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockCommune.Bussigny.getNoOFS(), pi.getNoOfs());
					Assert.assertEquals(date(year, 1, 1), pi.getDateDebut());
					Assert.assertEquals(date(year, 12, 31), pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) ppId, pi.getContribuable().getNumero());
				}
			}
		});
	}

	@Test
	public void testChevauchementDeFors() throws Exception {

		// mise en place sans validation car ce n'est pas un cas accepté par la validation (pp appartenant à plusieurs mc)
		final long ppId = doInNewTransactionAndSessionWithoutValidation(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Philibert", "Macaroni", date(1984, 3, 1), Sexe.MASCULIN);
				final EnsembleTiersCouple couple1 = addEnsembleTiersCouple(pp, null, date(2013, 4, 2), null);
				final EnsembleTiersCouple couple2 = addEnsembleTiersCouple(pp, null, date(2013, 5, 1), null);

				addForPrincipal(pp, date(2012, 3, 1), MotifFor.MAJORITE, date(2013, 4, 1), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Bussigny, ModeImposition.SOURCE);
				addForPrincipal(couple1.getMenage(), date(2013, 4, 2), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Bussigny, ModeImposition.SOURCE);
				addForPrincipal(couple2.getMenage(), date(2013, 5, 1), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Bussigny, ModeImposition.SOURCE);
				return pp.getNumero();
			}
		});

		// calculs
		try {
			doInNewTransactionAndSession(new TxCallbackWithoutResult() {
				@Override
				public void execute(TransactionStatus status) throws Exception {
					final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
					service.determine(pp);
					Assert.fail("La détermination aurait dû sauter en raison des chevauchements de fors");
				}
			});
		}
		catch (PeriodeImpositionImpotSourceServiceException e) {
			Assert.assertTrue(e.getMessage(), e.getMessage().startsWith("Chevauchement de fors principaux"));
		}
	}

	/**
	 * Exemple donné par R. Carbo dans un mail du 18.11 :
	 * <ul>
	 *     <li>sourcier lausannois qui obtient son permis C / la nationalité suisse le 15.02</li>
	 *     <li>déménagement à Morges en mars</li>
	 *     <li>départ HC (Neuchâtel) en avril</li>
	 *     <li>déménagement de Neuch' à Péseux (NE) en mai</li>
	 *     <li>déménagement de Péseux (NE) à Berne (BE) en juin</li>
	 *     <li>retour à Nyon en juillet</li>
	 *     <li>déménagement à Vevey en octobre</li>
	 * </ul>
	 * <br/>
	 * Le nombre de PIIS attendues est de 5 :
	 * <ol>
	 *     <li>une SOURCE sur Morges sur janvier à février</li>
	 *     <li>une MIXTE sur Morges sur mars à avril</li>
	 *     <li>une SOURCE sur Péseux (NE) sur mai/juin</li>
	 *     <li>une SOURCE sur Berne (BE) en juillet</li>
	 *     <li>une MIXTE sur Vevey d'août à décembre</li>
	 * </ol>
	 * <br/>
	 * <strong>[SIFISC-18817] Changement d'algorithme</strong>
	 * <br/>
	 * Le nombre de PIIS attendues est maintenant réduit à 2 :
	 * <ol>
	 *     <li>une SOURCE sur Morges sur janvier et février</li>
	 *     <li>une MIXTE sur Vevey de mars à décembre</li>
	 * </ol>
	 */
	@Test
	public void testNationalitePuisPlusieursDemenagementDansAnneeAvecPassageHC() throws Exception {

		final int year = RegDate.get().year() - 1;

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Gigi", "Jampère", date(1972, 6, 23), Sexe.FEMININ);
				addForPrincipal(pp, date(year, 1, 1), MotifFor.INDETERMINE, date(year, 2, 15), MotifFor.PERMIS_C_SUISSE, MockCommune.Lausanne, ModeImposition.SOURCE);
				addForPrincipal(pp, date(year, 2, 16), MotifFor.PERMIS_C_SUISSE, date(year, 3, 10), MotifFor.DEMENAGEMENT_VD, MockCommune.Lausanne);
				addForPrincipal(pp, date(year, 3, 11), MotifFor.DEMENAGEMENT_VD, date(year, 4, 22), MotifFor.DEPART_HC, MockCommune.Morges);
				addForPrincipal(pp, date(year, 4, 23), MotifFor.DEPART_HC, date(year, 5, 4), MotifFor.DEMENAGEMENT_VD, MockCommune.Neuchatel);
				addForPrincipal(pp, date(year, 5, 5), MotifFor.DEMENAGEMENT_VD, date(year, 6, 18), MotifFor.DEMENAGEMENT_VD, MockCommune.Peseux);
				addForPrincipal(pp, date(year, 6, 19), MotifFor.DEMENAGEMENT_VD, date(year, 7, 25), MotifFor.ARRIVEE_HC, MockCommune.Bern);
				addForPrincipal(pp, date(year, 7, 26), MotifFor.ARRIVEE_HC, date(year, 10, 7), MotifFor.DEMENAGEMENT_VD, MockCommune.Nyon);
				addForPrincipal(pp, date(year, 10, 8), MotifFor.DEMENAGEMENT_VD, MockCommune.Vevey);
				return pp.getNumero();
			}
		});

		// calculs des périodes
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
					Assert.assertEquals(Localisation.getVaud(), pi.getLocalisation());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, pi.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockCommune.Morges.getNoOFS(), pi.getNoOfs());
					Assert.assertEquals(date(year, 1, 1), pi.getDateDebut());
					Assert.assertEquals(date(year, 3, 1).getOneDayBefore(), pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) ppId, pi.getContribuable().getNumero());
				}
				{
					final PeriodeImpositionImpotSource pi = piis.get(1);
					Assert.assertNotNull(pi);
					Assert.assertEquals(PeriodeImpositionImpotSource.Type.MIXTE, pi.getType());
					Assert.assertEquals(Localisation.getVaud(), pi.getLocalisation());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, pi.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockCommune.Vevey.getNoOFS(), pi.getNoOfs());
					Assert.assertEquals(date(year, 3, 1), pi.getDateDebut());
					Assert.assertEquals(date(year, 12, 31).getLastDayOfTheMonth(), pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) ppId, pi.getContribuable().getNumero());
				}
			}
		});
	}

	/**
	 * Exemple donné par R. Carbo dans un mail du 18.11 :
	 * <ul>
	 *     <li>sourcier lausannois qui se marie avec un suisse le 16.02</li>
	 *     <li>déménagement à Morges en mars</li>
	 *     <li>départ HC (Neuchâtel) en avril</li>
	 *     <li>déménagement de Neuch' à Péseux (NE) en mai</li>
	 *     <li>déménagement de Péseux (NE) à Berne en juin</li>
	 *     <li>retour à Nyon en juillet</li>
	 *     <li>déménagement à Vevey en octobre</li>
	 * </ul>
	 * Le nombre de PIIS attendues est de 5 :
	 * <ol>
	 *     <li>une SOURCE sur Morges sur janvier à février</li>
	 *     <li>une MIXTE sur Morges sur mars à avril</li>
	 *     <li>une SOURCE sur Péseux sur mai/juin</li>
	 *     <li>une SOURCE sur Berne en juillet</li>
	 *     <li>une MIXTE sur Vevey d'août à décembre</li>
	 * </ol>
	 * <br/>
	 * <strong>[SIFISC-18817] Changement d'algorithme</strong>
	 * <br/>
	 * Le nombre de PIIS attendues est maintenant réduit à 2 :
	 * <ol>
	 *     <li>une SOURCE sur Morges sur janvier et février</li>
	 *     <li>une MIXTE sur Vevey de mars à décembre</li>
	 * </ol>
	 */
	@Test
	public void testMariagePuisPlusieursDemenagementDansAnneeAvecPassageHC() throws Exception {

		final int year = RegDate.get().year() - 1;

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Gigi", "Jampère", date(1972, 6, 23), Sexe.FEMININ);
				addForPrincipal(pp, date(year, 1, 1), MotifFor.INDETERMINE, date(year, 2, 15), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Lausanne, ModeImposition.SOURCE);

				final EnsembleTiersCouple couple = addEnsembleTiersCouple(pp, null, date(year, 2, 16), null);
				final MenageCommun mc = couple.getMenage();
				addForPrincipal(mc, date(year, 2, 16), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, date(year, 3, 10), MotifFor.DEMENAGEMENT_VD, MockCommune.Lausanne);
				addForPrincipal(mc, date(year, 3, 11), MotifFor.DEMENAGEMENT_VD, date(year, 4, 22), MotifFor.DEPART_HC, MockCommune.Morges);
				addForPrincipal(mc, date(year, 4, 23), MotifFor.DEPART_HC, date(year, 5, 4), MotifFor.DEPART_HC, MockCommune.Neuchatel);
				addForPrincipal(mc, date(year, 5, 5), MotifFor.DEPART_HC, date(year, 6, 18), MotifFor.DEMENAGEMENT_VD, MockCommune.Peseux);
				addForPrincipal(mc, date(year, 6, 19), MotifFor.DEMENAGEMENT_VD, date(year, 7, 25), MotifFor.ARRIVEE_HC, MockCommune.Bern);
				addForPrincipal(mc, date(year, 7, 26), MotifFor.ARRIVEE_HC, date(year, 10, 7), MotifFor.DEMENAGEMENT_VD, MockCommune.Nyon);
				addForPrincipal(mc, date(year, 10, 8), MotifFor.DEMENAGEMENT_VD, MockCommune.Vevey);
				return pp.getNumero();
			}
		});

		// calculs des périodes
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
					Assert.assertEquals(Localisation.getVaud(), pi.getLocalisation());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, pi.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockCommune.Morges.getNoOFS(), pi.getNoOfs());
					Assert.assertEquals(date(year, 1, 1), pi.getDateDebut());
					Assert.assertEquals(date(year, 3, 1).getOneDayBefore(), pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) ppId, pi.getContribuable().getNumero());
				}
				{
					final PeriodeImpositionImpotSource pi = piis.get(1);
					Assert.assertNotNull(pi);
					Assert.assertEquals(PeriodeImpositionImpotSource.Type.MIXTE, pi.getType());
					Assert.assertEquals(Localisation.getVaud(), pi.getLocalisation());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, pi.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockCommune.Vevey.getNoOFS(), pi.getNoOfs());
					Assert.assertEquals(date(year, 3, 1), pi.getDateDebut());
					Assert.assertEquals(date(year, 12, 31), pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) ppId, pi.getContribuable().getNumero());
				}
			}
		});
	}

	/**
	 * Exemple donné par R. Carbo dans un mail du 18.11 :
	 * <ul>
	 *     <li>sourcier lausannois qui obtient son permis C le 15.02</li>
	 *     <li>déménagement à Morges en mars</li>
	 *     <li>départ HS (France) en avril</li>
	 *     <li>retour à Nyon en juillet</li>
	 *     <li>déménagement à Vevey en octobre</li>
	 * </ul>
	 * Le nombre de PIIS attendues est de 4 :
	 * <ol>
	 *     <li>une SOURCE sur Morges sur janvier/février</li>
	 *     <li>une MIXTE sur Morges sur mars/avril</li>
	 *     <li>une SOURCE sur France sur fin avril/mai/juin/juillet</li>
	 *     <li>une MIXTE sur Vevey de fin juillet à décembre</li>
	 * </ol>
	 */
	@Test
	public void testNationalitePuisPlusieursDemenagementDansAnneeAvecPassageHS() throws Exception {

		final int year = RegDate.get().year() - 1;

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Gigi", "Jampère", date(1972, 6, 23), Sexe.FEMININ);
				addForPrincipal(pp, date(year, 1, 1), MotifFor.INDETERMINE, date(year, 2, 15), MotifFor.PERMIS_C_SUISSE, MockCommune.Lausanne, ModeImposition.SOURCE);
				addForPrincipal(pp, date(year, 2, 16), MotifFor.PERMIS_C_SUISSE, date(year, 3, 10), MotifFor.DEMENAGEMENT_VD, MockCommune.Lausanne);
				addForPrincipal(pp, date(year, 3, 11), MotifFor.DEMENAGEMENT_VD, date(year, 4, 22), MotifFor.DEPART_HS, MockCommune.Morges);
				addForPrincipal(pp, date(year, 4, 23), MotifFor.DEPART_HS, date(year, 7, 25), MotifFor.ARRIVEE_HS, MockPays.France);
				addForPrincipal(pp, date(year, 7, 26), MotifFor.ARRIVEE_HS, date(year, 10, 7), MotifFor.DEMENAGEMENT_VD, MockCommune.Nyon);
				addForPrincipal(pp, date(year, 10, 8), MotifFor.DEMENAGEMENT_VD, MockCommune.Vevey);
				return pp.getNumero();
			}
		});

		// calculs des périodes
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				final List<PeriodeImpositionImpotSource> piis = service.determine(pp);
				Assert.assertNotNull(piis);
				Assert.assertEquals(4, piis.size());
				{
					final PeriodeImpositionImpotSource pi = piis.get(0);
					Assert.assertNotNull(pi);
					Assert.assertEquals(PeriodeImpositionImpotSource.Type.SOURCE, pi.getType());
					Assert.assertEquals(Localisation.getVaud(), pi.getLocalisation());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, pi.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockCommune.Morges.getNoOFS(), pi.getNoOfs());
					Assert.assertEquals(date(year, 1, 1), pi.getDateDebut());
					Assert.assertEquals(date(year, 2, 1).getLastDayOfTheMonth(), pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) ppId, pi.getContribuable().getNumero());
				}
				{
					final PeriodeImpositionImpotSource pi = piis.get(1);
					Assert.assertNotNull(pi);
					Assert.assertEquals(PeriodeImpositionImpotSource.Type.MIXTE, pi.getType());
					Assert.assertEquals(Localisation.getVaud(), pi.getLocalisation());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, pi.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockCommune.Morges.getNoOFS(), pi.getNoOfs());
					Assert.assertEquals(date(year, 3, 1), pi.getDateDebut());
					Assert.assertEquals(date(year, 4, 22), pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) ppId, pi.getContribuable().getNumero());
				}
				{
					final PeriodeImpositionImpotSource pi = piis.get(2);
					Assert.assertNotNull(pi);
					Assert.assertEquals(PeriodeImpositionImpotSource.Type.SOURCE, pi.getType());
					Assert.assertEquals(Localisation.getHorsSuisse(MockPays.France.getNoOFS()), pi.getLocalisation());
					Assert.assertEquals(TypeAutoriteFiscale.PAYS_HS, pi.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockPays.France.getNoOFS(), pi.getNoOfs());
					Assert.assertEquals(date(year, 4, 23), pi.getDateDebut());
					Assert.assertEquals(date(year, 7, 25), pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) ppId, pi.getContribuable().getNumero());
				}
				{
					final PeriodeImpositionImpotSource pi = piis.get(3);
					Assert.assertNotNull(pi);
					Assert.assertEquals(PeriodeImpositionImpotSource.Type.MIXTE, pi.getType());
					Assert.assertEquals(Localisation.getVaud(), pi.getLocalisation());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, pi.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockCommune.Vevey.getNoOFS(), pi.getNoOfs());
					Assert.assertEquals(date(year, 7, 26), pi.getDateDebut());
					Assert.assertEquals(date(year, 12, 31), pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) ppId, pi.getContribuable().getNumero());
				}
			}
		});
	}

	/**
	 * Discussion avec R. Carbo le 21.11.2013 -> dans la spécification Swissdec, le terme "changement de canton" est
	 * interprété comme la date du départ, donc une arrivée HC au premier jour du mois correspond à un départ le mois
	 * précédent et doit donc générer une période VD tout de suite
	 */
	@Test
	public void testArriveeHCPremierJourDuMois() throws Exception {

		final int year = RegDate.get().year() - 1;
		final RegDate arrivee = date(year, 6, 1);

		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Philibert", "Taplace", date(1980, 4, 2), Sexe.MASCULIN);
				addForPrincipal(pp, arrivee, MotifFor.ARRIVEE_HC, date(year, 12, 31), MotifFor.DEPART_HS, MockCommune.Bussigny, ModeImposition.SOURCE);
				return pp.getNumero();
			}
		});

		// calcul des piis
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
					Assert.assertEquals(Localisation.getInconnue(), pi.getLocalisation());
					Assert.assertNull(pi.getTypeAutoriteFiscale());
					Assert.assertNull(pi.getNoOfs());
					Assert.assertEquals(date(year, 1, 1), pi.getDateDebut());
					Assert.assertEquals(arrivee.getOneDayBefore(), pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) ppId, pi.getContribuable().getNumero());
				}
				{
					final PeriodeImpositionImpotSource pi = piis.get(1);
					Assert.assertNotNull(pi);
					Assert.assertEquals(PeriodeImpositionImpotSource.Type.SOURCE, pi.getType());
					Assert.assertEquals(Localisation.getVaud(), pi.getLocalisation());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, pi.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockCommune.Bussigny.getNoOFS(), pi.getNoOfs());
					Assert.assertEquals(arrivee, pi.getDateDebut());
					Assert.assertEquals(date(year, 12, 31), pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) ppId, pi.getContribuable().getNumero());
				}
			}
		});
	}

	/**
	 * Discussion avec R. Carbo le 21.11.2013 -> dans la spécification Swissdec, le terme "changement de canton" est
	 * interprété comme la date du départ, donc un départ HC au dernier jour du mois doit donc générer une période HC dès le lendemain
	 */
	@Test
	public void testDepartHCDernierJourDuMois() throws Exception {

		final int year = RegDate.get().year() - 1;
		final RegDate depart = date(year, 5, 31);

		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Philibert", "Taplace", date(1980, 4, 2), Sexe.MASCULIN);
				addForPrincipal(pp, date(year, 1, 1), MotifFor.ARRIVEE_HS, depart, MotifFor.DEPART_HC, MockCommune.Bussigny, ModeImposition.SOURCE);
				addForPrincipal(pp, depart.getOneDayAfter(), MotifFor.DEPART_HC, date(year, 12, 31), MotifFor.DEPART_HS, MockCommune.Bern, ModeImposition.SOURCE);
				return pp.getNumero();
			}
		});

		// calcul des piis
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
					Assert.assertEquals(Localisation.getVaud(), pi.getLocalisation());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, pi.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockCommune.Bussigny.getNoOFS(), pi.getNoOfs());
					Assert.assertEquals(date(year, 1, 1), pi.getDateDebut());
					Assert.assertEquals(depart, pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) ppId, pi.getContribuable().getNumero());
				}
				{
					final PeriodeImpositionImpotSource pi = piis.get(1);
					Assert.assertNotNull(pi);
					Assert.assertEquals(PeriodeImpositionImpotSource.Type.SOURCE, pi.getType());
					Assert.assertEquals(Localisation.getHorsCanton("BE"), pi.getLocalisation());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_HC, pi.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockCommune.Bern.getNoOFS(), pi.getNoOfs());
					Assert.assertEquals(depart.getOneDayAfter(), pi.getDateDebut());
					Assert.assertEquals(date(year, 12, 31), pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) ppId, pi.getContribuable().getNumero());
				}
			}
		});
	}

	/**
	 * Discussion avec R. Carbo le 21.11.2013 -> dans la spécification Swissdec, le terme "changement de canton" est
	 * interprété comme la date du départ, donc un départ HC au premier jour du mois doit donc générer une période HC dès le mois suivant
	 */
	@Test
	public void testDepartHCPremierJourDuMois() throws Exception {

		final int year = RegDate.get().year() - 1;
		final RegDate depart = date(year, 6, 1);

		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Philibert", "Taplace", date(1980, 4, 2), Sexe.MASCULIN);
				addForPrincipal(pp, date(year, 1, 1), MotifFor.ARRIVEE_HS, depart, MotifFor.DEPART_HC, MockCommune.Bussigny, ModeImposition.SOURCE);
				addForPrincipal(pp, depart.getOneDayAfter(), MotifFor.DEPART_HC, date(year, 12, 31), MotifFor.DEPART_HS, MockCommune.Bern, ModeImposition.SOURCE);
				return pp.getNumero();
			}
		});

		// calcul des piis
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
					Assert.assertEquals(Localisation.getVaud(), pi.getLocalisation());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, pi.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockCommune.Bussigny.getNoOFS(), pi.getNoOfs());
					Assert.assertEquals(date(year, 1, 1), pi.getDateDebut());
					Assert.assertEquals(depart.getLastDayOfTheMonth(), pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) ppId, pi.getContribuable().getNumero());
				}
				{
					final PeriodeImpositionImpotSource pi = piis.get(1);
					Assert.assertNotNull(pi);
					Assert.assertEquals(PeriodeImpositionImpotSource.Type.SOURCE, pi.getType());
					Assert.assertEquals(Localisation.getHorsCanton("BE"), pi.getLocalisation());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_HC, pi.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockCommune.Bern.getNoOFS(), pi.getNoOfs());
					Assert.assertEquals(depart.getLastDayOfTheMonth().getOneDayAfter(), pi.getDateDebut());
					Assert.assertEquals(date(year, 12, 31), pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) ppId, pi.getContribuable().getNumero());
				}
			}
		});
	}

	/**
	 * Discussion avec R. Carbo le 21.11.2013 -> dans la spécification Swissdec, le terme "changement de canton" est
	 * interprété comme la date du départ, donc un départ HC au dernier jour du mois doit donc générer une période HC dès le lendemain
	 */
	@Test
	public void testChangementDeCantonSurUneFrontiereDeMois() throws Exception {

		final int year = RegDate.get().year() - 1;
		final RegDate depart = date(year, 5, 31);

		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Philibert", "Taplace", date(1980, 4, 2), Sexe.MASCULIN);
				addForPrincipal(pp, date(year, 1, 1), MotifFor.ARRIVEE_HS, depart, MotifFor.DEPART_HC, MockCommune.Neuchatel, ModeImposition.SOURCE);
				addForPrincipal(pp, depart.getOneDayAfter(), MotifFor.DEPART_HC, date(year, 12, 31), MotifFor.DEPART_HS, MockCommune.Bern, ModeImposition.SOURCE);

				final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.MENSUEL, date(2009, 1, 1));
				addRapportPrestationImposable(dpi, pp, date(year, 2, 1), date(year, 11, 23), false);
				return pp.getNumero();
			}
		});

		// calcul des piis
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
					Assert.assertEquals(Localisation.getHorsCanton("NE"), pi.getLocalisation());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_HC, pi.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockCommune.Neuchatel.getNoOFS(), pi.getNoOfs());
					Assert.assertEquals(date(year, 1, 1), pi.getDateDebut());
					Assert.assertEquals(depart, pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) ppId, pi.getContribuable().getNumero());
				}
				{
					final PeriodeImpositionImpotSource pi = piis.get(1);
					Assert.assertNotNull(pi);
					Assert.assertEquals(PeriodeImpositionImpotSource.Type.SOURCE, pi.getType());
					Assert.assertEquals(Localisation.getHorsCanton("BE"), pi.getLocalisation());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_HC, pi.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockCommune.Bern.getNoOFS(), pi.getNoOfs());
					Assert.assertEquals(depart.getOneDayAfter(), pi.getDateDebut());
					Assert.assertEquals(date(year, 12, 31), pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) ppId, pi.getContribuable().getNumero());
				}
			}
		});
	}

	@Test
	public void testObtentionPermisCPremierJourDuMoisAvant2014() throws Exception {

		final int year = 2013;
		final RegDate obtention = date(year, 4, 1);

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Alberto", "Luccini", date(1984, 3, 12), Sexe.MASCULIN);
				addForPrincipal(pp, date(year, 1, 1), MotifFor.INDETERMINE, obtention.getOneDayBefore(), MotifFor.PERMIS_C_SUISSE, MockCommune.Bussigny, ModeImposition.SOURCE);
				addForPrincipal(pp, obtention, MotifFor.PERMIS_C_SUISSE, MockCommune.Bussigny);
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
					Assert.assertEquals(Localisation.getVaud(), pi.getLocalisation());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, pi.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockCommune.Bussigny.getNoOFS(), pi.getNoOfs());
					Assert.assertEquals(date(year, 1, 1), pi.getDateDebut());
					Assert.assertEquals(obtention.getOneDayBefore(), pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) ppId, pi.getContribuable().getNumero());
				}
				{
					final PeriodeImpositionImpotSource pi = piis.get(1);
					Assert.assertNotNull(pi);
					Assert.assertEquals(PeriodeImpositionImpotSource.Type.MIXTE, pi.getType());
					Assert.assertEquals(Localisation.getVaud(), pi.getLocalisation());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, pi.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockCommune.Bussigny.getNoOFS(), pi.getNoOfs());
					Assert.assertEquals(obtention, pi.getDateDebut());
					Assert.assertEquals(date(year, 12, 31), pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) ppId, pi.getContribuable().getNumero());
				}
			}
		});
	}

	@Test
	public void testObtentionPermisCPremierJourDuMoisDepuis2014() throws Exception {

		final int year = 2014;
		final RegDate obtention = date(year, 4, 1);

		ForFiscalValidator.setFutureBeginDate(RegDateHelper.maximum(RegDate.get(), date(year + 1, 1, 1), NullDateBehavior.LATEST));
		try {
			// mise en place fiscale
			final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
				@Override
				public Long doInTransaction(TransactionStatus status) {
					final PersonnePhysique pp = addNonHabitant("Alberto", "Luccini", date(1984, 3, 12), Sexe.MASCULIN);
					addForPrincipal(pp, date(year, 1, 1), MotifFor.INDETERMINE, obtention.getOneDayBefore(), MotifFor.PERMIS_C_SUISSE, MockCommune.Bussigny, ModeImposition.SOURCE);
					addForPrincipal(pp, obtention, MotifFor.PERMIS_C_SUISSE, date(year, 12, 31), MotifFor.DEPART_HS, MockCommune.Bussigny);
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
						Assert.assertEquals(Localisation.getVaud(), pi.getLocalisation());
						Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, pi.getTypeAutoriteFiscale());
						Assert.assertEquals((Integer) MockCommune.Bussigny.getNoOFS(), pi.getNoOfs());
						Assert.assertEquals(date(year, 1, 1), pi.getDateDebut());
						Assert.assertEquals(obtention.getLastDayOfTheMonth(), pi.getDateFin());
						Assert.assertNotNull(pi.getContribuable());
						Assert.assertEquals((Long) ppId, pi.getContribuable().getNumero());
					}
					{
						final PeriodeImpositionImpotSource pi = piis.get(1);
						Assert.assertNotNull(pi);
						Assert.assertEquals(PeriodeImpositionImpotSource.Type.MIXTE, pi.getType());
						Assert.assertEquals(Localisation.getVaud(), pi.getLocalisation());
						Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, pi.getTypeAutoriteFiscale());
						Assert.assertEquals((Integer) MockCommune.Bussigny.getNoOFS(), pi.getNoOfs());
						Assert.assertEquals(obtention.getLastDayOfTheMonth().getOneDayAfter(), pi.getDateDebut());
						Assert.assertEquals(date(year, 12, 31), pi.getDateFin());
						Assert.assertNotNull(pi.getContribuable());
						Assert.assertEquals((Long) ppId, pi.getContribuable().getNumero());
					}
				}
			});
		}
		finally {
			ForFiscalValidator.setFutureBeginDate(null);
		}
	}

	/**
	 * [SIFISC-11191] mixte depuis 2010 qui part HC en 2013 -> vu comme "SOURCE" en 2012 ???
	 */
	@Test
	public void testDepartHCdeMixtePresentDepuisPlusieursAnnee() throws Exception {

		final RegDate arrivee = date(2010, 1, 1);
		final RegDate departHC = date(2013, 6, 12);
		final long noIndividu = 42748232L;

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				addIndividu(noIndividu, null, "Chevalier", "Ernest", Sexe.MASCULIN);
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addHabitant(noIndividu);
				addForPrincipal(pp, arrivee, MotifFor.ARRIVEE_HS, departHC, MotifFor.DEPART_HC, MockCommune.Aigle, ModeImposition.MIXTE_137_1);
				addForPrincipal(pp, departHC.getOneDayAfter(), MotifFor.DEPART_HC, MockCommune.Neuchatel, ModeImposition.SOURCE);
				return pp.getNumero();
			}
		});

		// calcul des piis
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				final List<PeriodeImpositionImpotSource> piis = service.determine(pp);
				Assert.assertNotNull(piis);
				Assert.assertEquals(5, piis.size());

				{
					final PeriodeImpositionImpotSource pi = piis.get(0);
					Assert.assertNotNull(pi);
					Assert.assertEquals(PeriodeImpositionImpotSource.Type.MIXTE, pi.getType());
					Assert.assertEquals(Localisation.getVaud(), pi.getLocalisation());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, pi.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockCommune.Aigle.getNoOFS(), pi.getNoOfs());
					Assert.assertEquals(arrivee, pi.getDateDebut());
					Assert.assertEquals(date(2010, 12, 31), pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) ppId, pi.getContribuable().getNumero());
				}
				{
					final PeriodeImpositionImpotSource pi = piis.get(1);
					Assert.assertNotNull(pi);
					Assert.assertEquals(PeriodeImpositionImpotSource.Type.MIXTE, pi.getType());
					Assert.assertEquals(Localisation.getVaud(), pi.getLocalisation());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, pi.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockCommune.Aigle.getNoOFS(), pi.getNoOfs());
					Assert.assertEquals(date(2011, 1, 1), pi.getDateDebut());
					Assert.assertEquals(date(2011, 12, 31), pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) ppId, pi.getContribuable().getNumero());
				}
				{
					final PeriodeImpositionImpotSource pi = piis.get(2);
					Assert.assertNotNull(pi);
					Assert.assertEquals(PeriodeImpositionImpotSource.Type.MIXTE, pi.getType());
					Assert.assertEquals(Localisation.getVaud(), pi.getLocalisation());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, pi.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockCommune.Aigle.getNoOFS(), pi.getNoOfs());
					Assert.assertEquals(date(2012, 1, 1), pi.getDateDebut());
					Assert.assertEquals(date(2012, 12, 31), pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) ppId, pi.getContribuable().getNumero());
				}
				{
					final PeriodeImpositionImpotSource pi = piis.get(3);
					Assert.assertNotNull(pi);
					Assert.assertEquals(PeriodeImpositionImpotSource.Type.SOURCE, pi.getType());
					Assert.assertEquals(Localisation.getVaud(), pi.getLocalisation());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, pi.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockCommune.Aigle.getNoOFS(), pi.getNoOfs());
					Assert.assertEquals(date(2013, 1, 1), pi.getDateDebut());
					Assert.assertEquals(departHC.getLastDayOfTheMonth(), pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) ppId, pi.getContribuable().getNumero());
				}
				{
					final PeriodeImpositionImpotSource pi = piis.get(4);
					Assert.assertNotNull(pi);
					Assert.assertEquals(PeriodeImpositionImpotSource.Type.SOURCE, pi.getType());
					Assert.assertEquals(Localisation.getHorsCanton("NE"), pi.getLocalisation());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_HC, pi.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockCommune.Neuchatel.getNoOFS(), pi.getNoOfs());
					Assert.assertEquals(departHC.getLastDayOfTheMonth().getOneDayAfter(), pi.getDateDebut());
					Assert.assertEquals(date(2013, 12, 31)   , pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) ppId, pi.getContribuable().getNumero());
				}

			}
		});
	}

	/**
	 * [SIFISC-11191] ordinaire depuis 2010 qui part HC en 2013 (avec RT) -> vu comme "SOURCE" en 2012 ???
	 */
	@Test
	public void testDepartHCdeOrdinaireAvecRTPresentDepuisPlusieursAnnee() throws Exception {

		final RegDate arrivee = date(2010, 1, 1);
		final RegDate departHC = date(2013, 6, 12);
		final long noIndividu = 42748232L;

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				addIndividu(noIndividu, null, "Chevalier", "Ernest", Sexe.MASCULIN);
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addHabitant(noIndividu);
				addForPrincipal(pp, arrivee, MotifFor.ARRIVEE_HS, departHC, MotifFor.DEPART_HC, MockCommune.Aigle, ModeImposition.ORDINAIRE);
				addForPrincipal(pp, departHC.getOneDayAfter(), MotifFor.DEPART_HC, MockCommune.Neuchatel, ModeImposition.ORDINAIRE);

				final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.MENSUEL, date(2009, 1, 1));
				addRapportPrestationImposable(dpi, pp, arrivee, departHC, false);
				return pp.getNumero();
			}
		});

		// calcul des piis
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				final List<PeriodeImpositionImpotSource> piis = service.determine(pp);
				Assert.assertNotNull(piis);
				Assert.assertEquals(4, piis.size());

				{
					final PeriodeImpositionImpotSource pi = piis.get(0);
					Assert.assertNotNull(pi);
					Assert.assertEquals(PeriodeImpositionImpotSource.Type.MIXTE, pi.getType());
					Assert.assertEquals(Localisation.getVaud(), pi.getLocalisation());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, pi.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockCommune.Aigle.getNoOFS(), pi.getNoOfs());
					Assert.assertEquals(arrivee, pi.getDateDebut());
					Assert.assertEquals(date(2010, 12, 31), pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) ppId, pi.getContribuable().getNumero());
				}
				{
					final PeriodeImpositionImpotSource pi = piis.get(1);
					Assert.assertNotNull(pi);
					Assert.assertEquals(PeriodeImpositionImpotSource.Type.MIXTE, pi.getType());
					Assert.assertEquals(Localisation.getVaud(), pi.getLocalisation());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, pi.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockCommune.Aigle.getNoOFS(), pi.getNoOfs());
					Assert.assertEquals(date(2011, 1, 1), pi.getDateDebut());
					Assert.assertEquals(date(2011, 12, 31), pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) ppId, pi.getContribuable().getNumero());
				}
				{
					final PeriodeImpositionImpotSource pi = piis.get(2);
					Assert.assertNotNull(pi);
					Assert.assertEquals(PeriodeImpositionImpotSource.Type.MIXTE, pi.getType());
					Assert.assertEquals(Localisation.getVaud(), pi.getLocalisation());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, pi.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockCommune.Aigle.getNoOFS(), pi.getNoOfs());
					Assert.assertEquals(date(2012, 1, 1), pi.getDateDebut());
					Assert.assertEquals(date(2012, 12, 31), pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) ppId, pi.getContribuable().getNumero());
				}
				{
					final PeriodeImpositionImpotSource pi = piis.get(3);
					Assert.assertNotNull(pi);
					Assert.assertEquals(PeriodeImpositionImpotSource.Type.SOURCE, pi.getType());
					Assert.assertEquals(Localisation.getHorsCanton("NE"), pi.getLocalisation());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_HC, pi.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockCommune.Neuchatel.getNoOFS(), pi.getNoOfs());
					Assert.assertEquals(date(2013, 1, 1), pi.getDateDebut());
					Assert.assertEquals(date(2013, 12, 31)   , pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) ppId, pi.getContribuable().getNumero());
				}
			}
		});
	}

	/**
	 * [SIFISC-12331] Cas de la commune HC qui n'est pas prise en compte dans une PIIS
	 */
	@Test
	public void testCommuneHcSurPeriodeImpositionSource12331() throws Exception {

		final int lastYear = RegDate.get().year() - 1;
		final RegDate arrivee = date(lastYear, 1, 1);
		final RegDate obtentionPermisC = date(lastYear, 1, 20);
		final RegDate departHC = date(lastYear, 2, 10);
		final RegDate retourHC = date(lastYear, 3, 5);
		final RegDate mariage = date(lastYear, 3, 21);
		final RegDate arriveeHSmenage = date(lastYear, 4, 6);
		final RegDate departHCmenage = date(lastYear, 4, 29);
		final RegDate retourHCmenage = date(lastYear, 5, 2);
		final long noIndividu = 42748232L;

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noIndividu, null, "Chevalier", "Ernest", Sexe.MASCULIN);
				marieIndividu(ind, mariage);
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addHabitant(noIndividu);
				addForPrincipal(pp, arrivee, MotifFor.ARRIVEE_HS, obtentionPermisC.getOneDayBefore(), MotifFor.PERMIS_C_SUISSE, MockCommune.Lausanne, ModeImposition.SOURCE);
				addForPrincipal(pp, obtentionPermisC, MotifFor.PERMIS_C_SUISSE, departHC, MotifFor.DEPART_HC, MockCommune.Lausanne, ModeImposition.ORDINAIRE);
				addForPrincipal(pp, departHC.getOneDayAfter(), MotifFor.DEPART_HC, retourHC.getOneDayBefore(), MotifFor.ARRIVEE_HC, MockCommune.Bale, ModeImposition.SOURCE);
				addForPrincipal(pp, retourHC, MotifFor.ARRIVEE_HC, mariage.getOneDayBefore(), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Aigle, ModeImposition.SOURCE);

				final EnsembleTiersCouple couple = addEnsembleTiersCouple(pp, null, mariage, null);
				final MenageCommun mc = couple.getMenage();
				addForPrincipal(mc, mariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, arriveeHSmenage.getOneDayBefore(), MotifFor.ARRIVEE_HS, MockPays.France, ModeImposition.SOURCE);
				addForPrincipal(mc, arriveeHSmenage, MotifFor.ARRIVEE_HS, departHCmenage, MotifFor.DEPART_HC, MockCommune.Aubonne, ModeImposition.MIXTE_137_1);
				addForPrincipal(mc, departHCmenage.getOneDayAfter(), MotifFor.DEPART_HC, retourHCmenage.getOneDayBefore(), MotifFor.ARRIVEE_HC, MockCommune.Bern, ModeImposition.SOURCE);
				addForPrincipal(mc, retourHCmenage, MotifFor.ARRIVEE_HC, MockCommune.Bussigny, ModeImposition.ORDINAIRE);

				return pp.getNumero();
			}
		});

		// calcul des PIIS
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				final List<PeriodeImpositionImpotSource> piis = service.determine(pp);
				Assert.assertNotNull(piis);
				Assert.assertEquals(6, piis.size());

				{
					// ordinaire qui part HC -> PIIS source avant le départ, qui fusionne avec la PIIS d'avant l'obtention du permis C

					final PeriodeImpositionImpotSource pi = piis.get(0);
					Assert.assertNotNull(pi);
					Assert.assertEquals(PeriodeImpositionImpotSource.Type.SOURCE, pi.getType());
					Assert.assertEquals(Localisation.getVaud(), pi.getLocalisation());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, pi.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockCommune.Lausanne.getNoOFS(), pi.getNoOfs());
					Assert.assertEquals(arrivee, pi.getDateDebut());
					Assert.assertEquals(departHC.getLastDayOfTheMonth(), pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) ppId, pi.getContribuable().getNumero());
				}
				{
					// le for du couple est HS après le mariage, qui est donc assimilé à un départ HS

					final PeriodeImpositionImpotSource pi = piis.get(1);
					Assert.assertNotNull(pi);
					Assert.assertEquals(PeriodeImpositionImpotSource.Type.SOURCE, pi.getType());
					Assert.assertEquals(Localisation.getHorsCanton("BS"), pi.getLocalisation());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_HC, pi.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockCommune.Bale.getNoOFS(), pi.getNoOfs());
					Assert.assertEquals(departHC.getLastDayOfTheMonth().getOneDayAfter(), pi.getDateDebut());
					Assert.assertEquals(mariage.getOneDayBefore(), pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) ppId, pi.getContribuable().getNumero());
				}
				{
					// le for du couple est HS après le mariage, qui est donc assimilé à un départ HS

					final PeriodeImpositionImpotSource pi = piis.get(2);
					Assert.assertNotNull(pi);
					Assert.assertEquals(PeriodeImpositionImpotSource.Type.SOURCE, pi.getType());
					Assert.assertEquals(Localisation.getHorsSuisse(MockPays.France.getNoOFS()), pi.getLocalisation());
					Assert.assertEquals(TypeAutoriteFiscale.PAYS_HS, pi.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockPays.France.getNoOFS(), pi.getNoOfs());
					Assert.assertEquals(mariage, pi.getDateDebut());
					Assert.assertEquals(arriveeHSmenage.getOneDayBefore(), pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) ppId, pi.getContribuable().getNumero());
				}
				{
					final PeriodeImpositionImpotSource pi = piis.get(3);
					Assert.assertNotNull(pi);
					Assert.assertEquals(PeriodeImpositionImpotSource.Type.MIXTE, pi.getType());
					Assert.assertEquals(Localisation.getVaud(), pi.getLocalisation());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, pi.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockCommune.Aubonne.getNoOFS(), pi.getNoOfs());
					Assert.assertEquals(arriveeHSmenage, pi.getDateDebut());
					Assert.assertEquals(departHCmenage.getLastDayOfTheMonth(), pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) ppId, pi.getContribuable().getNumero());
				}
				{
					// dans le problème du cas jira SIFISC-12331, cette période était "SOURCE, localisation inconnue" avec les mêmes dates
					// le problème était qu'elle était issue d'un "trou" (donc rempli par une PIIS "Source sans localisation") creusé
					// par le mauvais calcul de la date de fin de la période (fin au 1.05 avec fraction sans décalage de date, puis retour d'un jour en arrière)
					// qui rendait la période réduite à néant (date fin < date début)

					final PeriodeImpositionImpotSource pi = piis.get(4);
					Assert.assertNotNull(pi);
					Assert.assertEquals(PeriodeImpositionImpotSource.Type.SOURCE, pi.getType());
					Assert.assertEquals(Localisation.getHorsCanton("BE"), pi.getLocalisation());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_HC, pi.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockCommune.Bern.getNoOFS(), pi.getNoOfs());
					Assert.assertEquals(departHCmenage.getLastDayOfTheMonth().getOneDayAfter(), pi.getDateDebut());
					Assert.assertEquals(retourHCmenage.getLastDayOfTheMonth(), pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) ppId, pi.getContribuable().getNumero());
				}
				{
					final PeriodeImpositionImpotSource pi = piis.get(5);
					Assert.assertNotNull(pi);
					Assert.assertEquals(PeriodeImpositionImpotSource.Type.MIXTE, pi.getType());
					Assert.assertEquals(Localisation.getVaud(), pi.getLocalisation());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, pi.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockCommune.Bussigny.getNoOFS(), pi.getNoOfs());
					Assert.assertEquals(retourHCmenage.getLastDayOfTheMonth().getOneDayAfter(), pi.getDateDebut());
					Assert.assertEquals(date(lastYear, 12, 31), pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) ppId, pi.getContribuable().getNumero());
				}
			}
		});
	}

	/**
	 * [SIFISC-12326] cas d'un ordinaire/mixte1 qui part HC dans la PF et qui revient encore la même année -> la piis d'avant son
	 * départ ne doit pas être "SOURCE" mais "MIXTE"
	 * <br/>
	 * [SIFISC-18817] Il n'y a plus qu'une seule période d'imposition IS maintenant
	 */
	@Test
	public void testOrdinaireDepartHorsCantonPuisRetourMemePeriode() throws Exception {

		final int lastYear = RegDate.get().year() - 1;
		final long noIndividu = 1263812L;
		final RegDate obtentionNationalite = date(lastYear - 1, 6, 1);
		final RegDate depart = date(lastYear, 5, 3);
		final RegDate retour = date(lastYear, 10, 20);

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noIndividu, null, "Lade", "Réga", Sexe.FEMININ);
				addNationalite(ind, MockPays.Suisse, obtentionNationalite, null);
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {

				// création du contribuable
				final PersonnePhysique pp = addHabitant(noIndividu);
				addForPrincipal(pp, obtentionNationalite, MotifFor.PERMIS_C_SUISSE, depart, MotifFor.DEPART_HC, MockCommune.Leysin);
				addForPrincipal(pp, depart.getOneDayAfter(), MotifFor.DEPART_HC, retour.getOneDayBefore(), MotifFor.ARRIVEE_HC, MockCommune.Bale);
				addForPrincipal(pp, retour, MotifFor.ARRIVEE_HC, MockCommune.Lausanne);

				// mise en place d'un RT sur l'année dernière pour générer des PIIS sur cette année seulement
				final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.MENSUEL, date(2009, 1, 1));
				addRapportPrestationImposable(dpi, pp, date(lastYear, 1, 1), date(lastYear, 5, 31), false);

				return pp.getNumero();
			}
		});

		// calcul des PIIS
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
					Assert.assertEquals(Localisation.getVaud(), pi.getLocalisation());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, pi.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockCommune.Lausanne.getNoOFS(), pi.getNoOfs());
					Assert.assertEquals(date(lastYear, 1, 1), pi.getDateDebut());
					Assert.assertEquals(date(lastYear, 12, 31), pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) ppId, pi.getContribuable().getNumero());
				}
			}
		});
	}

	/**
	 * [SIFISC-12326] cas d'un sourcier qui part HC dans la PF et qui revient encore la même année (en ordinaire cette fois)
	 * -> la piis d'avant son départ doit-être "SOURCE"
	 */
	@Test
	public void testSourcierDepartHorsCantonPuisRetourMemePeriode() throws Exception {

		final int lastYear = RegDate.get().year() - 1;
		final long noIndividu = 1263812L;
		final RegDate depart = date(lastYear, 5, 3);
		final RegDate retour = date(lastYear, 10, 20);

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				addIndividu(noIndividu, null, "Lade", "Réga", Sexe.FEMININ);
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {

				// création du contribuable
				final PersonnePhysique pp = addHabitant(noIndividu);
				addForPrincipal(pp, date(lastYear, 1, 1), MotifFor.ARRIVEE_HS, depart, MotifFor.DEPART_HC, MockCommune.Leysin, ModeImposition.SOURCE);
				addForPrincipal(pp, depart.getOneDayAfter(), MotifFor.DEPART_HC, retour.getOneDayBefore(), MotifFor.ARRIVEE_HC, MockCommune.Bale, ModeImposition.SOURCE);
				addForPrincipal(pp, retour, MotifFor.ARRIVEE_HC, MockCommune.Lausanne, ModeImposition.ORDINAIRE);

				// mise en place d'un RT sur l'année dernière pour générer des PIIS sur cette année seulement
				final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.MENSUEL, date(2009, 1, 1));
				addRapportPrestationImposable(dpi, pp, date(lastYear, 1, 1), date(lastYear, 5, 31), false);

				return pp.getNumero();
			}
		});

		// calcul des PIIS
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
					Assert.assertEquals(Localisation.getVaud(), pi.getLocalisation());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, pi.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockCommune.Leysin.getNoOFS(), pi.getNoOfs());
					Assert.assertEquals(date(lastYear, 1, 1), pi.getDateDebut());
					Assert.assertEquals(depart.getLastDayOfTheMonth(), pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) ppId, pi.getContribuable().getNumero());
				}
				{
					final PeriodeImpositionImpotSource pi = piis.get(1);
					Assert.assertNotNull(pi);
					Assert.assertEquals(PeriodeImpositionImpotSource.Type.SOURCE, pi.getType());
					Assert.assertEquals(Localisation.getHorsCanton("BS"), pi.getLocalisation());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_HC, pi.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockCommune.Bale.getNoOFS(), pi.getNoOfs());
					Assert.assertEquals(depart.getLastDayOfTheMonth().getOneDayAfter(), pi.getDateDebut());
					Assert.assertEquals(retour.getLastDayOfTheMonth(), pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) ppId, pi.getContribuable().getNumero());
				}
				{
					final PeriodeImpositionImpotSource pi = piis.get(2);
					Assert.assertNotNull(pi);
					Assert.assertEquals(PeriodeImpositionImpotSource.Type.MIXTE, pi.getType());
					Assert.assertEquals(Localisation.getVaud(), pi.getLocalisation());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, pi.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockCommune.Lausanne.getNoOFS(), pi.getNoOfs());
					Assert.assertEquals(retour.getLastDayOfTheMonth().getOneDayAfter(), pi.getDateDebut());
					Assert.assertEquals(date(lastYear, 12, 31), pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) ppId, pi.getContribuable().getNumero());
				}
			}
		});
	}

	/**
	 * [SIFISC-12326] cas d'un ordinaire/mixte1 qui part HC dans la PF et qui revient encore la même année en étant passé par HS
	 * pendant son séjour HC -> la PF d'avant son départ vaudois doit rester SOURCE
	 * <br/>
	 * [SIFISC-18817] Plus de fractionnement au passage VD &lt;-&gt; HC d'un ordinaire
	 */
	@Test
	public void testOrdinaireDepartHorsCantonPuisRetourApresPassageHorsSuisseMemePeriode() throws Exception {

		final int lastYear = RegDate.get().year() - 1;
		final long noIndividu = 1263812L;
		final RegDate obtentionNationalite = date(lastYear - 1, 6, 1);
		final RegDate depart = date(lastYear, 5, 3);
		final RegDate departHS = date(lastYear, 6, 8);
		final RegDate retourHS = date(lastYear, 8, 15);
		final RegDate retour = date(lastYear, 10, 20);

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noIndividu, null, "Lade", "Réga", Sexe.FEMININ);
				addNationalite(ind, MockPays.Suisse, obtentionNationalite, null);
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {

				// création du contribuable
				final PersonnePhysique pp = addHabitant(noIndividu);
				addForPrincipal(pp, obtentionNationalite, MotifFor.PERMIS_C_SUISSE, depart, MotifFor.DEPART_HC, MockCommune.Leysin);
				addForPrincipal(pp, depart.getOneDayAfter(), MotifFor.DEPART_HC, departHS, MotifFor.DEPART_HS, MockCommune.Bale);
				addForPrincipal(pp, departHS.getOneDayAfter(), MotifFor.DEPART_HS, retourHS.getOneDayBefore(), MotifFor.ARRIVEE_HS, MockPays.Allemagne);
				addForPrincipal(pp, retourHS, MotifFor.ARRIVEE_HS, retour.getOneDayBefore(), MotifFor.ARRIVEE_HC, MockCommune.Bale);
				addForPrincipal(pp, retour, MotifFor.ARRIVEE_HC, MockCommune.Lausanne);

				// mise en place d'un RT sur l'année dernière pour générer des PIIS sur cette année seulement
				final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.MENSUEL, date(2009, 1, 1));
				addRapportPrestationImposable(dpi, pp, date(lastYear, 1, 1), date(lastYear, 5, 31), false);

				return pp.getNumero();
			}
		});

		// calcul des PIIS
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
					Assert.assertEquals(Localisation.getHorsCanton(MockCanton.BaleVille.getSigleOFS()), pi.getLocalisation());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_HC, pi.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockCommune.Bale.getNoOFS(), pi.getNoOfs());
					Assert.assertEquals(RegDate.get(lastYear, 1, 1), pi.getDateDebut());
					Assert.assertEquals(departHS, pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) ppId, pi.getContribuable().getNumero());
				}
				{
					final PeriodeImpositionImpotSource pi = piis.get(1);
					Assert.assertNotNull(pi);
					Assert.assertEquals(PeriodeImpositionImpotSource.Type.SOURCE, pi.getType());
					Assert.assertEquals(Localisation.getHorsSuisse(MockPays.Allemagne.getNoOFS()), pi.getLocalisation());
					Assert.assertEquals(TypeAutoriteFiscale.PAYS_HS, pi.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockPays.Allemagne.getNoOFS(), pi.getNoOfs());
					Assert.assertEquals(departHS.getOneDayAfter(), pi.getDateDebut());
					Assert.assertEquals(retourHS.getOneDayBefore(), pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) ppId, pi.getContribuable().getNumero());
				}
				{
					final PeriodeImpositionImpotSource pi = piis.get(2);
					Assert.assertNotNull(pi);
					Assert.assertEquals(PeriodeImpositionImpotSource.Type.MIXTE, pi.getType());
					Assert.assertEquals(Localisation.getVaud(), pi.getLocalisation());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, pi.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockCommune.Lausanne.getNoOFS(), pi.getNoOfs());
					Assert.assertEquals(retourHS, pi.getDateDebut());
					Assert.assertEquals(date(lastYear, 12, 31), pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) ppId, pi.getContribuable().getNumero());
				}
			}
		});
	}

	/**
	 * [SIFISC-13325][SIFIS-12326] Cas du contribuable arrivé depuis HS en cours d'année et qui repart HC ensuite (toujours la même année)
	 */
	@Test
	public void testArriveeHorsSuissePuisDepartHorsCantonDansMemeAnnee() throws Exception {

		final long noIndividu = 2678156L;
		final int lastYear = RegDate.get().year() - 1;
		final RegDate arrivee = date(lastYear, 1, 15);
		final RegDate depart = date(lastYear, 7, 12);

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				addIndividu(noIndividu, null, "Frigoletta", "Alessio", Sexe.MASCULIN);
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addHabitant(noIndividu);
				addForPrincipal(pp, arrivee, MotifFor.ARRIVEE_HS, depart, MotifFor.DEPART_HC, MockCommune.Lausanne, ModeImposition.MIXTE_137_1);
				addForPrincipal(pp, depart.getOneDayAfter(), MotifFor.DEPART_HC, MockCommune.Geneve);
				return pp.getNumero();
			}
		});

		// calcul des piis
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
					Assert.assertEquals(Localisation.getInconnue(), pi.getLocalisation());
					Assert.assertNull(pi.getTypeAutoriteFiscale());
					Assert.assertNull(pi.getNoOfs());
					Assert.assertEquals(date(lastYear, 1, 1), pi.getDateDebut());
					Assert.assertEquals(arrivee.getOneDayBefore(), pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) ppId, pi.getContribuable().getNumero());
				}
				{
					final PeriodeImpositionImpotSource pi = piis.get(1);
					Assert.assertNotNull(pi);
					Assert.assertEquals(PeriodeImpositionImpotSource.Type.SOURCE, pi.getType());
					Assert.assertEquals(Localisation.getVaud(), pi.getLocalisation());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, pi.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockCommune.Lausanne.getNoOFS(), pi.getNoOfs());
					Assert.assertEquals(arrivee, pi.getDateDebut());
					Assert.assertEquals(depart.getLastDayOfTheMonth(), pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) ppId, pi.getContribuable().getNumero());
				}
				{
					final PeriodeImpositionImpotSource pi = piis.get(2);
					Assert.assertNotNull(pi);
					Assert.assertEquals(PeriodeImpositionImpotSource.Type.SOURCE, pi.getType());
					Assert.assertEquals(Localisation.getHorsCanton("GE"), pi.getLocalisation());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_HC, pi.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockCommune.Geneve.getNoOFS(), pi.getNoOfs());
					Assert.assertEquals(depart.getLastDayOfTheMonth().getOneDayAfter(), pi.getDateDebut());
					Assert.assertEquals(date(lastYear, 12, 31), pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) ppId, pi.getContribuable().getNumero());
				}
			}
		});
	}

	/**
	 * [SIFISC-13325][SIFIS-12326] Cas du contribuable arrivé depuis HS en cours d'année, qui repart HC ensuite (toujours la même année) pour revenir en fin d'année
	 */
	@Test
	public void testArriveeHorsSuissePuisDepartHorsCantonEtRetourDansMemeAnnee() throws Exception {

		final long noIndividu = 2678156L;
		final int lastYear = RegDate.get().year() - 1;
		final RegDate arrivee = date(lastYear, 1, 15);
		final RegDate depart = date(lastYear, 7, 12);
		final RegDate retour = date(lastYear, 10, 26);

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				addIndividu(noIndividu, null, "Frigoletta", "Alessio", Sexe.MASCULIN);
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addHabitant(noIndividu);
				addForPrincipal(pp, arrivee, MotifFor.ARRIVEE_HS, depart, MotifFor.DEPART_HC, MockCommune.Lausanne, ModeImposition.MIXTE_137_1);
				addForPrincipal(pp, depart.getOneDayAfter(), MotifFor.DEPART_HC, retour.getOneDayBefore(), MotifFor.ARRIVEE_HC, MockCommune.Geneve, ModeImposition.SOURCE);
				addForPrincipal(pp, retour, MotifFor.ARRIVEE_HC, MockCommune.Grandson);
				return pp.getNumero();
			}
		});

		// calcul des piis
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				final List<PeriodeImpositionImpotSource> piis = service.determine(pp);
				Assert.assertNotNull(piis);
				Assert.assertEquals(4, piis.size());

				{
					final PeriodeImpositionImpotSource pi = piis.get(0);
					Assert.assertNotNull(pi);
					Assert.assertEquals(PeriodeImpositionImpotSource.Type.SOURCE, pi.getType());
					Assert.assertEquals(Localisation.getInconnue(), pi.getLocalisation());
					Assert.assertNull(pi.getTypeAutoriteFiscale());
					Assert.assertNull(pi.getNoOfs());
					Assert.assertEquals(date(lastYear, 1, 1), pi.getDateDebut());
					Assert.assertEquals(arrivee.getOneDayBefore(), pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) ppId, pi.getContribuable().getNumero());
				}
				{
					final PeriodeImpositionImpotSource pi = piis.get(1);
					Assert.assertNotNull(pi);
					Assert.assertEquals(PeriodeImpositionImpotSource.Type.MIXTE, pi.getType());
					Assert.assertEquals(Localisation.getVaud(), pi.getLocalisation());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, pi.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockCommune.Lausanne.getNoOFS(), pi.getNoOfs());
					Assert.assertEquals(arrivee, pi.getDateDebut());
					Assert.assertEquals(depart.getLastDayOfTheMonth(), pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) ppId, pi.getContribuable().getNumero());
				}
				{
					final PeriodeImpositionImpotSource pi = piis.get(2);
					Assert.assertNotNull(pi);
					Assert.assertEquals(PeriodeImpositionImpotSource.Type.SOURCE, pi.getType());
					Assert.assertEquals(Localisation.getHorsCanton("GE"), pi.getLocalisation());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_HC, pi.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockCommune.Geneve.getNoOFS(), pi.getNoOfs());
					Assert.assertEquals(depart.getLastDayOfTheMonth().getOneDayAfter(), pi.getDateDebut());
					Assert.assertEquals(retour.getLastDayOfTheMonth(), pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) ppId, pi.getContribuable().getNumero());
				}
				{
					final PeriodeImpositionImpotSource pi = piis.get(3);
					Assert.assertNotNull(pi);
					Assert.assertEquals(PeriodeImpositionImpotSource.Type.MIXTE, pi.getType());
					Assert.assertEquals(Localisation.getVaud(), pi.getLocalisation());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, pi.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockCommune.Grandson.getNoOFS(), pi.getNoOfs());
					Assert.assertEquals(retour.getLastDayOfTheMonth().getOneDayAfter(), pi.getDateDebut());
					Assert.assertEquals(date(lastYear, 12, 31), pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) ppId, pi.getContribuable().getNumero());
				}
			}
		});
	}

	/**
	 * [SIFISC-12981] Cas du mixte 1 qui part HC et revient source puis passe mixte dans la même année
	 * --> la PIIS d'avant le départ HC devrait être MIXTE
	 */
	@Test
	public void testMixte1PartiHorsCantonRevientSourceEtPasseMixteDansMemePeriode() throws Exception {

		final long noIndividu = 2678156L;
		final int lastYear = RegDate.get().year() - 1;
		final RegDate arrivee = date(lastYear, 1, 15);
		final RegDate depart = date(lastYear, 7, 12);
		final RegDate retour = date(lastYear, 9, 26);
		final RegDate passageMixte = date(lastYear, 11, 23);
		final RegDate departFinal = date(lastYear, 12, 31);

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				addIndividu(noIndividu, null, "Frigoletta", "Alessio", Sexe.MASCULIN);
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addHabitant(noIndividu);
				addForPrincipal(pp, arrivee, MotifFor.ARRIVEE_HS, depart, MotifFor.DEPART_HC, MockCommune.Lausanne, ModeImposition.MIXTE_137_1);
				addForPrincipal(pp, depart.getOneDayAfter(), MotifFor.DEPART_HC, retour.getOneDayBefore(), MotifFor.ARRIVEE_HC, MockCommune.Geneve, ModeImposition.SOURCE);
				addForPrincipal(pp, retour, MotifFor.ARRIVEE_HC, passageMixte.getOneDayBefore(), MotifFor.CHGT_MODE_IMPOSITION, MockCommune.Grandson, ModeImposition.SOURCE);
				addForPrincipal(pp, passageMixte, MotifFor.CHGT_MODE_IMPOSITION, departFinal, MotifFor.DEPART_HS, MockCommune.Grandson, ModeImposition.MIXTE_137_1);
				return pp.getNumero();
			}
		});

		// calcul des piis
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				final List<PeriodeImpositionImpotSource> piis = service.determine(pp);
				Assert.assertNotNull(piis);
				Assert.assertEquals(4, piis.size());

				{
					final PeriodeImpositionImpotSource pi = piis.get(0);
					Assert.assertNotNull(pi);
					Assert.assertEquals(PeriodeImpositionImpotSource.Type.SOURCE, pi.getType());
					Assert.assertEquals(Localisation.getInconnue(), pi.getLocalisation());
					Assert.assertNull(pi.getTypeAutoriteFiscale());
					Assert.assertNull(pi.getNoOfs());
					Assert.assertEquals(date(lastYear, 1, 1), pi.getDateDebut());
					Assert.assertEquals(arrivee.getOneDayBefore(), pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) ppId, pi.getContribuable().getNumero());
				}
				{
					final PeriodeImpositionImpotSource pi = piis.get(1);
					Assert.assertNotNull(pi);
					Assert.assertEquals(PeriodeImpositionImpotSource.Type.MIXTE, pi.getType());
					Assert.assertEquals(Localisation.getVaud(), pi.getLocalisation());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, pi.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockCommune.Lausanne.getNoOFS(), pi.getNoOfs());
					Assert.assertEquals(arrivee, pi.getDateDebut());
					Assert.assertEquals(depart.getLastDayOfTheMonth(), pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) ppId, pi.getContribuable().getNumero());
				}
				{
					final PeriodeImpositionImpotSource pi = piis.get(2);
					Assert.assertNotNull(pi);
					Assert.assertEquals(PeriodeImpositionImpotSource.Type.SOURCE, pi.getType());
					Assert.assertEquals(Localisation.getHorsCanton("GE"), pi.getLocalisation());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_HC, pi.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockCommune.Geneve.getNoOFS(), pi.getNoOfs());
					Assert.assertEquals(depart.getLastDayOfTheMonth().getOneDayAfter(), pi.getDateDebut());
					Assert.assertEquals(retour.getLastDayOfTheMonth(), pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) ppId, pi.getContribuable().getNumero());
				}
				{
					final PeriodeImpositionImpotSource pi = piis.get(3);
					Assert.assertNotNull(pi);
					Assert.assertEquals(PeriodeImpositionImpotSource.Type.MIXTE, pi.getType());
					Assert.assertEquals(Localisation.getVaud(), pi.getLocalisation());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, pi.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockCommune.Grandson.getNoOFS(), pi.getNoOfs());
					Assert.assertEquals(retour.getLastDayOfTheMonth().getOneDayAfter(), pi.getDateDebut());
					Assert.assertEquals(date(lastYear, 12, 31), pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) ppId, pi.getContribuable().getNumero());
				}
			}
		});
	}

	/**
	 * [SIFISC-12981] Cas du mixte 1 qui part HC et revient source puis obtient son permis C dans la même année
	 * --> la PIIS d'avant le départ HC devrait rester SOURCE
	 */
	@Test
	public void testMixte1PartiHorsCantonRevientSourceEtObtientPermisCDansMemePeriode() throws Exception {

		final long noIndividu = 2678156L;
		final int lastYear = RegDate.get().year() - 1;
		final RegDate arrivee = date(lastYear, 1, 15);
		final RegDate depart = date(lastYear, 7, 12);
		final RegDate retour = date(lastYear, 9, 26);
		final RegDate permisC = date(lastYear, 11, 23);
		final RegDate departFinal = date(lastYear, 12, 31);

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				addIndividu(noIndividu, null, "Frigoletta", "Alessio", Sexe.MASCULIN);
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addHabitant(noIndividu);
				addForPrincipal(pp, arrivee, MotifFor.ARRIVEE_HS, depart, MotifFor.DEPART_HC, MockCommune.Lausanne, ModeImposition.MIXTE_137_1);
				addForPrincipal(pp, depart.getOneDayAfter(), MotifFor.DEPART_HC, retour.getOneDayBefore(), MotifFor.ARRIVEE_HC, MockCommune.Geneve, ModeImposition.SOURCE);
				addForPrincipal(pp, retour, MotifFor.ARRIVEE_HC, permisC.getOneDayBefore(), MotifFor.PERMIS_C_SUISSE, MockCommune.Grandson, ModeImposition.SOURCE);
				addForPrincipal(pp, permisC, MotifFor.PERMIS_C_SUISSE, departFinal, MotifFor.DEPART_HS, MockCommune.Grandson);
				return pp.getNumero();
			}
		});

		// calcul des piis
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				final List<PeriodeImpositionImpotSource> piis = service.determine(pp);
				Assert.assertNotNull(piis);
				Assert.assertEquals(5, piis.size());

				{
					final PeriodeImpositionImpotSource pi = piis.get(0);
					Assert.assertNotNull(pi);
					Assert.assertEquals(PeriodeImpositionImpotSource.Type.SOURCE, pi.getType());
					Assert.assertEquals(Localisation.getInconnue(), pi.getLocalisation());
					Assert.assertNull(pi.getTypeAutoriteFiscale());
					Assert.assertNull(pi.getNoOfs());
					Assert.assertEquals(date(lastYear, 1, 1), pi.getDateDebut());
					Assert.assertEquals(arrivee.getOneDayBefore(), pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) ppId, pi.getContribuable().getNumero());
				}
				{
					final PeriodeImpositionImpotSource pi = piis.get(1);
					Assert.assertNotNull(pi);
					Assert.assertEquals(PeriodeImpositionImpotSource.Type.SOURCE, pi.getType());
					Assert.assertEquals(Localisation.getVaud(), pi.getLocalisation());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, pi.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockCommune.Lausanne.getNoOFS(), pi.getNoOfs());
					Assert.assertEquals(arrivee, pi.getDateDebut());
					Assert.assertEquals(depart.getLastDayOfTheMonth(), pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) ppId, pi.getContribuable().getNumero());
				}
				{
					final PeriodeImpositionImpotSource pi = piis.get(2);
					Assert.assertNotNull(pi);
					Assert.assertEquals(PeriodeImpositionImpotSource.Type.SOURCE, pi.getType());
					Assert.assertEquals(Localisation.getHorsCanton("GE"), pi.getLocalisation());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_HC, pi.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockCommune.Geneve.getNoOFS(), pi.getNoOfs());
					Assert.assertEquals(depart.getLastDayOfTheMonth().getOneDayAfter(), pi.getDateDebut());
					Assert.assertEquals(retour.getLastDayOfTheMonth(), pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) ppId, pi.getContribuable().getNumero());
				}
				{
					final PeriodeImpositionImpotSource pi = piis.get(3);
					Assert.assertNotNull(pi);
					Assert.assertEquals(PeriodeImpositionImpotSource.Type.SOURCE, pi.getType());
					Assert.assertEquals(Localisation.getVaud(), pi.getLocalisation());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, pi.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockCommune.Grandson.getNoOFS(), pi.getNoOfs());
					Assert.assertEquals(retour.getLastDayOfTheMonth().getOneDayAfter(), pi.getDateDebut());
					Assert.assertEquals(permisC.getLastDayOfTheMonth(), pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) ppId, pi.getContribuable().getNumero());
				}
				{
					final PeriodeImpositionImpotSource pi = piis.get(4);
					Assert.assertNotNull(pi);
					Assert.assertEquals(PeriodeImpositionImpotSource.Type.MIXTE, pi.getType());
					Assert.assertEquals(Localisation.getVaud(), pi.getLocalisation());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, pi.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockCommune.Grandson.getNoOFS(), pi.getNoOfs());
					Assert.assertEquals(permisC.getLastDayOfTheMonth().getOneDayAfter(), pi.getDateDebut());
					Assert.assertEquals(date(lastYear, 12, 31), pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) ppId, pi.getContribuable().getNumero());
				}
			}
		});
	}

	/**
	 * [SIFISC-12981] Cas du mixte 1 qui part HC et revient source puis se marie avec un mixte dans la même année
	 * [SIFISC-18817] Le mariage avec un mixte découpe maintenant les périodes (SOURCE avant, MIXTE après)
	 * --> la PIIS d'avant le départ HC devrait être SOURCE
	 */
	@Test
	public void testMixte1PartiHorsCantonRevientSourceEtSeMarieAvecMixteDansMemePeriode() throws Exception {

		final long noIndividu = 2678156L;
		final int lastYear = RegDate.get().year() - 1;
		final RegDate arrivee = date(lastYear, 1, 15);
		final RegDate depart = date(lastYear, 7, 12);
		final RegDate retour = date(lastYear, 9, 26);
		final RegDate mariage = date(lastYear, 11, 23);
		final RegDate departFinal = date(lastYear, 12, 31);

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				addIndividu(noIndividu, null, "Frigoletta", "Alessio", Sexe.MASCULIN);
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addHabitant(noIndividu);
				addForPrincipal(pp, arrivee, MotifFor.ARRIVEE_HS, depart, MotifFor.DEPART_HC, MockCommune.Lausanne, ModeImposition.MIXTE_137_1);
				addForPrincipal(pp, depart.getOneDayAfter(), MotifFor.DEPART_HC, retour.getOneDayBefore(), MotifFor.ARRIVEE_HC, MockCommune.Geneve, ModeImposition.SOURCE);
				addForPrincipal(pp, retour, MotifFor.ARRIVEE_HC, mariage.getOneDayBefore(), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Grandson, ModeImposition.SOURCE);

				final EnsembleTiersCouple couple = addEnsembleTiersCouple(pp, null, mariage, null);
				final MenageCommun mc = couple.getMenage();
				addForPrincipal(mc, mariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, departFinal, MotifFor.DEPART_HS, MockCommune.Grandson, ModeImposition.MIXTE_137_1);

				return pp.getNumero();
			}
		});

		// calcul des piis
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				final List<PeriodeImpositionImpotSource> piis = service.determine(pp);
				Assert.assertNotNull(piis);
				Assert.assertEquals(5, piis.size());

				{
					final PeriodeImpositionImpotSource pi = piis.get(0);
					Assert.assertNotNull(pi);
					Assert.assertEquals(PeriodeImpositionImpotSource.Type.SOURCE, pi.getType());
					Assert.assertEquals(Localisation.getInconnue(), pi.getLocalisation());
					Assert.assertNull(pi.getTypeAutoriteFiscale());
					Assert.assertNull(pi.getNoOfs());
					Assert.assertEquals(date(lastYear, 1, 1), pi.getDateDebut());
					Assert.assertEquals(arrivee.getOneDayBefore(), pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) ppId, pi.getContribuable().getNumero());
				}
				{
					final PeriodeImpositionImpotSource pi = piis.get(1);
					Assert.assertNotNull(pi);
					Assert.assertEquals(PeriodeImpositionImpotSource.Type.SOURCE, pi.getType());
					Assert.assertEquals(Localisation.getVaud(), pi.getLocalisation());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, pi.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockCommune.Lausanne.getNoOFS(), pi.getNoOfs());
					Assert.assertEquals(arrivee, pi.getDateDebut());
					Assert.assertEquals(depart.getLastDayOfTheMonth(), pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) ppId, pi.getContribuable().getNumero());
				}
				{
					final PeriodeImpositionImpotSource pi = piis.get(2);
					Assert.assertNotNull(pi);
					Assert.assertEquals(PeriodeImpositionImpotSource.Type.SOURCE, pi.getType());
					Assert.assertEquals(Localisation.getHorsCanton("GE"), pi.getLocalisation());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_HC, pi.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockCommune.Geneve.getNoOFS(), pi.getNoOfs());
					Assert.assertEquals(depart.getLastDayOfTheMonth().getOneDayAfter(), pi.getDateDebut());
					Assert.assertEquals(retour.getLastDayOfTheMonth(), pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) ppId, pi.getContribuable().getNumero());
				}
				{
					final PeriodeImpositionImpotSource pi = piis.get(3);
					Assert.assertNotNull(pi);
					Assert.assertEquals(PeriodeImpositionImpotSource.Type.SOURCE, pi.getType());
					Assert.assertEquals(Localisation.getVaud(), pi.getLocalisation());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, pi.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockCommune.Grandson.getNoOFS(), pi.getNoOfs());
					Assert.assertEquals(retour.getLastDayOfTheMonth().getOneDayAfter(), pi.getDateDebut());
					Assert.assertEquals(mariage.getLastDayOfTheMonth(), pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) ppId, pi.getContribuable().getNumero());
				}
				{
					final PeriodeImpositionImpotSource pi = piis.get(4);
					Assert.assertNotNull(pi);
					Assert.assertEquals(PeriodeImpositionImpotSource.Type.MIXTE, pi.getType());
					Assert.assertEquals(Localisation.getVaud(), pi.getLocalisation());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, pi.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockCommune.Grandson.getNoOFS(), pi.getNoOfs());
					Assert.assertEquals(mariage.getLastDayOfTheMonth().getOneDayAfter(), pi.getDateDebut());
					Assert.assertEquals(date(lastYear, 12, 31), pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) ppId, pi.getContribuable().getNumero());
				}
			}
		});
	}

	/**
	 * [SIFISC-12981] Cas du mixte 1 qui part HC et revient source puis se marie avec un ORDINAIRE dans la même année
	 * --> la PIIS d'avant le départ HC devrait rester SOURCE
	 */
	@Test
	public void testMixte1PartiHorsCantonRevientSourceEtSeMarieAvecOrdinaireDansMemePeriode() throws Exception {

		final long noIndividu = 2678156L;
		final int lastYear = RegDate.get().year() - 1;
		final RegDate arrivee = date(lastYear, 1, 15);
		final RegDate depart = date(lastYear, 7, 12);
		final RegDate retour = date(lastYear, 9, 26);
		final RegDate mariage = date(lastYear, 11, 23);
		final RegDate departFinal = date(lastYear, 12, 31);

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				addIndividu(noIndividu, null, "Frigoletta", "Alessio", Sexe.MASCULIN);
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addHabitant(noIndividu);
				addForPrincipal(pp, arrivee, MotifFor.ARRIVEE_HS, depart, MotifFor.DEPART_HC, MockCommune.Lausanne, ModeImposition.MIXTE_137_1);
				addForPrincipal(pp, depart.getOneDayAfter(), MotifFor.DEPART_HC, retour.getOneDayBefore(), MotifFor.ARRIVEE_HC, MockCommune.Geneve, ModeImposition.SOURCE);
				addForPrincipal(pp, retour, MotifFor.ARRIVEE_HC, mariage.getOneDayBefore(), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Grandson, ModeImposition.SOURCE);

				final EnsembleTiersCouple couple = addEnsembleTiersCouple(pp, null, mariage, null);
				final MenageCommun mc = couple.getMenage();
				addForPrincipal(mc, mariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, departFinal, MotifFor.DEPART_HS, MockCommune.Grandson);

				return pp.getNumero();
			}
		});

		// calcul des piis
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				final List<PeriodeImpositionImpotSource> piis = service.determine(pp);
				Assert.assertNotNull(piis);
				Assert.assertEquals(5, piis.size());

				{
					final PeriodeImpositionImpotSource pi = piis.get(0);
					Assert.assertNotNull(pi);
					Assert.assertEquals(PeriodeImpositionImpotSource.Type.SOURCE, pi.getType());
					Assert.assertEquals(Localisation.getInconnue(), pi.getLocalisation());
					Assert.assertNull(pi.getTypeAutoriteFiscale());
					Assert.assertNull(pi.getNoOfs());
					Assert.assertEquals(date(lastYear, 1, 1), pi.getDateDebut());
					Assert.assertEquals(arrivee.getOneDayBefore(), pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) ppId, pi.getContribuable().getNumero());
				}
				{
					final PeriodeImpositionImpotSource pi = piis.get(1);
					Assert.assertNotNull(pi);
					Assert.assertEquals(PeriodeImpositionImpotSource.Type.SOURCE, pi.getType());
					Assert.assertEquals(Localisation.getVaud(), pi.getLocalisation());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, pi.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockCommune.Lausanne.getNoOFS(), pi.getNoOfs());
					Assert.assertEquals(arrivee, pi.getDateDebut());
					Assert.assertEquals(depart.getLastDayOfTheMonth(), pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) ppId, pi.getContribuable().getNumero());
				}
				{
					final PeriodeImpositionImpotSource pi = piis.get(2);
					Assert.assertNotNull(pi);
					Assert.assertEquals(PeriodeImpositionImpotSource.Type.SOURCE, pi.getType());
					Assert.assertEquals(Localisation.getHorsCanton("GE"), pi.getLocalisation());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_HC, pi.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockCommune.Geneve.getNoOFS(), pi.getNoOfs());
					Assert.assertEquals(depart.getLastDayOfTheMonth().getOneDayAfter(), pi.getDateDebut());
					Assert.assertEquals(retour.getLastDayOfTheMonth(), pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) ppId, pi.getContribuable().getNumero());
				}
				{
					final PeriodeImpositionImpotSource pi = piis.get(3);
					Assert.assertNotNull(pi);
					Assert.assertEquals(PeriodeImpositionImpotSource.Type.SOURCE, pi.getType());
					Assert.assertEquals(Localisation.getVaud(), pi.getLocalisation());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, pi.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockCommune.Grandson.getNoOFS(), pi.getNoOfs());
					Assert.assertEquals(retour.getLastDayOfTheMonth().getOneDayAfter(), pi.getDateDebut());
					Assert.assertEquals(mariage.getLastDayOfTheMonth(), pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) ppId, pi.getContribuable().getNumero());
				}
				{
					final PeriodeImpositionImpotSource pi = piis.get(4);
					Assert.assertNotNull(pi);
					Assert.assertEquals(PeriodeImpositionImpotSource.Type.MIXTE, pi.getType());
					Assert.assertEquals(Localisation.getVaud(), pi.getLocalisation());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, pi.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockCommune.Grandson.getNoOFS(), pi.getNoOfs());
					Assert.assertEquals(mariage.getLastDayOfTheMonth().getOneDayAfter(), pi.getDateDebut());
					Assert.assertEquals(date(lastYear, 12, 31), pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) ppId, pi.getContribuable().getNumero());
				}
			}
		});
	}

	/**
	 * Cas du SIFISC-17535 : la PIIS de la PF avant la fusion de commune HC présente la commune du for après
	 * la fusion (= de la PF suivante !!)
	 */
	@Test
	public void testFusionCommunesHC() throws Exception {

		// mise en place civile -> rien
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				// rien
			}
		});

		// mise en place fiscale : for sur commune HC qui fusionne à la fin de l'année du dernier rapport de travail
		final long idPP = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Albertine", "Dugeat", date(1984, 5, 30), Sexe.FEMININ);
				final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.MENSUEL, date(2009, 1, 1));
				addRapportPrestationImposable(dpi, pp, date(2014, 1, 1), date(2015, 4, 12), false);
				addForPrincipal(pp, date(2014, 1, 1), MotifFor.ARRIVEE_HS, date(2015, 12, 31), MotifFor.FUSION_COMMUNES, MockCommune.Peseux, ModeImposition.SOURCE);
				addForPrincipal(pp, date(2016, 1, 1), MotifFor.FUSION_COMMUNES, MockCommune.Neuchatel, ModeImposition.SOURCE);
				return pp.getNumero();
			}
		});

		// calcul des PIIS
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(idPP);
				final List<PeriodeImpositionImpotSource> piis = service.determine(pp);
				Assert.assertNotNull(piis);
				Assert.assertEquals(2, piis.size());
				{
					final PeriodeImpositionImpotSource pi = piis.get(0);
					Assert.assertNotNull(pi);
					Assert.assertEquals(PeriodeImpositionImpotSource.Type.SOURCE, pi.getType());
					Assert.assertEquals(Localisation.getHorsCanton(MockCanton.Neuchatel.getSigleOFS()), pi.getLocalisation());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_HC, pi.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockCommune.Peseux.getNoOFS(), pi.getNoOfs());
					Assert.assertEquals(date(2014, 1, 1), pi.getDateDebut());
					Assert.assertEquals(date(2014, 12, 31), pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) idPP, pi.getContribuable().getNumero());
				}
				{
					final PeriodeImpositionImpotSource pi = piis.get(1);
					Assert.assertNotNull(pi);
					Assert.assertEquals(PeriodeImpositionImpotSource.Type.SOURCE, pi.getType());
					Assert.assertEquals(Localisation.getHorsCanton(MockCanton.Neuchatel.getSigleOFS()), pi.getLocalisation());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_HC, pi.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockCommune.Peseux.getNoOFS(), pi.getNoOfs());
					Assert.assertEquals(date(2015, 1, 1), pi.getDateDebut());
					Assert.assertEquals(date(2015, 12, 31), pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) idPP, pi.getContribuable().getNumero());
				}
			}
		});
	}

	/**
	 * [SIFISC-18078] Cas du mixte 1 qui part HC au 31.12 de la même période
	 * --> la PIIS d'avant le départ HC devrait être MIXTE
	 */
	@Test
	public void testMixte1PartiHorsCantonAu31DecembreDeMemePeriode() throws Exception {

		final long noIndividu = 2678156L;
		final int lastYear = RegDate.get().year() - 1;
		final RegDate arrivee = date(lastYear, 1, 15);
		final RegDate depart = date(lastYear, 7, 12);
		final RegDate retour = date(lastYear, 9, 26);
		final RegDate passageMixte = date(lastYear, 11, 23);
		final RegDate departFinal = date(lastYear, 12, 31);

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				addIndividu(noIndividu, null, "Frigoletta", "Alessio", Sexe.MASCULIN);
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addHabitant(noIndividu);
				addForPrincipal(pp, arrivee, MotifFor.ARRIVEE_HS, departFinal, MotifFor.DEPART_HC, MockCommune.Lausanne, ModeImposition.MIXTE_137_1);
				addForPrincipal(pp, departFinal.getOneDayAfter(), MotifFor.DEPART_HC, null, null, MockCommune.Bern, ModeImposition.SOURCE);
				return pp.getNumero();
			}
		});

		// calcul des piis
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
					Assert.assertEquals(Localisation.getInconnue(), pi.getLocalisation());
					Assert.assertNull(pi.getTypeAutoriteFiscale());
					Assert.assertNull(pi.getNoOfs());
					Assert.assertEquals(date(lastYear, 1, 1), pi.getDateDebut());
					Assert.assertEquals(arrivee.getOneDayBefore(), pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) ppId, pi.getContribuable().getNumero());
				}
				{
					final PeriodeImpositionImpotSource pi = piis.get(1);
					Assert.assertNotNull(pi);
					Assert.assertEquals(PeriodeImpositionImpotSource.Type.MIXTE, pi.getType());
					Assert.assertEquals(Localisation.getVaud(), pi.getLocalisation());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, pi.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockCommune.Lausanne.getNoOFS(), pi.getNoOfs());
					Assert.assertEquals(arrivee, pi.getDateDebut());
					Assert.assertEquals(departFinal, pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) ppId, pi.getContribuable().getNumero());
				}

			}
		});
	}

	/**
	 * [SIFISC-21190] cas d'une personne à la source dont l'arrivée hors canton se fait directement au premier janvier avec un for
	 * hors-canton source précédent -> ça pêtait en NPE
	 */
	@Test
	public void testForSourcePileFinAnneeAvecArriveeHC() throws Exception {

		final long noIndividu = 4651543L;
		final RegDate dateArriveeHS = date(2012, 1, 1);
		final RegDate dateDebutRapportTravail = date(2012, 8, 12);
		final RegDate dateArriveeHC = date(2014, 1, 1);
		final RegDate dateMariage = date(2015, 9, 13);
		final RegDate dateDepart = date(2015, 12, 31);

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				addIndividu(noIndividu, null, "Grandrenaud", "Escapador", Sexe.MASCULIN);
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addHabitant(noIndividu);
				addForPrincipal(pp, dateArriveeHS, MotifFor.ARRIVEE_HS, dateArriveeHC.getOneDayBefore(), MotifFor.ARRIVEE_HC, MockCommune.Bern, ModeImposition.SOURCE);
				addForPrincipal(pp, dateArriveeHC, MotifFor.ARRIVEE_HC, dateMariage.getOneDayBefore(), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Lausanne, ModeImposition.SOURCE);

				final EnsembleTiersCouple couple = addEnsembleTiersCouple(pp, null, dateMariage, null);
				final MenageCommun mc = couple.getMenage();
				addForPrincipal(mc, dateMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, dateDepart, MotifFor.DEPART_HS, MockCommune.Lausanne, ModeImposition.SOURCE);

				final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.MENSUEL, date(2009, 1, 1));
				addRapportPrestationImposable(dpi, pp, dateDebutRapportTravail, dateDepart, false);

				return pp.getNumero();
			}
		});

		// calcul des piis
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				final List<PeriodeImpositionImpotSource> piis = service.determine(pp);
				Assert.assertNotNull(piis);
				Assert.assertEquals(4, piis.size());

				{
					final PeriodeImpositionImpotSource pi = piis.get(0);
					Assert.assertNotNull(pi);
					Assert.assertEquals(PeriodeImpositionImpotSource.Type.SOURCE, pi.getType());
					Assert.assertEquals(Localisation.getHorsCanton(MockCommune.Bern.getSigleCanton()), pi.getLocalisation());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_HC, pi.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockCommune.Bern.getNoOFS(), pi.getNoOfs());
					Assert.assertEquals(date(2012, 1, 1), pi.getDateDebut());
					Assert.assertEquals(date(2012, 12, 31), pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) ppId, pi.getContribuable().getNumero());
				}
				{
					final PeriodeImpositionImpotSource pi = piis.get(1);
					Assert.assertNotNull(pi);
					Assert.assertEquals(PeriodeImpositionImpotSource.Type.SOURCE, pi.getType());
					Assert.assertEquals(Localisation.getHorsCanton(MockCommune.Bern.getSigleCanton()), pi.getLocalisation());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_HC, pi.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockCommune.Bern.getNoOFS(), pi.getNoOfs());
					Assert.assertEquals(date(2013, 1, 1), pi.getDateDebut());
					Assert.assertEquals(date(2013, 12, 31), pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) ppId, pi.getContribuable().getNumero());
				}
				{
					final PeriodeImpositionImpotSource pi = piis.get(2);
					Assert.assertNotNull(pi);
					Assert.assertEquals(PeriodeImpositionImpotSource.Type.SOURCE, pi.getType());
					Assert.assertEquals(Localisation.getVaud(), pi.getLocalisation());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, pi.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockCommune.Lausanne.getNoOFS(), pi.getNoOfs());
					Assert.assertEquals(date(2014, 1, 1), pi.getDateDebut());
					Assert.assertEquals(date(2014, 12, 31), pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) ppId, pi.getContribuable().getNumero());
				}
				{
					final PeriodeImpositionImpotSource pi = piis.get(3);
					Assert.assertNotNull(pi);
					Assert.assertEquals(PeriodeImpositionImpotSource.Type.SOURCE, pi.getType());
					Assert.assertEquals(Localisation.getVaud(), pi.getLocalisation());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, pi.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockCommune.Lausanne.getNoOFS(), pi.getNoOfs());
					Assert.assertEquals(date(2015, 1, 1), pi.getDateDebut());
					Assert.assertEquals(date(2015, 12, 31), pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) ppId, pi.getContribuable().getNumero());
				}
			}
		});
	}

	/**
	 * [SIFISC-21541] Cas du mixte 2 qui fonctionnait à l'identique du mixte 1 mais qui n'aurait pas dû
	 */
	@Test
	public void testArriveeHCMixte2AvecForPrecedentSource() throws Exception {

		final long noIndividu = 4651543L;
		final RegDate dateArriveeHS = date(2014, 5, 14);
		final RegDate dateArriveeHC = date(2016, 6, 15);
		final RegDate dateDeces = date(2016, 10, 10);

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				addIndividu(noIndividu, null, "Grandrenaud", "Escapador", Sexe.MASCULIN);
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addHabitant(noIndividu);
				addForPrincipal(pp, dateArriveeHS, MotifFor.ARRIVEE_HS, dateArriveeHC.getOneDayBefore(), MotifFor.ARRIVEE_HC, MockCommune.Bern, ModeImposition.SOURCE);
				addForPrincipal(pp, dateArriveeHC, MotifFor.ARRIVEE_HC, dateDeces, MotifFor.VEUVAGE_DECES, MockCommune.Lausanne, ModeImposition.MIXTE_137_2);
				return pp.getNumero();
			}
		});

		// calcul des piis
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
					Assert.assertEquals(Localisation.getVaud(), pi.getLocalisation());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, pi.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockCommune.Lausanne.getNoOFS(), pi.getNoOfs());
					Assert.assertEquals(date(2016, 1, 1), pi.getDateDebut());
					Assert.assertEquals(dateDeces, pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) ppId, pi.getContribuable().getNumero());
				}
				{
					final PeriodeImpositionImpotSource pi = piis.get(1);
					Assert.assertNotNull(pi);
					Assert.assertEquals(PeriodeImpositionImpotSource.Type.SOURCE, pi.getType());
					Assert.assertEquals(Localisation.getInconnue(), pi.getLocalisation());
					Assert.assertNull(pi.getTypeAutoriteFiscale());
					Assert.assertNull(pi.getNoOfs());
					Assert.assertEquals(dateDeces.getOneDayAfter(), pi.getDateDebut());
					Assert.assertEquals(date(2016, 12, 31), pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) ppId, pi.getContribuable().getNumero());
				}
			}
		});
	}

	/**
	 * [SIFISC-21541] Cas du mixte 2 qui fonctionnait déjà en absence de for avant l'arrivée HC
	 */
	@Test
	public void testArriveeHCMixte2SansForPrecedent() throws Exception {

		final long noIndividu = 4651543L;
		final RegDate dateArriveeHS = date(2014, 5, 14);
		final RegDate dateArriveeHC = date(2016, 6, 15);
		final RegDate dateDeces = date(2016, 10, 10);

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				addIndividu(noIndividu, null, "Grandrenaud", "Escapador", Sexe.MASCULIN);
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addHabitant(noIndividu);
				addForPrincipal(pp, dateArriveeHC, MotifFor.ARRIVEE_HC, dateDeces, MotifFor.VEUVAGE_DECES, MockCommune.Lausanne, ModeImposition.MIXTE_137_2);
				return pp.getNumero();
			}
		});

		// calcul des piis
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
					Assert.assertEquals(Localisation.getVaud(), pi.getLocalisation());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, pi.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockCommune.Lausanne.getNoOFS(), pi.getNoOfs());
					Assert.assertEquals(date(2016, 1, 1), pi.getDateDebut());
					Assert.assertEquals(dateDeces, pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) ppId, pi.getContribuable().getNumero());
				}
				{
					final PeriodeImpositionImpotSource pi = piis.get(1);
					Assert.assertNotNull(pi);
					Assert.assertEquals(PeriodeImpositionImpotSource.Type.SOURCE, pi.getType());
					Assert.assertEquals(Localisation.getInconnue(), pi.getLocalisation());
					Assert.assertNull(pi.getTypeAutoriteFiscale());
					Assert.assertNull(pi.getNoOfs());
					Assert.assertEquals(dateDeces.getOneDayAfter(), pi.getDateDebut());
					Assert.assertEquals(date(2016, 12, 31), pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) ppId, pi.getContribuable().getNumero());
				}
			}
		});
	}

	/**
	 * [SIFISC-21541] Cas du mixte 1 qui fonctionnait déjà
	 */
	@Test
	public void testArriveeHCMixte1AvecForPrecedentSource() throws Exception {

		final long noIndividu = 4651543L;
		final RegDate dateArriveeHS = date(2014, 5, 14);
		final RegDate dateArriveeHC = date(2016, 6, 15);
		final RegDate dateDeces = date(2016, 10, 10);

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				addIndividu(noIndividu, null, "Grandrenaud", "Escapador", Sexe.MASCULIN);
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addHabitant(noIndividu);
				addForPrincipal(pp, dateArriveeHS, MotifFor.ARRIVEE_HS, dateArriveeHC.getOneDayBefore(), MotifFor.ARRIVEE_HC, MockCommune.Bern, ModeImposition.SOURCE);
				addForPrincipal(pp, dateArriveeHC, MotifFor.ARRIVEE_HC, dateDeces, MotifFor.VEUVAGE_DECES, MockCommune.Lausanne, ModeImposition.MIXTE_137_1);
				return pp.getNumero();
			}
		});

		// calcul des piis
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
					Assert.assertEquals(Localisation.getHorsCanton(MockCommune.Bern.getSigleCanton()), pi.getLocalisation());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_HC, pi.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockCommune.Bern.getNoOFS(), pi.getNoOfs());
					Assert.assertEquals(date(2016, 1, 1), pi.getDateDebut());
					Assert.assertEquals(dateArriveeHC.getLastDayOfTheMonth(), pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) ppId, pi.getContribuable().getNumero());
				}
				{
					final PeriodeImpositionImpotSource pi = piis.get(1);
					Assert.assertNotNull(pi);
					Assert.assertEquals(PeriodeImpositionImpotSource.Type.MIXTE, pi.getType());
					Assert.assertEquals(Localisation.getVaud(), pi.getLocalisation());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, pi.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockCommune.Lausanne.getNoOFS(), pi.getNoOfs());
					Assert.assertEquals(dateArriveeHC.getLastDayOfTheMonth().getOneDayAfter(), pi.getDateDebut());
					Assert.assertEquals(dateDeces, pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) ppId, pi.getContribuable().getNumero());
				}
				{
					final PeriodeImpositionImpotSource pi = piis.get(2);
					Assert.assertNotNull(pi);
					Assert.assertEquals(PeriodeImpositionImpotSource.Type.SOURCE, pi.getType());
					Assert.assertEquals(Localisation.getInconnue(), pi.getLocalisation());
					Assert.assertNull(pi.getTypeAutoriteFiscale());
					Assert.assertNull(pi.getNoOfs());
					Assert.assertEquals(dateDeces.getOneDayAfter(), pi.getDateDebut());
					Assert.assertEquals(date(2016, 12, 31), pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) ppId, pi.getContribuable().getNumero());
				}
			}
		});
	}

	/**
	 * [SIFISC-21541] Cas du mixte 1 qui fonctionnait déjà
	 */
	@Test
	public void testArriveeHCMixte1SansForPrecedent() throws Exception {

		final long noIndividu = 4651543L;
		final RegDate dateArriveeHS = date(2014, 5, 14);
		final RegDate dateArriveeHC = date(2016, 6, 15);
		final RegDate dateDeces = date(2016, 10, 10);

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				addIndividu(noIndividu, null, "Grandrenaud", "Escapador", Sexe.MASCULIN);
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addHabitant(noIndividu);
				addForPrincipal(pp, dateArriveeHC, MotifFor.ARRIVEE_HC, dateDeces, MotifFor.VEUVAGE_DECES, MockCommune.Lausanne, ModeImposition.MIXTE_137_1);
				return pp.getNumero();
			}
		});

		// calcul des piis
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
					Assert.assertEquals(Localisation.getInconnue(), pi.getLocalisation());
					Assert.assertNull(pi.getTypeAutoriteFiscale());
					Assert.assertNull(pi.getNoOfs());
					Assert.assertEquals(date(2016, 1, 1), pi.getDateDebut());
					Assert.assertEquals(dateArriveeHC.getLastDayOfTheMonth(), pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) ppId, pi.getContribuable().getNumero());
				}
				{
					final PeriodeImpositionImpotSource pi = piis.get(1);
					Assert.assertNotNull(pi);
					Assert.assertEquals(PeriodeImpositionImpotSource.Type.MIXTE, pi.getType());
					Assert.assertEquals(Localisation.getVaud(), pi.getLocalisation());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, pi.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockCommune.Lausanne.getNoOFS(), pi.getNoOfs());
					Assert.assertEquals(dateArriveeHC.getLastDayOfTheMonth().getOneDayAfter(), pi.getDateDebut());
					Assert.assertEquals(dateDeces, pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) ppId, pi.getContribuable().getNumero());
				}
				{
					final PeriodeImpositionImpotSource pi = piis.get(2);
					Assert.assertNotNull(pi);
					Assert.assertEquals(PeriodeImpositionImpotSource.Type.SOURCE, pi.getType());
					Assert.assertEquals(Localisation.getInconnue(), pi.getLocalisation());
					Assert.assertNull(pi.getTypeAutoriteFiscale());
					Assert.assertNull(pi.getNoOfs());
					Assert.assertEquals(dateDeces.getOneDayAfter(), pi.getDateDebut());
					Assert.assertEquals(date(2016, 12, 31), pi.getDateFin());
					Assert.assertNotNull(pi.getContribuable());
					Assert.assertEquals((Long) ppId, pi.getContribuable().getNumero());
				}
			}
		});
	}
}
