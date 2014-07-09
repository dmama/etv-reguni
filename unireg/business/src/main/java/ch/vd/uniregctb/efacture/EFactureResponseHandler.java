package ch.vd.uniregctb.efacture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.technical.esb.EsbMessage;
import ch.vd.uniregctb.jms.EsbMessageHandler;

public class EFactureResponseHandler implements EsbMessageHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(EFactureResponseHandler.class);

	private EFactureResponseService responseService;

	public void setResponseService(EFactureResponseService responseService) {
		this.responseService = responseService;
	}

	@Override
	public void onEsbMessage(EsbMessage esbMessage) {

		LOGGER.info(String.format("Arrivée de la réponse e-facture au message '%s'", esbMessage.getBusinessCorrelationId()));
		try {
			onResponse(esbMessage.getBusinessCorrelationId());
		}
		catch (RuntimeException e) {
			LOGGER.error(e.getMessage(), e);
			throw e;
		}
	}

	private void onResponse(String businessId) {
		responseService.onNewResponse(businessId);
	}
}
