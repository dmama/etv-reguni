package ch.vd.unireg.jms;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.jms.JmsException;
import org.w3c.dom.Document;

import ch.vd.technical.esb.EsbMessage;
import ch.vd.technical.esb.jms.EsbJmsTemplate;
import ch.vd.technical.esb.jms.EsbMessageListener;
import ch.vd.technical.esb.jms.EsbMessageListenerContainer;
import ch.vd.unireg.common.StringRenderer;
import ch.vd.unireg.load.DetailedLoadMeter;
import ch.vd.unireg.stats.DetailedLoadMonitorable;
import ch.vd.unireg.stats.LoadDetail;

/**
 * Classe d'entrée des messages JMS de l'ESB dans Unireg, et qui loggue les appels et les
 * temps de traitement
 */
public class GentilEsbMessageListenerContainer extends EsbMessageListenerContainer implements InitializingBean, DetailedLoadMonitorable, MessageListenerContainerJmxInterface {

	private static final Logger APP_LOGGER = LoggerFactory.getLogger(GentilEsbMessageListenerContainer.class);
	private static final Logger JMS_LOGGER = LoggerFactory.getLogger("unireg.jms");

	private static final StringRenderer<EsbMessage> RENDERER = new StringRenderer<EsbMessage>() {
		@Override
		public String toString(EsbMessage msg) {
			final String businessId = msg.getBusinessId();
			final Document doc = getDocument(msg);
			final String typePart = getRootElementTypePart(doc, businessId);
			final String uri = EsbMessageHelper.extractNamespaceURI(doc, businessId, APP_LOGGER);
			if (StringUtils.isBlank(msg.getBusinessCorrelationId())) {
				return String.format("queue='%s', sender='%s', businessUser='%s', businessId='%s', ns='%s'%s",
				                     msg.getServiceDestination(), msg.getApplication(), msg.getBusinessUser(),
				                     businessId, uri, typePart);
			}
			else {
				return String.format("queue='%s', sender='%s', businessUser='%s', businessId='%s', businessCorrelationId='%s', ns='%s'%s",
				                     msg.getServiceDestination(), msg.getApplication(), msg.getBusinessUser(),
				                     businessId, msg.getBusinessCorrelationId(), uri, typePart);
			}
		}

		@Nullable
		private Document getDocument(EsbMessage msg) {
			try {
				return msg.getBodyAsDocument();
			}
			catch (Exception e) {
				APP_LOGGER.warn(String.format("Exception lors de la récupération du DOM du message '%s'", msg.getBusinessId()), e);
				return null;
			}
		}

		private String getRootElementTypePart(Document doc, String businessId) {
			final String typePart;
			final String type = EsbMessageHelper.extractRootElementType(doc, businessId, APP_LOGGER);
			if (StringUtils.isNotBlank(type)) {
				typePart = String.format(", type='%s'", type);
			}
			else {
				typePart = StringUtils.EMPTY;
			}
			return typePart;
		}
	};

	private final DetailedLoadMeter<EsbMessage> loadMeter = new DetailedLoadMeter<>(RENDERER);

	private final AtomicInteger nbMessagesRecus = new AtomicInteger(0);
	private final AtomicInteger nbMessagesEnException = new AtomicInteger(0);
	private final AtomicInteger nbMessagesEnErreur = new AtomicInteger(0);

	private EsbJmsTemplate esbTemplate;
	private EsbMessageHandler handler;
	private EsbBusinessErrorHandler esbErrorHandler;
	private EsbMessageTracingFactoryImpl esbMessageTracingFactory;
	private String description;

	public void setHandler(EsbMessageHandler handler) {
		this.handler = handler;
	}

	public void setEsbTemplate(EsbJmsTemplate esbTemplate) {
		this.esbTemplate = esbTemplate;
	}

	public void setEsbErrorHandler(EsbBusinessErrorHandler esbErrorHandler) {
		this.esbErrorHandler = esbErrorHandler;
	}

