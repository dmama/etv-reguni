package ch.vd.uniregctb.common;

import java.util.concurrent.TimeUnit;

import junit.framework.Assert;
import org.junit.Test;

public class ASyncStorageWithPeriodicCleanupTest extends ASyncStorageTest<ASyncStorageWithPeriodicCleanup<String, String>> {

	@Override
	protected ASyncStorageWithPeriodicCleanup<String, String> buildStorage() {
		return new ASyncStorageWithPeriodicCleanup<String, String>(1);   // 1 seconde
	}

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		service.start("CleanupThread");
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

		// pendant ce temps-là, le cleanup doit relâcher l'un des documents arrivés plus haut
		Thread.sleep(2050);

		// le vieux document ne doit plus y être
		{
			final long tsDebut = TimeHelper.getPreciseCurrentTimeMillis();
			final ASyncStorage.RetrievalResult<String, String> resultat = service.retrieve(key, 300, TimeUnit.MILLISECONDS);
			final long tsFin = TimeHelper.getPreciseCurrentTimeMillis();
			Assert.assertTrue(resultat instanceof ASyncStorage.RetrievalTimeout);
			Assert.assertEquals(key, resultat.key);
			Assert.assertTrue(tsFin - tsDebut >= 300);
		}

		// mais le nouveau, toujours
		{
			final long tsDebut = TimeHelper.getPreciseCurrentTimeMillis();
			final ASyncStorage.RetrievalResult<String, String> resultat = service.retrieve(keyLate, 300, TimeUnit.MILLISECONDS);
			final long tsFin = TimeHelper.getPreciseCurrentTimeMillis();
			Assert.assertTrue(resultat instanceof ASyncStorage.RetrievalData);
			Assert.assertEquals(keyLate, resultat.key);
			Assert.assertTrue(tsFin - tsDebut < 50);
		}

		thread.join();
	}

}
