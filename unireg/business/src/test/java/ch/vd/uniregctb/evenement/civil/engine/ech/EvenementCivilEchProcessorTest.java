package ch.vd.uniregctb.evenement.civil.engine.ech;

import java.util.Set;

import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEch;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEchErreur;
import ch.vd.uniregctb.evenement.civil.interne.testing.Testing;
import ch.vd.uniregctb.interfaces.service.mock.DefaultMockServiceCivil;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.ActionEvenementCivilEch;
import ch.vd.uniregctb.type.EtatEvenementCivil;
import ch.vd.uniregctb.type.TypeEvenementCivilEch;
import ch.vd.uniregctb.type.TypeEvenementErreur;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class EvenementCivilEchProcessorTest extends AbstractEvenementCivilEchProcessorTest {

	@Test(timeout = 10000L)
	public void testEvenementExceptionDansHandle() throws Exception {

		traiterEvenementTesting(Testing.NoExceptionDansHandle, new AfterHandleCallback() {
			@Override
			public void checkEvent(EvenementCivilEch evt) {
				assertEquals(EtatEvenementCivil.EN_ERREUR, evt.getEtat());

				final Set<EvenementCivilEchErreur> erreurs = evt.getErreurs();
				assertNotNull(erreurs);
				assertEquals(1, erreurs.size());

				final EvenementCivilEchErreur erreur0 = erreurs.iterator().next();
				assertNotNull(erreur0);
				assertEquals(TypeEvenementErreur.ERROR, erreur0.getType());
				assertEquals("Exception de test", erreur0.getMessage());
			}
		});
	}

	@Test(timeout = 10000L)
	public void testEvenementTraiteAvecWarningDansHandle() throws Exception {

		traiterEvenementTesting(Testing.NoTraiteAvecWarningDansHandle, new AfterHandleCallback() {
			@Override
			public void checkEvent(EvenementCivilEch evt) {
				assertEquals(EtatEvenementCivil.A_VERIFIER, evt.getEtat());

				final Set<EvenementCivilEchErreur> erreurs = evt.getErreurs();
				assertNotNull(erreurs);
				assertEquals(1, erreurs.size());

				final EvenementCivilEchErreur erreur0 = erreurs.iterator().next();
				assertNotNull(erreur0);
				assertEquals(TypeEvenementErreur.WARNING, erreur0.getType());
				assertEquals("Warning de test", erreur0.getMessage());
			}
		});
	}

	@Test(timeout = 10000L)
	public void testEvenementRedondantAvecWarningDansHandle() throws Exception {

		traiterEvenementTesting(Testing.NoRedondantAvecWarningDansHandle, new AfterHandleCallback() {
			@Override
			public void checkEvent(EvenementCivilEch evt) {
				assertEquals(EtatEvenementCivil.A_VERIFIER, evt.getEtat());

				final Set<EvenementCivilEchErreur> erreurs = evt.getErreurs();
				assertNotNull(erreurs);
				assertEquals(1, erreurs.size());

				final EvenementCivilEchErreur erreur0 = erreurs.iterator().next();
				assertNotNull(erreur0);
				assertEquals(TypeEvenementErreur.WARNING, erreur0.getType());
				assertEquals("Warning de test", erreur0.getMessage());
			}
		});
	}

	@Test(timeout = 10000L)
	public void testEvenementTraiteSansWarningDansHandle() throws Exception {

		traiterEvenementTesting(Testing.NoTraiteSansWarning, new AfterHandleCallback() {
			@Override
			public void checkEvent(EvenementCivilEch evt) {
				assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat());
				assertEmpty(evt.getErreurs());
			}
		});
	}

	@Test(timeout = 10000L)
	public void testEvenementRedondantSansWarningDansHandle() throws Exception {

		traiterEvenementTesting(Testing.NoRedondantSansWarning, new AfterHandleCallback() {
			@Override
			public void checkEvent(EvenementCivilEch evt) {
				assertEquals(EtatEvenementCivil.REDONDANT, evt.getEtat());
				assertEmpty(evt.getErreurs());
			}
		});
	}

	private interface AfterHandleCallback {
		void checkEvent(EvenementCivilEch evt);
	}

	private void traiterEvenementTesting(final long noEventTesting, final AfterHandleCallback afterHandleCallback) throws Exception {
		final long noIndividu = 12345L;

		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
				addIndividu(noIndividu, date(1982, 1, 2), "Anoa", "Yamomoto", true);
			}
		});

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final PersonnePhysique hab = new PersonnePhysique(true);
				hab.setNumeroIndividu(noIndividu);
				tiersDAO.save(hab);
				return null;
			}
		});

		// événement civil (avec individu déjà renseigné pour ne pas devoir appeler RCPers...)
		final long testingId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = new EvenementCivilEch();
				evt.setId(noEventTesting);
				evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
				evt.setDateEvenement(date(2000, 1, 1));
				evt.setEtat(EtatEvenementCivil.A_TRAITER);
				evt.setNumeroIndividu(noIndividu);
				evt.setType(TypeEvenementCivilEch.TESTING);
				return hibernateTemplate.merge(evt).getId();
			}
		});

		// traitement synchrone de l'événement
		traiterEvenement(noIndividu, testingId);

		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = evtCivilDAO.get(testingId);
				assertNotNull(evt);
				afterHandleCallback.checkEvent(evt);
				return null;
			}
		});
	}
}
