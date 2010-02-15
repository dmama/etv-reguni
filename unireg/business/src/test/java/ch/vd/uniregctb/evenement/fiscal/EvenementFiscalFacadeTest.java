package ch.vd.uniregctb.evenement.fiscal;

import java.io.StringWriter;
import java.io.Writer;

import junit.framework.Assert;

import org.apache.xmlbeans.XmlOptions;
import org.easymock.EasyMock;
import org.junit.Test;
import org.springframework.jms.core.JmsOperations;
import org.springframework.jms.core.MessageCreator;
import org.springframework.test.annotation.ExpectedException;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.evenement.EvenementFiscal;
import ch.vd.uniregctb.evenement.EvenementFiscalDAO;
import ch.vd.uniregctb.evenement.EvenementFiscalSituationFamille;
import ch.vd.uniregctb.evenement.fiscal.jms.EvenementFiscalFacadeImpl;
import ch.vd.uniregctb.parametrage.ParametreAppService;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.TypeEvenementFiscal;

public class EvenementFiscalFacadeTest extends BusinessTest {

	private final static String DB_UNIT_DATA_FILE = "classpath:ch/vd/uniregctb/evenement/fiscal/EvenementFiscalServiceTest.xml";
	private final static Long NUMERO_CONTRIBUABLE = 12300002L;

	private EvenementFiscalServiceImpl evenementFiscalService;
	private EvenementFiscalFacadeImpl evenementFiscalFacade;
	private EvenementFiscalDAO evenementFiscalDAO;
	private JmsOperations jmsOperations;
	private TiersDAO tiersDAO;


	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		evenementFiscalDAO = getBean(EvenementFiscalDAO.class, "evenementFiscalDAO");
		tiersDAO = getBean(TiersDAO.class, "tiersDAO");

		evenementFiscalService = new EvenementFiscalServiceImpl();

		evenementFiscalService.setEvenementFiscalDAO(evenementFiscalDAO);
		evenementFiscalFacade = new EvenementFiscalFacadeImpl();
		jmsOperations = EasyMock.createMock(JmsOperations.class);
		evenementFiscalService.setEvenementFiscalFacade(evenementFiscalFacade);
		evenementFiscalFacade.setPublisher(jmsOperations);

		final ParametreAppService parametres = getBean(ParametreAppService.class, "parametreAppService");
		evenementFiscalService.setParametres(parametres);

		loadDatabase(DB_UNIT_DATA_FILE);
	}

	@Test
	public void publierEvenementSerialiserEvenement() throws Exception {
		TypeEvenementFiscal typeEvenement = TypeEvenementFiscal.CHANGEMENT_SITUATION_FAMILLE;
		Tiers tiers = tiersDAO.get(NUMERO_CONTRIBUABLE);
		EvenementFiscalSituationFamille evenementSituationFamille = evenementFiscalDAO.creerEvenementSituationFamille(tiers, typeEvenement, RegDate.get(), new Long(1));
		evenementSituationFamille = (EvenementFiscalSituationFamille) evenementFiscalDAO.save(evenementSituationFamille);
		ch.vd.fiscalite.registre.evenementFiscalV1.EvenementFiscalSituationFamilleType evt = evenementFiscalFacade
				.creerEvenementFiscal(evenementSituationFamille);
		Assert.assertTrue(evt.validate());
		Writer writer = new StringWriter();
		evt.save(writer, new XmlOptions().setSaveOuter());
		Assert.assertTrue(writer.toString().length() > 0);
	}

	@Test
	public void publierEvenement() throws Exception {
		jmsOperations.send((MessageCreator) EasyMock.anyObject());
		EasyMock.replay(jmsOperations);

		TypeEvenementFiscal typeEvenement = TypeEvenementFiscal.OUVERTURE_FOR;
		Tiers tiers = tiersDAO.get(NUMERO_CONTRIBUABLE);
		EvenementFiscal evenementFiscal = evenementFiscalDAO.creerEvenementFor(tiers, typeEvenement, RegDate.get(), MotifFor.ARRIVEE_HS, null, new Long(1));
		evenementFiscal = evenementFiscalDAO.save(evenementFiscal);
		evenementFiscalFacade.publierEvenement(evenementFiscal);
	}

	@Test
	@ExpectedException(IllegalArgumentException.class)
	public void publierEvenementArgumentNull() throws Exception {
		evenementFiscalFacade.publierEvenement(null);
	}

	@Test
	@ExpectedException(EvenementFiscalException.class)
	public void publierEvenementInError() throws Exception {
		jmsOperations.send((MessageCreator) EasyMock.anyObject());
		EasyMock.expectLastCall().andThrow(new RuntimeException("error"));
		EasyMock.replay(jmsOperations);

		TypeEvenementFiscal typeEvenement = TypeEvenementFiscal.OUVERTURE_FOR;
		Tiers tiers = tiersDAO.get(NUMERO_CONTRIBUABLE);
		EvenementFiscal evenementFiscal = evenementFiscalDAO.creerEvenementFor(tiers, typeEvenement, RegDate.get(), MotifFor.ARRIVEE_HS, null, new Long(1));
		evenementFiscal = evenementFiscalDAO.save(evenementFiscal);
		evenementFiscalFacade.publierEvenement(evenementFiscal);
	}


}
