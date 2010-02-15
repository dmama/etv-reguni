package ch.vd.vuta.processing;

import java.io.IOException;
import java.io.StringBufferInputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.springframework.context.ApplicationContext;
import org.springframework.util.Assert;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import ch.vd.vuta.SmsException;
import ch.vd.vuta.model.SmsDAO;
import ch.vd.vuta.model.SmsModel;

public class SmsProcessor {
	
	public static Logger LOGGER = Logger.getLogger(SmsProcessor.class);
	
	private ApplicationContext applicationContext;
	private SmsDAO smsDAO;

	
	public SmsProcessor(ApplicationContext ctx) {
		
		applicationContext = ctx;

		smsDAO = (SmsDAO)applicationContext.getBean("smsDAO");
	}
	
	/**
	 * Point d'entrée principal du processeur
	 * 
	 * @param smsAsXml Le XML envoyé par MNC
	 * @return Une chaine qui correspond au status a renvoyer a l'expediteur
	 * @throws Exception
	 */
	public ProcessorResponse treatSms(String smsAsXml) {

		String resp = ProcessorResponse.ERROR_RESPONSE;
		
		// Si exception => SMS/XML invalide
		SmsModel sms = null;
		try {
			sms = decodeSmsAsXml(smsAsXml);
			LOGGER.debug("SMS décodé:"+sms);
			
			// On controle que le NO est bien formé
			try {
				// Si exception, le numero est invalide
				validateNumeroContribuable(sms);
				LOGGER.debug("Numéro de contribuable "+sms.getNumeroCTB()+" trouvé");
				
				// Tout est en ordre
				sms.setStatus(true, ProcessorResponse.STATUS_OK);
		
				// Reponse pour l'envoyeur => OK
				resp = encodeNoCtbAsXmlResponse(sms.getNumeroCTB());
				LOGGER.info("SMS traité avec succès");
			}
			// Exception parce que le NO_CTB est faux
			catch (SmsException e) {
				sms.setStatus(false, e.getMessage());
			}
		}
		// Exception parce que le XML est invalide
		catch (SmsException e) {
			
			// XML invalide
			// Sauve juste le SMS as-is + un status
			sms = new SmsModel();
			sms.setSmsComplet(smsAsXml);
			sms.setStatus(false, ProcessorResponse.STATUS_XML_INVALIDE);
		}

		// Sauve le SMS en base
		// Dans un block try/catch pour que si le commit marche pas, on envoie quand meme la reponse
		try {
			smsDAO.save(sms);
		}
		catch (Exception e) {
			e.printStackTrace();
			LOGGER.error(e);
			LOGGER.error("!!! Erreur grave. Exception après une autre Exception !!!");
		}

		return new ProcessorResponse(resp, sms);
	}
	
	protected void validateNumeroContribuable(SmsModel sms) throws SmsException {
		
		String ctbStr = sms.getTexte().substring(4);
		ctbStr = ctbStr.trim();

	   	try {
	   		Integer ctb = Integer.parseInt(ctbStr);
		   	sms.setNumeroCTB(ctb);
	   	}
	   	catch (NumberFormatException e) {
	   		//e.printStackTrace();
	   		throw new SmsException(generateNumeroCtbInvalideMessage(ctbStr));
	   	}
	}
	
	public static String generateNumeroCtbInvalideMessage(String ctbStr) {
		
		String msg = ProcessorResponse.STATUS_NO_CTB_INVALIDE+": '"+ctbStr+"'";
		return msg;
	}

	public static String generateNumeroCtbIntrouvableMessage(String ctbStr) {
		
		String msg = ProcessorResponse.STATUS_NO_CTB_INTROUVABLE+": '"+ctbStr+"'";
		return msg;
	}
	
	public static String encodeNoCtbAsXmlResponse(Integer noCtb) {
		String text = String.format("IFD %s: Merci pour votre demande de 8 acomptes IFD. Vous recevrez ceux-ci en mars prochain, payables de mai à décembre 2008. Admin. cantonale des impôts", noCtb.toString());
		return text;
	}
	
