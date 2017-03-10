package ch.vd.uniregctb.evenement;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.technical.esb.EsbMessage;
import ch.vd.uniregctb.jms.EsbBusinessCode;
import ch.vd.uniregctb.jms.EsbBusinessException;
import ch.vd.uniregctb.jms.EsbMessageHandler;
import ch.vd.uniregctb.jms.EsbMessageHelper;

/**
 * Dispatcher à la sortie de la queue JMS, selon le namespace du message entrant
 */
public class EvenementNamespaceDispatchingEsbHandler implements EsbMessageHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(EvenementNamespaceDispatchingEsbHandler.class);

	private Map<String, EsbMessageHandler> handlers;

	public void setHandlers(Map<String, EsbMessageHandler> handlers) {
		this.handlers = handlers;
	}

	@Override
	public void onEsbMessage(EsbMessage message) throws Exception {
		final String ns = EsbMessageHelper.extractNamespaceURI(message, LOGGER);
		final EsbMessageHandler handler = handlers.get(ns);
		if (handler != null) {
			handler.onEsbMessage(message);
		}
		else {
			throw new EsbBusinessException(EsbBusinessCode.MESSAGE_NON_SUPPORTE, String.format("Namespace non-reconnu : %s", ns), null);
		}
	}
}
