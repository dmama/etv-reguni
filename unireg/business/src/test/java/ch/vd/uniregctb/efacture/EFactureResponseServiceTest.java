package ch.vd.uniregctb.efacture;

import junit.framework.Assert;
import org.junit.Test;

import ch.vd.uniregctb.common.TimeHelper;
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
		final long tsDebut = TimeHelper.getPreciseCurrentTimeMillis();
		final String nomDocument = "Mon document qui ne vient pas...";
		final boolean received = service.waitForResponse(nomDocument, 1000);
		final long tsFin = TimeHelper.getPreciseCurrentTimeMillis();
		Assert.assertFalse(received);
		final long attente = tsFin - tsDebut;
		Assert.assertTrue("Attente = " + attente + "ms", attente >= 1000);
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

		final long tsDebut = TimeHelper.getPreciseCurrentTimeMillis();
		final boolean resultat = service.waitForResponse(businessId, 1000);
		final long tsFin = TimeHelper.getPreciseCurrentTimeMillis();
		Assert.assertTrue(resultat);
		Assert.assertTrue(tsFin - tsDebut < 1000);
		Assert.assertTrue(tsFin - tsDebut >= 200);

		thread.join();
	}
}
