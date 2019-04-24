package ch.vd.unireg.evenement.party;

import java.util.List;

import org.jetbrains.annotations.NotNull;

import ch.vd.unireg.evenement.RequestHandlerResult;
import ch.vd.unireg.jms.EsbBusinessException;
import ch.vd.unireg.xml.ServiceException;
import ch.vd.unireg.xml.event.party.v1.Request;
import ch.vd.unireg.xml.event.party.v1.Response;

/**
 * Handler de requêtes sur les données de tiers.
 */
public interface RequestHandlerV1<T extends Request> {
	/**
	 * Reçoit et répond à la requête.
	 *
	 * @param request la requête
	 * @return le résultat du traitement, qui contient la réponse + les éventuels fichiers attachés
	 * @throws ServiceException en cas d'impossibilité de répondre à la requête
	 */
	RequestHandlerResult<? extends Response> handle(T request) throws ServiceException, EsbBusinessException;

	@NotNull
	List<String> getRequestXSDs();

	@NotNull
	List<String> getResponseXSDs();
}
