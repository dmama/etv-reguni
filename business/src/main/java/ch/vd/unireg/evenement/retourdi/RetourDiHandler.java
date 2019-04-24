package ch.vd.unireg.evenement.retourdi;

import java.util.Map;

import ch.vd.unireg.jms.EsbBusinessException;

/**
 * Interface de base des handlers chargés de prendre en compte les données en retour d'une déclaration d'impôt
 * @param <T> le type de la donnée retournée
 */
public interface RetourDiHandler<T> {

	/**
	 * @return la XSD reconnue par ce handler
	 */
	String getRequestXSD();

	/**
	 * @return la classe (correspondant à la XSD renvoyée par {@link #getRequestXSD()}) traitée par ce handler
	 */
	Class<T> getHandledClass();

	/**
	 * Traitement des données reçues
	 * @param document document reçu regroupant les données présentes dans la DI
	 * @param incomingHeaders les headers (méta-information) du message entrant
	 * @throws EsbBusinessException en cas de souci
	 */
	void doHandle(T document, Map<String, String> incomingHeaders) throws EsbBusinessException;
}
