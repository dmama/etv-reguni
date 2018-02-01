package ch.vd.unireg.evenement.organisation.engine;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.NotImplementedException;
import ch.vd.unireg.interfaces.organisation.mock.MockServiceOrganisation;
import ch.vd.unireg.interfaces.organisation.mock.data.builder.MockOrganisationFactory;
import ch.vd.unireg.common.BusinessTest;
import ch.vd.unireg.evenement.organisation.EvenementOrganisation;
import ch.vd.unireg.evenement.organisation.EvenementOrganisationBasicInfo;
import ch.vd.unireg.evenement.organisation.EvenementOrganisationDAO;
import ch.vd.unireg.evenement.organisation.EvenementOrganisationProcessingMode;
import ch.vd.unireg.evenement.organisation.engine.processor.EvenementOrganisationProcessor;
import ch.vd.unireg.type.EtatEvenementOrganisation;
import ch.vd.unireg.type.TypeEvenementOrganisation;

public class EvenementOrganisationRetryProcessorTest extends BusinessTest {

	public EvenementOrganisationRetryProcessorTest() {
		setWantIndexationTiers(true);
	}

	private EvenementOrganisation addEvent(Long noOrganisation, long id, TypeEvenementOrganisation type, RegDate date, EtatEvenementOrganisation etat) {
		final EvenementOrganisation event = new EvenementOrganisation();
		event.setId(id);
		event.setNoOrganisation(noOrganisation);
		event.setType(type);
		event.setDateEvenement(date);
		event.setEtat(etat);
		return hibernateTemplate.merge(event);
	}

