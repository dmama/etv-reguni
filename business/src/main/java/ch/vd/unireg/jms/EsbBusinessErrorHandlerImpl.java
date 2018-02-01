package ch.vd.unireg.jms;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import ch.vd.technical.esb.EsbMessage;
import ch.vd.technical.esb.EsbMessageImpl;
import ch.vd.technical.esb.jms.EsbJmsTemplate;

/**
 * Classe centralisée d'envoi des erreurs ESB dans une queue spécifique
 */
public class EsbBusinessErrorHandlerImpl implements EsbBusinessErrorHandler {

	private String destinationQueue;
	private EsbJmsTemplate esbTemplate;

	public void setEsbTemplate(EsbJmsTemplate esbTemplate) {
		this.esbTemplate = esbTemplate;
	}

	public void setDestinationQueue(String destinationQueue) {
		this.destinationQueue = destinationQueue;
	}

	@Override
	public void onBusinessError(EsbMessage esbMessage, String errorDescription, @Nullable Throwable throwable, EsbBusinessCode errorCode) throws Exception {
		final EsbMessageImpl m = buildEsbErrorMessage(esbMessage, errorDescription, throwable, errorCode);
		m.setServiceDestination(destinationQueue);
		esbTemplate.send(m);
	}

	protected static EsbMessageImpl buildEsbErrorMessage(EsbMessage esbMessage, String errorDescription, Throwable throwable, EsbBusinessCode errorCode) throws IOException {
		final EsbMessageImpl m = (EsbMessageImpl) (esbMessage instanceof EsbMessageWrapper ? ((EsbMessageWrapper) esbMessage).getUltimateTarget() : esbMessage);

		if (throwable != null) {
			try (Writer exceptionWriter = new StringWriter(); PrintWriter printWriter = new PrintWriter(exceptionWriter)) {
				throwable.printStackTrace(printWriter);
				printWriter.flush();
				m.addHeaderInternal(EsbMessage.EXCEPTION_TRACE, exceptionWriter.toString());
			}
		}

		m.addHeaderInternal(EsbMessage.ERROR_CODE, errorCode.getCode());
		m.addHeaderInternal(EsbMessage.EXCEPTION_MESSAGE, errorDescription);
		m.addHeaderInternal(EsbMessage.ERROR_TYPE, errorCode.getType().toString());

		m.addHeaderInternal(EsbMessageImpl.ESB_ORIG_MESSAGE_ID, esbMessage.getMessageId());
		m.addHeaderInternal(EsbMessage.MESSAGE_ID, UUID.randomUUID().toString());
		m.addHeaderInternal(EsbMessageImpl.ESB_ORIG_APPLICATION, esbMessage.getApplication());
		m.addHeaderInternal(EsbMessageImpl.ESB_ORIG_CONTEXT, esbMessage.getContext());
		m.addHeaderInternal(EsbMessageImpl.ESB_ORIG_DOMAIN, esbMessage.getDomain());
		m.addHeaderInternal(EsbMessageImpl.ESB_ORIG_DESTINATION, esbMessage.getServiceDestination());
		return m;
	}
}