	public void setEsbMessageTracingFactory(EsbMessageTracingFactoryImpl esbMessageTracingFactory) {
		this.esbMessageTracingFactory = esbMessageTracingFactory;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	private void onEsbMessage(EsbMessage msg) {
		final EsbMessage message = esbMessageTracingFactory != null ? esbMessageTracingFactory.wrap(msg) : msg;
		final Instant start = loadMeter.start(message);
		Throwable t = null;
		String businessError = null;
		final String displayedValue = RENDERER.toString(message);       // on est obligé de le faire tout de suite car l'envoi en queue d'erreur, par exemple, modifie le message en place...
		try {
			try {
				nbMessagesRecus.incrementAndGet();
				handle(message);
			}
			catch (EsbBusinessException e) {
				businessError = e.getMessage();
				onBusinessError(message, businessError, e.getCause(), e.getCode());
				nbMessagesEnErreur.incrementAndGet();
			}
		}
		catch (RuntimeException | Error e) {
			nbMessagesEnException.incrementAndGet();
			t = e;
			throw e;
		}
		catch (Exception e) {
			nbMessagesEnException.incrementAndGet();
			t = e;
			throw new RuntimeException(e);      // wrapping
		}
		finally {
			final Instant end = loadMeter.end();
			if (JMS_LOGGER.isInfoEnabled()) {
				final String exceptMsg = t != null ? String.format(", %s thrown", t.getClass().getName()) : StringUtils.EMPTY;
				final String businessErrorMsg = StringUtils.isNotBlank(businessError) ? String.format(", error='%s'", StringUtils.abbreviate(businessError, 100)) : StringUtils.EMPTY;
				final String logString = String.format("[load=%d] (%d ms) %s%s%s",
				                                       loadMeter.getLoad() + 1,
				                                       Duration.between(start, end).toMillis(),
				                                       displayedValue,
				                                       businessErrorMsg,
				                                       exceptMsg);
				JMS_LOGGER.info(logString);
			}
		}
	}

	/**
	 * Appel du handler
	 * @param message message à traiter
	 * @throws ch.vd.unireg.jms.EsbBusinessException en cas de problème métier à envoyer en queue d'erreur
	 * @throws Exception en cas de souci... causera un renvoi en DLQ
	 */
	private void handle(EsbMessage message) throws Exception {
		handler.onEsbMessage(message);
	}

	/**
	 * C'est ici qu'aterrisent tous les messages à renvoyer en queue d'erreur
	 * @param message le message entrant
	 * @param description la description de l'erreur métier
	 * @param throwable l'exception à la source de cette erreur, si applicable
	 * @param errorCode un code d'erreur
	 * @throws Exception en cas de souci (si une exception saute ici, le message sera envoyé en DLQ)
	 */
	private void onBusinessError(EsbMessage message, String description, @Nullable Throwable throwable, EsbBusinessCode errorCode) throws Exception {
		esbErrorHandler.onBusinessError(message, description, throwable, errorCode);
	}

	@Override
	protected void validateConfiguration() {
		super.validateConfiguration();
		if (handler == null) {
			throw new IllegalArgumentException("Handler must be set");
		}
		if (esbErrorHandler == null) {
			throw new IllegalArgumentException("EsbErrorHandler must be set");
		}
		if (esbTemplate == null) {
			throw new IllegalArgumentException("EsbTemplate must be set");
		}
	}

	@Override
	public void initialize() {

		// l'arrivée d'un message appellera la méthode onEsbMessage(EsbMessage)
		final EsbMessageListener messageListener = new EsbMessageListener() {
			@Override
			public void onEsbMessage(EsbMessage message) throws Exception {
				GentilEsbMessageListenerContainer.this.onEsbMessage(message);
			}
		};
		messageListener.setEsbTemplate(esbTemplate);

		setMessageListener(messageListener);
		super.initialize();
	}

	@Override
	public List<LoadDetail> getLoadDetails() {
		return loadMeter.getLoadDetails();
	}

	@Override
	public int getLoad() {
		return loadMeter.getLoad();
	}

	@Override
	public void start() throws JmsException {
		super.start();
		if (APP_LOGGER.isInfoEnabled()) {
			APP_LOGGER.info(String.format("Ecoute sur la queue '%s' démarrée", getDestinationName()));
		}
	}

	@Override
	public void stop() throws JmsException {
		super.stop();
		if (APP_LOGGER.isInfoEnabled()) {
			APP_LOGGER.info(String.format("Ecoute sur la queue '%s' arrêtée", getDestinationName()));
		}
	}

	@Override
	public int getReceivedMessages() {
		return nbMessagesRecus.intValue();
	}

	@Override
	public int getMessagesWithException() {
		return nbMessagesEnException.intValue();
	}

	@Override
	public int getMessagesWithBusinessError() {
		return nbMessagesEnErreur.intValue();
	}

	@Override
	public String getDescription() {
		return description;
	}
}
