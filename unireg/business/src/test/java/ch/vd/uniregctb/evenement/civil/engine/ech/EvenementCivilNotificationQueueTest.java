package ch.vd.uniregctb.evenement.civil.engine.ech;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import junit.framework.Assert;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEch;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEchBasicInfo;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEchService;
import ch.vd.uniregctb.type.ActionEvenementCivilEch;
import ch.vd.uniregctb.type.EtatEvenementCivil;
import ch.vd.uniregctb.type.TypeEvenementCivilEch;

public class EvenementCivilNotificationQueueTest extends BusinessTest {

	private EvenementCivilNotificationQueueImpl queue;

	@Override
	protected void runOnSetUp() throws Exception {
		super.runOnSetUp();
		queue = new EvenementCivilNotificationQueueImpl();
		queue.setEvtCivilService(getBean(EvenementCivilEchService.class, "evtCivilEchService"));
	}

	private EvenementCivilEch addEvenementCivil(Long id, long noIndividu, RegDate date, TypeEvenementCivilEch type, ActionEvenementCivilEch action, EtatEvenementCivil etat) {
		final EvenementCivilEch evt = new EvenementCivilEch();
		evt.setId(id);
		evt.setDateEvenement(date);
		evt.setNumeroIndividu(noIndividu);
		evt.setType(type);
		evt.setAction(action);
		evt.setEtat(etat);
		return hibernateTemplate.merge(evt);
	}

	@Test
	public void testRecupVide() throws Exception {
		Assert.assertEquals(0, queue.getInflightCount());
		Assert.assertNull(queue.poll(1, TimeUnit.MILLISECONDS));
		Assert.assertNull(queue.poll(20, TimeUnit.MILLISECONDS));
	}

