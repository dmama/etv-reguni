package ch.vd.uniregctb.evenement.declaration;

import java.util.Map;

import org.springframework.core.io.ClassPathResource;

import ch.vd.unireg.xml.event.declaration.v2.DeclarationEvent;
import ch.vd.uniregctb.jms.EsbBusinessException;

/**
 * Interface de base des handlers d'événements de déclaration entrant
 * @param <T> type de l'événement entrant
 */
public interface EvenementDeclarationHandler<T extends DeclarationEvent> {

	/**
	 * C'est ici qu'on fait le boulot
	 * @param event événement entrant
	 * @param headers headers du message entrant
	 * @throws EsbBusinessException en cas de souci (= renvoi dans TAO-Admin)
	 */
	void handle(T event, Map<String, String> headers) throws EsbBusinessException;

	/**
	 * @return la XSD de l'événement entrant
	 */
	ClassPathResource getXSD();
}
