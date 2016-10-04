package ch.vd.uniregctb.jms;

/**
 * Interface qui définit les attributs qui doivent être exposés par JMX
 * sur une instance de {@link ch.vd.technical.esb.jms.EsbMessageEndpointManager}
 */
@SuppressWarnings({"UnusedDeclaration"})
public interface MessageListenerContainerJmxInterface {

	/**
	 * @return le nom de la queue connectée aux listeners de ce container
	 */
	String getDestinationName();

	/**
	 * @return une description du but métier ou non de ce listener
	 */
	String getDescription();

	/**
	 * @return le nombre maximal configuré de consommateurs (= listeners) concurrents
	 */
	int getMaxConcurrentConsumers();

	/**
	 * @return le nombre de messages reçus depuis le démarrage de l'application
	 */
	int getReceivedMessages();

	/**
	 * @return le nombre de messages reçus qui ont lancé une exception (-> départ DLQ)
	 */
	int getMessagesWithException();

	/**
	 * @return le nombre de messages reçus que l'on a renvoyé en erreur (-> ERROR queue ou XXX-admin)
	 */
	int getMessagesWithBusinessError();

	/**
	 * Ouverture de vanne
	 */
	void start();

	/**
	 * Fermeture de vanne
	 */
	void stop();

	/**
	 * @return <code>true</code> si la vanne est ouverte
	 */
	boolean isRunning();
}
