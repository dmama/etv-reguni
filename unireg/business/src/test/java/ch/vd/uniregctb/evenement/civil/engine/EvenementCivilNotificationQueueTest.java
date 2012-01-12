package ch.vd.uniregctb.evenement.civil.engine;

import java.util.List;
import java.util.concurrent.TimeUnit;

import junit.framework.Assert;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterne;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterneDAO;
import ch.vd.uniregctb.type.EtatEvenementCivil;
import ch.vd.uniregctb.type.TypeEvenementCivil;

public class EvenementCivilNotificationQueueTest extends BusinessTest {

	private EvenementCivilNotificationQueueImpl queue;

	@Override
	protected void runOnSetUp() throws Exception {
		super.runOnSetUp();
		queue = new EvenementCivilNotificationQueueImpl();
		queue.setEvtCivilDao(getBean(EvenementCivilExterneDAO.class, "evenementCivilExterneDAO"));
		queue.setHibernateTemplate(hibernateTemplate);
		queue.setTransactionManager(transactionManager);
	}

	private EvenementCivilExterne addEvenementCivilExterne(Long id, long noIndividu, RegDate date, TypeEvenementCivil type, EtatEvenementCivil etat) {
		final EvenementCivilExterne evt = new EvenementCivilExterne();
		evt.setId(id);
		evt.setDateEvenement(date);
		evt.setNumeroIndividuPrincipal(noIndividu);
		evt.setType(type);
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
				addEvenementCivilExterne(1L, noIndividu, date(1999, 1, 1), TypeEvenementCivil.CHGT_CATEGORIE_ETRANGER, EtatEvenementCivil.EN_ERREUR);
				addEvenementCivilExterne(5L, noIndividu, date(1999, 2, 5), TypeEvenementCivil.CHGT_CATEGORIE_ETRANGER, EtatEvenementCivil.A_TRAITER);
				addEvenementCivilExterne(3L, noIndividu, date(1999, 3, 3), TypeEvenementCivil.CHGT_CATEGORIE_ETRANGER, EtatEvenementCivil.EN_ATTENTE);
				addEvenementCivilExterne(2L, noIndividu, date(1999, 4, 2), TypeEvenementCivil.CHGT_CATEGORIE_ETRANGER, EtatEvenementCivil.TRAITE);
				addEvenementCivilExterne(6L, noIndividu, date(1999, 5, 6), TypeEvenementCivil.CHGT_CATEGORIE_ETRANGER, EtatEvenementCivil.A_VERIFIER);
				addEvenementCivilExterne(4L, noIndividu, date(1999, 6, 4), TypeEvenementCivil.CHGT_CATEGORIE_ETRANGER, EtatEvenementCivil.FORCE);
				return null;
			}
		});

		// envois dans la queue
		Assert.assertEquals(0, queue.getInflightCount());
		queue.add(noIndividuSans);
		Assert.assertEquals(1, queue.getInflightCount());
		queue.add(noIndividu);
		Assert.assertEquals(2, queue.getInflightCount());
		queue.add(noIndividu);      // c'est un doublon -> il ne devrait apparaître qu'une fois en sortie
		Assert.assertEquals(2, queue.getInflightCount());

		// première récupération : individu sans événement -> collection vide
		final List<EvenementCivilNotificationQueue.EvtCivilInfo> infoSans = queue.poll(1, TimeUnit.MILLISECONDS);
		Assert.assertNotNull(infoSans);
		Assert.assertEquals(0, infoSans.size());
		Assert.assertEquals(1, queue.getInflightCount());

		// deuxième récupération : individu avec événements -> collection avec 3 éléments (seulements les événements non traités)
		final List<EvenementCivilNotificationQueue.EvtCivilInfo> infoAvec = queue.poll(1, TimeUnit.MILLISECONDS);
		Assert.assertNotNull(infoAvec);
		Assert.assertEquals(3, infoAvec.size());
		Assert.assertEquals(0, queue.getInflightCount());
		{
			final EvenementCivilNotificationQueue.EvtCivilInfo evtCivilInfo = infoAvec.get(0);
			Assert.assertEquals(1L, evtCivilInfo.idEvenement);
			Assert.assertEquals(date(1999, 1, 1), evtCivilInfo.date);
			Assert.assertEquals(EtatEvenementCivil.EN_ERREUR, evtCivilInfo.etat);
			Assert.assertEquals(TypeEvenementCivil.CHGT_CATEGORIE_ETRANGER, evtCivilInfo.type);
		}
		{
			final EvenementCivilNotificationQueue.EvtCivilInfo evtCivilInfo = infoAvec.get(1);
			Assert.assertEquals(5L, evtCivilInfo.idEvenement);
			Assert.assertEquals(date(1999, 2, 5), evtCivilInfo.date);
			Assert.assertEquals(EtatEvenementCivil.A_TRAITER, evtCivilInfo.etat);
			Assert.assertEquals(TypeEvenementCivil.CHGT_CATEGORIE_ETRANGER, evtCivilInfo.type);
		}
		{
			final EvenementCivilNotificationQueue.EvtCivilInfo evtCivilInfo = infoAvec.get(2);
			Assert.assertEquals(3L, evtCivilInfo.idEvenement);
			Assert.assertEquals(date(1999, 3, 3), evtCivilInfo.date);
			Assert.assertEquals(EtatEvenementCivil.EN_ATTENTE, evtCivilInfo.etat);
			Assert.assertEquals(TypeEvenementCivil.CHGT_CATEGORIE_ETRANGER, evtCivilInfo.type);
		}

		// troisième tentative de récupération : rien
		Assert.assertNull(queue.poll(1, TimeUnit.MILLISECONDS));
	}
}
