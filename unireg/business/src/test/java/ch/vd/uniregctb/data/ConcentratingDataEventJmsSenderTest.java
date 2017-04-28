package ch.vd.uniregctb.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;

import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.xml.event.data.v1.DataEvent;
import ch.vd.unireg.xml.event.data.v1.FiscalEventSendRequestEvent;
import ch.vd.unireg.xml.event.data.v1.TiersChangeEvent;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.evenement.fiscal.EvenementFiscalFor;
import ch.vd.uniregctb.hibernate.interceptor.ModificationInterceptor;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.Sexe;

public class ConcentratingDataEventJmsSenderTest extends BusinessTest {

	private ConcentratingDataEventJmsSender concentrator;

	private void buildConcentrator(DataEventSender sender, boolean enabled) throws Exception {
		final ConcentratingDataEventJmsSender concentrator = new ConcentratingDataEventJmsSender();
		concentrator.setEvenementsFiscauxActives(enabled);
		concentrator.setParentInterceptor(getBean(ModificationInterceptor.class, "modificationInterceptor"));
		concentrator.setParentService(getBean(DataEventService.class, "dataEventService"));
		concentrator.setSender(sender);
		concentrator.afterPropertiesSet();
		this.concentrator = concentrator;
	}

	@NotNull
	private static DataEventSender buildCollectingSender(List<List<DataEvent>> destinationCollection) {
		return destinationCollection::add;
	}

	@Override
	protected void runOnSetUp() throws Exception {
		super.runOnSetUp();
		this.concentrator = null;               // pour en avoir un, il faut appeler buildConcentrator()
	}

	@Override
	public void onTearDown() throws Exception {
		if (concentrator != null) {
			concentrator.destroy();
			concentrator = null;
		}
		super.onTearDown();
	}

	@Test
	public void testEmptyTransaction() throws Exception {

		final List<List<DataEvent>> collected = new ArrayList<>();
		buildConcentrator(buildCollectingSender(collected), true);

		// transaction complètement vide
		doInNewTransactionAndSession(status -> null);

		// on vérifie que rien n'est passé
		Assert.assertEquals(0, collected.size());
	}

