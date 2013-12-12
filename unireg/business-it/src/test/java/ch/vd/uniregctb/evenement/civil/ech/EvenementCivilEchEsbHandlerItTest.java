package ch.vd.uniregctb.evenement.civil.ech;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.jms.connection.JmsTransactionManager;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.technical.esb.EsbMessage;
import ch.vd.technical.esb.jms.EsbJmsTemplate;
import ch.vd.technical.esb.store.raft.RaftEsbStore;
import ch.vd.uniregctb.common.BusinessItTest;
import ch.vd.uniregctb.evenement.EvenementTest;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilException;
import ch.vd.uniregctb.jms.EsbBusinessException;
import ch.vd.uniregctb.jms.GentilEsbMessageEndpointListener;
import ch.vd.uniregctb.type.ActionEvenementCivilEch;
import ch.vd.uniregctb.type.EtatEvenementCivil;
import ch.vd.uniregctb.type.TypeEvenementCivilEch;

public class EvenementCivilEchEsbHandlerItTest extends EvenementTest {

	private String INPUT_QUEUE;
	private EvenementCivilEchSenderImpl sender;
	private EvenementCivilEchEsbHandler esbHandler;
	private List<EvenementCivilEch> evenementsTraites;
	private List<EvenementCivilEch> evenementsIgnores;
	private List<String> evenementsVusPasser;

	private static final Set<TypeEvenementCivilEch> IGNORED = EnumSet.of(TypeEvenementCivilEch.CHGT_BLOCAGE_ADRESSE,
	                                                                     TypeEvenementCivilEch.CHGT_RELIGION,
	                                                                     TypeEvenementCivilEch.CORR_RELIGION,
	                                                                     TypeEvenementCivilEch.CORR_LIEU_NAISSANCE);

	@Before
	public void setup() throws Exception {

		INPUT_QUEUE = uniregProperties.getProperty("testprop.jms.queue.evtCivilEch");

		final RaftEsbStore esbStore = new RaftEsbStore();
		esbStore.setEndpoint("TestRaftStore");

		esbTemplate = new EsbJmsTemplate();
		esbTemplate.setConnectionFactory(jmsConnectionFactory);
		esbTemplate.setEsbStore(esbStore);
		esbTemplate.setReceiveTimeout(200);
		esbTemplate.setApplication("unireg");
		esbTemplate.setDomain("fiscalite");
		esbTemplate.setSessionTransacted(true);
		if (esbTemplate instanceof InitializingBean) {
			((InitializingBean) esbTemplate).afterPropertiesSet();
		}

		clearQueue(INPUT_QUEUE);

		evenementsTraites = new ArrayList<>();
		final EvenementCivilEchReceptionHandler receptionHandler = new EvenementCivilEchReceptionHandler() {
			@Override
			public EvenementCivilEch saveIncomingEvent(EvenementCivilEch event) {
				// pas de sauvegarde ici...
				return event;
			}

			@Override
			public EvenementCivilEch handleEvent(EvenementCivilEch event, EvenementCivilEchProcessingMode mode) throws EvenementCivilException {
				evenementsTraites.add(event);
				return event;
			}
		};

		evenementsVusPasser = new ArrayList<>();
		evenementsIgnores = new ArrayList<>();
		esbHandler = new EvenementCivilEchEsbHandler() {
			@Override
			public void onEsbMessage(EsbMessage message) throws EsbBusinessException {
				try {
					super.onEsbMessage(message);
				}
				finally {
					synchronized (evenementsVusPasser) {
						evenementsVusPasser.add(message.getBusinessId());
						evenementsVusPasser.notifyAll();
					}
				}
			}

			@Override
			protected void onIgnoredEvent(EvenementCivilEch evt) {
				super.onIgnoredEvent(evt);
				evenementsIgnores.add(evt);
			}
		};
		esbHandler.setRecuperateur(null);
		esbHandler.setIgnoredEventTypes(IGNORED);
		esbHandler.setReceptionHandler(receptionHandler);
		esbHandler.afterPropertiesSet();

		final GentilEsbMessageEndpointListener listener = new GentilEsbMessageEndpointListener();
		listener.setTransactionManager(new JmsTransactionManager(jmsConnectionFactory));
		listener.setEsbTemplate(esbTemplate);
		listener.setHandler(esbHandler);

		initEndpointManager(INPUT_QUEUE, listener);

		sender = new EvenementCivilEchSenderImpl();
		sender.setEsbTemplate(esbTemplate);
		sender.setOutputQueue(INPUT_QUEUE);
		sender.afterPropertiesSet();
	}

