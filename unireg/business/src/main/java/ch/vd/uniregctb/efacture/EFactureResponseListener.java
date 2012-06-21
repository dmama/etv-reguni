package ch.vd.uniregctb.efacture;

import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;

import ch.vd.technical.esb.EsbMessage;
import ch.vd.technical.esb.jms.EsbMessageEndpointListener;
import ch.vd.uniregctb.jms.MonitorableMessageListener;

public class EFactureResponseListener extends EsbMessageEndpointListener implements MonitorableMessageListener {

	private static final Logger LOGGER = Logger.getLogger(EFactureResponseListener.class);

	private final AtomicInteger nbMessagesRecus = new AtomicInteger(0);

	private EFactureResponseService responseService;

	public void setResponseService(EFactureResponseService responseService) {
		this.responseService = responseService;
	}

	@Override
	public void onEsbMessage(EsbMessage esbMessage) throws Exception {

		nbMessagesRecus.incrementAndGet();

		LOGGER.info(String.format("Arrivée de la réponse e-facture au message '%s'", esbMessage.getBusinessCorrelationId()));
		onResponse(esbMessage.getBusinessCorrelationId());
	}

	private void onResponse(String businessId) {
		responseService.onNewResponse(businessId);
	}

	@Override
	public int getNombreMessagesRecus() {
		return nbMessagesRecus.intValue();
	}
}
