package ch.vd.uniregctb.evenement.externe.jms;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.log4j.Logger;
import org.apache.xmlbeans.XmlError;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlOptions;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.Lifecycle;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;
import org.springframework.util.Assert;

import ch.vd.fiscalite.registre.evenementImpotSourceV1.EvenementImpotSourceQuittanceDocument;
import ch.vd.fiscalite.registre.evenementImpotSourceV1.EvenementImpotSourceQuittanceType;
import ch.vd.uniregctb.evenement.externe.DelegateEvenementExterne;
import ch.vd.uniregctb.evenement.externe.EvenementExterneException;
import ch.vd.uniregctb.evenement.externe.EvenementExterneFacade;
import ch.vd.uniregctb.evenement.externe.EvenementExterneResultat;
import ch.vd.uniregctb.evenement.externe.EvenementExterneService;
import ch.vd.uniregctb.evenement.externe.IEvenementExterne;

/**
 * Implémentation standard de {@link EvenementExterneService}.
 *
 * @author xcicfh (last modified by $Author: xcipdt $ @ $Date: 2008/03/28 15:55:58 $)
 * @version $Revision: 1.7 $
 */
public class EvenementExterneFacadeImpl implements EvenementExterneFacade, InitializingBean, DisposableBean {

	/**
	 * Le logger.
	 */
	private static final Logger LOGGER = Logger.getLogger(EvenementExterneFacadeImpl.class);

	/**
	 * contient le listener principal.
	 */
	private Lifecycle listenerContainer;

	/**
	 * contient le message listener permettant de déléguer la reception d'un message
	 */
	private MessageListener messageListener;


	private JmsTemplate jmsTemplateOutput;

