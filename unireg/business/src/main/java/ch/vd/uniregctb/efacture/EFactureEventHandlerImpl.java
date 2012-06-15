package ch.vd.uniregctb.efacture;

import org.apache.log4j.Logger;
import org.springframework.core.io.ClassPathResource;

public class EFactureEventHandlerImpl implements EFactureEventHandler {

	private final Logger LOGGER = Logger.getLogger(EFactureEventHandlerImpl.class);

	@Override
	public void handle(EFactureEvent event) {
		if (event instanceof ChangementSituationDestinataire) {

			// TODO e-facture à faire

			final ChangementSituationDestinataire chgt = (ChangementSituationDestinataire) event;
			LOGGER.info(String.format("Reçu demande de type %s sur le contribuable %d", chgt.getAction(), chgt.getNoTiers()));
		}
		else {
			throw new IllegalArgumentException("Type d'événement non supporté : " + event.getClass());
		}
	}

	@Override
	public ClassPathResource getRequestXSD() {
		return new ClassPathResource("eVD-0025-1-0.xsd");
	}
}
