package ch.vd.uniregctb.editique.impl;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.springframework.jms.core.MessageCreator;

import ch.vd.editique.service.enumeration.TypeFormat;
import ch.vd.editique.service.enumeration.TypeImpression;
import ch.vd.editique.service.enumeration.TypeMessagePropertiesNames;

public class RequestSendMessageCreator implements MessageCreator {

	private static final String DI_ID = "DI_ID";

	private static final int MAX_PRIORITY = 9;

	private Message message;

	private String xml;

	private TypeImpression typeImpression;

	private String typeDocument;

	private String nomDocument;

	private TypeFormat typeFormat;

	private boolean archive;

	public Message getMessage() {
		return message;
	}

	public void setMessage(Message message) {
		this.message = message;
	}

	public String getXml() {
		return xml;
	}

	public void setXml(String xml) {
		this.xml = xml;
	}

	public TypeImpression getTypeImpression() {
		return typeImpression;
	}

	public void setTypeImpression(TypeImpression typeImpression) {
		this.typeImpression = typeImpression;
	}

	public String getTypeDocument() {
		return typeDocument;
	}

	public void setTypeDocument(String typeDocument) {
		this.typeDocument = typeDocument;
	}

	public String getNomDocument() {
		return nomDocument;
	}

	public void setNomDocument(String nomDocument) {
		this.nomDocument = nomDocument;
	}

	public boolean isArchive() {
		return archive;
	}

	public void setArchive(boolean archive) {
		this.archive = archive;
	}

	public TypeFormat getTypeFormat() {
		return typeFormat;
	}

	public void setTypeFormat(TypeFormat typeFormat) {
		this.typeFormat = typeFormat;
	}

	public RequestSendMessageCreator(String xml, String nomDocument, String typeDocument, TypeImpression typeImpression, TypeFormat typeFormat, boolean archive) {
		super();
		this.xml = xml;
		this.nomDocument = nomDocument;
		this.typeDocument = typeDocument;
		this.typeImpression = typeImpression;
		this.archive = archive;
		this.typeFormat = typeFormat;
	}

	public Message createMessage(Session session) throws JMSException {
		TextMessage message = session.createTextMessage();
		message.setText(xml);
		message.setStringProperty(TypeMessagePropertiesNames.PRINT_MODE_MESSAGE_PROPERTY_NAME.toString(), typeImpression
				.toString());
		message.setBooleanProperty(TypeMessagePropertiesNames.ARCHIVE_MESSAGE_PROPERTY_FLAG.toString(), archive);
		message.setStringProperty(TypeMessagePropertiesNames.DOCUMENT_TYPE_MESSAGE_PROPERTY_NAME.toString(), typeDocument);
		if (typeImpression == TypeImpression.DIRECT) {
			message.setStringProperty(TypeMessagePropertiesNames.RETURN_FORMAT_MESSAGE_PROPERTY_NAME.toString(), typeFormat.toString());
			message.setJMSPriority(MAX_PRIORITY);
			//TODO (FDE) workaround
			message.setStringProperty(DI_ID, nomDocument);
		}
		setMessage(message);
		return message;
	}

}
