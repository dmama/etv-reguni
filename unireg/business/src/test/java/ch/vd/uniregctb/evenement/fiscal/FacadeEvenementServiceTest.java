package ch.vd.uniregctb.evenement.fiscal;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.jms.listener.DefaultMessageListenerContainer;
import org.springframework.test.annotation.ExpectedException;
import org.springframework.test.context.ContextConfiguration;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.evenement.EvenementFiscal;
import ch.vd.uniregctb.evenement.EvenementFiscalDAO;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypeEvenementFiscal;

@ContextConfiguration(locations = {
		"classpath:ch/vd/uniregctb/evenement/fiscal/facade-evenement-service-test.xml"
})
public class FacadeEvenementServiceTest extends BusinessTest {

	private static final Logger LOGGER = Logger.getLogger(FacadeEvenementServiceTest.class);

	private final static String DB_UNIT_DATA_FILE = "classpath:ch/vd/uniregctb/evenement/fiscal/FacadeEvenementServiceTest.xml";
	private final static Long NUMERO_CONTRIBUABLE = 12300002L;
	private final static Long NUMERO_DPI = 12500001L;

	private MyMessageListener evenementFiscalMessageListener;
	private EvenementFiscalFacade evenementFiscalFacade;
	private EvenementFiscalService evenementFiscalService;
	private EvenementFiscalDAO evenementFiscalDAO;
	private TiersDAO tiersDAO;
	private DefaultMessageListenerContainer evenementFiscalListenerContainer;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		evenementFiscalMessageListener = getBean(MyMessageListener.class, "evenementFiscalMessageListener");
		evenementFiscalFacade = getBean(EvenementFiscalFacade.class, "evenementFiscalFacade");
		evenementFiscalService = getBean(EvenementFiscalService.class, "evenementFiscalService");
		evenementFiscalDAO = getBean(EvenementFiscalDAO.class, "evenementFiscalDAO");
		tiersDAO = getBean(TiersDAO.class, "tiersDAO");
		evenementFiscalListenerContainer = getBean(DefaultMessageListenerContainer.class, "evenementFiscalListenerContainer");

		loadDatabase(DB_UNIT_DATA_FILE);

		evenementFiscalMessageListener.reset();
	}

	@Test
	@ExpectedException(IllegalArgumentException.class)
	public void publierEvenementWithNull() throws Exception {
		EvenementFiscal evenement = null;
		evenementFiscalFacade.publierEvenement(evenement);
	}

	// FIXME (jec) faire fonctionner et s'assurer que l'événement est bien envoyé + existe en base"
	@Ignore
	@Test
	public void publierEvenementSituationFamille() throws Exception {

		Tiers tiers = addNonHabitant("Jean", "Test", date(1970, 1, 1), Sexe.MASCULIN);
		tiers.setNumero(NUMERO_CONTRIBUABLE);
		tiers = tiersDAO.save(tiers);

		EvenementFiscal evenement = evenementFiscalDAO.creerEvenementSituationFamille(tiers,
				TypeEvenementFiscal.CHANGEMENT_SITUATION_FAMILLE, RegDate.get(), new Long(1));
		evenementFiscalService.publierEvenementFiscal(evenement);
		Thread.sleep(1000);
		Assert.assertEquals(1, evenementFiscalMessageListener.counter);
	}

	// FIXME (jec) faire fonctionner et s'assurer que l'événement est bien envoyé + existe en base"
	@Ignore
	@Test
	public void publierEvenementForDPI() throws Exception {
		Tiers tiers = new DebiteurPrestationImposable();
		tiers.setNumero(NUMERO_DPI);
		tiers = tiersDAO.save(tiers);

		EvenementFiscal evenement = evenementFiscalDAO.creerEvenementFor(tiers, TypeEvenementFiscal.OUVERTURE_FOR,
				RegDate.get(), null, null, new Long(1));
		evenementFiscalService.publierEvenementFiscal(evenement);
		Thread.sleep(1000);
		Assert.assertEquals(1, evenementFiscalMessageListener.counter);
	}

	// FIXME (jec) faire fonctionner et s'assurer que l'événement est bien envoyé + existe en base"
	@Ignore
	@Test
	public void publierEvenementWithoutListener() throws Exception {

		evenementFiscalListenerContainer.stop();

		Tiers tiers = new Entreprise();
		tiers.setNumero(NUMERO_CONTRIBUABLE);
		tiers = tiersDAO.save(tiers);

		EvenementFiscal evenement = evenementFiscalDAO.creerEvenementSituationFamille(tiers, TypeEvenementFiscal.CHANGEMENT_SITUATION_FAMILLE, RegDate.get(), new Long(1));
		evenementFiscalService.publierEvenementFiscal(evenement);

		Assert.assertEquals(0, evenementFiscalMessageListener.counter);
		evenementFiscalListenerContainer.start();
		waitUntilCounter(1);
		Assert.assertEquals(1, evenementFiscalMessageListener.counter);
	}

	private void waitUntilCounter(int value) throws Exception {

		final int max = 100; // 30 secondes
		int nbTimes = 0;
		while (evenementFiscalMessageListener.counter != value && nbTimes < max) {
			Thread.sleep(300);
			nbTimes++;
		}
		nbTimes = 0;
	}


	public static class MyMessageListener implements org.springframework.jms.listener.SessionAwareMessageListener {

		private int counter = 0;

		public MyMessageListener() {
		}

		public void reset() {
			counter = 0;
		}

		public void onMessage(Message msg, Session session) throws JMSException {
			String  id= msg.getJMSCorrelationID();
			LOGGER.debug("CorrelationId: " + id);
			String msgId =msg.getJMSMessageID();
			LOGGER.debug("MessageId: " + msgId);
			Long time = msg.getJMSTimestamp();
			LOGGER.debug("Timestamp: " + time);
			counter++;
		}

	}

}
