package ch.vd.uniregctb.evenement.jms;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;

import org.springframework.jms.support.converter.MessageConversionException;
import org.springframework.jms.support.converter.MessageConverter;

public class EvenementCivilUnitaireMessageConverter implements MessageConverter {

	//private static final Logger LOGGER = Logger.getLogger(EvenementCivilUnitaireMessageConverter.class);

	public Object fromMessage(Message message) throws JMSException, MessageConversionException {

		/*
		try {
			EvenementCivilXmlBean bean = EvenementCivilXmlBean.Factory.parse(message.toString());
			bean.getConjoint();
		}
		catch (XmlException e) {
			LOGGER.error(e,e);
		}
		*/
		return null;//new EvenementCivil();
	}

	public Message toMessage(Object object, Session session) throws JMSException, MessageConversionException {

		/*
		try {
			//EvenementCivilUnitaire ec = (EvenementCivilUnitaire)object;
		}
		catch (XmlException e) {
			LOGGER.error(e,e);
		}
		*/
		//return new Message("The xml string");
		return null;
	}

}
