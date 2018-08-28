package ch.vd.unireg.evenement.docsortant;

import java.util.Collections;
import java.util.Date;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.technical.esb.jms.EsbJmsTemplate;
import ch.vd.technical.esb.store.raft.RaftEsbStore;
import ch.vd.unireg.xml.event.docsortant.v1.Archivage;
import ch.vd.unireg.xml.event.docsortant.v1.Archives;
import ch.vd.unireg.xml.event.docsortant.v1.Caracteristiques;
import ch.vd.unireg.xml.event.docsortant.v1.CodeSupport;
import ch.vd.unireg.xml.event.docsortant.v1.Document;
import ch.vd.unireg.xml.event.docsortant.v1.Documents;
import ch.vd.unireg.xml.event.docsortant.v1.DonneesMetier;
import ch.vd.unireg.xml.event.docsortant.v1.Population;
import ch.vd.unireg.common.AuthenticationHelper;
import ch.vd.unireg.common.XmlUtils;
import ch.vd.unireg.evenement.EvenementTest;

public class EvenementDocumentSortantSenderITTest extends EvenementTest {

	private String OUTPUT_QUEUE;
	private EvenementDocumentSortantSenderImpl sender;

	@Before
	public void setUp() throws Exception {

		OUTPUT_QUEUE = uniregProperties.getProperty("testprop.jms.queue.notification.document.sortant");

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

		buildEsbMessageValidator(new Resource[]{new ClassPathResource("event/docsortant/documentSortantRepElec-1.xsd")});

		sender = new EvenementDocumentSortantSenderImpl();
		sender.setServiceDestination("test");
		sender.setOutputQueue(OUTPUT_QUEUE);
		sender.setEsbTemplate(esbTemplate);
		sender.setEsbValidator(esbValidator);
		sender.setEnabled(true);
		sender.afterPropertiesSet();
	}

	@Test(timeout = 10000L)
	public void testEnvoiEvenement() throws Exception {

		AuthenticationHelper.pushPrincipal("EvenementDocumentSortantSenderITTest");
		try {
			final Date date = DateHelper.getDateTime(2016, 5, 12, 21, 17, 57);
			final Document doc = new Document();
			doc.setArchive(new Archives("monIdDocument", "402", "5653", "332"));
			doc.setCaracteristiques(new Caracteristiques(XmlUtils.date2xmlcal(date), "451", "SSTYPEDOC", "NOMDOC", "EMETTEUR", CodeSupport.CED, new Archivage(false, null)));
			doc.setDonneesMetier(new DonneesMetier(Population.PM, null, Boolean.TRUE, 20220, null));
			doc.setIdentifiantSupervision("UNIREG-identifiant-unique");

			sender.sendEvenementDocumentSortant("monBusinessIdAMoi", new Documents(Collections.singletonList(doc), XmlUtils.date2xmlcal(date)), false, null, true);

			// On vérifie que l'on a bien envoyé le message
			final String texte = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><ev-docsortant-1:documents xmlns:ev-docsortant-1=\"http://www.vd.ch/fiscalite/dperm/repelec/document-sortant/1\" horodatage=\"2016-05-12T21:17:57.000+02:00\"><ev-docsortant-1:documentSortant><ev-docsortant-1:identifiantSupervision>UNIREG-identifiant-unique</ev-docsortant-1:identifiantSupervision><ev-docsortant-1:caracteristiques><ev-docsortant-1:datePublication>2016-05-12T21:17:57.000+02:00</ev-docsortant-1:datePublication><ev-docsortant-1:typeDocument>451</ev-docsortant-1:typeDocument><ev-docsortant-1:sousTypeDocument>SSTYPEDOC</ev-docsortant-1:sousTypeDocument><ev-docsortant-1:nomDocument>NOMDOC</ev-docsortant-1:nomDocument><ev-docsortant-1:emetteur>EMETTEUR</ev-docsortant-1:emetteur><ev-docsortant-1:support>CED</ev-docsortant-1:support><ev-docsortant-1:archivage><ev-docsortant-1:valeurProbante>false</ev-docsortant-1:valeurProbante></ev-docsortant-1:archivage></ev-docsortant-1:caracteristiques><ev-docsortant-1:donneesMetier><ev-docsortant-1:axe>PM</ev-docsortant-1:axe><ev-docsortant-1:periodeFiscalePerenne>true</ev-docsortant-1:periodeFiscalePerenne><ev-docsortant-1:numeroContribuable>20220</ev-docsortant-1:numeroContribuable></ev-docsortant-1:donneesMetier><ev-docsortant-1:archive><ev-docsortant-1:idDocument>monIdDocument</ev-docsortant-1:idDocument><ev-docsortant-1:typDocument>402</ev-docsortant-1:typDocument><ev-docsortant-1:nomDossier>5653</ev-docsortant-1:nomDossier><ev-docsortant-1:typDossier>332</ev-docsortant-1:typDossier></ev-docsortant-1:archive></ev-docsortant-1:documentSortant></ev-docsortant-1:documents>";
			assertTextMessage(OUTPUT_QUEUE, texte);

		}
		finally {
			AuthenticationHelper.popPrincipal();
		}
	}
}