	@Test(timeout = BusinessItTest.JMS_TIMEOUT)
	public void testReceptionEvenement() throws Exception {
		final Long idEvenement = 34674524122L;
		final RegDate dateEvenement = RegDate.get();
		final Long refMessageId = 12L;
		final ActionEvenementCivilEch action = ActionEvenementCivilEch.PREMIERE_LIVRAISON;
		final TypeEvenementCivilEch type = TypeEvenementCivilEch.CHGT_RELATION_ANNONCE;

		final EvenementCivilEch evt = new EvenementCivilEch();
		evt.setId(idEvenement);
		evt.setAction(action);
		evt.setDateEvenement(dateEvenement);
		evt.setCommentaireTraitement("turlututu");
		evt.setDateTraitement(DateHelper.getCurrentDate());
		evt.setEtat(EtatEvenementCivil.A_VERIFIER);
		evt.setNumeroIndividu(23153612L);
		evt.setRefMessageId(refMessageId);
		evt.setType(type);

		Assert.assertEquals(0, evenementsTraites.size());
		sender.sendEvent(evt, "toto");

		// On attend le message
		synchronized (evenementsVusPasser) {
			while (evenementsVusPasser.size() == 0) {
				evenementsVusPasser.wait();
			}
		}
		Assert.assertEquals(1, evenementsVusPasser.size());
		Assert.assertEquals(1, evenementsTraites.size());

		final EvenementCivilEch recu = evenementsTraites.get(0);
		Assert.assertNotNull(recu);
		Assert.assertEquals(idEvenement, recu.getId());
		Assert.assertEquals(action, recu.getAction());
		Assert.assertEquals(dateEvenement, recu.getDateEvenement());
		Assert.assertNull(recu.getCommentaireTraitement());
		Assert.assertNull(recu.getDateTraitement());
		Assert.assertEquals(EtatEvenementCivil.A_TRAITER, recu.getEtat());
		Assert.assertNull(recu.getNumeroIndividu());
		Assert.assertEquals(refMessageId, recu.getRefMessageId());
		Assert.assertEquals(type, recu.getType());

		Assert.assertEquals(0, evenementsIgnores.size());
	}

	@Test(timeout = BusinessItTest.JMS_TIMEOUT)
	public void testEvenementsIgnores() throws Exception {

		for (TypeEvenementCivilEch type : IGNORED) {

			evenementsVusPasser.clear();
			evenementsTraites.clear();
			evenementsIgnores.clear();

			final Long idEvenement = 34674524122L + type.ordinal();
			final RegDate dateEvenement = RegDate.get();
			final Long refMessageId = 12L;
			final ActionEvenementCivilEch action = ActionEvenementCivilEch.PREMIERE_LIVRAISON;

			final EvenementCivilEch evt = new EvenementCivilEch();
			evt.setId(idEvenement);
			evt.setAction(action);
			evt.setDateEvenement(dateEvenement);
			evt.setCommentaireTraitement("turlututu");
			evt.setDateTraitement(DateHelper.getCurrentDate());
			evt.setEtat(EtatEvenementCivil.A_VERIFIER);
			evt.setNumeroIndividu(23153612L);
			evt.setRefMessageId(refMessageId);
			evt.setType(type);

			Assert.assertEquals(0, evenementsTraites.size());
			sender.sendEvent(evt, "toto");

			// On attend le message
			synchronized (evenementsVusPasser) {
				while (evenementsVusPasser.size() == 0) {
					evenementsVusPasser.wait();
				}
			}
			Assert.assertEquals("type " + type, 1, evenementsVusPasser.size());
			Assert.assertEquals("type " + type, 0, evenementsTraites.size());
			Assert.assertEquals("type " + type, 1, evenementsIgnores.size());

			final EvenementCivilEch recu = evenementsIgnores.get(0);
			Assert.assertNotNull("type " + type, recu);
			Assert.assertEquals("type " + type, idEvenement, recu.getId());
			Assert.assertEquals("type " + type, action, recu.getAction());
			Assert.assertEquals("type " + type, dateEvenement, recu.getDateEvenement());
			Assert.assertNull("type " + type, recu.getCommentaireTraitement());
			Assert.assertNull("type " + type, recu.getDateTraitement());
			Assert.assertEquals("type " + type, EtatEvenementCivil.A_TRAITER, recu.getEtat());
			Assert.assertNull("type " + type, recu.getNumeroIndividu());
			Assert.assertEquals("type " + type, refMessageId, recu.getRefMessageId());
			Assert.assertEquals("type " + type, type, recu.getType());
		}
	}
}
