package ch.vd.uniregctb.jms;

/**
 * Interface qui définit les attributs qui doivent être exposés par JMX
 * sur une instance de {@link org.springframework.jms.listener.DefaultMessageListenerContainer}
 */
@SuppressWarnings({"UnusedDeclaration"})
public interface MessageListenerContainerJmxInterface {

	/**
	 * @return le nom de la queue connectée aux listeners de ce container
	 */
	String getDestinationName();

	/**
	 * @return le nombre de consommateurs (= listeners) actifs
	 */
	int getActiveConsumerCount();

	/**
	 * @return le nombre de consommateurs (= listeners) concurrents
	 */
	int getConcurrentConsumers();

	/**
	 * @return le nombre maximal configuré de consommateurs (= listeners) concurrents
	 */
	int getMaxConcurrentConsumers();

}
