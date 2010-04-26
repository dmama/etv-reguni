package ch.vd.uniregctb.editique.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

import javax.jms.BytesMessage;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Message;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlOptions;
import org.springframework.jms.core.JmsTemplate;

import ch.vd.editique.service.enumeration.TypeFormat;
import ch.vd.editique.service.enumeration.TypeImpression;
import ch.vd.editique.service.enumeration.TypeMessagePropertiesNames;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.editique.EditiqueException;
import ch.vd.uniregctb.editique.EditiqueResultat;
import ch.vd.uniregctb.editique.EditiqueService;
import ch.vd.uniregctb.jms.JmsTemplateTracing;
import ch.vd.uniregctb.stats.StatsService;

/**
 * Implémentation standard de {@link EditiqueService}.
 *
 * @author xcifwi (last modified by $Author: xcicfh $ @ $Date: 2008/04/08 07:57:42 $)
 * @version $Revision: 1.23 $
 */
public final class EditiqueServiceImpl implements EditiqueService {

	private static final Logger LOGGER = Logger.getLogger(EditiqueServiceImpl.class);

	private static final String ENCODING_ISO_8859_1 = "ISO-8859-1";
	private static final String DI_ID = "DI_ID";
	private static final int BUFFER_SIZE = 1024;

	private static final String PDF_MIME = "application/pdf";
	private static final String PCL_MIME = "application/x-pcl";
	private static final String TIF_MIME = "image/tiff";
	private static final String AFP_MIME = "application/afp";

	/** Le type de document à transmettre au service pour UNIREG */
	public static final String TYPE_DOSSIER_UNIREG = "003";

	private FoldersService foldersService;

	// ConnectionFactory pour les envois/réceptions JMS transactionnels (batch)
	private ConnectionFactory txConnectionFactory;

	// ConnectionFactory pour les envois/réceptions JMS non-transactionnels (direct)
	private ConnectionFactory noTxConnectionFactory;

	private StatsService jmsStatsService;

	private String queueEditiqueOutput;

	private String queueEditiqueInput;

	/** Temps d'attente (en secondes) du retour du document PDF / PCL lors d'une impression locale. */
	private int receiveTimeout = 120;

	/**
	 * {@inheritDoc}
	 */
	public EditiqueResultat creerDocumentImmediatement(String nomDocument, String typeDocument, TypeFormat typeFormat, Object object, boolean archive) throws EditiqueException, JMSException {
		// envoi de la demande
		if (LOGGER.isDebugEnabled()) {
			final String msg = String.format("Demande d'impression locale du document %s (%s)", nomDocument, typeDocument);
			LOGGER.debug(msg);
		}
		final String id = envoyerDocument(noTxConnectionFactory, nomDocument, typeDocument, object, TypeImpression.DIRECT, typeFormat, archive);

		// demande envoyée, attente de la réponse
		if (LOGGER.isDebugEnabled()) {
			final String msg = String.format("Demande d'impression locale du document %s (%s) envoyée : %s", nomDocument, typeDocument, id);
			LOGGER.debug(msg);
		}
		final EditiqueResultat resultat = getDocument(noTxConnectionFactory, typeFormat, nomDocument, true);

		// log de l'état de la réponse
		if (LOGGER.isDebugEnabled()) {
			final String statut;
			if (resultat == null) {
				statut = "Time-out";
			}
			else if (resultat.getDocument() == null) {
				statut = String.format("Erreur (%s), ", resultat.getError());
			}
			else {
				statut = "OK";
			}
			final String msg = String.format("Retour d'impression locale reçu pour document %s (%s) : %s", nomDocument, typeDocument, statut);
			LOGGER.debug(msg);
		}
		return resultat;
	}

	/**
	 * {@inheritDoc}
	 */
	public void creerDocumentParBatch(Object object, String typeDocument, boolean archive) throws EditiqueException {
		envoyerDocument(txConnectionFactory, null, typeDocument, object, TypeImpression.BATCH, null, archive);
	}

