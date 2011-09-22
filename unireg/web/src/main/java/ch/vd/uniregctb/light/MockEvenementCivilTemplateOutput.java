package ch.vd.uniregctb.light;

import javax.jms.Destination;
import javax.jms.Message;
import javax.jms.Queue;

import org.springframework.jms.JmsException;
import org.springframework.jms.core.BrowserCallback;
import org.springframework.jms.core.JmsOperations;
import org.springframework.jms.core.MessageCreator;
import org.springframework.jms.core.MessagePostProcessor;
import org.springframework.jms.core.ProducerCallback;
import org.springframework.jms.core.SessionCallback;

public class MockEvenementCivilTemplateOutput implements JmsOperations {

	@Override
	public Object browse(BrowserCallback action) throws JmsException {
		return null;
	}

	@Override
	public Object browse(Queue queue, BrowserCallback action) throws JmsException {
		return null;
	}

	@Override
	public Object browse(String queueName, BrowserCallback action) throws JmsException {
		return null;
	}

	@Override
	public Object browseSelected(String messageSelector, BrowserCallback action) throws JmsException {
		return null;
	}

	@Override
	public Object browseSelected(Queue queue, String messageSelector, BrowserCallback action) throws JmsException {
		return null;
	}

	@Override
	public Object browseSelected(String queueName, String messageSelector, BrowserCallback action) throws JmsException {
		return null;
	}

	@Override
	public void convertAndSend(Object message) throws JmsException {
	}

	@Override
	public void convertAndSend(Destination destination, Object message) throws JmsException {
	}

	@Override
	public void convertAndSend(String destinationName, Object message) throws JmsException {
	}

	@Override
	public void convertAndSend(Object message, MessagePostProcessor postProcessor) throws JmsException {
	}

	@Override
	public void convertAndSend(Destination destination, Object message, MessagePostProcessor postProcessor) throws JmsException {
	}

	@Override
	public void convertAndSend(String destinationName, Object message, MessagePostProcessor postProcessor) throws JmsException {
	}

	@Override
	public Object execute(SessionCallback action) throws JmsException {
		return null;
	}

	@Override
	public Object execute(ProducerCallback action) throws JmsException {
		return null;
	}

	@Override
	public Object execute(Destination destination, ProducerCallback action) throws JmsException {
		return null;
	}

	@Override
	public Object execute(String destinationName, ProducerCallback action) throws JmsException {
		return null;
	}

	@Override
	public Message receive() throws JmsException {
		return null;
	}

	@Override
	public Message receive(Destination destination) throws JmsException {
		return null;
	}

	@Override
	public Message receive(String destinationName) throws JmsException {
		return null;
	}

	@Override
	public Object receiveAndConvert() throws JmsException {
		return null;
	}

	@Override
	public Object receiveAndConvert(Destination destination) throws JmsException {
		return null;
	}

	@Override
	public Object receiveAndConvert(String destinationName) throws JmsException {
		return null;
	}

	@Override
	public Message receiveSelected(String messageSelector) throws JmsException {
		return null;
	}

	@Override
	public Message receiveSelected(Destination destination, String messageSelector) throws JmsException {
		return null;
	}

	@Override
	public Message receiveSelected(String destinationName, String messageSelector) throws JmsException {
		return null;
	}

	@Override
	public Object receiveSelectedAndConvert(String messageSelector) throws JmsException {
		return null;
	}

	@Override
	public Object receiveSelectedAndConvert(Destination destination, String messageSelector) throws JmsException {
		return null;
	}

	@Override
	public Object receiveSelectedAndConvert(String destinationName, String messageSelector) throws JmsException {
		return null;
	}

	@Override
	public void send(MessageCreator messageCreator) throws JmsException {
	}

	@Override
	public void send(Destination destination, MessageCreator messageCreator) throws JmsException {
	}

	@Override
	public void send(String destinationName, MessageCreator messageCreator) throws JmsException {
	}

}
