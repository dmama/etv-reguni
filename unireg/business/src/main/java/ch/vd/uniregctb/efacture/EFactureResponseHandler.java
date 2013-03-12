package ch.vd.uniregctb.efacture;

import org.apache.log4j.Logger;

import ch.vd.technical.esb.EsbMessage;
import ch.vd.uniregctb.jms.EsbMessageHandler;

public class EFactureResponseHandler implements EsbMessageHandler {

	private static final Logger LOGGER = Logger.getLogger(EFactureResponseHandler.class);

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
			LOGGER.error(e, e);
			throw e;
		}
	}

	private void onResponse(String businessId) {
		responseService.onNewResponse(businessId);
	}
}
