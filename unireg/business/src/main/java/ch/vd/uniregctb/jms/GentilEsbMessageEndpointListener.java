package ch.vd.uniregctb.jms;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.technical.esb.EsbMessage;
import ch.vd.technical.esb.jms.EsbMessageEndpointListener;
import ch.vd.uniregctb.common.StringRenderer;
import ch.vd.uniregctb.load.DetailedLoadMeter;
import ch.vd.uniregctb.load.DetailedLoadMonitorable;
import ch.vd.uniregctb.load.LoadDetail;

/**
 * Classe d'entrée des messages JMS de l'ESB dans Unireg, et qui loggue les appels et les
 * temps de traitement
 */
public class GentilEsbMessageEndpointListener extends EsbMessageEndpointListener implements InitializingBean, DetailedLoadMonitorable, MonitorableMessageListener {

	private static final Logger APP_LOGGER = LoggerFactory.getLogger(GentilEsbMessageEndpointListener.class);
	private static final Logger JMS_LOGGER = LoggerFactory.getLogger("unireg.jms");

	private static final StringRenderer<EsbMessage> RENDERER = new StringRenderer<EsbMessage>() {
		@Override
		public String toString(EsbMessage msg) {
			final String typePart = getRootElementTypePart(msg);
			if (StringUtils.isBlank(msg.getBusinessCorrelationId())) {
				return String.format("queue='%s', sender='%s', businessUser='%s', businessId='%s', ns='%s'%s",
				                     msg.getServiceDestination(), msg.getApplication(), msg.getBusinessUser(),
				                     msg.getBusinessId(), EsbMessageHelper.extractNamespaceURI(msg, APP_LOGGER), typePart);
			}
			else {
				return String.format("queue='%s', sender='%s', businessUser='%s', businessId='%s', businessCorrelationId='%s', ns='%s'%s",
				                     msg.getServiceDestination(), msg.getApplication(), msg.getBusinessUser(),
				                     msg.getBusinessId(), msg.getBusinessCorrelationId(), EsbMessageHelper.extractNamespaceURI(msg, APP_LOGGER), typePart);
			}
		}

		private String getRootElementTypePart(EsbMessage msg) {
			final String typePart;
			final String type = EsbMessageHelper.extractRootElementType(msg, APP_LOGGER);
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

	private EsbMessageHandler handler;
	private EsbBusinessErrorHandler esbErrorHandler;
	private EsbMessageTracingFactoryImpl esbMessageTracingFactory;

	public void setHandler(EsbMessageHandler handler) {
		this.handler = handler;
	}

	public void setEsbErrorHandler(EsbBusinessErrorHandler esbErrorHandler) {
		this.esbErrorHandler = esbErrorHandler;
	}

	public void setEsbMessageTracingFactory(EsbMessageTracingFactoryImpl esbMessageTracingFactory) {
		this.esbMessageTracingFactory = esbMessageTracingFactory;
	}

	@Override
	public void onEsbMessage(EsbMessage msg) {
		final EsbMessage message = esbMessageTracingFactory != null ? esbMessageTracingFactory.wrap(msg) : msg;
		final long start = loadMeter.start(message);
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
			final long end = loadMeter.end();
			if (JMS_LOGGER.isInfoEnabled()) {
				final String exceptMsg = t != null ? String.format(", %s thrown", t.getClass().getName()) : StringUtils.EMPTY;
				final String businessErrorMsg = StringUtils.isNotBlank(businessError) ? String.format(", error='%s'", StringUtils.abbreviate(businessError, 100)) : StringUtils.EMPTY;
				final String logString = String.format("[load=%d] (%d ms) %s%s%s",
				                                       loadMeter.getLoad() + 1,
				                                       TimeUnit.NANOSECONDS.toMillis(end - start),
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
	 * @throws EsbBusinessException en cas de problème métier à envoyer en queue d'erreur
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
	public void afterPropertiesSet() throws Exception {
		super.afterPropertiesSet();
		if (handler == null) {
			throw new IllegalArgumentException("Handler must be set");
		}
		if (esbErrorHandler == null) {
			throw new IllegalArgumentException("EsbErrorHandler must be set");
		}
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
	public int getNombreMessagesRecus() {
		return nbMessagesRecus.intValue();
	}

	@Override
	public int getNombreMessagesRenvoyesEnErreur() {
		return nbMessagesEnErreur.intValue();
	}

	@Override
	public int getNombreMessagesRenvoyesEnException() {
		return nbMessagesEnException.intValue();
	}
}