	@Test(timeout = 10000L)
	public void testBasics() throws Exception {
		
		final long noOrganisationSans = 17385423L;
		final long noOrganisationAvecAttente = 16745234L;
		final long noOrganisationAvecErreur = 2367485247L;
		final long noOrganisationAvecAttenteEtErreur = 43784236L;

		serviceOrganisation.setUp(new MockServiceOrganisation() {
			@Override
			protected void init() {
				addOrganisation(MockOrganisationFactory.createDummySA(noOrganisationSans, "SansAttente", RegDate.get(2015, 4, 29)));
				addOrganisation(MockOrganisationFactory.createDummySA(noOrganisationAvecAttente, "AvecAttente", RegDate.get(2015, 4, 29)));
				addOrganisation(MockOrganisationFactory.createDummySA(noOrganisationAvecErreur, "AvecErreur", RegDate.get(2015, 4, 29)));
				addOrganisation(MockOrganisationFactory.createDummySA(noOrganisationAvecAttenteEtErreur, "AvecAttente-Erreur", RegDate.get(2015, 4, 29)));
			}
		});

		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				long id = 0L;

				final RegDate date = RegDate.get();

				final TypeEvenementOrganisation type = TypeEvenementOrganisation.FOSC_AUTRE_MUTATION;

				addEvent(noOrganisationSans, ++id, type, date, EtatEvenementOrganisation.A_VERIFIER);
				addEvent(noOrganisationSans, ++id, type, date, EtatEvenementOrganisation.FORCE);
				addEvent(noOrganisationSans, ++id, type, date, EtatEvenementOrganisation.REDONDANT);
				addEvent(noOrganisationSans, ++id, type, date, EtatEvenementOrganisation.TRAITE);

				addEvent(noOrganisationAvecAttente, ++id, type, date, EtatEvenementOrganisation.A_VERIFIER);
				addEvent(noOrganisationAvecAttente, ++id, type, date, EtatEvenementOrganisation.EN_ATTENTE);

				addEvent(noOrganisationAvecErreur, ++id, type, date, EtatEvenementOrganisation.FORCE);
				addEvent(noOrganisationAvecErreur, ++id, type, date, EtatEvenementOrganisation.EN_ERREUR);

				addEvent(noOrganisationAvecAttenteEtErreur, ++id, type, date, EtatEvenementOrganisation.TRAITE);
				addEvent(noOrganisationAvecAttenteEtErreur, ++id, type, date, EtatEvenementOrganisation.EN_ATTENTE);
				addEvent(noOrganisationAvecAttenteEtErreur, ++id, type, date, EtatEvenementOrganisation.EN_ERREUR);


				return null;
			}
		});

		final AtomicInteger pointer = new AtomicInteger(0);
		final Map<Integer, EvenementOrganisationProcessor.Listener> listeners = new HashMap<>();

		final class IntegratedQueueAndProcessor implements EvenementOrganisationProcessor, EvenementOrganisationNotificationQueue {
			@NotNull
			@Override
			public ListenerHandle registerListener(Listener listener) {
				final int id = pointer.incrementAndGet();
				final ListenerHandle handle = new ListenerHandle() {
					@Override
					public void unregister() {
						listeners.remove(id);
					}
				};
				listeners.put(id, listener);
				return handle;
			}

			@Override
			public void restartProcessingThread() {
				throw new NotImplementedException();
			}

			@Override
			public void forceEvenement(EvenementOrganisationBasicInfo evt) {
				throw new UnsupportedOperationException();
			}

			private void notifyTraitement(Long noOrganisation) {
				for (Listener listener : listeners.values()) {
					listener.onOrganisationTraite(noOrganisation);
				}
			}

			@Override
			public void post(Long noOrganisation, EvenementOrganisationProcessingMode mode) {
				// traitement immédiat
				notifyTraitement(noOrganisation);
			}

			@Override
			public void postAll(Collection<Long> nosOrganisations) {
				// traitement immédiat
				for (Long noOrganisation : nosOrganisations) {
					notifyTraitement(noOrganisation);
				}
			}

			@Override
			public Batch poll(Duration timeout) throws InterruptedException {
				throw new NotImplementedException();
			}

			@Override
			public int getTotalCount() {
				throw new NotImplementedException();
			}

			@Override
			public int getInBulkQueueCount() {
				throw new NotImplementedException();
			}

			@Override
			public Long getBulkQueueSlidingAverageAge() {
				throw new NotImplementedException();
			}

			@Override
			public Long getBulkQueueGlobalAverageAge() {
				throw new NotImplementedException();
			}

			@Override
			public int getInPriorityQueueCount() {
				throw new NotImplementedException();
			}

			@Override
			public Long getPriorityQueueSlidingAverageAge() {
				throw new NotImplementedException();
			}

			@Override
			public Long getPriorityQueueGlobalAverageAge() {
				throw new NotImplementedException();
			}

			@Override
			public int getInImmediateQueueCount() {
				throw new NotImplementedException();
			}

			@Override
			public Long getImmediateQueueSlidingAverageAge() {
				throw new NotImplementedException();
			}

			@Override
			public Long getImmediateQueueGlobalAverageAge() {
				throw new NotImplementedException();
			}

			@Override
			public int getInFinalQueueCount() {
				throw new NotImplementedException();
			}

			@Override
			public int getInHatchesCount() {
				throw new NotImplementedException();
			}
		}

		final IntegratedQueueAndProcessor queueProcessor = new IntegratedQueueAndProcessor();
		final EvenementOrganisationRetryProcessorImpl retry = new EvenementOrganisationRetryProcessorImpl();
		retry.setEvtOrganisationDAO(getBean(EvenementOrganisationDAO.class, "evenementOrganisationDAO"));
		retry.setTransactionManager(transactionManager);
		retry.setProcessor(queueProcessor);
		retry.setNotificationQueue(queueProcessor);
		
		// et maintenant : le test !
		
		// Ca, c'est pour vérifier que l'on traite les bons individus
		final Set<Long> remaining = new HashSet<>(Arrays.asList(noOrganisationSans, noOrganisationAvecAttente, noOrganisationAvecErreur, noOrganisationAvecAttenteEtErreur));
		final EvenementOrganisationProcessor.ListenerHandle handleRemaining = queueProcessor.registerListener(new EvenementOrganisationProcessor.Listener() {
			@Override
			public void onOrganisationTraite(long noOrganisation) {
				remaining.remove(noOrganisation);
			}

			@Override
			public void onStop() {
			}
		});

		// lancement des travaux
		try {
			retry.retraiteEvenements(null);
		}
		finally {
			handleRemaining.unregister();
		}
		
		// au final : il ne doit plus rester que le noOrganisationSans dans la liste remaining, et tous les listeners doivent avoir été dés-enregistrés
		Assert.assertEquals(1, remaining.size());
		Assert.assertEquals((Long) noOrganisationSans, remaining.iterator().next());
		Assert.assertEquals(0, listeners.size());
	}
}
