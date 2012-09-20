package ch.vd.uniregctb.evenement.civil.engine.ech;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang.mutable.MutableInt;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.mock.MockServiceCivil;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEch;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEchDAO;
import ch.vd.uniregctb.evenement.civil.ech.MockEvenementCivilEchRethrower;
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
		
		final class MyHandle implements EvenementCivilEchProcessor.ListenerHandle {
			public final int value;

			MyHandle(int value) {
				this.value = value;
			}
		}

		final MutableInt pointer = new MutableInt(0);
		final Map<EvenementCivilEchProcessor.ListenerHandle, EvenementCivilEchProcessor.Listener> listeners = new HashMap<EvenementCivilEchProcessor.ListenerHandle, EvenementCivilEchProcessor.Listener>();

		final class IntegratedQueueAndProcessor implements EvenementCivilEchProcessor, EvenementCivilNotificationQueue {
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
			public void post(Long noIndividu, boolean immediate) {
				// traitement immédiat
				for (Listener listener : listeners.values()) {
					listener.onIndividuTraite(noIndividu);
				}
			}

			@Override
			public void postAll(Collection<Long> nosIndividus) {
				// traitement immédiat
				for (Long noIndividu : nosIndividus) {
					for (Listener listener : listeners.values()) {
						listener.onIndividuTraite(noIndividu);
					}
				}
			}

			@Override
			public Batch poll(long timeout, TimeUnit unit) throws InterruptedException {
				throw new NotImplementedException();
			}

			@Override
			public int getInflightCount() {
				throw new NotImplementedException();
			}
		}
		final IntegratedQueueAndProcessor queueProcessor = new IntegratedQueueAndProcessor();
		final EvenementCivilEchRetryProcessorImpl retry = new EvenementCivilEchRetryProcessorImpl();
		retry.setEvtCivilDAO(getBean(EvenementCivilEchDAO.class, "evenementCivilEchDAO"));
		retry.setRethrower(new MockEvenementCivilEchRethrower());
		retry.setTransactionManager(transactionManager);
		retry.setProcessor(queueProcessor);
		retry.setNotificationQueue(queueProcessor);
		
		// et maintenant : le test !
		
		// Ca, c'est pour vérifier que l'on traite les bons individus
		final Set<Long> remaining = new HashSet<Long>(Arrays.asList(noIndividuSans, noIndividuAvecAttente, noIndividuAvecErreur, noIndividuAvecAttenteEtErreur));
		final EvenementCivilEchProcessor.ListenerHandle handleRemaining = queueProcessor.registerListener(new EvenementCivilEchProcessor.Listener() {
			@Override
			public void onIndividuTraite(long noIndividu) {
				remaining.remove(noIndividu);
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
