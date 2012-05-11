package ch.vd.uniregctb.editique.impl;

import junit.framework.Assert;
import org.apache.commons.lang.mutable.MutableObject;
import org.junit.Test;

import ch.vd.registre.base.utils.NotImplementedException;
import ch.vd.uniregctb.cache.UniregCacheInterface;
import ch.vd.uniregctb.common.TimeHelper;
import ch.vd.uniregctb.common.WithoutSpringTest;
import ch.vd.uniregctb.editique.EditiqueResultat;
import ch.vd.uniregctb.editique.EditiqueResultatDocument;
import ch.vd.uniregctb.editique.EditiqueResultatRecu;
import ch.vd.uniregctb.editique.EditiqueResultatTimeout;
import ch.vd.uniregctb.editique.RetourImpressionTrigger;
import ch.vd.uniregctb.stats.LoadMonitor;
import ch.vd.uniregctb.stats.ServiceStats;
import ch.vd.uniregctb.stats.ServiceTracingInterface;
import ch.vd.uniregctb.stats.StatsService;

public class EditiqueRetourImpressionStorageServiceTest extends WithoutSpringTest {

	private EditiqueRetourImpressionStorageServiceImpl service;

	/**
	 * Dummy class pour ne pas avoir besoin de tout spring pour juste du log...
	 */
	private static class DummyStatsService implements StatsService {

		@Override
		public void registerService(String serviceName, ServiceTracingInterface tracing) {
			// celle-ci est censée être appelée dans le onSetup()
		}

		@Override
		public void registerCache(String serviceName, UniregCacheInterface cache) {
			throw new NotImplementedException();
		}

		@Override
		public void registerLoadMonitor(String serviceName, LoadMonitor monitor) {
			throw new NotImplementedException();
		}

		@Override
		public void unregisterService(String serviceName) {
			// celle-ci est censée être appelée dans le onTearDown()
		}

		@Override
		public void unregisterCache(String serviceName) {
			throw new NotImplementedException();
		}

		@Override
		public void unregisterLoadMonitor(String serviceName) {
			throw new NotImplementedException();
		}

		@Override
		public ServiceStats getServiceStats(String serviceName) {
			throw new NotImplementedException();
		}

