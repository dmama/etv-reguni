package ch.vd.uniregctb.evenement.rapport.travail;

import java.util.List;

import org.springframework.core.io.ClassPathResource;

import ch.vd.registre.base.validation.ValidationException;
import ch.vd.unireg.xml.event.rt.response.v1.MiseAJourRapportTravailResponse;
import ch.vd.uniregctb.xml.ServiceException;

public interface RapportTravailRequestHandler {
	/**
	 * Reçoit et répond à la requête.
	 *
	 *
	 * @param request la requête
	 * @return la réponse
	 * @throws ch.vd.uniregctb.xml.ServiceException en cas d'impossibilité de répondre à la requête
	 */
	MiseAJourRapportTravailResponse handle(MiseAjourRapportTravail request) throws ServiceException, ValidationException;

	ClassPathResource getRequestXSD();

	List<ClassPathResource> getResponseXSD();
}
