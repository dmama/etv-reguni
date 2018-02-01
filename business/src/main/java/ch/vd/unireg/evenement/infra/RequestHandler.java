package ch.vd.unireg.evenement.infra;

import java.util.List;

import org.springframework.core.io.ClassPathResource;

import ch.vd.unireg.xml.event.infra.v1.Request;
import ch.vd.unireg.xml.event.infra.v1.Response;
import ch.vd.unireg.evenement.RequestHandlerResult;
import ch.vd.unireg.jms.EsbBusinessException;
import ch.vd.unireg.xml.ServiceException;

public interface RequestHandler<T extends Request> {
	/**
	 * Reçoit et répond à la requête.
	 *
	 * @param request la requête
	 * @return le résultat du traitement, qui contient la réponse + les éventuels fichiers attachés
	 * @throws ServiceException en cas d'impossibilité de répondre à la requête
	 */
	RequestHandlerResult<? extends Response> handle(T request) throws ServiceException, EsbBusinessException;

	ClassPathResource getRequestXSD();

	List<ClassPathResource> getResponseXSD();
}
