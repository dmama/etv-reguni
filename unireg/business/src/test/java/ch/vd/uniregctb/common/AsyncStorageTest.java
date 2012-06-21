package ch.vd.uniregctb.common;

import java.util.concurrent.TimeUnit;

import junit.framework.Assert;
import org.junit.Test;

public class AsyncStorageTest<S extends AsyncStorage<String, String>> extends WithoutSpringTest {

	protected S service;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		service = buildStorage();
	}

	protected S buildStorage() {
		//noinspection unchecked
		return (S) new AsyncStorage<String, String>();
	}

	@Test(timeout = 1500)
	public void testTimeout() throws Exception {
		// je ne reçois rien, je vérifie que je sors au bout du temps requis : 1s
		final long tsDebut = TimeHelper.getPreciseCurrentTimeMillis();
		final String key = "Mon document qui ne vient pas...";
		final AsyncStorage.RetrievalResult<String> received = service.get(key, 1000, TimeUnit.MILLISECONDS);
		final long tsFin = TimeHelper.getPreciseCurrentTimeMillis();
		Assert.assertTrue(received instanceof AsyncStorage.RetrievalTimeout);
		Assert.assertEquals(key, received.key);
		final long attente = tsFin - tsDebut;
		Assert.assertTrue("Attente = " + attente + "ms", attente >= 1000);
	}

	@Test(timeout = 1200)
	public void testReceptionEnCoursAttente() throws Exception {

		// on lance un thread qui envoie une réponse dans 200ms
		// et on commence tout de suite à attendre

		final String key = "Ma question";
		final String value = "Ma réponse";
		final Thread thread = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					Thread.sleep(200);
					service.add(key, value);
				}
				catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
			}
		});
		thread.start();

		final long tsDebut = TimeHelper.getPreciseCurrentTimeMillis();
		final AsyncStorage.RetrievalResult<String> resultat = service.get(key, 1000, TimeUnit.MILLISECONDS);
		final long tsFin = TimeHelper.getPreciseCurrentTimeMillis();
		Assert.assertTrue(resultat instanceof AsyncStorage.RetrievalData);
		Assert.assertEquals(key, resultat.key);
		Assert.assertEquals(value, ((AsyncStorage.RetrievalData) resultat).data);
		Assert.assertTrue(tsFin - tsDebut < 1000);
		Assert.assertTrue(tsFin - tsDebut >= 200);

		thread.join();
	}

	@Test(timeout = 1200)
	public void testReceptionTropTard() throws Exception {

		// on lance un thread qui envoie un document dans 200ms
		// et on commence tout de suite à attendre (mais seulement 150ms) -> le document ne doit pas être vu

		final String key = "Ma question importante";
		final String value = "Ma réponse tant attendue";
		final Thread thread = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					Thread.sleep(200);
					service.add(key, value);
				}
				catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
			}
		});
		thread.start();

		// premier essai
		{
			final long tsDebut = TimeHelper.getPreciseCurrentTimeMillis();
			final AsyncStorage.RetrievalResult<String> resultat = service.get(key, 150, TimeUnit.MILLISECONDS);
			final long tsFin = TimeHelper.getPreciseCurrentTimeMillis();
			Assert.assertTrue(resultat instanceof AsyncStorage.RetrievalTimeout);
			Assert.assertEquals(key, resultat.key);
			Assert.assertTrue(tsFin - tsDebut >= 150);
		}

		// deuxième essai : cette fois il doit venir
		{
			final AsyncStorage.RetrievalResult<String> resultat = service.get(key, 150, TimeUnit.MILLISECONDS);
			Assert.assertTrue(resultat instanceof AsyncStorage.RetrievalData);
			Assert.assertEquals(key, resultat.key);
			Assert.assertEquals(value, ((AsyncStorage.RetrievalData) resultat).data);
		}

		thread.join();
	}

	@Test(timeout = 500)
	public void testReceptionAvantDemande() throws Exception {

		// on envoie un document maintenant, même avant de commencer à l'attendre,
		// on attend 200 ms et on le demande : il doit être là immédiatement

		final String key = "Mon document déjà là";
		final String value = "Bon contenu...";
		service.add(key, value);

		Thread.sleep(200);

		final long tsDebut = TimeHelper.getPreciseCurrentTimeMillis();
		final AsyncStorage.RetrievalResult<String> resultat = service.get(key, 1000, TimeUnit.MILLISECONDS);
		final long tsFin = TimeHelper.getPreciseCurrentTimeMillis();
		Assert.assertTrue(resultat instanceof AsyncStorage.RetrievalData);
		Assert.assertEquals(key, resultat.key);
		Assert.assertTrue(tsFin - tsDebut < 50);
	}

	@Test(timeout = 1000)
	public void testDeuxiemeDemande() throws Exception {

		// on envoie un document, on le réceptionne
		// on tente une deuxième réception : il ne doit plus être disponible

		final String key = "Mon document déjà là";
		final String value = "Bon contenu...";
		{
			service.add(key, value);
		}

		Thread.sleep(200);

		// première réception
		{
			final long tsDebut = TimeHelper.getPreciseCurrentTimeMillis();
			final AsyncStorage.RetrievalResult<String> resultat = service.get(key, 1000, TimeUnit.MILLISECONDS);
			final long tsFin = TimeHelper.getPreciseCurrentTimeMillis();
			Assert.assertTrue(resultat instanceof AsyncStorage.RetrievalData);
			Assert.assertEquals(key, resultat.key);
			Assert.assertTrue(tsFin - tsDebut < 50);
		}

		// seconde réception : on doit taper dans le timeout sans résultat
		{
			final long tsDebut = TimeHelper.getPreciseCurrentTimeMillis();
			final AsyncStorage.RetrievalResult<String> resultat = service.get(key, 500, TimeUnit.MILLISECONDS);
			final long tsFin = TimeHelper.getPreciseCurrentTimeMillis();
			Assert.assertTrue(resultat instanceof AsyncStorage.RetrievalTimeout);
			Assert.assertEquals(key, resultat.key);
			Assert.assertTrue(tsFin - tsDebut >= 500);
		}
	}

	@Test(timeout = 800)
	public void testReceptionPendantAttenteMaisPasLeBon() throws Exception {

		// on lance un thread qui envoie un document dans 200ms
		// et on commence tout de suite à attendre

		final String keyAttendue = "Mon document tant attendu";
		final String keyRecue = "Mon document reçu";

		final Thread thread = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					Thread.sleep(200);
					service.add(keyRecue, null);
				}
				catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
			}
		});
		thread.start();

		final long tsDebut = TimeHelper.getPreciseCurrentTimeMillis();
		final AsyncStorage.RetrievalResult<String> resultat = service.get(keyAttendue, 500, TimeUnit.MILLISECONDS);
		final long tsFin = TimeHelper.getPreciseCurrentTimeMillis();
		Assert.assertTrue(resultat instanceof AsyncStorage.RetrievalTimeout);
		Assert.assertEquals(keyAttendue, resultat.key);
		Assert.assertTrue(tsFin - tsDebut >= 500);

		thread.join();
	}

	@Test(timeout = 1000)
	public void testPlusieursReceptionsPendantAttenteDontLeBon() throws Exception {

		// on lance un thread qui envoie un document dans 200ms
		// et on commence tout de suite à attendre

		final String keyAttendue = "Mon document tant attendu";
		final String keyRecue = "Mon autre document reçu";

		final Thread thread = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					Thread.sleep(200);
					service.add(keyRecue, null);

					Thread.sleep(100);
					service.add(keyAttendue, null);
				}
				catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
			}
		});
		thread.start();

		final long tsDebut = TimeHelper.getPreciseCurrentTimeMillis();
		final AsyncStorage.RetrievalResult<String> resultat = service.get(keyAttendue, 500, TimeUnit.MILLISECONDS);
		final long tsFin = TimeHelper.getPreciseCurrentTimeMillis();
		Assert.assertTrue(resultat instanceof AsyncStorage.RetrievalData);
		Assert.assertEquals(keyAttendue, resultat.key);
		Assert.assertTrue(tsFin - tsDebut < 350);

		thread.join();
	}
}
