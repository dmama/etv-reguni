package ch.vd.unireg.evenement.entreprise;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.BusinessTest;
import ch.vd.unireg.evenement.entreprise.engine.EvenementEntrepriseNotificationQueueImpl;
import ch.vd.unireg.stats.StatsServiceImpl;
import ch.vd.unireg.type.EtatEvenementEntreprise;
import ch.vd.unireg.type.TypeEvenementEntreprise;

import static ch.vd.unireg.evenement.entreprise.interne.demenagement.DemenagementSiegeStrategyTest.getEvenementEntreprise;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Raphaël Marmier, 2016-11-04, <raphael.marmier@vd.ch>
 */
public class EvenementEntrepriseCivileReceptionHandlerTest extends BusinessTest {

	private EvenementEntrepriseReceptionHandlerImpl receptionHandler;

	private EvenementEntrepriseDAO evtEntrepriseDAO;
	private EvenementEntrepriseService evtEntrepriseService;

	@Before
	public void setUp() {

		evtEntrepriseDAO = getBean(EvenementEntrepriseDAO.class, "evenementEntrepriseDAO");

		receptionHandler = new EvenementEntrepriseReceptionHandlerImpl();
		receptionHandler.setEvtEntrepriseDAO(evtEntrepriseDAO);
		receptionHandler.setTransactionManager(transactionManager);
		receptionHandler.setStatsService(new StatsServiceImpl());
		receptionHandler.setNotificationQueue(new EvenementEntrepriseNotificationQueueImpl(3));
	}

	@Test
	public void testDejaRecu() throws Exception {

		final EvenementEntreprise eventPresentSansBusinessId = createEvent(1L, 100L, TypeEvenementEntreprise.FOSC_NOUVELLE_ENTREPRISE, date(2010, 8, 18), EtatEvenementEntreprise.EN_ERREUR);
		final EvenementEntreprise eventPresent = createEvent(1L, 100L, TypeEvenementEntreprise.FOSC_NOUVELLE_ENTREPRISE, date(2010, 8, 18), EtatEvenementEntreprise.EN_ERREUR);
		eventPresent.setBusinessId("nimporteBizId");


		// Persistence événement
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {

			@Override
			public void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
				hibernateTemplate.merge(eventPresentSansBusinessId);
				hibernateTemplate.merge(eventPresent);
			}
		});

		doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus transactionStatus) {

				assertFalse(receptionHandler.dejaRecu("maBizId"));

				return null;
			}
		});

		final EvenementEntreprise event = createEvent(1L, 100L, TypeEvenementEntreprise.FOSC_NOUVELLE_ENTREPRISE, date(2014, 1, 1), EtatEvenementEntreprise.TRAITE);
		event.setBusinessId("maBizId");

		// Persistence événement
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {

			@Override
			public void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
				hibernateTemplate.merge(event);
			}
		});

		doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus transactionStatus) {

				assertTrue(receptionHandler.dejaRecu("maBizId"));

				return null;
			}
		});
	}

	@Test
	public void testSaveIncomingEvent() throws Exception {

		// Création de l'événement

		final EvenementEntreprise event1 = createEvent(1L, 101L, TypeEvenementEntreprise.FOSC_NOUVELLE_ENTREPRISE, date(2015, 8, 18), EtatEvenementEntreprise.EN_ERREUR);
		event1.setBusinessId("biz1_101");
		final EvenementEntreprise event2 = createEvent(2L, 102L, TypeEvenementEntreprise.FOSC_AUTRE_MUTATION, date(2015, 8, 18), EtatEvenementEntreprise.A_TRAITER);
		event2.setBusinessId("biz2_102");
		final EvenementEntreprise event3 = createEvent(3L, 103L, TypeEvenementEntreprise.IDE_MUTATION, date(2015, 8, 18), EtatEvenementEntreprise.A_TRAITER);
		event3.setBusinessId("biz3_103");

		// Persistence événement
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {

			@Override
			public void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
				hibernateTemplate.merge(event1);
				hibernateTemplate.merge(event2);
				hibernateTemplate.merge(event3);
			}
		});

		doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus transactionStatus) {

				{
					final List<EvenementEntreprise> evts = evtEntrepriseDAO.getEvenementsForNoEvenement(1L);
					assertEquals(1, evts.size());
					EvenementEntreprise evt = evts.get(0);
					assertEquals(1L, evt.getNoEvenement());
					assertEquals(101L, evt.getNoEntrepriseCivile());
					assertEquals("biz1_101", evt.getBusinessId());
					assertEquals(TypeEvenementEntreprise.FOSC_NOUVELLE_ENTREPRISE, evt.getType());
					assertEquals(date(2015, 8, 18), evt.getDateEvenement());
					assertEquals(EtatEvenementEntreprise.EN_ERREUR, evt.getEtat());
				}
				{
					final List<EvenementEntreprise> evts = evtEntrepriseDAO.getEvenementsForNoEvenement(2L);
					assertEquals(1, evts.size());
					EvenementEntreprise evt = evts.get(0);
					assertEquals(2L, evt.getNoEvenement());
					assertEquals(102L, evt.getNoEntrepriseCivile());
					assertEquals("biz2_102", evt.getBusinessId());
					assertEquals(TypeEvenementEntreprise.FOSC_AUTRE_MUTATION, evt.getType());
					assertEquals(date(2015, 8, 18), evt.getDateEvenement());
					assertEquals(EtatEvenementEntreprise.A_TRAITER, evt.getEtat());
				}
				{
					final List<EvenementEntreprise> evts = evtEntrepriseDAO.getEvenementsForNoEvenement(3L);
					assertEquals(1, evts.size());
					EvenementEntreprise evt = evts.get(0);
					assertEquals(3L, evt.getNoEvenement());
					assertEquals(103L, evt.getNoEntrepriseCivile());
					assertEquals("biz3_103", evt.getBusinessId());
					assertEquals(TypeEvenementEntreprise.IDE_MUTATION, evt.getType());
					assertEquals(date(2015, 8, 18), evt.getDateEvenement());
					assertEquals(EtatEvenementEntreprise.A_TRAITER, evt.getEtat());
				}

				return null;
			}
		});

	}

	@NotNull
	protected static EvenementEntreprise createEvent(Long noEvenement, Long noEntrepriseCivile, TypeEvenementEntreprise type, RegDate date, EtatEvenementEntreprise etat) {
		final EvenementEntreprise event = new EvenementEntreprise();
		event.setNoEvenement(noEvenement);
		event.setNoEntrepriseCivile(noEntrepriseCivile);
		event.setType(type);
		event.setDateEvenement(date);
		event.setEtat(etat);
		return event;
	}
}