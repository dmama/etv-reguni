package ch.vd.uniregctb.evenement.fiscal;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.tx.TxCallbackWithoutResult;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.data.DataEventSender;
import ch.vd.uniregctb.hibernate.interceptor.ModificationInterceptor;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.Sexe;

public class DelegatingEvenementFiscalSenderTest extends BusinessTest {

	private DelegatingEvenementFiscalSender sender;
	private MyDataEventSender dataEventSender;

	private static class MyDataEventSender implements DataEventSender {

		final List<List<Long>> collectedCalls = new LinkedList<>();

		@Override
		public void sendEvenementsFiscaux(List<Long> idsEvenementsFiscaux) {
			collectedCalls.add(idsEvenementsFiscaux);
		}
	}

	@Override
	protected void runOnSetUp() throws Exception {
		super.runOnSetUp();
		this.dataEventSender = new MyDataEventSender();
	}

	@Override
	public void onTearDown() throws Exception {
		if (this.sender != null) {
			this.sender.destroy();
		}
		super.onTearDown();
	}

	private void buildDelegatingSender(boolean enabled) throws Exception {
		final DelegatingEvenementFiscalSender sender = new DelegatingEvenementFiscalSender();
		sender.setEnabled(enabled);
		sender.setParent(getBean(ModificationInterceptor.class, "modificationInterceptor"));
		sender.setDataEventSender(dataEventSender);
		sender.afterPropertiesSet();
		this.sender = sender;
	}

	@Test
	public void testEmptyTransaction() throws Exception {

		// active sender
		buildDelegatingSender(true);

		// création d'une entité, mais sans envoi d'événement fiscal
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
				addNonHabitant("Alfredo", "Alfredi", null, Sexe.MASCULIN);
			}
		});

		// vérification que rien n'a été reçu
		Assert.assertEquals(0, dataEventSender.collectedCalls.size());
	}

	@Test
	public void testRolledBackTransaction() throws Exception {

		// active sender
		buildDelegatingSender(true);

		// création d'une entité, avec envoi d'événement fiscal mais transaction marquée à annuler
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = addNonHabitant("Alfredo", "Alfredi", null, Sexe.MASCULIN);
				final ForFiscalPrincipal ffp = addForPrincipal(pp, date(2010, 1, 1), MotifFor.ARRIVEE_HS, MockCommune.Cossonay);

				final EvenementFiscal evtFiscal = addEvenementFiscalFor(pp, ffp, date(2010, 1, 1), EvenementFiscalFor.TypeEvenementFiscalFor.OUVERTURE);
				sender.sendEvent(evtFiscal);

				// rollback only
				status.setRollbackOnly();
			}
		});

		// vérification que rien n'a été reçu (la transaction a été annulée)
		Assert.assertEquals(0, dataEventSender.collectedCalls.size());
	}

	@Test
	public void testCommittedTransactionAvecPlusieursEvenementsFiscaux() throws Exception {

		// active sender
		buildDelegatingSender(true);

		// création d'une entité, avec envoi de deux événements fiscaux
		final List<Long> ids = doInNewTransactionAndSession(new TxCallback<List<Long>>() {
			@Override
			public List<Long> execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = addNonHabitant("Alfredo", "Alfredi", null, Sexe.MASCULIN);

				final RegDate dateOuvertureFor = date(2010, 1, 1);
				final RegDate dateFermetureFor = date(2016, 2, 1);
				final ForFiscalPrincipal ffp = addForPrincipal(pp, dateOuvertureFor, MotifFor.ARRIVEE_HS, dateFermetureFor, MotifFor.DEPART_HS, MockCommune.Cossonay);

				final EvenementFiscal evtFiscalOuverture = addEvenementFiscalFor(pp, ffp, dateOuvertureFor, EvenementFiscalFor.TypeEvenementFiscalFor.OUVERTURE);
				final EvenementFiscal evtFiscalFermeture = addEvenementFiscalFor(pp, ffp, dateFermetureFor, EvenementFiscalFor.TypeEvenementFiscalFor.FERMETURE);

				// volontairement dans l'ordre inverse pour bien vérifier que cet ordre est conservé
				sender.sendEvent(evtFiscalFermeture);
				sender.sendEvent(evtFiscalOuverture);

				// l'ordre des identifiants ici doit être le même que l'ordre des envois dans le "sender", pour que ce test soit pertinent
				return Arrays.asList(evtFiscalFermeture.getId(), evtFiscalOuverture.getId());
			}
		});

		// vérification qu'un seul appel a été fait, avec les deux identifiants dedans
		Assert.assertEquals(1, dataEventSender.collectedCalls.size());
		Assert.assertEquals(ids, dataEventSender.collectedCalls.get(0));
	}

	@Test
	public void testCommittedTransactionAvecPlusieursEvenementsFiscauxMaisEnvoiDesactive() throws Exception {

		// inactive sender
		buildDelegatingSender(false);

		// création d'une entité, avec envoi de deux événements fiscaux
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = addNonHabitant("Alfredo", "Alfredi", null, Sexe.MASCULIN);

				final RegDate dateOuvertureFor = date(2010, 1, 1);
				final RegDate dateFermetureFor = date(2016, 2, 1);
				final ForFiscalPrincipal ffp = addForPrincipal(pp, dateOuvertureFor, MotifFor.ARRIVEE_HS, dateFermetureFor, MotifFor.DEPART_HS, MockCommune.Cossonay);

				final EvenementFiscal evtFiscalOuverture = addEvenementFiscalFor(pp, ffp, dateOuvertureFor, EvenementFiscalFor.TypeEvenementFiscalFor.OUVERTURE);
				final EvenementFiscal evtFiscalFermeture = addEvenementFiscalFor(pp, ffp, dateFermetureFor, EvenementFiscalFor.TypeEvenementFiscalFor.FERMETURE);

				// volontairement dans l'ordre inverse pour bien vérifier que cet ordre est conservé
				sender.sendEvent(evtFiscalFermeture);
				sender.sendEvent(evtFiscalOuverture);
			}
		});

		// vérification qu'aucun appel n'a été propagé
		Assert.assertEquals(0, dataEventSender.collectedCalls.size());
	}
}
