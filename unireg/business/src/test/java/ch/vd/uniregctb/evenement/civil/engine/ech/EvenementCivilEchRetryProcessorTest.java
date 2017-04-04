package ch.vd.uniregctb.evenement.civil.engine.ech;

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
import ch.vd.unireg.interfaces.civil.mock.MockServiceCivil;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEch;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEchDAO;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEchProcessingMode;
import ch.vd.uniregctb.evenement.civil.ech.MockEvenementCivilEchRecuperateur;
import ch.vd.uniregctb.type.ActionEvenementCivilEch;
import ch.vd.uniregctb.type.EtatEvenementCivil;
import ch.vd.uniregctb.type.TypeEvenementCivilEch;

public class EvenementCivilEchRetryProcessorTest extends BusinessTest {

	private EvenementCivilEch addEvent(long noIndividu, long id, TypeEvenementCivilEch type, ActionEvenementCivilEch action, RegDate date, EtatEvenementCivil etat) {
		final EvenementCivilEch event = new EvenementCivilEch();
		event.setId(id);
		event.setNumeroIndividu(noIndividu);
		event.setType(type);
		event.setAction(action);
		event.setDateEvenement(date);
		event.setEtat(etat);
		return hibernateTemplate.merge(event);
	}
	
	@Test(timeout = 10000L)
	public void testBasics() throws Exception {
		
		final long noIndividuSans = 17385423L;
		final long noIndividuAvecAttente = 16745234L;
		final long noIndividuAvecErreur = 2367485247L;
		final long noIndividuAvecAttenteEtErreur = 43784236L;
		
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				addIndividu(noIndividuSans, null, "Sans", "Rien", true);
				addIndividu(noIndividuAvecAttente, null, "Avec", "Attente", false);
				addIndividu(noIndividuAvecErreur, null, "Avec", "Erreur", false);
				addIndividu(noIndividuAvecAttenteEtErreur, null, "Avec", "Attente-Erreur", false);
			}
		});
		
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				long id = 0L;

				final RegDate date = RegDate.get();
				final TypeEvenementCivilEch type = TypeEvenementCivilEch.TESTING;
				final ActionEvenementCivilEch action = ActionEvenementCivilEch.PREMIERE_LIVRAISON;

				addEvent(noIndividuSans, ++id, type, action, date, EtatEvenementCivil.A_VERIFIER);
				addEvent(noIndividuSans, ++id, type, action, date, EtatEvenementCivil.FORCE);
				addEvent(noIndividuSans, ++id, type, action, date, EtatEvenementCivil.REDONDANT);
				addEvent(noIndividuSans, ++id, type, action, date, EtatEvenementCivil.TRAITE);

				addEvent(noIndividuAvecAttente, ++id, type, action, date, EtatEvenementCivil.A_VERIFIER);
				addEvent(noIndividuAvecAttente, ++id, type, action, date, EtatEvenementCivil.EN_ATTENTE);

				addEvent(noIndividuAvecErreur, ++id, type, action, date, EtatEvenementCivil.FORCE);
				addEvent(noIndividuAvecErreur, ++id, type, action, date, EtatEvenementCivil.EN_ERREUR);

				addEvent(noIndividuAvecAttenteEtErreur, ++id, type, action, date, EtatEvenementCivil.TRAITE);
				addEvent(noIndividuAvecAttenteEtErreur, ++id, type, action, date, EtatEvenementCivil.EN_ATTENTE);
				addEvent(noIndividuAvecAttenteEtErreur, ++id, type, action, date, EtatEvenementCivil.EN_ERREUR);

				return null;
			}
		});
		
		final AtomicInteger pointer = new AtomicInteger(0);
		final Map<Integer, EvenementCivilEchProcessor.Listener> listeners = new HashMap<>();

		final class IntegratedQueueAndProcessor implements EvenementCivilEchProcessor, EvenementCivilNotificationQueue {
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
			public void restartProcessingThread(boolean agressiveKill) {
				throw new NotImplementedException();
			}

			private void notifyTraitement(Long noIndividu) {
				for (Listener listener : listeners.values()) {
					listener.onIndividuTraite(noIndividu);
				}
			}

			@Override
			public void post(Long noIndividu, EvenementCivilEchProcessingMode mode) {
				// traitement immédiat
				notifyTraitement(noIndividu);
			}

			@Override
			public void postAll(Collection<Long> nosIndividus) {
				// traitement immédiat
				for (Long noIndividu : nosIndividus) {
					notifyTraitement(noIndividu);
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
			public int getInBatchQueueCount() {
				throw new NotImplementedException();
			}

			@Override
			public Long getBatchQueueSlidingAverageAge() {
				throw new NotImplementedException();
			}

			@Override
			public Long getBatchQueueGlobalAverageAge() {
				throw new NotImplementedException();
			}

			@Override
			public int getInManualQueueCount() {
				throw new NotImplementedException();
			}

			@Override
			public Long getManualQueueGlobalAverageAge() {
				throw new NotImplementedException();
			}

			@Override
			public Long getManualQueueSlidingAverageAge() {
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
		final EvenementCivilEchRetryProcessorImpl retry = new EvenementCivilEchRetryProcessorImpl();
		retry.setEvtCivilDAO(getBean(EvenementCivilEchDAO.class, "evenementCivilEchDAO"));
		retry.setRecuperateur(new MockEvenementCivilEchRecuperateur());
		retry.setTransactionManager(transactionManager);
		retry.setProcessor(queueProcessor);
		retry.setNotificationQueue(queueProcessor);
		
		// et maintenant : le test !
		
		// Ca, c'est pour vérifier que l'on traite les bons individus
		final Set<Long> remaining = new HashSet<>(Arrays.asList(noIndividuSans, noIndividuAvecAttente, noIndividuAvecErreur, noIndividuAvecAttenteEtErreur));
		final EvenementCivilEchProcessor.ListenerHandle handleRemaining = queueProcessor.registerListener(new EvenementCivilEchProcessor.Listener() {
			@Override
			public void onIndividuTraite(long noIndividu) {
				remaining.remove(noIndividu);
			}

			@Override
			public void onStop() {
			}
		});

		// lancement des travaux
		retry.retraiteEvenements(null);
		handleRemaining.unregister();
		
		// au final : il ne doit plus rester que le noIndividuSans dans la liste remaining, et tous les listeners doivent avoir été dés-enregistrés
		Assert.assertEquals(1, remaining.size());
		Assert.assertEquals((Long) noIndividuSans, remaining.iterator().next());
		Assert.assertEquals(0, listeners.size());
	}
}
