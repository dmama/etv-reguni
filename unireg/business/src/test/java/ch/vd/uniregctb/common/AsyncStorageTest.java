package ch.vd.uniregctb.common;

import java.time.Duration;
import java.time.Instant;

import org.junit.Assert;
import org.junit.Test;

import ch.vd.registre.base.date.InstantHelper;

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
		final Instant debut = InstantHelper.get();
		final String key = "Mon document qui ne vient pas...";
		final AsyncStorage.RetrievalResult<String> received = service.get(key, Duration.ofSeconds(1));
		final Instant fin = InstantHelper.get();
		Assert.assertTrue(received instanceof AsyncStorage.RetrievalTimeout);
		Assert.assertEquals(key, received.key);

		final Duration attente = Duration.between(debut, fin);
		Assert.assertTrue("Attente = " + attente.toMillis() + "ms", attente.toMillis() >= 1000);
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

		final Instant debut = InstantHelper.get();
		final AsyncStorage.RetrievalResult<String> resultat = service.get(key, Duration.ofSeconds(1));
		final Instant fin = InstantHelper.get();
		Assert.assertTrue(resultat instanceof AsyncStorage.RetrievalData);
		Assert.assertEquals(key, resultat.key);
		Assert.assertEquals(value, ((AsyncStorage.RetrievalData) resultat).data);

		final Duration attente = Duration.between(debut, fin);
		Assert.assertTrue(attente.toString(), attente.toMillis() < 1000);
		Assert.assertTrue(attente.toString(), attente.toMillis() >= 200);

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
			final Instant debut = InstantHelper.get();
			final AsyncStorage.RetrievalResult<String> resultat = service.get(key, Duration.ofMillis(150));
			final Instant fin = InstantHelper.get();
			Assert.assertTrue(resultat instanceof AsyncStorage.RetrievalTimeout);
			Assert.assertEquals(key, resultat.key);

			final Duration attente = Duration.between(debut, fin);
			Assert.assertTrue(attente.toString(), attente.toMillis() >= 150);
		}

		// deuxième essai : cette fois il doit venir
		{
			final AsyncStorage.RetrievalResult<String> resultat = service.get(key, Duration.ofMillis(150));
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

		final Instant debut = InstantHelper.get();
		final AsyncStorage.RetrievalResult<String> resultat = service.get(key, Duration.ofMillis(1000));
		final Instant fin = InstantHelper.get();
		Assert.assertTrue(resultat instanceof AsyncStorage.RetrievalData);
		Assert.assertEquals(key, resultat.key);

		final Duration processDuration = Duration.between(debut, fin);
		Assert.assertTrue(processDuration.toString(), processDuration.toMillis() < 50);
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
			final Instant debut = InstantHelper.get();
			final AsyncStorage.RetrievalResult<String> resultat = service.get(key, Duration.ofMillis(1000));
			final Instant fin = InstantHelper.get();
			Assert.assertTrue(resultat instanceof AsyncStorage.RetrievalData);
			Assert.assertEquals(key, resultat.key);

			final Duration attente = Duration.between(debut, fin);
			Assert.assertTrue(attente.toString(), attente.toMillis() < 50);
		}

		// seconde réception : on doit taper dans le timeout sans résultat
		{
			final Instant debut = InstantHelper.get();
			final AsyncStorage.RetrievalResult<String> resultat = service.get(key, Duration.ofMillis(500));
			final Instant fin = InstantHelper.get();
			Assert.assertTrue(resultat instanceof AsyncStorage.RetrievalTimeout);
			Assert.assertEquals(key, resultat.key);

			final Duration attente = Duration.between(debut, fin);
			Assert.assertTrue(attente.toString(), attente.toMillis() >= 500);
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

		final Instant debut = InstantHelper.get();
		final AsyncStorage.RetrievalResult<String> resultat = service.get(keyAttendue, Duration.ofMillis(500));
		final Instant fin = InstantHelper.get();
		Assert.assertTrue(resultat instanceof AsyncStorage.RetrievalTimeout);
		Assert.assertEquals(keyAttendue, resultat.key);

		final Duration attente = Duration.between(debut, fin);
		Assert.assertTrue(attente.toString(), attente.toMillis() >= 500);

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

		final Instant debut = InstantHelper.get();
		final AsyncStorage.RetrievalResult<String> resultat = service.get(keyAttendue, Duration.ofMillis(500));
		final Instant fin = InstantHelper.get();
		Assert.assertTrue(resultat instanceof AsyncStorage.RetrievalData);
		Assert.assertEquals(keyAttendue, resultat.key);

		final Duration attente = Duration.between(debut, fin);
		Assert.assertTrue(attente.toString(), attente.toMillis() < 350);

		thread.join();
	}
}
