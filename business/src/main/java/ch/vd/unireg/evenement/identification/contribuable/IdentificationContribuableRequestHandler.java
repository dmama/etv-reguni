package ch.vd.unireg.evenement.identification.contribuable;

import javax.xml.bind.JAXBElement;
import java.util.List;

import org.springframework.core.io.ClassPathResource;

import ch.vd.unireg.jms.EsbBusinessException;

public interface IdentificationContribuableRequestHandler<REQ, RESP> {

	/**
	 * @return le chemin vers la XSD de la requête
	 */
	ClassPathResource getRequestXSD();

	/**
	 * @return le ou les chemins vers la ou les XSD des réponses possibles
	 */
	List<ClassPathResource> getResponseXSD();

	/**
	 * Traite la requête de demande d'identification
	 * @param request la requête
	 * @return la réponse à la requête traitée, déjà dans sa forme <i>marshallable</i>
	 * @throws EsbBusinessException en cas de problème à notifier à TAO-Admin
	 */
	JAXBElement<RESP> handle(REQ request, String businessId) throws EsbBusinessException;
}
