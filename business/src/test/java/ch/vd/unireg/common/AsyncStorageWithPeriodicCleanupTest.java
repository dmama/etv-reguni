package ch.vd.unireg.common;

import java.time.Duration;
import java.time.Instant;

import org.junit.Assert;
import org.junit.Test;

import ch.vd.registre.base.date.InstantHelper;

public class AsyncStorageWithPeriodicCleanupTest<S extends AsyncStorageWithPeriodicCleanup<String, String>> extends AsyncStorageTest<S> {

	@Override
	protected S buildStorage() {
		//noinspection unchecked
		return (S) new AsyncStorageWithPeriodicCleanup<String, String>(1, "CleanupThread");   // 1 seconde
	}

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		service.start();
	}

	@Override
	public void onTearDown() throws Exception {
		service.stop();
		super.onTearDown();
	}

	@Test(timeout = 4000)
	public void testCleanupVieilleReception() throws Exception {

		// on envoie un document maintenant, et on attend 2050 ms
		// (un peu plus du double de la période de cleanup pour être sûr)
		// quand on le demande alors, il ne doit pas y être

		final String key = "Mon document qui ne m'attend plus";
		final String keyLate = "Retardataire...";

		service.add(key, null);
		final Thread thread = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					Thread.sleep(1800);
					service.add(keyLate, null);
				}
				catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
			}
		});
		thread.start();

		Assert.assertEquals(0, service.getNbPurgedElements());

		// pendant ce temps-là, le cleanup doit relâcher l'un des documents arrivés plus haut
		Thread.sleep(2050);

		// le vieux document ne doit plus y être
		{
			final Instant debut = InstantHelper.get();
			final AsyncStorage.RetrievalResult<String> resultat = service.get(key, Duration.ofMillis(300));
			final Instant fin = InstantHelper.get();
			Assert.assertTrue(resultat instanceof AsyncStorage.RetrievalTimeout);
			Assert.assertEquals(key, resultat.key);

			final Duration attente = Duration.between(debut, fin);
			Assert.assertTrue(attente.toString(), attente.toMillis() >= 300);
		}

		// mais le nouveau, toujours
		{
			final Instant debut = InstantHelper.get();
			final AsyncStorage.RetrievalResult<String> resultat = service.get(keyLate, Duration.ofMillis(300));
			final Instant fin = InstantHelper.get();
			Assert.assertTrue(resultat instanceof AsyncStorage.RetrievalData);
			Assert.assertEquals(keyLate, resultat.key);

			final Duration attente = Duration.between(debut, fin);
			Assert.assertTrue(attente.toString(), attente.toMillis() < 50);
		}

		Assert.assertEquals(1, service.getNbPurgedElements());
		thread.join();
	}
}
