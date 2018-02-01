package ch.vd.unireg.evenement.organisation;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.BusinessTest;
import ch.vd.unireg.evenement.organisation.engine.EvenementOrganisationNotificationQueueImpl;
import ch.vd.unireg.stats.StatsServiceImpl;
import ch.vd.unireg.type.EtatEvenementOrganisation;
import ch.vd.unireg.type.TypeEvenementOrganisation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Raphaël Marmier, 2016-11-04, <raphael.marmier@vd.ch>
 */
public class EvenementOrganisationReceptionHandlerTest extends BusinessTest {

	private EvenementOrganisationReceptionHandlerImpl receptionHandler;

	private EvenementOrganisationDAO evtOrganisationDAO;
	private EvenementOrganisationService evtOrganisationService;

	@Before
	public void setUp() {

		evtOrganisationDAO  = getBean(EvenementOrganisationDAO.class, "evenementOrganisationDAO");

		receptionHandler = new EvenementOrganisationReceptionHandlerImpl();
		receptionHandler.setEvtOrganisationDAO(evtOrganisationDAO);
		receptionHandler.setTransactionManager(transactionManager);
		receptionHandler.setStatsService(new StatsServiceImpl());
		receptionHandler.setNotificationQueue(new EvenementOrganisationNotificationQueueImpl(3));
	}

	@Test
	public void testDejaRecu() throws Exception {

		final EvenementOrganisation eventPresentSansBusinessId = createEvent(1L, 100L, TypeEvenementOrganisation.FOSC_NOUVELLE_ENTREPRISE, date(2010, 8, 18), EtatEvenementOrganisation.EN_ERREUR);
		final EvenementOrganisation eventPresent = createEvent(1L, 100L, TypeEvenementOrganisation.FOSC_NOUVELLE_ENTREPRISE, date(2010, 8, 18), EtatEvenementOrganisation.EN_ERREUR);
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

		final EvenementOrganisation event = createEvent(1L, 100L, TypeEvenementOrganisation.FOSC_NOUVELLE_ENTREPRISE, date(2014, 1, 1), EtatEvenementOrganisation.TRAITE);
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

		final EvenementOrganisation event1 = createEvent(1L, 101L, TypeEvenementOrganisation.FOSC_NOUVELLE_ENTREPRISE, date(2015, 8, 18), EtatEvenementOrganisation.EN_ERREUR);
		event1.setBusinessId("biz1_101");
		final EvenementOrganisation event2 = createEvent(2L, 102L, TypeEvenementOrganisation.FOSC_AUTRE_MUTATION, date(2015, 8, 18), EtatEvenementOrganisation.A_TRAITER);
		event2.setBusinessId("biz2_102");
		final EvenementOrganisation event3 = createEvent(3L, 103L, TypeEvenementOrganisation.IDE_MUTATION, date(2015, 8, 18), EtatEvenementOrganisation.A_TRAITER);
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
					final List<EvenementOrganisation> evts = evtOrganisationDAO.getEvenementsForNoEvenement(1L);
					assertEquals(1, evts.size());
					EvenementOrganisation evt = evts.get(0);
					assertEquals(1L, evt.getNoEvenement());
					assertEquals(101L, evt.getNoOrganisation());
					assertEquals("biz1_101", evt.getBusinessId());
					assertEquals(TypeEvenementOrganisation.FOSC_NOUVELLE_ENTREPRISE, evt.getType());
					assertEquals(date(2015, 8, 18), evt.getDateEvenement());
					assertEquals(EtatEvenementOrganisation.EN_ERREUR, evt.getEtat());
				}
				{
					final List<EvenementOrganisation> evts = evtOrganisationDAO.getEvenementsForNoEvenement(2L);
					assertEquals(1, evts.size());
					EvenementOrganisation evt = evts.get(0);
					assertEquals(2L, evt.getNoEvenement());
					assertEquals(102L, evt.getNoOrganisation());
					assertEquals("biz2_102", evt.getBusinessId());
					assertEquals(TypeEvenementOrganisation.FOSC_AUTRE_MUTATION, evt.getType());
					assertEquals(date(2015, 8, 18), evt.getDateEvenement());
					assertEquals(EtatEvenementOrganisation.A_TRAITER, evt.getEtat());
				}
				{
					final List<EvenementOrganisation> evts = evtOrganisationDAO.getEvenementsForNoEvenement(3L);
					assertEquals(1, evts.size());
					EvenementOrganisation evt = evts.get(0);
					assertEquals(3L, evt.getNoEvenement());
					assertEquals(103L, evt.getNoOrganisation());
					assertEquals("biz3_103", evt.getBusinessId());
					assertEquals(TypeEvenementOrganisation.IDE_MUTATION, evt.getType());
					assertEquals(date(2015, 8, 18), evt.getDateEvenement());
					assertEquals(EtatEvenementOrganisation.A_TRAITER, evt.getEtat());
				}

				return null;
			}
		});

	}

	@NotNull
	protected static EvenementOrganisation createEvent(Long noEvenement, Long noOrganisation, TypeEvenementOrganisation type, RegDate date, EtatEvenementOrganisation etat) {
		final EvenementOrganisation event = new EvenementOrganisation();
		event.setNoEvenement(noEvenement);
		event.setNoOrganisation(noOrganisation);
		event.setType(type);
		event.setDateEvenement(date);
		event.setEtat(etat);
		return event;
	}
}