		@Override
		public String buildStats() {
			throw new NotImplementedException();
		}
	}

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		service = new EditiqueRetourImpressionStorageServiceImpl();
		service.setCleanupPeriod(1);    // 1 seconde
		service.setStatsService(new DummyStatsService());
		service.afterPropertiesSet();
	}

	@Override
	public void onTearDown() throws Exception {
		service.destroy();
		service = null;
		
		super.onTearDown();
	}

	@Test(timeout = 1500) // 1000 milliseondes + 500 pour tenir compte du temps d'exécution du test lui-même et d'un éventuel garbage collection
	public void testTimeout() throws Exception {

		// je ne reçois rien, je vérifie que je sors au bout du temps requis : 1s
		final long tsDebut = TimeHelper.getPreciseCurrentTimeMillis();
		final String nomDocument = "Mon document qui ne vient pas...";
		final EditiqueResultat res = service.getDocument(nomDocument, 1000);
		final long tsFin = TimeHelper.getPreciseCurrentTimeMillis();
		Assert.assertTrue(res instanceof EditiqueResultatTimeout);
		Assert.assertEquals(nomDocument, res.getIdDocument());
		final long attente = tsFin - tsDebut;
		Assert.assertTrue("Attente = " + attente + "ms", attente >= 1000);
	}

	private static EditiqueResultatRecu buildResultat(String idDocument) {
		return buildResultat(idDocument, TimeHelper.getPreciseCurrentTimeMillis());
	}

	private static EditiqueResultatRecu buildResultat(String idDocument, long timestampReceived) {
		return new EditiqueResultatDocumentImpl(idDocument, null, null, null, timestampReceived);
	}

	@Test(timeout = 1200)
	public void testReceptionEnCoursAttente() throws Exception {

		// on lance un thread qui envoie un document dans 200ms
		// et on commence tout de suite à attendre

		final String nomDocument = "Mon document tant attendu";
		final EditiqueResultatRecu envoi = buildResultat(nomDocument);
		final Thread thread = new Thread(new Runnable() {
			@Override
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

		final long tsDebut = TimeHelper.getPreciseCurrentTimeMillis();
		final EditiqueResultat resultat = service.getDocument(nomDocument, 1000);
		final long tsFin = TimeHelper.getPreciseCurrentTimeMillis();
		Assert.assertTrue(resultat instanceof EditiqueResultatDocument);
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
		final EditiqueResultatRecu envoi = buildResultat(nomDocument);
		final Thread thread = new Thread(new Runnable() {
			@Override
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
			final long tsDebut = TimeHelper.getPreciseCurrentTimeMillis();
			final EditiqueResultat resultat = service.getDocument(nomDocument, 150);
			final long tsFin = TimeHelper.getPreciseCurrentTimeMillis();
			Assert.assertTrue(resultat instanceof EditiqueResultatTimeout);
			Assert.assertEquals(nomDocument, resultat.getIdDocument());
			Assert.assertTrue(tsFin - tsDebut >= 150);
		}

		// deuxième essai : cette fois il doit venir
		{
			final EditiqueResultat resultat = service.getDocument(nomDocument, 150);
			Assert.assertTrue(resultat instanceof EditiqueResultatDocument);
		}

		thread.join();
	}

	@Test(timeout = 500)
	public void testReceptionAvantDemande() throws Exception {

		// on envoie un document maintenant, même avant de commencer à l'attendre,
		// on attend 200 ms et on le demande : il doit être là immédiatement

		final String nomDocument = "Mon document déjà là";
		{
			final EditiqueResultatRecu resultat = buildResultat(nomDocument);
			service.onArriveeRetourImpression(resultat);
		}

		Thread.sleep(200);

		final long tsDebut = TimeHelper.getPreciseCurrentTimeMillis();
		final EditiqueResultat resultat = service.getDocument(nomDocument, 1000);
		final long tsFin = TimeHelper.getPreciseCurrentTimeMillis();
		Assert.assertTrue(resultat instanceof EditiqueResultatDocument);
		Assert.assertEquals(nomDocument, resultat.getIdDocument());
		Assert.assertTrue(tsFin - tsDebut < 50);
	}

	@Test(timeout = 1000)
	public void testDeuxiemeDemande() throws Exception {

		// on envoie un document, on le réceptionne
		// on tente une deuxième réception : il ne doit plus être disponible

		final String nomDocument = "Mon document déjà là";
		{
			final EditiqueResultatRecu resultat = buildResultat(nomDocument);
			service.onArriveeRetourImpression(resultat);
		}

		Thread.sleep(200);

		// première réception
		{
			final long tsDebut = TimeHelper.getPreciseCurrentTimeMillis();
			final EditiqueResultat resultat = service.getDocument(nomDocument, 1000);
			final long tsFin = TimeHelper.getPreciseCurrentTimeMillis();
			Assert.assertTrue(resultat instanceof EditiqueResultatDocument);
			Assert.assertEquals(nomDocument, resultat.getIdDocument());
			Assert.assertTrue(tsFin - tsDebut < 50);
		}

		// seconde réception : on doit taper dans le timeout sans résultat
		{
			final long tsDebut = TimeHelper.getPreciseCurrentTimeMillis();
			final EditiqueResultat resultat = service.getDocument(nomDocument, 500);
			final long tsFin = TimeHelper.getPreciseCurrentTimeMillis();
			Assert.assertTrue(resultat instanceof EditiqueResultatTimeout);
			Assert.assertEquals(nomDocument, resultat.getIdDocument());
			Assert.assertTrue(tsFin - tsDebut >= 500);
		}
	}

	@Test(timeout = 800)
	public void testReceptionPendantAttenteMaisPasLeBon() throws Exception {

		// on lance un thread qui envoie un document dans 200ms
		// et on commence tout de suite à attendre

		final String nomDocumentAttendu = "Mon document tant attendu";
		final String nomDocumentEvoye = "Mon document reçu";

		final EditiqueResultatRecu envoi = buildResultat(nomDocumentEvoye);
		final Thread thread = new Thread(new Runnable() {
			@Override
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

		final long tsDebut = TimeHelper.getPreciseCurrentTimeMillis();
		final EditiqueResultat resultat = service.getDocument(nomDocumentAttendu, 500);
		final long tsFin = TimeHelper.getPreciseCurrentTimeMillis();
		Assert.assertTrue(resultat instanceof EditiqueResultatTimeout);
		Assert.assertEquals(nomDocumentAttendu, resultat.getIdDocument());
		Assert.assertTrue(tsFin - tsDebut >= 500);

		thread.join();
	}

	@Test(timeout = 1000)
	public void testPlusieursReceptionsPendantAttenteDontLeBon() throws Exception {

		// on lance un thread qui envoie un document dans 200ms
		// et on commence tout de suite à attendre

		final String nomDocumentAttendu = "Mon document tant attendu";
		final String nomDocumentEvoye = "Mon autre document reçu";

		final EditiqueResultatRecu documentAttendu = buildResultat(nomDocumentAttendu);
		final EditiqueResultatRecu autreDocument = buildResultat(nomDocumentEvoye);
		final Thread thread = new Thread(new Runnable() {
			@Override
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

		final long tsDebut = TimeHelper.getPreciseCurrentTimeMillis();
		final EditiqueResultat resultat = service.getDocument(nomDocumentAttendu, 500);
		final long tsFin = TimeHelper.getPreciseCurrentTimeMillis();
		Assert.assertTrue(resultat instanceof EditiqueResultatDocument);
		Assert.assertEquals(nomDocumentAttendu, resultat.getIdDocument());
		Assert.assertTrue(tsFin - tsDebut < 350);

		thread.join();
	}

	@Test(timeout = 4000)
	public void testCleanupVieilleReception() throws Exception {

		// on envoie un document maintenant, et on attend 2050 ms
		// (un peu plus du double de la période de cleanup pour être sûr)
		// quand on le demande alors, il ne doit pas y être

		final String nomDocument = "Mon document qui ne m'attend plus";
		final String nomDocumentVoyageurTemporel = "Time traveler";
		{
			final EditiqueResultatRecu resultat = buildResultat(nomDocument);
			service.onArriveeRetourImpression(resultat);

			// date de réception : 1.8s dans le futur !
			// (afin de tester que seuls les vieux documents sont effacés)
			final EditiqueResultatRecu timeTraveler = buildResultat(nomDocumentVoyageurTemporel, TimeHelper.getPreciseCurrentTimeMillis() + 1800L);
			service.onArriveeRetourImpression(timeTraveler);
		}

		// pendant ce temps-là, le cleanup doit relâcher l'un des documents arrivés plus haut
		Thread.sleep(2050);

		// le vieux document ne doit plus y être
		{
			final long tsDebut = TimeHelper.getPreciseCurrentTimeMillis();
			final EditiqueResultat resultat = service.getDocument(nomDocument, 300);
			final long tsFin = TimeHelper.getPreciseCurrentTimeMillis();
			Assert.assertTrue(resultat instanceof EditiqueResultatTimeout);
			Assert.assertEquals(nomDocument, resultat.getIdDocument());
			Assert.assertTrue(tsFin - tsDebut >= 300);
		}

		// mais le nouveau, toujours
		{
			final long tsDebut = TimeHelper.getPreciseCurrentTimeMillis();
			final EditiqueResultat resultat = service.getDocument(nomDocumentVoyageurTemporel, 300);
			final long tsFin = TimeHelper.getPreciseCurrentTimeMillis();
			Assert.assertTrue(resultat instanceof EditiqueResultatDocument);
			Assert.assertEquals(nomDocumentVoyageurTemporel, resultat.getIdDocument());
			Assert.assertTrue(tsFin - tsDebut < 50);
		}
	}

	@Test
	public void testTriggerEnregistreAvantReception() throws Exception {

		// on enregistre un trigger, on attend un peu, il ne doit pas avoir bougé ;
		// quand le document du trigger arrive, on doit le voir dans le trigger
		final MutableObject res = new MutableObject(null);
		final RetourImpressionTrigger myTrigger = new RetourImpressionTrigger() {
			@Override
			public void trigger(EditiqueResultatRecu resultat) throws Exception {
				res.setValue(resultat);
			}
		};

		final String nomDocument = "Mon document qui déclenche tout";
		service.registerTrigger(nomDocument, myTrigger);

		// on attend un peu... rien ne bouge, normalement
		Thread.sleep(1000);

		Assert.assertNull("Pourquoi le résultat est-il déjà revenu?", res.getValue());

		// maintenant le document arrive...
		service.onArriveeRetourImpression(buildResultat(nomDocument));

		// on laisse la main quelques instants...
		Thread.sleep(100);

		final Object value = res.getValue();
		Assert.assertNotNull("Pas encore arrivé ?", value);
		Assert.assertTrue(value instanceof EditiqueResultatDocument);

		final EditiqueResultatDocument resultatRecu = (EditiqueResultatDocument) value;
		Assert.assertEquals(nomDocument, resultatRecu.getIdDocument());
	}

	@Test
	public void testTriggerEnregistreApresReception() throws Exception {

		// arrivée d'un document, on attends un peu, puis on enregistre le trigger
		// dès le retour de la méthode d'enregistrement, le trigger doit déjà avoir
		// été déclanché

		// maintenant le document arrive...
		final String nomDocument = "Mon document qui arrive entre deux...";
		service.onArriveeRetourImpression(buildResultat(nomDocument));

		// on attend un peu... rien ne bouge, normalement
		Thread.sleep(500);

        // on enregistre un trigger
		final MutableObject res = new MutableObject(null);
		final RetourImpressionTrigger myTrigger = new RetourImpressionTrigger() {
			@Override
			public void trigger(EditiqueResultatRecu resultat) throws Exception {
				res.setValue(resultat);
			}
		};

		Assert.assertNull("Initialisation bizarre...", res.getValue());
		service.registerTrigger(nomDocument, myTrigger);

		// on laisse la main quelques instants...
		Thread.sleep(100);

		final Object value = res.getValue();
		Assert.assertNotNull("Le trigger n'a pas encore été appelé ? Bizarre...", value);
		Assert.assertTrue(value instanceof EditiqueResultatDocument);

		final EditiqueResultatDocument resultatRecu = (EditiqueResultatDocument) value;
		Assert.assertEquals(nomDocument, resultatRecu.getIdDocument());
	}

	@Test
	public void testTriggerEnregistreSurMauvaisDocument() throws Exception {

		// on enregistre un trigger, on attend un peu, il ne doit pas avoir bougé ;
		// quand le document du trigger arrive, il ne doit toujours pas bouger (ce n'est pas le bon document !)
		final MutableObject res = new MutableObject(null);
		final RetourImpressionTrigger myTrigger = new RetourImpressionTrigger() {
			@Override
			public void trigger(EditiqueResultatRecu resultat) throws Exception {
				res.setValue(resultat);
			}
		};

		final String nomDocumentAttendu = "Mon document qui est attendu";
		final String nomDocumentArrive = "Mon document qui arrive";
		service.registerTrigger(nomDocumentAttendu, myTrigger);

		// on attend un peu... rien ne bouge, normalement
		Thread.sleep(1000);

		Assert.assertNull("Pourquoi le résultat est-il revenu?", res.getValue());

		// maintenant le document arrive...
		service.onArriveeRetourImpression(buildResultat(nomDocumentArrive));

		// on laisse la main quelques instants...
		Thread.sleep(1000);

		Assert.assertNull("Pourquoi le trigger a-t-il été lancé ?", res.getValue());
	}
}