	/**
	 * {@inheritDoc}
	 */
	public void afterPropertiesSet() throws Exception {
		if (listenerContainer == null) {
			throw new IllegalArgumentException("listenerContainer is required");
		}
		if (messageListener == null) {
			throw new IllegalArgumentException("messageListener is required");
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void destroy() throws Exception {
		if (getListenerContainer() != null)
			getListenerContainer().stop();
	}

	/**
	 * Obtient le listener principal.
	 *
	 * @return Retourne le listener principal.
	 */
	public Lifecycle getListenerContainer() {
		return listenerContainer;
	}

	/**
	 * Définit le message listener principal.
	 *
	 * @param listenerContainer
	 *            le message listener principal.
	 */
	public void setListenerContainer(Lifecycle listenerContainer) {
		this.listenerContainer = listenerContainer;
	}

	/**
	 * Définit le message listener permettant de déléguer la reception d'un message.
	 *
	 * @param messageListener
	 *            le message listener permettant de déléguer la reception d'un message.
	 */
	public void setMessageListener(MessageListener messageListener) {
		Assert.notNull(messageListener);
		this.messageListener = messageListener;
		this.messageListener.setParent(this);
	}

	/**
	 * {@inheritDoc}
	 */
	public void setDelegate(DelegateEvenementExterne delegate) {
		messageListener.setDelegate(delegate);
	}

	/**
	 * {@inheritDoc}
	 */
	public void sendEvent(IEvenementExterne evenementExterne) throws Exception {
		if (evenementExterne == null) {
			throw new IllegalArgumentException("Argument evenement ne peut être null.");
		}
		final ByteArrayOutputStream writer = new ByteArrayOutputStream();
		try {
			writeXml(writer, (XmlObject) evenementExterne);
		}
		catch (Exception e) {
			String message = "Exception lors de la sérialisation xml";
			LOGGER.fatal(message, e);
			throw new EvenementExterneException(message);
		}
		try {
			jmsTemplateOutput.send(new MessageCreator() {

				public Message createMessage(Session session) throws JMSException {
					TextMessage message = session.createTextMessage();
					message.setText(writer.toString());
					return message;
				}
			});
		}
		catch (Exception e) {
			String message = "Exception lors du processus d'envoi d'un événement externe";
			LOGGER.fatal(message, e);

			throw new EvenementExterneException(message);
		}
	}
	/**
	 * Créer la réponse avec les informations contenues dans le message.
	 *
	 * @param message
	 *            message JMS
	 * @return Retourne un réponse
	 * @throws JMSException
	 *             arrive quand survient une erreur JMS.
	 */
	EvenementExterneResultat createResultfromMessage(Message message) throws JMSException {
		EvenementExterneResultatImpl resultat = new EvenementExterneResultatImpl();
		resultat.setCorrelationId(message.getJMSMessageID());
		if (message instanceof TextMessage) {
			TextMessage msg = (TextMessage) message;
			String text = msg.getText();
			if (text == null || text.length() == 0) {
				resultat.setError(new Exception("message reçu est vide"));
				return resultat;
			}
			resultat.setText(text);
			InputStream stream = new ByteArrayInputStream(text.getBytes());
			try {
				IEvenementExterne evt = readXml(stream);
				resultat.setEvenement(evt);
			}
			catch (Exception e) {
				resultat.setError(new Exception("Erreur dans la deserialisation", e));
			}
			finally {
				if (stream != null) {
					try {
						stream.close();
					}
					catch (IOException e) {
					}
				}
			}
		}
		else {
			resultat.setError(new Exception("Message de type " + message.getClass().getName() + " non accepté."));
		}
		return resultat;
	}

	/**
	 * {@inheritDoc}
	 */
	public EvenementImpotSourceQuittanceType creerEvenementImpotSource() {
		return EvenementImpotSourceQuittanceDocument.Factory.newInstance().addNewEvenementImpotSourceQuittance();
	}

	/**
	 * désirialize un objet depuis un flux
	 *
	 * @param reader
	 *            Le flux contenant la représentation xml de le nouvelle instance de l'object à retourner.
	 * @return Retourne une nouvelle instance de l'boject représenté par le flux.
	 * @throws IOException
	 *             Arrive quand une errreur sur le flux survient.
	 * @throws XmlException
	 *             Arrive quand une erreur sur la désérialisation survient.
	 * @throws EvenementExterneException
	 *             Arrive quand une erreur dans la validation survient.
	 */
	protected IEvenementExterne readXml(InputStream reader) throws IOException, XmlException, EvenementExterneException {
		try {
			XmlObject evt = XmlObject.Factory.parse(reader);
			if (evt == null) {
				throw new RuntimeException("Unexcepted error");
			}
			XmlOptions validateOptions = new XmlOptions();
			ArrayList<XmlError> errorList = new ArrayList<XmlError>();
			validateOptions.setErrorListener(errorList);
			if (!evt.validate(validateOptions)) {
				StringBuilder builder = new StringBuilder();
				for (XmlError error : errorList) {
					builder.append("\n");
					builder.append("Message: " + error.getErrorCode() + " " + error.getMessage() + "\n");
					builder.append("Location of invalid XML: " + error.getCursorLocation().xmlText() + "\n");
					throw new EvenementExterneException(builder.toString());
				}
			}
			if (evt instanceof EvenementImpotSourceQuittanceDocument) {
				return ((EvenementImpotSourceQuittanceDocument) evt).getEvenementImpotSourceQuittance();
			}
			return null;
		}
		finally {
			if (reader != null) {
				reader.close();
			}
		}
	}

	/**
	 * Sérialise une objet.
	 *
	 * @param writer
	 *            stream
	 * @param object
	 *            objet à sérialiser
	 * @throws IOException
	 *             Arrive lors de la sérialisation
	 * @throws EvenementExterneException
	 *             Cette exception survient si l'object à sérialiser n'est pas valide.
	 */
	protected void writeXml(OutputStream writer, XmlObject object) throws IOException, EvenementExterneException {
		XmlOptions validateOptions = new XmlOptions();
		ArrayList<XmlError> errorList = new ArrayList<XmlError>();
		validateOptions.setErrorListener(errorList);
		if (!object.validate(validateOptions)) {
			StringBuilder builder = new StringBuilder();
			for (XmlError error : errorList) {
				builder.append("\n");
				builder.append("Message: " + error.getErrorCode() + " " + error.getMessage() + "\n");
				builder.append("Location of invalid XML: " + error.getCursorLocation().xmlText() + "\n");
				throw new EvenementExterneException(builder.toString());
			}
		}
		object.save(writer, new XmlOptions().setSaveOuter());
	}



	/**
	 * @param jmsTemplateOutput the jmsTemplateOutput to set
	 */
	public void setJmsTemplateOutput(JmsTemplate jmsTemplateOutput) {
		this.jmsTemplateOutput = jmsTemplateOutput;
	}
}
