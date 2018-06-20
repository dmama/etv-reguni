package ch.vd.unireg.evenement.entreprise.engine;

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
import ch.vd.unireg.common.BusinessTest;
import ch.vd.unireg.evenement.entreprise.EvenementEntreprise;
import ch.vd.unireg.evenement.entreprise.EvenementEntrepriseBasicInfo;
import ch.vd.unireg.evenement.entreprise.EvenementEntrepriseDAO;
import ch.vd.unireg.evenement.entreprise.EvenementEntrepriseProcessingMode;
import ch.vd.unireg.evenement.entreprise.engine.processor.EvenementEntrepriseProcessor;
import ch.vd.unireg.interfaces.entreprise.mock.MockServiceEntreprise;
import ch.vd.unireg.interfaces.entreprise.mock.data.builder.MockEntrepriseFactory;
import ch.vd.unireg.type.EtatEvenementEntreprise;
import ch.vd.unireg.type.TypeEvenementEntreprise;

public class EvenementEntrepriseCivileRetryProcessorTest extends BusinessTest {

	public EvenementEntrepriseCivileRetryProcessorTest() {
		setWantIndexationTiers(true);
	}

	private EvenementEntreprise addEvent(Long noEntrepriseCivile, long id, TypeEvenementEntreprise type, RegDate date, EtatEvenementEntreprise etat) {
		final EvenementEntreprise event = new EvenementEntreprise();
		event.setId(id);
		event.setNoEntrepriseCivile(noEntrepriseCivile);
		event.setType(type);
		event.setDateEvenement(date);
		event.setEtat(etat);
		return hibernateTemplate.merge(event);
	}

	@Test(timeout = 10000L)
	public void testBasics() throws Exception {
		
		final long noEntrepriseSans = 17385423L;
		final long noEntrepriseAvecAttente = 16745234L;
		final long noEntrepriseAvecErreur = 2367485247L;
		final long noEntrepriseAvecAttenteEtErreur = 43784236L;

		serviceEntreprise.setUp(new MockServiceEntreprise() {
			@Override
			protected void init() {
				addEntreprise(MockEntrepriseFactory.createDummySA(noEntrepriseSans, "SansAttente", RegDate.get(2015, 4, 29)));
				addEntreprise(MockEntrepriseFactory.createDummySA(noEntrepriseAvecAttente, "AvecAttente", RegDate.get(2015, 4, 29)));
				addEntreprise(MockEntrepriseFactory.createDummySA(noEntrepriseAvecErreur, "AvecErreur", RegDate.get(2015, 4, 29)));
				addEntreprise(MockEntrepriseFactory.createDummySA(noEntrepriseAvecAttenteEtErreur, "AvecAttente-Erreur", RegDate.get(2015, 4, 29)));
			}
		});

		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				long id = 0L;

				final RegDate date = RegDate.get();

				final TypeEvenementEntreprise type = TypeEvenementEntreprise.FOSC_AUTRE_MUTATION;

				addEvent(noEntrepriseSans, ++id, type, date, EtatEvenementEntreprise.A_VERIFIER);
				addEvent(noEntrepriseSans, ++id, type, date, EtatEvenementEntreprise.FORCE);
				addEvent(noEntrepriseSans, ++id, type, date, EtatEvenementEntreprise.REDONDANT);
				addEvent(noEntrepriseSans, ++id, type, date, EtatEvenementEntreprise.TRAITE);

				addEvent(noEntrepriseAvecAttente, ++id, type, date, EtatEvenementEntreprise.A_VERIFIER);
				addEvent(noEntrepriseAvecAttente, ++id, type, date, EtatEvenementEntreprise.EN_ATTENTE);

				addEvent(noEntrepriseAvecErreur, ++id, type, date, EtatEvenementEntreprise.FORCE);
				addEvent(noEntrepriseAvecErreur, ++id, type, date, EtatEvenementEntreprise.EN_ERREUR);

				addEvent(noEntrepriseAvecAttenteEtErreur, ++id, type, date, EtatEvenementEntreprise.TRAITE);
				addEvent(noEntrepriseAvecAttenteEtErreur, ++id, type, date, EtatEvenementEntreprise.EN_ATTENTE);
				addEvent(noEntrepriseAvecAttenteEtErreur, ++id, type, date, EtatEvenementEntreprise.EN_ERREUR);


				return null;
			}
		});

		final AtomicInteger pointer = new AtomicInteger(0);
		final Map<Integer, EvenementEntrepriseProcessor.Listener> listeners = new HashMap<>();

		final class IntegratedQueueAndProcessor implements EvenementEntrepriseProcessor, EvenementEntrepriseNotificationQueue {
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
			public void forceEvenement(EvenementEntrepriseBasicInfo evt) {
				throw new UnsupportedOperationException();
			}

			private void notifyTraitement(Long noEntreprise) {
				for (Listener listener : listeners.values()) {
					listener.onEntrepriseTraitee(noEntreprise);
				}
			}

			@Override
			public void post(Long noEntrepriseCivile, EvenementEntrepriseProcessingMode mode) {
				// traitement immédiat
				notifyTraitement(noEntrepriseCivile);
			}

			@Override
			public void postAll(Collection<Long> nosEntreprisesCiviles) {
				// traitement immédiat
				for (Long no : nosEntreprisesCiviles) {
					notifyTraitement(no);
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
		final EvenementEntrepriseRetryProcessorImpl retry = new EvenementEntrepriseRetryProcessorImpl();
		retry.setEvtEntrepriseDAO(getBean(EvenementEntrepriseDAO.class, "evenementEntrepriseDAO"));
		retry.setTransactionManager(transactionManager);
		retry.setProcessor(queueProcessor);
		retry.setNotificationQueue(queueProcessor);
		
		// et maintenant : le test !
		
		// Ca, c'est pour vérifier que l'on traite les bons individus
		final Set<Long> remaining = new HashSet<>(Arrays.asList(noEntrepriseSans, noEntrepriseAvecAttente, noEntrepriseAvecErreur, noEntrepriseAvecAttenteEtErreur));
		final EvenementEntrepriseProcessor.ListenerHandle handleRemaining = queueProcessor.registerListener(new EvenementEntrepriseProcessor.Listener() {
			@Override
			public void onEntrepriseTraitee(long noEntrepriseCivile) {
				remaining.remove(noEntrepriseCivile);
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
		
		// au final : il ne doit plus rester que le noEntrepriseSans dans la liste remaining, et tous les listeners doivent avoir été dés-enregistrés
		Assert.assertEquals(1, remaining.size());
		Assert.assertEquals((Long) noEntrepriseSans, remaining.iterator().next());
		Assert.assertEquals(0, listeners.size());
	}
}
