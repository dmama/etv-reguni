package ch.vd.uniregctb.lr.manager;

import org.junit.Test;
import org.springframework.transaction.TransactionStatus;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.uniregctb.common.ObjectNotFoundException;
import ch.vd.uniregctb.common.WebTest;
import ch.vd.uniregctb.declaration.PeriodeFiscale;
import ch.vd.uniregctb.lr.view.ListeRecapDetailView;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.PeriodeDecompte;
import ch.vd.uniregctb.type.PeriodiciteDecompte;
import ch.vd.uniregctb.type.TypeEtatDeclaration;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.fail;

public class ListeRecapManagerTest extends WebTest {

	private ListeRecapEditManager lrEditManager = null;

	private final static String DB_UNIT_FILE = "ListeRecapManagerTest.xml";

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		lrEditManager = getBean(ListeRecapEditManager.class, "lrEditManager");


	}

	/**
	 * Teste la methode creerLr
	 *
	 * @throws Exception
	 */
	@Test
	public void testCreerLr() throws Exception {
		loadDatabase(DB_UNIT_FILE);

		{
			ListeRecapDetailView lrView = lrEditManager.creerLr(new Long(12500001));
			RegDate dateDebutPeriodeAttendue = RegDate.get(2008, 2, 1);
			RegDate dateFinPeriodeAttendue = RegDate.get(2008, 2, 29);
			assertNotNull(lrView);
			assertEquals(dateDebutPeriodeAttendue, lrView.getRegDateDebutPeriode());
			assertEquals(dateFinPeriodeAttendue, lrView.getRegDateFinPeriode());
		}

		{
			ListeRecapDetailView lrView = lrEditManager.creerLr(new Long(12500002));
			assertNotNull(lrView);
			RegDate dateDebutPeriodeAttendue = RegDate.get(2008, 3, 1);
			RegDate dateFinPeriodeAttendue = RegDate.get(2008, 3, 31);
			assertEquals(dateDebutPeriodeAttendue, lrView.getRegDateDebutPeriode());
			assertEquals(dateFinPeriodeAttendue, lrView.getRegDateFinPeriode());
		}

		{
			ListeRecapDetailView lrView = lrEditManager.creerLr(new Long(12500003));
			assertNotNull(lrView);
			RegDate dateDebutPeriodeAttendue = RegDate.get(2007, 3, 1);
			RegDate dateFinPeriodeAttendue = RegDate.get(2007, 3, 31);
			assertEquals(dateDebutPeriodeAttendue, lrView.getRegDateDebutPeriode());
			assertEquals(dateFinPeriodeAttendue, lrView.getRegDateFinPeriode());
		}
	}

	/**
	 * Teste la methode testCreerLrForPeriodiciteUnique
	 *
	 * @throws Exception
	 */
	@Test
	public void testCreerLrForPeriodiciteUnique() throws Exception {

		loadDatabase(DB_UNIT_FILE);

		{
			ListeRecapDetailView lrView = lrEditManager.creerLr(new Long(12500004));
			RegDate dateDebutPeriodeAttendue = RegDate.get(2007, 4, 1);
			RegDate dateFinPeriodeAttendue = RegDate.get(2007, 4, 30);
			assertNotNull(lrView);
			assertEquals(dateDebutPeriodeAttendue, lrView.getRegDateDebutPeriode());
			assertEquals(dateFinPeriodeAttendue, lrView.getRegDateFinPeriode());
		}

		{
			ListeRecapDetailView lrView = lrEditManager.creerLr(new Long(12500005));
			assertNotNull(lrView);
			RegDate dateDebutPeriodeAttendue = RegDate.get(2008, 4, 1);
			RegDate dateFinPeriodeAttendue = RegDate.get(2008, 6, 30);
			assertEquals(dateDebutPeriodeAttendue, lrView.getRegDateDebutPeriode());
			assertEquals(dateFinPeriodeAttendue, lrView.getRegDateFinPeriode());
		}

		{
			ListeRecapDetailView lrView = lrEditManager.creerLr(new Long(12500006));
			assertNotNull(lrView);
			RegDate dateDebutPeriodeAttendue = RegDate.get(2007, 7, 1);
			RegDate dateFinPeriodeAttendue = RegDate.get(2007, 12, 31);
			assertEquals(dateDebutPeriodeAttendue, lrView.getRegDateDebutPeriode());
			assertEquals(dateFinPeriodeAttendue, lrView.getRegDateFinPeriode());
		}

		{
			ListeRecapDetailView lrView = lrEditManager.creerLr(new Long(12500007));
			assertNotNull(lrView);
			RegDate dateDebutPeriodeAttendue = RegDate.get(2008, 1, 1);
			RegDate dateFinPeriodeAttendue = RegDate.get(2008, 12, 31);
			assertEquals(dateDebutPeriodeAttendue, lrView.getRegDateDebutPeriode());
			assertEquals(dateFinPeriodeAttendue, lrView.getRegDateFinPeriode());
		}
	}

	@Test
	public void testLRForPeriodicitesMultiples() throws Exception {
		//Ajout d'une première periodicite'
		final int anneeReference = RegDate.get().year();
		final int anneeSuivante = anneeReference + 1;
		final long dpiId = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				DebiteurPrestationImposable dpi = addDebiteur();
				tiersService.addPeriodicite(dpi, PeriodiciteDecompte.TRIMESTRIEL, null, date(anneeReference, 1, 1), date(anneeReference, 12, 31));
				tiersService.addPeriodicite(dpi, PeriodiciteDecompte.MENSUEL, null, date(anneeSuivante, 1, 1), null);

				addForDebiteur(dpi, date(anneeReference, 1, 1), MotifFor.INDETERMINE, null, null, MockCommune.Bex);

				final PeriodeFiscale fiscale = addPeriodeFiscale(anneeReference);

				addLR(dpi, date(anneeReference, 1, 1), PeriodiciteDecompte.TRIMESTRIEL, fiscale, TypeEtatDeclaration.EMISE);
				addLR(dpi, date(anneeReference, 4, 1), PeriodiciteDecompte.TRIMESTRIEL, fiscale, TypeEtatDeclaration.EMISE);
				addLR(dpi, date(anneeReference, 7, 1), PeriodiciteDecompte.TRIMESTRIEL, fiscale, TypeEtatDeclaration.EMISE);
				addLR(dpi, date(anneeReference, 10, 1), PeriodiciteDecompte.TRIMESTRIEL, fiscale, TypeEtatDeclaration.EMISE);
				return dpi.getNumero();
			}
		});

		ListeRecapDetailView lrView = lrEditManager.creerLr(dpiId);
		assertNotNull(lrView);
		RegDate dateDebutPeriodeAttendue = RegDate.get(anneeSuivante, 1, 1);
		RegDate dateFinPeriodeAttendue = RegDate.get(anneeSuivante, 1, 31);
		assertEquals(dateDebutPeriodeAttendue, lrView.getRegDateDebutPeriode());
		assertEquals(dateFinPeriodeAttendue, lrView.getRegDateFinPeriode());
	}


	//[UNIREG-3115]
	@Test
	public void testLRForPeriodicites_3115() throws Exception {
		//Ajout d'une première periodicite'
		final int anneeReference = 2009;
		final long dpiId = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				DebiteurPrestationImposable dpi = addDebiteur();
				tiersService.addPeriodicite(dpi, PeriodiciteDecompte.TRIMESTRIEL, null, date(anneeReference, 9, 1), null);
				addForDebiteur(dpi, date(anneeReference, 9, 1), MotifFor.INDETERMINE, null, null, MockCommune.Bex);

				final PeriodeFiscale fiscale = addPeriodeFiscale(anneeReference);

				addLR(dpi, date(anneeReference, 10, 1), PeriodiciteDecompte.TRIMESTRIEL, fiscale, TypeEtatDeclaration.EMISE);

				return dpi.getNumero();
			}
		});

		ListeRecapDetailView lrView = lrEditManager.creerLr(dpiId);
		assertNotNull(lrView);
		RegDate dateDebutPeriodeAttendue = RegDate.get(anneeReference, 7, 1);
		RegDate dateFinPeriodeAttendue = RegDate.get(anneeReference, 9, 30);
		assertEquals(dateDebutPeriodeAttendue, lrView.getRegDateDebutPeriode());
		assertEquals(dateFinPeriodeAttendue, lrView.getRegDateFinPeriode());
	}


	//[UNIREG-3120]
	@Test
	public void testLRForPeriodicites_3120() throws Exception {
		//Ajout d'une première periodicite'
		final int anneeReference = 2009;
		final int anneeSuivante = 2010;
		final long dpiId = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				DebiteurPrestationImposable dpi = addDebiteur();
				tiersService.addPeriodicite(dpi, PeriodiciteDecompte.TRIMESTRIEL, null, date(anneeReference, 9, 1), null);
				addForDebiteur(dpi, date(anneeReference, 1, 1), MotifFor.INDETERMINE, date(anneeReference, 6, 30), MotifFor.INDETERMINE, MockCommune.Bex);
				addForDebiteur(dpi, date(anneeSuivante, 10, 1), MotifFor.INDETERMINE, null, null, MockCommune.Bex);

				final PeriodeFiscale fiscale = addPeriodeFiscale(anneeReference);

				addLR(dpi, date(anneeReference, 1, 1), PeriodiciteDecompte.TRIMESTRIEL, fiscale, TypeEtatDeclaration.EMISE);
				addLR(dpi, date(anneeReference, 4, 1), PeriodiciteDecompte.TRIMESTRIEL, fiscale, TypeEtatDeclaration.EMISE);

				return dpi.getNumero();
			}
		});

		ListeRecapDetailView lrView = lrEditManager.creerLr(dpiId);
		assertNotNull(lrView);
		RegDate dateDebutPeriodeAttendue = RegDate.get(anneeSuivante, 10, 1);
		RegDate dateFinPeriodeAttendue = RegDate.get(anneeSuivante, 12, 31);
		assertEquals(dateDebutPeriodeAttendue, lrView.getRegDateDebutPeriode());
		assertEquals(dateFinPeriodeAttendue, lrView.getRegDateFinPeriode());
	}

	//[UNIREG-3120] le debiteur n'a pas de for pour la periode pour laquelle on veut générer la lr
	@Test
	public void testLRForPeriodicites_3120_2() throws Exception {
		//Ajout d'une première periodicite'
		final int anneeReference = 2009;
		final int anneeSuivante = 2010;
		final long dpiId = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				DebiteurPrestationImposable dpi = addDebiteur();
				tiersService.addPeriodicite(dpi, PeriodiciteDecompte.TRIMESTRIEL, null, date(anneeReference, 1, 1), null);
				addForDebiteur(dpi, date(anneeReference, 1, 1), MotifFor.INDETERMINE, date(anneeReference, 6, 30), MotifFor.INDETERMINE, MockCommune.Bex);
				addForDebiteur(dpi, date(anneeSuivante, 10, 1), MotifFor.INDETERMINE, date(anneeSuivante, 12, 31), MotifFor.INDETERMINE, MockCommune.Bex);

				final PeriodeFiscale fiscale2009 = addPeriodeFiscale(anneeReference);
				final PeriodeFiscale fiscale2010 = addPeriodeFiscale(anneeSuivante);

				addLR(dpi, date(anneeReference, 1, 1), PeriodiciteDecompte.TRIMESTRIEL, fiscale2009, TypeEtatDeclaration.EMISE);
				addLR(dpi, date(anneeReference, 4, 1), PeriodiciteDecompte.TRIMESTRIEL, fiscale2009, TypeEtatDeclaration.EMISE);
				addLR(dpi, date(anneeSuivante, 10, 1), PeriodiciteDecompte.TRIMESTRIEL, fiscale2010, TypeEtatDeclaration.EMISE);

				return dpi.getNumero();
			}
		});

		try {
			lrEditManager.creerLr(dpiId);
			fail("Il ne doit plus être possible de créer des LR");
		}
		catch (ObjectNotFoundException e) {
			assertEquals("Toutes les LR du débiteur ont déjà été émises", e.getMessage());
		}
	}

	//le debiteur possède plusieurs for mais pas de manière continue
	@Test
	public void testLRForPeriodicites_3120_3() throws Exception {
//Ajout d'une première periodicite'
		final int anneeReference = 2009;
		final int anneeSuivante = 2010;
		final int anneePostSuivante = 2011;

		final long dpiId = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				DebiteurPrestationImposable dpi = addDebiteur();
				tiersService.addPeriodicite(dpi, PeriodiciteDecompte.TRIMESTRIEL, null, date(anneeReference, 1, 1), null);
				addForDebiteur(dpi, date(anneeReference, 1, 1), MotifFor.INDETERMINE, date(anneeReference, 6, 30), MotifFor.INDETERMINE, MockCommune.Bex);
				addForDebiteur(dpi, date(anneeSuivante, 10, 1), MotifFor.INDETERMINE, date(anneeSuivante, 12, 31), MotifFor.INDETERMINE, MockCommune.Bex);
				addForDebiteur(dpi, date(anneePostSuivante, 3, 1), MotifFor.INDETERMINE, null, null, MockCommune.Bex);
				final PeriodeFiscale fiscale2009 = addPeriodeFiscale(anneeReference);
				final PeriodeFiscale fiscale2010 = addPeriodeFiscale(anneeSuivante);
				final PeriodeFiscale fiscale2011 = addPeriodeFiscale(anneePostSuivante);

				addLR(dpi, date(anneeReference, 1, 1), PeriodiciteDecompte.TRIMESTRIEL, fiscale2009, TypeEtatDeclaration.EMISE);
				addLR(dpi, date(anneeReference, 4, 1), PeriodiciteDecompte.TRIMESTRIEL, fiscale2009, TypeEtatDeclaration.EMISE);
				addLR(dpi, date(anneeSuivante, 10, 1), PeriodiciteDecompte.TRIMESTRIEL, fiscale2010, TypeEtatDeclaration.EMISE);

				return dpi.getNumero();
			}
		});

		ListeRecapDetailView lrView = lrEditManager.creerLr(dpiId);
		assertNotNull(lrView);
		RegDate dateDebutPeriodeAttendue = RegDate.get(anneePostSuivante, 1, 1);
		RegDate dateFinPeriodeAttendue = RegDate.get(anneePostSuivante, 3, 31);
		assertEquals(dateDebutPeriodeAttendue, lrView.getRegDateDebutPeriode());
		assertEquals(dateFinPeriodeAttendue, lrView.getRegDateFinPeriode());
	}


	@Test
	public void testLRForPeriodicitesMultiplesUniques() throws Exception {
		//Ajout d'une première periodicite'
		final int anneeReference = RegDate.get().year();
		final int anneeSuivante = anneeReference + 1;
		final long dpiId = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				DebiteurPrestationImposable dpi = addDebiteur();

				tiersService.addPeriodicite(dpi, PeriodiciteDecompte.UNIQUE, PeriodeDecompte.M08, date(anneeReference, 1, 1), date(anneeReference, 12, 31));

				tiersService.addPeriodicite(dpi, PeriodiciteDecompte.MENSUEL, null, date(anneeSuivante, 1, 1), null);
				addForDebiteur(dpi, date(anneeReference, 1, 1), MotifFor.INDETERMINE, null, null, MockCommune.Bex);

				final PeriodeFiscale periodeFiscale = addPeriodeFiscale(anneeReference);

				addLRPeriodiciteUnique(dpi, date(anneeReference, 8, 1), date(anneeReference, 8, 31), periodeFiscale, TypeEtatDeclaration.EMISE);
				return dpi.getNumero();
			}
		});

		ListeRecapDetailView lrView = lrEditManager.creerLr(dpiId);
		assertNotNull(lrView);
		RegDate dateDebutPeriodeAttendue = RegDate.get(anneeSuivante, 1, 1);
		RegDate dateFinPeriodeAttendue = RegDate.get(anneeSuivante, 1, 31);
		assertEquals(dateDebutPeriodeAttendue, lrView.getRegDateDebutPeriode());
		assertEquals(dateFinPeriodeAttendue, lrView.getRegDateFinPeriode());
	}


	@Test
	public void testLRForPeriodicitesMensuelUnique() throws Exception {
	//Ajout d'une première periodicite'final int anneeReference = RegDate.get().year();
		final int anneeReference = RegDate.get().year();
		final int anneeSuivante = anneeReference + 1;

		final long dpiId = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				DebiteurPrestationImposable dpi = addDebiteur();

				tiersService.addPeriodicite(dpi, PeriodiciteDecompte.TRIMESTRIEL, null, date(anneeReference, 1, 1), date(anneeReference, 12, 31));
				tiersService.addPeriodicite(dpi, PeriodiciteDecompte.UNIQUE, PeriodeDecompte.M06, date(anneeSuivante, 1, 1), null);
				addForDebiteur(dpi, date(anneeReference, 1, 1), MotifFor.INDETERMINE, null, null, MockCommune.Bex);

				final PeriodeFiscale periodeFiscale = addPeriodeFiscale(anneeReference);
				addLR(dpi, date(anneeReference, 1, 1), PeriodiciteDecompte.TRIMESTRIEL, periodeFiscale, TypeEtatDeclaration.EMISE);
				addLR(dpi, date(anneeReference, 4, 1), PeriodiciteDecompte.TRIMESTRIEL, periodeFiscale, TypeEtatDeclaration.EMISE);
				addLR(dpi, date(anneeReference, 7, 1), PeriodiciteDecompte.TRIMESTRIEL, periodeFiscale, TypeEtatDeclaration.EMISE);
				addLR(dpi, date(anneeReference, 10, 1), PeriodiciteDecompte.TRIMESTRIEL, periodeFiscale, TypeEtatDeclaration.EMISE);

				return dpi.getNumero();
			}
		});

		ListeRecapDetailView lrView = lrEditManager.creerLr(dpiId);
		assertNotNull(lrView);
		RegDate dateDebutPeriodeAttendue = RegDate.get(anneeSuivante, 6, 1);
		RegDate dateFinPeriodeAttendue = RegDate.get(anneeSuivante, 6, 30);
		assertEquals(dateDebutPeriodeAttendue, lrView.getRegDateDebutPeriode());
		assertEquals(dateFinPeriodeAttendue, lrView.getRegDateFinPeriode());
	}

	@Test
	public void testLRForPeriodicitesMensuelUniqueMensuel() throws Exception {
		//Ajout d'une première periodicite'

		final int anneeReference = RegDate.get().year();
		final int anneePrecedente = anneeReference - 1;
		final int anneeSuivante = anneeReference + 1;
		final long dpiId = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				DebiteurPrestationImposable dpi = addDebiteur();
				tiersService.addPeriodicite(dpi, PeriodiciteDecompte.MENSUEL, null, date(anneePrecedente, 1, 1), date(anneePrecedente, 12, 31));
				tiersService.addPeriodicite(dpi, PeriodiciteDecompte.UNIQUE, PeriodeDecompte.M06, date(anneeReference, 1, 1), date(anneeReference, 12, 31));
				tiersService.addPeriodicite(dpi, PeriodiciteDecompte.MENSUEL, null, date(anneeSuivante, 1, 1), null);

				addForDebiteur(dpi, date(anneePrecedente, 3, 1), MotifFor.INDETERMINE, null, null, MockCommune.Bex);

				return dpi.getNumero();
			}
		});

		ListeRecapDetailView lrView = lrEditManager.creerLr(dpiId);
		assertNotNull(lrView);
		RegDate dateDebutPeriodeAttendue = RegDate.get(anneePrecedente, 3, 1);
		RegDate dateFinPeriodeAttendue = RegDate.get(anneePrecedente, 3, 31);
		assertEquals(dateDebutPeriodeAttendue, lrView.getRegDateDebutPeriode());
		assertEquals(dateFinPeriodeAttendue, lrView.getRegDateFinPeriode());
	}


	@Test
	public void testLRForPeriodicitesTrimestriel() throws Exception {
		//Ajout d'une première periodicite'final int anneeReference = RegDate.get().year();
		final int anneeReference = RegDate.get().year();
		final int anneeSuivante = anneeReference + 1;

		final long dpiId = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				DebiteurPrestationImposable dpi = addDebiteur();

				tiersService.addPeriodicite(dpi, PeriodiciteDecompte.TRIMESTRIEL, null, date(anneeReference, 3, 1), null);

				addForDebiteur(dpi, date(anneeReference, 1, 1), MotifFor.INDETERMINE, null, null, MockCommune.Bex);

				final PeriodeFiscale periodeFiscale = addPeriodeFiscale(anneeReference);
				addLR(dpi, date(anneeReference, 1, 1), PeriodiciteDecompte.TRIMESTRIEL, periodeFiscale, TypeEtatDeclaration.EMISE);
				addLR(dpi, date(anneeReference, 4, 1), PeriodiciteDecompte.TRIMESTRIEL, periodeFiscale, TypeEtatDeclaration.EMISE);
				addLR(dpi, date(anneeReference, 7, 1), PeriodiciteDecompte.TRIMESTRIEL, periodeFiscale, TypeEtatDeclaration.EMISE);
				addLR(dpi, date(anneeReference, 10, 1), PeriodiciteDecompte.TRIMESTRIEL, periodeFiscale, TypeEtatDeclaration.EMISE);

				return dpi.getNumero();
			}
		});

		ListeRecapDetailView lrView = lrEditManager.creerLr(new Long(dpiId));
		assertNotNull(lrView);
		RegDate dateDebutPeriodeAttendue = RegDate.get(anneeSuivante, 1, 1);
		RegDate dateFinPeriodeAttendue = RegDate.get(anneeSuivante, 3, 31);
		assertEquals(dateDebutPeriodeAttendue, lrView.getRegDateDebutPeriode());
		assertEquals(dateFinPeriodeAttendue, lrView.getRegDateFinPeriode());
	}

	@Test
	public void testLRForPeriodicitesTrimestrielSansDecembre() throws Exception {
		//Ajout d'une première periodicite'final int anneeReference = RegDate.get().year();
		final int anneeReference = RegDate.get().year();
		final int anneeSuivante = anneeReference + 1;

		final long dpiId = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				DebiteurPrestationImposable dpi = addDebiteur();

				tiersService.addPeriodicite(dpi, PeriodiciteDecompte.TRIMESTRIEL, null, date(anneeReference, 3, 1), null);

				addForDebiteur(dpi, date(anneeReference, 1, 1), MotifFor.INDETERMINE, null, null, MockCommune.Bex);

				final PeriodeFiscale periodeFiscale = addPeriodeFiscale(anneeReference);
				addLR(dpi, date(anneeReference, 1, 1), PeriodiciteDecompte.TRIMESTRIEL, periodeFiscale, TypeEtatDeclaration.EMISE);
				addLR(dpi, date(anneeReference, 4, 1), PeriodiciteDecompte.TRIMESTRIEL, periodeFiscale, TypeEtatDeclaration.EMISE);
				addLR(dpi, date(anneeReference, 7, 1), PeriodiciteDecompte.TRIMESTRIEL, periodeFiscale, TypeEtatDeclaration.EMISE);


				return dpi.getNumero();
			}
		});

		ListeRecapDetailView lrView = lrEditManager.creerLr(dpiId);
		assertNotNull(lrView);
		RegDate dateDebutPeriodeAttendue = RegDate.get(anneeReference, 10, 1);
		RegDate dateFinPeriodeAttendue = RegDate.get(anneeReference, 12, 31);
		assertEquals(dateDebutPeriodeAttendue, lrView.getRegDateDebutPeriode());
		assertEquals(dateFinPeriodeAttendue, lrView.getRegDateFinPeriode());
	}

	@Test
	public void testLRForPeriodicitesUnique() throws Exception {
		//Ajout d'une première periodicite'final int anneeReference = RegDate.get().year();
		final int anneeReference = RegDate.get().year();
		final int anneeSuivante = anneeReference + 1;

		final long dpiId = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				DebiteurPrestationImposable dpi = addDebiteur();

				tiersService.addPeriodicite(dpi, PeriodiciteDecompte.UNIQUE, PeriodeDecompte.M07, date(anneeReference, 3, 1), null);

				addForDebiteur(dpi, date(anneeReference, 1, 1), MotifFor.INDETERMINE, null, null, MockCommune.Bex);

				final PeriodeFiscale periodeFiscale = addPeriodeFiscale(anneeReference);
				addLRPeriodiciteUnique(dpi, date(anneeReference, 7, 1), date(anneeReference, 7, 31), periodeFiscale, TypeEtatDeclaration.EMISE);

				return dpi.getNumero();
			}
		});

		ListeRecapDetailView lrView = lrEditManager.creerLr(dpiId);
		assertNotNull(lrView);
		RegDate dateDebutPeriodeAttendue = RegDate.get(anneeSuivante, 7, 1);
		RegDate dateFinPeriodeAttendue = RegDate.get(anneeSuivante, 7, 31);
		assertEquals(dateDebutPeriodeAttendue, lrView.getRegDateDebutPeriode());
		assertEquals(dateFinPeriodeAttendue, lrView.getRegDateFinPeriode());
	}

	@Test
	public void testLRForPeriodicitesUniqueSansLR() throws Exception {
		//Ajout d'une première periodicite'final int anneeReference = RegDate.get().year();
		final int anneeReference = RegDate.get().year();
		final int anneeSuivante = anneeReference + 1;

		final long dpiId = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				DebiteurPrestationImposable dpi = addDebiteur();

				tiersService.addPeriodicite(dpi, PeriodiciteDecompte.UNIQUE, PeriodeDecompte.M07, date(anneeReference, 3, 1), null);

				addForDebiteur(dpi, date(anneeReference, 1, 1), MotifFor.INDETERMINE, null, null, MockCommune.Bex);

				return dpi.getNumero();
			}
		});

		ListeRecapDetailView lrView = lrEditManager.creerLr(dpiId);
		assertNotNull(lrView);
		RegDate dateDebutPeriodeAttendue = RegDate.get(anneeReference, 7, 1);
		RegDate dateFinPeriodeAttendue = RegDate.get(anneeReference, 7, 31);
		assertEquals(dateDebutPeriodeAttendue, lrView.getRegDateDebutPeriode());
		assertEquals(dateFinPeriodeAttendue, lrView.getRegDateFinPeriode());
	}
}
