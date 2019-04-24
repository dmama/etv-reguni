package ch.vd.unireg.evenement.rapport.travail;

import java.util.List;

import ch.vd.shared.validation.ValidationException;
import ch.vd.unireg.xml.ServiceException;
import ch.vd.unireg.xml.event.rt.response.v1.MiseAJourRapportTravailResponse;

public interface RapportTravailRequestHandler {
	/**
	 * Reçoit et répond à la requête.
	 *
	 *
	 * @param request la requête
	 * @return la réponse
	 * @throws ch.vd.unireg.xml.ServiceException en cas d'impossibilité de répondre à la requête
	 */
	MiseAJourRapportTravailResponse handle(MiseAjourRapportTravail request) throws ServiceException, ValidationException;

	List<String> getRequestXSDs();

	List<String> getResponseXSDs();
}
