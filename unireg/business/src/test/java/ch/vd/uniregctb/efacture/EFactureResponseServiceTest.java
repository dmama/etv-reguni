package ch.vd.uniregctb.efacture;

import java.time.Duration;
import java.time.Instant;

import org.junit.Assert;
import org.junit.Test;

import ch.vd.registre.base.date.InstantHelper;
import ch.vd.uniregctb.common.WithoutSpringTest;
import ch.vd.uniregctb.stats.MockStatsService;

public class EFactureResponseServiceTest extends WithoutSpringTest {

	private EFactureResponseServiceImpl service;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		service = new EFactureResponseServiceImpl();
		service.setCleanupPeriod(1);    // 1 seconde
		service.setStatsService(new MockStatsService());
		service.afterPropertiesSet();
	}

	@Override
	public void onTearDown() throws Exception {
		service.destroy();
		service = null;
		super.onTearDown();
	}

	@Test(timeout = 1500)
	public void testTimeout() throws Exception {
		// je ne reçois rien, je vérifie que je sors au bout du temps requis : 1s
		final Instant debut = InstantHelper.get();
		final String nomDocument = "Mon document qui ne vient pas...";
		final boolean received = service.waitForResponse(nomDocument, Duration.ofSeconds(1));
		final Instant fin = InstantHelper.get();
		Assert.assertFalse(received);

		final Duration attente = Duration.between(debut, fin);
		Assert.assertTrue("Attente = " + attente.toMillis() + "ms", attente.toMillis() >= 1000);
	}

	@Test(timeout = 1200)
	public void testReceptionEnCoursAttente() throws Exception {

		// on lance un thread qui envoie une réponse dans 200ms
		// et on commence tout de suite à attendre

		final String businessId = "Ma réponse";
		final Thread thread = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					Thread.sleep(200);
					service.onNewResponse(businessId);
				}
				catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
			}
		});
		thread.start();

		final Instant debut = InstantHelper.get();
		final boolean resultat = service.waitForResponse(businessId, Duration.ofSeconds(1));
		final Instant fin = InstantHelper.get();
		Assert.assertTrue(resultat);

		final Duration attente = Duration.between(debut, fin);
		Assert.assertTrue(attente.toString(), attente.toMillis() < 1000);
		Assert.assertTrue(attente.toString(), attente.toMillis() >= 200);

		thread.join();
	}
}
