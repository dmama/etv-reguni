package ch.vd.uniregctb.editique.impl;

import junit.framework.Assert;
import org.junit.Test;

import ch.vd.uniregctb.common.WithoutSpringTest;
import ch.vd.uniregctb.editique.EditiqueResultat;

public class EditiqueRetourImpressionStorageServiceTest extends WithoutSpringTest {

	private EditiqueRetourImpressionStorageServiceImpl service;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		service = new EditiqueRetourImpressionStorageServiceImpl();
		service.setCleanupPeriod(1);    // 1 seconde
		service.afterPropertiesSet();
	}

	@Override
	public void onTearDown() throws Exception {
		service.destroy();
		service = null;
		
		super.onTearDown();
	}

	@Test(timeout = 1100)
	public void testTimeout() throws Exception {

		// je ne reçois rien, je vérifie que je sors au bout du temps requis : 1s
		final long tsDebut = System.currentTimeMillis();
		final EditiqueResultat res = service.getDocument("Mon document qui ne vient pas...", 1000);
		final long tsFin = System.currentTimeMillis();
		Assert.assertNull(res);
		Assert.assertTrue(tsFin - tsDebut > 1000);
	}

	private static EditiqueResultat buildResultat(String idDocument) {
		return buildResultat(idDocument, System.currentTimeMillis());
	}

	private static EditiqueResultat buildResultat(String idDocument, long timestampReceived) {
		final EditiqueResultatImpl resultat = new EditiqueResultatImpl();
		resultat.setIdDocument(idDocument);
		resultat.setTimestampReceived(timestampReceived);
		return resultat;
	}

	@Test(timeout = 1200)
	public void testReceptionEnCoursAttente() throws Exception {

		// on lance un thread qui envoie un document dans 200ms
		// et on commence tout de suite à attendre

		final String nomDocument = "Mon document tant attendu";
		final EditiqueResultat envoi = buildResultat(nomDocument);
		final Thread thread = new Thread(new Runnable() {
			public void run() {
				try {
					Thread.sleep(200);
					service.onArriveeRetourImpression(envoi);
				}
				catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
			}
		});
		thread.start();

		final long tsDebut = System.currentTimeMillis();
		final EditiqueResultat resultat = service.getDocument(nomDocument, 1000);
		final long tsFin = System.currentTimeMillis();
		Assert.assertNotNull(resultat);
		Assert.assertEquals(nomDocument, resultat.getIdDocument());
		Assert.assertTrue(tsFin - tsDebut < 1000);
		Assert.assertTrue(tsFin - tsDebut >= 200);

		thread.join();
	}

	@Test(timeout = 1200)
	public void testReceptionTropTard() throws Exception {

		// on lance un thread qui envoie un document dans 200ms
		// et on commence tout de suite à attendre (mais seulement 150ms) -> le document ne doit pas être vu

		final String nomDocument = "Mon document tant attendu";
		final EditiqueResultat envoi = buildResultat(nomDocument);
		final Thread thread = new Thread(new Runnable() {
			public void run() {
				try {
					Thread.sleep(200);
					service.onArriveeRetourImpression(envoi);
				}
				catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
			}
		});
		thread.start();

		// premier essai
		{
			final long tsDebut = System.currentTimeMillis();
			final EditiqueResultat resultat = service.getDocument(nomDocument, 150);
			final long tsFin = System.currentTimeMillis();
			Assert.assertNull(resultat);
			Assert.assertTrue(tsFin - tsDebut >= 150);
		}

		// deuxième essai : cette fois il doit venir
		{
			final EditiqueResultat resultat = service.getDocument(nomDocument, 150);
			Assert.assertNotNull(resultat);
		}

		thread.join();
	}

	@Test(timeout = 500)
	public void testReceptionAvantDemande() throws Exception {

		// on envoie un document maintenant, même avant de commencer à l'attendre,
		// on attend 200 ms et on le demande : il doit être là immédiatement

		final String nomDocument = "Mon document déjà là";
		{
			final EditiqueResultat resultat = buildResultat(nomDocument);
			service.onArriveeRetourImpression(resultat);
		}

		Thread.sleep(200);

		final long tsDebut = System.currentTimeMillis();
		final EditiqueResultat resultat = service.getDocument(nomDocument, 1000);
		final long tsFin = System.currentTimeMillis();
		Assert.assertNotNull(resultat);
		Assert.assertEquals(nomDocument, resultat.getIdDocument());
		Assert.assertTrue(tsFin - tsDebut < 20);
	}

	@Test(timeout = 1000)
	public void testDeuxiemeDemande() throws Exception {

		// on envoie un document, on le réceptionne
		// on tente une deuxième réception : il ne doit plus être disponible

		final String nomDocument = "Mon document déjà là";
		{
			final EditiqueResultat resultat = buildResultat(nomDocument);
			service.onArriveeRetourImpression(resultat);
		}

		Thread.sleep(200);

		// première réception
		{
			final long tsDebut = System.currentTimeMillis();
			final EditiqueResultat resultat = service.getDocument(nomDocument, 1000);
			final long tsFin = System.currentTimeMillis();
			Assert.assertNotNull(resultat);
			Assert.assertEquals(nomDocument, resultat.getIdDocument());
			Assert.assertTrue(tsFin - tsDebut < 20);
		}

		// seconde réception : on doit taper dans le timeout sans résultat
		{
			final long tsDebut = System.currentTimeMillis();
			final EditiqueResultat resultat = service.getDocument(nomDocument, 500);
			final long tsFin = System.currentTimeMillis();
			Assert.assertNull(resultat);
			Assert.assertTrue(tsFin - tsDebut >= 500);
		}
	}

	@Test(timeout = 600)
	public void testReceptionPendantAttenteMaisPasLeBon() throws Exception {

		// on lance un thread qui envoie un document dans 200ms
		// et on commence tout de suite à attendre

		final String nomDocumentAttendu = "Mon document tant attendu";
		final String nomDocumentEvoye = "Mon document reçu";

		final EditiqueResultat envoi = buildResultat(nomDocumentEvoye);
		final Thread thread = new Thread(new Runnable() {
			public void run() {
				try {
					Thread.sleep(200);
					service.onArriveeRetourImpression(envoi);
				}
				catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
			}
		});
		thread.start();

		final long tsDebut = System.currentTimeMillis();
		final EditiqueResultat resultat = service.getDocument(nomDocumentAttendu, 500);
		final long tsFin = System.currentTimeMillis();
		Assert.assertNull(resultat);
		Assert.assertTrue(tsFin - tsDebut >= 500);

		thread.join();
	}

	@Test(timeout = 600)
	public void testPlusieursReceptionsPendantAttenteDontLeBon() throws Exception {

		// on lance un thread qui envoie un document dans 200ms
		// et on commence tout de suite à attendre

		final String nomDocumentAttendu = "Mon document tant attendu";
		final String nomDocumentEvoye = "Mon autre document reçu";

		final EditiqueResultat documentAttendu = buildResultat(nomDocumentAttendu);
		final EditiqueResultat autreDocument = buildResultat(nomDocumentEvoye);
		final Thread thread = new Thread(new Runnable() {
			public void run() {
				try {
					Thread.sleep(200);
					service.onArriveeRetourImpression(autreDocument);

					Thread.sleep(100);
					service.onArriveeRetourImpression(documentAttendu);
				}
				catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
			}
		});
		thread.start();

		final long tsDebut = System.currentTimeMillis();
		final EditiqueResultat resultat = service.getDocument(nomDocumentAttendu, 500);
		final long tsFin = System.currentTimeMillis();
		Assert.assertNotNull(resultat);
		Assert.assertEquals(nomDocumentAttendu, resultat.getIdDocument());
		Assert.assertTrue(tsFin - tsDebut < 350);

		thread.join();
	}

	@Test(timeout = 2500)
	public void testCleanupVieilleReception() throws Exception {

		// on envoie un document maintenant, et on attend 2050 ms
		// (un peu plus du double de la période de cleanup pour être sûr)
		// quand on le demande alors, il ne doit pas y être

		final String nomDocument = "Mon document qui ne m'attend plus";
		final String nomDocumentVoyageurTemporel = "Time traveler";
		{
			final EditiqueResultat resultat = buildResultat(nomDocument);
			service.onArriveeRetourImpression(resultat);

			// date de réception : 1.8s dans le futur !
			// (afin de tester que seuls les vieux documents sont effacés)
			final EditiqueResultat timeTraveler = buildResultat(nomDocumentVoyageurTemporel, System.currentTimeMillis() + 1800L);
			service.onArriveeRetourImpression(timeTraveler);
		}

		// pendant ce temps-là, le cleanup doit relâcher l'un des documents arrivés plus haut
		Thread.sleep(2050);

		// le vieux document ne doit plus y être
		{
			final long tsDebut = System.currentTimeMillis();
			final EditiqueResultat resultat = service.getDocument(nomDocument, 300);
			final long tsFin = System.currentTimeMillis();
			Assert.assertNull(resultat);
			Assert.assertTrue(tsFin - tsDebut >= 300);
		}

		// mais le nouveau, toujours
		{
			final long tsDebut = System.currentTimeMillis();
			final EditiqueResultat resultat = service.getDocument(nomDocumentVoyageurTemporel, 300);
			final long tsFin = System.currentTimeMillis();
			Assert.assertNotNull(resultat);
			Assert.assertEquals(nomDocumentVoyageurTemporel, resultat.getIdDocument());
			Assert.assertTrue(tsFin - tsDebut < 20);
		}
	}
}
