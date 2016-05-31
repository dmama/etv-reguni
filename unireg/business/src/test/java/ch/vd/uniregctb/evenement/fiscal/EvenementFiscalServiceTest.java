package ch.vd.uniregctb.evenement.fiscal;

import java.util.List;

import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.declaration.Declaration;
import ch.vd.uniregctb.declaration.DeclarationImpotSource;
import ch.vd.uniregctb.declaration.PeriodeFiscale;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.type.CategorieImpotSource;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.PeriodiciteDecompte;
import ch.vd.uniregctb.type.Sexe;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class EvenementFiscalServiceTest extends BusinessTest {

	private EvenementFiscalService evenementFiscalService;
	private MockEvenementFiscalSender evenementFiscalSender;
	private EvenementFiscalDAO evenementFiscalDAO;


	private TiersDAO tiersDAO;

	public EvenementFiscalServiceTest() {
		super();
	}

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		evenementFiscalService = getBean(EvenementFiscalService.class, "evenementFiscalService");
		evenementFiscalDAO = getBean(EvenementFiscalDAO.class, "evenementFiscalDAO");
	    evenementFiscalSender = getBean(MockEvenementFiscalSender.class, "evenementFiscalSender");
		tiersDAO = getBean( TiersDAO.class, "tiersDAO");
	}

	@Test
	public void testPublierEvenementFor() throws Exception {

		// mise en place fiscale
		final long id = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				evenementFiscalSender.count = 0;
				assertEquals(0, evenementFiscalDAO.getAll().size());

				final PersonnePhysique pp = addNonHabitant("Laurent", "Schmidt", date(1970, 5, 27), Sexe.MASCULIN);
				return pp.getNumero();
			}
		});

		// création d'un for et envoi d'un événement
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(id);
				assertNotNull(pp);
				final ForFiscalPrincipal ffp = addForPrincipal(pp, RegDate.get().addDays(-5), MotifFor.ARRIVEE_HS, MockCommune.Lausanne);
				evenementFiscalService.publierEvenementFiscalOuvertureFor(ffp);
			}
		});

		// Vérifie que l'événement a été envoyé
		assertEquals(1, evenementFiscalSender.count);

		// Vérifie que l'événement est dans la base
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final List<EvenementFiscal> events = evenementFiscalDAO.getAll();
				assertEquals(1, events.size());
				assertForEvent(id, RegDate.get().addDays(-5), EvenementFiscalFor.TypeEvenementFiscalFor.OUVERTURE, events.get(0));
			}
		});
	}

	@Test
	public void publierEvenementFiscalRetourLR() throws Exception {

		// mise en place fiscale
		final long id = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				evenementFiscalSender.count = 0;
				assertEquals(0, evenementFiscalDAO.getAll().size());

				// le DPI
				final DebiteurPrestationImposable debiteur = addDebiteur(CategorieImpotSource.ADMINISTRATEURS, PeriodiciteDecompte.ANNUEL, date(2009, 1, 1));
				return debiteur.getNumero();
			}
		});

		// création d'une LR et publication d'un événement correspondant
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersDAO.get(id);
				assertNotNull(dpi);
				addForDebiteur(dpi, date(2009, 1, 1), MotifFor.DEBUT_PRESTATION_IS, date(2009, 12, 31), MotifFor.CESSATION_ACTIVITE_FUSION_FAILLITE, MockCommune.Lausanne);

				final PeriodeFiscale pf = addPeriodeFiscale(2009);
				final DeclarationImpotSource lr = addLR(dpi, date(2009, 1, 1), PeriodiciteDecompte.ANNUEL, pf);
				evenementFiscalService.publierEvenementFiscalEmissionListeRecapitulative(lr, RegDate.get().addDays(-2));
			}
		});

		// Vérifie que l'événement a été envoyé
		assertEquals(1, evenementFiscalSender.count);

		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				// Vérifie que l'événement est dans la base
				final List<EvenementFiscal> events = evenementFiscalDAO.getAll();
				assertEquals(1, events.size());
				assertDeclarationEvent(id, RegDate.get().addDays(-2), EvenementFiscalDeclaration.TypeAction.EMISSION, date(2009, 1, 1), date(2009, 12, 31), DeclarationImpotSource.class, events.get(0));
			}
		});
	}

	@Test
	public void publierEvenementFiscalChangementSituation() throws Exception {

		// mise en place
		final long id = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				evenementFiscalSender.count = 0;
				assertEquals(0, evenementFiscalDAO.getAll().size());

				final PersonnePhysique pp = addNonHabitant("Laurent", "Schmidt", date(1970, 4, 2), Sexe.MASCULIN);
				evenementFiscalService.publierEvenementFiscalChangementSituationFamille(RegDate.get().addDays(-3), pp);
				return pp.getNumero();
			}
		});

		// Vérifie que l'événement a été envoyé
		assertEquals(1, evenementFiscalSender.count);

		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				// Vérifie que l'événement est dans la base
				final List<EvenementFiscal> events = evenementFiscalDAO.getAll();
				assertEquals(1, events.size());
				assertCSFEvent(id, RegDate.get().addDays(-3), events.get(0));
			}
		});
	}

	private static void assertCSFEvent(Long tiersId, RegDate dateEvenement, EvenementFiscal event) {
		assertEvent(tiersId, dateEvenement, EvenementFiscalSituationFamille.class, event);
	}

	private static void assertForEvent(Long tiersId, RegDate dateEvenement, EvenementFiscalFor.TypeEvenementFiscalFor type, EvenementFiscal event) {
		assertEvent(tiersId, dateEvenement, EvenementFiscalFor.class, event);
		final EvenementFiscalFor forEvent = (EvenementFiscalFor) event;
		assertEquals(type, forEvent.getType());
		assertNotNull(forEvent.getForFiscal());
	}

	private static void assertDeclarationEvent(Long tiersId,
	                                           RegDate dateEvenement,
	                                           EvenementFiscalDeclaration.TypeAction type,
	                                           RegDate dateDebutPeriode,
	                                           RegDate dateFinPeriode,
	                                           Class<? extends Declaration> expectedDeclarationClass,
	                                           EvenementFiscal event) {
		assertEvent(tiersId, dateEvenement, EvenementFiscalDeclaration.class, event);

		final EvenementFiscalDeclaration declaEvent = (EvenementFiscalDeclaration) event;
		assertEquals(type, declaEvent.getTypeAction());

		final Declaration declaration = declaEvent.getDeclaration();
		assertInstanceOf(expectedDeclarationClass, declaration);
		assertEquals(dateDebutPeriode, declaration.getDateDebut());
		assertEquals(dateFinPeriode, declaration.getDateFin());
	}

	private static void assertEvent(Long tiersId, RegDate dateEvenement, Class<? extends EvenementFiscal> expectedClass, EvenementFiscal event0) {
		assertNotNull(event0);
		assertEquals(tiersId, event0.getTiers().getNumero());
		assertEquals(dateEvenement, event0.getDateValeur());
		assertEquals(expectedClass, event0.getClass());
	}
}
