package ch.vd.unireg.evenement.declaration;

import java.util.List;
import java.util.Map;

import ch.vd.unireg.jms.EsbBusinessException;
import ch.vd.unireg.xml.event.declaration.v2.DeclarationEvent;

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
	List<String> getXSDs();
}
