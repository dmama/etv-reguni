package ch.vd.technical.esb.jms;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Queue;
import javax.jms.TextMessage;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import ch.vd.technical.esb.EsbMessage;
import ch.vd.technical.esb.EsbMessageImpl;
import ch.vd.technical.esb.marshaler.JmsMarshaler;

/**
 * Implémentation du message listener ESB qui est <b>entièrement</b> inclus dans une transaction (à la différence de la classe {@link EsbMessageListenerTransacted} qui crée une transaction limitée à
 * la méthode <i>onEsbMessage</i>).
 */
@Transactional(rollbackFor = Throwable.class)
public abstract class TransactionalEsbMessageListener extends AbstractEsbMessageListener {

	public final void onMessage(Message message) {

		Assert.notNull(esbTemplate, "esbSyncTemplate must be set, check for overriding method");
		Assert.notNull(esbTemplate.getEsbStore(), "esbStore must be set");

		try {

			if (message instanceof TextMessage) {
				TextMessage tm = (TextMessage) message;
				// get real queue name from JMS Message
				String origineDestinationName = ((Queue) tm.getJMSDestination()).getQueueName();

				EsbMessageImpl m = (EsbMessageImpl) JmsMarshaler.fromJms(tm, esbTemplate.getEsbStore());
				// avoid problem sending error message if serviceDestination not set by mediation
				m.setServiceDestination(origineDestinationName);

				// si message n'est pas filtré alors appel de onEsbMessage

				if (!filterTestMessage(m)) {
					m.setServiceDestination(ServiceDestinationHelper.resolveServiceDestination(m.getServiceDestination()));
					m.addHeaderInternal(EsbMessage.MESSAGE_RECEIVE_DATE, getCurrentDateAsString());

					// Do not ack an internal message
					if (!m.isInternal()) {
						esbTemplate.ack(m, origineDestinationName);
					}

					onEsbMessage(m);
				}
			}
			else {
				throw new Exception("JMS Message must be a TextMessage");
			}

		}
		catch (Exception e) {
			String messageId = "Business ID=";
			try {
				messageId += message.getStringProperty("businessId");
			}
			catch (JMSException e1) {
				messageId += "n/a";
			}
			throw new RuntimeException(messageId, e);
		}

	}

}
