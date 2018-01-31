package ch.vd.uniregctb.evenement.party;

import java.util.List;

import org.springframework.core.io.ClassPathResource;

import ch.vd.unireg.xml.event.party.v2.Request;
import ch.vd.unireg.xml.event.party.v2.Response;
import ch.vd.uniregctb.evenement.RequestHandlerResult;
import ch.vd.uniregctb.jms.EsbBusinessException;
import ch.vd.uniregctb.xml.ServiceException;

/**
 * Handler de requêtes sur les données de tiers.
 */
public interface RequestHandlerV2<T extends Request> {
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
