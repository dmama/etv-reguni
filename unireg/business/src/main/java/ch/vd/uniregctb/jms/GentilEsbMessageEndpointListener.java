package ch.vd.uniregctb.jms;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.technical.esb.ErrorType;
import ch.vd.technical.esb.EsbMessage;
import ch.vd.technical.esb.jms.EsbMessageEndpointListener;
import ch.vd.uniregctb.load.DetailedLoadMeter;
import ch.vd.uniregctb.load.DetailedLoadMonitorable;
import ch.vd.uniregctb.load.LoadDetail;
import ch.vd.uniregctb.load.LoadDetailRenderer;

/**
 * Classe d'entrée des messages JMS de l'ESB dans Unireg, et qui loggue les appels et les
 * temps de traitement
 */
public class GentilEsbMessageEndpointListener extends EsbMessageEndpointListener implements InitializingBean, DetailedLoadMonitorable, MonitorableMessageListener {

	private static final Logger LOGGER = Logger.getLogger("unireg.jms");

	private static final LoadDetailRenderer<EsbMessage> RENDERER = new LoadDetailRenderer<EsbMessage>() {
		@Override
		public String toString(EsbMessage msg) {
			if (StringUtils.isBlank(msg.getBusinessCorrelationId())) {
				return String.format("queue=%s, sender='%s', businessUser='%s', businessId='%s'",
				                     msg.getServiceDestination(), msg.getApplication(), msg.getBusinessUser(),
				                     msg.getBusinessId());
			}
			else {
				return String.format("queue=%s, sender='%s', businessUser='%s', businessId='%s', businessCorrelationId='%s'",
				                     msg.getServiceDestination(), msg.getApplication(), msg.getBusinessUser(),
				                     msg.getBusinessId(), msg.getBusinessCorrelationId());
			}
		}
	};

	private final DetailedLoadMeter<EsbMessage> loadMeter = new DetailedLoadMeter<>(RENDERER);

	private final AtomicInteger nbMessagesRecus = new AtomicInteger(0);
	private final AtomicInteger nbMessagesEnException = new AtomicInteger(0);
	private final AtomicInteger nbMessagesEnErreur = new AtomicInteger(0);

	private EsbMessageHandler handler;

	public void setHandler(EsbMessageHandler handler) {
		this.handler = handler;
	}

	@Override
	public void onEsbMessage(EsbMessage message) throws Exception {
		final long start = loadMeter.start(message);
		Throwable t = null;
		String businessError = null;
		final String displayedValue = RENDERER.toString(message);       // on est obligé de le faire tout de suite car l'envoi en queue d'erreur, par exemple, modifie le message en place...
		try {
			nbMessagesRecus.incrementAndGet();
			handle(message);
		}
		catch (EsbBusinessException e) {
			try {
				businessError = buildErrorMessage(e);
				onBusinessError(message, businessError, e.getCause(), e.getErrorType(), e.getErrorCode());
				nbMessagesEnErreur.incrementAndGet();
			}
			catch (Throwable throwable) {
				nbMessagesEnException.incrementAndGet();
				t = throwable;
				throw throwable;
			}
		}
		catch (Throwable throwable) {
			nbMessagesEnException.incrementAndGet();
			t = throwable;
			throw throwable;
		}
		finally {
			final long end = loadMeter.end();
			if (LOGGER.isInfoEnabled()) {
				final String exceptMsg = t != null ? String.format(", %s thrown", t.getClass().getName()) : StringUtils.EMPTY;
				final String businessErrorMsg = StringUtils.isNotBlank(businessError) ? String.format(", error='%s'", StringUtils.abbreviate(businessError, 100)) : StringUtils.EMPTY;
				final String msg = String.format("[load=%d] (%d ms) %s%s%s",
				                                 loadMeter.getLoad() + 1,
				                                 TimeUnit.NANOSECONDS.toMillis(end - start),
				                                 displayedValue,
				                                 businessErrorMsg,
				                                 exceptMsg);
				LOGGER.info(msg);
			}
		}
	}

	private static String buildErrorMessage(EsbBusinessException e) {
		final String msg;
		if (StringUtils.isBlank(e.getMessage())) {
			Throwable causeWithMessage = e.getCause();
			while (causeWithMessage != null && StringUtils.isBlank(causeWithMessage.getMessage())) {
				causeWithMessage = causeWithMessage.getCause();
			}
			if (causeWithMessage != null) {
				msg = causeWithMessage.getMessage();
			}
			else if (e.getCause() != null) {
				msg = e.getCause().getClass().getName();
			}
			else {
				msg = e.getLibelle();
			}
		}
		else {
			msg = e.getMessage();
		}
		return msg;
	}

	/**
	 * Méthode surchargeable pour appeler le handler
	 * @param message message à traiter
	 * @throws EsbBusinessException en cas de problème métier à envoyer en queue d'erreur
	 * @throws Exception en cas de souci... causera un renvoi en DLQ
	 */
	protected void handle(EsbMessage message) throws Exception {
		handler.onEsbMessage(message);
	}

	/**
	 * C'est ici qu'aterrisent tous les messages à renvoyer en queue d'erreur
	 * @param message le message entrant
	 * @param description la description de l'erreur métier
	 * @param throwable l'exception à la source de cette erreur, si applicable
	 * @param errorType le type d'erreur
	 * @param errorCode un code d'erreur
	 * @throws Exception en cas de souci (si une exception saute ici, le message sera envoyé en DLQ)
	 */
	protected void onBusinessError(EsbMessage message, String description, @Nullable Throwable throwable, ErrorType errorType, String errorCode) throws Exception {
		final Exception ex;
		if (throwable != null && !(throwable instanceof Exception)) {
			// wrapping...
			ex = new Exception(throwable);
		}
		else {
			ex = (Exception) throwable;
		}
		getEsbTemplate().sendError(message, description, ex, errorType, errorCode);
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if (handler == null) {
			throw new IllegalArgumentException("Handler must be set");
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
