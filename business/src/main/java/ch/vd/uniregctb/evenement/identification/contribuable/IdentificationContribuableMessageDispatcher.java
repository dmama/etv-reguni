package ch.vd.uniregctb.evenement.identification.contribuable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.technical.esb.EsbMessage;
import ch.vd.uniregctb.jms.EsbMessageHandler;
import ch.vd.uniregctb.jms.EsbMessageHelper;

/**
 * Handler de messages ESB d'identification capable de déterminer s'il faut utiliser la version xmlbeans ou la version jaxb2
 * pour traiter le message entrant en se basant sur le numéro de version du namespace (= derniers caractères [0-9.]+ du namespace)
 */
public class IdentificationContribuableMessageDispatcher implements EsbMessageHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(IdentificationContribuableMessageDispatcher.class);
	private static final Pattern VERSION_EXTRACTING_PATTERN = Pattern.compile("[^0-9.]([0-9]+)(\\.[0-9]+)*$");

	private EsbMessageHandler v1Handler;
	private EsbMessageHandler laterVersionsHandler;

	@SuppressWarnings("UnusedDeclaration")
	public void setV1Handler(EsbMessageHandler v1Handler) {
		this.v1Handler = v1Handler;
	}

	@SuppressWarnings("UnusedDeclaration")
	public void setLaterVersionsHandler(EsbMessageHandler laterVersionsHandler) {
		this.laterVersionsHandler = laterVersionsHandler;
	}

	@Override
	public void onEsbMessage(EsbMessage message) throws Exception {
		final String ns = EsbMessageHelper.extractNamespaceURI(message, LOGGER);
		if (isForV1(ns)) {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug(String.format("Message d'identification '%s' (ns '%s') orienté vers traitement v1", message.getBusinessId(), ns));
			}
			v1Handler.onEsbMessage(message);
		}
		else {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug(String.format("Message d'identification '%s' (ns '%s') orienté vers traitement v2+", message.getBusinessId(), ns));
			}
			laterVersionsHandler.onEsbMessage(message);
		}
	}

	protected static boolean isForV1(String ns) {
		final Matcher matcher = VERSION_EXTRACTING_PATTERN.matcher(ns);
		final boolean forV1;
		if (matcher.find()) {
			final int major = Integer.parseInt(matcher.group(1));
			forV1 = major == 1;
		}
		else {
			LOGGER.warn(String.format("Impossible de déterminer la version du service d'identification à utiliser depuis le namespace '%s', on part sur la plus récente...", ns));
			forV1 = false;
		}
		return forV1;
	}
}
