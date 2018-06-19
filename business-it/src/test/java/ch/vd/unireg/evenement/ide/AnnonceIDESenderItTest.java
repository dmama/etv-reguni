package ch.vd.unireg.evenement.ide;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.registre.base.date.RegDate;
import ch.vd.technical.esb.jms.EsbJmsTemplate;
import ch.vd.technical.esb.store.raft.RaftEsbStore;
import ch.vd.unireg.common.AuthenticationHelper;
import ch.vd.unireg.evenement.EvenementTest;
import ch.vd.unireg.interfaces.entreprise.data.AnnonceIDE;
import ch.vd.unireg.interfaces.entreprise.data.AnnonceIDEData;
import ch.vd.unireg.interfaces.entreprise.data.FormeLegale;
import ch.vd.unireg.interfaces.entreprise.data.TypeAnnonce;
import ch.vd.unireg.interfaces.entreprise.data.TypeEtablissementCivil;
import ch.vd.unireg.interfaces.entreprise.rcent.RCEntAnnonceIDEHelper;
import ch.vd.unireg.interfaces.entreprise.rcent.RCEntSchemaHelper;

/**
 * @author Raphaël Marmier, 2016-08-17, <raphael.marmier@vd.ch>
 */
public class AnnonceIDESenderItTest extends EvenementTest {

	private final String OUTPUT_QUEUE = uniregProperties.getProperty("testprop.jms.queue.evtRapportAnnonceIDE");
	private AnnonceIDESenderImpl sender;

	@Before
	public void setUp() throws Exception {
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

		clearQueue(OUTPUT_QUEUE);

		buildEsbMessageValidator(RCEntSchemaHelper.getRCEntSchemaClassPathResource());

		sender = new AnnonceIDESenderImpl();
		sender.setServiceDestination("test");
		sender.setOutputQueue(OUTPUT_QUEUE);
		sender.setEsbTemplate(esbTemplate);
		sender.setEsbValidator(esbValidator);
		sender.setEnabled(true);
		if (sender instanceof InitializingBean) {
			((InitializingBean) sender).afterPropertiesSet();
		}
	}

	@Test
	public void testSendEvent() throws Exception {

		final String EXPECTED = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><eVD-0024-3:noticeRequest xmlns:eVD-0024-3=\"http://evd.vd.ch/xmlns/eVD-0024/3\" xmlns:eCH-0007-6=\"http://www.ech.ch/xmlns/eCH-0007/6\" xmlns:eCH-0010-6=\"http://www.ech.ch/xmlns/eCH-0010/6\" xmlns:eCH-0044-4=\"http://www.ech.ch/xmlns/eCH-0044/4\" xmlns:eCH-0097-2=\"http://www.ech.ch/xmlns/eCH-0097/2\" xmlns:eVD-0004-3=\"http://evd.vd.ch/xmlns/eVD-0004/3\" xmlns:eVD-0022-3=\"http://evd.vd.ch/xmlns/eVD-0022/3\"><eVD-0022-3:noticeRequestHeader><eVD-0022-3:noticeRequestIdentification><eVD-0022-3:noticeRequestId>123456</eVD-0022-3:noticeRequestId><eVD-0022-3:typeOfNoticeRequest>1</eVD-0022-3:typeOfNoticeRequest><eVD-0022-3:reportingApplication><eVD-0022-3:id>2</eVD-0022-3:id><eVD-0022-3:applicationName>UNIREG</eVD-0022-3:applicationName></eVD-0022-3:reportingApplication><eVD-0022-3:noticeRequestDateTime>2016-08-19T00:00:00</eVD-0022-3:noticeRequestDateTime><eVD-0022-3:IDESource><eCH-0097-2:organisationIdCategory>CH.IDE</eCH-0097-2:organisationIdCategory><eCH-0097-2:organisationId>CHE322886489</eCH-0097-2:organisationId></eVD-0022-3:IDESource></eVD-0022-3:noticeRequestIdentification><eVD-0022-3:userId>c4zem2</eVD-0022-3:userId><eVD-0022-3:comment>Ceci est une annonce de test.</eVD-0022-3:comment></eVD-0022-3:noticeRequestHeader><eVD-0022-3:noticeRequestBody><eVD-0022-3:typeOfLocation>1</eVD-0022-3:typeOfLocation></eVD-0022-3:noticeRequestBody></eVD-0024-3:noticeRequest>";

		AuthenticationHelper.pushPrincipal("annonceIDESenderImplItTest");

		try {
			clearQueue(OUTPUT_QUEUE);

			final AnnonceIDE
					annonceIDE = new AnnonceIDE(123456L, TypeAnnonce.CREATION, RegDate.get(2016, 8, 19).asJavaDate(), new AnnonceIDEData.UtilisateurImpl("c4zem2", null), TypeEtablissementCivil.ETABLISSEMENT_PRINCIPAL, null,
					                            new AnnonceIDEData.InfoServiceIDEObligEtenduesImpl(RCEntAnnonceIDEHelper.NO_IDE_ADMINISTRATION_CANTONALE_DES_IMPOTS, RCEntAnnonceIDEHelper.NO_APPLICATION_UNIREG, RCEntAnnonceIDEHelper.NOM_APPLICATION_UNIREG));
			annonceIDE.setCommentaire("Ceci est une annonce de test.");
			final AnnonceIDEData.ContenuImpl contenu = new AnnonceIDEData.ContenuImpl();
			contenu.setNom("Synergy SA");
			contenu.setFormeLegale(FormeLegale.N_0109_ASSOCIATION);

			sender.sendEvent(annonceIDE, "businessIdDeTestAnnonceIDE");
			assertTextMessage(OUTPUT_QUEUE, EXPECTED);
		}
		finally {
			AuthenticationHelper.popPrincipal();
		}
	}

	@Test
	public void testSendEventVide() throws Exception {

		AuthenticationHelper.pushPrincipal("annonceIDESenderImplItTest");

		try {
			clearQueue(OUTPUT_QUEUE);

			sender.sendEvent(null, "businessIdDeTestAnnonceIDE");

			Assert.fail();
		}
		catch (IllegalArgumentException e) {
			Assert.assertEquals("Contenu de l'annonce manquant.", e.getMessage());
		}
		finally {
			AuthenticationHelper.popPrincipal();
		}
	}
}