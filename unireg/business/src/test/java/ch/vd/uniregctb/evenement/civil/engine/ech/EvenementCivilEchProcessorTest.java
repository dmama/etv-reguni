package ch.vd.uniregctb.evenement.civil.engine.ech;

import java.util.Set;

import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEch;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEchErreur;
import ch.vd.uniregctb.evenement.civil.interne.testing.Testing;
import ch.vd.uniregctb.interfaces.service.mock.DefaultMockServiceCivil;
import ch.vd.uniregctb.interfaces.service.mock.MockServiceCivil;
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
	
	@Test(timeout = 10000L)
	public void testTraitementEvenementIndexationPureSiErreur() throws Exception {
		
		final long noIndividu = 316547256L;
		
		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				addIndividu(noIndividu, date(1976, 8, 22), "Kaderate", "Yamamoto", true); 
			}
		});
		
		// aucune mise en place fiscale -> le souci lors du traitement du mariage, ce sera justement que le tiers n'existe pas...
		
		// création des événements sur cet individu : 1 événement qui part en erreur et un événement d'indexation pure à une date postérieure (ce dernier doit être traité quand-même)
		final long evtErreurId = 32657743L;
		final long evtIndexationId = 423677342L;
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {

				final RegDate dateMariage = date(2000, 1, 1);
				
				// événement qui partira en erreur
				final EvenementCivilEch evtErreur = new EvenementCivilEch();
				evtErreur.setId(evtErreurId);
				evtErreur.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
				evtErreur.setDateEvenement(dateMariage);
				evtErreur.setEtat(EtatEvenementCivil.A_TRAITER);
				evtErreur.setNumeroIndividu(noIndividu);
				evtErreur.setType(TypeEvenementCivilEch.MARIAGE);
				hibernateTemplate.merge(evtErreur);
				
				// événement d'indexation pure (obtention d'origine)
				final EvenementCivilEch evtIndexation = new EvenementCivilEch();
				evtIndexation.setId(evtIndexationId);
				evtIndexation.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
				evtIndexation.setDateEvenement(dateMariage.addYears(1));              // après celui qui partira en erreur, donc
				evtIndexation.setEtat(EtatEvenementCivil.A_TRAITER);
				evtIndexation.setNumeroIndividu(noIndividu);
				evtIndexation.setType(TypeEvenementCivilEch.OBENTION_DROIT_CITE);
				hibernateTemplate.merge(evtIndexation);
				return null;
			}
		});

		// traitement de l'événement, passage en erreur et traitement de l'indexation pure quand-même (si tout va bien...)
		traiterEvenement(noIndividu, evtErreurId);
		
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evtErreur = evtCivilDAO.get(evtErreurId);
				assertNotNull(evtErreur);
				assertEquals(EtatEvenementCivil.EN_ERREUR, evtErreur.getEtat());

				final EvenementCivilEch evtIndexation = evtCivilDAO.get(evtIndexationId);
				assertNotNull(evtIndexation);
				assertEquals(EtatEvenementCivil.TRAITE, evtIndexation.getEtat());
				return null;
			}
		});
	}

	@Test(timeout = 10000L)
	public void testMiseEnAttente() throws Exception {

		final long noIndividu = 316547256L;

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				addIndividu(noIndividu, date(1976, 8, 22), "Kaderate", "Yamamoto", true);
			}
		});

		// aucune mise en place fiscale -> le souci lors du traitement du mariage, ce sera justement que le tiers n'existe pas...

		// création des événements sur cet individu : 1 événement qui part en erreur et un événement d'indexation pure à une date postérieure (ce dernier doit être traité quand-même)
		final long evtErreurId = 32657743L;
		final long evtAttenteId = 423677342L;
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {

				final RegDate dateMariage = date(2000, 1, 1);

				// événement qui partira en erreur
				final EvenementCivilEch evtErreur = new EvenementCivilEch();
				evtErreur.setId(evtErreurId);
				evtErreur.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
				evtErreur.setDateEvenement(dateMariage);
				evtErreur.setEtat(EtatEvenementCivil.A_TRAITER);
				evtErreur.setNumeroIndividu(noIndividu);
				evtErreur.setType(TypeEvenementCivilEch.MARIAGE);
				hibernateTemplate.merge(evtErreur);

				// événement qui sera mis en attente
				final EvenementCivilEch evtAttente = new EvenementCivilEch();
				evtAttente.setId(evtAttenteId);
				evtAttente.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
				evtAttente.setDateEvenement(dateMariage.addYears(1));              // après celui qui partira en erreur, donc
				evtAttente.setEtat(EtatEvenementCivil.A_TRAITER);
				evtAttente.setNumeroIndividu(noIndividu);
				evtAttente.setType(TypeEvenementCivilEch.DIVORCE);
				hibernateTemplate.merge(evtAttente);
				return null;
			}
		});

		// traitement de l'événement, passage en erreur et traitement de l'indexation pure quand-même (si tout va bien...)
		traiterEvenement(noIndividu, evtErreurId);

		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evtErreur = evtCivilDAO.get(evtErreurId);
				assertNotNull(evtErreur);
				assertEquals(EtatEvenementCivil.EN_ERREUR, evtErreur.getEtat());

				final EvenementCivilEch evtIndexation = evtCivilDAO.get(evtAttenteId);
				assertNotNull(evtIndexation);
				assertEquals(EtatEvenementCivil.EN_ATTENTE, evtIndexation.getEtat());
				return null;
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
				addHabitant(noIndividu);
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
