package ch.vd.uniregctb.evenement.party;

import org.springframework.core.io.ClassPathResource;

import ch.vd.unireg.xml.event.party.v1.Request;
import ch.vd.uniregctb.xml.ServiceException;

/**
 * Handler de requêtes sur les données de tiers.
 */
public interface RequestHandler<T extends Request> {
	/**
	 * Reçoit et répond à la requête.
	 *
	 * @param request la requête
	 * @return le résultat du traitement, qui contient la réponse + les éventuels fichiers attachés
	 * @throws ServiceException en cas d'impossibilité de répondre à la requête
	 */
	RequestHandlerResult handle(T request) throws ServiceException;

	ClassPathResource getRequestXSD();

	ClassPathResource getResponseXSD();
}