	/**
	 * Cette méthode permet d'envoyer un object afin de créer un document de type <code>typeImpression</code> avec le nom
	 * <code>nomDocument</code>
	 *
	 * @param connectionFactory
	 * @param nomDocument nom du fichier à créer ou nom du fichier de l'archive
	 * @param object object à envoyer.
	 * @param typeImpression type de l'impression
	 * @throws EditiqueException si un problème survient durant la sérialistation de l'object ou durant l'envoie du message au serveur JMS.
	 */
	private String envoyerDocument(ConnectionFactory connectionFactory, final String nomDocument, final String typeDocument, Object object, final TypeImpression typeImpression,
	                               TypeFormat typeFormat, boolean archive) throws EditiqueException {
		final ByteArrayOutputStream writer = new ByteArrayOutputStream();
		final String xml;

		// Si l'objet est de type String, cela signifie que l'objet est d�j� au format XML et que la s�rialisation n'est
		// pas nécessaire.

		try {
			// Sérialisation de l'objet.
			writeXml(writer, object);
		}
		catch (Exception e) {
			final String message = "Exception lors de la sérialisation xml";
			LOGGER.fatal(message, e);

			/*
			 * Attention : throw new PerceptionException(message, e) --> INTERDIT. En effet l'exception e peut �tre de type
			 * XmlSerializationException et dans ce cas contenir un objet XPathLocation qui n'est pas sérialisable.
			 */
			throw new EditiqueException(message);

		} finally {
			try {
				writer.close();
			}
			catch(IOException ex) {
				throw new EditiqueException("Erreur dans la cloture du xml", ex);
			}
		}

		String xmlTmp;
		try {
			xmlTmp = writer.toString(ENCODING_ISO_8859_1);
		}
		catch (UnsupportedEncodingException e1) {
			throw new EditiqueException("Erreur d'encoding", e1);
		}

		// FIXME (FDE) Bidouille pour palier le fait que la balise root du xml n'est pas générée pour une raison inconnue
		if (xmlTmp.indexOf("xml-fragment") >= 0) {
			xml = StringUtils.replace(xmlTmp, "xml-fragment", "FichierImpression");
		} else {
			StringBuilder sbXml = new StringBuilder(xmlTmp);
			sbXml.insert(xmlTmp.indexOf("?>") + 2, "<FichierImpression>");
			sbXml.append("</FichierImpression>");
			xml = sbXml.toString();
		}

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace(xml);
		}

