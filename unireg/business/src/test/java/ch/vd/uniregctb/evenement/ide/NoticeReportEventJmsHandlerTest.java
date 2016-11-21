package ch.vd.uniregctb.evenement.ide;

import org.junit.Before;
import org.junit.Test;

import ch.vd.technical.esb.EsbMessage;
import ch.vd.technical.esb.EsbMessageFactory;
import ch.vd.unireg.interfaces.organisation.data.AnnonceIDEEnvoyee;
import ch.vd.unireg.interfaces.organisation.data.BaseAnnonceIDE;
import ch.vd.unireg.interfaces.organisation.data.NumeroIDE;
import ch.vd.unireg.interfaces.organisation.data.StatutAnnonce;
import ch.vd.unireg.interfaces.organisation.data.TypeAnnonce;
import ch.vd.uniregctb.common.WithoutSpringTest;
import ch.vd.uniregctb.hibernate.HibernateTemplateImpl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Raphaël Marmier, 2016-08-17, <raphael.marmier@vd.ch>
 */
public class NoticeReportEventJmsHandlerTest extends WithoutSpringTest {

	private NoticeReportEventJmsHandler handler;

	private final SingleShotMockRapportAnnonceIDEProcessor rapportAnnonceIDEProcessor = new SingleShotMockRapportAnnonceIDEProcessor();

	@Before
	public void setUp() throws Exception {
		handler = new NoticeReportEventJmsHandler();
		handler.setReponseIDEProcessor(rapportAnnonceIDEProcessor);
		handler.setHibernateTemplate(new HibernateTemplateImpl() {
			@Override
			public void flush() {
			}
		});
		handler.afterPropertiesSet();
	}

	@Test
	public void testRapportSansErreur() throws Exception {
		final String message = "<eVD-0024:noticeRequestReport xmlns:eVD-0024=\"http://evd.vd.ch/xmlns/eVD-0024/3\">\n" +
				"\t<noticeRequest xmlns=\"http://evd.vd.ch/xmlns/eVD-0022/3\">\n" +
				"\t\t<eVD-0022-3:noticeRequestHeader xmlns:eVD-0022-3=\"http://evd.vd.ch/xmlns/eVD-0022/3\">\n" +
				"\t\t\t<eVD-0022-3:noticeRequestIdentification>\n" +
				"\t\t\t\t<eVD-0022-3:noticeRequestId>1000</eVD-0022-3:noticeRequestId>\n" +
				"\t\t\t\t<eVD-0022-3:typeOfNoticeRequest>1</eVD-0022-3:typeOfNoticeRequest>\n" +
				"\t\t\t\t<eVD-0022-3:reportingApplication>\n" +
				"\t\t\t\t\t<eVD-0022-3:id>1</eVD-0022-3:id>\n" +
				"\t\t\t\t\t<eVD-0022-3:applicationName>UNIREG</eVD-0022-3:applicationName>\n" +
				"\t\t\t\t</eVD-0022-3:reportingApplication>\n" +
				"\t\t\t\t<eVD-0022-3:noticeRequestDateTime>2016-09-15T10:30:53</eVD-0022-3:noticeRequestDateTime>\n" +
				"\t\t\t\t<eVD-0022-3:IDESource>\n" +
				"\t\t\t\t\t<eCH-0097-2:organisationIdCategory xmlns:eCH-0097-2=\"http://www.ech.ch/xmlns/eCH-0097/2\">CH.IDE</eCH-0097-2:organisationIdCategory>\n" +
				"\t\t\t\t\t<eCH-0097-2:organisationId xmlns:eCH-0097-2=\"http://www.ech.ch/xmlns/eCH-0097/2\">CHE999999996</eCH-0097-2:organisationId>\n" +
				"\t\t\t\t</eVD-0022-3:IDESource>\n" +
				"\t\t\t</eVD-0022-3:noticeRequestIdentification>\n" +
				"\t\t</eVD-0022-3:noticeRequestHeader>\n" +
				"\t\t<eVD-0022-3:noticeRequestBody xmlns:eVD-0022-3=\"http://evd.vd.ch/xmlns/eVD-0022/3\">\n" +
				"\t\t\t<eVD-0022-3:uid>\n" +
				"\t\t\t\t<eCH-0097-2:uidOrganisationIdCategorie xmlns:eCH-0097-2=\"http://www.ech.ch/xmlns/eCH-0097/2\">CHE</eCH-0097-2:uidOrganisationIdCategorie>\n" +
				"\t\t\t\t<eCH-0097-2:uidOrganisationId xmlns:eCH-0097-2=\"http://www.ech.ch/xmlns/eCH-0097/2\">111111114</eCH-0097-2:uidOrganisationId>\n" +
				"\t\t\t</eVD-0022-3:uid>\n" +
				"\t\t\t<eVD-0022-3:typeOfLocation>1</eVD-0022-3:typeOfLocation>\n" +
				"\t\t\t<eVD-0022-3:legalForm>0109</eVD-0022-3:legalForm>\n" +
				"\t\t\t<eVD-0022-3:branchText>Fabrication d'objet synthétiques</eVD-0022-3:branchText>\n" +
				"\t\t</eVD-0022-3:noticeRequestBody>\n" +
				"\t</noticeRequest>\n" +
				"\t<noticeRequestStatus xmlns=\"http://evd.vd.ch/xmlns/eVD-0022/3\">\n" +
				"\t\t<noticeRequestStatusCode>5</noticeRequestStatusCode>\n" +
				"\t\t<noticeRequestStatusDate>2016-09-15T10:34:54</noticeRequestStatusDate>\n" +
				"\t</noticeRequestStatus>\n" +
				"</eVD-0024:noticeRequestReport>";

		final EsbMessage m = EsbMessageFactory.createMessage();
		m.setBusinessId("test-buid");
		m.setBusinessUser("test-principal");
		m.setServiceDestination("test-destination");
		m.setServiceReplyTo("test-replyTo");
		m.setContext("ReponseAnnonceIDE");
		m.setBody(message);

		handler.onEsbMessage(m);

		assertTrue(rapportAnnonceIDEProcessor.isUsed());
		assertNotNull(rapportAnnonceIDEProcessor.getAnnonceIDE());
		final AnnonceIDEEnvoyee annonceIDE = rapportAnnonceIDEProcessor.getAnnonceIDE();

		assertEquals(1000L, annonceIDE.getNumero().longValue());
		assertEquals(TypeAnnonce.CREATION, annonceIDE.getType());

		final NumeroIDE noIde = annonceIDE.getNoIde();
		assertNotNull(noIde);
		assertEquals("CHE111111114", noIde.getValeur());

		final BaseAnnonceIDE.Statut statut = annonceIDE.getStatut();
		assertNotNull(statut);
		assertEquals(StatutAnnonce.REFUSE_IDE, statut.getStatut());

		final BaseAnnonceIDE.Contenu contenu = annonceIDE.getContenu();
		assertNotNull(contenu);
		assertEquals("Fabrication d'objet synthétiques", contenu.getSecteurActivite());
	}
}
