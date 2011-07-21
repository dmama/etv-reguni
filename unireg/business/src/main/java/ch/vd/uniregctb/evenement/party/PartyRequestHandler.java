package ch.vd.uniregctb.evenement.party;

import org.springframework.core.io.ClassPathResource;

import ch.vd.unireg.xml.event.party.Request;
import ch.vd.unireg.xml.event.party.Response;
import ch.vd.uniregctb.xml.ServiceException;

/**
 * Handler de requêtes sur les données de tiers.
 */
public interface PartyRequestHandler<T extends Request> {
	/**
	 * Reçoit et répond à la requête.
	 *
	 * @param request la requête
	 * @return une réponse
	 * @throws ServiceException en cas d'impossibilité de répondre à la requête
	 */
	Response handle(T request) throws ServiceException;

	ClassPathResource getRequestXSD();

	ClassPathResource getResponseXSD();
}