		try {
			final RequestSendMessageCreator messageCreator = new RequestSendMessageCreator(xml, nomDocument, typeDocument, typeImpression, typeFormat, archive);

			final JmsTemplate internal = new JmsTemplate(connectionFactory);
			internal.setDefaultDestinationName(queueEditiqueOutput);
			internal.afterPropertiesSet();

			final JmsTemplateTracing output = new JmsTemplateTracing();
			output.setStatsService(jmsStatsService);
			output.setTarget(internal);
			output.afterPropertiesSet();
			try {
				output.send(messageCreator);
				final String jmsMessageID = messageCreator.getMessage().getJMSMessageID();
				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace("Message ID JMS :" + jmsMessageID + "--");
					LOGGER.trace("ID :" +  nomDocument + "--");
				}
				return jmsMessageID;
			}
			finally {
				output.destroy();
			}
		}
		catch (Exception e) {
			final String message = "Exception lors du processus d'envoi d'un document au service Editique JMS";
			LOGGER.fatal(message, e);

			throw new EditiqueException(message);
		}
	}

	protected EditiqueResultat getDocument(ConnectionFactory connectionFactory, TypeFormat typeFormat, String correlationID, boolean appliqueDelai) throws JMSException {

		final JmsTemplate input = new JmsTemplate(connectionFactory);
		final long timeout = (appliqueDelai ? receiveTimeout * 1000 : JmsTemplate.RECEIVE_TIMEOUT_NO_WAIT);
		input.setReceiveTimeout(timeout);

		// On n'extrait de la queue que le message demandé
		final Message message = input.receiveSelected(queueEditiqueInput, DI_ID + " = '" + correlationID + "'");
		if (message == null) {
			return null;
		}

		return createResultfromMessage(message, typeFormat);
	}

	/**
	 * {@inheritDoc}
	 */
	public byte[] getPDFDeDocumentDepuisArchive(Long noContribuable, String typeDocument, String nomDocument) throws EditiqueException {
		try {
			final String noContribuableFormate = FormatNumeroHelper.numeroCTBToDisplay(noContribuable);
			return foldersService.getDocument(TYPE_DOSSIER_UNIREG, noContribuableFormate, typeDocument, nomDocument, FoldersService.PDF_FORMAT);
		}
		catch (Exception e) {
			final String message = "Erreur technique lors de l'appel au service folders.";
			LOGGER.fatal(message, e);
			throw new EditiqueException(message, e);
		}
	}

	/**
	 * Créer la réponse avec les informations contenues dans le message.
	 *
	 * @param message message JMS
	 * @param typeFormat
	 * @return Retourne un réponse
	 * @throws JMSException arrive quand survient une erreur JMS.
	 */
	private EditiqueResultat createResultfromMessage(Message message, TypeFormat typeFormat) throws JMSException {
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("EditiqueService: createResultfromMessage");
		}

		final EditiqueResultatImpl resultat = new EditiqueResultatImpl();
		resultat.setTimestampRecieved(System.currentTimeMillis());

		if (message instanceof BytesMessage) {
			final BytesMessage msg = (BytesMessage) message;
			final byte[] buffer = new byte[BUFFER_SIZE];
			int size;
			final ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
			while ((size = msg.readBytes(buffer)) > 0) {
				out.write(buffer, 0, size);
			}
			try {
				out.flush();
				resultat.setDocument(out.toByteArray());
				out.close();
			}
			catch (Exception ex) {
				resultat.setError(ex.getMessage());
			}

			final String documentType = msg.getStringProperty(TypeMessagePropertiesNames.DOCUMENT_TYPE_MESSAGE_PROPERTY_NAME.toString());
			final String idDocument = msg.getStringProperty(DI_ID);
			final String error = msg.getStringProperty(TypeMessagePropertiesNames.ERROR_MESSAGE_PROPERTY_NAME.toString());

			resultat.setDocumentType(documentType);
			resultat.setIdDocument(idDocument);
			resultat.setContentType(getContentType(typeFormat));
			resultat.setError(error);

			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace(resultat.toString());
			}
		}
		else {
			resultat.setError("message n'est pas un javax.jms.ByteMessage.");
		}
		return resultat;
	}

	private static String getContentType(TypeFormat typeFormat) {
		if (TypeFormat.PDF.equals(typeFormat)) {
			return PDF_MIME;
		}
		else if (TypeFormat.PCL.equals(typeFormat)) {
			return PCL_MIME;
		}
		else if (TypeFormat.TIF.equals(typeFormat)) {
			return TIF_MIME;
		}
		else if (TypeFormat.AFP.equals(typeFormat)) {
			return AFP_MIME;
		}
		else {
			throw new RuntimeException("TypeFormat non supporté : " + typeFormat);
		}
	}

	private void writeXml(OutputStream writer, Object object) throws Exception {
		if (object instanceof XmlObject) {
			writeXml(writer, (XmlObject) object);
		}
		else if (object instanceof String) {
			writer.write(((String) object).getBytes());
		}
	}

	private void writeXml(OutputStream writer, XmlObject object) throws IOException {
		XmlOptions xmlOptions = new XmlOptions();
		xmlOptions.setCharacterEncoding(ENCODING_ISO_8859_1);
		object.save(writer, xmlOptions);
	}

	public void setTxConnectionFactory(ConnectionFactory txConnectionFactory) {
		this.txConnectionFactory = txConnectionFactory;
	}

	public void setNoTxConnectionFactory(ConnectionFactory noTxConnectionFactory) {
		this.noTxConnectionFactory = noTxConnectionFactory;
	}

	public void setQueueEditiqueOutput(String queueEditiqueOutput) {
		this.queueEditiqueOutput = queueEditiqueOutput;
	}

	public void setQueueEditiqueInput(String queueEditiqueInput) {
		this.queueEditiqueInput = queueEditiqueInput;
	}

	public void setJmsStatsService(StatsService jmsStatsService) {
		this.jmsStatsService = jmsStatsService;
	}

	public void setReceiveTimeout(int recieveTimeout) {
		this.receiveTimeout = recieveTimeout;
	}

	public void setFoldersService(FoldersService foldersService) {
		this.foldersService = foldersService;
	}
}