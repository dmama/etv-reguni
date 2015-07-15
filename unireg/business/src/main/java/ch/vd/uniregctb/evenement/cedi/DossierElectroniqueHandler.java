package ch.vd.uniregctb.evenement.cedi;

import java.util.Map;

import org.springframework.core.io.ClassPathResource;

import ch.vd.uniregctb.jms.EsbBusinessException;

public interface DossierElectroniqueHandler<T> {

	ClassPathResource getRequestXSD();

	Class<T> getHandledClass();

	void doHandle(T document, Map<String, String> incomingHeaders) throws EsbBusinessException;
}
