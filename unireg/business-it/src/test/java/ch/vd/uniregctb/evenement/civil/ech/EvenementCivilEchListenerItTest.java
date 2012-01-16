package ch.vd.uniregctb.evenement.civil.ech;

import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.dao.DataAccessException;
import org.springframework.jms.connection.JmsTransactionManager;
import org.springframework.orm.hibernate3.HibernateTemplate;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.technical.esb.jms.EsbJmsTemplate;
import ch.vd.technical.esb.store.raft.RaftEsbStore;
import ch.vd.uniregctb.common.BusinessItTest;
import ch.vd.uniregctb.evenement.EvenementTest;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilException;
import ch.vd.uniregctb.type.ActionEvenementCivilEch;
import ch.vd.uniregctb.type.EtatEvenementCivil;
import ch.vd.uniregctb.type.TypeEvenementCivilEch;

public class EvenementCivilEchListenerItTest extends EvenementTest {

	private String INPUT_QUEUE;
	private EvenementCivilEchSenderImpl sender;
	private EvenementCivilEchListener listener;
	private List<EvenementCivilEch> evenementsRecus;


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
		if (esbTemplate instanceof InitializingBean) {
			((InitializingBean) esbTemplate).afterPropertiesSet();
		}

		clearQueue(INPUT_QUEUE);

		// flush est vraiment la seule méthode appelée...
		final HibernateTemplate hibernateTemplate = new HibernateTemplate() {
			@Override
			public void flush() throws DataAccessException {
			}
		};

		evenementsRecus = new ArrayList<EvenementCivilEch>();
		final EvenementCivilEchReceptionHandler receptionHandler = new EvenementCivilEchReceptionHandler() {
			@Override
			public EvenementCivilEch saveIncomingEvent(EvenementCivilEch event) {
				// pas de sauvegarde ici...
				return event;
			}

			@Override
			public EvenementCivilEch handleEvent(EvenementCivilEch event) throws EvenementCivilException {
				evenementsRecus.add(event);
				return event;
			}
		};

		listener = new EvenementCivilEchListener();
		listener.setFetchEventsOnStartup(false);
		listener.setEvtCivilDAO(null);      // pas nécessaire tant qu'on ne va pas chercher les événement à relancer
		listener.setIgnoredEventTypes(null);
		listener.setReceptionHandler(receptionHandler);
		listener.setTransactionManager(new JmsTransactionManager(jmsConnectionFactory));
		listener.setEsbTemplate(esbTemplate);
		listener.afterPropertiesSet();
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
		evt.setNumeroContribuablePersonnePhysique(426721527L);
		evt.setNumeroIndividu(23153612L);
		evt.setRefMessageId(refMessageId);
		evt.setType(type);

		Assert.assertEquals(0, evenementsRecus.size());
		sender.sendEvent(evt, "toto");

		// On attend le message
		while (evenementsRecus.isEmpty()) {
			Thread.sleep(100);
		}
		Assert.assertEquals(1, evenementsRecus.size());

		final EvenementCivilEch recu = evenementsRecus.get(0);
		Assert.assertNotNull(recu);
		Assert.assertEquals(idEvenement, recu.getId());
		Assert.assertEquals(action, recu.getAction());
		Assert.assertEquals(dateEvenement, recu.getDateEvenement());
		Assert.assertNull(recu.getCommentaireTraitement());
		Assert.assertNull(recu.getDateTraitement());
		Assert.assertEquals(EtatEvenementCivil.A_TRAITER, recu.getEtat());
		Assert.assertNull(recu.getNumeroContribuablePersonnePhysique());
		Assert.assertNull(recu.getNumeroIndividu());
		Assert.assertEquals(refMessageId, recu.getRefMessageId());
		Assert.assertEquals(type, recu.getType());
	}
}