	// Ré-envoi
	/*
	Message renvoyé :
	<?xml version="1.0" encoding="UTF-8" ?>
	<NotificationReply>
	<message>
	<text>Nous avons reçu votre demande de mensualisation des accomptes de l’IFD pour le contribuable numéro 12345678</text>
	<cost>20</cost>
	</message>
	</NotificationReply>
	*/
	public static String getTextAsXmlResponse(String text) {
		String resp = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>"+
		"<NotificationReply>"+
		"<message>"+
		"<text>"+text+"</text>"+
		"<cost>20</cost>"+
		"</message>"+
		"</NotificationReply>";
		return resp;
	}
	
	// Reception
	/*
	<?xml version="1.0" encoding="UTF-8" ?>
	<Notification>
	<instance>blu</instance>
	<sender>+41761234567</sender>
	<operator>sunrise</operator>
	<service>IFD</service>
	<language>fr</language>
	<command>FORWARD</command>
	<requestUid>sms9676205</requestUid>
	<parameters>
	<text>IFD 12345678</text>
	<message>IFD 12345678</message>
	</parameters>
	</Notification>
	*/
	/**
	 * Prends le xml d'entrée et en crée un SmsModel prêt pour insertion en base
	 * 
	 * @param smsAsXml Le XML de MNC
	 * @return SmsModel le sms bien formé avec les infos provenant du XML
	 * @throws SmsException en cas de probleme avec le XML
	 */
	protected SmsModel decodeSmsAsXml(String smsAsXml) throws SmsException {
		
		SmsModel sms = new SmsModel();

        try {
        	DocumentBuilderFactory fabrique = DocumentBuilderFactory.newInstance();
        	DocumentBuilder constructeur = fabrique.newDocumentBuilder();
        	Document document = constructeur.parse(new StringBufferInputStream(smsAsXml));
        	
        	Element racine = document.getDocumentElement();
        	NodeList rootList = racine.getChildNodes();
        	Node params = null; 
        	
        	int nb = rootList.getLength();
        	for (int i=0;i<nb;i++) {
        		Node elem = rootList.item(i);
        		String name = elem.getNodeName();
        		String value = elem.getTextContent();

        		LOGGER.debug("'"+name+":"+value+"'");
        		
        		if (name.equals("sender")) {
        			sms.setNumeroNatel(value);
        		}
        		else if (name.equals("operator")) {
        			sms.setOperateur(value);
        		}
        		else if (name.equals("language")) {
        			sms.setLangue(value);
        		}
        		else if (name.equals("requestUid")) {
        			sms.setRequestUid(value);
        		}
        		else if (name.equals("text")) {
        			sms.setTexte(value);
        		}
        		else if (name.equals("parameters")) {
        			params = elem;
        		}
        	}
        	
        	// Le texte du SMS
        	Node textNode = null;
    		NodeList list = params.getChildNodes();
    		for (int i=0;i<list.getLength();i++) {
    			textNode = list.item(i); // <text>
    			if (textNode.getNodeName().equals("text")) {
    				break;
    			}
    		}
        	Assert.isTrue(textNode != null);
        	//String name = textNode.getNodeName();
        	String value = textNode.getTextContent();
        	sms.setTexte(value);
        	
        	// le SMS complet
        	sms.setSmsComplet(smsAsXml);
        }
		catch (ParserConfigurationException e) {
			LOGGER.error(e);
			throw new SmsException("XML invalide", e);
        }
		catch (SAXException e) {
			LOGGER.error(e);
			throw new SmsException("XML invalide", e);
        }
		catch (IOException e) {
			LOGGER.error(e);
			throw new SmsException("XML invalide", e);
        }
		
		return sms;
	}

	public static String getSmsAsXml(String natel, String texte, String operateur, String langue, String requestUid) {
		
		String smsAsXml = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>"+
		"<Notification>\n"+
		"\t<instance>blu</instance>\t\n"+
		"\t\t<sender>"+natel+"</sender>\n"+
		"\t<operator>"+operateur+"</operator>\n"+
		"\t<service>IFD</service>\n\n"+
		"\t<language>"+langue+"</language>\n"+
		"\t<command>FORWARD</command>\n"+
		"\t<requestUid>"+requestUid+"</requestUid>\n"+
		"\t\t<parameters>\n"+
		"\t\t\t<text>"+texte+"</text>\n"+
		"\t\t<message>"+texte+"</message>\n"+
		"</parameters>\n"+
		"</Notification>\n\n\n";
		
		return smsAsXml;
	}

}
