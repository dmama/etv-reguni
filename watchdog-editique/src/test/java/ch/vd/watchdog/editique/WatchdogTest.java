package ch.vd.watchdog.editique;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ch.vd.editique.service.enumeration.TypeFormat;
import ch.vd.editique.service.enumeration.TypeImpression;
import ch.vd.editique.service.enumeration.TypeMessagePropertiesNames;
import ch.vd.technical.esb.EsbMessage;
import ch.vd.technical.esb.EsbMessageFactory;
import ch.vd.technical.esb.jms.EsbJmsTemplate;
import ch.vd.technical.esb.util.EsbDataHandler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:jms.xml", "classpath:cedi.xml"})
public abstract class WatchdogTest implements ApplicationContextAware {

	private static EsbMessageFactory esbMessageFactory;
	private static EsbJmsTemplate esbTemplate;
	private static Listener listener;
	private static final String inputQueue = "unireg.retourImpression-WATCHDOG";
	private static final String outputQueue = "imprimer";
	private ApplicationContext context;

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.context = applicationContext;
	}

	@Before
	public void setup() {
		esbTemplate = context.getBean("esbJmsTemplate", EsbJmsTemplate.class);
		esbMessageFactory = context.getBean("esbMessageFactory", EsbMessageFactory.class);
		listener = context.getBean("editiqueListener", Listener.class);
	}

	@Test(timeout = 60000)
	public void testImpressionEditique() throws Exception {

		final String nomDocument = String.format("2011 01 086006202 %s", new SimpleDateFormat("yyyyMMddHHmmssSSS").format(new Date()));

		// envoi d'une demande d'impression
		{
			final EsbMessage m = esbMessageFactory.createMessage();
			m.setBusinessId(nomDocument);
			m.setBusinessUser("Watchdog-Unireg");
			m.setServiceDestination(outputQueue);
			m.setServiceReplyTo(inputQueue);
			m.setContext("evenementEditique");

			// meta-info requis par l'ESB
			m.addHeader(TypeMessagePropertiesNames.PRINT_MODE_MESSAGE_PROPERTY_NAME.toString(), TypeImpression.DIRECT.toString());
			m.addHeader(TypeMessagePropertiesNames.ARCHIVE_MESSAGE_PROPERTY_FLAG.toString(), "false");
			m.addHeader(TypeMessagePropertiesNames.DOCUMENT_TYPE_MESSAGE_PROPERTY_NAME.toString(), "RGPI0802");
			m.addHeader(TypeMessagePropertiesNames.RETURN_FORMAT_MESSAGE_PROPERTY_NAME.toString(), TypeFormat.PCL.toString());
			m.addHeader("DI_ID", nomDocument);
			m.setBody(BODY);

			// on envoie l'impression
			esbTemplate.send(m);
		}

		// réception de l'impression
		{
			// on attend la réponse
			while (listener.receivedCount() == 0) {
				Thread.sleep(100);
			}
			assertEquals(1, listener.receivedCount());

			// on s'assure qu'on a reçu le bon message
			final EsbMessage message = listener.getReceivedMessages().get(0);
			assertNotNull(message);
			assertEquals(nomDocument, message.getBusinessCorrelationId());

			// pour la forme, on s'assure qu'il y a bien un attachement, mais sans vérifier l'attachement lui-même (comment le faire ?)
			final Set<String> attachements = message.getAttachmentsNames();
			assertEquals(1, attachements.size());
			assertEquals("data", attachements.iterator().next());

			final EsbDataHandler attachement = message.getAttachment("data");
			assertNotNull(attachement);
		}
	}

	private static final String BODY = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
			"<FichierImpression xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n" +
			"  <Document>\n" +
			"    <infoDocument>\n" +
			"      <prefixe>RGPI0802DOCUM</prefixe>\n" +
			"      <idEnvoi/>\n" +
			"      <typDoc>DI</typDoc>\n" +
			"      <codDoc>DI_VAUDTAX</codDoc>\n" +
			"      <cleRgp>\n" +
			"        <anneeFiscale>2011</anneeFiscale>\n" +
			"      </cleRgp>\n" +
			"      <version>1.0</version>\n" +
			"      <logo>CANT</logo>\n" +
			"      <populations>PP</populations>\n" +
			"    </infoDocument>\n" +
			"    <infoEnteteDocument>\n" +
			"      <prefixe>RGPI0802HAUT1</prefixe>\n" +
			"      <porteAdresse>\n" +
			"        <adresse>\n" +
			"          <adresseCourrierLigne1>Monsieur et Madame</adresseCourrierLigne1>\n" +
			"          <adresseCourrierLigne2>Laurent Schmid</adresseCourrierLigne2>\n" +
			"          <adresseCourrierLigne3>Christine Schmid</adresseCourrierLigne3>\n" +
			"          <adresseCourrierLigne4>La Tuilière</adresseCourrierLigne4>\n" +
			"          <adresseCourrierLigne5>1168 Villars-sous-Yens</adresseCourrierLigne5>\n" +
			"          <adresseCourrierLigne6 xsi:nil=\"true\"/>\n" +
			"        </adresse>\n" +
			"      </porteAdresse>\n" +
			"      <expediteur>\n" +
			"        <adresse>\n" +
			"          <adresseCourrierLigne1>Office d'impôt du district</adresseCourrierLigne1>\n" +
			"          <adresseCourrierLigne2>de Morges</adresseCourrierLigne2>\n" +
			"          <adresseCourrierLigne3>Avenue de la Gottaz 32</adresseCourrierLigne3>\n" +
			"          <adresseCourrierLigne4>Case Postale 67</adresseCourrierLigne4>\n" +
			"          <adresseCourrierLigne5>1110 Morges 2</adresseCourrierLigne5>\n" +
			"          <adresseCourrierLigne6 xsi:nil=\"true\"/>\n" +
			"        </adresse>\n" +
			"        <adrMes xsi:nil=\"true\"/>\n" +
			"        <numTelephone>021'557'93'00</numTelephone>\n" +
			"        <numFax>021'557'93'80</numFax>\n" +
			"        <numCCP>10-711-9</numCCP>\n" +
			"        <ideUti>[UT] iamtestuser</ideUti>\n" +
			"        <dateExpedition>20110920</dateExpedition>\n" +
			"      </expediteur>\n" +
			"      <destinataire>\n" +
			"        <adresse>\n" +
			"          <adresseCourrierLigne1>Monsieur et Madame</adresseCourrierLigne1>\n" +
			"          <adresseCourrierLigne2>Laurent Schmid</adresseCourrierLigne2>\n" +
			"          <adresseCourrierLigne3>Christine Schmid</adresseCourrierLigne3>\n" +
			"          <adresseCourrierLigne4>La Tuilière</adresseCourrierLigne4>\n" +
			"          <adresseCourrierLigne5>1168 Villars-sous-Yens</adresseCourrierLigne5>\n" +
			"          <adresseCourrierLigne6 xsi:nil=\"true\"/>\n" +
			"        </adresse>\n" +
			"        <numContribuable>860.062.02</numContribuable>\n" +
			"      </destinataire>\n" +
			"    </infoEnteteDocument>\n" +
			"    <DIVDTAX>\n" +
			"      <InfoDI>\n" +
			"        <ANNEEFISCALE>2011</ANNEEFISCALE>\n" +
			"        <DELAIRETOUR>21.11.2011</DELAIRETOUR>\n" +
			"        <NOCANT>860.062.02</NOCANT>\n" +
			"        <NOOID>10-0</NOOID>\n" +
			"        <CODBARR>08600620220110110</CODBARR>\n" +
			"        <CODETRAME>0</CODETRAME>\n" +
			"        <CODESEGMENT>0</CODESEGMENT>\n" +
			"      </InfoDI>\n" +
			"      <AdresseRetour>\n" +
			"        <ADRES1RETOUR>Centre d'enregistrement</ADRES1RETOUR>\n" +
			"        <ADRES2RETOUR>des déclarations d'impôt</ADRES2RETOUR>\n" +
			"        <ADRES3RETOUR>CEDI 10</ADRES3RETOUR>\n" +
			"        <ADRES4RETOUR>1014 Lausanne Adm cant</ADRES4RETOUR>\n" +
			"        <ADRES5RETOUR xsi:nil=\"true\"/>\n" +
			"        <ADRES6RETOUR xsi:nil=\"true\"/>\n" +
			"      </AdresseRetour>\n" +
			"      <FormuleAppel>Monsieur et Madame</FormuleAppel>\n" +
			"      <Contrib1>\n" +
			"        <INDETATCIVIL1>Marié(e)</INDETATCIVIL1>\n" +
			"        <INDNOMPRENOM1>Monsieur Laurent Schmid</INDNOMPRENOM1>\n" +
			"        <INDDATENAISS1>09.02.1961</INDDATENAISS1>\n" +
			"        <NAVS13>756.1738.6057.29</NAVS13>\n" +
			"      </Contrib1>\n" +
			"      <Contrib2>\n" +
			"        <INDETATCIVIL2>Marié(e)</INDETATCIVIL2>\n" +
			"        <INDNOMPRENOM2>Madame Christine Schmid</INDNOMPRENOM2>\n" +
			"        <INDDATENAISS2>20.10.1960</INDDATENAISS2>\n" +
			"        <NAVS13>756.9416.6914.08</NAVS13>\n" +
			"      </Contrib2>\n" +
			"      <Annexes>\n" +
			"        <Annexe_250>1</Annexe_250>\n" +
			"      </Annexes>\n" +
			"    </DIVDTAX>\n" +
			"  </Document>\n" +
			"</FichierImpression>";
}
