package ch.vd.uniregctb.jms;

import ch.vd.uniregctb.interfaces.service.ServiceTracing;
import ch.vd.uniregctb.stats.StatsService;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.jms.JmsException;
import org.springframework.jms.core.*;

import javax.jms.Destination;
import javax.jms.Message;
import javax.jms.Queue;

/**
 * Version spécialisée du template Jsm qui relève les temps d'exécution des méthodes publiques.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class JmsTemplateTracing implements JmsOperations, InitializingBean, DisposableBean {

	private JmsTemplate target;
	private StatsService statsService;

	private final ServiceTracing tracing = new ServiceTracing();

	public void setTarget(JmsTemplate target) {
		this.target = target;
	}

	public void setStatsService(StatsService statsService) {
		this.statsService = statsService;
	}

	public void afterPropertiesSet() throws Exception {
		if (statsService != null) {
			statsService.registerRaw(target.getDefaultDestinationName(), tracing);
		}
	}

	public void destroy() throws Exception {
		if (statsService != null) {
			statsService.unregisterRaw(target.getDefaultDestinationName());
		}
	}

	public Object execute(SessionCallback action) throws JmsException {
		long time = tracing.start();
		try {
			return target.execute(action);
		}
		finally {
			tracing.end(time);
		}
	}

	public Object execute(ProducerCallback action) throws JmsException {
		long time = tracing.start();
		try {
			return target.execute(action);
		}
		finally {
			tracing.end(time);
		}
	}

	public Object execute(Destination destination, ProducerCallback action) throws JmsException {
		long time = tracing.start();
		try {
			return target.execute(destination, action);
		}
		finally {
			tracing.end(time);
		}
	}

	public Object execute(String destinationName, ProducerCallback action) throws JmsException {
		long time = tracing.start();
		try {
			return target.execute(destinationName, action);
		}
		finally {
			tracing.end(time);
		}
	}

	public void send(MessageCreator messageCreator) throws JmsException {
		long time = tracing.start();
		try {
			target.send(messageCreator);
		}
		finally {
			tracing.end(time);
		}
	}

	public void send(Destination destination, MessageCreator messageCreator) throws JmsException {
		long time = tracing.start();
		try {
			target.send(destination, messageCreator);
		}
		finally {
			tracing.end(time);
		}
	}

	public void send(String destinationName, MessageCreator messageCreator) throws JmsException {
		long time = tracing.start();
		try {
			target.send(destinationName, messageCreator);
		}
		finally {
			tracing.end(time);
		}
	}

	public void convertAndSend(Object message) throws JmsException {
		long time = tracing.start();
		try {
			target.convertAndSend(message);
		}
		finally {
			tracing.end(time);
		}
	}

	public void convertAndSend(Destination destination, Object message) throws JmsException {
		long time = tracing.start();
		try {
			target.convertAndSend(destination, message);
		}
		finally {
			tracing.end(time);
		}
	}

	public void convertAndSend(String destinationName, Object message) throws JmsException {
		long time = tracing.start();
		try {
			target.convertAndSend(destinationName, message);
		}
		finally {
			tracing.end(time);
		}
	}

	public void convertAndSend(Object message, MessagePostProcessor postProcessor) throws JmsException {
		long time = tracing.start();
		try {
			target.convertAndSend(message, postProcessor);
		}
		finally {
			tracing.end(time);
		}
	}

	public void convertAndSend(Destination destination, Object message, MessagePostProcessor postProcessor) throws JmsException {
		long time = tracing.start();
		try {
			target.convertAndSend(destination, message, postProcessor);
		}
		finally {
			tracing.end(time);
		}
	}

	public void convertAndSend(String destinationName, Object message, MessagePostProcessor postProcessor) throws JmsException {
		long time = tracing.start();
		try {
			target.convertAndSend(destinationName, message, postProcessor);
		}
		finally {
			tracing.end(time);
		}
	}

	public Message receive() throws JmsException {
		long time = tracing.start();
		try {
			return target.receive();
		}
		finally {
			tracing.end(time);
		}
	}

	public Message receive(Destination destination) throws JmsException {
		long time = tracing.start();
		try {
			return target.receive(destination);
		}
		finally {
			tracing.end(time);
		}
	}

	public Message receive(String destinationName) throws JmsException {
		long time = tracing.start();
		try {
			return target.receive(destinationName);
		}
		finally {
			tracing.end(time);
		}
	}

	public Message receiveSelected(String messageSelector) throws JmsException {
		long time = tracing.start();
		try {
			return target.receiveSelected(messageSelector);
		}
		finally {
			tracing.end(time);
		}
	}

	public Message receiveSelected(Destination destination, String messageSelector) throws JmsException {
		long time = tracing.start();
		try {
			return target.receiveSelected(destination, messageSelector);
		}
		finally {
			tracing.end(time);
		}
	}

	public Message receiveSelected(String destinationName, String messageSelector) throws JmsException {
		long time = tracing.start();
		try {
			return target.receiveSelected(destinationName, messageSelector);
		}
		finally {
			tracing.end(time);
		}
	}

	public Object receiveAndConvert() throws JmsException {
		long time = tracing.start();
		try {
			return target.receiveAndConvert();
		}
		finally {
			tracing.end(time);
		}
	}

	public Object receiveAndConvert(Destination destination) throws JmsException {
		long time = tracing.start();
		try {
			return target.receiveAndConvert(destination);
		}
		finally {
			tracing.end(time);
		}
	}

	public Object receiveAndConvert(String destinationName) throws JmsException {
		long time = tracing.start();
		try {
			return target.receiveAndConvert(destinationName);
		}
		finally {
			tracing.end(time);
		}
	}

	public Object receiveSelectedAndConvert(String messageSelector) throws JmsException {
		long time = tracing.start();
		try {
			return target.receiveSelectedAndConvert(messageSelector);
		}
		finally {
			tracing.end(time);
		}
	}

	public Object receiveSelectedAndConvert(Destination destination, String messageSelector) throws JmsException {
		long time = tracing.start();
		try {
			return target.receiveSelectedAndConvert(destination, messageSelector);
		}
		finally {
			tracing.end(time);
		}
	}

	public Object receiveSelectedAndConvert(String destinationName, String messageSelector) throws JmsException {
		long time = tracing.start();
		try {
			return target.receiveSelectedAndConvert(destinationName, messageSelector);
		}
		finally {
			tracing.end(time);
		}
	}

	public Object browse(BrowserCallback action) throws JmsException {
		long time = tracing.start();
		try {
			return target.browse(action);
		}
		finally {
			tracing.end(time);
		}
	}

	public Object browse(Queue queue, BrowserCallback action) throws JmsException {
		long time = tracing.start();
		try {
			return target.browse(queue, action);
		}
		finally {
			tracing.end(time);
		}
	}

	public Object browse(String queueName, BrowserCallback action) throws JmsException {
		long time = tracing.start();
		try {
			return target.browse(queueName, action);
		}
		finally {
			tracing.end(time);
		}
	}

	public Object browseSelected(String messageSelector, BrowserCallback action) throws JmsException {
		long time = tracing.start();
		try {
			return target.browseSelected(messageSelector, action);
		}
		finally {
			tracing.end(time);
		}
	}

	public Object browseSelected(Queue queue, String messageSelector, BrowserCallback action) throws JmsException {
		long time = tracing.start();
		try {
			return target.browseSelected(queue, messageSelector, action);
		}
		finally {
			tracing.end(time);
		}
	}

	public Object browseSelected(String queueName, String messageSelector, BrowserCallback action) throws JmsException {
		long time = tracing.start();
		try {
			return target.browseSelected(queueName, messageSelector, action);
		}
		finally {
			tracing.end(time);
		}
	}
}
