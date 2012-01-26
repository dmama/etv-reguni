package ch.vd.uniregctb.evenement.civil.engine.ech;

import java.util.concurrent.atomic.AtomicBoolean;

import junit.framework.Assert;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEch;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEchDAO;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.interfaces.model.mock.MockIndividu;
import ch.vd.uniregctb.interfaces.service.mock.DefaultMockServiceCivil;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.ActionEvenementCivilEch;
import ch.vd.uniregctb.type.EtatEvenementCivil;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.TypeEvenementCivilEch;

public class EvenementCivilEchProcessorTest extends BusinessTest {
	
	private EvenementCivilNotificationQueueImpl queue;
	private EvenementCivilEchProcessor processor;
	private EvenementCivilEchDAO evtCivilDAO;

	@Override
	protected void runOnSetUp() throws Exception {
		super.runOnSetUp();

		evtCivilDAO  = getBean(EvenementCivilEchDAO.class, "evenementCivilEchDAO");
		final EvenementCivilEchTranslator translator = getBean(EvenementCivilEchTranslator.class, "evenementCivilEchTranslator");

		queue = new EvenementCivilNotificationQueueImpl();
		queue.setEvtCivilDAO(evtCivilDAO);
		queue.setHibernateTemplate(hibernateTemplate);
		queue.setTransactionManager(transactionManager);
		queue.afterPropertiesSet();

		processor = new EvenementCivilEchProcessor();
		processor.setEvtCivilDAO(evtCivilDAO);
		processor.setNotificationQueue(queue);
		processor.setTransactionManager(transactionManager);
		processor.setTranslator(translator);
		processor.start();
	}

	@Override
	public void onTearDown() throws Exception {
		processor.stop();
		super.onTearDown();
	}

	private void traiterEvenement(long noIndividu, final long noEvenement) throws InterruptedException {
		// on se met en position pour voir quand le traitement aura été effectué
		final AtomicBoolean jobDone = new AtomicBoolean(false);
		processor.setMonitor(new EvenementCivilEchProcessor.ProcessingMonitor() {
			@Override
			public void onProcessingEnd(long evtId) {
				jobDone.set(evtId == noEvenement);
			}
		});

		// notification d'arrivée d'événement sur l'individu
		queue.add(noIndividu);

		// on attend que le traitement se fasse
		while (!jobDone.get()) {
			Thread.sleep(100L);
		}
	}

	@Test(timeout = 10000L)
	public void testNaissance() throws Exception {

		final long noIndividu = 54678215611L;
		final RegDate dateNaissance = RegDate.get().addMonths(-1);

		// le nouveau né apparaît au civil
		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
				addIndividu(noIndividu, dateNaissance, "Toubo", "Toupeti", true);
			}
		});

		// événement civil (avec individu déjà renseigné pour ne pas devoir appeler RCPers...)
		final long naissanceId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = new EvenementCivilEch();
				evt.setId(1235563456L);
				evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
				evt.setDateEvenement(dateNaissance);
				evt.setEtat(EtatEvenementCivil.A_TRAITER);
				evt.setNumeroIndividu(noIndividu);
				evt.setType(TypeEvenementCivilEch.NAISSANCE);

				final PersonnePhysique pp = tiersService.getPersonnePhysiqueByNumeroIndividu(noIndividu);
				Assert.assertNull(pp);

				return hibernateTemplate.merge(evt).getId();
			}
		});

		// traitement synchrone de l'événement
		traiterEvenement(noIndividu, naissanceId);

		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = evtCivilDAO.get(naissanceId);
				Assert.assertNotNull(evt);
				Assert.assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat());

				final PersonnePhysique pp = tiersService.getPersonnePhysiqueByNumeroIndividu(noIndividu);
				Assert.assertNotNull(pp);

				return null;
			}
		});
	}

	@Test(timeout = 10000L)
	public void testDeces() throws Exception {

		final long noIndividu = 267813451L;
		final RegDate dateNaissance = RegDate.get().addMonths(-1).addYears(-30);
		final RegDate dateDeces = RegDate.get().addMonths(-1);
		
		// mise en place de son vivant
		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
				addIndividu(noIndividu, dateNaissance, "Quatre", "Jessica", false);
			}
		});
		
		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addHabitant(noIndividu);
				addForPrincipal(pp, dateNaissance.addYears(18), MotifFor.MAJORITE, MockCommune.Lausanne);
				return pp.getNumero();
			}
		});
		
		// décès civil
		doModificationIndividu(noIndividu, new IndividuModification() {
			@Override
			public void modifyIndividu(MockIndividu individu) {
				individu.setDateDeces(dateDeces);
			}
		});
		
		// événement de décès
		final long decesId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = new EvenementCivilEch();
				evt.setId(67235L);
				evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
				evt.setDateEvenement(dateDeces);
				evt.setEtat(EtatEvenementCivil.A_TRAITER);
				evt.setNumeroIndividu(noIndividu);
				evt.setType(TypeEvenementCivilEch.DECES);
				return hibernateTemplate.merge(evt).getId();
			}
		});

		// traitement synchrone de l'événement
		traiterEvenement(noIndividu, decesId);
		
		// vérification du traitement de l'événement
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = evtCivilDAO.get(decesId);
				Assert.assertNotNull(evt);
				Assert.assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat());

				final PersonnePhysique pp = tiersService.getPersonnePhysiqueByNumeroIndividu(noIndividu);
				Assert.assertNotNull(pp);
				Assert.assertEquals((Long) ppId, pp.getNumero());
				
				final ForFiscalPrincipal ffp = pp.getDernierForFiscalPrincipal();
				Assert.assertNotNull(ffp);
				Assert.assertEquals(MotifFor.VEUVAGE_DECES, ffp.getMotifFermeture());
				Assert.assertEquals(dateDeces, ffp.getDateFin());
				return null;
			}
		});
	}
}