	@Test
	public void testCreationEntity() throws Exception {

		final List<List<DataEvent>> collected = new ArrayList<>();
		buildConcentrator(buildCollectingSender(collected), true);

		// création de la personne physique non-habitante
		final long ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addNonHabitant("Alfredo", "De Montbuisson", null, Sexe.MASCULIN);
			return pp.getNumero();
		});

		// normalement, un évémenent de nettoyage de cache pour le nouveau contribuable a dû arriver
		Assert.assertEquals(1, collected.size());

		final List<DataEvent> events = collected.get(0);
		Assert.assertEquals(1, events.size());
		Assert.assertEquals(TiersChangeEvent.class, events.get(0).getClass());
		Assert.assertEquals(ppId, ((TiersChangeEvent) events.get(0)).getId());
	}

	@Test
	public void testEnvoiEvenementFiscal() throws Exception {

		// création de la personne physique non-habitante
		final long ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addNonHabitant("Alfredo", "De Montbuisson", null, Sexe.MASCULIN);
			return pp.getNumero();
		});

		// mise en place du système de collecte
		final List<List<DataEvent>> collected = new ArrayList<>();
		buildConcentrator(buildCollectingSender(collected), true);

		// ajout d'un for principal sur l'entité créée au préalable
		final long idEvtFiscal = doInNewTransactionAndSession(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				Assert.assertNotNull(pp);
				final ForFiscalPrincipal ffp = addForPrincipal(pp, date(1995, 1, 1), MotifFor.ARRIVEE_HS, MockCommune.Echallens);
				final EvenementFiscalFor evtFiscal = addEvenementFiscalFor(pp, ffp, ffp.getDateDebut(), EvenementFiscalFor.TypeEvenementFiscalFor.OUVERTURE);
				concentrator.sendEvent(evtFiscal);
				return evtFiscal.getId();
			}
		});

		// que s'est-il passé ?
		// normalement, on attend :
		// - un message pour la modification du tiers
		// - un événement fiscal pour l'ouverture du for
		Assert.assertEquals(1, collected.size());

		final List<DataEvent> events = collected.get(0);
		Assert.assertEquals(2, events.size());
		{
			Assert.assertEquals(TiersChangeEvent.class, events.get(0).getClass());
			Assert.assertEquals(ppId, ((TiersChangeEvent) events.get(0)).getId());
		}
		{
			Assert.assertEquals(FiscalEventSendRequestEvent.class, events.get(1).getClass());
			final FiscalEventSendRequestEvent evt = (FiscalEventSendRequestEvent) events.get(1);
			Assert.assertEquals(Collections.singletonList(idEvtFiscal), evt.getId());
			doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
				@Override
				protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
					final EvenementFiscalFor evtFiscal = hibernateTemplate.get(EvenementFiscalFor.class, idEvtFiscal);
					Assert.assertNotNull(evtFiscal);
					Assert.assertFalse(evtFiscal.isAnnule());
					Assert.assertEquals(EvenementFiscalFor.TypeEvenementFiscalFor.OUVERTURE, evtFiscal.getType());
					Assert.assertEquals(date(1995, 1, 1), evtFiscal.getDateValeur());
					Assert.assertEquals((Long) ppId, evtFiscal.getForFiscal().getTiers().getNumero());
				}
			});
		}
	}

	@Test
	public void testEnvoiEvenementFiscalDesactive() throws Exception {

		// création de la personne physique non-habitante
		final long ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addNonHabitant("Alfredo", "De Montbuisson", null, Sexe.MASCULIN);
			return pp.getNumero();
		});

		// mise en place du système de collecte
		final List<List<DataEvent>> collected = new ArrayList<>();
		buildConcentrator(buildCollectingSender(collected), false);     // desactivation des événements fiscaux

		// ajout d'un for principal sur l'entité créée au préalable
		final long idEvtFiscal = doInNewTransactionAndSession(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				Assert.assertNotNull(pp);
				final ForFiscalPrincipal ffp = addForPrincipal(pp, date(1995, 1, 1), MotifFor.ARRIVEE_HS, MockCommune.Echallens);
				final EvenementFiscalFor evtFiscal = addEvenementFiscalFor(pp, ffp, ffp.getDateDebut(), EvenementFiscalFor.TypeEvenementFiscalFor.OUVERTURE);
				concentrator.sendEvent(evtFiscal);
				return evtFiscal.getId();
			}
		});

		// que s'est-il passé ?
		// normalement, on attend :
		// - un message pour la modification du tiers
		// - un événement fiscal pour l'ouverture du for
		Assert.assertEquals(1, collected.size());

		final List<DataEvent> events = collected.get(0);
		Assert.assertEquals(1, events.size());
		{
			Assert.assertEquals(TiersChangeEvent.class, events.get(0).getClass());
			Assert.assertEquals(ppId, ((TiersChangeEvent) events.get(0)).getId());
		}
	}

	@Test
	public void testRegroupementEvenementsFiscauxALaFin() throws Exception {

		final class IdsTiers {
			long pp1;
			long pp2;
		}

		// création de la personne physique non-habitante
		final IdsTiers ids = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp1 = addNonHabitant("Alfredo", "De Montbuisson", null, Sexe.MASCULIN);
			final PersonnePhysique pp2 = addNonHabitant("Félicie", "D'Augustina", null, Sexe.FEMININ);

			final IdsTiers res = new IdsTiers();
			res.pp1 = pp1.getNumero();
			res.pp2 = pp2.getNumero();
			return res;
		});

		// mise en place du système de collecte
		final List<List<DataEvent>> collected = new ArrayList<>();
		buildConcentrator(buildCollectingSender(collected), true);

		final class IdsEvtsFiscaux {
			long evt1;
			long evt2;
		}

		// ajout d'un for principal sur l'entité créée au préalable
		final IdsEvtsFiscaux idsEvts = doInNewTransactionAndSession(new TxCallback<IdsEvtsFiscaux>() {
			@Override
			public IdsEvtsFiscaux execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp1 = (PersonnePhysique) tiersDAO.get(ids.pp1);
				Assert.assertNotNull(pp1);
				final ForFiscalPrincipal ffp1 = addForPrincipal(pp1, date(1995, 1, 1), MotifFor.ARRIVEE_HS, MockCommune.Echallens);
				final EvenementFiscalFor evtFiscal1 = addEvenementFiscalFor(pp1, ffp1, ffp1.getDateDebut(), EvenementFiscalFor.TypeEvenementFiscalFor.OUVERTURE);
				concentrator.sendEvent(evtFiscal1);

				final PersonnePhysique pp2 = (PersonnePhysique) tiersDAO.get(ids.pp2);
				Assert.assertNotNull(pp2);
				final ForFiscalPrincipal ffp2 = addForPrincipal(pp2, date(1998, 3, 1), MotifFor.ARRIVEE_HS, MockCommune.Echallens);
				final EvenementFiscalFor evtFiscal2 = addEvenementFiscalFor(pp2, ffp2, ffp2.getDateDebut(), EvenementFiscalFor.TypeEvenementFiscalFor.OUVERTURE);
				concentrator.sendEvent(evtFiscal2);

				final IdsEvtsFiscaux res = new IdsEvtsFiscaux();
				res.evt1 = evtFiscal1.getId();
				res.evt2 = evtFiscal2.getId();
				return res;
			}
		});

		// que s'est-il passé ?
		// normalement, on attend :
		// - un message pour la modification du tiers
		// - un événement fiscal pour l'ouverture du for
		Assert.assertEquals(1, collected.size());

		final List<DataEvent> events = collected.get(0);
		Assert.assertEquals(3, events.size());
		{
			Assert.assertEquals(TiersChangeEvent.class, events.get(0).getClass());
			Assert.assertEquals(ids.pp1, ((TiersChangeEvent) events.get(0)).getId());
		}
		{
			Assert.assertEquals(TiersChangeEvent.class, events.get(1).getClass());
			Assert.assertEquals(ids.pp2, ((TiersChangeEvent) events.get(1)).getId());
		}
		{
			Assert.assertEquals(FiscalEventSendRequestEvent.class, events.get(2).getClass());
			final FiscalEventSendRequestEvent evt = (FiscalEventSendRequestEvent) events.get(2);
			Assert.assertEquals(Arrays.asList(idsEvts.evt1, idsEvts.evt2), evt.getId());
		}
	}

	@Test
	public void testRolledBackTransaction() throws Exception {

		// création de la personne physique non-habitante
		final long ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addNonHabitant("Alfredo", "De Montbuisson", null, Sexe.MASCULIN);
			return pp.getNumero();
		});

		// mise en place du système de collecte
		final List<List<DataEvent>> collected = new ArrayList<>();
		buildConcentrator(buildCollectingSender(collected), true);

		// ajout d'un for principal sur l'entité créée au préalable
		final long idEvtFiscal = doInNewTransactionAndSession(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				Assert.assertNotNull(pp);
				final ForFiscalPrincipal ffp = addForPrincipal(pp, date(1995, 1, 1), MotifFor.ARRIVEE_HS, MockCommune.Echallens);
				final EvenementFiscalFor evtFiscal = addEvenementFiscalFor(pp, ffp, ffp.getDateDebut(), EvenementFiscalFor.TypeEvenementFiscalFor.OUVERTURE);
				concentrator.sendEvent(evtFiscal);

				// la transaction sera de toute façon annulée
				status.setRollbackOnly();

				return evtFiscal.getId();
			}
		});

		// que s'est-il passé ?
		// normalement, on attend :
		// - un message pour la modification du tiers
		// - un événement fiscal pour l'ouverture du for
		// mais comme la transaction a été annulée, on n'a rien du tout
		Assert.assertEquals(0, collected.size());
	}
}
