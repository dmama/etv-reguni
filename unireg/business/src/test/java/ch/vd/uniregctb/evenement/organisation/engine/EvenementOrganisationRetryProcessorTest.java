package ch.vd.uniregctb.evenement.organisation.engine;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.mutable.MutableInt;
import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.NotImplementedException;
import ch.vd.unireg.interfaces.organisation.mock.DefaultMockServiceOrganisation;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisation;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationDAO;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationProcessingMode;
import ch.vd.uniregctb.evenement.organisation.MockEvenementOrganisationRecuperateur;
import ch.vd.uniregctb.evenement.organisation.engine.processor.EvenementOrganisationProcessor;
import ch.vd.uniregctb.type.EmetteurEvenementOrganisation;
import ch.vd.uniregctb.type.EtatEvenementOrganisation;
import ch.vd.uniregctb.type.TypeEvenementOrganisation;

public class EvenementOrganisationRetryProcessorTest extends BusinessTest {

	private EvenementOrganisation addEvent(Long noOrganisation, long id, TypeEvenementOrganisation type, RegDate date, EtatEvenementOrganisation etat,
	                                       EmetteurEvenementOrganisation senderId, @Nullable String refDataEmetteur) {
		final EvenementOrganisation event = new EvenementOrganisation();
		event.setId(id);
		event.setNoOrganisation(noOrganisation);
		event.setType(type);
		event.setDateEvenement(date);
		event.setEtat(etat);
		event.setIdentiteEmetteur(senderId);
		event.setRefDataEmetteur(refDataEmetteur);
		return hibernateTemplate.merge(event);
	}

	// FIXME: Complete test !!!!
	@Ignore
	@Test(timeout = 10000L)
	public void testBasics() throws Exception {
		
		final long noIndividuSans = 17385423L;
		final long noIndividuAvecAttente = 16745234L;
		final long noIndividuAvecErreur = 2367485247L;
		final long noIndividuAvecAttenteEtErreur = 43784236L;
		
		serviceOrganisation.setUp(new DefaultMockServiceOrganisation());

/*
		serviceOrganisation.setUp(new MockServiceOrganisation() {
			@Override
			protected void init() {
				addIndividu(noIndividuSans, null, "Sans", "Rien", true);
				addIndividu(noIndividuAvecAttente, null, "Avec", "Attente", false);
				addIndividu(noIndividuAvecErreur, null, "Avec", "Erreur", false);
				addIndividu(noIndividuAvecAttenteEtErreur, null, "Avec", "Attente-Erreur", false);
			}
		});
*/
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				long id = 0L;

				final RegDate date = RegDate.get();
/*
				final TypeEvenementOrganisation type = TypeEvenementOrganisation.TESTING;
				final ActionEvenementOrganisation action = ActionEvenementOrganisation.PREMIERE_LIVRAISON;

				addEvent(noIndividuSans, ++id, type, action, date, EtatEvenementOrganisation.A_VERIFIER);
				addEvent(noIndividuSans, ++id, type, action, date, EtatEvenementOrganisation.FORCE);
				addEvent(noIndividuSans, ++id, type, action, date, EtatEvenementOrganisation.REDONDANT);
				addEvent(noIndividuSans, ++id, type, action, date, EtatEvenementOrganisation.TRAITE);

				addEvent(noIndividuAvecAttente, ++id, type, action, date, EtatEvenementOrganisation.A_VERIFIER);
				addEvent(noIndividuAvecAttente, ++id, type, action, date, EtatEvenementOrganisation.EN_ATTENTE);

				addEvent(noIndividuAvecErreur, ++id, type, action, date, EtatEvenementOrganisation.FORCE);
				addEvent(noIndividuAvecErreur, ++id, type, action, date, EtatEvenementOrganisation.EN_ERREUR);

				addEvent(noIndividuAvecAttenteEtErreur, ++id, type, action, date, EtatEvenementOrganisation.TRAITE);
				addEvent(noIndividuAvecAttenteEtErreur, ++id, type, action, date, EtatEvenementOrganisation.EN_ATTENTE);
				addEvent(noIndividuAvecAttenteEtErreur, ++id, type, action, date, EtatEvenementOrganisation.EN_ERREUR);
*/

				return null;
			}
		});

		final class MyHandle implements EvenementOrganisationProcessor.ListenerHandle {
			public final int value;

			MyHandle(int value) {
				this.value = value;
			}
		}

		final MutableInt pointer = new MutableInt(0);
		final Map<EvenementOrganisationProcessor.ListenerHandle, EvenementOrganisationProcessor.Listener> listeners = new HashMap<>();

		final class IntegratedQueueAndProcessor implements EvenementOrganisationProcessor, EvenementOrganisationNotificationQueue {
			@Override
			public ListenerHandle registerListener(Listener listener) {
				pointer.increment();
				final MyHandle handle = new MyHandle(pointer.intValue());
				listeners.put(handle, listener);
				return handle;
			}

			@Override
			public void unregisterListener(ListenerHandle handle) {
				Assert.assertTrue(handle instanceof MyHandle);
				listeners.remove(handle);
			}

			@Override
			public void restartProcessingThread(boolean agressiveKill) {
				throw new NotImplementedException();
			}

			private void notifyTraitement(Long noOrganisation) {
				for (Listener listener : listeners.values()) {
					listener.onOrganisationTraite(noOrganisation);
				}
			}

			@Override
			public void post(Long noIndividu, EvenementOrganisationProcessingMode mode) {
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
			public Batch poll(long timeout, TimeUnit unit) throws InterruptedException {
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
		retry.setRecuperateur(new MockEvenementOrganisationRecuperateur());
		retry.setTransactionManager(transactionManager);
		retry.setProcessor(queueProcessor);
		retry.setNotificationQueue(queueProcessor);
		
		// et maintenant : le test !
		
		// Ca, c'est pour vérifier que l'on traite les bons individus
		final Set<Long> remaining = new HashSet<>(Arrays.asList(noIndividuSans, noIndividuAvecAttente, noIndividuAvecErreur, noIndividuAvecAttenteEtErreur));
		final EvenementOrganisationProcessor.ListenerHandle handleRemaining = queueProcessor.registerListener(new EvenementOrganisationProcessor.Listener() {
			@Override
			public void onOrganisationTraite(long noOrganisation) {
				remaining.remove(noOrganisation);
			}

			@Override
			public void onStop() {
			}
		});
		Assert.assertTrue(handleRemaining instanceof MyHandle);
		
		// lancement des travaux
		retry.retraiteEvenements(null);
		queueProcessor.unregisterListener(handleRemaining);
		
		// au final : il ne doit plus rester que le noIndividuSans dans la liste remaining, et tous les listeners doivent avoir été dés-enregistrés
		Assert.assertEquals(1, remaining.size());
		Assert.assertEquals((Long) noIndividuSans, remaining.iterator().next());
		Assert.assertEquals(0, listeners.size());
	}
}