	@Test
	public void testRecupSimple() throws Exception {

		final long noIndividu = 243523L;
		final long noIndividuSans = 2433L;

		// préparation des événements dans la base
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				addEvenementCivil(1L, noIndividu, date(1999, 1, 1), TypeEvenementCivilEch.NAISSANCE, ActionEvenementCivilEch.PREMIERE_LIVRAISON, EtatEvenementCivil.EN_ERREUR);
				addEvenementCivil(5L, noIndividu, date(1999, 2, 5), TypeEvenementCivilEch.NAISSANCE, ActionEvenementCivilEch.PREMIERE_LIVRAISON, EtatEvenementCivil.A_TRAITER);
				addEvenementCivil(3L, noIndividu, date(1999, 3, 3), TypeEvenementCivilEch.NAISSANCE, ActionEvenementCivilEch.PREMIERE_LIVRAISON, EtatEvenementCivil.EN_ATTENTE);
				addEvenementCivil(2L, noIndividu, date(1999, 4, 2), TypeEvenementCivilEch.NAISSANCE, ActionEvenementCivilEch.PREMIERE_LIVRAISON, EtatEvenementCivil.TRAITE);
				addEvenementCivil(6L, noIndividu, date(1999, 5, 6), TypeEvenementCivilEch.NAISSANCE, ActionEvenementCivilEch.PREMIERE_LIVRAISON, EtatEvenementCivil.A_VERIFIER);
				addEvenementCivil(4L, noIndividu, date(1999, 6, 4), TypeEvenementCivilEch.NAISSANCE, ActionEvenementCivilEch.PREMIERE_LIVRAISON, EtatEvenementCivil.FORCE);
				addEvenementCivil(8L, noIndividu, date(1999, 3, 3), TypeEvenementCivilEch.DIVORCE, ActionEvenementCivilEch.PREMIERE_LIVRAISON, EtatEvenementCivil.EN_ATTENTE);
				addEvenementCivil(7L, noIndividu, date(1999, 3, 3), TypeEvenementCivilEch.ARRIVEE, ActionEvenementCivilEch.PREMIERE_LIVRAISON, EtatEvenementCivil.EN_ATTENTE);
				return null;
			}
		});

		// envois dans la queue
		Assert.assertEquals(0, queue.getInflightCount());
		queue.post(noIndividuSans);
		Assert.assertEquals(1, queue.getInflightCount());
		queue.post(noIndividu);
		Assert.assertEquals(2, queue.getInflightCount());
		queue.post(noIndividu);      // c'est un doublon -> il ne devrait apparaître qu'une fois en sortie
		Assert.assertEquals(2, queue.getInflightCount());

		// première récupération : individu sans événement -> collection vide
		final EvenementCivilNotificationQueue.Batch infoSans = queue.poll(1, TimeUnit.MILLISECONDS);
		Assert.assertNotNull(infoSans);
		Assert.assertEquals(noIndividuSans, infoSans.noIndividu);
		Assert.assertNotNull(infoSans.contenu);
		Assert.assertEquals(0, infoSans.contenu.size());
		Assert.assertEquals(1, queue.getInflightCount());

		// deuxième récupération : individu avec événements -> collection avec 3 éléments (seulements les événements non traités)
		final EvenementCivilNotificationQueue.Batch infoAvec = queue.poll(1, TimeUnit.MILLISECONDS);
		Assert.assertNotNull(infoAvec);
		Assert.assertEquals(noIndividu, infoAvec.noIndividu);
		Assert.assertNotNull(infoAvec.contenu);
		Assert.assertEquals(5, infoAvec.contenu.size());
		Assert.assertEquals(0, queue.getInflightCount());
		{
			final EvenementCivilEchBasicInfo evtCivilInfo = infoAvec.contenu.get(0);
			Assert.assertEquals(1L, evtCivilInfo.idEvenement);
			Assert.assertEquals(date(1999, 1, 1), evtCivilInfo.date);
			Assert.assertEquals(EtatEvenementCivil.EN_ERREUR, evtCivilInfo.etat);
			Assert.assertEquals(TypeEvenementCivilEch.NAISSANCE, evtCivilInfo.type);
			Assert.assertEquals(ActionEvenementCivilEch.PREMIERE_LIVRAISON, evtCivilInfo.action);
		}
		{
			final EvenementCivilEchBasicInfo evtCivilInfo = infoAvec.contenu.get(1);
			Assert.assertEquals(5L, evtCivilInfo.idEvenement);
			Assert.assertEquals(date(1999, 2, 5), evtCivilInfo.date);
			Assert.assertEquals(EtatEvenementCivil.A_TRAITER, evtCivilInfo.etat);
			Assert.assertEquals(TypeEvenementCivilEch.NAISSANCE, evtCivilInfo.type);
			Assert.assertEquals(ActionEvenementCivilEch.PREMIERE_LIVRAISON, evtCivilInfo.action);
		}
		{
			final EvenementCivilEchBasicInfo evtCivilInfo = infoAvec.contenu.get(2);
			Assert.assertEquals(3L, evtCivilInfo.idEvenement);
			Assert.assertEquals(date(1999, 3, 3), evtCivilInfo.date);
			Assert.assertEquals(EtatEvenementCivil.EN_ATTENTE, evtCivilInfo.etat);
			Assert.assertEquals(TypeEvenementCivilEch.NAISSANCE, evtCivilInfo.type);
			Assert.assertEquals(ActionEvenementCivilEch.PREMIERE_LIVRAISON, evtCivilInfo.action);
		}
		{
			final EvenementCivilEchBasicInfo evtCivilInfo = infoAvec.contenu.get(3);
			Assert.assertEquals(7L, evtCivilInfo.idEvenement);
			Assert.assertEquals(date(1999, 3, 3), evtCivilInfo.date);
			Assert.assertEquals(EtatEvenementCivil.EN_ATTENTE, evtCivilInfo.etat);
			Assert.assertEquals(TypeEvenementCivilEch.ARRIVEE, evtCivilInfo.type);
			Assert.assertEquals(ActionEvenementCivilEch.PREMIERE_LIVRAISON, evtCivilInfo.action);
		}
		{
			final EvenementCivilEchBasicInfo evtCivilInfo = infoAvec.contenu.get(4);
			Assert.assertEquals(8L, evtCivilInfo.idEvenement);
			Assert.assertEquals(date(1999, 3, 3), evtCivilInfo.date);
			Assert.assertEquals(EtatEvenementCivil.EN_ATTENTE, evtCivilInfo.etat);
			Assert.assertEquals(TypeEvenementCivilEch.DIVORCE, evtCivilInfo.type);
			Assert.assertEquals(ActionEvenementCivilEch.PREMIERE_LIVRAISON, evtCivilInfo.action);
		}

		// troisième tentative de récupération : rien
		Assert.assertNull(queue.poll(1, TimeUnit.MILLISECONDS));
	}
	
	@Test
	public void testRecupSimpleAvecPostAll() throws Exception {

		final long noIndividu = 243523L;
		final long noIndividuSans = 2433L;

		// préparation des événements dans la base
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				addEvenementCivil(1L, noIndividu, date(1999, 1, 1), TypeEvenementCivilEch.NAISSANCE, ActionEvenementCivilEch.PREMIERE_LIVRAISON, EtatEvenementCivil.EN_ERREUR);
				addEvenementCivil(5L, noIndividu, date(1999, 2, 5), TypeEvenementCivilEch.NAISSANCE, ActionEvenementCivilEch.PREMIERE_LIVRAISON, EtatEvenementCivil.A_TRAITER);
				addEvenementCivil(3L, noIndividu, date(1999, 3, 3), TypeEvenementCivilEch.NAISSANCE, ActionEvenementCivilEch.PREMIERE_LIVRAISON, EtatEvenementCivil.EN_ATTENTE);
				addEvenementCivil(2L, noIndividu, date(1999, 4, 2), TypeEvenementCivilEch.NAISSANCE, ActionEvenementCivilEch.PREMIERE_LIVRAISON, EtatEvenementCivil.TRAITE);
				addEvenementCivil(6L, noIndividu, date(1999, 5, 6), TypeEvenementCivilEch.NAISSANCE, ActionEvenementCivilEch.PREMIERE_LIVRAISON, EtatEvenementCivil.A_VERIFIER);
				addEvenementCivil(4L, noIndividu, date(1999, 6, 4), TypeEvenementCivilEch.NAISSANCE, ActionEvenementCivilEch.PREMIERE_LIVRAISON, EtatEvenementCivil.FORCE);
				addEvenementCivil(8L, noIndividu, date(1999, 3, 3), TypeEvenementCivilEch.DIVORCE, ActionEvenementCivilEch.PREMIERE_LIVRAISON, EtatEvenementCivil.EN_ATTENTE);
				addEvenementCivil(7L, noIndividu, date(1999, 3, 3), TypeEvenementCivilEch.ARRIVEE, ActionEvenementCivilEch.PREMIERE_LIVRAISON, EtatEvenementCivil.EN_ATTENTE);
				return null;
			}
		});

		// envois dans la queue
		Assert.assertEquals(0, queue.getInflightCount());
		queue.postAll(Arrays.asList(noIndividuSans, noIndividu, noIndividu));
		Assert.assertEquals(2, queue.getInflightCount());

		// première récupération : individu sans événement -> collection vide
		final EvenementCivilNotificationQueue.Batch infoSans = queue.poll(1, TimeUnit.MILLISECONDS);
		Assert.assertNotNull(infoSans);
		Assert.assertEquals(noIndividuSans, infoSans.noIndividu);
		Assert.assertNotNull(infoSans.contenu);
		Assert.assertEquals(0, infoSans.contenu.size());
		Assert.assertEquals(1, queue.getInflightCount());

		// deuxième récupération : individu avec événements -> collection avec 3 éléments (seulements les événements non traités)
		final EvenementCivilNotificationQueue.Batch infoAvec = queue.poll(1, TimeUnit.MILLISECONDS);
		Assert.assertNotNull(infoAvec);
		Assert.assertEquals(noIndividu, infoAvec.noIndividu);
		Assert.assertNotNull(infoAvec.contenu);
		Assert.assertEquals(5, infoAvec.contenu.size());
		Assert.assertEquals(0, queue.getInflightCount());
		{
			final EvenementCivilEchBasicInfo evtCivilInfo = infoAvec.contenu.get(0);
			Assert.assertEquals(1L, evtCivilInfo.idEvenement);
			Assert.assertEquals(date(1999, 1, 1), evtCivilInfo.date);
			Assert.assertEquals(EtatEvenementCivil.EN_ERREUR, evtCivilInfo.etat);
			Assert.assertEquals(TypeEvenementCivilEch.NAISSANCE, evtCivilInfo.type);
			Assert.assertEquals(ActionEvenementCivilEch.PREMIERE_LIVRAISON, evtCivilInfo.action);
		}
		{
			final EvenementCivilEchBasicInfo evtCivilInfo = infoAvec.contenu.get(1);
			Assert.assertEquals(5L, evtCivilInfo.idEvenement);
			Assert.assertEquals(date(1999, 2, 5), evtCivilInfo.date);
			Assert.assertEquals(EtatEvenementCivil.A_TRAITER, evtCivilInfo.etat);
			Assert.assertEquals(TypeEvenementCivilEch.NAISSANCE, evtCivilInfo.type);
			Assert.assertEquals(ActionEvenementCivilEch.PREMIERE_LIVRAISON, evtCivilInfo.action);
		}
		{
			final EvenementCivilEchBasicInfo evtCivilInfo = infoAvec.contenu.get(2);
			Assert.assertEquals(3L, evtCivilInfo.idEvenement);
			Assert.assertEquals(date(1999, 3, 3), evtCivilInfo.date);
			Assert.assertEquals(EtatEvenementCivil.EN_ATTENTE, evtCivilInfo.etat);
			Assert.assertEquals(TypeEvenementCivilEch.NAISSANCE, evtCivilInfo.type);
			Assert.assertEquals(ActionEvenementCivilEch.PREMIERE_LIVRAISON, evtCivilInfo.action);
		}
		{
			final EvenementCivilEchBasicInfo evtCivilInfo = infoAvec.contenu.get(3);
			Assert.assertEquals(7L, evtCivilInfo.idEvenement);
			Assert.assertEquals(date(1999, 3, 3), evtCivilInfo.date);
			Assert.assertEquals(EtatEvenementCivil.EN_ATTENTE, evtCivilInfo.etat);
			Assert.assertEquals(TypeEvenementCivilEch.ARRIVEE, evtCivilInfo.type);
			Assert.assertEquals(ActionEvenementCivilEch.PREMIERE_LIVRAISON, evtCivilInfo.action);
		}
		{
			final EvenementCivilEchBasicInfo evtCivilInfo = infoAvec.contenu.get(4);
			Assert.assertEquals(8L, evtCivilInfo.idEvenement);
			Assert.assertEquals(date(1999, 3, 3), evtCivilInfo.date);
			Assert.assertEquals(EtatEvenementCivil.EN_ATTENTE, evtCivilInfo.etat);
			Assert.assertEquals(TypeEvenementCivilEch.DIVORCE, evtCivilInfo.type);
			Assert.assertEquals(ActionEvenementCivilEch.PREMIERE_LIVRAISON, evtCivilInfo.action);
		}

		// troisième tentative de récupération : rien
		Assert.assertNull(queue.poll(1, TimeUnit.MILLISECONDS));
	}
}
