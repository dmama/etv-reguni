package ch.vd.uniregctb.jms;

/**
 * Interface qui définit les attributs qui doivent être exposés par JMX
 * sur une instance de {@link ch.vd.technical.esb.jms.EsbMessageEndpointManager}
 */
@SuppressWarnings({"UnusedDeclaration"})
public interface MessageEndpointManagerJmxInterface {

	/**
	 * @return le nom de la queue connectée aux listeners de ce container
	 */
	String getDestinationName();

	/**
	 * @return le nombre maximal configuré de consommateurs (= listeners) concurrents
	 */
	int getMaxConcurrentConsumers();

	/**
	 * @return le nombre de messages reçus depuis le démarrage de l'application
	 */
	int getReceivedMessages();

}
