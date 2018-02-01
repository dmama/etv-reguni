package ch.vd.unireg.editique.impl;

import java.time.Duration;
import java.time.Instant;

import org.apache.commons.lang3.mutable.MutableObject;
import org.junit.Assert;
import org.junit.Test;

import ch.vd.registre.base.date.InstantHelper;
import ch.vd.unireg.common.WithoutSpringTest;
import ch.vd.unireg.editique.EditiqueResultat;
import ch.vd.unireg.editique.EditiqueResultatDocument;
import ch.vd.unireg.editique.EditiqueResultatRecu;
import ch.vd.unireg.editique.EditiqueResultatTimeout;
import ch.vd.unireg.editique.RetourImpressionTrigger;
import ch.vd.unireg.stats.MockStatsService;

public class EditiqueRetourImpressionStorageServiceTest extends WithoutSpringTest {

	private EditiqueRetourImpressionStorageServiceImpl service;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		service = new EditiqueRetourImpressionStorageServiceImpl();
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

	@Test(timeout = 1500) // 1000 milliseondes + 500 pour tenir compte du temps d'exécution du test lui-même et d'un éventuel garbage collection
	public void testTimeout() throws Exception {

		// je ne reçois rien, je vérifie que je sors au bout du temps requis : 1s
		final Instant debut = InstantHelper.get();
		final String nomDocument = "Mon document qui ne vient pas...";
		final EditiqueResultat res = service.getDocument(nomDocument, Duration.ofMillis(1000));
		final Instant fin = InstantHelper.get();
		Assert.assertTrue(res instanceof EditiqueResultatTimeout);
		Assert.assertEquals(nomDocument, res.getIdDocument());

		final Duration attente = Duration.between(debut, fin);
		Assert.assertTrue("Attente = " + attente.toMillis() + "ms", attente.toMillis() >= 1000);
	}

	private static EditiqueResultatRecu buildResultat(String idDocument) {
		return new EditiqueResultatDocumentImpl(idDocument, null, null, null);
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

		final Instant debut = InstantHelper.get();
		final EditiqueResultat resultat = service.getDocument(nomDocument, Duration.ofMillis(1000));
		final Instant fin = InstantHelper.get();
		Assert.assertTrue(resultat instanceof EditiqueResultatDocument);
		Assert.assertEquals(nomDocument, resultat.getIdDocument());

		final Duration attente = Duration.between(debut, fin);
		Assert.assertTrue(attente.toString(), attente.toMillis() < 1000);
		Assert.assertTrue(attente.toString(), attente.toMillis() >= 200);

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
			final Instant debut = InstantHelper.get();
			final EditiqueResultat resultat = service.getDocument(nomDocument, Duration.ofMillis(150));
			final Instant fin = InstantHelper.get();
			Assert.assertTrue(resultat instanceof EditiqueResultatTimeout);
			Assert.assertEquals(nomDocument, resultat.getIdDocument());

			final Duration attente = Duration.between(debut, fin);
			Assert.assertTrue(attente.toString(), attente.toMillis() >= 150);
		}

		// deuxième essai : cette fois il doit venir
		{
			final EditiqueResultat resultat = service.getDocument(nomDocument, Duration.ofMillis(150));
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

		final Instant debut = InstantHelper.get();
		final EditiqueResultat resultat = service.getDocument(nomDocument, Duration.ofMillis(1000));
		final Instant fin = InstantHelper.get();
		Assert.assertTrue(resultat instanceof EditiqueResultatDocument);
		Assert.assertEquals(nomDocument, resultat.getIdDocument());

		final Duration attente = Duration.between(debut, fin);
		Assert.assertTrue(attente.toString(), attente.toMillis() < 50);
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
			final Instant debut = InstantHelper.get();
			final EditiqueResultat resultat = service.getDocument(nomDocument, Duration.ofMillis(1000));
			final Instant fin = InstantHelper.get();
			Assert.assertTrue(resultat instanceof EditiqueResultatDocument);
			Assert.assertEquals(nomDocument, resultat.getIdDocument());

			final Duration attente = Duration.between(debut, fin);
			Assert.assertTrue(attente.toString(), attente.toMillis() < 50);
		}

		// seconde réception : on doit taper dans le timeout sans résultat
		{
			final Instant debut = InstantHelper.get();
			final EditiqueResultat resultat = service.getDocument(nomDocument, Duration.ofMillis(500));
			final Instant fin = InstantHelper.get();
			Assert.assertTrue(resultat instanceof EditiqueResultatTimeout);
			Assert.assertEquals(nomDocument, resultat.getIdDocument());

			final Duration attente = Duration.between(debut, fin);
			Assert.assertTrue(attente.toString(), attente.toMillis() >= 500);
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

		final Instant debut = InstantHelper.get();
		final EditiqueResultat resultat = service.getDocument(nomDocumentAttendu, Duration.ofMillis(500));
		final Instant fin = InstantHelper.get();
		Assert.assertTrue(resultat instanceof EditiqueResultatTimeout);
		Assert.assertEquals(nomDocumentAttendu, resultat.getIdDocument());

		final Duration attente = Duration.between(debut, fin);
		Assert.assertTrue(attente.toString(), attente.toMillis() >= 500);

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

		final Instant debut = InstantHelper.get();
		final EditiqueResultat resultat = service.getDocument(nomDocumentAttendu, Duration.ofMillis(500));
		final Instant fin = InstantHelper.get();
		Assert.assertTrue(resultat instanceof EditiqueResultatDocument);
		Assert.assertEquals(nomDocumentAttendu, resultat.getIdDocument());

		final Duration attente = Duration.between(debut, fin);
		Assert.assertTrue(attente.toString(), attente.toMillis() < 350);

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
		}

		// date de réception : 1.8s dans le futur !
		// (afin de tester que seuls les vieux documents sont effacés)
		final Thread thread = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					Thread.sleep(1800L);
					final EditiqueResultatRecu timeTraveler = buildResultat(nomDocumentVoyageurTemporel);
					service.onArriveeRetourImpression(timeTraveler);
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
			final Instant debut = InstantHelper.get();
			final EditiqueResultat resultat = service.getDocument(nomDocument, Duration.ofMillis(300));
			final Instant fin = InstantHelper.get();
			Assert.assertTrue(resultat instanceof EditiqueResultatTimeout);
			Assert.assertEquals(nomDocument, resultat.getIdDocument());

			final Duration attente = Duration.between(debut, fin);
			Assert.assertTrue(attente.toString(), attente.toMillis() >= 300);
		}

		// mais le nouveau, toujours
		{
			final Instant debut = InstantHelper.get();
			final EditiqueResultat resultat = service.getDocument(nomDocumentVoyageurTemporel, Duration.ofMillis(300));
			final Instant fin = InstantHelper.get();
			Assert.assertTrue(resultat instanceof EditiqueResultatDocument);
			Assert.assertEquals(nomDocumentVoyageurTemporel, resultat.getIdDocument());

			final Duration attente = Duration.between(debut, fin);
			Assert.assertTrue(attente.toString(), attente.toMillis() < 50);
		}

		thread.join();
	}

	@Test
	public void testTriggerEnregistreAvantReception() throws Exception {

		// on enregistre un trigger, on attend un peu, il ne doit pas avoir bougé ;
		// quand le document du trigger arrive, on doit le voir dans le trigger
		final MutableObject<EditiqueResultatRecu> res = new MutableObject<>(null);
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
		final MutableObject<EditiqueResultatRecu> res = new MutableObject<>(null);
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
		final MutableObject<EditiqueResultatRecu> res = new MutableObject<>(null);
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
