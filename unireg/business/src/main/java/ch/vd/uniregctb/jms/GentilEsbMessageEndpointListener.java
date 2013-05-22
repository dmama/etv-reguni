package ch.vd.uniregctb.jms;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.InitializingBean;

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

	private static final Logger APP_LOGGER = Logger.getLogger(GentilEsbMessageEndpointListener.class);
	private static final Logger JMS_LOGGER = Logger.getLogger("unireg.jms");

	private static final LoadDetailRenderer<EsbMessage> RENDERER = new LoadDetailRenderer<EsbMessage>() {
		@Override
		public String toString(EsbMessage msg) {
			if (StringUtils.isBlank(msg.getBusinessCorrelationId())) {
				return String.format("queue='%s', sender='%s', businessUser='%s', businessId='%s', ns='%s'",
				                     msg.getServiceDestination(), msg.getApplication(), msg.getBusinessUser(),
				                     msg.getBusinessId(), extractNamespaceURI(msg));
			}
			else {
				return String.format("queue='%s', sender='%s', businessUser='%s', businessId='%s', businessCorrelationId='%s', ns='%s'",
				                     msg.getServiceDestination(), msg.getApplication(), msg.getBusinessUser(),
				                     msg.getBusinessId(), msg.getBusinessCorrelationId(), extractNamespaceURI(msg));
			}
		}
	};

	private final DetailedLoadMeter<EsbMessage> loadMeter = new DetailedLoadMeter<>(RENDERER);

	private final AtomicInteger nbMessagesRecus = new AtomicInteger(0);
	private final AtomicInteger nbMessagesEnException = new AtomicInteger(0);
	private final AtomicInteger nbMessagesEnErreur = new AtomicInteger(0);

	private EsbMessageHandler handler;
	private EsbBusinessErrorHandler esbErrorHandler;

	public void setHandler(EsbMessageHandler handler) {
		this.handler = handler;
	}

	public void setEsbErrorHandler(EsbBusinessErrorHandler esbErrorHandler) {
		this.esbErrorHandler = esbErrorHandler;
	}

	/**
	 * Essaie d'extraire le <i>namespace</i> du <i>root element</i> du message passé en paramètre. S'il n'existe pas, une chaîne vide est retournée.
	 * @param msg message ESB dont on veut connaître le <i>namespace</i>
	 * @return l'URI du <i>namespace</i> extrait, une chaîne vide en absence de <i>namespace</i> et "???" en cas d'erreur à l'extraction
	 */
	private static String extractNamespaceURI(EsbMessage msg) {
		try {
			return StringUtils.trimToEmpty(msg.getBodyAsDocument().getDocumentElement().getNamespaceURI());
		}
		catch (Exception e) {
			APP_LOGGER.warn(String.format("Exception lors de l'extraction du namespace du message '%s'", msg.getBusinessId()), e);
			return "???";
		}
	}

	@Override
	public void onEsbMessage(EsbMessage message) {
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
				final String msg = String.format("[load=%d] (%d ms) %s%s%s",
				                                 loadMeter.getLoad() + 1,
				                                 TimeUnit.NANOSECONDS.toMillis(end - start),
				                                 displayedValue,
				                                 businessErrorMsg,
				                                 exceptMsg);
				JMS_LOGGER.info(msg);
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
